package com.gitnx.repository.controller;

import com.gitnx.organization.entity.Organization;
import com.gitnx.organization.service.OrganizationService;
import com.gitnx.repository.dto.CreateRepositoryRequest;
import com.gitnx.repository.dto.ImportRepositoryRequest;
import com.gitnx.repository.entity.GitRepository;
import com.gitnx.repository.service.GitRepositoryService;
import com.gitnx.user.entity.User;
import com.gitnx.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

import java.util.List;

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
    public String createRepo(@Valid @ModelAttribute("createRequest") CreateRepositoryRequest request,
                             BindingResult bindingResult,
                             @RequestParam(required = false) Long organizationId,
                             @AuthenticationPrincipal UserDetails userDetails,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        request.setOrganizationId(organizationId);

        if (bindingResult.hasErrors()) {
            var realErrors = bindingResult.getFieldErrors().stream()
                    .filter(e -> !"organizationId".equals(e.getField()) && !"visibility".equals(e.getField()))
                    .toList();
            if (!realErrors.isEmpty()) {
                model.addAttribute("organizations", organizationService.listByUser(userDetails.getUsername()));
                return "repository/new";
            }
        }

        try {
            gitRepositoryService.create(userDetails.getUsername(), request);
            return "redirect:/" + userDetails.getUsername() + "/" + request.getName();
        } catch (IllegalArgumentException e) {
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
    public String importRepo(@Valid @ModelAttribute("importRequest") ImportRepositoryRequest request,
                             BindingResult bindingResult,
                             @RequestParam(required = false) Long organizationId,
                             @AuthenticationPrincipal UserDetails userDetails,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        // organizationId를 수동으로 세팅 (빈 문자열 바인딩 에러 방지)
        request.setOrganizationId(organizationId);

        // organizationId 바인딩 에러는 무시
        if (bindingResult.hasErrors()) {
            var realErrors = bindingResult.getFieldErrors().stream()
                    .filter(e -> !"organizationId".equals(e.getField()) && !"visibility".equals(e.getField()))
                    .toList();
            if (!realErrors.isEmpty()) {
                model.addAttribute("organizations", organizationService.listByUser(userDetails.getUsername()));
                return "repository/import";
            }
        }

        // GitHub OAuth 토큰 자동 주입
        User user = userService.getByUsername(userDetails.getUsername());
        if (user.getGithubAccessToken() != null && !user.getGithubAccessToken().isBlank()) {
            request.setAccessToken(user.getGithubAccessToken());
        }

        try {
            GitRepository imported = gitRepositoryService.importFromUrl(userDetails.getUsername(), request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Repository imported successfully from " + request.getCloneUrl());
            return "redirect:/" + userDetails.getUsername() + "/" + imported.getName();
        } catch (IllegalArgumentException e) {
            bindingResult.reject("error", e.getMessage());
            model.addAttribute("organizations", organizationService.listByUser(userDetails.getUsername()));
            return "repository/import";
        } catch (Exception e) {
            bindingResult.reject("error", "Failed to import repository: " + e.getMessage());
            model.addAttribute("organizations", organizationService.listByUser(userDetails.getUsername()));
            return "repository/import";
        }
    }
}
