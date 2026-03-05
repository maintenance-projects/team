package com.gitnx.issue.repository;

import com.gitnx.issue.entity.Issue;
import com.gitnx.issue.enums.IssueState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface IssueJpaRepository extends JpaRepository<Issue, Long> {

    Page<Issue> findByGitRepositoryIdAndState(Long repositoryId, IssueState state, Pageable pageable);

    Page<Issue> findByGitRepositoryId(Long repositoryId, Pageable pageable);

    Optional<Issue> findByGitRepositoryIdAndIssueNumber(Long repositoryId, Integer issueNumber);

    @Query("SELECT COALESCE(MAX(i.issueNumber), 0) FROM Issue i WHERE i.gitRepository.id = :repoId")
    int findMaxIssueNumber(Long repoId);

    long countByGitRepositoryIdAndState(Long repositoryId, IssueState state);
}
