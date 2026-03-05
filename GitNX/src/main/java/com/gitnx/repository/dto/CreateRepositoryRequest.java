package com.gitnx.repository.dto;

import com.gitnx.repository.enums.RepositoryVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRepositoryRequest {

    @NotBlank(message = "Repository name is required")
    @Size(min = 1, max = 100, message = "Repository name must be 1-100 characters")
    @Pattern(regexp = "^[a-zA-Z0-9._-]+$", message = "Repository name can only contain letters, numbers, dots, hyphens and underscores")
    private String name;

    @Size(max = 500)
    private String description;

    private RepositoryVisibility visibility = RepositoryVisibility.PRIVATE;

    private String defaultBranch = "main";

    private boolean initWithReadme = true;
}
