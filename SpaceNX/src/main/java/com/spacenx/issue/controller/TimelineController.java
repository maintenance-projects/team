package com.spacenx.issue.controller;

import com.spacenx.issue.dto.UpdateIssueDatesRequest;
import com.spacenx.issue.entity.Issue;
import com.spacenx.issue.service.IssueService;
import com.spacenx.space.entity.Space;
import com.spacenx.space.service.SpaceService;
import com.spacenx.sprint.entity.Sprint;
import com.spacenx.sprint.service.SprintService;
import com.spacenx.user.entity.User;
import com.spacenx.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/spaces/{spaceKey}/timeline")
public class TimelineController {

    private final IssueService issueService;
    private final SpaceService spaceService;
    private final SprintService sprintService;
    private final UserService userService;

    @GetMapping
    public String timeline(@PathVariable String spaceKey,
                           @AuthenticationPrincipal UserDetails userDetails,
                           Model model) {
        User currentUser = userService.findByUsername(userDetails.getUsername());
        Space space = spaceService.getSpaceByKey(spaceKey);
        List<Issue> issues = issueService.getIssuesBySpaceWithAssignee(space.getId()).stream()
                .filter(i -> i.getStartDate() != null || i.getDueDate() != null)
                .toList();
        List<Sprint> sprints = sprintService.getSprintsBySpace(space.getId());

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentSpace", space);
        model.addAttribute("issues", issues);
        model.addAttribute("sprints", sprints);
        model.addAttribute("activeTab", "timeline");
        return "timeline/index";
    }

    @GetMapping("/data")
    @ResponseBody
    public Map<String, Object> timelineData(@PathVariable String spaceKey) {
        Space space = spaceService.getSpaceByKey(spaceKey);
        List<Issue> issues = issueService.getIssuesBySpaceWithAssignee(space.getId()).stream()
                .filter(i -> i.getStartDate() != null || i.getDueDate() != null)
                .toList();
        List<Sprint> sprints = sprintService.getSprintsBySpace(space.getId());

        Map<String, Object> data = new HashMap<>();

        List<Map<String, Object>> issueItems = issues.stream()
                .map(issue -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", issue.getId());
                    item.put("issueKey", issue.getIssueKey());
                    item.put("title", issue.getTitle());
                    item.put("status", issue.getStatus().name());
                    item.put("priority", issue.getPriority() != null ? issue.getPriority().name() : null);
                    item.put("issueType", issue.getIssueType().name());
                    item.put("startDate", issue.getStartDate() != null ? issue.getStartDate().toString() : null);
                    item.put("dueDate", issue.getDueDate() != null ? issue.getDueDate().toString() : null);
                    item.put("assignee", issue.getAssignee() != null ? issue.getAssignee().getDisplayName() : null);
                    return item;
                })
                .collect(Collectors.toList());

        List<Map<String, Object>> sprintItems = sprints.stream()
                .map(sprint -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", sprint.getId());
                    item.put("name", sprint.getName());
                    item.put("status", sprint.getStatus().name());
                    item.put("startDate", sprint.getStartDate() != null ? sprint.getStartDate().toString() : null);
                    item.put("endDate", sprint.getEndDate() != null ? sprint.getEndDate().toString() : null);
                    return item;
                })
                .collect(Collectors.toList());

        data.put("issues", issueItems);
        data.put("sprints", sprintItems);

        // Calculate overall date range for the timeline
        LocalDate minDate = LocalDate.now();
        LocalDate maxDate = LocalDate.now().plusWeeks(4);
        for (Issue issue : issues) {
            if (issue.getStartDate() != null && issue.getStartDate().isBefore(minDate)) {
                minDate = issue.getStartDate();
            }
            if (issue.getDueDate() != null && issue.getDueDate().isAfter(maxDate)) {
                maxDate = issue.getDueDate();
            }
        }
        // Add padding of 1 week on each side
        data.put("startDate", minDate.minusWeeks(1).toString());
        data.put("endDate", maxDate.plusWeeks(1).toString());

        return data;
    }

    @PostMapping("/update-dates")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateIssueDates(
            @PathVariable String spaceKey,
            @RequestBody UpdateIssueDatesRequest request) {
        try {
            Issue updated = issueService.updateIssueDates(
                    request.getIssueId(), request.getStartDate(), request.getEndDate());
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("issueId", updated.getId());
            response.put("startDate", updated.getStartDate() != null ? updated.getStartDate().toString() : null);
            response.put("endDate", updated.getDueDate() != null ? updated.getDueDate().toString() : null);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}
