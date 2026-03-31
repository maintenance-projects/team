package com.gitnx.repository.controller;

import com.gitnx.user.entity.User;
import com.gitnx.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RestController
@RequestMapping("/github")
@RequiredArgsConstructor
public class GitHubRepoController {

    private final UserService userService;
    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * GitHub API를 통해 현재 사용자의 레포지토리 목록 조회
     * - 개인 repo + 접근 가능한 org repo 포함
     */
    @GetMapping("/repos")
    public ResponseEntity<String> listGithubRepos(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int perPage,
            @RequestParam(defaultValue = "") String search) {

        User user = userService.getByUsername(userDetails.getUsername());
        String token = user.getGithubAccessToken();
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().body("{\"error\": \"GitHub token not found\"}");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", "application/vnd.github+json");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        // affiliation=owner,collaborator,organization_member 로 모든 접근 가능한 repo 조회
        String url = "https://api.github.com/user/repos"
                + "?affiliation=owner,collaborator,organization_member"
                + "&sort=updated&direction=desc"
                + "&per_page=" + perPage
                + "&page=" + page;

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
        return ResponseEntity.ok()
                .header("X-GitHub-Link", response.getHeaders().getFirst("Link"))
                .body(response.getBody());
    }
}
