package com.spacenx.shortcut.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateShortcutRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String url;

    private String description;
}
