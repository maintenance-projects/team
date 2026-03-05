package com.gitnx.mergerequest.entity;

import com.gitnx.common.entity.BaseTimeEntity;
import com.gitnx.mergerequest.enums.ReviewState;
import com.gitnx.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "ng_merge_request_reviewer", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"merge_request_id", "reviewer_id"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MergeRequestReviewer extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merge_request_id", nullable = false)
    private MergeRequest mergeRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id", nullable = false)
    private User reviewer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReviewState state = ReviewState.PENDING;
}
