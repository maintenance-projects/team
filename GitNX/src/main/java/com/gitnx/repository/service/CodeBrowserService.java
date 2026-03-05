package com.gitnx.repository.service;

import com.gitnx.common.exception.GitOperationException;
import com.gitnx.common.exception.ResourceNotFoundException;
import com.gitnx.common.util.FileTypeUtils;
import com.gitnx.common.util.MarkdownRenderer;
import com.gitnx.repository.dto.FileContentDto;
import com.gitnx.repository.dto.FileTreeEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CodeBrowserService {

    private final GitRepositoryService gitRepositoryService;
    private final MarkdownRenderer markdownRenderer;

    public Repository openRepository(String owner, String repoName) {
        File repoDir = gitRepositoryService.getRepoDiskPath(owner, repoName);
        try {
            return new FileRepositoryBuilder()
                    .setGitDir(repoDir)
                    .readEnvironment()
                    .build();
        } catch (IOException e) {
            throw new GitOperationException("Failed to open repository: " + owner + "/" + repoName, e);
        }
    }

    public List<FileTreeEntry> getTree(String owner, String repoName, String branch, String path) {
        try (Repository repo = openRepository(owner, repoName)) {
            ObjectId branchId = repo.resolve("refs/heads/" + branch);
            if (branchId == null) {
                return Collections.emptyList();
            }

            try (RevWalk revWalk = new RevWalk(repo)) {
                RevCommit commit = revWalk.parseCommit(branchId);
                RevTree tree = commit.getTree();

                List<FileTreeEntry> entries = new ArrayList<>();
                try (TreeWalk treeWalk = new TreeWalk(repo)) {
                    treeWalk.addTree(tree);
                    treeWalk.setRecursive(false);

                    if (path != null && !path.isEmpty()) {
                        treeWalk.setFilter(PathFilter.create(path));
                        // Need to walk into the path
                        while (treeWalk.next()) {
                            if (treeWalk.isSubtree() && treeWalk.getPathString().equals(path)) {
                                treeWalk.enterSubtree();
                                break;
                            } else if (treeWalk.isSubtree() && path.startsWith(treeWalk.getPathString() + "/")) {
                                treeWalk.enterSubtree();
                            }
                        }
                        // Now collect entries at this level
                        while (treeWalk.next()) {
                            String entryPath = treeWalk.getPathString();
                            // Only entries directly under the given path
                            if (!entryPath.startsWith(path + "/")) continue;
                            String relativeName = entryPath.substring(path.length() + 1);
                            if (relativeName.contains("/")) continue;

                            addEntry(entries, treeWalk, repo);
                        }
                    } else {
                        while (treeWalk.next()) {
                            addEntry(entries, treeWalk, repo);
                        }
                    }
                }

                Collections.sort(entries);
                return entries;
            }
        } catch (IOException e) {
            throw new GitOperationException("Failed to get file tree", e);
        }
    }

    private void addEntry(List<FileTreeEntry> entries, TreeWalk treeWalk, Repository repo) throws IOException {
        String type = treeWalk.isSubtree() ? "dir" : "file";
        long size = 0;
        if (!treeWalk.isSubtree()) {
            ObjectLoader loader = repo.open(treeWalk.getObjectId(0));
            size = loader.getSize();
        }

        entries.add(FileTreeEntry.builder()
                .name(treeWalk.getNameString())
                .path(treeWalk.getPathString())
                .type(type)
                .size(size)
                .build());
    }

    public FileContentDto getFileContent(String owner, String repoName, String branch, String path) {
        try (Repository repo = openRepository(owner, repoName)) {
            ObjectId branchId = repo.resolve("refs/heads/" + branch);
            if (branchId == null) {
                throw new ResourceNotFoundException("Branch not found: " + branch);
            }

            try (RevWalk revWalk = new RevWalk(repo)) {
                RevCommit commit = revWalk.parseCommit(branchId);
                RevTree tree = commit.getTree();

                try (TreeWalk treeWalk = TreeWalk.forPath(repo, path, tree)) {
                    if (treeWalk == null) {
                        throw new ResourceNotFoundException("File not found: " + path);
                    }

                    ObjectLoader loader = repo.open(treeWalk.getObjectId(0));
                    byte[] bytes = loader.getBytes();
                    boolean binary = FileTypeUtils.isBinary(bytes);

                    String fileName = path.contains("/") ? path.substring(path.lastIndexOf('/') + 1) : path;
                    String content = binary ? null : new String(bytes, StandardCharsets.UTF_8);
                    int lineCount = content != null ? content.split("\n", -1).length : 0;

                    return FileContentDto.builder()
                            .name(fileName)
                            .path(path)
                            .content(content)
                            .language(FileTypeUtils.detectLanguage(fileName))
                            .size(bytes.length)
                            .lineCount(lineCount)
                            .binary(binary)
                            .build();
                }
            }
        } catch (IOException e) {
            throw new GitOperationException("Failed to read file content", e);
        }
    }

    public String getReadmeContent(String owner, String repoName, String branch) {
        try (Repository repo = openRepository(owner, repoName)) {
            ObjectId branchId = repo.resolve("refs/heads/" + branch);
            if (branchId == null) return null;

            try (RevWalk revWalk = new RevWalk(repo)) {
                RevCommit commit = revWalk.parseCommit(branchId);
                RevTree tree = commit.getTree();

                String[] readmeNames = {"README.md", "readme.md", "Readme.md", "README", "README.txt"};
                for (String name : readmeNames) {
                    try (TreeWalk treeWalk = TreeWalk.forPath(repo, name, tree)) {
                        if (treeWalk != null) {
                            ObjectLoader loader = repo.open(treeWalk.getObjectId(0));
                            String content = new String(loader.getBytes(), StandardCharsets.UTF_8);
                            if (name.toLowerCase().endsWith(".md")) {
                                return markdownRenderer.render(content);
                            }
                            return "<pre>" + content + "</pre>";
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Failed to read README", e);
        }
        return null;
    }

    public String getReadmeFileName(String owner, String repoName, String branch) {
        try (Repository repo = openRepository(owner, repoName)) {
            ObjectId branchId = repo.resolve("refs/heads/" + branch);
            if (branchId == null) return null;

            try (RevWalk revWalk = new RevWalk(repo)) {
                RevCommit commit = revWalk.parseCommit(branchId);
                RevTree tree = commit.getTree();

                String[] readmeNames = {"README.md", "readme.md", "Readme.md", "README", "README.txt"};
                for (String name : readmeNames) {
                    try (TreeWalk treeWalk = TreeWalk.forPath(repo, name, tree)) {
                        if (treeWalk != null) {
                            return name;
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.warn("Failed to find README filename", e);
        }
        return null;
    }
}
