package com.gitnx.user.service;

import com.gitnx.common.client.WorkbenchUserClient;
import com.gitnx.user.dto.RegisterRequest;
import com.gitnx.user.dto.UserSearchDto;
import com.gitnx.user.entity.User;
import com.gitnx.user.repository.UserRepository;
import com.gitnx.common.client.WorkbenchUserClient.WorkbenchUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    public List<UserSearchDto> searchUsers(String query, int limit) {
        org.springframework.data.domain.PageRequest pageRequest = org.springframework.data.domain.PageRequest.of(0, limit);
        
        // 1. 로컬 유저 검색
        List<User> localUsers;
        if (query == null || query.isBlank()) {
            localUsers = userRepository.findAll(pageRequest).getContent();
        } else {
            localUsers = userRepository.findByUsernameContainingIgnoreCaseOrDisplayNameContainingIgnoreCase(query, query, pageRequest);
        }

        // 2. Workbench 유저 검색
        List<WorkbenchUser> workbenchUsers = workbenchUserClient.getAllUsers();
        
        // 3. 결과 통합 및 DTO 변환
        Map<String, UserSearchDto> combined = new LinkedHashMap<>();

        // 로컬 유저 먼저 채우기
        for (User u : localUsers) {
            combined.put(u.getUsername(), UserSearchDto.from(u));
        }

        // Workbench 유저 추가 (중복 제외 및 필터링)
        String lowerQuery = (query != null) ? query.toLowerCase() : "";
        for (WorkbenchUser wb : workbenchUsers) {
            if (combined.containsKey(wb.getUserId())) continue;
            
            boolean matches = lowerQuery.isEmpty() || 
                             wb.getUserId().toLowerCase().contains(lowerQuery) || 
                             (wb.getUserName() != null && wb.getUserName().toLowerCase().contains(lowerQuery));
            
            if (matches) {
                combined.put(wb.getUserId(), UserSearchDto.builder()
                        .username(wb.getUserId())
                        .displayName(wb.getUserName())
                        .build());
                if (combined.size() >= limit) break;
            }
        }

        return new ArrayList<>(combined.values());
    }
}
