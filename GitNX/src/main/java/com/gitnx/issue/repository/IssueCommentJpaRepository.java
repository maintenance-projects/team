package com.gitnx.issue.repository;

import com.gitnx.issue.entity.IssueComment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IssueCommentJpaRepository extends JpaRepository<IssueComment, Long> {
}
