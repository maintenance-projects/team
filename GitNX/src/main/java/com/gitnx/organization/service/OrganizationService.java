package com.gitnx.organization.service;

import com.gitnx.common.exception.ResourceNotFoundException;
import com.gitnx.organization.entity.Organization;
import com.gitnx.organization.entity.OrganizationMember;
import com.gitnx.organization.repository.OrganizationJpaRepository;
import com.gitnx.organization.repository.OrganizationMemberJpaRepository;
import com.gitnx.repository.enums.RepositoryRole;
import com.gitnx.repository.service.GitRepositoryService;
import com.gitnx.user.entity.User;
import com.gitnx.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrganizationService {

    private final OrganizationJpaRepository orgRepository;
    private final OrganizationMemberJpaRepository memberRepository;
    private final UserService userService;
    private final GitRepositoryService gitRepositoryService;

    @Transactional
    public Organization create(String name, String description, String ownerUsername) {
        if (orgRepository.existsByName(name)) {
            throw new IllegalArgumentException("Organization already exists: " + name);
        }

        User owner = userService.getByUsername(ownerUsername);

        Organization org = Organization.builder()
                .name(name)
                .description(description)
                .owner(owner)
                .build();
        org = orgRepository.save(org);

        OrganizationMember member = OrganizationMember.builder()
                .organization(org)
                .user(owner)
                .role(RepositoryRole.OWNER)
                .build();
        memberRepository.save(member);

        return org;
    }

    public List<Organization> listAll() {
        return orgRepository.findAll();
    }

    public Organization getByName(String name) {
        return orgRepository.findByName(name)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found: " + name));
    }

    public List<Organization> listByUser(String username) {
        return memberRepository.findByUserUsernameWithOrganization(username).stream()
                .map(OrganizationMember::getOrganization)
                .toList();
    }

    public List<OrganizationMember> getMembers(String orgName) {
        Organization org = getByName(orgName);
        return memberRepository.findByOrganizationIdWithUser(org.getId());
    }

    @Transactional
    public void addMember(String orgName, String targetUsername, RepositoryRole role, String currentUsername) {
        Organization org = getByName(orgName);
        verifyOwner(org, currentUsername);

        User targetUser = userService.findOrCreateFromWorkbench(targetUsername);

        if (memberRepository.existsByOrganizationIdAndUserId(org.getId(), targetUser.getId())) {
            throw new IllegalArgumentException("User '" + targetUsername + "' is already a member");
        }

        OrganizationMember member = OrganizationMember.builder()
                .organization(org)
                .user(targetUser)
                .role(role)
                .build();
        memberRepository.save(member);
        log.info("Added member '{}' with role {} to organization '{}'", targetUsername, role, orgName);
    }

    @Transactional
    public void removeMember(String orgName, Long memberId, String currentUsername) {
        Organization org = getByName(orgName);
        verifyOwner(org, currentUsername);

        OrganizationMember member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        if (member.getRole() == RepositoryRole.OWNER && member.getUser().getUsername().equals(org.getOwner().getUsername())) {
            throw new IllegalArgumentException("Cannot remove the organization owner");
        }

        memberRepository.delete(member);
        log.info("Removed member from organization '{}'", orgName);
    }

    public boolean isOwner(String orgName, String username) {
        Organization org = getByName(orgName);
        User user = userService.findByUsername(username).orElse(null);
        if (user == null) return false;

        Optional<OrganizationMember> member =
                memberRepository.findByOrganizationIdAndUserId(org.getId(), user.getId());
        return member.isPresent() && member.get().getRole() == RepositoryRole.OWNER;
    }

    public boolean isMember(String orgName, String username) {
        Organization org = getByName(orgName);
        User user = userService.findByUsername(username).orElse(null);
        if (user == null) return false;
        return memberRepository.existsByOrganizationIdAndUserId(org.getId(), user.getId());
    }

    @Transactional
    public void delete(String orgName, String currentUsername) {
        Organization org = getByName(orgName);
        verifyOwner(org, currentUsername);

        gitRepositoryService.deleteAllByOrganization(org.getId());
        memberRepository.deleteAll(memberRepository.findByOrganizationIdWithUser(org.getId()));
        orgRepository.delete(org);
        log.info("Deleted organization '{}'", orgName);
    }

    private void verifyOwner(Organization org, String username) {
        User user = userService.getByUsername(username);
        OrganizationMember member = memberRepository
                .findByOrganizationIdAndUserId(org.getId(), user.getId())
                .orElseThrow(() -> new AccessDeniedException("Not a member of this organization"));
        if (member.getRole() != RepositoryRole.OWNER) {
            throw new AccessDeniedException("Only the organization owner can manage members");
        }
    }
}
