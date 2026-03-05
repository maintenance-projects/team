package com.gitnx.repository.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class FileTreeEntry implements Comparable<FileTreeEntry> {

    private String name;
    private String path;
    private String type; // "dir" or "file"
    private long size;

    @Override
    public int compareTo(FileTreeEntry other) {
        // Directories first, then alphabetical
        if (this.type.equals(other.type)) {
            return this.name.compareToIgnoreCase(other.name);
        }
        return this.type.equals("dir") ? -1 : 1;
    }
}
