package com.gitnx.repository.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BranchDto {
    private String name;
    private boolean isDefault;
    private String lastCommitHash;
    private String lastCommitMessage;
}
