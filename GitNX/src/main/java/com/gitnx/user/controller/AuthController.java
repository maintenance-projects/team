package com.gitnx.user.controller;

import com.gitnx.organization.entity.Organization;
import com.gitnx.organization.service.OrganizationService;
import com.gitnx.repository.dto.RepositoryDto;
import com.gitnx.repository.service.GitRepositoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final GitRepositoryService gitRepositoryService;
    private final OrganizationService organizationService;

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        String username = userDetails.getUsername();

        // Personal repos (organization이 null인 것)
        List<RepositoryDto> personalRepos = gitRepositoryService.listAccessible(username).stream()
                .filter(r -> r.getOrganizationId() == null)
                .toList();

        // Organization별 레포 그룹
        List<Organization> orgs = organizationService.listByUser(username);
        Map<Organization, List<RepositoryDto>> orgRepos = new LinkedHashMap<>();
        for (Organization org : orgs) {
            orgRepos.put(org, gitRepositoryService.listByOrganization(org.getId()));
        }

        model.addAttribute("personalRepos", personalRepos);
        model.addAttribute("orgRepos", orgRepos);
        model.addAttribute("organizations", orgs);
        return "dashboard/index";
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/dashboard";
    }
}
