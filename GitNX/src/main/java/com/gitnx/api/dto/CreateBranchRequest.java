package com.gitnx.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateBranchRequest {

    @NotBlank(message = "Branch name is required")
    private String name;

    private String sourceBranch;
}
