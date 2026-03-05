package com.spacenx.space.service;

import com.spacenx.form.repository.FormTemplateRepository;
import com.spacenx.form.service.FormService;
import com.spacenx.issue.repository.IssueRepository;
import com.spacenx.issue.repository.LabelRepository;
import com.spacenx.shortcut.repository.ShortcutRepository;
import com.spacenx.space.dto.CreateSpaceRequest;
import com.spacenx.space.entity.Space;
import com.spacenx.space.entity.SpaceMember;
import com.spacenx.space.enums.MemberRole;
import com.spacenx.space.repository.SpaceMemberRepository;
import com.spacenx.space.repository.SpaceRepository;
import com.spacenx.sprint.repository.SprintRepository;
import com.spacenx.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SpaceService {

    private final SpaceRepository spaceRepository;
    private final SpaceMemberRepository spaceMemberRepository;
    private final IssueRepository issueRepository;
    private final SprintRepository sprintRepository;
    private final LabelRepository labelRepository;
    private final ShortcutRepository shortcutRepository;
    private final FormTemplateRepository formTemplateRepository;
    private final FormService formService;

    @Transactional
    public Space createSpace(CreateSpaceRequest request, User owner) {
        if (spaceRepository.existsBySpaceKey(request.getSpaceKey())) {
            throw new RuntimeException("Space key already exists: " + request.getSpaceKey());
        }

        Space space = Space.builder()
                .name(request.getName())
                .spaceKey(request.getSpaceKey())
                .description(request.getDescription())
                .spaceType(request.getSpaceType())
                .owner(owner)
                .build();

        space = spaceRepository.save(space);

        addMember(space, owner, MemberRole.ADMIN);

        return space;
    }

    @Transactional(readOnly = true)
    public Space getSpace(Long id) {
        return spaceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Space not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Space getSpaceByKey(String key) {
        return spaceRepository.findBySpaceKey(key)
                .orElseThrow(() -> new RuntimeException("Space not found with key: " + key));
    }

    @Transactional(readOnly = true)
    public List<Space> getSpacesForUser(User user) {
        return spaceRepository.findByMemberUser(user);
    }

    @Transactional
    public SpaceMember addMember(Space space, User user, MemberRole role) {
        if (spaceMemberRepository.existsBySpaceAndUser(space, user)) {
            throw new RuntimeException("User is already a member of this space");
        }

        SpaceMember member = SpaceMember.builder()
                .space(space)
                .user(user)
                .role(role)
                .build();

        return spaceMemberRepository.save(member);
    }

    @Transactional
    public void removeMember(Long memberId) {
        spaceMemberRepository.deleteById(memberId);
    }

    @Transactional(readOnly = true)
    public List<SpaceMember> getMembers(Space space) {
        return spaceMemberRepository.findBySpaceWithUser(space);
    }

    @Transactional(readOnly = true)
    public Optional<MemberRole> getMemberRole(Space space, User user) {
        return spaceMemberRepository.findBySpaceAndUser(space, user)
                .map(SpaceMember::getRole);
    }

    @Transactional
    public void deleteSpace(Space space) {
        // Delete form templates and their files
        formTemplateRepository.findBySpace(space).forEach(template -> {
            formService.deleteFormTemplateFile(template);
        });
        formTemplateRepository.deleteAll(formTemplateRepository.findBySpace(space));

        // Delete issues (cascades to attachments, comments)
        issueRepository.deleteAll(issueRepository.findBySpace(space));

        // Delete sprints
        sprintRepository.deleteAll(sprintRepository.findBySpace(space));

        // Delete labels, shortcuts, members
        labelRepository.deleteAll(labelRepository.findBySpace(space));
        shortcutRepository.deleteAll(shortcutRepository.findBySpace(space));
        spaceMemberRepository.deleteAll(spaceMemberRepository.findBySpace(space));

        // Delete the space itself
        spaceRepository.delete(space);
    }
}
