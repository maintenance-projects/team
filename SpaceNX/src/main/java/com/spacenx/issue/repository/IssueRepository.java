package com.spacenx.issue.repository;

import com.spacenx.issue.entity.Issue;
import com.spacenx.issue.enums.IssuePriority;
import com.spacenx.issue.enums.IssueStatus;
import com.spacenx.issue.enums.IssueType;
import com.spacenx.space.entity.Space;
import com.spacenx.sprint.entity.Sprint;
import com.spacenx.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IssueRepository extends JpaRepository<Issue, Long> {
    List<Issue> findBySpace(Space space);

    @Query("SELECT i FROM Issue i LEFT JOIN FETCH i.assignee WHERE i.space = :space")
    List<Issue> findBySpaceWithAssignee(@Param("space") Space space);
    List<Issue> findBySpaceAndStatus(Space space, IssueStatus status);
    List<Issue> findBySpaceAndSprint(Space space, Sprint sprint);

    @Query("SELECT i FROM Issue i LEFT JOIN FETCH i.assignee WHERE i.space = :space AND i.status = :status")
    List<Issue> findBySpaceAndStatusWithAssignee(@Param("space") Space space, @Param("status") IssueStatus status);

    @Query("SELECT i FROM Issue i LEFT JOIN FETCH i.assignee WHERE i.space = :space AND i.sprint = :sprint")
    List<Issue> findBySpaceAndSprintWithAssignee(@Param("space") Space space, @Param("sprint") Sprint sprint);
    List<Issue> findBySpaceAndSprintIsNull(Space space);

    @Query("SELECT i FROM Issue i LEFT JOIN FETCH i.assignee WHERE i.space = :space AND i.sprint IS NULL")
    List<Issue> findBySpaceAndSprintIsNullWithAssignee(@Param("space") Space space);
    List<Issue> findByAssignee(User assignee);
    long countBySpaceAndStatus(Space space, IssueStatus status);
    List<Issue> findBySpaceOrderByOrderIndexAsc(Space space);
    Optional<Issue> findTopBySpaceOrderByIssueKeyDesc(Space space);
    Optional<Issue> findByIssueKey(String issueKey);

    @Query("SELECT i FROM Issue i" +
           " LEFT JOIN FETCH i.assignee" +
           " LEFT JOIN FETCH i.reporter" +
           " LEFT JOIN FETCH i.sprint" +
           " LEFT JOIN FETCH i.epic" +
           " LEFT JOIN FETCH i.labels" +
           " WHERE i.issueKey = :issueKey")
    Optional<Issue> findByIssueKeyWithDetails(@Param("issueKey") String issueKey);

    @Query("SELECT i FROM Issue i LEFT JOIN FETCH i.assignee WHERE i.space = :space" +
           " AND (:status IS NULL OR i.status = :status)" +
           " AND (:type IS NULL OR i.issueType = :type)" +
           " AND (:priority IS NULL OR i.priority = :priority)" +
           " AND (:assignee IS NULL OR i.assignee = :assignee)" +
           " AND (:sprintId IS NULL OR i.sprint.id = :sprintId)" +
           " AND (:search IS NULL OR LOWER(i.title) LIKE :search)" +
           " ORDER BY i.createdAt DESC")
    List<Issue> findBySpaceWithFilters(@Param("space") Space space,
                                       @Param("status") IssueStatus status,
                                       @Param("type") IssueType type,
                                       @Param("priority") IssuePriority priority,
                                       @Param("assignee") User assignee,
                                       @Param("sprintId") Long sprintId,
                                       @Param("search") String search);
}
