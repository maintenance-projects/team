package com.spacenx.issue.service;

import com.spacenx.issue.dto.CreateCommentRequest;
import com.spacenx.issue.dto.CreateIssueRequest;
import com.spacenx.issue.dto.UpdateIssueRequest;
import com.spacenx.issue.entity.Issue;
import com.spacenx.issue.entity.IssueComment;
import com.spacenx.issue.entity.Label;
import com.spacenx.issue.enums.IssuePriority;
import com.spacenx.issue.enums.IssueStatus;
import com.spacenx.issue.enums.IssueType;
import com.spacenx.issue.repository.IssueCommentRepository;
import com.spacenx.issue.repository.IssueRepository;
import com.spacenx.issue.repository.LabelRepository;
import com.spacenx.space.entity.Space;
import com.spacenx.space.repository.SpaceRepository;
import com.spacenx.sprint.entity.Sprint;
import com.spacenx.sprint.repository.SprintRepository;
import com.spacenx.user.entity.User;
import com.spacenx.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IssueService {

    private final IssueRepository issueRepository;
    private final IssueCommentRepository issueCommentRepository;
    private final SpaceRepository spaceRepository;
    private final UserRepository userRepository;
    private final SprintRepository sprintRepository;
    private final LabelRepository labelRepository;

    @Transactional
    public Issue createIssue(Long spaceId, CreateIssueRequest request, User reporter) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space not found with id: " + spaceId));

        String issueKey = generateNextIssueKey(space);

        Issue issue = Issue.builder()
                .space(space)
                .issueKey(issueKey)
                .title(request.getTitle())
                .description(request.getDescription())
                .issueType(request.getIssueType() != null ? request.getIssueType() : IssueType.TASK)
                .priority(request.getPriority())
                .reporter(reporter)
                .startDate(request.getStartDate())
                .dueDate(request.getDueDate())
                .storyPoints(request.getStoryPoints())
                .build();

        if (request.getAssigneeId() != null) {
            User assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new RuntimeException("Assignee not found with id: " + request.getAssigneeId()));
            issue.setAssignee(assignee);
        }

        if (request.getParentId() != null) {
            Issue parent = issueRepository.findById(request.getParentId())
                    .orElseThrow(() -> new RuntimeException("Parent issue not found with id: " + request.getParentId()));
            issue.setParent(parent);
        }

        if (request.getEpicId() != null) {
            Issue epic = issueRepository.findById(request.getEpicId())
                    .orElseThrow(() -> new RuntimeException("Epic not found with id: " + request.getEpicId()));
            issue.setEpic(epic);
        }

        if (request.getSprintId() != null) {
            Sprint sprint = sprintRepository.findById(request.getSprintId())
                    .orElseThrow(() -> new RuntimeException("Sprint not found with id: " + request.getSprintId()));
            issue.setSprint(sprint);
        }

        if (request.getLabelIds() != null && !request.getLabelIds().isEmpty()) {
            Set<Label> labels = new HashSet<>(labelRepository.findAllById(request.getLabelIds()));
            issue.setLabels(labels);
        }

        return issueRepository.save(issue);
    }

    @Transactional(readOnly = true)
    public Issue getIssue(Long id) {
        return issueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Issue not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Issue getIssueByKey(String issueKey) {
        return issueRepository.findByIssueKeyWithDetails(issueKey)
                .orElseThrow(() -> new RuntimeException("Issue not found with key: " + issueKey));
    }

    @Transactional(readOnly = true)
    public List<Issue> getIssuesBySpace(Long spaceId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space not found with id: " + spaceId));
        return issueRepository.findBySpace(space);
    }

    @Transactional(readOnly = true)
    public List<Issue> getIssuesBySpaceWithAssignee(Long spaceId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space not found with id: " + spaceId));
        return issueRepository.findBySpaceWithAssignee(space);
    }

    @Transactional(readOnly = true)
    public List<Issue> getIssuesBySpaceAndStatus(Long spaceId, IssueStatus status) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space not found with id: " + spaceId));
        return issueRepository.findBySpaceAndStatus(space, status);
    }

    @Transactional(readOnly = true)
    public List<Issue> getIssuesBySpaceAndStatusWithAssignee(Long spaceId, IssueStatus status) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space not found with id: " + spaceId));
        return issueRepository.findBySpaceAndStatusWithAssignee(space, status);
    }

    @Transactional(readOnly = true)
    public List<Issue> getIssuesBySpaceAndSprint(Long spaceId, Long sprintId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space not found with id: " + spaceId));
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint not found with id: " + sprintId));
        return issueRepository.findBySpaceAndSprint(space, sprint);
    }

    @Transactional(readOnly = true)
    public List<Issue> getIssuesBySpaceAndSprintWithAssignee(Long spaceId, Long sprintId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space not found with id: " + spaceId));
        Sprint sprint = sprintRepository.findById(sprintId)
                .orElseThrow(() -> new RuntimeException("Sprint not found with id: " + sprintId));
        return issueRepository.findBySpaceAndSprintWithAssignee(space, sprint);
    }

    @Transactional(readOnly = true)
    public List<Issue> getIssuesBySpaceWithFilters(Long spaceId, IssueStatus status, IssueType type,
                                                    IssuePriority priority, Long assigneeId,
                                                    Long sprintId, String search) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space not found with id: " + spaceId));

        User assignee = null;
        if (assigneeId != null) {
            assignee = userRepository.findById(assigneeId)
                    .orElseThrow(() -> new RuntimeException("User not found with id: " + assigneeId));
        }

        String searchTerm = (search != null && !search.isBlank()) ? "%" + search.trim().toLowerCase() + "%" : null;

        return issueRepository.findBySpaceWithFilters(space, status, type, priority, assignee, sprintId, searchTerm);
    }

    @Transactional(readOnly = true)
    public List<Issue> getBacklogIssues(Long spaceId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space not found with id: " + spaceId));
        return issueRepository.findBySpaceAndSprintIsNull(space);
    }

    @Transactional(readOnly = true)
    public List<Issue> getBacklogIssuesWithAssignee(Long spaceId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space not found with id: " + spaceId));
        return issueRepository.findBySpaceAndSprintIsNullWithAssignee(space);
    }

    @Transactional
    public Issue updateIssue(Long id, UpdateIssueRequest request) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Issue not found with id: " + id));

        if (request.getTitle() != null) {
            issue.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            issue.setDescription(request.getDescription());
        }
        if (request.getIssueType() != null) {
            issue.setIssueType(request.getIssueType());
        }
        if (request.getStatus() != null) {
            issue.setStatus(request.getStatus());
        }
        if (request.getPriority() != null) {
            issue.setPriority(request.getPriority());
        }
        if (request.getAssigneeId() != null) {
            if (request.getAssigneeId() == 0L) {
                issue.setAssignee(null);
            } else {
                User assignee = userRepository.findById(request.getAssigneeId())
                        .orElseThrow(() -> new RuntimeException("Assignee not found with id: " + request.getAssigneeId()));
                issue.setAssignee(assignee);
            }
        }
        if (request.getSprintId() != null) {
            if (request.getSprintId() == 0L) {
                issue.setSprint(null);
            } else {
                Sprint sprint = sprintRepository.findById(request.getSprintId())
                        .orElseThrow(() -> new RuntimeException("Sprint not found with id: " + request.getSprintId()));
                issue.setSprint(sprint);
            }
        }
        if (request.getLabelIds() != null) {
            Set<Label> labels = new HashSet<>(labelRepository.findAllById(request.getLabelIds()));
            issue.setLabels(labels);
        }
        if (request.getStartDate() != null) {
            issue.setStartDate(request.getStartDate());
        }
        if (request.getDueDate() != null) {
            issue.setDueDate(request.getDueDate());
        }
        if (request.getStoryPoints() != null) {
            issue.setStoryPoints(request.getStoryPoints());
        }
        if (request.getEpicId() != null) {
            if (request.getEpicId() == 0L) {
                issue.setEpic(null);
            } else {
                Issue epic = issueRepository.findById(request.getEpicId())
                        .orElseThrow(() -> new RuntimeException("Epic not found with id: " + request.getEpicId()));
                issue.setEpic(epic);
            }
        }
        if (request.getOrderIndex() != null) {
            issue.setOrderIndex(request.getOrderIndex());
        }

        return issueRepository.save(issue);
    }

    @Transactional
    public Issue updateIssueDates(Long issueId, LocalDate startDate, LocalDate dueDate) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found with id: " + issueId));
        issue.setStartDate(startDate);
        issue.setDueDate(dueDate);
        return issueRepository.save(issue);
    }

    @Transactional
    public Issue updateIssueStatus(Long id, IssueStatus status) {
        Issue issue = issueRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Issue not found with id: " + id));
        issue.setStatus(status);
        return issueRepository.save(issue);
    }

    @Transactional
    public IssueComment addComment(Long issueId, CreateCommentRequest request, User author) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found with id: " + issueId));

        IssueComment comment = IssueComment.builder()
                .issue(issue)
                .author(author)
                .content(request.getContent())
                .build();

        return issueCommentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public List<IssueComment> getComments(Long issueId) {
        Issue issue = issueRepository.findById(issueId)
                .orElseThrow(() -> new RuntimeException("Issue not found with id: " + issueId));
        return issueCommentRepository.findByIssueOrderByCreatedAtAsc(issue);
    }

    @Transactional(readOnly = true)
    public Map<IssueStatus, Long> getIssueCountByStatus(Long spaceId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space not found with id: " + spaceId));

        return Arrays.stream(IssueStatus.values())
                .collect(Collectors.toMap(
                        status -> status,
                        status -> issueRepository.countBySpaceAndStatus(space, status)
                ));
    }

    private String generateNextIssueKey(Space space) {
        Optional<Issue> lastIssue = issueRepository.findTopBySpaceOrderByIssueKeyDesc(space);

        int nextNumber = 1;
        if (lastIssue.isPresent()) {
            String lastKey = lastIssue.get().getIssueKey();
            String numberPart = lastKey.substring(lastKey.lastIndexOf('-') + 1);
            nextNumber = Integer.parseInt(numberPart) + 1;
        }

        return space.getSpaceKey() + "-" + nextNumber;
    }
}
