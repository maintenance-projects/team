package com.spacenx.issue.dto;

import com.spacenx.issue.enums.IssuePriority;
import com.spacenx.issue.enums.IssueType;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class CreateIssueRequest {

    @NotBlank
    private String title;

    private String description;

    private IssueType issueType;

    private IssuePriority priority;

    private Long assigneeId;

    private Long parentId;

    private Long epicId;

    private Long sprintId;

    private List<Long> labelIds;

    private LocalDate startDate;

    private LocalDate dueDate;

    private Integer storyPoints;
}
