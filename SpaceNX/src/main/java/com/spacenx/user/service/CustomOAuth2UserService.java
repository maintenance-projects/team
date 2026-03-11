package com.spacenx.user.service;

import com.spacenx.user.entity.User;
import com.spacenx.user.repository.UserRepository;
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

        User user = userRepository.findByProviderAndProviderId("GITHUB", githubId)
                .orElseGet(() -> findOrCreateUser(githubId, login, email, avatarUrl, name));

        Map<String, Object> userAttributes = new HashMap<>(attributes);
        userAttributes.put("username", user.getUsername());

        return new CustomOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                userAttributes,
                user.getUsername()
        );
    }

    private User findOrCreateUser(String githubId, String login, String email, String avatarUrl, String name) {
        // Try to link to existing account by email
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

        // Try to link to existing account by username
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

        // Create new user
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

        log.info("Created new user from GitHub OAuth: {}", login);
        return userRepository.save(newUser);
    }
}
