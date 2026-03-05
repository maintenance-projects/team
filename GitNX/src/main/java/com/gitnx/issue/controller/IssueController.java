package com.gitnx.issue.controller;

import com.gitnx.issue.entity.Issue;
import com.gitnx.issue.entity.Label;
import com.gitnx.issue.enums.IssueState;
import com.gitnx.issue.service.IssueService;
import com.gitnx.repository.service.GitRepositoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;
    private final GitRepositoryService gitRepositoryService;

    @GetMapping("/{owner}/{repo}/issues")
    public String list(@PathVariable String owner, @PathVariable String repo,
                       @RequestParam(required = false, defaultValue = "OPEN") String state,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        IssueState issueState = "ALL".equals(state) ? null : IssueState.valueOf(state);
        Page<Issue> issues = issueService.list(owner, repo, issueState, page);

        model.addAttribute("owner", owner);
        model.addAttribute("repo", repo);
        model.addAttribute("gitRepo", gitRepositoryService.getByOwnerAndName(owner, repo));
        model.addAttribute("issues", issues);
        model.addAttribute("currentState", state);
        model.addAttribute("openCount", issueService.countByState(owner, repo, IssueState.OPEN));
        model.addAttribute("closedCount", issueService.countByState(owner, repo, IssueState.CLOSED));
        model.addAttribute("activeTab", "issues");

        return "issue/list";
    }

    @GetMapping("/{owner}/{repo}/issues/new")
    public String newIssueForm(@PathVariable String owner, @PathVariable String repo, Model model) {
        List<Label> labels = issueService.getLabels(owner, repo);
        model.addAttribute("owner", owner);
        model.addAttribute("repo", repo);
        model.addAttribute("gitRepo", gitRepositoryService.getByOwnerAndName(owner, repo));
        model.addAttribute("labels", labels);
        model.addAttribute("activeTab", "issues");
        return "issue/new";
    }

    @PostMapping("/{owner}/{repo}/issues")
    public String create(@PathVariable String owner, @PathVariable String repo,
                         @RequestParam String title, @RequestParam(required = false) String body,
                         @AuthenticationPrincipal UserDetails userDetails) {
        Issue issue = issueService.create(owner, repo, title, body, userDetails.getUsername());
        return "redirect:/" + owner + "/" + repo + "/issues/" + issue.getIssueNumber();
    }

    @GetMapping("/{owner}/{repo}/issues/{number}")
    public String detail(@PathVariable String owner, @PathVariable String repo,
                         @PathVariable int number, Model model) {
        Issue issue = issueService.getByNumber(owner, repo, number);
        List<Label> allLabels = issueService.getLabels(owner, repo);

        model.addAttribute("owner", owner);
        model.addAttribute("repo", repo);
        model.addAttribute("gitRepo", gitRepositoryService.getByOwnerAndName(owner, repo));
        model.addAttribute("issue", issue);
        model.addAttribute("allLabels", allLabels);
        model.addAttribute("activeTab", "issues");

        return "issue/detail";
    }

    @PostMapping("/{owner}/{repo}/issues/{number}/close")
    public String close(@PathVariable String owner, @PathVariable String repo,
                        @PathVariable int number) {
        issueService.close(owner, repo, number);
        return "redirect:/" + owner + "/" + repo + "/issues/" + number;
    }

    @PostMapping("/{owner}/{repo}/issues/{number}/reopen")
    public String reopen(@PathVariable String owner, @PathVariable String repo,
                         @PathVariable int number) {
        issueService.reopen(owner, repo, number);
        return "redirect:/" + owner + "/" + repo + "/issues/" + number;
    }

    @PostMapping("/{owner}/{repo}/issues/{number}/comment")
    public String comment(@PathVariable String owner, @PathVariable String repo,
                          @PathVariable int number, @RequestParam String body,
                          @AuthenticationPrincipal UserDetails userDetails) {
        issueService.addComment(owner, repo, number, body, userDetails.getUsername());
        return "redirect:/" + owner + "/" + repo + "/issues/" + number;
    }

    // Label management
    @GetMapping("/{owner}/{repo}/labels")
    public String labels(@PathVariable String owner, @PathVariable String repo, Model model) {
        List<Label> labels = issueService.getLabels(owner, repo);
        model.addAttribute("owner", owner);
        model.addAttribute("repo", repo);
        model.addAttribute("gitRepo", gitRepositoryService.getByOwnerAndName(owner, repo));
        model.addAttribute("labels", labels);
        model.addAttribute("activeTab", "issues");
        return "issue/labels";
    }

    @PostMapping("/{owner}/{repo}/labels")
    public String createLabel(@PathVariable String owner, @PathVariable String repo,
                              @RequestParam String name, @RequestParam String color,
                              @RequestParam(required = false) String description,
                              RedirectAttributes redirectAttributes) {
        try {
            issueService.createLabel(owner, repo, name, color, description);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/" + owner + "/" + repo + "/labels";
    }

    @PostMapping("/{owner}/{repo}/labels/{id}/delete")
    public String deleteLabel(@PathVariable String owner, @PathVariable String repo,
                              @PathVariable Long id) {
        issueService.deleteLabel(id);
        return "redirect:/" + owner + "/" + repo + "/labels";
    }
}
