package com.gitnx.repository.dto;

import com.gitnx.repository.enums.RepositoryVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImportRepositoryRequest {

    @NotBlank(message = "Clone URL is required")
    private String cloneUrl;

    private String name;

    @Size(max = 500)
    private String description;

    private RepositoryVisibility visibility = RepositoryVisibility.PRIVATE;

    private String accessToken;
}
