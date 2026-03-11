package com.gitnx.user.controller;

import com.gitnx.common.config.GitAuthSessionStore;
import com.gitnx.common.config.GitAuthSessionStore.GitAuthSession;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import com.gitnx.user.entity.User;
import com.gitnx.user.repository.UserRepository;

import java.util.Map;

@Slf4j
@Controller
@RequiredArgsConstructor
public class GitCredentialController {

    private final GitAuthSessionStore sessionStore;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Credential helper가 호출 - 인증 세션 생성
     */
    @PostMapping("/api/git/auth/start")
    @ResponseBody
    public Map<String, String> startAuth() {
        GitAuthSession session = sessionStore.create();
        log.debug("Git auth session created: {}", session.getSessionId());
        return Map.of("sessionId", session.getSessionId());
    }

    /**
     * Credential helper가 폴링 - 인증 완료 여부 확인
     */
    @GetMapping("/api/git/auth/poll/{sessionId}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> pollAuth(@PathVariable String sessionId) {
        GitAuthSession session = sessionStore.get(sessionId);
        if (session == null) {
            return ResponseEntity.notFound().build();
        }
        if (session.isCompleted()) {
            return ResponseEntity.ok(Map.of(
                    "status", "complete",
                    "username", session.getAuthenticatedUsername()
            ));
        }
        return ResponseEntity.ok(Map.of("status", "pending"));
    }

    /**
     * 브라우저에서 보여줄 인증 페이지
     */
    @GetMapping("/git-auth/{sessionId}")
    public String authPage(@PathVariable String sessionId, Model model, HttpSession httpSession) {
        GitAuthSession session = sessionStore.get(sessionId);
        if (session == null) {
            model.addAttribute("error", "인증 세션이 만료되었습니다. 다시 시도해주세요.");
            return "auth/git-auth";
        }
        if (session.isCompleted()) {
            model.addAttribute("completed", true);
            model.addAttribute("username", session.getAuthenticatedUsername());
            return "auth/git-auth";
        }

        // GitHub OAuth 리다이렉트를 위해 sessionId를 HTTP 세션에 저장
        httpSession.setAttribute("gitAuthSessionId", sessionId);

        model.addAttribute("sessionId", sessionId);
        return "auth/git-auth";
    }

    /**
     * 일반 로그인 (username/password) 으로 Git 인증
     */
    @PostMapping("/git-auth/{sessionId}/login")
    public String loginAuth(@PathVariable String sessionId,
                            @RequestParam String username,
                            @RequestParam String password,
                            Model model) {
        GitAuthSession session = sessionStore.get(sessionId);
        if (session == null) {
            model.addAttribute("error", "인증 세션이 만료되었습니다.");
            return "auth/git-auth";
        }

        // 사용자 인증
        User user = userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElse(null);

        if (user == null || !user.isEnabled() || !passwordEncoder.matches(password, user.getPassword())) {
            model.addAttribute("sessionId", sessionId);
            model.addAttribute("error", "아이디 또는 비밀번호가 올바르지 않습니다.");
            return "auth/git-auth";
        }

        sessionStore.complete(sessionId, user.getUsername());
        log.info("Git auth completed via login for user: {}", user.getUsername());

        model.addAttribute("completed", true);
        model.addAttribute("username", user.getUsername());
        return "auth/git-auth";
    }
}