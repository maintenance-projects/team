package com.gitnx.user.service;

import com.gitnx.common.config.GitAuthSessionStore;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class GitNxOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final GitAuthSessionStore sessionStore;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        HttpSession session = request.getSession();

        // GitHub 계정 연동 흐름 → 원래 인증 복원 후 리턴
        String linkUsername = (String) session.getAttribute("githubLinkUsername");
        if (linkUsername != null) {
            String returnUrl = (String) session.getAttribute("githubLinkReturnUrl");
            Authentication originalAuth = (Authentication) session.getAttribute("githubLinkOriginalAuth");

            if (originalAuth != null) {
                SecurityContextHolder.getContext().setAuthentication(originalAuth);
            }

            session.removeAttribute("githubLinkUsername");
            session.removeAttribute("githubLinkReturnUrl");
            session.removeAttribute("githubLinkOriginalAuth");

            log.info("GitHub token linked for user: {}", linkUsername);
            getRedirectStrategy().sendRedirect(request, response,
                    returnUrl != null ? returnUrl : "/dashboard");
            return;
        }

        // Git credential 인증 흐름 → 세션 완료 처리
        String gitAuthSessionId = (String) session.getAttribute("gitAuthSessionId");
        if (gitAuthSessionId != null) {
            String username = authentication.getName();
            sessionStore.complete(gitAuthSessionId, username);
            session.removeAttribute("gitAuthSessionId");

            log.info("Git auth completed via GitHub OAuth for user: {}", username);
            getRedirectStrategy().sendRedirect(request, response, "/git-auth/" + gitAuthSessionId);
            return;
        }

        // 일반 로그인 → 대시보드로 이동
        setDefaultTargetUrl("/dashboard");
        setAlwaysUseDefaultTargetUrl(true);
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
