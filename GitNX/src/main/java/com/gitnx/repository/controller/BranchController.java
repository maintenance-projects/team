package com.gitnx.repository.controller;

import com.gitnx.repository.dto.BranchDto;
import com.gitnx.repository.service.BranchService;
import com.gitnx.repository.service.GitRepositoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;
    private final GitRepositoryService gitRepositoryService;

    @GetMapping("/{owner}/{repo}/branches")
    public String branches(@PathVariable String owner, @PathVariable String repo, Model model) {
        List<BranchDto> branches = branchService.listBranches(owner, repo);

        model.addAttribute("owner", owner);
        model.addAttribute("repo", repo);
        model.addAttribute("gitRepo", gitRepositoryService.getByOwnerAndName(owner, repo));
        model.addAttribute("branches", branches);
        model.addAttribute("activeTab", "branches");

        return "code/branches";
    }
}
