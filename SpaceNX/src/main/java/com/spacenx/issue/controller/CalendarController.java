package com.spacenx.issue.controller;

import com.spacenx.issue.dto.UpdateIssueDatesRequest;
import com.spacenx.issue.entity.Issue;
import com.spacenx.issue.enums.IssuePriority;
import com.spacenx.issue.service.IssueService;
import com.spacenx.space.entity.Space;
import com.spacenx.space.service.SpaceService;
import com.spacenx.user.entity.User;
import com.spacenx.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/spaces/{spaceKey}/calendar")
public class CalendarController {

    private final IssueService issueService;
    private final SpaceService spaceService;
    private final UserService userService;

    @GetMapping
    public String calendar(@PathVariable String spaceKey,
                           @AuthenticationPrincipal UserDetails userDetails,
                           Model model) {
        User currentUser = userService.findByUsername(userDetails.getUsername());
        Space space = spaceService.getSpaceByKey(spaceKey);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentSpace", space);
        model.addAttribute("activeTab", "calendar");
        return "calendar/index";
    }

    @GetMapping("/events")
    @ResponseBody
    public List<Map<String, Object>> calendarEvents(@PathVariable String spaceKey) {
        Space space = spaceService.getSpaceByKey(spaceKey);
        List<Issue> issues = issueService.getIssuesBySpace(space.getId()).stream()
                .filter(i -> i.getStartDate() != null || i.getDueDate() != null)
                .toList();

        return issues.stream()
                .map(issue -> {
                    Map<String, Object> event = new HashMap<>();
                    event.put("id", issue.getId());
                    event.put("title", issue.getIssueKey() + " " + issue.getTitle());
                    event.put("start", issue.getStartDate() != null
                            ? issue.getStartDate().toString()
                            : issue.getDueDate() != null ? issue.getDueDate().toString() : null);
                    event.put("end", issue.getDueDate() != null ? issue.getDueDate().toString() : null);
                    event.put("color", getPriorityColor(issue.getPriority()));
                    Map<String, Object> extendedProps = new HashMap<>();
                    extendedProps.put("issueKey", issue.getIssueKey());
                    extendedProps.put("status", issue.getStatus() != null ? issue.getStatus().name() : null);
                    event.put("extendedProps", extendedProps);
                    return event;
                })
                .collect(Collectors.toList());
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

    private String getPriorityColor(IssuePriority priority) {
        if (priority == null) {
            return "#6b7280";
        }
        return switch (priority) {
            case HIGHEST -> "#dc2626";
            case HIGH -> "#f97316";
            case MEDIUM -> "#eab308";
            case LOW -> "#22c55e";
            case LOWEST -> "#6b7280";
        };
    }
}
