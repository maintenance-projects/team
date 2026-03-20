package com.gitnx.repository.controller;

import com.gitnx.repository.entity.GitRepository;
import com.gitnx.repository.entity.RepositoryMember;
import com.gitnx.repository.enums.RepositoryRole;
import com.gitnx.repository.service.GitRepositoryService;
import com.gitnx.repository.service.RepositoryMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class SettingsController {

    private final RepositoryMemberService memberService;
    private final GitRepositoryService gitRepositoryService;

    @GetMapping("/{owner}/{repo}/settings")
    public String settings(@PathVariable String owner, @PathVariable String repo,
                           @AuthenticationPrincipal UserDetails userDetails,
                           Model model) {
        if (!memberService.isOwner(owner, repo, userDetails.getUsername())) {
            throw new AccessDeniedException("Only the repository owner can access settings");
        }

        GitRepository gitRepo = gitRepositoryService.getByOwnerAndName(owner, repo);
        List<RepositoryMember> members = memberService.getMembers(owner, repo);

        model.addAttribute("owner", owner);
        model.addAttribute("repo", repo);
        model.addAttribute("gitRepo", gitRepo);
        model.addAttribute("members", members);
        model.addAttribute("roles", List.of(RepositoryRole.MAINTAINER, RepositoryRole.DEVELOPER, RepositoryRole.GUEST));
        model.addAttribute("activeTab", "settings");

        return "repository/settings";
    }

    @PostMapping("/{owner}/{repo}/settings/members")
    public String addMember(@PathVariable String owner, @PathVariable String repo,
                            @RequestParam String username,
                            @RequestParam RepositoryRole role,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        try {
            memberService.addMember(owner, repo, username, role, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Member '" + username + "' added successfully");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/" + owner + "/" + repo + "/settings";
    }

    @PostMapping("/{owner}/{repo}/settings/members/{memberId}/role")
    public String changeRole(@PathVariable String owner, @PathVariable String repo,
                             @PathVariable Long memberId,
                             @RequestParam RepositoryRole role,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        try {
            memberService.changeRole(owner, repo, memberId, role, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Role updated successfully");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/" + owner + "/" + repo + "/settings";
    }

    @PostMapping("/{owner}/{repo}/settings/members/{memberId}/delete")
    public String removeMember(@PathVariable String owner, @PathVariable String repo,
                               @PathVariable Long memberId,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        try {
            memberService.removeMember(owner, repo, memberId, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Member removed successfully");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/" + owner + "/" + repo + "/settings";
    }

    @PostMapping("/{owner}/{repo}/settings/delete")
    public String deleteRepository(@PathVariable String owner, @PathVariable String repo,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        if (!memberService.isOwner(owner, repo, userDetails.getUsername())) {
            throw new AccessDeniedException("Only the repository owner can delete this repository");
        }
        gitRepositoryService.delete(owner, repo);
        redirectAttributes.addFlashAttribute("successMessage", "Repository deleted successfully");
        return "redirect:/dashboard";
    }
}
