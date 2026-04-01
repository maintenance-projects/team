package com.gitnx.common.config;

import com.gitnx.user.entity.User;
import com.gitnx.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Workbench MSG_USER 테이블에서 직접 인증.
 * 인증 성공 시 GitNX ng_user에 자동 생성/동기화.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkbenchAuthenticationProvider implements AuthenticationProvider {

    @Qualifier("workbenchJdbcTemplate")
    private final JdbcTemplate workbenchJdbc;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String userId = authentication.getName();
        String rawPassword = authentication.getCredentials().toString();

        // MSG_USER에서 사용자 조회
        List<Map<String, Object>> rows = workbenchJdbc.queryForList(
                "SELECT user_id, user_name, email, password FROM WB_ORGANIZATION.msg_user WHERE user_id = ?",
                userId);

        if (rows.isEmpty()) {
            throw new BadCredentialsException("User not found");
        }

        Map<String, Object> row = rows.get(0);
        String storedPassword = (String) row.get("password");

        // BCrypt 비밀번호 검증
        if (!passwordEncoder.matches(rawPassword, storedPassword)) {
            throw new BadCredentialsException("Invalid password");
        }

        String userName = (String) row.get("user_name");
        String userEmail = (String) row.get("email");

        // GitNX ng_user에 자동 생성/동기화
        User gitnxUser = userRepository.findByUsername(userId).orElseGet(() -> {
            String email = userEmail != null ? userEmail : userId + "@workbench.local";
            // email 중복 시 고유한 email 생성
            if (userRepository.existsByEmail(email)) {
                email = userId + "+" + System.currentTimeMillis() + "@workbench.local";
            }
            User newUser = User.builder()
                    .username(userId)
                    .email(email)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .displayName(userName != null ? userName : userId)
                    .provider("WORKBENCH")
                    .build();
            log.info("Auto-registered workbench user: {}", userId);
            return userRepository.save(newUser);
        });

        // displayName 동기화
        if (userName != null && !userName.equals(gitnxUser.getDisplayName())) {
            gitnxUser.setDisplayName(userName);
            userRepository.save(gitnxUser);
        }

        return new UsernamePasswordAuthenticationToken(
                new org.springframework.security.core.userdetails.User(
                        gitnxUser.getUsername(),
                        "",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                ),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
