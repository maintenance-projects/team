package com.spacenx.space.controller;

import com.spacenx.space.entity.Space;
import com.spacenx.space.entity.SpaceMember;
import com.spacenx.space.service.SpaceService;
import com.spacenx.user.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API controller for space member operations.
 * Used by the @mention autocomplete feature in issue comments.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/spaces/{spaceKey}/members")
public class SpaceMemberApiController {

    private final SpaceService spaceService;

    /**
     * Search space members by username or display name.
     * Returns a JSON list of matching members for autocomplete.
     *
     * @param spaceKey the space key
     * @param q        optional search query to filter members
     * @return list of member objects with id, username, and displayName
     */
    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchMembers(
            @PathVariable String spaceKey,
            @RequestParam(required = false, defaultValue = "") String q) {

        Space space = spaceService.getSpaceByKey(spaceKey);
        List<SpaceMember> members = spaceService.getMembers(space);

        String query = q.trim().toLowerCase();

        List<Map<String, Object>> result = members.stream()
                .filter(member -> {
                    if (query.isEmpty()) {
                        return true;
                    }
                    User user = member.getUser();
                    String username = user.getUsername().toLowerCase();
                    String displayName = user.getDisplayName() != null
                            ? user.getDisplayName().toLowerCase() : "";
                    return username.contains(query) || displayName.contains(query);
                })
                .map(member -> {
                    User user = member.getUser();
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", user.getId());
                    map.put("username", user.getUsername());
                    map.put("displayName", user.getDisplayName() != null
                            ? user.getDisplayName() : user.getUsername());
                    return map;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(result);
    }
}
