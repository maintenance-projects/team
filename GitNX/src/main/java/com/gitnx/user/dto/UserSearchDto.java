package com.gitnx.user.dto;

import com.gitnx.user.entity.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserSearchDto {
    private String username;
    private String displayName;

    public static UserSearchDto from(User user) {
        return UserSearchDto.builder()
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .build();
    }
}
