package com.spacenx.issue.repository;

import com.spacenx.issue.entity.Issue;
import com.spacenx.issue.entity.IssueComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface IssueCommentRepository extends JpaRepository<IssueComment, Long> {
    @Query("SELECT c FROM IssueComment c JOIN FETCH c.author WHERE c.issue = :issue ORDER BY c.createdAt ASC")
    List<IssueComment> findByIssueOrderByCreatedAtAsc(@Param("issue") Issue issue);
}
