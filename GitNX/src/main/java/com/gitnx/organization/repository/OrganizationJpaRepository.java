package com.gitnx.organization.repository;

import com.gitnx.organization.entity.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface OrganizationJpaRepository extends JpaRepository<Organization, Long> {

    @Query("SELECT o FROM Organization o JOIN FETCH o.owner WHERE o.name = :name")
    Optional<Organization> findByName(String name);

    @Query("SELECT o FROM Organization o JOIN FETCH o.owner")
    java.util.List<Organization> findAllWithOwner();

    boolean existsByName(String name);
}
