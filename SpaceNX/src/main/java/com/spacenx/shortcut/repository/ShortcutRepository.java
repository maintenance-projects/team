package com.spacenx.shortcut.repository;

import com.spacenx.shortcut.entity.Shortcut;
import com.spacenx.space.entity.Space;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ShortcutRepository extends JpaRepository<Shortcut, Long> {
    List<Shortcut> findBySpace(Space space);
}
