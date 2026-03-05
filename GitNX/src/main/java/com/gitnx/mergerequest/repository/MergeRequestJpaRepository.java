package com.gitnx.mergerequest.repository;

import com.gitnx.mergerequest.entity.MergeRequest;
import com.gitnx.mergerequest.enums.MergeRequestState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MergeRequestJpaRepository extends JpaRepository<MergeRequest, Long> {

    Page<MergeRequest> findByGitRepositoryId(Long repositoryId, Pageable pageable);

    Page<MergeRequest> findByGitRepositoryIdAndState(Long repositoryId, MergeRequestState state, Pageable pageable);

    Optional<MergeRequest> findByGitRepositoryIdAndMrNumber(Long repositoryId, Integer mrNumber);

    @Query("SELECT COALESCE(MAX(mr.mrNumber), 0) FROM MergeRequest mr WHERE mr.gitRepository.id = :repoId")
    int findMaxMrNumber(@Param("repoId") Long repoId);

    long countByGitRepositoryIdAndState(Long repositoryId, MergeRequestState state);
}
