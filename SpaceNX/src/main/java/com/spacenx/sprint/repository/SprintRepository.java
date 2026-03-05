package com.spacenx.sprint.repository;

import com.spacenx.space.entity.Space;
import com.spacenx.sprint.entity.Sprint;
import com.spacenx.sprint.enums.SprintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SprintRepository extends JpaRepository<Sprint, Long> {
    List<Sprint> findBySpace(Space space);
    Optional<Sprint> findBySpaceAndStatus(Space space, SprintStatus status);
    List<Sprint> findBySpaceAndStatusNot(Space space, SprintStatus status);
    List<Sprint> findBySpaceAndStatusOrderByEndDateAsc(Space space, SprintStatus status);

    @Query("SELECT DISTINCT s FROM Sprint s LEFT JOIN FETCH s.issues i LEFT JOIN FETCH i.assignee WHERE s.space = :space")
    List<Sprint> findBySpaceWithIssues(@Param("space") Space space);

    @Query("SELECT s FROM Sprint s LEFT JOIN FETCH s.issues i LEFT JOIN FETCH i.assignee WHERE s.space = :space AND s.status = :status")
    Optional<Sprint> findBySpaceAndStatusWithIssues(@Param("space") Space space, @Param("status") SprintStatus status);
}
