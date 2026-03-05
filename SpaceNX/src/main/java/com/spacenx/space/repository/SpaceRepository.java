package com.spacenx.space.repository;

import com.spacenx.space.entity.Space;
import com.spacenx.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SpaceRepository extends JpaRepository<Space, Long> {
    List<Space> findByOwner(User owner);
    Optional<Space> findBySpaceKey(String spaceKey);
    boolean existsBySpaceKey(String spaceKey);

    @Query("SELECT s FROM Space s JOIN SpaceMember sm ON sm.space = s WHERE sm.user = :user")
    List<Space> findByMemberUser(User user);
}
