package com.spacenx.issue.controller;

import com.spacenx.issue.dto.CreateCommentRequest;
import com.spacenx.issue.dto.CreateIssueRequest;
import com.spacenx.issue.dto.UpdateIssueRequest;
import com.spacenx.issue.entity.Attachment;
import com.spacenx.issue.entity.Issue;
import com.spacenx.issue.entity.IssueComment;
import com.spacenx.issue.entity.Label;
import com.spacenx.issue.enums.IssuePriority;
import com.spacenx.issue.enums.IssueStatus;
import com.spacenx.issue.enums.IssueType;
import com.spacenx.issue.service.AttachmentService;
import com.spacenx.issue.service.IssueService;
import com.spacenx.issue.service.LabelService;
import com.spacenx.space.entity.Space;
import com.spacenx.space.entity.SpaceMember;
import com.spacenx.space.service.SpaceService;
import com.spacenx.sprint.entity.Sprint;
import com.spacenx.sprint.service.SprintService;
import com.spacenx.user.entity.User;
import com.spacenx.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
@RequestMapping("/spaces/{spaceKey}/issues")
public class IssueController {

    private final IssueService issueService;
    private final SpaceService spaceService;
    private final UserService userService;
    private final LabelService labelService;
    private final SprintService sprintService;
    private final AttachmentService attachmentService;

    @GetMapping
    public String listIssues(@PathVariable String spaceKey,
                             @RequestParam(required = false) IssueStatus status,
                             @RequestParam(required = false) IssueType type,
                             @RequestParam(required = false) IssuePriority priority,
                             @RequestParam(required = false) Long assigneeId,
                             @RequestParam(required = false) Long sprintId,
                             @RequestParam(required = false) String search,
                             @AuthenticationPrincipal UserDetails userDetails,
                             Model model) {
        User currentUser = userService.findByUsername(userDetails.getUsername());
        Space space = spaceService.getSpaceByKey(spaceKey);

        List<Issue> issues = issueService.getIssuesBySpaceWithFilters(
                space.getId(), status, type, priority, assigneeId, sprintId, search);

        List<SpaceMember> members = spaceService.getMembers(space);
        List<Sprint> sprints = sprintService.getSprintsBySpace(space.getId());

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentSpace", space);
        model.addAttribute("issues", issues);
        model.addAttribute("members", members);
        model.addAttribute("sprints", sprints);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedPriority", priority);
        model.addAttribute("selectedAssigneeId", assigneeId);
        model.addAttribute("selectedSprintId", sprintId);
        model.addAttribute("selectedSearch", search);
        model.addAttribute("activeTab", "list");
        return "issue/list";
    }

    @GetMapping("/new")
    public String createForm(@PathVariable String spaceKey,
                             @RequestParam(required = false) String startDate,
                             @AuthenticationPrincipal UserDetails userDetails,
                             Model model) {
        User currentUser = userService.findByUsername(userDetails.getUsername());
        Space space = spaceService.getSpaceByKey(spaceKey);
        List<SpaceMember> members = spaceService.getMembers(space);
        List<Label> labels = labelService.getLabels(space.getId());
        List<Sprint> sprints = sprintService.getSprintsBySpace(space.getId());
        List<Issue> epics = issueService.getIssuesBySpaceAndStatus(space.getId(), IssueStatus.TODO)
                .stream()
                .filter(i -> i.getIssueType() == IssueType.EPIC)
                .toList();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentSpace", space);
        model.addAttribute("members", members);
        model.addAttribute("labels", labels);
        model.addAttribute("sprints", sprints);
        model.addAttribute("epics", epics);
        CreateIssueRequest createIssueRequest = new CreateIssueRequest();
        if (startDate != null && !startDate.isBlank()) {
            try {
                createIssueRequest.setStartDate(java.time.LocalDate.parse(startDate));
            } catch (Exception ignored) {
                // Invalid date format, ignore
            }
        }
        model.addAttribute("createIssueRequest", createIssueRequest);
        model.addAttribute("activeTab", "list");
        return "issue/create";
    }

    @PostMapping
    public String createIssue(@PathVariable String spaceKey,
                              @ModelAttribute CreateIssueRequest request,
                              @AuthenticationPrincipal UserDetails userDetails,
                              RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.findByUsername(userDetails.getUsername());
            Space space = spaceService.getSpaceByKey(spaceKey);
            Issue issue = issueService.createIssue(space.getId(), request, currentUser);
            return "redirect:/spaces/" + spaceKey + "/issues/" + issue.getIssueKey();
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/spaces/" + spaceKey + "/issues/new";
        }
    }

    @GetMapping("/{issueKey}")
    public String issueDetail(@PathVariable String spaceKey,
                              @PathVariable String issueKey,
                              @AuthenticationPrincipal UserDetails userDetails,
                              Model model) {
        User currentUser = userService.findByUsername(userDetails.getUsername());
        Space space = spaceService.getSpaceByKey(spaceKey);
        Issue issue = issueService.getIssueByKey(issueKey);
        List<IssueComment> comments = issueService.getComments(issue.getId());
        List<SpaceMember> members = spaceService.getMembers(space);
        List<Attachment> attachments = attachmentService.getAttachmentsByIssue(issue);
        List<Label> labels = labelService.getLabels(space.getId());
        List<Sprint> sprints = sprintService.getSprintsBySpace(space.getId());
        List<Issue> epics = issueService.getIssuesBySpaceAndStatus(space.getId(), IssueStatus.TODO)
                .stream()
                .filter(i -> i.getIssueType() == IssueType.EPIC && !i.getId().equals(issue.getId()))
                .toList();
        List<Long> issueLabelIds = issue.getLabels() != null
                ? issue.getLabels().stream().map(Label::getId).toList()
                : List.of();

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("currentSpace", space);
        model.addAttribute("issue", issue);
        model.addAttribute("comments", comments);
        model.addAttribute("members", members);
        model.addAttribute("attachments", attachments);
        model.addAttribute("labels", labels);
        model.addAttribute("sprints", sprints);
        model.addAttribute("epics", epics);
        model.addAttribute("issueLabelIds", issueLabelIds);
        model.addAttribute("createCommentRequest", new CreateCommentRequest());
        model.addAttribute("activeTab", "list");
        return "issue/detail";
    }

    @PostMapping("/{issueKey}/update")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> updateIssueField(
            @PathVariable String spaceKey,
            @PathVariable String issueKey,
            @RequestBody UpdateIssueRequest request) {
        try {
            Issue issue = issueService.getIssueByKey(issueKey);
            issueService.updateIssue(issue.getId(), request);
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    @PostMapping("/{issueKey}/comments")
    public String addComment(@PathVariable String spaceKey,
                             @PathVariable String issueKey,
                             @ModelAttribute CreateCommentRequest request,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.findByUsername(userDetails.getUsername());
            Issue issue = issueService.getIssueByKey(issueKey);
            issueService.addComment(issue.getId(), request, currentUser);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/spaces/" + spaceKey + "/issues/" + issueKey;
    }

    @PostMapping("/{issueKey}/status")
    public String updateStatus(@PathVariable String spaceKey,
                               @PathVariable String issueKey,
                               @RequestParam IssueStatus status,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        try {
            Issue issue = issueService.getIssueByKey(issueKey);
            issueService.updateIssueStatus(issue.getId(), status);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/spaces/" + spaceKey + "/issues/" + issueKey;
    }

    @PostMapping("/{issueKey}/attachments")
    public String uploadAttachment(@PathVariable String spaceKey,
                                   @PathVariable String issueKey,
                                   @RequestParam("files") List<MultipartFile> files,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.findByUsername(userDetails.getUsername());
            Issue issue = issueService.getIssueByKey(issueKey);
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    attachmentService.uploadFile(file, issue, currentUser);
                }
            }
            redirectAttributes.addFlashAttribute("success", "File(s) uploaded successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to upload file: " + e.getMessage());
        }
        return "redirect:/spaces/" + spaceKey + "/issues/" + issueKey;
    }

    @GetMapping("/{issueKey}/attachments/{id}/download")
    public ResponseEntity<Resource> downloadAttachment(@PathVariable String spaceKey,
                                                       @PathVariable String issueKey,
                                                       @PathVariable Long id) {
        Attachment attachment = attachmentService.getAttachment(id);
        Path filePath = attachmentService.getAttachmentFile(attachment);

        try {
            Resource resource = new UrlResource(filePath.toUri());
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(attachment.getContentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"" + attachment.getOriginalFilename() + "\"")
                    .body(resource);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Failed to read file: " + attachment.getOriginalFilename(), e);
        }
    }

    @PostMapping("/{issueKey}/attachments/{id}/delete")
    public String deleteAttachment(@PathVariable String spaceKey,
                                   @PathVariable String issueKey,
                                   @PathVariable Long id,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        try {
            attachmentService.deleteAttachment(id);
            redirectAttributes.addFlashAttribute("success", "Attachment deleted successfully.");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete attachment: " + e.getMessage());
        }
        return "redirect:/spaces/" + spaceKey + "/issues/" + issueKey;
    }
}
