package com.gitnx.issue.repository;

import com.gitnx.issue.entity.Milestone;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MilestoneJpaRepository extends JpaRepository<Milestone, Long> {

    List<Milestone> findByGitRepositoryId(Long repositoryId);
}
