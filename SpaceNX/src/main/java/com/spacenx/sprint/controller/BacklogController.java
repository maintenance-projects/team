package com.spacenx.sprint.controller;

import com.spacenx.issue.dto.UpdateIssueRequest;
import com.spacenx.issue.entity.Issue;
import com.spacenx.issue.service.IssueService;
import com.spacenx.space.entity.Space;
import com.spacenx.space.service.SpaceService;
import com.spacenx.sprint.dto.CreateSprintRequest;
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

import java.util.List;

@Controller
@RequiredArgsConstructor
@RequestMapping("/spaces/{spaceKey}")
public class BacklogController {

    private final SprintService sprintService;
    private final IssueService issueService;
    private final SpaceService spaceService;
    private final UserService userService;

    @GetMapping("/backlog")
    public String backlog(@PathVariable String spaceKey,
                          @AuthenticationPrincipal UserDetails userDetails,
                          Model model) {
        User currentUser = userService.findByUsername(userDetails.getUsername());
        Space space = spaceService.getSpaceByKey(spaceKey);
        List<Sprint> sprints = sprintService.getSprintsBySpaceWithIssues(space.getId());
        List<Issue> backlogIssues = issueService.getBacklogIssuesWithAssignee(space.getId());

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentSpace", space);
        model.addAttribute("sprints", sprints);
        model.addAttribute("backlogIssues", backlogIssues);
        model.addAttribute("createSprintRequest", new CreateSprintRequest());
        model.addAttribute("activeTab", "backlog");
        return "backlog/index";
    }

    @PostMapping("/sprints")
    public String createSprint(@PathVariable String spaceKey,
                               @ModelAttribute CreateSprintRequest request,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        try {
            Space space = spaceService.getSpaceByKey(spaceKey);
            sprintService.createSprint(space.getId(), request);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/spaces/" + spaceKey + "/backlog";
    }

    @PostMapping("/sprints/{sprintId}/start")
    public String startSprint(@PathVariable String spaceKey,
                              @PathVariable Long sprintId,
                              RedirectAttributes redirectAttributes) {
        try {
            sprintService.startSprint(sprintId);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/spaces/" + spaceKey + "/backlog";
    }

    @PostMapping("/sprints/{sprintId}/complete")
    public String completeSprint(@PathVariable String spaceKey,
                                 @PathVariable Long sprintId,
                                 RedirectAttributes redirectAttributes) {
        try {
            sprintService.completeSprint(sprintId);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/spaces/" + spaceKey + "/backlog";
    }

    @PostMapping("/issues/{issueId}/assign-sprint")
    public String assignSprint(@PathVariable String spaceKey,
                               @PathVariable Long issueId,
                               @RequestParam(required = false) Long sprintId,
                               RedirectAttributes redirectAttributes) {
        try {
            UpdateIssueRequest updateRequest = new UpdateIssueRequest();
            updateRequest.setSprintId(sprintId);
            issueService.updateIssue(issueId, updateRequest);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/spaces/" + spaceKey + "/backlog";
    }
}
