package com.gitnx.repository.service;

import com.gitnx.common.exception.GitOperationException;
import com.gitnx.common.exception.ResourceNotFoundException;
import com.gitnx.repository.dto.CommitDto;
import com.gitnx.repository.dto.DiffEntryDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawTextComparator;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommitService {

    private final CodeBrowserService codeBrowserService;

    public List<CommitDto> getCommitLog(String owner, String repoName, String branch, int page, int size) {
        try (Repository repo = codeBrowserService.openRepository(owner, repoName)) {
            ObjectId branchId = repo.resolve("refs/heads/" + branch);
            if (branchId == null) return List.of();

            List<CommitDto> commits = new ArrayList<>();
            try (RevWalk revWalk = new RevWalk(repo)) {
                revWalk.markStart(revWalk.parseCommit(branchId));

                int skip = page * size;
                int count = 0;
                for (RevCommit commit : revWalk) {
                    if (count < skip) {
                        count++;
                        continue;
                    }
                    if (commits.size() >= size) break;

                    commits.add(toCommitDto(commit));
                    count++;
                }
            }
            return commits;
        } catch (IOException e) {
            throw new GitOperationException("Failed to read commit log", e);
        }
    }

    public CommitDto getCommit(String owner, String repoName, String commitHash) {
        try (Repository repo = codeBrowserService.openRepository(owner, repoName)) {
            ObjectId commitId = repo.resolve(commitHash);
            if (commitId == null) {
                throw new ResourceNotFoundException("Commit not found: " + commitHash);
            }

            try (RevWalk revWalk = new RevWalk(repo)) {
                RevCommit commit = revWalk.parseCommit(commitId);
                return toCommitDto(commit);
            }
        } catch (IOException e) {
            throw new GitOperationException("Failed to read commit", e);
        }
    }

    public List<DiffEntryDto> getCommitDiff(String owner, String repoName, String commitHash) {
        try (Repository repo = codeBrowserService.openRepository(owner, repoName)) {
            ObjectId commitId = repo.resolve(commitHash);
            if (commitId == null) {
                throw new ResourceNotFoundException("Commit not found: " + commitHash);
            }

            try (RevWalk revWalk = new RevWalk(repo)) {
                RevCommit commit = revWalk.parseCommit(commitId);

                AbstractTreeIterator oldTreeIter;
                if (commit.getParentCount() > 0) {
                    RevCommit parent = revWalk.parseCommit(commit.getParent(0));
                    oldTreeIter = prepareTreeParser(repo, parent.getTree());
                } else {
                    oldTreeIter = new EmptyTreeIterator();
                }

                AbstractTreeIterator newTreeIter = prepareTreeParser(repo, commit.getTree());

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try (DiffFormatter df = new DiffFormatter(out)) {
                    df.setRepository(repo);
                    df.setDiffComparator(RawTextComparator.DEFAULT);
                    df.setDetectRenames(true);

                    List<DiffEntry> diffs = df.scan(oldTreeIter, newTreeIter);
                    List<DiffEntryDto> result = new ArrayList<>();

                    for (DiffEntry diff : diffs) {
                        out.reset();
                        df.format(diff);
                        String diffContent = out.toString(StandardCharsets.UTF_8);

                        result.add(DiffEntryDto.builder()
                                .oldPath(diff.getOldPath())
                                .newPath(diff.getNewPath())
                                .changeType(diff.getChangeType().name())
                                .diffContent(diffContent)
                                .build());
                    }
                    return result;
                }
            }
        } catch (IOException e) {
            throw new GitOperationException("Failed to compute diff", e);
        }
    }

    public List<DiffEntryDto> getBranchDiff(String owner, String repoName, String sourceBranch, String targetBranch) {
        try (Repository repo = codeBrowserService.openRepository(owner, repoName)) {
            ObjectId sourceId = repo.resolve("refs/heads/" + sourceBranch);
            ObjectId targetId = repo.resolve("refs/heads/" + targetBranch);
            if (sourceId == null || targetId == null) return List.of();

            try (RevWalk revWalk = new RevWalk(repo)) {
                RevCommit sourceCommit = revWalk.parseCommit(sourceId);
                RevCommit targetCommit = revWalk.parseCommit(targetId);

                AbstractTreeIterator oldTreeIter = prepareTreeParser(repo, targetCommit.getTree());
                AbstractTreeIterator newTreeIter = prepareTreeParser(repo, sourceCommit.getTree());

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try (DiffFormatter df = new DiffFormatter(out)) {
                    df.setRepository(repo);
                    df.setDiffComparator(RawTextComparator.DEFAULT);
                    df.setDetectRenames(true);

                    List<DiffEntry> diffs = df.scan(oldTreeIter, newTreeIter);
                    List<DiffEntryDto> result = new ArrayList<>();

                    for (DiffEntry diff : diffs) {
                        out.reset();
                        df.format(diff);
                        String diffContent = out.toString(StandardCharsets.UTF_8);

                        result.add(DiffEntryDto.builder()
                                .oldPath(diff.getOldPath())
                                .newPath(diff.getNewPath())
                                .changeType(diff.getChangeType().name())
                                .diffContent(diffContent)
                                .build());
                    }
                    return result;
                }
            }
        } catch (IOException e) {
            throw new GitOperationException("Failed to compute branch diff", e);
        }
    }

    private CanonicalTreeParser prepareTreeParser(Repository repo, RevTree tree) throws IOException {
        CanonicalTreeParser parser = new CanonicalTreeParser();
        try (var reader = repo.newObjectReader()) {
            parser.reset(reader, tree.getId());
        }
        return parser;
    }

    private CommitDto toCommitDto(RevCommit commit) {
        return CommitDto.builder()
                .hash(commit.getName())
                .shortHash(commit.getName().substring(0, 7))
                .message(commit.getFullMessage())
                .authorName(commit.getAuthorIdent().getName())
                .authorEmail(commit.getAuthorIdent().getEmailAddress())
                .authorDate(LocalDateTime.ofInstant(
                        Instant.ofEpochSecond(commit.getCommitTime()),
                        ZoneId.systemDefault()))
                .parentCount(commit.getParentCount())
                .build();
    }
}
