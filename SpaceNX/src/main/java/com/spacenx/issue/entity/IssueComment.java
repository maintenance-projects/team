package com.spacenx.issue.entity;

import com.spacenx.common.entity.BaseTimeEntity;
import com.spacenx.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "snx_issue_comment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IssueComment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id")
    private Issue issue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private User author;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;
}
