package com.gitnx.mergerequest.service;

import com.gitnx.common.exception.GitOperationException;
import com.gitnx.mergerequest.entity.MergeRequest;
import com.gitnx.mergerequest.enums.MergeRequestState;
import com.gitnx.repository.service.CodeBrowserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.MergeResult;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MergeService {

    private final CodeBrowserService codeBrowserService;
    private final MergeRequestService mergeRequestService;

    public MergeResult merge(String owner, String repoName, int mrNumber, String mergedByUsername) {
        MergeRequest mr = mergeRequestService.getByNumber(owner, repoName, mrNumber);

        if (mr.getState() != MergeRequestState.OPEN) {
            throw new IllegalStateException("Merge request is not open");
        }

        try (Repository repo = codeBrowserService.openRepository(owner, repoName)) {
            Git git = new Git(repo);

            ObjectId sourceId = repo.resolve("refs/heads/" + mr.getSourceBranch());
            ObjectId targetId = repo.resolve("refs/heads/" + mr.getTargetBranch());

            if (sourceId == null) {
                throw new GitOperationException("Source branch not found: " + mr.getSourceBranch());
            }
            if (targetId == null) {
                throw new GitOperationException("Target branch not found: " + mr.getTargetBranch());
            }

            // Checkout target branch
            git.checkout().setName(mr.getTargetBranch()).call();

            // Merge source into target
            MergeResult result = git.merge()
                    .include(sourceId)
                    .setFastForward(MergeCommand.FastForwardMode.FF)
                    .setMessage("Merge branch '" + mr.getSourceBranch() + "' into " + mr.getTargetBranch()
                            + "\n\nMerge request !" + mr.getMrNumber())
                    .call();

            if (result.getMergeStatus().isSuccessful()) {
                mergeRequestService.markAsMerged(mr, mergedByUsername);
                log.info("Successfully merged MR !{} in {}/{}", mrNumber, owner, repoName);
            } else {
                // Reset on failure
                git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD)
                        .setRef(targetId.getName()).call();
                log.warn("Merge failed for MR !{}: {}", mrNumber, result.getMergeStatus());
            }

            return result;
        } catch (GitOperationException e) {
            throw e;
        } catch (Exception e) {
            throw new GitOperationException("Failed to merge: " + e.getMessage(), e);
        }
    }

    public boolean canMerge(String owner, String repoName, String sourceBranch, String targetBranch) {
        try (Repository repo = codeBrowserService.openRepository(owner, repoName)) {
            ObjectId sourceId = repo.resolve("refs/heads/" + sourceBranch);
            ObjectId targetId = repo.resolve("refs/heads/" + targetBranch);
            return sourceId != null && targetId != null;
        } catch (Exception e) {
            return false;
        }
    }
}
