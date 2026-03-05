package com.spacenx.issue.controller;

import com.spacenx.issue.entity.Issue;
import com.spacenx.issue.enums.IssueStatus;
import com.spacenx.issue.service.IssueService;
import com.spacenx.space.entity.Space;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequiredArgsConstructor
@RequestMapping("/spaces/{spaceKey}/board")
public class BoardController {

    private final IssueService issueService;
    private final SpaceService spaceService;
    private final SprintService sprintService;
    private final UserService userService;

    @GetMapping
    public String board(@PathVariable String spaceKey,
                        @AuthenticationPrincipal UserDetails userDetails,
                        Model model) {
        User currentUser = userService.findByUsername(userDetails.getUsername());
        Space space = spaceService.getSpaceByKey(spaceKey);
        Optional<Sprint> activeSprint = sprintService.getActiveSprint(space.getId());

        Map<IssueStatus, List<Issue>> issuesByStatus = new LinkedHashMap<>();
        if (activeSprint.isPresent()) {
            List<Issue> sprintIssues = issueService.getIssuesBySpaceAndSprintWithAssignee(
                    space.getId(), activeSprint.get().getId());
            for (IssueStatus status : IssueStatus.values()) {
                issuesByStatus.put(status, sprintIssues.stream()
                        .filter(i -> i.getStatus() == status)
                        .toList());
            }
        } else {
            for (IssueStatus status : IssueStatus.values()) {
                issuesByStatus.put(status,
                        issueService.getIssuesBySpaceAndStatusWithAssignee(space.getId(), status));
            }
        }

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentSpace", space);
        model.addAttribute("activeSprint", activeSprint.orElse(null));
        model.addAttribute("sprints", sprintService.getSprintsBySpace(space.getId()));
        model.addAttribute("issuesByStatus", issuesByStatus);
        model.addAttribute("todoIssues", issuesByStatus.getOrDefault(IssueStatus.TODO, List.of()));
        model.addAttribute("inProgressIssues", issuesByStatus.getOrDefault(IssueStatus.IN_PROGRESS, List.of()));
        model.addAttribute("inReviewIssues", issuesByStatus.getOrDefault(IssueStatus.IN_REVIEW, List.of()));
        model.addAttribute("doneIssues", issuesByStatus.getOrDefault(IssueStatus.DONE, List.of()));
        model.addAttribute("activeTab", "board");
        return "board/index";
    }

    @PostMapping("/move")
    public String moveIssue(@PathVariable String spaceKey,
                            @RequestParam Long issueId,
                            @RequestParam IssueStatus status,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        try {
            issueService.updateIssueStatus(issueId, status);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/spaces/" + spaceKey + "/board";
    }
}
