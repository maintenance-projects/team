package com.spacenx.shortcut.service;

import com.spacenx.shortcut.entity.Shortcut;
import com.spacenx.shortcut.repository.ShortcutRepository;
import com.spacenx.space.entity.Space;
import com.spacenx.space.repository.SpaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ShortcutService {

    private final ShortcutRepository shortcutRepository;
    private final SpaceRepository spaceRepository;

    @Transactional
    public Shortcut createShortcut(Long spaceId, String name, String url, String description) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space not found with id: " + spaceId));

        Shortcut shortcut = Shortcut.builder()
                .space(space)
                .name(name)
                .url(url)
                .description(description)
                .build();

        return shortcutRepository.save(shortcut);
    }

    @Transactional(readOnly = true)
    public List<Shortcut> getShortcuts(Long spaceId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space not found with id: " + spaceId));
        return shortcutRepository.findBySpace(space);
    }

    @Transactional
    public void deleteShortcut(Long id) {
        if (!shortcutRepository.existsById(id)) {
            throw new RuntimeException("Shortcut not found with id: " + id);
        }
        shortcutRepository.deleteById(id);
    }
}
