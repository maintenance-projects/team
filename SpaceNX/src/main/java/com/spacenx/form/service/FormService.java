package com.spacenx.form.service;

import com.spacenx.form.entity.FormTemplate;
import com.spacenx.form.repository.FormTemplateRepository;
import com.spacenx.space.entity.Space;
import com.spacenx.space.repository.SpaceRepository;
import com.spacenx.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
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
public class FormService {

    private final FormTemplateRepository formTemplateRepository;
    private final SpaceRepository spaceRepository;

    @Value("${spacenx.uploads.base-path:${user.home}/spacenx-uploads}")
    private String uploadsBasePath;

    @Transactional
    public FormTemplate createFormTemplate(Long spaceId, String name, String description,
                                           MultipartFile file, User uploader) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space not found with id: " + spaceId));

        if (file.isEmpty()) {
            throw new RuntimeException("파일을 선택해주세요.");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String storedFilename = UUID.randomUUID() + extension;

        Path uploadDir = getUploadDir(space.getSpaceKey());
        try {
            Files.createDirectories(uploadDir);
            Path targetPath = uploadDir.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Failed to store form template file: {}", originalFilename, e);
            throw new RuntimeException("파일 저장에 실패했습니다: " + originalFilename, e);
        }

        FormTemplate template = FormTemplate.builder()
                .space(space)
                .name(name)
                .description(description)
                .originalFilename(originalFilename)
                .storedFilename(storedFilename)
                .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .fileSize(file.getSize())
                .uploader(uploader)
                .active(true)
                .build();

        return formTemplateRepository.save(template);
    }

    @Transactional(readOnly = true)
    public List<FormTemplate> getFormTemplates(Long spaceId) {
        Space space = spaceRepository.findById(spaceId)
                .orElseThrow(() -> new RuntimeException("Space not found with id: " + spaceId));
        return formTemplateRepository.findBySpace(space);
    }

    @Transactional(readOnly = true)
    public FormTemplate getFormTemplate(Long id) {
        return formTemplateRepository.findByIdWithUploader(id)
                .orElseThrow(() -> new RuntimeException("Form template not found with id: " + id));
    }

    @Transactional(readOnly = true)
    public Resource getFormTemplateFile(FormTemplate template) {
        Path filePath = getUploadDir(template.getSpace().getSpaceKey())
                .resolve(template.getStoredFilename());

        if (!Files.exists(filePath)) {
            throw new RuntimeException("파일을 찾을 수 없습니다: " + template.getOriginalFilename());
        }

        try {
            return new UrlResource(filePath.toUri());
        } catch (Exception e) {
            throw new RuntimeException("파일을 읽을 수 없습니다: " + template.getOriginalFilename(), e);
        }
    }

    @Transactional
    public void deleteFormTemplate(Long id) {
        FormTemplate template = getFormTemplate(id);
        deleteFormTemplateFile(template);
        formTemplateRepository.delete(template);
    }

    public void deleteFormTemplateFile(FormTemplate template) {
        Path filePath = getUploadDir(template.getSpace().getSpaceKey())
                .resolve(template.getStoredFilename());
        try {
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.warn("Failed to delete form template file from disk: {}", filePath, e);
        }
    }

    private Path getUploadDir(String spaceKey) {
        return Paths.get(uploadsBasePath, spaceKey, "forms");
    }
}
