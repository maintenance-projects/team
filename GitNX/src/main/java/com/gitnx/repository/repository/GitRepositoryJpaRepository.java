package com.gitnx.repository.repository;

import com.gitnx.repository.entity.GitRepository;
import com.gitnx.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GitRepositoryJpaRepository extends JpaRepository<GitRepository, Long> {

    List<GitRepository> findByOwnerOrderByCreatedAtDesc(User owner);

    Optional<GitRepository> findByOwnerUsernameAndName(String ownerUsername, String name);

    Optional<GitRepository> findByOwnerUsernameAndNameAndOrganizationIsNull(String ownerUsername, String name);

    Optional<GitRepository> findByOwnerUsernameAndNameAndOrganizationId(String ownerUsername, String name, Long organizationId);

    boolean existsByOwnerAndName(User owner, String name);

    boolean existsByOwnerAndNameAndOrganizationId(User owner, String name, Long organizationId);

    boolean existsByOwnerAndNameAndOrganizationIsNull(User owner, String name);

    Optional<GitRepository> findFirstByName(String name);

    List<GitRepository> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId);
}
