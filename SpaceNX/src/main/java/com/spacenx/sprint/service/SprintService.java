package com.spacenx.sprint.service;

import com.spacenx.space.entity.Space;
import com.spacenx.space.repository.SpaceRepository;
import com.spacenx.sprint.dto.CreateSprintRequest;
import com.spacenx.sprint.entity.Sprint;
import com.spacenx.sprint.enums.SprintStatus;
import com.spacenx.sprint.repository.SprintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SprintService {

    private final SprintRepository sprintRepository;
    private final SpaceRepository spaceRepository;

    @Transactional
    public Sprint createSprint(Long spaceId, CreateSprintRequest request) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space not found with id: " + spaceId));

        Sprint sprint = Sprint.builder()
                .space(space)
                .name(request.getName())
                .goal(request.getGoal())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .status(SprintStatus.PLANNED)
                .build();

        return sprintRepository.save(sprint);
    }

    @Transactional(readOnly = true)
    public Sprint getSprint(Long id) {
        return sprintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sprint not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public List<Sprint> getSprintsBySpace(Long spaceId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space not found with id: " + spaceId));
        return sprintRepository.findBySpace(space);
    }

    @Transactional(readOnly = true)
    public List<Sprint> getSprintsBySpaceWithIssues(Long spaceId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space not found with id: " + spaceId));
        return sprintRepository.findBySpaceWithIssues(space);
    }

    @Transactional(readOnly = true)
    public Optional<Sprint> getActiveSprint(Long spaceId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space not found with id: " + spaceId));
        return sprintRepository.findBySpaceAndStatus(space, SprintStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Optional<Sprint> getActiveSprintWithIssues(Long spaceId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space not found with id: " + spaceId));
        return sprintRepository.findBySpaceAndStatusWithIssues(space, SprintStatus.ACTIVE);
    }

    @Transactional
    public Sprint startSprint(Long id) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sprint not found with id: " + id));

        if (sprint.getStatus() != SprintStatus.PLANNED) {
            throw new RuntimeException("Only planned sprints can be started");
        }

        sprint.setStatus(SprintStatus.ACTIVE);
        if (sprint.getStartDate() == null) {
            sprint.setStartDate(LocalDate.now());
        }

        return sprintRepository.save(sprint);
    }

    @Transactional(readOnly = true)
    public List<Sprint> getCompletedSprints(Long spaceId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space not found with id: " + spaceId));
        return sprintRepository.findBySpaceAndStatusOrderByEndDateAsc(space, SprintStatus.COMPLETED);
    }

    @Transactional
    public Sprint completeSprint(Long id) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sprint not found with id: " + id));

        if (sprint.getStatus() != SprintStatus.ACTIVE) {
            throw new RuntimeException("Only active sprints can be completed");
        }

        sprint.setStatus(SprintStatus.COMPLETED);

        return sprintRepository.save(sprint);
    }
}
