package com.spacenx.space.controller;

import com.spacenx.issue.entity.Issue;
import com.spacenx.issue.enums.IssuePriority;
import com.spacenx.issue.enums.IssueStatus;
import com.spacenx.issue.service.IssueService;
import com.spacenx.space.dto.CreateSpaceRequest;
import com.spacenx.space.dto.SpaceSettingsRequest;
import com.spacenx.space.entity.Space;
import com.spacenx.space.entity.SpaceMember;
import com.spacenx.space.enums.MemberRole;
import com.spacenx.space.service.SpaceService;
import com.spacenx.sprint.entity.Sprint;
import com.spacenx.sprint.service.SprintService;
import com.spacenx.user.entity.User;
import com.spacenx.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/spaces")
public class SpaceController {

    private final SpaceService spaceService;
    private final IssueService issueService;
    private final SprintService sprintService;
    private final UserService userService;

    @GetMapping
    public String listSpaces() {
        return "redirect:/dashboard";
    }

    @GetMapping("/create")
    public String createForm(Model model) {
        model.addAttribute("createSpaceRequest", new CreateSpaceRequest());
        return "space/create";
    }

    @PostMapping
    public String createSpace(@ModelAttribute CreateSpaceRequest request,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.findByUsername(userDetails.getUsername());
            Space space = spaceService.createSpace(request, currentUser);
            return "redirect:/spaces/" + space.getSpaceKey() + "/summary";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/spaces/create";
        }
    }

    @GetMapping("/{spaceKey}/summary")
    public String summary(@PathVariable String spaceKey,
                          @AuthenticationPrincipal UserDetails userDetails,
                          Model model) {
        User currentUser = userService.findByUsername(userDetails.getUsername());
        Space space = spaceService.getSpaceByKey(spaceKey);
        Map<IssueStatus, Long> issueStats = issueService.getIssueCountByStatus(space.getId());
        List<SpaceMember> members = spaceService.getMembers(space);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentSpace", space);
        model.addAttribute("activeTab", "summary");

        model.addAttribute("todoCount", issueStats.getOrDefault(IssueStatus.TODO, 0L));
        model.addAttribute("inProgressCount", issueStats.getOrDefault(IssueStatus.IN_PROGRESS, 0L));
        model.addAttribute("inReviewCount", issueStats.getOrDefault(IssueStatus.IN_REVIEW, 0L));
        model.addAttribute("doneCount", issueStats.getOrDefault(IssueStatus.DONE, 0L));
        model.addAttribute("members", members);

        // Issues by status for modals
        model.addAttribute("todoIssues", issueService.getIssuesBySpaceAndStatusWithAssignee(space.getId(), IssueStatus.TODO));
        model.addAttribute("inProgressIssues", issueService.getIssuesBySpaceAndStatusWithAssignee(space.getId(), IssueStatus.IN_PROGRESS));
        model.addAttribute("inReviewIssues", issueService.getIssuesBySpaceAndStatusWithAssignee(space.getId(), IssueStatus.IN_REVIEW));
        model.addAttribute("doneIssues", issueService.getIssuesBySpaceAndStatusWithAssignee(space.getId(), IssueStatus.DONE));

        // Recent issues
        List<Issue> allIssues = issueService.getIssuesBySpace(space.getId());
        List<Issue> recentIssues = allIssues.stream()
                .sorted(Comparator.comparing(Issue::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .collect(Collectors.toList());
        model.addAttribute("recentIssues", recentIssues);

        // Priority distribution
        long totalIssues = issueStats.values().stream().mapToLong(Long::longValue).sum();
        model.addAttribute("totalIssues", totalIssues);

        if (totalIssues > 0) {
            Map<IssuePriority, Long> priorityDistribution = allIssues.stream()
                    .collect(Collectors.groupingBy(Issue::getPriority, Collectors.counting()));
            model.addAttribute("priorityDistribution", priorityDistribution);

            // Issues grouped by priority for modals
            Map<IssuePriority, List<Issue>> issuesByPriority = allIssues.stream()
                    .collect(Collectors.groupingBy(Issue::getPriority));
            model.addAttribute("issuesByPriority", issuesByPriority);
        }

        // Active sprint (use eager-loading variant to avoid LazyInitializationException)
        Optional<Sprint> activeSprint = sprintService.getActiveSprintWithIssues(space.getId());
        activeSprint.ifPresent(sprint -> {
            model.addAttribute("activeSprint", sprint);
            long sprintTotal = sprint.getIssues().size();
            if (sprintTotal > 0) {
                long sprintDone = sprint.getIssues().stream()
                        .filter(i -> i.getStatus() == IssueStatus.DONE)
                        .count();
                model.addAttribute("sprintProgress", (int) (sprintDone * 100 / sprintTotal));
            } else {
                model.addAttribute("sprintProgress", 0);
            }
        });

        return "space/summary";
    }

    @GetMapping("/{spaceKey}/settings")
    public String settings(@PathVariable String spaceKey,
                           @AuthenticationPrincipal UserDetails userDetails,
                           Model model) {
        User currentUser = userService.findByUsername(userDetails.getUsername());
        Space space = spaceService.getSpaceByKey(spaceKey);
        List<SpaceMember> members = spaceService.getMembers(space);

        SpaceSettingsRequest settingsRequest = new SpaceSettingsRequest();
        settingsRequest.setName(space.getName());
        settingsRequest.setSpaceKey(space.getSpaceKey());
        settingsRequest.setDescription(space.getDescription());

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentSpace", space);
        model.addAttribute("spaceSettingsRequest", settingsRequest);
        model.addAttribute("members", members);
        model.addAttribute("activeTab", "settings");
        return "space/settings";
    }

    @PostMapping("/{spaceKey}/members")
    public String addMember(@PathVariable String spaceKey,
                            @RequestParam String username,
                            @RequestParam MemberRole role,
                            RedirectAttributes redirectAttributes) {
        try {
            Space space = spaceService.getSpaceByKey(spaceKey);
            User user = userService.findByUsername(username);
            spaceService.addMember(space, user, role);
            redirectAttributes.addFlashAttribute("successMessage", user.getUsername() + " 멤버가 추가되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/spaces/" + spaceKey + "/settings";
    }

    @DeleteMapping("/{spaceKey}")
    public String deleteSpace(@PathVariable String spaceKey,
                              RedirectAttributes redirectAttributes) {
        try {
            Space space = spaceService.getSpaceByKey(spaceKey);
            spaceService.deleteSpace(space);
            redirectAttributes.addFlashAttribute("successMessage", "스페이스가 삭제되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/spaces/" + spaceKey + "/settings";
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/{spaceKey}/members/{memberId}")
    public String removeMember(@PathVariable String spaceKey,
                               @PathVariable Long memberId,
                               RedirectAttributes redirectAttributes) {
        try {
            spaceService.removeMember(memberId);
            redirectAttributes.addFlashAttribute("successMessage", "멤버가 제거되었습니다.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/spaces/" + spaceKey + "/settings";
    }
}
