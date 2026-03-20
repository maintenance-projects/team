package com.gitnx.repository.service;

import com.gitnx.repository.entity.GitRepository;
import com.gitnx.repository.entity.RepositoryMember;
import com.gitnx.repository.enums.RepositoryRole;
import com.gitnx.repository.repository.RepositoryMemberJpaRepository;
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
public class RepositoryMemberService {

    private final RepositoryMemberJpaRepository memberJpaRepository;
    private final GitRepositoryService gitRepositoryService;
    private final UserService userService;

    public List<RepositoryMember> getMembers(String ownerUsername, String repoName) {
        GitRepository repo = gitRepositoryService.getByOwnerAndName(ownerUsername, repoName);
        return memberJpaRepository.findByGitRepositoryIdWithUser(repo.getId());
    }

    @Transactional
    public void addMember(String ownerUsername, String repoName,
                          String targetUsername, RepositoryRole role, String currentUsername) {
        GitRepository repo = gitRepositoryService.getByOwnerAndName(ownerUsername, repoName);
        verifyOwner(repo, currentUsername);

        if (role == RepositoryRole.OWNER) {
            throw new IllegalArgumentException("Cannot assign OWNER role");
        }

        User targetUser = userService.findOrCreateFromWorkbench(targetUsername);

        if (memberJpaRepository.existsByGitRepositoryIdAndUserId(repo.getId(), targetUser.getId())) {
            throw new IllegalArgumentException("User '" + targetUsername + "' is already a member");
        }

        RepositoryMember member = RepositoryMember.builder()
                .gitRepository(repo)
                .user(targetUser)
                .role(role)
                .build();

        memberJpaRepository.save(member);
        log.info("Added member '{}' with role {} to repository '{}/{}'",
                targetUsername, role, ownerUsername, repoName);
    }

    @Transactional
    public void changeRole(String ownerUsername, String repoName,
                           Long memberId, RepositoryRole newRole, String currentUsername) {
        GitRepository repo = gitRepositoryService.getByOwnerAndName(ownerUsername, repoName);
        verifyOwner(repo, currentUsername);

        if (newRole == RepositoryRole.OWNER) {
            throw new IllegalArgumentException("Cannot assign OWNER role");
        }

        RepositoryMember member = memberJpaRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        if (member.getRole() == RepositoryRole.OWNER) {
            throw new IllegalArgumentException("Cannot change the owner's role");
        }

        member.setRole(newRole);
        memberJpaRepository.save(member);
        log.info("Changed role of member '{}' to {} in repository '{}/{}'",
                member.getUser().getUsername(), newRole, ownerUsername, repoName);
    }

    @Transactional
    public void removeMember(String ownerUsername, String repoName,
                             Long memberId, String currentUsername) {
        GitRepository repo = gitRepositoryService.getByOwnerAndName(ownerUsername, repoName);
        verifyOwner(repo, currentUsername);

        RepositoryMember member = memberJpaRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));

        if (member.getRole() == RepositoryRole.OWNER) {
            throw new IllegalArgumentException("Cannot remove the repository owner");
        }

        memberJpaRepository.delete(member);
        log.info("Removed member '{}' from repository '{}/{}'",
                member.getUser().getUsername(), ownerUsername, repoName);
    }

    public boolean isOwner(String ownerUsername, String repoName, String username) {
        GitRepository repo = gitRepositoryService.getByOwnerAndName(ownerUsername, repoName);
        User user = userService.findByUsername(username).orElse(null);
        if (user == null) return false;

        Optional<RepositoryMember> member =
                memberJpaRepository.findByGitRepositoryIdAndUserId(repo.getId(), user.getId());
        return member.isPresent() && member.get().getRole() == RepositoryRole.OWNER;
    }

    public boolean canPush(Long repositoryId, String username) {
        Optional<RepositoryMember> member =
                memberJpaRepository.findByGitRepositoryIdAndUserUsername(repositoryId, username);
        if (member.isEmpty()) return false;

        RepositoryRole role = member.get().getRole();
        return role == RepositoryRole.OWNER
                || role == RepositoryRole.MAINTAINER
                || role == RepositoryRole.DEVELOPER;
    }

    public boolean isMember(Long repositoryId, String username) {
        return memberJpaRepository.existsByGitRepositoryIdAndUserUsername(repositoryId, username);
    }

    private void verifyOwner(GitRepository repo, String username) {
        User user = userService.getByUsername(username);
        RepositoryMember member = memberJpaRepository
                .findByGitRepositoryIdAndUserId(repo.getId(), user.getId())
                .orElseThrow(() -> new AccessDeniedException("Not a member of this repository"));
        if (member.getRole() != RepositoryRole.OWNER) {
            throw new AccessDeniedException("Only the repository owner can manage members");
        }
    }
}
