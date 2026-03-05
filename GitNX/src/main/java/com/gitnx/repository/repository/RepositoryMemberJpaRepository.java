package com.gitnx.repository.repository;

import com.gitnx.repository.entity.RepositoryMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RepositoryMemberJpaRepository extends JpaRepository<RepositoryMember, Long> {

    List<RepositoryMember> findByGitRepositoryId(Long repositoryId);

    @Query("SELECT m FROM RepositoryMember m JOIN FETCH m.user WHERE m.gitRepository.id = :repoId")
    List<RepositoryMember> findByGitRepositoryIdWithUser(@Param("repoId") Long repositoryId);

    Optional<RepositoryMember> findByGitRepositoryIdAndUserId(Long repositoryId, Long userId);

    boolean existsByGitRepositoryIdAndUserId(Long repositoryId, Long userId);

    Optional<RepositoryMember> findByGitRepositoryIdAndUserUsername(Long repositoryId, String username);

    boolean existsByGitRepositoryIdAndUserUsername(Long repositoryId, String username);

    @Query("SELECT m FROM RepositoryMember m JOIN FETCH m.gitRepository g JOIN FETCH g.owner WHERE m.user.username = :username ORDER BY g.createdAt DESC")
    List<RepositoryMember> findByUserUsernameWithRepository(@Param("username") String username);
}
