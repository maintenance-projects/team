package com.spacenx.issue.repository;

import com.spacenx.issue.entity.Label;
import com.spacenx.space.entity.Space;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LabelRepository extends JpaRepository<Label, Long> {
    List<Label> findBySpace(Space space);
}
