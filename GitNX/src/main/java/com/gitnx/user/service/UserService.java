package com.gitnx.user.service;

import com.gitnx.common.client.WorkbenchUserClient;
import com.gitnx.user.dto.RegisterRequest;
import com.gitnx.user.dto.UserSearchDto;
import com.gitnx.user.entity.User;
import com.gitnx.user.repository.UserRepository;
import com.gitnx.common.client.WorkbenchUserClient.WorkbenchUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
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
    @Qualifier("workbenchJdbcTemplate")
    private final JdbcTemplate workbenchJdbc;

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
     * username으로 로컬 DB에서 먼저 찾고, 없으면 Workbench DB에서 조회 후 자동 생성.
     */
    @Transactional
    public User findOrCreateFromWorkbench(String username) {
        // 1. 로컬 DB에서 먼저 찾기
        Optional<User> local = userRepository.findByUsername(username);
        if (local.isPresent()) return local.get();

        // 2. Workbench DB에서 직접 조회
        List<Map<String, Object>> rows = workbenchJdbc.queryForList(
                "SELECT user_id, user_name, email FROM WB_ORGANIZATION.msg_user WHERE user_id = ?",
                username);

        if (rows.isEmpty()) {
            throw new IllegalArgumentException("User not found in Workbench: " + username);
        }

        Map<String, Object> row = rows.get(0);
        String userId = (String) row.get("user_id");
        String userName = (String) row.get("user_name");
        String userEmail = (String) row.get("email");

        log.info("[UserService] Creating GitNX user from Workbench DB: userId={}, userName={}",
                userId, userName);

        String email = userEmail != null ? userEmail : userId + "@workbench.local";
        if (userRepository.existsByEmail(email)) {
            email = userId + "+" + System.currentTimeMillis() + "@workbench.local";
        }

        User user = User.builder()
                .username(userId)
                .email(email)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .displayName(userName)
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

    @Transactional
    public void unlinkGithub(String username) {
        User user = getByUsername(username);
        user.setGithubAccessToken(null);
        userRepository.save(user);
    }
}
