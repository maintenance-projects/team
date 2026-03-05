package com.spacenx.sprint.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateSprintRequest {

    @NotBlank
    private String name;

    private String goal;

    private LocalDate startDate;

    private LocalDate endDate;
}
