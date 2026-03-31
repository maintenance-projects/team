package com.gitnx.user.service;

import com.gitnx.common.client.WorkbenchUserClient;
import com.gitnx.user.dto.RegisterRequest;
import com.gitnx.user.entity.User;
import com.gitnx.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WorkbenchUserClient workbenchUserClient;

    private static final Set<String> RESERVED_USERNAMES = Set.of(
            "login", "register", "logout", "dashboard", "new", "settings",
            "admin", "api", "git", "explore", "help", "about",
            "static", "css", "js", "img", "assets", "error"
    );

    @Transactional
    public User register(RegisterRequest request) {
        if (RESERVED_USERNAMES.contains(request.getUsername().toLowerCase())) {
            throw new IllegalArgumentException("This username is reserved");
        }
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (!request.getPassword().equals(request.getPasswordConfirm())) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        User user = User.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .displayName(request.getDisplayName() != null ? request.getDisplayName() : request.getUsername())
                .build();

        return userRepository.save(user);
    }

    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
    }

    /**
     * username으로 로컬 DB에서 먼저 찾고, 없으면 Workbench API에서 조회 후 자동 생성.
     */
    @Transactional
    public User findOrCreateFromWorkbench(String username) {
        // 1. 로컬 DB에서 먼저 찾기
        Optional<User> local = userRepository.findByUsername(username);
        if (local.isPresent()) return local.get();

        // 2. Workbench API로 유저 조회
        WorkbenchUserClient.WorkbenchUser wbUser = workbenchUserClient.getUserDetail(username);
        if (wbUser == null) {
            throw new IllegalArgumentException("User not found in Workbench: " + username);
        }

        // 3. GitNX ng_user에 자동 생성
        log.info("[UserService] Creating GitNX user from Workbench: userId={}, userName={}",
                wbUser.getUserId(), wbUser.getUserName());

        User user = User.builder()
                .username(wbUser.getUserId())
                .email(wbUser.getUserEmail() != null ? wbUser.getUserEmail() : wbUser.getUserId() + "@workbench.local")
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .displayName(wbUser.getUserName())
                .provider("WORKBENCH")
                .build();

        return userRepository.save(user);
    }

    public java.util.List<User> searchUsers(String query, int limit) {
        org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(0, limit);
        if (query == null || query.isBlank()) {
            return userRepository.findAll(pageRequest).getContent();
        }
        return userRepository.findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(query, query, pageRequest);
    }
}
