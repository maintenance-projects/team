package com.gitnx.common.config;

import com.gitnx.user.entity.User;
import com.gitnx.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Git HTTP Basic Auth용 커스텀 AuthenticationProvider.
 * - GitHub OAuth 사용자: username만 일치하면 인증 통과 (비밀번호 무시)
 * - 일반(LOCAL) 사용자: username + BCrypt 비밀번호 검증
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GitAuthenticationProvider implements AuthenticationProvider {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = authentication.getName();
        String rawPassword = (String) authentication.getCredentials();

        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        if (!user.isEnabled()) {
            throw new BadCredentialsException("Account is disabled");
        }

        // GitHub OAuth 사용자: 아이디만 일치하면 인증 통과
        if ("GITHUB".equals(user.getProvider())) {
            log.debug("Git auth via GitHub OAuth for user: {}", username);
            return createSuccessToken(user);
        }

        // 일반 사용자: BCrypt 비밀번호 검증
        if (passwordEncoder.matches(rawPassword, user.getPassword())) {
            return createSuccessToken(user);
        }

        throw new BadCredentialsException("Invalid credentials");
    }

    private UsernamePasswordAuthenticationToken createSuccessToken(User user) {
        var userDetails = new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                user.isEnabled(),
                true, true, true,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );
        return new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}