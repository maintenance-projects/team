package com.gitnx.mergerequest.controller;

import com.gitnx.mergerequest.entity.MergeRequest;
import com.gitnx.mergerequest.enums.MergeRequestState;
import com.gitnx.mergerequest.service.MergeRequestService;
import com.gitnx.mergerequest.service.MergeService;
import com.gitnx.repository.dto.DiffEntryDto;
import com.gitnx.repository.service.BranchService;
import com.gitnx.repository.service.CommitService;
import com.gitnx.repository.service.GitRepositoryService;
import lombok.RequiredArgsConstructor;
import org.eclipse.jgit.api.MergeResult;
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
public class MergeRequestController {

    private final MergeRequestService mergeRequestService;
    private final MergeService mergeService;
    private final GitRepositoryService gitRepositoryService;
    private final BranchService branchService;
    private final CommitService commitService;

    @GetMapping("/{owner}/{repo}/merge-requests")
    public String list(@PathVariable String owner, @PathVariable String repo,
                       @RequestParam(required = false, defaultValue = "OPEN") String state,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        MergeRequestState mrState = "ALL".equals(state) ? null : MergeRequestState.valueOf(state);
        Page<MergeRequest> mergeRequests = mergeRequestService.list(owner, repo, mrState, page);

        model.addAttribute("owner", owner);
        model.addAttribute("repo", repo);
        model.addAttribute("gitRepo", gitRepositoryService.getByOwnerAndName(owner, repo));
        model.addAttribute("mergeRequests", mergeRequests);
        model.addAttribute("currentState", state);
        model.addAttribute("openCount", mergeRequestService.countByState(owner, repo, MergeRequestState.OPEN));
        model.addAttribute("mergedCount", mergeRequestService.countByState(owner, repo, MergeRequestState.MERGED));
        model.addAttribute("closedCount", mergeRequestService.countByState(owner, repo, MergeRequestState.CLOSED));
        model.addAttribute("activeTab", "mergerequests");

        return "mergerequest/list";
    }

    @GetMapping("/{owner}/{repo}/merge-requests/new")
    public String newMrForm(@PathVariable String owner, @PathVariable String repo, Model model) {
        model.addAttribute("owner", owner);
        model.addAttribute("repo", repo);
        model.addAttribute("gitRepo", gitRepositoryService.getByOwnerAndName(owner, repo));
        model.addAttribute("branches", branchService.listBranches(owner, repo));
        model.addAttribute("activeTab", "mergerequests");
        return "mergerequest/new";
    }

    @PostMapping("/{owner}/{repo}/merge-requests")
    public String create(@PathVariable String owner, @PathVariable String repo,
                         @RequestParam String title,
                         @RequestParam(required = false) String description,
                         @RequestParam String sourceBranch,
                         @RequestParam String targetBranch,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        if (sourceBranch.equals(targetBranch)) {
            redirectAttributes.addFlashAttribute("error", "Source and target branches must be different");
            return "redirect:/" + owner + "/" + repo + "/merge-requests/new";
        }

        MergeRequest mr = mergeRequestService.create(owner, repo, title, description,
                sourceBranch, targetBranch, userDetails.getUsername());
        return "redirect:/" + owner + "/" + repo + "/merge-requests/" + mr.getMrNumber();
    }

    @GetMapping("/{owner}/{repo}/merge-requests/{number}")
    public String detail(@PathVariable String owner, @PathVariable String repo,
                         @PathVariable int number, Model model) {
        MergeRequest mr = mergeRequestService.getByNumber(owner, repo, number);

        List<DiffEntryDto> diffs = List.of();
        boolean canMerge = false;

        if (mr.getState() == MergeRequestState.OPEN) {
            try {
                diffs = commitService.getBranchDiff(owner, repo, mr.getSourceBranch(), mr.getTargetBranch());
                canMerge = mergeService.canMerge(owner, repo, mr.getSourceBranch(), mr.getTargetBranch());
            } catch (Exception e) {
                model.addAttribute("diffError", "Could not compute diff: " + e.getMessage());
            }
        }

        model.addAttribute("owner", owner);
        model.addAttribute("repo", repo);
        model.addAttribute("gitRepo", gitRepositoryService.getByOwnerAndName(owner, repo));
        model.addAttribute("mr", mr);
        model.addAttribute("diffs", diffs);
        model.addAttribute("canMerge", canMerge);
        model.addAttribute("activeTab", "mergerequests");

        return "mergerequest/detail";
    }

    @PostMapping("/{owner}/{repo}/merge-requests/{number}/comment")
    public String comment(@PathVariable String owner, @PathVariable String repo,
                          @PathVariable int number,
                          @RequestParam String body,
                          @RequestParam(required = false) String filePath,
                          @RequestParam(required = false) Integer lineNumber,
                          @AuthenticationPrincipal UserDetails userDetails) {
        mergeRequestService.addComment(owner, repo, number, body, filePath, lineNumber,
                userDetails.getUsername());
        return "redirect:/" + owner + "/" + repo + "/merge-requests/" + number;
    }

    @PostMapping("/{owner}/{repo}/merge-requests/{number}/approve")
    public String approve(@PathVariable String owner, @PathVariable String repo,
                          @PathVariable int number,
                          @AuthenticationPrincipal UserDetails userDetails) {
        mergeRequestService.approve(owner, repo, number, userDetails.getUsername());
        return "redirect:/" + owner + "/" + repo + "/merge-requests/" + number;
    }

    @PostMapping("/{owner}/{repo}/merge-requests/{number}/request-changes")
    public String requestChanges(@PathVariable String owner, @PathVariable String repo,
                                  @PathVariable int number,
                                  @AuthenticationPrincipal UserDetails userDetails) {
        mergeRequestService.requestChanges(owner, repo, number, userDetails.getUsername());
        return "redirect:/" + owner + "/" + repo + "/merge-requests/" + number;
    }

    @PostMapping("/{owner}/{repo}/merge-requests/{number}/merge")
    public String merge(@PathVariable String owner, @PathVariable String repo,
                        @PathVariable int number,
                        @AuthenticationPrincipal UserDetails userDetails,
                        RedirectAttributes redirectAttributes) {
        try {
            MergeResult result = mergeService.merge(owner, repo, number, userDetails.getUsername());
            if (result.getMergeStatus().isSuccessful()) {
                redirectAttributes.addFlashAttribute("success", "Merge request successfully merged!");
            } else {
                redirectAttributes.addFlashAttribute("error",
                        "Merge failed: " + result.getMergeStatus());
            }
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Merge failed: " + e.getMessage());
        }
        return "redirect:/" + owner + "/" + repo + "/merge-requests/" + number;
    }

    @PostMapping("/{owner}/{repo}/merge-requests/{number}/close")
    public String close(@PathVariable String owner, @PathVariable String repo,
                        @PathVariable int number) {
        mergeRequestService.close(owner, repo, number);
        return "redirect:/" + owner + "/" + repo + "/merge-requests/" + number;
    }

    @PostMapping("/{owner}/{repo}/merge-requests/{number}/reopen")
    public String reopen(@PathVariable String owner, @PathVariable String repo,
                         @PathVariable int number) {
        mergeRequestService.reopen(owner, repo, number);
        return "redirect:/" + owner + "/" + repo + "/merge-requests/" + number;
    }
}
