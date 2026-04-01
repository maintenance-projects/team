package com.gitnx.organization.controller;

import com.gitnx.organization.entity.Organization;
import com.gitnx.organization.service.OrganizationService;
import com.gitnx.repository.dto.*;
import com.gitnx.repository.entity.GitRepository;
import com.gitnx.repository.service.BranchService;
import com.gitnx.repository.service.CodeBrowserService;
import com.gitnx.repository.service.CommitService;
import com.gitnx.repository.service.GitRepositoryService;
import com.gitnx.repository.service.RepositoryMemberService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Arrays;
import java.util.List;

/**
 * Organization 소속 레포지토리의 코드 브라우저.
 * URL: /org/{orgName}/{repo}, /org/{orgName}/{repo}/tree/..., etc.
 */
@Controller
@RequiredArgsConstructor
public class OrgRepoController {

    private final GitRepositoryService gitRepositoryService;
    private final OrganizationService organizationService;
    private final CodeBrowserService codeBrowserService;
    private final BranchService branchService;
    private final RepositoryMemberService memberService;
    private final CommitService commitService;

    @Value("${gitnx.clone.http-base-url}")
    private String httpBaseUrl;

    @GetMapping("/org/{orgName}/{repo}")
    public String repoRoot(@PathVariable String orgName, @PathVariable String repo,
                           @AuthenticationPrincipal UserDetails userDetails, Model model) {
        Organization org = organizationService.getByName(orgName);
        GitRepository gitRepo = gitRepositoryService.getByOwnerAndNameAndOrganization(
                gitRepo(org, repo).getOwner().getUsername(), repo, org.getId());
        String branch = gitRepo.getDefaultBranch();

        populateModel(model, org, gitRepo, branch, userDetails);

        List<FileTreeEntry> entries = codeBrowserService.getTree(
                gitRepo.getOwner().getUsername(), repo, branch, "");
        String readme = codeBrowserService.getReadmeContent(
                gitRepo.getOwner().getUsername(), repo, branch);
        String readmeFileName = codeBrowserService.getReadmeFileName(
                gitRepo.getOwner().getUsername(), repo, branch);

        model.addAttribute("entries", entries);
        model.addAttribute("readme", readme);
        model.addAttribute("readmeFileName", readmeFileName);
        model.addAttribute("currentPath", "");
        model.addAttribute("pathSegments", List.of());

        return "code/tree";
    }

    @GetMapping("/org/{orgName}/{repo}/tree/{branch}/**")
    public String tree(@PathVariable String orgName, @PathVariable String repo,
                       @PathVariable String branch, HttpServletRequest request,
                       @AuthenticationPrincipal UserDetails userDetails, Model model) {
        Organization org = organizationService.getByName(orgName);
        GitRepository gitRepo = gitRepo(org, repo);
        String fullPath = extractPath(request, "/org/" + orgName + "/" + repo + "/tree/" + branch);

        populateModel(model, org, gitRepo, branch, userDetails);

        List<FileTreeEntry> entries = codeBrowserService.getTree(
                gitRepo.getOwner().getUsername(), repo, branch, fullPath);
        model.addAttribute("entries", entries);
        model.addAttribute("currentPath", fullPath);
        model.addAttribute("pathSegments", fullPath.isEmpty() ? List.of() : Arrays.asList(fullPath.split("/")));

        return "code/tree";
    }

    @GetMapping("/org/{orgName}/{repo}/blob/{branch}/**")
    public String blob(@PathVariable String orgName, @PathVariable String repo,
                       @PathVariable String branch, HttpServletRequest request,
                       @AuthenticationPrincipal UserDetails userDetails, Model model) {
        Organization org = organizationService.getByName(orgName);
        GitRepository gitRepo = gitRepo(org, repo);
        String filePath = extractPath(request, "/org/" + orgName + "/" + repo + "/blob/" + branch);

        populateModel(model, org, gitRepo, branch, userDetails);

        FileContentDto fileContent = codeBrowserService.getFileContent(
                gitRepo.getOwner().getUsername(), repo, branch, filePath);
        model.addAttribute("file", fileContent);
        model.addAttribute("pathSegments", Arrays.asList(filePath.split("/")));

        return "code/blob";
    }

    @GetMapping("/org/{orgName}/{repo}/commits/{branch}")
    public String commits(@PathVariable String orgName, @PathVariable String repo,
                          @PathVariable String branch,
                          @org.springframework.web.bind.annotation.RequestParam(defaultValue = "0") int page,
                          @AuthenticationPrincipal UserDetails userDetails, Model model) {
        Organization org = organizationService.getByName(orgName);
        GitRepository gitRepo = gitRepo(org, repo);
        String owner = gitRepo.getOwner().getUsername();

        List<CommitDto> commits = commitService.getCommitLog(owner, repo, branch, page, 30);

        populateModel(model, org, gitRepo, branch, userDetails);
        model.addAttribute("commits", commits);
        model.addAttribute("currentPage", page);
        model.addAttribute("hasMore", commits.size() == 30);
        model.addAttribute("activeTab", "commits");
        return "code/commits";
    }

    private GitRepository gitRepo(Organization org, String repoName) {
        List<RepositoryDto> repos = gitRepositoryService.listByOrganization(org.getId());
        return repos.stream()
                .filter(r -> r.getName().equals(repoName))
                .findFirst()
                .map(r -> gitRepositoryService.getByOwnerAndNameAndOrganization(r.getOwnerUsername(), repoName, org.getId()))
                .orElseThrow(() -> new com.gitnx.common.exception.ResourceNotFoundException(
                        "Repository not found: " + org.getName() + "/" + repoName));
    }

    private void populateModel(Model model, Organization org, GitRepository gitRepo,
                                String branch, UserDetails userDetails) {
        List<BranchDto> branches = branchService.listBranches(
                gitRepo.getOwner().getUsername(), gitRepo.getName());

        boolean isOwner = userDetails != null
                && organizationService.isOwner(org.getName(), userDetails.getUsername());

        model.addAttribute("owner", gitRepo.getOwner().getUsername());
        model.addAttribute("repo", gitRepo.getName());
        model.addAttribute("orgName", org.getName());
        model.addAttribute("gitRepo", gitRepo);
        model.addAttribute("currentBranch", branch);
        model.addAttribute("branches", branches);
        model.addAttribute("activeTab", "code");
        model.addAttribute("isRepoOwner", isOwner);
        model.addAttribute("cloneUrl", httpBaseUrl + "/" + gitRepo.getOwner().getUsername() + "/" + gitRepo.getName() + ".git");
    }

    private String extractPath(HttpServletRequest request, String prefix) {
        String uri = request.getRequestURI();
        String path = uri.length() > prefix.length() ? uri.substring(prefix.length()) : "";
        if (path.startsWith("/")) path = path.substring(1);
        return path;
    }
}
