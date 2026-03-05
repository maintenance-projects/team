package com.spacenx.form.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateFormTemplateRequest {

    @NotBlank
    private String name;

    private String description;
}
