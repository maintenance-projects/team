package com.gitnx.common.config;

import lombok.Getter;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class GitAuthSessionStore {

    private final Map<String, GitAuthSession> sessions = new ConcurrentHashMap<>();

    public GitAuthSession create() {
        String sessionId = UUID.randomUUID().toString();
        GitAuthSession session = new GitAuthSession(sessionId);
        sessions.put(sessionId, session);
        return session;
    }

    public GitAuthSession get(String sessionId) {
        return sessions.get(sessionId);
    }

    public void complete(String sessionId, String username) {
        GitAuthSession session = sessions.get(sessionId);
        if (session != null) {
            session.complete(username);
        }
    }

    @Scheduled(fixedRate = 60_000)
    public void cleanup() {
        Instant cutoff = Instant.now().minusSeconds(300); // 5분 만료
        sessions.entrySet().removeIf(e -> e.getValue().getCreatedAt().isBefore(cutoff));
    }

    @Getter
    public static class GitAuthSession {
        private final String sessionId;
        private final Instant createdAt;
        private volatile String authenticatedUsername;
        private volatile boolean completed;

        public GitAuthSession(String sessionId) {
            this.sessionId = sessionId;
            this.createdAt = Instant.now();
        }

        public void complete(String username) {
            this.authenticatedUsername = username;
            this.completed = true;
        }
    }
}