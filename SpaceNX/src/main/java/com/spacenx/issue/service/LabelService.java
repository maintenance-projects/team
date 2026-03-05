package com.spacenx.issue.service;

import com.spacenx.issue.entity.Label;
import com.spacenx.issue.repository.LabelRepository;
import com.spacenx.space.entity.Space;
import com.spacenx.space.repository.SpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LabelService {

    private final LabelRepository labelRepository;
    private final SpaceRepository spaceRepository;

    @Transactional
    public Label createLabel(Long spaceId, String name, String color) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space not found with id: " + spaceId));

        Label label = Label.builder()
                .space(space)
                .name(name)
                .color(color)
                .build();

        return labelRepository.save(label);
    }

    @Transactional(readOnly = true)
    public List<Label> getLabels(Long spaceId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space not found with id: " + spaceId));
        return labelRepository.findBySpace(space);
    }

    @Transactional
    public void deleteLabel(Long id) {
        if (!labelRepository.existsById(id)) {
            throw new RuntimeException("Label not found with id: " + id);
        }
        labelRepository.deleteById(id);
    }
}
