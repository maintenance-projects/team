package com.gitnx.repository.controller;

import com.gitnx.organization.service.OrganizationService;
import com.gitnx.repository.dto.CreateRepositoryRequest;
import com.gitnx.repository.dto.ImportRepositoryRequest;
import com.gitnx.repository.entity.GitRepository;
import com.gitnx.repository.service.GitRepositoryService;
import com.gitnx.user.entity.User;
import com.gitnx.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Slf4j
@Controller
@RequiredArgsConstructor
public class RepositoryController {

    private final GitRepositoryService gitRepositoryService;
    private final UserService userService;
    private final OrganizationService organizationService;

    @GetMapping("/new")
    public String newRepoForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("createRequest", new CreateRepositoryRequest());
        model.addAttribute("organizations", organizationService.listByUser(userDetails.getUsername()));
        return "repository/new";
    }

    @PostMapping("/new")
    public String createRepo(@ModelAttribute("createRequest") CreateRepositoryRequest request,
                             BindingResult bindingResult,
                             @RequestParam(required = false) Long organizationId,
                             @AuthenticationPrincipal UserDetails userDetails,
                             Model model) {
        request.setOrganizationId(organizationId);
        log.info("[CreateRepo] name={}, orgId={}, errors={}", request.getName(), organizationId, bindingResult.getAllErrors());

        if (request.getName() == null || request.getName().isBlank()) {
            bindingResult.reject("error", "Repository name is required");
            model.addAttribute("organizations", organizationService.listByUser(userDetails.getUsername()));
            return "repository/new";
        }

        try {
            gitRepositoryService.create(userDetails.getUsername(), request);
            return "redirect:/" + userDetails.getUsername() + "/" + request.getName();
        } catch (Exception e) {
            log.error("[CreateRepo] failed: {}", e.getMessage());
            bindingResult.reject("error", e.getMessage());
            model.addAttribute("organizations", organizationService.listByUser(userDetails.getUsername()));
            return "repository/new";
        }
    }

    @GetMapping("/import")
    public String importRepoForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        model.addAttribute("importRequest", new ImportRepositoryRequest());
        model.addAttribute("organizations", organizationService.listByUser(userDetails.getUsername()));
        return "repository/import";
    }

    @PostMapping("/import")
    public String importRepo(@ModelAttribute("importRequest") ImportRepositoryRequest request,
                             BindingResult bindingResult,
                             @RequestParam(required = false) Long organizationId,
                             @AuthenticationPrincipal UserDetails userDetails,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        request.setOrganizationId(organizationId);
        log.info("[ImportRepo] cloneUrl={}, name={}, orgId={}, errors={}",
                request.getCloneUrl(), request.getName(), organizationId, bindingResult.getAllErrors());

        if (request.getCloneUrl() == null || request.getCloneUrl().isBlank()) {
            log.warn("[ImportRepo] cloneUrl is empty");
            bindingResult.reject("error", "Clone URL is required");
            model.addAttribute("organizations", organizationService.listByUser(userDetails.getUsername()));
            return "repository/import";
        }

        // GitHub OAuth 토큰 자동 주입
        User user = userService.getByUsername(userDetails.getUsername());
        if (user.getGithubAccessToken() != null && !user.getGithubAccessToken().isBlank()) {
            request.setAccessToken(user.getGithubAccessToken());
            log.info("[ImportRepo] GitHub token injected");
        } else {
            log.warn("[ImportRepo] No GitHub token found");
        }

        try {
            GitRepository imported = gitRepositoryService.importFromUrl(userDetails.getUsername(), request);
            log.info("[ImportRepo] success: {}", imported.getName());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Repository imported successfully from " + request.getCloneUrl());
            return "redirect:/" + userDetails.getUsername() + "/" + imported.getName();
        } catch (Exception e) {
            log.error("[ImportRepo] failed: {}", e.getMessage(), e);
            bindingResult.reject("error", "Failed to import: " + e.getMessage());
            model.addAttribute("organizations", organizationService.listByUser(userDetails.getUsername()));
            return "repository/import";
        }
    }
}
