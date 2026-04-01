package com.gitnx.organization.controller;

import com.gitnx.organization.entity.Organization;
import com.gitnx.organization.entity.OrganizationMember;
import com.gitnx.organization.service.OrganizationService;
import com.gitnx.repository.dto.RepositoryDto;
import com.gitnx.repository.enums.RepositoryRole;
import com.gitnx.repository.service.GitRepositoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class OrganizationController {

    private final OrganizationService organizationService;
    private final GitRepositoryService gitRepositoryService;

    @GetMapping("/organizations")
    public String list(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        List<Organization> orgs = organizationService.listByUser(userDetails.getUsername());
        model.addAttribute("organizations", orgs);
        return "organization/list";
    }

    @GetMapping("/organizations/new")
    public String newForm() {
        return "organization/new";
    }

    @PostMapping("/organizations/new")
    public String create(@RequestParam String name,
                         @RequestParam(required = false) String description,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        try {
            organizationService.create(name, description, userDetails.getUsername());
            return "redirect:/org/" + name;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/organizations/new";
        }
    }

    @GetMapping("/org/{orgName}")
    public String detail(@PathVariable String orgName,
                         @AuthenticationPrincipal UserDetails userDetails,
                         Model model) {
        Organization org = organizationService.getByName(orgName);
        List<RepositoryDto> repos = gitRepositoryService.listByOrganization(org.getId());

        model.addAttribute("org", org);
        model.addAttribute("repositories", repos);
        model.addAttribute("isOwner", organizationService.isOwner(orgName, userDetails.getUsername()));
        return "organization/detail";
    }

    @GetMapping("/org/{orgName}/settings")
    public String settings(@PathVariable String orgName,
                           @AuthenticationPrincipal UserDetails userDetails,
                           Model model) {
        if (!organizationService.isOwner(orgName, userDetails.getUsername())) {
            throw new org.springframework.security.access.AccessDeniedException("Only owners can access settings");
        }

        Organization org = organizationService.getByName(orgName);
        List<OrganizationMember> members = organizationService.getMembers(orgName);

        model.addAttribute("org", org);
        model.addAttribute("members", members);
        return "organization/settings";
    }

    @PostMapping("/org/{orgName}/settings/members")
    public String addMember(@PathVariable String orgName,
                            @RequestParam String username,
                            @RequestParam RepositoryRole role,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        try {
            organizationService.addMember(orgName, username, role, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Member '" + username + "' added");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/org/" + orgName + "/settings";
    }

    @PostMapping("/org/{orgName}/settings/members/{memberId}/delete")
    public String removeMember(@PathVariable String orgName, @PathVariable Long memberId,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        try {
            organizationService.removeMember(orgName, memberId, userDetails.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Member removed");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/org/" + orgName + "/settings";
    }

    @PostMapping("/org/{orgName}/settings/delete")
    public String delete(@PathVariable String orgName,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        organizationService.delete(orgName, userDetails.getUsername());
        redirectAttributes.addFlashAttribute("successMessage", "Organization deleted");
        return "redirect:/organizations";
    }
}
