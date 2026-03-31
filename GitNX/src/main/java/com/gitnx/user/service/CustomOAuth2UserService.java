package com.gitnx.user.service;

import com.gitnx.user.entity.User;
import com.gitnx.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        Map<String, Object> attributes = oAuth2User.getAttributes();
        String githubId = String.valueOf(attributes.get("id"));
        String login = (String) attributes.get("login");
        String email = (String) attributes.get("email");
        String avatarUrl = (String) attributes.get("avatar_url");
        String name = (String) attributes.get("name");

        String accessToken = userRequest.getAccessToken().getTokenValue();

        // 기존 사용자 찾거나 자동 회원가입
        User user = userRepository.findByProviderAndProviderId("GITHUB", githubId)
                .orElseGet(() -> findOrCreateUser(githubId, login, email, avatarUrl, name));

        // 매 로그인마다 토큰 갱신 + 프로필 업데이트
        user.setGithubAccessToken(accessToken);
        if (avatarUrl != null) {
            user.setAvatarUrl(avatarUrl);
        }
        if (name != null) {
            user.setDisplayName(name);
        }
        userRepository.save(user);

        Map<String, Object> userAttributes = new HashMap<>(attributes);
        userAttributes.put("username", user.getUsername());

        return new CustomOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                userAttributes,
                user.getUsername()
        );
    }

    private User findOrCreateUser(String githubId, String login, String email, String avatarUrl, String name) {
        // 이메일로 기존 계정 검색
        if (email != null) {
            Optional<User> existingByEmail = userRepository.findByEmail(email);
            if (existingByEmail.isPresent()) {
                User existing = existingByEmail.get();
                existing.setProvider("GITHUB");
                existing.setProviderId(githubId);
                if (existing.getAvatarUrl() == null) {
                    existing.setAvatarUrl(avatarUrl);
                }
                log.info("Linked GitHub account to existing user by email: {}", existing.getUsername());
                return userRepository.save(existing);
            }
        }

        // username으로 기존 계정 검색
        Optional<User> existingByUsername = userRepository.findByUsername(login);
        if (existingByUsername.isPresent()) {
            User existing = existingByUsername.get();
            existing.setProvider("GITHUB");
            existing.setProviderId(githubId);
            if (existing.getAvatarUrl() == null) {
                existing.setAvatarUrl(avatarUrl);
            }
            log.info("Linked GitHub account to existing user by username: {}", existing.getUsername());
            return userRepository.save(existing);
        }

        // 자동 회원가입
        String userEmail = email != null ? email : login + "@github.user";
        User newUser = User.builder()
                .username(login)
                .email(userEmail)
                .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                .displayName(name != null ? name : login)
                .avatarUrl(avatarUrl)
                .provider("GITHUB")
                .providerId(githubId)
                .build();

        log.info("Auto-registered new user from GitHub: {}", login);
        return userRepository.save(newUser);
    }
}
