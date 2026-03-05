package com.spacenx.issue.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class UpdateIssueDatesRequest {

    private Long issueId;

    private LocalDate startDate;

    private LocalDate endDate;
}
