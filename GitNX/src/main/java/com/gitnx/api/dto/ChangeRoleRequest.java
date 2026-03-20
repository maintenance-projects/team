package com.gitnx.api.dto;

import com.gitnx.repository.enums.RepositoryRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChangeRoleRequest {

    @NotNull(message = "Role is required")
    private RepositoryRole role;
}
