package com.spacenx.issue.entity;

import com.spacenx.common.entity.BaseTimeEntity;
import com.spacenx.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "snx_attachment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Attachment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "issue_id", nullable = false)
    private Issue issue;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id", nullable = false)
    private User uploader;

    @Column(nullable = false)
    private String originalFilename;

    @Column(nullable = false)
    private String storedFilename;

    @Column(nullable = false)
    private String contentType;

    @Column(nullable = false)
    private Long fileSize;

    /**
     * Returns a human-readable file size string.
     */
    public String getFileSizeFormatted() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.1f KB", fileSize / 1024.0);
        } else {
            return String.format("%.1f MB", fileSize / (1024.0 * 1024));
        }
    }

    /**
     * Returns a Font Awesome icon class based on the content type.
     */
    public String getFileIcon() {
        if (contentType == null) return "fa-file";
        if (contentType.startsWith("image/")) return "fa-file-image";
        if (contentType.equals("application/pdf")) return "fa-file-pdf";
        if (contentType.contains("word") || contentType.contains("document")) return "fa-file-word";
        if (contentType.contains("excel") || contentType.contains("spreadsheet")) return "fa-file-excel";
        if (contentType.contains("powerpoint") || contentType.contains("presentation")) return "fa-file-powerpoint";
        if (contentType.contains("zip") || contentType.contains("archive") || contentType.contains("compressed")) return "fa-file-archive";
        if (contentType.startsWith("text/")) return "fa-file-alt";
        if (contentType.startsWith("video/")) return "fa-file-video";
        if (contentType.startsWith("audio/")) return "fa-file-audio";
        return "fa-file";
    }
}
