package com.gitnx.repository.controller;

import com.gitnx.repository.dto.CreateRepositoryRequest;
import com.gitnx.repository.dto.ImportRepositoryRequest;
import com.gitnx.repository.dto.RepositoryDto;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class RepositoryController {

    private final GitRepositoryService gitRepositoryService;
    private final UserService userService;

    @GetMapping("/new")
    public String newRepoForm(Model model) {
        model.addAttribute("createRequest", new CreateRepositoryRequest());
        return "repository/new";
    }

    @PostMapping("/new")
    public String createRepo(@Valid @ModelAttribute("createRequest") CreateRepositoryRequest request,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            return "repository/new";
        }

        try {
            gitRepositoryService.create(userDetails.getUsername(), request);
            return "redirect:/" + userDetails.getUsername() + "/" + request.getName();
        } catch (IllegalArgumentException e) {
            bindingResult.reject("error", e.getMessage());
            return "repository/new";
        }
    }

    @GetMapping("/import")
    public String importRepoForm(Model model, @AuthenticationPrincipal UserDetails userDetails) {
        model.addAttribute("importRequest", new ImportRepositoryRequest());
        addGithubTokenFlag(model, userDetails);
        return "repository/import";
    }

    @PostMapping("/import")
    public String importRepo(@Valid @ModelAttribute("importRequest") ImportRepositoryRequest request,
                             BindingResult bindingResult,
                             @AuthenticationPrincipal UserDetails userDetails,
                             Model model,
                             RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            addGithubTokenFlag(model, userDetails);
            return "repository/import";
        }

        // GitHub OAuth 인증 선택 시 저장된 토큰 사용
        if (request.isUseGithubAuth()) {
            User user = userService.getByUsername(userDetails.getUsername());
            if (user.getGithubAccessToken() != null && !user.getGithubAccessToken().isBlank()) {
                request.setAccessToken(user.getGithubAccessToken());
            } else {
                bindingResult.reject("error", "GitHub 계정이 연결되어 있지 않습니다. Access Token을 직접 입력해주세요.");
                addGithubTokenFlag(model, userDetails);
                return "repository/import";
            }
        }

        try {
            GitRepository imported = gitRepositoryService.importFromUrl(userDetails.getUsername(), request);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Repository imported successfully from " + request.getCloneUrl());
            return "redirect:/" + userDetails.getUsername() + "/" + imported.getName();
        } catch (IllegalArgumentException e) {
            bindingResult.reject("error", e.getMessage());
            addGithubTokenFlag(model, userDetails);
            return "repository/import";
        } catch (Exception e) {
            bindingResult.reject("error", "Failed to import repository: " + e.getMessage());
            addGithubTokenFlag(model, userDetails);
            return "repository/import";
        }
    }

    private void addGithubTokenFlag(Model model, UserDetails userDetails) {
        User user = userService.getByUsername(userDetails.getUsername());
        boolean hasGithubToken = "GITHUB".equals(user.getProvider())
                && user.getGithubAccessToken() != null
                && !user.getGithubAccessToken().isBlank();
        model.addAttribute("hasGithubToken", hasGithubToken);
    }
}
