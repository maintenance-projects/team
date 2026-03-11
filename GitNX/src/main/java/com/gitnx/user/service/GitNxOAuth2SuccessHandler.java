package com.gitnx.user.service;

import com.gitnx.common.config.GitAuthSessionStore;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
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
        String gitAuthSessionId = (String) request.getSession().getAttribute("gitAuthSessionId");

        if (gitAuthSessionId != null) {
            // Git credential 인증 흐름 → 세션 완료 처리
            String username = authentication.getName();
            sessionStore.complete(gitAuthSessionId, username);
            request.getSession().removeAttribute("gitAuthSessionId");

            log.info("Git auth completed via GitHub OAuth for user: {}", username);
            getRedirectStrategy().sendRedirect(request, response, "/git-auth/" + gitAuthSessionId);
            return;
        }

        // 일반 웹 로그인 → 대시보드로 이동
        setDefaultTargetUrl("/dashboard");
        setAlwaysUseDefaultTargetUrl(true);
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
