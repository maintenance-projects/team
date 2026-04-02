package com.gitnx.repository.controller;

import com.gitnx.organization.entity.Organization;
import com.gitnx.organization.service.OrganizationService;
import com.gitnx.repository.dto.ImportRepositoryRequest;
import com.gitnx.repository.enums.RepositoryRole;
import com.gitnx.repository.enums.RepositoryVisibility;
import com.gitnx.repository.service.GitRepositoryService;
import com.gitnx.user.entity.User;
import com.gitnx.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Controller
@RequestMapping("/github/sync")
@RequiredArgsConstructor
public class GitHubSyncController {

    private final UserService userService;
    private final OrganizationService organizationService;
    private final GitRepositoryService gitRepositoryService;
    @Qualifier("workbenchJdbcTemplate")
    private final JdbcTemplate workbenchJdbc;
    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping
    public String syncPage() {
        return "github/sync";
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @PostMapping
    public String doSync(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User user = userService.getByUsername(userDetails.getUsername());
        String token = user.getGithubAccessToken();

        if (token == null || token.isBlank()) {
            model.addAttribute("error", "GitHub 계정이 연동되어 있지 않습니다. 먼저 GitHub 계정을 연동해주세요.");
            return "github/sync";
        }

        List<String> logs = new ArrayList<>();

        // 1. 개인 레포 import (organizationId 없이)
        logs.add("── Personal Repositories");
        List<Map> personalRepos = fetchList(token, "/user/repos?per_page=100&type=owner&affiliation=owner");
        int personalCount = 0;
        for (Map repo : personalRepos) {
            // org 레포는 건너뜀 (owner.type == "Organization")
            Map owner = (Map) repo.get("owner");
            if (owner != null && "Organization".equals(owner.get("type"))) continue;

            String repoName = (String) repo.get("name");
            String cloneUrl = (String) repo.get("clone_url");
            Boolean isPrivate = (Boolean) repo.get("private");
            String description = (String) repo.get("description");

            try {
                ImportRepositoryRequest req = new ImportRepositoryRequest();
                req.setCloneUrl(cloneUrl);
                req.setName(repoName);
                req.setDescription(description);
                req.setAccessToken(token);
                req.setVisibility(Boolean.TRUE.equals(isPrivate) ? RepositoryVisibility.PRIVATE : RepositoryVisibility.PUBLIC);
                // organizationId 없음 → personal
                gitRepositoryService.importFromUrl(userDetails.getUsername(), req);
                logs.add("  ✓ [repo] " + repoName);
                personalCount++;
            } catch (Exception e) {
                logs.add("  · [repo] " + repoName + ": " + e.getMessage());
            }
        }
        logs.add("  → " + personalCount + "개 완료");

        // 2. GitHub org 목록 가져오기
        List<Map> orgs = fetchList(token, "/user/orgs?per_page=100");

        for (Map org : orgs) {
            String orgLogin = (String) org.get("login");
            logs.add("── Organization: " + orgLogin);

            // 2. GitNX에 org 생성
            try {
                organizationService.create(orgLogin, (String) org.get("description"), userDetails.getUsername());
                logs.add("  ✓ Organization 생성됨");
            } catch (Exception e) {
                logs.add("  · Organization 이미 존재 또는 생략: " + e.getMessage());
            }

            Organization gitNxOrg;
            try {
                gitNxOrg = organizationService.getByName(orgLogin);
            } catch (Exception e) {
                logs.add("  ✗ Organization 조회 실패, 건너뜀");
                continue;
            }

            // 3. org 레포 import
            List<Map> repos = fetchList(token, "/orgs/" + orgLogin + "/repos?per_page=100&type=all");
            logs.add("  레포지토리 " + repos.size() + "개 발견");
            for (Map repo : repos) {
                String repoName = (String) repo.get("name");
                String cloneUrl = (String) repo.get("clone_url");
                Boolean isPrivate = (Boolean) repo.get("private");
                String description = (String) repo.get("description");

                try {
                    ImportRepositoryRequest req = new ImportRepositoryRequest();
                    req.setCloneUrl(cloneUrl);
                    req.setName(repoName);
                    req.setDescription(description);
                    req.setOrganizationId(gitNxOrg.getId());
                    req.setAccessToken(token);
                    req.setVisibility(Boolean.TRUE.equals(isPrivate) ? RepositoryVisibility.PRIVATE : RepositoryVisibility.PUBLIC);

                    gitRepositoryService.importFromUrl(userDetails.getUsername(), req);
                    logs.add("  ✓ [repo] " + repoName);
                } catch (Exception e) {
                    logs.add("  ✗ [repo] " + repoName + ": " + e.getMessage());
                }
            }

            // 4. Workbench 전체 유저를 org 멤버로 추가
            List<Map<String, Object>> wbUsers = workbenchJdbc.queryForList(
                    "SELECT user_id FROM WB_ORGANIZATION.msg_user ORDER BY user_name");
            logs.add("  Workbench 멤버 " + wbUsers.size() + "명 추가 중");
            for (Map<String, Object> wbUser : wbUsers) {
                String userId = (String) wbUser.get("user_id");
                if (userId == null || userId.equalsIgnoreCase(userDetails.getUsername())) continue;
                try {
                    organizationService.addMember(orgLogin, userId, RepositoryRole.MEMBER, userDetails.getUsername());
                    logs.add("  ✓ [member] " + userId);
                } catch (Exception e) {
                    logs.add("  · [member] " + userId + ": " + e.getMessage());
                }
            }
        }

        logs.add("── 완료");
        model.addAttribute("logs", logs);
        return "github/sync";
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Map> fetchList(String token, String path) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        headers.set("Accept", "application/vnd.github+json");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<List> response = restTemplate.exchange(
                    "https://api.github.com" + path, HttpMethod.GET, entity, List.class);
            return response.getBody() != null ? response.getBody() : List.of();
        } catch (Exception e) {
            log.warn("GitHub API 호출 실패: {}", path, e);
            return List.of();
        }
    }
}
