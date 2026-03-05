package com.gitnx.mergerequest.repository;

import com.gitnx.mergerequest.entity.ReviewComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewCommentJpaRepository extends JpaRepository<ReviewComment, Long> {
}
