package com.gitnx.repository.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileContentDto {
    private String name;
    private String path;
    private String content;
    private String language;
    private long size;
    private int lineCount;
    private boolean binary;
}
