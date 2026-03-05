package com.gitnx.mergerequest.repository;

import com.gitnx.mergerequest.entity.MergeRequestReviewer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MergeRequestReviewerJpaRepository extends JpaRepository<MergeRequestReviewer, Long> {

    Optional<MergeRequestReviewer> findByMergeRequestIdAndReviewerId(Long mergeRequestId, Long reviewerId);
}
