package com.gitnx.common.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.File;

@Configuration
public class JGitConfig {

    @Value("${gitnx.repos.base-path}")
    private String basePath;

    @PostConstruct
    public void init() {
        File dir = new File(basePath);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }

    public String getBasePath() {
        return basePath;
    }
}
