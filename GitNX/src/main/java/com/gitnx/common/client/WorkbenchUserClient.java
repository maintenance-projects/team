package com.gitnx.common.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Workbench API를 호출하여 유저 정보를 조회하는 클라이언트.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkbenchUserClient {

    @Value("${workbench.api.base-url}")
    private String baseUrl;

    private final RestTemplate restTemplate;

    /**
     * Workbench userId로 유저 상세 정보 조회.
     */
    public WorkbenchUser getUserDetail(String userId) {
        try {
            String url = baseUrl + "/organization/detail/" + userId;
            WorkbenchUser user = restTemplate.postForObject(url, null, WorkbenchUser.class);
            log.info("[WorkbenchClient] getUserDetail userId={}, result={}", userId, user);
            return user;
        } catch (Exception e) {
            log.warn("[WorkbenchClient] getUserDetail failed for userId={}: {}", userId, e.getMessage());
            return null;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WorkbenchUser {
        private String userId;
        private String userName;
        private String userEmail;
        private String partId;
        private String partName;
        private String position;
        private String grade;
    }
}
