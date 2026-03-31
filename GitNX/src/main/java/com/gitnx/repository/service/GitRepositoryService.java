package com.gitnx.repository.service;

import com.gitnx.common.config.JGitConfig;
import com.gitnx.common.exception.GitOperationException;
import com.gitnx.common.exception.ResourceNotFoundException;
import com.gitnx.repository.dto.CreateRepositoryRequest;
import com.gitnx.repository.dto.ImportRepositoryRequest;
import com.gitnx.repository.dto.RepositoryDto;
import com.gitnx.repository.entity.GitRepository;
import com.gitnx.repository.entity.RepositoryMember;
import com.gitnx.repository.enums.RepositoryRole;
import com.gitnx.repository.repository.GitRepositoryJpaRepository;
import com.gitnx.repository.repository.RepositoryMemberJpaRepository;
import com.gitnx.issue.repository.IssueJpaRepository;
import com.gitnx.issue.repository.LabelJpaRepository;
import com.gitnx.issue.repository.MilestoneJpaRepository;
import com.gitnx.mergerequest.repository.MergeRequestJpaRepository;
import com.gitnx.user.entity.User;
import com.gitnx.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.TransportHttp;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class GitRepositoryService {

    private final GitRepositoryJpaRepository repoJpaRepository;
    private final RepositoryMemberJpaRepository memberJpaRepository;
    private final IssueJpaRepository issueJpaRepository;
    private final LabelJpaRepository labelJpaRepository;
    private final MilestoneJpaRepository milestoneJpaRepository;
    private final MergeRequestJpaRepository mergeRequestJpaRepository;
    private final UserService userService;
    private final JGitConfig jGitConfig;

    @Transactional
    public GitRepository create(String ownerUsername, CreateRepositoryRequest request) {
        User owner = userService.getByUsername(ownerUsername);

        if (repoJpaRepository.existsByOwnerAndName(owner, request.getName())) {
            throw new IllegalArgumentException("Repository already exists: " + request.getName());
        }

        String diskPath = jGitConfig.getBasePath() + "/" + ownerUsername + "/" + request.getName() + ".git";

        GitRepository repo = GitRepository.builder()
                .name(request.getName())
                .description(request.getDescription())
                .visibility(request.getVisibility())
                .defaultBranch(request.getDefaultBranch())
                .diskPath(diskPath)
                .owner(owner)
                .build();

        repo = repoJpaRepository.save(repo);

        // Create bare git repo on disk
        initBareRepository(diskPath, request, owner);

        // Add owner as member
        RepositoryMember member = RepositoryMember.builder()
                .gitRepository(repo)
                .user(owner)
                .role(RepositoryRole.OWNER)
                .build();
        memberJpaRepository.save(member);

        return repo;
    }

    @Transactional
    public GitRepository importFromUrl(String ownerUsername, ImportRepositoryRequest request) {
        User owner = userService.getByUsername(ownerUsername);

        // Extract repo name from URL if not provided
        String repoName = request.getName();
        if (repoName == null || repoName.isBlank()) {
            repoName = extractRepoNameFromUrl(request.getCloneUrl());
        }

        if (repoJpaRepository.existsByOwnerAndName(owner, repoName)) {
            throw new IllegalArgumentException("Repository already exists: " + repoName);
        }

        String diskPath = jGitConfig.getBasePath() + "/" + ownerUsername + "/" + repoName + ".git";
        File repoDir = new File(diskPath);
        repoDir.getParentFile().mkdirs();

        try {
            String cloneUrl = request.getCloneUrl();
            String token = request.getAccessToken();
            log.debug("Import - cloneUrl: {}, hasToken: {}",
                    cloneUrl, token != null && !token.isBlank());

            // 시스템 git 명령어로 clone (JGit은 GitHub private repo 인증 처리에 한계가 있음)
            // GitHub는 미인증 요청에 404를 반환하여 JGit의 CredentialsProvider가 동작하지 않음
            String authenticatedUrl = cloneUrl;
            if (token != null && !token.isBlank() && cloneUrl.startsWith("https://")) {
                authenticatedUrl = "https://x-access-token:" + token
                        + "@" + cloneUrl.substring("https://".length());
            }

            ProcessBuilder pb = new ProcessBuilder(
                    "git", "clone", "--bare", authenticatedUrl, diskPath);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                log.error("git clone failed (exit {}): {}", exitCode, output);
                throw new GitOperationException(
                        "Failed to clone repository from " + cloneUrl + ": " + output);
            }

            log.info("Successfully cloned repository from {} to {}", cloneUrl, diskPath);

        } catch (GitOperationException e) {
            // Clean up on failure
            cleanUpDirectory(repoDir);
            throw e;
        } catch (Exception e) {
            // Clean up on failure
            cleanUpDirectory(repoDir);
            log.error("Failed to clone repository from {}: {}", request.getCloneUrl(), e.getMessage(), e);
            throw new GitOperationException("Failed to clone repository from " + request.getCloneUrl(), e);
        }

        // Detect default branch from cloned repo
        String defaultBranch = detectDefaultBranch(repoDir);

        GitRepository repo = GitRepository.builder()
                .name(repoName)
                .description(request.getDescription())
                .visibility(request.getVisibility())
                .defaultBranch(defaultBranch)
                .diskPath(diskPath)
                .owner(owner)
                .build();

        repo = repoJpaRepository.save(repo);

        // Add owner as member
        RepositoryMember member = RepositoryMember.builder()
                .gitRepository(repo)
                .user(owner)
                .role(RepositoryRole.OWNER)
                .build();
        memberJpaRepository.save(member);

        return repo;
    }

    private void cleanUpDirectory(File dir) {
        if (dir.exists()) {
            try {
                Files.walk(dir.toPath())
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException ex) {
                log.warn("Failed to clean up directory: {}", dir, ex);
            }
        }
    }

    /**
     * Extract repository name from a Git clone URL.
     * e.g. "https://github.com/user/my-repo.git" → "my-repo"
     */
    private String extractRepoNameFromUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("Clone URL is required");
        }

        // Remove trailing slash and .git suffix
        String cleaned = url.trim();
        if (cleaned.endsWith("/")) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        if (cleaned.endsWith(".git")) {
            cleaned = cleaned.substring(0, cleaned.length() - 4);
        }

        // Extract last path segment
        int lastSlash = cleaned.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == cleaned.length() - 1) {
            throw new IllegalArgumentException("Cannot extract repository name from URL: " + url);
        }

        return cleaned.substring(lastSlash + 1);
    }

    /**
     * Detect the default branch from a bare repository's HEAD reference.
     * Falls back to "main" if detection fails.
     */
    private String detectDefaultBranch(File bareRepoDir) {
        try {
            Repository repository = new FileRepositoryBuilder()
                    .setGitDir(bareRepoDir)
                    .readEnvironment()
                    .build();

            String fullBranch = repository.getFullBranch();
            repository.close();

            if (fullBranch != null && fullBranch.startsWith("refs/heads/")) {
                return fullBranch.substring("refs/heads/".length());
            }
        } catch (IOException e) {
            log.warn("Failed to detect default branch for {}, falling back to 'main'", bareRepoDir, e);
        }
        return "main";
    }

    private void initBareRepository(String diskPath, CreateRepositoryRequest request, User owner) {
        File repoDir = new File(diskPath);
        repoDir.getParentFile().mkdirs();

        try {
            if (request.isInitWithReadme()) {
                // Create non-bare first, then convert
                File tempDir = Files.createTempDirectory("gitnx-init-").toFile();
                try (Git git = Git.init().setDirectory(tempDir).setInitialBranch(request.getDefaultBranch()).call()) {
                    // Create README.md
                    File readme = new File(tempDir, "README.md");
                    Files.writeString(readme.toPath(), "# " + request.getName() + "\n\n" +
                            (request.getDescription() != null ? request.getDescription() : ""));

                    git.add().addFilepattern(".").call();
                    git.commit()
                            .setMessage("Initial commit")
                            .setAuthor(new PersonIdent(owner.getDisplayName(), owner.getEmail()))
                            .call();

                    // Clone as bare
                    Git.cloneRepository()
                            .setURI(tempDir.toURI().toString())
                            .setDirectory(repoDir)
                            .setBare(true)
                            .call()
                            .close();
                }
                // Clean up temp dir
                Files.walk(tempDir.toPath())
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } else {
                Git.init().setDirectory(repoDir).setBare(true)
                        .setInitialBranch(request.getDefaultBranch()).call().close();
            }
        } catch (Exception e) {
            throw new GitOperationException("Failed to initialize git repository", e);
        }
    }

    public List<RepositoryDto> listAll() {
        return repoJpaRepository.findAll().stream()
                .map(RepositoryDto::from)
                .toList();
    }

    public List<RepositoryDto> listByOwner(String username) {
        User owner = userService.getByUsername(username);
        return repoJpaRepository.findByOwnerOrderByCreatedAtDesc(owner).stream()
                .map(RepositoryDto::from)
                .toList();
    }

    public List<RepositoryDto> listAccessible(String username) {
        return memberJpaRepository.findByUserUsernameWithRepository(username).stream()
                .map(member -> RepositoryDto.from(member.getGitRepository()))
                .toList();
    }

    public GitRepository getByOwnerAndName(String owner, String name) {
        return repoJpaRepository.findByOwnerUsernameAndName(owner, name)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Repository not found: " + owner + "/" + name));
    }

    @Transactional
    public void delete(String owner, String name) {
        GitRepository repo = getByOwnerAndName(owner, name);
        Long repoId = repo.getId();

        // Delete related entities first
        mergeRequestJpaRepository.deleteAll(mergeRequestJpaRepository.findByGitRepositoryId(repoId, org.springframework.data.domain.Pageable.unpaged()));
        issueJpaRepository.deleteAll(issueJpaRepository.findByGitRepositoryId(repoId, org.springframework.data.domain.Pageable.unpaged()));
        labelJpaRepository.deleteAll(labelJpaRepository.findByGitRepositoryId(repoId));
        milestoneJpaRepository.deleteAll(milestoneJpaRepository.findByGitRepositoryId(repoId));
        memberJpaRepository.deleteAll(memberJpaRepository.findByGitRepositoryId(repoId));

        // Delete bare repo from disk
        File repoDir = new File(repo.getDiskPath());
        if (repoDir.exists()) {
            try {
                Files.walk(repoDir.toPath())
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile)
                        .forEach(File::delete);
            } catch (IOException e) {
                log.warn("Failed to delete repo directory: {}", repoDir, e);
            }
        }

        repoJpaRepository.delete(repo);
    }

    public File getRepoDiskPath(String owner, String name) {
        GitRepository repo = getByOwnerAndName(owner, name);
        return new File(repo.getDiskPath());
    }
}
