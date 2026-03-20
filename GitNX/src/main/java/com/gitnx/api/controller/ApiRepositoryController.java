package com.gitnx.api.controller;

import com.gitnx.api.dto.CreateBranchRequest;
import com.gitnx.repository.dto.*;
import com.gitnx.repository.entity.GitRepository;
import com.gitnx.repository.service.BranchService;
import com.gitnx.repository.service.CodeBrowserService;
import com.gitnx.repository.service.CommitService;
import com.gitnx.repository.service.GitRepositoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/repositories")
@RequiredArgsConstructor
public class ApiRepositoryController {

    private final GitRepositoryService gitRepositoryService;
    private final CommitService commitService;
    private final BranchService branchService;
    private final CodeBrowserService codeBrowserService;

    @GetMapping
    public ResponseEntity<List<RepositoryDto>> listRepositories() {
        return ResponseEntity.ok(gitRepositoryService.listAll());
    }

    @GetMapping("/{owner}/{repo}")
    public ResponseEntity<RepositoryDto> getRepository(
            @PathVariable String owner,
            @PathVariable String repo) {
        GitRepository gitRepo = gitRepositoryService.getByOwnerAndName(owner, repo);
        return ResponseEntity.ok(RepositoryDto.from(gitRepo));
    }

    @GetMapping("/{owner}/{repo}/commits")
    public ResponseEntity<List<CommitDto>> getCommits(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam(required = false) String branch,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size) {
        if (branch == null || branch.isBlank()) {
            GitRepository gitRepo = gitRepositoryService.getByOwnerAndName(owner, repo);
            branch = gitRepo.getDefaultBranch();
        }
        return ResponseEntity.ok(commitService.getCommitLog(owner, repo, branch, page, size));
    }

    @GetMapping("/{owner}/{repo}/branches")
    public ResponseEntity<List<BranchDto>> listBranches(
            @PathVariable String owner,
            @PathVariable String repo) {
        return ResponseEntity.ok(branchService.listBranches(owner, repo));
    }

    @PostMapping("/{owner}/{repo}/branches")
    public ResponseEntity<BranchDto> createBranch(
            @PathVariable String owner,
            @PathVariable String repo,
            @Valid @RequestBody CreateBranchRequest request) {
        BranchDto branch = branchService.createBranch(owner, repo, request.getName(), request.getSourceBranch());
        return ResponseEntity.status(HttpStatus.CREATED).body(branch);
    }

    @DeleteMapping("/{owner}/{repo}")
    public ResponseEntity<Void> deleteRepository(
            @PathVariable String owner,
            @PathVariable String repo) {
        gitRepositoryService.delete(owner, repo);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{owner}/{repo}/tree")
    public ResponseEntity<List<FileTreeEntry>> getTree(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam(required = false) String ref,
            @RequestParam(required = false, defaultValue = "") String path) {
        String branch = (ref == null || ref.isBlank())
                ? gitRepositoryService.getByOwnerAndName(owner, repo).getDefaultBranch()
                : ref;
        List<FileTreeEntry> entries = codeBrowserService.getTree(owner, repo, branch, path);
        return ResponseEntity.ok(entries);
    }

    @GetMapping("/{owner}/{repo}/blob")
    public ResponseEntity<FileContentDto> getBlob(
            @PathVariable String owner,
            @PathVariable String repo,
            @RequestParam(required = false) String ref,
            @RequestParam String path) {
        String branch = (ref == null || ref.isBlank())
                ? gitRepositoryService.getByOwnerAndName(owner, repo).getDefaultBranch()
                : ref;
        FileContentDto content = codeBrowserService.getFileContent(owner, repo, branch, path);
        return ResponseEntity.ok(content);
    }
}
