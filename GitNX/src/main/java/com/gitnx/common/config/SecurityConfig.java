package com.gitnx.common.config;

import com.gitnx.user.service.CustomOAuth2UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final GitAuthenticationProvider gitAuthenticationProvider;
    private final com.gitnx.user.service.GitNxOAuth2SuccessHandler gitNxOAuth2SuccessHandler;

    /**
     * Git HTTP protocol chain - handles /repo/** requests.
     * Uses HTTP Basic auth (stateless).
     * - git-receive-pack (push) requires authentication
     * - git-upload-pack (clone/fetch) is public
     */
    @Bean
    @Order(1)
    public SecurityFilterChain gitFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(new AntPathRequestMatcher("/repo/**"))
            .authorizeHttpRequests(auth -> auth
                // receive-pack POST endpoint requires auth
                .requestMatchers(new AntPathRequestMatcher("/repo/**/git-receive-pack")).authenticated()
                // info/refs?service=git-receive-pack also requires auth
                .requestMatchers(new GitReceivePackInfoRefsRequestMatcher()).authenticated()
                // Everything else (clone/fetch/info) is public
                .anyRequest().permitAll()
            )
            .httpBasic(basic -> basic
                .realmName("GitNX")
            )
            .authenticationManager(new ProviderManager(gitAuthenticationProvider))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            );
        return http.build();
    }

    /**
     * Web application chain - handles all other requests.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                        new AntPathRequestMatcher("/login"),
                        new AntPathRequestMatcher("/register"),
                        new AntPathRequestMatcher("/css/**"),
                        new AntPathRequestMatcher("/js/**"),
                        new AntPathRequestMatcher("/img/**"),
                        new AntPathRequestMatcher("/error/**"),
                        new AntPathRequestMatcher("/oauth2/**"),
                        new AntPathRequestMatcher("/login/oauth2/code/**"),
                        new AntPathRequestMatcher("/api/git/auth/**"),
                        new AntPathRequestMatcher("/git-auth/**")
                ).permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .defaultSuccessUrl("/dashboard", true)
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .oauth2Login(oauth2 -> oauth2
                .loginPage("/login")
                .userInfoEndpoint(ui -> ui.userService(customOAuth2UserService))
                .successHandler(gitNxOAuth2SuccessHandler)
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .permitAll()
            );
        return http.build();
    }

    /**
     * Matches info/refs requests with service=git-receive-pack parameter.
     * These need authentication because they initiate a push session.
     */
    private static class GitReceivePackInfoRefsRequestMatcher implements RequestMatcher {
        @Override
        public boolean matches(HttpServletRequest request) {
            String uri = request.getRequestURI();
            String service = request.getParameter("service");
            return uri.contains("/info/refs") && "git-receive-pack".equals(service);
        }
    }
}
