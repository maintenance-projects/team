package com.spacenx.issue.entity;

import com.spacenx.common.entity.BaseTimeEntity;
import com.spacenx.issue.enums.IssuePriority;
import com.spacenx.issue.enums.IssueStatus;
import com.spacenx.issue.enums.IssueType;
import com.spacenx.space.entity.Space;
import com.spacenx.sprint.entity.Sprint;
import com.spacenx.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "snx_issue")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Issue extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "space_id")
    private Space space;

    @Column(nullable = false, unique = true)
    private String issueKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IssueType issueType;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private IssueStatus status = IssueStatus.TODO;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private IssuePriority priority = IssuePriority.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Issue parent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "epic_id")
    private Issue epic;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;

    @ManyToMany
    @JoinTable(
            name = "snx_issue_label",
            joinColumns = @JoinColumn(name = "issue_id"),
            inverseJoinColumns = @JoinColumn(name = "label_id")
    )
    @Builder.Default
    private Set<Label> labels = new HashSet<>();

    private LocalDate startDate;

    private LocalDate dueDate;

    private Integer storyPoints;

    @Builder.Default
    private Integer orderIndex = 0;

    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Attachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "issue", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<IssueComment> comments = new ArrayList<>();

    public boolean isOverdue() {
        return dueDate != null && status != IssueStatus.DONE && dueDate.isBefore(LocalDate.now());
    }
}
