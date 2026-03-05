package com.gitnx.repository.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class DiffEntryDto {
    private String oldPath;
    private String newPath;
    private String changeType; // ADD, MODIFY, DELETE, RENAME, COPY
    private String diffContent; // unified diff text
}
