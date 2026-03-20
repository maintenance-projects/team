package com.gitnx.api.dto;

import com.gitnx.repository.enums.RepositoryRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AddMemberRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotNull(message = "Role is required")
    private RepositoryRole role;
}
