package com.gitnx.issue.service;

import com.gitnx.common.exception.ResourceNotFoundException;
import com.gitnx.issue.entity.Issue;
import com.gitnx.issue.entity.IssueComment;
import com.gitnx.issue.entity.Label;
import com.gitnx.issue.enums.IssueState;
import com.gitnx.issue.repository.IssueCommentJpaRepository;
import com.gitnx.issue.repository.IssueJpaRepository;
import com.gitnx.issue.repository.LabelJpaRepository;
import com.gitnx.issue.repository.MilestoneJpaRepository;
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

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IssueService {

    private final IssueJpaRepository issueRepository;
    private final IssueCommentJpaRepository commentRepository;
    private final LabelJpaRepository labelRepository;
    private final MilestoneJpaRepository milestoneRepository;
    private final GitRepositoryService gitRepositoryService;
    private final UserService userService;

    @Transactional
    public Issue create(String owner, String repoName, String title, String body, String authorUsername) {
        GitRepository repo = gitRepositoryService.getByOwnerAndName(owner, repoName);
        User author = userService.getByUsername(authorUsername);
        int nextNumber = issueRepository.findMaxIssueNumber(repo.getId()) + 1;

        Issue issue = Issue.builder()
                .issueNumber(nextNumber)
                .title(title)
                .body(body)
                .gitRepository(repo)
                .author(author)
                .assignee(author)
                .build();

        return issueRepository.save(issue);
    }

    public Page<Issue> list(String owner, String repoName, IssueState state, int page) {
        GitRepository repo = gitRepositoryService.getByOwnerAndName(owner, repoName);
        PageRequest pageRequest = PageRequest.of(page, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        if (state != null) {
            return issueRepository.findByGitRepositoryIdAndState(repo.getId(), state, pageRequest);
        }
        return issueRepository.findByGitRepositoryId(repo.getId(), pageRequest);
    }

    public Issue getByNumber(String owner, String repoName, int issueNumber) {
        GitRepository repo = gitRepositoryService.getByOwnerAndName(owner, repoName);
        return issueRepository.findByGitRepositoryIdAndIssueNumber(repo.getId(), issueNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Issue #" + issueNumber + " not found"));
    }

    @Transactional
    public Issue close(String owner, String repoName, int issueNumber) {
        Issue issue = getByNumber(owner, repoName, issueNumber);
        issue.setState(IssueState.CLOSED);
        return issueRepository.save(issue);
    }

    @Transactional
    public Issue reopen(String owner, String repoName, int issueNumber) {
        Issue issue = getByNumber(owner, repoName, issueNumber);
        issue.setState(IssueState.OPEN);
        return issueRepository.save(issue);
    }

    @Transactional
    public Issue update(String owner, String repoName, int issueNumber, String title, String body) {
        Issue issue = getByNumber(owner, repoName, issueNumber);
        issue.setTitle(title);
        issue.setBody(body);
        return issueRepository.save(issue);
    }

    @Transactional
    public IssueComment addComment(String owner, String repoName, int issueNumber, String body, String authorUsername) {
        Issue issue = getByNumber(owner, repoName, issueNumber);
        User author = userService.getByUsername(authorUsername);

        IssueComment comment = IssueComment.builder()
                .body(body)
                .issue(issue)
                .author(author)
                .build();

        return commentRepository.save(comment);
    }

    @Transactional
    public void addLabel(String owner, String repoName, int issueNumber, Long labelId) {
        Issue issue = getByNumber(owner, repoName, issueNumber);
        Label label = labelRepository.findById(labelId)
                .orElseThrow(() -> new ResourceNotFoundException("Label not found"));
        if (!issue.getLabels().contains(label)) {
            issue.getLabels().add(label);
            issueRepository.save(issue);
        }
    }

    @Transactional
    public void removeLabel(String owner, String repoName, int issueNumber, Long labelId) {
        Issue issue = getByNumber(owner, repoName, issueNumber);
        issue.getLabels().removeIf(l -> l.getId().equals(labelId));
        issueRepository.save(issue);
    }

    public long countByState(String owner, String repoName, IssueState state) {
        GitRepository repo = gitRepositoryService.getByOwnerAndName(owner, repoName);
        return issueRepository.countByGitRepositoryIdAndState(repo.getId(), state);
    }

    // Label management
    public List<Label> getLabels(String owner, String repoName) {
        GitRepository repo = gitRepositoryService.getByOwnerAndName(owner, repoName);
        return labelRepository.findByGitRepositoryId(repo.getId());
    }

    @Transactional
    public Label createLabel(String owner, String repoName, String name, String color, String description) {
        GitRepository repo = gitRepositoryService.getByOwnerAndName(owner, repoName);
        if (labelRepository.existsByGitRepositoryIdAndName(repo.getId(), name)) {
            throw new IllegalArgumentException("Label already exists: " + name);
        }

        Label label = Label.builder()
                .name(name)
                .color(color)
                .description(description)
                .gitRepository(repo)
                .build();
        return labelRepository.save(label);
    }

    @Transactional
    public void deleteLabel(Long labelId) {
        labelRepository.deleteById(labelId);
    }
}
