package com.gitnx;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class GitNxApplication {

    public static void main(String[] args) {
        SpringApplication.run(GitNxApplication.class, args);
    }
}
