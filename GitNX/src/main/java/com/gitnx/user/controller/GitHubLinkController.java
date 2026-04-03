package com.gitnx.user.controller;

import com.gitnx.user.service.UserService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class GitHubLinkController {

    private final UserService userService;

    /**
     * GitHub 계정 연동 - OAuth 플로우로 리다이렉트
     * 토큰만 저장하고 기존 세션(Workbench 로그인) 유지
     */
    @GetMapping("/settings/github/link")
    public String linkGithub(HttpSession session,
                             @AuthenticationPrincipal UserDetails userDetails,
                             @RequestParam(defaultValue = "/dashboard") String returnUrl) {
        session.setAttribute("githubLinkUsername", userDetails.getUsername());
        session.setAttribute("githubLinkReturnUrl", returnUrl);
        session.setAttribute("githubLinkOriginalAuth",
                SecurityContextHolder.getContext().getAuthentication());
        return "redirect:/oauth2/authorization/github";
    }

    @PostMapping("/settings/github/unlink")
    public String unlinkGithub(@AuthenticationPrincipal UserDetails userDetails,
                               @RequestParam(defaultValue = "/import") String returnUrl,
                               RedirectAttributes redirectAttributes) {
        userService.unlinkGithub(userDetails.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "GitHub 계정 연동이 해제되었습니다.");
        return "redirect:" + returnUrl;
    }
}
