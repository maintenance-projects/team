package com.gitnx.mergerequest.service;

import com.gitnx.common.exception.ResourceNotFoundException;
import com.gitnx.mergerequest.entity.MergeRequest;
import com.gitnx.mergerequest.entity.MergeRequestReviewer;
import com.gitnx.mergerequest.entity.ReviewComment;
import com.gitnx.mergerequest.enums.MergeRequestState;
import com.gitnx.mergerequest.enums.ReviewState;
import com.gitnx.mergerequest.repository.MergeRequestJpaRepository;
import com.gitnx.mergerequest.repository.MergeRequestReviewerJpaRepository;
import com.gitnx.mergerequest.repository.ReviewCommentJpaRepository;
import com.gitnx.repository.entity.GitRepository;
import com.gitnx.repository.service.GitRepositoryService;
import com.gitnx.user.entity.User;
import com.gitnx.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MergeRequestService {

    private final MergeRequestJpaRepository mrRepository;
    private final ReviewCommentJpaRepository reviewCommentRepository;
    private final MergeRequestReviewerJpaRepository reviewerRepository;
    private final GitRepositoryService gitRepositoryService;
    private final UserService userService;

    @Transactional
    public MergeRequest create(String owner, String repoName, String title, String description,
                                String sourceBranch, String targetBranch, String authorUsername) {
        GitRepository repo = gitRepositoryService.getByOwnerAndName(owner, repoName);
        User author = userService.getByUsername(authorUsername);
        int nextNumber = mrRepository.findMaxMrNumber(repo.getId()) + 1;

        MergeRequest mr = MergeRequest.builder()
                .mrNumber(nextNumber)
                .title(title)
                .description(description)
                .sourceBranch(sourceBranch)
                .targetBranch(targetBranch)
                .gitRepository(repo)
                .author(author)
                .build();

        return mrRepository.save(mr);
    }

    public Page<MergeRequest> list(String owner, String repoName, MergeRequestState state, int page) {
        GitRepository repo = gitRepositoryService.getByOwnerAndName(owner, repoName);
        PageRequest pageRequest = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (state != null) {
            return mrRepository.findByGitRepositoryIdAndState(repo.getId(), state, pageRequest);
        }
        return mrRepository.findByGitRepositoryId(repo.getId(), pageRequest);
    }

    public MergeRequest getByNumber(String owner, String repoName, int mrNumber) {
        GitRepository repo = gitRepositoryService.getByOwnerAndName(owner, repoName);
        return mrRepository.findByGitRepositoryIdAndMrNumber(repo.getId(), mrNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Merge Request !" + mrNumber + " not found"));
    }

    @Transactional
    public MergeRequest close(String owner, String repoName, int mrNumber) {
        MergeRequest mr = getByNumber(owner, repoName, mrNumber);
        mr.setState(MergeRequestState.CLOSED);
        return mrRepository.save(mr);
    }

    @Transactional
    public MergeRequest reopen(String owner, String repoName, int mrNumber) {
        MergeRequest mr = getByNumber(owner, repoName, mrNumber);
        if (mr.getState() == MergeRequestState.MERGED) {
            throw new IllegalStateException("Cannot reopen a merged merge request");
        }
        mr.setState(MergeRequestState.OPEN);
        return mrRepository.save(mr);
    }

    @Transactional
    public void markAsMerged(MergeRequest mr, String mergedByUsername) {
        User mergedBy = userService.getByUsername(mergedByUsername);
        mr.setState(MergeRequestState.MERGED);
        mr.setMergedAt(LocalDateTime.now());
        mr.setMergedBy(mergedBy);
        mrRepository.save(mr);
    }

    @Transactional
    public ReviewComment addComment(String owner, String repoName, int mrNumber,
                                     String body, String filePath, Integer lineNumber,
                                     String authorUsername) {
        MergeRequest mr = getByNumber(owner, repoName, mrNumber);
        User author = userService.getByUsername(authorUsername);

        ReviewComment comment = ReviewComment.builder()
                .body(body)
                .filePath(filePath)
                .lineNumber(lineNumber)
                .mergeRequest(mr)
                .author(author)
                .build();

        return reviewCommentRepository.save(comment);
    }

    @Transactional
    public MergeRequestReviewer approve(String owner, String repoName, int mrNumber, String reviewerUsername) {
        MergeRequest mr = getByNumber(owner, repoName, mrNumber);
        User reviewer = userService.getByUsername(reviewerUsername);

        MergeRequestReviewer mrReviewer = reviewerRepository
                .findByMergeRequestIdAndReviewerId(mr.getId(), reviewer.getId())
                .orElseGet(() -> MergeRequestReviewer.builder()
                        .mergeRequest(mr)
                        .reviewer(reviewer)
                        .build());

        mrReviewer.setState(ReviewState.APPROVED);
        return reviewerRepository.save(mrReviewer);
    }

    @Transactional
    public MergeRequestReviewer requestChanges(String owner, String repoName, int mrNumber, String reviewerUsername) {
        MergeRequest mr = getByNumber(owner, repoName, mrNumber);
        User reviewer = userService.getByUsername(reviewerUsername);

        MergeRequestReviewer mrReviewer = reviewerRepository
                .findByMergeRequestIdAndReviewerId(mr.getId(), reviewer.getId())
                .orElseGet(() -> MergeRequestReviewer.builder()
                        .mergeRequest(mr)
                        .reviewer(reviewer)
                        .build());

        mrReviewer.setState(ReviewState.CHANGES_REQUESTED);
        return reviewerRepository.save(mrReviewer);
    }

    public long countByState(String owner, String repoName, MergeRequestState state) {
        GitRepository repo = gitRepositoryService.getByOwnerAndName(owner, repoName);
        return mrRepository.countByGitRepositoryIdAndState(repo.getId(), state);
    }
}
