package com.gitnx.repository.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommitDto {
    private String hash;
    private String shortHash;
    private String message;
    private String authorName;
    private String authorEmail;
    private LocalDateTime authorDate;
    private int parentCount;
}
