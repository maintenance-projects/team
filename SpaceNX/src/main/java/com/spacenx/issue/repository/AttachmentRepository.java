package com.spacenx.issue.repository;

import com.spacenx.issue.entity.Attachment;
import com.spacenx.issue.entity.Issue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    List<Attachment> findByIssue(Issue issue);
    List<Attachment> findByIssueOrderByCreatedAtDesc(Issue issue);
}
