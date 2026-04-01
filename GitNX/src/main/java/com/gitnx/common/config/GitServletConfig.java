package com.gitnx.common.config;

import com.gitnx.repository.entity.GitRepository;
import com.gitnx.repository.enums.RepositoryVisibility;
import java.util.List;
import com.gitnx.repository.repository.GitRepositoryJpaRepository;
import com.gitnx.repository.repository.RepositoryMemberJpaRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jgit.errors.RepositoryNotFoundException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.ReceivePack;
import org.eclipse.jgit.transport.UploadPack;
import org.eclipse.jgit.transport.resolver.ReceivePackFactory;
import org.eclipse.jgit.transport.resolver.RepositoryResolver;
import org.eclipse.jgit.transport.resolver.ServiceNotAuthorizedException;
import org.eclipse.jgit.transport.resolver.ServiceNotEnabledException;
import org.eclipse.jgit.transport.resolver.UploadPackFactory;
import org.eclipse.jgit.http.server.GitServlet;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class GitServletConfig {

    private final GitRepositoryJpaRepository repoJpaRepository;
    private final RepositoryMemberJpaRepository memberJpaRepository;

    @Bean
    public ServletRegistrationBean<GitServlet> gitServletRegistration() {
        GitServlet gitServlet = new GitServlet();
        gitServlet.setRepositoryResolver(new GitNxRepositoryResolver());
        gitServlet.setUploadPackFactory(new GitNxUploadPackFactory());
        gitServlet.setReceivePackFactory(new GitNxReceivePackFactory());

        ServletRegistrationBean<GitServlet> registration =
                new ServletRegistrationBean<>(gitServlet, "/repo/*");
        registration.setName("gitServlet");
        registration.setLoadOnStartup(1);

        registration.addInitParameter("base-path", "");
        registration.addInitParameter("export-all", "true");

        return registration;
    }

    /**
     * Custom RepositoryResolver that maps URL paths to bare repositories on disk.
     * URL pattern: /repo/{owner}/{repo}.git or /repo/{owner}/{repo}
     */
    private class GitNxRepositoryResolver implements RepositoryResolver<HttpServletRequest> {

        @Override
        public Repository open(HttpServletRequest req, String name)
                throws RepositoryNotFoundException, ServiceNotAuthorizedException,
                ServiceNotEnabledException {

            log.debug("Git HTTP request - resolving repository: {}", name);

            // name comes in as "{owner}/{repo}.git" or "{owner}/{repo}"
            String repoPath = name;
            if (repoPath.endsWith(".git")) {
                repoPath = repoPath.substring(0, repoPath.length() - 4);
            }

            String[] parts = repoPath.split("/", 2);
            if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
                throw new RepositoryNotFoundException(name);
            }

            String ownerName = parts[0];
            String repoName = parts[1];

            // Look up in database by owner + repo name
            List<GitRepository> repos = repoJpaRepository.findByOwnerUsernameAndName(ownerName, repoName);
            if (repos.isEmpty()) throw new RepositoryNotFoundException(name);
            GitRepository gitRepo = repos.get(0);

            File repoDir = new File(gitRepo.getDiskPath());
            if (!repoDir.exists()) {
                throw new RepositoryNotFoundException(name);
            }

            // Store resolved entity for UploadPackFactory / ReceivePackFactory
            req.setAttribute("gitnx.repository", gitRepo);

            try {
                return new FileRepositoryBuilder()
                        .setGitDir(repoDir)
                        .readEnvironment()
                        .build();
            } catch (IOException e) {
                throw new RepositoryNotFoundException(name, e);
            }
        }
    }

    /**
     * Upload pack factory - allows clone/fetch.
     * Public repos: anyone can clone.
     * Private repos: only members can clone.
     */
    private class GitNxUploadPackFactory implements UploadPackFactory<HttpServletRequest> {
        @Override
        public UploadPack create(HttpServletRequest req, Repository db)
                throws ServiceNotEnabledException, ServiceNotAuthorizedException {

            GitRepository gitRepo = (GitRepository) req.getAttribute("gitnx.repository");

            if (gitRepo != null && gitRepo.getVisibility() == RepositoryVisibility.PRIVATE) {
                String username = req.getRemoteUser();
                if (username == null) {
                    throw new ServiceNotAuthorizedException();
                }
                boolean isMember = memberJpaRepository
                        .existsByGitRepositoryIdAndUserUsername(gitRepo.getId(), username);
                if (!isMember) {
                    throw new ServiceNotAuthorizedException();
                }
            }

            return new UploadPack(db);
        }
    }

    /**
     * Receive pack factory - allows push only for members with write permission.
     * OWNER and MEMBER can push.
     */
    private class GitNxReceivePackFactory implements ReceivePackFactory<HttpServletRequest> {
        @Override
        public ReceivePack create(HttpServletRequest req, Repository db)
                throws ServiceNotEnabledException, ServiceNotAuthorizedException {

            String user = req.getRemoteUser();
            if (user == null) {
                throw new ServiceNotAuthorizedException();
            }

            GitRepository gitRepo = (GitRepository) req.getAttribute("gitnx.repository");
            if (gitRepo == null) {
                throw new ServiceNotAuthorizedException();
            }

            // Check if user is a member (OWNER or MEMBER can push)
            boolean isMember = memberJpaRepository
                    .existsByGitRepositoryIdAndUserUsername(gitRepo.getId(), user);

            if (!isMember) {
                log.warn("Push denied: user '{}' is not a member of repository '{}'", user, gitRepo.getName());
                throw new ServiceNotAuthorizedException();
            }

            ReceivePack rp = new ReceivePack(db);
            rp.setAllowCreates(true);
            rp.setAllowDeletes(true);
            rp.setAllowNonFastForwards(true);
            return rp;
        }
    }
}
