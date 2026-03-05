package com.gitnx.mergerequest.entity;

import com.gitnx.common.entity.BaseTimeEntity;
import com.gitnx.mergerequest.enums.MergeRequestState;
import com.gitnx.repository.entity.GitRepository;
import com.gitnx.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "ng_merge_request", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"repository_id", "mr_number"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MergeRequest extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mr_number", nullable = false)
    private Integer mrNumber;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    @Builder.Default
    private MergeRequestState state = MergeRequestState.OPEN;

    @Column(nullable = false, length = 100)
    private String sourceBranch;

    @Column(nullable = false, length = 100)
    private String targetBranch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "repository_id", nullable = false)
    private GitRepository gitRepository;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    private LocalDateTime mergedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merged_by_id")
    private User mergedBy;

    @OneToMany(mappedBy = "mergeRequest", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<ReviewComment> reviewComments = new ArrayList<>();

    @OneToMany(mappedBy = "mergeRequest", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MergeRequestReviewer> reviewers = new ArrayList<>();
}
