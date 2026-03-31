package com.gitnx.api.controller;

import com.gitnx.user.dto.UserSearchDto;
import com.gitnx.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ApiUserController {

    private final UserService userService;

    @GetMapping("/search")
    public ResponseEntity<List<UserSearchDto>> search(
            @RequestParam(name = "q", defaultValue = "") String query,
            @RequestParam(name = "limit", defaultValue = "10") int limit) {
        List<UserSearchDto> result = userService.searchUsers(query, limit)
                .stream()
                .map(UserSearchDto::from)
                .toList();
        return ResponseEntity.ok(result);
    }
}
