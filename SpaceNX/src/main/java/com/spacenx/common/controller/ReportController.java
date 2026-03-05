package com.spacenx.common.controller;

import com.spacenx.issue.entity.Issue;
import com.spacenx.issue.enums.IssueStatus;
import com.spacenx.issue.service.IssueService;
import com.spacenx.space.entity.Space;
import com.spacenx.space.service.SpaceService;
import com.spacenx.sprint.entity.Sprint;
import com.spacenx.sprint.enums.SprintStatus;
import com.spacenx.sprint.service.SprintService;
import com.spacenx.user.entity.User;
import com.spacenx.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@RequestMapping("/spaces/{spaceKey}/reports")
public class ReportController {

    private final IssueService issueService;
    private final SpaceService spaceService;
    private final SprintService sprintService;
    private final UserService userService;

    @GetMapping
    public String reports(@PathVariable String spaceKey,
                          @AuthenticationPrincipal UserDetails userDetails,
                          Model model) {
        User currentUser = userService.findByUsername(userDetails.getUsername());
        Space space = spaceService.getSpaceByKey(spaceKey);
        Map<IssueStatus, Long> issueStats = issueService.getIssueCountByStatus(space.getId());

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentSpace", space);
        model.addAttribute("issueStats", issueStats);
        model.addAttribute("activeTab", "reports");
        return "report/index";
    }

    @GetMapping("/data")
    @ResponseBody
    public Map<String, Object> reportData(@PathVariable String spaceKey) {
        Space space = spaceService.getSpaceByKey(spaceKey);
        Map<String, Object> data = new HashMap<>();

        // Issue counts by status
        Map<IssueStatus, Long> statusCounts = issueService.getIssueCountByStatus(space.getId());
        data.put("statusDistribution", statusCounts);

        // Issue counts by priority
        List<Issue> allIssues = issueService.getIssuesBySpaceWithAssignee(space.getId());
        Map<String, Long> priorityCounts = allIssues.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getPriority() != null ? i.getPriority().name() : "NONE",
                        Collectors.counting()
                ));
        data.put("priorityDistribution", priorityCounts);

        // Issue counts by assignee
        Map<String, Long> assigneeCounts = allIssues.stream()
                .collect(Collectors.groupingBy(
                        i -> i.getAssignee() != null ? i.getAssignee().getDisplayName() : "Unassigned",
                        Collectors.counting()
                ));
        data.put("assigneeWorkload", assigneeCounts);

        // Sprint progress - template expects an object with hasActiveSprint boolean
        List<Sprint> sprints = sprintService.getSprintsBySpace(space.getId());
        Map<String, Object> sprintProgress = new HashMap<>();
        Sprint activeSprint = sprints.stream()
                .filter(s -> s.getStatus() == SprintStatus.ACTIVE)
                .findFirst()
                .orElse(null);

        List<Issue> activeSprintIssues = null;

        if (activeSprint != null) {
            sprintProgress.put("hasActiveSprint", true);
            sprintProgress.put("name", activeSprint.getName());
            activeSprintIssues = issueService.getIssuesBySpaceAndSprintWithAssignee(space.getId(), activeSprint.getId());
            long doneCount = activeSprintIssues.stream()
                    .filter(i -> i.getStatus() == IssueStatus.DONE)
                    .count();
            long inProgressCount = activeSprintIssues.stream()
                    .filter(i -> i.getStatus() == IssueStatus.IN_PROGRESS || i.getStatus() == IssueStatus.IN_REVIEW)
                    .count();
            long todoCount = activeSprintIssues.stream()
                    .filter(i -> i.getStatus() == IssueStatus.TODO)
                    .count();
            sprintProgress.put("totalIssues", activeSprintIssues.size());
            sprintProgress.put("doneIssues", doneCount);
            sprintProgress.put("done", doneCount);
            sprintProgress.put("inProgress", inProgressCount);
            sprintProgress.put("todo", todoCount);
        } else {
            sprintProgress.put("hasActiveSprint", false);
        }
        data.put("sprintProgress", sprintProgress);

        // Burndown & Burnup chart data (based on active sprint)
        data.put("burndown", buildBurndownData(activeSprint, activeSprintIssues));
        data.put("burnup", buildBurnupData(activeSprint, activeSprintIssues));

        // Velocity chart data (based on completed sprints + active sprint)
        data.put("velocity", buildVelocityData(space.getId()));

        return data;
    }

    /**
     * Build burndown chart data for the active sprint.
     * Uses story points if available, otherwise falls back to issue count.
     * Approximates daily completion from issue updatedAt timestamps.
     */
    private Map<String, Object> buildBurndownData(Sprint activeSprint, List<Issue> sprintIssues) {
        Map<String, Object> burndown = new HashMap<>();

        if (activeSprint == null || sprintIssues == null || sprintIssues.isEmpty()) {
            burndown.put("hasData", false);
            return burndown;
        }

        LocalDate startDate = activeSprint.getStartDate();
        LocalDate endDate = activeSprint.getEndDate();
        if (startDate == null || endDate == null) {
            burndown.put("hasData", false);
            return burndown;
        }

        burndown.put("hasData", true);
        burndown.put("sprintName", activeSprint.getName());

        boolean useStoryPoints = sprintIssues.stream().anyMatch(i -> i.getStoryPoints() != null && i.getStoryPoints() > 0);

        int totalWork = useStoryPoints
                ? sprintIssues.stream().mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0).sum()
                : sprintIssues.size();

        burndown.put("useStoryPoints", useStoryPoints);
        burndown.put("totalWork", totalWork);

        // Build daily remaining work
        LocalDate today = LocalDate.now();
        LocalDate chartEnd = endDate.isBefore(today) ? endDate : today;
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;

        List<String> labels = new ArrayList<>();
        List<Number> idealLine = new ArrayList<>();
        List<Number> actualLine = new ArrayList<>();

        // Collect done issues with their completion date (approximated by updatedAt)
        Map<LocalDate, Integer> completedPerDay = new HashMap<>();
        for (Issue issue : sprintIssues) {
            if (issue.getStatus() == IssueStatus.DONE && issue.getUpdatedAt() != null) {
                LocalDate completedDate = issue.getUpdatedAt().toLocalDate();
                int points = useStoryPoints ? (issue.getStoryPoints() != null ? issue.getStoryPoints() : 0) : 1;
                completedPerDay.merge(completedDate, points, Integer::sum);
            }
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd");
        int cumulativeCompleted = 0;

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            labels.add(date.format(fmt));

            // Ideal burndown: linear from totalWork to 0
            long dayIndex = ChronoUnit.DAYS.between(startDate, date);
            double idealRemaining = totalWork - ((double) totalWork * dayIndex / (totalDays - 1));
            idealLine.add(Math.round(idealRemaining * 100.0) / 100.0);

            // Actual line: only up to today
            if (!date.isAfter(chartEnd)) {
                cumulativeCompleted += completedPerDay.getOrDefault(date, 0);
                actualLine.add(totalWork - cumulativeCompleted);
            }
        }

        burndown.put("labels", labels);
        burndown.put("idealLine", idealLine);
        burndown.put("actualLine", actualLine);

        return burndown;
    }

    /**
     * Build burnup chart data for the active sprint.
     * Shows cumulative completed work over time, plus total scope line.
     */
    private Map<String, Object> buildBurnupData(Sprint activeSprint, List<Issue> sprintIssues) {
        Map<String, Object> burnup = new HashMap<>();

        if (activeSprint == null || sprintIssues == null || sprintIssues.isEmpty()) {
            burnup.put("hasData", false);
            return burnup;
        }

        LocalDate startDate = activeSprint.getStartDate();
        LocalDate endDate = activeSprint.getEndDate();
        if (startDate == null || endDate == null) {
            burnup.put("hasData", false);
            return burnup;
        }

        burnup.put("hasData", true);
        burnup.put("sprintName", activeSprint.getName());

        boolean useStoryPoints = sprintIssues.stream().anyMatch(i -> i.getStoryPoints() != null && i.getStoryPoints() > 0);

        int totalWork = useStoryPoints
                ? sprintIssues.stream().mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0).sum()
                : sprintIssues.size();

        burnup.put("useStoryPoints", useStoryPoints);
        burnup.put("totalWork", totalWork);

        LocalDate today = LocalDate.now();
        LocalDate chartEnd = endDate.isBefore(today) ? endDate : today;

        List<String> labels = new ArrayList<>();
        List<Number> scopeLine = new ArrayList<>();
        List<Number> completedLine = new ArrayList<>();

        // Collect done issues with their completion date
        Map<LocalDate, Integer> completedPerDay = new HashMap<>();
        for (Issue issue : sprintIssues) {
            if (issue.getStatus() == IssueStatus.DONE && issue.getUpdatedAt() != null) {
                LocalDate completedDate = issue.getUpdatedAt().toLocalDate();
                int points = useStoryPoints ? (issue.getStoryPoints() != null ? issue.getStoryPoints() : 0) : 1;
                completedPerDay.merge(completedDate, points, Integer::sum);
            }
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM/dd");
        int cumulativeCompleted = 0;

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            labels.add(date.format(fmt));
            scopeLine.add(totalWork);

            if (!date.isAfter(chartEnd)) {
                cumulativeCompleted += completedPerDay.getOrDefault(date, 0);
                completedLine.add(cumulativeCompleted);
            }
        }

        burnup.put("labels", labels);
        burnup.put("scopeLine", scopeLine);
        burnup.put("completedLine", completedLine);

        return burnup;
    }

    /**
     * Build velocity chart data from completed sprints (and optionally the active sprint).
     * Shows story points (or issue count) completed per sprint.
     */
    private Map<String, Object> buildVelocityData(Long spaceId) {
        Map<String, Object> velocity = new HashMap<>();

        List<Sprint> completedSprints = sprintService.getCompletedSprints(spaceId);
        Optional<Sprint> activeSprintOpt = sprintService.getActiveSprint(spaceId);

        List<Sprint> velocitySprints = new ArrayList<>(completedSprints);
        activeSprintOpt.ifPresent(velocitySprints::add);

        if (velocitySprints.isEmpty()) {
            velocity.put("hasData", false);
            return velocity;
        }

        velocity.put("hasData", true);

        List<String> sprintNames = new ArrayList<>();
        List<Number> completedPoints = new ArrayList<>();
        List<Number> totalPoints = new ArrayList<>();

        for (Sprint sprint : velocitySprints) {
            sprintNames.add(sprint.getName());
            List<Issue> sprintIssues = issueService.getIssuesBySpaceAndSprint(spaceId, sprint.getId());

            boolean useStoryPoints = sprintIssues.stream().anyMatch(i -> i.getStoryPoints() != null && i.getStoryPoints() > 0);

            int completed = sprintIssues.stream()
                    .filter(i -> i.getStatus() == IssueStatus.DONE)
                    .mapToInt(i -> useStoryPoints ? (i.getStoryPoints() != null ? i.getStoryPoints() : 0) : 1)
                    .sum();

            int total = useStoryPoints
                    ? sprintIssues.stream().mapToInt(i -> i.getStoryPoints() != null ? i.getStoryPoints() : 0).sum()
                    : sprintIssues.size();

            completedPoints.add(completed);
            totalPoints.add(total);
        }

        velocity.put("sprintNames", sprintNames);
        velocity.put("completedPoints", completedPoints);
        velocity.put("totalPoints", totalPoints);

        return velocity;
    }
}
