package com.gitnx.repository.controller;

import com.gitnx.repository.dto.*;
import com.gitnx.repository.entity.GitRepository;
import com.gitnx.repository.service.BranchService;
import com.gitnx.repository.service.CodeBrowserService;
import com.gitnx.repository.service.GitRepositoryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Arrays;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class CodeBrowserController {

    private final GitRepositoryService gitRepositoryService;
    private final CodeBrowserService codeBrowserService;
    private final BranchService branchService;

    @Value("${gitnx.clone.http-base-url}")
    private String httpBaseUrl;

    @GetMapping("/{owner}/{repo}")
    public String repoRoot(@PathVariable String owner, @PathVariable String repo, Model model) {
        GitRepository gitRepo = gitRepositoryService.getByOwnerAndName(owner, repo);
        String branch = gitRepo.getDefaultBranch();

        populateRepoModel(model, owner, repo, branch, "");

        List<FileTreeEntry> entries = codeBrowserService.getTree(owner, repo, branch, "");
        String readme = codeBrowserService.getReadmeContent(owner, repo, branch);
        String readmeFileName = codeBrowserService.getReadmeFileName(owner, repo, branch);

        model.addAttribute("entries", entries);
        model.addAttribute("readme", readme);
        model.addAttribute("readmeFileName", readmeFileName);
        model.addAttribute("currentPath", "");
        model.addAttribute("pathSegments", List.of());

        return "code/tree";
    }

    @GetMapping("/{owner}/{repo}/tree/{branch}/**")
    public String tree(@PathVariable String owner, @PathVariable String repo,
                       @PathVariable String branch, HttpServletRequest request, Model model) {
        String fullPath = extractPath(request, "/" + owner + "/" + repo + "/tree/" + branch);

        populateRepoModel(model, owner, repo, branch, fullPath);

        List<FileTreeEntry> entries = codeBrowserService.getTree(owner, repo, branch, fullPath);
        model.addAttribute("entries", entries);
        model.addAttribute("currentPath", fullPath);
        model.addAttribute("pathSegments", fullPath.isEmpty() ? List.of() : Arrays.asList(fullPath.split("/")));

        return "code/tree";
    }

    @GetMapping("/{owner}/{repo}/blob/{branch}/**")
    public String blob(@PathVariable String owner, @PathVariable String repo,
                       @PathVariable String branch, HttpServletRequest request, Model model) {
        String filePath = extractPath(request, "/" + owner + "/" + repo + "/blob/" + branch);

        populateRepoModel(model, owner, repo, branch, filePath);

        FileContentDto fileContent = codeBrowserService.getFileContent(owner, repo, branch, filePath);
        model.addAttribute("file", fileContent);
        model.addAttribute("pathSegments", Arrays.asList(filePath.split("/")));

        return "code/blob";
    }

    @GetMapping("/{owner}/{repo}/raw/{branch}/**")
    @ResponseBody
    public ResponseEntity<byte[]> raw(@PathVariable String owner, @PathVariable String repo,
                                       @PathVariable String branch, HttpServletRequest request) {
        String filePath = extractPath(request, "/" + owner + "/" + repo + "/raw/" + branch);

        FileContentDto fileContent = codeBrowserService.getFileContent(owner, repo, branch, filePath);

        MediaType mediaType;
        if (fileContent.isBinary()) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        } else {
            mediaType = MediaType.TEXT_PLAIN;
        }

        byte[] data = fileContent.isBinary()
                ? new byte[0]  // binary not supported in raw view yet
                : fileContent.getContent().getBytes();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileContent.getName() + "\"")
                .contentType(mediaType)
                .body(data);
    }

    private void populateRepoModel(Model model, String owner, String repo, String branch, String path) {
        GitRepository gitRepo = gitRepositoryService.getByOwnerAndName(owner, repo);
        List<BranchDto> branches = branchService.listBranches(owner, repo);

        model.addAttribute("owner", owner);
        model.addAttribute("repo", repo);
        model.addAttribute("gitRepo", gitRepo);
        model.addAttribute("currentBranch", branch);
        model.addAttribute("branches", branches);
        model.addAttribute("activeTab", "code");
        model.addAttribute("cloneUrl", httpBaseUrl + "/" + owner + "/" + repo + ".git");
    }

    private String extractPath(HttpServletRequest request, String prefix) {
        String uri = request.getRequestURI();
        String path = uri.length() > prefix.length() ? uri.substring(prefix.length()) : "";
        if (path.startsWith("/")) path = path.substring(1);
        return path;
    }
}
