package com.spacenx.space.repository;

import com.spacenx.space.entity.Space;
import com.spacenx.space.entity.SpaceMember;
import com.spacenx.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SpaceMemberRepository extends JpaRepository<SpaceMember, Long> {
    List<SpaceMember> findBySpace(Space space);
    List<SpaceMember> findByUser(User user);
    Optional<SpaceMember> findBySpaceAndUser(Space space, User user);
    boolean existsBySpaceAndUser(Space space, User user);

    @Query("SELECT sm FROM SpaceMember sm JOIN FETCH sm.user WHERE sm.space = :space")
    List<SpaceMember> findBySpaceWithUser(Space space);
}
