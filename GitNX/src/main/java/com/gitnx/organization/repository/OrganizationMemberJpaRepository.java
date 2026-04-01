package com.gitnx.organization.repository;

import com.gitnx.organization.entity.OrganizationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface OrganizationMemberJpaRepository extends JpaRepository<OrganizationMember, Long> {

    @Query("SELECT m FROM OrganizationMember m JOIN FETCH m.user WHERE m.organization.id = :orgId")
    List<OrganizationMember> findByOrganizationIdWithUser(Long orgId);

    @Query("SELECT m FROM OrganizationMember m JOIN FETCH m.organization WHERE m.user.username = :username")
    List<OrganizationMember> findByUserUsernameWithOrganization(String username);

    Optional<OrganizationMember> findByOrganizationIdAndUserId(Long orgId, Long userId);

    boolean existsByOrganizationIdAndUserId(Long orgId, Long userId);
}
