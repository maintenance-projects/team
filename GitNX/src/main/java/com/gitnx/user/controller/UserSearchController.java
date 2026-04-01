package com.gitnx.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class UserSearchController {

    @Qualifier("workbenchJdbcTemplate")
    private final JdbcTemplate workbenchJdbc;

    /**
     * Workbench DB에서 사용자 검색 (autocomplete용)
     */
    @GetMapping("/settings/users/search")
    public List<Map<String, Object>> searchUsers(@RequestParam(defaultValue = "") String q) {
        if (q.isBlank()) {
            return workbenchJdbc.queryForList(
                    "SELECT user_id, user_name, email FROM WB_ORGANIZATION.msg_user ORDER BY user_name LIMIT 20");
        }
        String like = "%" + q + "%";
        return workbenchJdbc.queryForList(
                "SELECT user_id, user_name, email FROM WB_ORGANIZATION.msg_user " +
                "WHERE user_id LIKE ? OR user_name LIKE ? ORDER BY user_name LIMIT 20",
                like, like);
    }
}
