package com.spacenx.space.dto;

import com.spacenx.space.enums.SpaceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateSpaceRequest {

    @NotBlank
    private String name;

    @NotBlank
    @Pattern(regexp = "^[A-Z][A-Z0-9]*$", message = "Space key must be uppercase letters and digits, starting with a letter")
    private String spaceKey;

    private String description;

    private SpaceType spaceType;
}
