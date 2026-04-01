package com.gitnx.repository.dto;

import com.gitnx.repository.entity.GitRepository;
import com.gitnx.repository.enums.RepositoryVisibility;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class RepositoryDto {

    private Long id;
    private String name;
    private String description;
    private RepositoryVisibility visibility;
    private String defaultBranch;
    private String ownerUsername;
    private Long organizationId;
    private LocalDateTime createdAt;

    public static RepositoryDto from(GitRepository repo) {
        return RepositoryDto.builder()
                .id(repo.getId())
                .name(repo.getName())
                .description(repo.getDescription())
                .visibility(repo.getVisibility())
                .defaultBranch(repo.getDefaultBranch())
                .ownerUsername(repo.getOwner().getUsername())
                .organizationId(repo.getOrganization() != null ? repo.getOrganization().getId() : null)
                .createdAt(repo.getCreatedAt())
                .build();
    }
}
