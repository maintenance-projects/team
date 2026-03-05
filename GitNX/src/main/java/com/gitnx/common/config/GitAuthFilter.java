package com.gitnx.common.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Ensures 401 responses from /repo/** include WWW-Authenticate header
 * so git clients know to use HTTP Basic authentication.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GitAuthFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        if (request.getRequestURI().startsWith("/repo/")) {
            chain.doFilter(request, new WwwAuthenticateResponseWrapper(response));
        } else {
            chain.doFilter(request, response);
        }
    }

    private static class WwwAuthenticateResponseWrapper extends HttpServletResponseWrapper {

        WwwAuthenticateResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        @Override
        public void sendError(int sc) throws IOException {
            addBasicChallenge(sc);
            super.sendError(sc);
        }

        @Override
        public void sendError(int sc, String msg) throws IOException {
            addBasicChallenge(sc);
            super.sendError(sc, msg);
        }

        private void addBasicChallenge(int sc) {
            if (sc == SC_UNAUTHORIZED && getHeader("WWW-Authenticate") == null) {
                setHeader("WWW-Authenticate", "Basic realm=\"GitNX\"");
            }
        }
    }
}