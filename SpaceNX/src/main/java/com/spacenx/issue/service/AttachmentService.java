package com.spacenx.issue.service;

import com.spacenx.issue.entity.Attachment;
import com.spacenx.issue.entity.Issue;
import com.spacenx.issue.repository.AttachmentRepository;
import com.spacenx.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;

    @Value("${spacenx.uploads.base-path:${user.home}/spacenx-uploads}")
    private String uploadsBasePath;

    @Value("${spacenx.uploads.max-file-size:10485760}")
    private long maxFileSize;

    @Transactional
    public Attachment uploadFile(MultipartFile file, Issue issue, User uploader) {
        if (file.isEmpty()) {
            throw new RuntimeException("Cannot upload empty file");
        }

        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("File size exceeds maximum allowed size of " +
                    (maxFileSize / (1024 * 1024)) + "MB");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String storedFilename = UUID.randomUUID() + extension;

        Path uploadDir = getUploadDir(issue.getSpace().getSpaceKey(), issue.getIssueKey());

        try {
            Files.createDirectories(uploadDir);
            Path targetPath = uploadDir.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to store file: {}", originalFilename, e);
            throw new RuntimeException("Failed to store file: " + originalFilename, e);
        }

        Attachment attachment = Attachment.builder()
                .issue(issue)
                .uploader(uploader)
                .originalFilename(originalFilename)
                .storedFilename(storedFilename)
                .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .fileSize(file.getSize())
                .build();

        return attachmentRepository.save(attachment);
    }

    @Transactional(readOnly = true)
    public List<Attachment> getAttachmentsByIssue(Issue issue) {
        return attachmentRepository.findByIssueOrderByCreatedAtDesc(issue);
    }

    @Transactional(readOnly = true)
    public Attachment getAttachment(Long id) {
        return attachmentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attachment not found with id: " + id));
    }

    @Transactional
    public void deleteAttachment(Long id) {
        Attachment attachment = getAttachment(id);
        Path filePath = getUploadDir(
                attachment.getIssue().getSpace().getSpaceKey(),
                attachment.getIssue().getIssueKey()
        ).resolve(attachment.getStoredFilename());

        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Failed to delete file from disk: {}", filePath, e);
        }

        attachmentRepository.delete(attachment);
    }

    @Transactional(readOnly = true)
    public Path getAttachmentFile(Attachment attachment) {
        Path filePath = getUploadDir(
                attachment.getIssue().getSpace().getSpaceKey(),
                attachment.getIssue().getIssueKey()
        ).resolve(attachment.getStoredFilename());

        if (!Files.exists(filePath)) {
            throw new RuntimeException("Attachment file not found on disk: " + attachment.getOriginalFilename());
        }

        return filePath;
    }

    private Path getUploadDir(String spaceKey, String issueKey) {
        return Paths.get(uploadsBasePath, spaceKey, issueKey);
    }
}
