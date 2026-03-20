package com.gitnx.api.dto;

import com.gitnx.repository.entity.RepositoryMember;
import com.gitnx.repository.enums.RepositoryRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class MemberDto {
    private Long memberId;
    private String username;
    private String displayName;
    private String email;
    private RepositoryRole role;
    private LocalDateTime joinedAt;

    public static MemberDto from(RepositoryMember member) {
        return MemberDto.builder()
                .memberId(member.getId())
                .username(member.getUser().getUsername())
                .displayName(member.getUser().getDisplayName())
                .email(member.getUser().getEmail())
                .role(member.getRole())
                .joinedAt(member.getCreatedAt())
                .build();
    }
}
