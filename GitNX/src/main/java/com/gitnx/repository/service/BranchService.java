package com.gitnx.repository.service;

import com.gitnx.common.exception.GitOperationException;
import com.gitnx.repository.dto.BranchDto;
import com.gitnx.repository.entity.GitRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BranchService {

    private final CodeBrowserService codeBrowserService;
    private final GitRepositoryService gitRepositoryService;

    public List<BranchDto> listBranches(String owner, String repoName) {
        GitRepository gitRepo = gitRepositoryService.getByOwnerAndName(owner, repoName);
        String defaultBranch = gitRepo.getDefaultBranch();

        try (Repository repo = codeBrowserService.openRepository(owner, repoName)) {
            Git git = new Git(repo);
            List<Ref> refs = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();

            List<BranchDto> branches = new ArrayList<>();
            try (RevWalk revWalk = new RevWalk(repo)) {
                for (Ref ref : refs) {
                    String name = ref.getName();
                    if (!name.startsWith("refs/heads/")) continue;
                    String branchName = name.substring("refs/heads/".length());

                    String commitHash = "";
                    String commitMessage = "";
                    try {
                        RevCommit commit = revWalk.parseCommit(ref.getObjectId());
                        commitHash = commit.getName().substring(0, 7);
                        commitMessage = commit.getShortMessage();
                        revWalk.reset();
                    } catch (Exception e) {
                        log.warn("Failed to parse commit for branch {}", branchName, e);
                    }

                    branches.add(BranchDto.builder()
                            .name(branchName)
                            .isDefault(branchName.equals(defaultBranch))
                            .lastCommitHash(commitHash)
                            .lastCommitMessage(commitMessage)
                            .build());
                }
            }
            return branches;
        } catch (Exception e) {
            throw new GitOperationException("Failed to list branches", e);
        }
    }

    public BranchDto createBranch(String owner, String repoName, String branchName, String sourceBranch) {
        GitRepository gitRepo = gitRepositoryService.getByOwnerAndName(owner, repoName);
        if (sourceBranch == null || sourceBranch.isBlank()) {
            sourceBranch = gitRepo.getDefaultBranch();
        }

        try (Repository repo = codeBrowserService.openRepository(owner, repoName)) {
            if (repo.resolve("refs/heads/" + sourceBranch) == null) {
                throw new IllegalArgumentException("Source branch '" + sourceBranch + "' does not exist");
            }
            if (repo.resolve("refs/heads/" + branchName) != null) {
                throw new IllegalArgumentException("Branch '" + branchName + "' already exists");
            }

            Git git = new Git(repo);
            git.branchCreate()
                    .setName(branchName)
                    .setStartPoint("refs/heads/" + sourceBranch)
                    .call();

            Ref newRef = repo.exactRef("refs/heads/" + branchName);
            String commitHash = "";
            String commitMessage = "";
            try (RevWalk revWalk = new RevWalk(repo)) {
                RevCommit commit = revWalk.parseCommit(newRef.getObjectId());
                commitHash = commit.getName().substring(0, 7);
                commitMessage = commit.getShortMessage();
            }

            return BranchDto.builder()
                    .name(branchName)
                    .isDefault(false)
                    .lastCommitHash(commitHash)
                    .lastCommitMessage(commitMessage)
                    .build();
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new GitOperationException("Failed to create branch", e);
        }
    }

    public boolean branchExists(String owner, String repoName, String branchName) {
        try (Repository repo = codeBrowserService.openRepository(owner, repoName)) {
            return repo.resolve("refs/heads/" + branchName) != null;
        } catch (Exception e) {
            return false;
        }
    }
}
