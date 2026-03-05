package com.gitnx.issue.repository;

import com.gitnx.issue.entity.Label;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LabelJpaRepository extends JpaRepository<Label, Long> {

    List<Label> findByGitRepositoryId(Long repositoryId);

    Optional<Label> findByGitRepositoryIdAndName(Long repositoryId, String name);

    boolean existsByGitRepositoryIdAndName(Long repositoryId, String name);
}
