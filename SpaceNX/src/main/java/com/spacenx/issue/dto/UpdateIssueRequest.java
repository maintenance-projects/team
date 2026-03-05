package com.spacenx.issue.dto;

import com.spacenx.issue.enums.IssuePriority;
import com.spacenx.issue.enums.IssueStatus;
import com.spacenx.issue.enums.IssueType;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class UpdateIssueRequest {

    private String title;

    private String description;

    private IssueType issueType;

    private IssueStatus status;

    private IssuePriority priority;

    private Long assigneeId;

    private Long sprintId;

    private List<Long> labelIds;

    private LocalDate startDate;

    private LocalDate dueDate;

    private Integer storyPoints;

    private Long epicId;

    private Integer orderIndex;
}
