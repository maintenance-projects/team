package com.gitnx.repository.controller;

import com.gitnx.repository.dto.BranchDto;
import com.gitnx.repository.dto.CommitDto;
import com.gitnx.repository.dto.DiffEntryDto;
import com.gitnx.repository.service.BranchService;
import com.gitnx.repository.service.CommitService;
import com.gitnx.repository.service.GitRepositoryService;
import com.gitnx.repository.service.RepositoryMemberService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class CommitController {

    private final CommitService commitService;
    private final BranchService branchService;
    private final GitRepositoryService gitRepositoryService;
    private final RepositoryMemberService memberService;

    @GetMapping("/{owner}/{repo}/commits/{branch}")
    public String commits(@PathVariable String owner, @PathVariable String repo,
                          @PathVariable String branch,
                          @RequestParam(defaultValue = "0") int page,
                          @AuthenticationPrincipal UserDetails userDetails,
                          Model model) {
        List<CommitDto> commits = commitService.getCommitLog(owner, repo, branch, page, 30);
        List<BranchDto> branches = branchService.listBranches(owner, repo);

        model.addAttribute("owner", owner);
        model.addAttribute("repo", repo);
        model.addAttribute("gitRepo", gitRepositoryService.getByOwnerAndName(owner, repo));
        model.addAttribute("currentBranch", branch);
        model.addAttribute("branches", branches);
        model.addAttribute("commits", commits);
        model.addAttribute("currentPage", page);
        model.addAttribute("hasMore", commits.size() == 30);
        model.addAttribute("activeTab", "commits");
        model.addAttribute("isRepoOwner",
                userDetails != null && memberService.isOwner(owner, repo, userDetails.getUsername()));

        return "code/commits";
    }

    @GetMapping("/{owner}/{repo}/commit/{hash}")
    public String commitDetail(@PathVariable String owner, @PathVariable String repo,
                               @PathVariable String hash,
                               @AuthenticationPrincipal UserDetails userDetails,
                               Model model) {
        CommitDto commit = commitService.getCommit(owner, repo, hash);
        List<DiffEntryDto> diffs = commitService.getCommitDiff(owner, repo, hash);

        model.addAttribute("owner", owner);
        model.addAttribute("repo", repo);
        model.addAttribute("gitRepo", gitRepositoryService.getByOwnerAndName(owner, repo));
        model.addAttribute("commit", commit);
        model.addAttribute("diffs", diffs);
        model.addAttribute("activeTab", "commits");
        model.addAttribute("isRepoOwner",
                userDetails != null && memberService.isOwner(owner, repo, userDetails.getUsername()));

        return "code/commit-detail";
    }
}
