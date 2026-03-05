package com.spacenx.form.repository;

import com.spacenx.form.entity.FormTemplate;
import com.spacenx.space.entity.Space;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FormTemplateRepository extends JpaRepository<FormTemplate, Long> {

    @Query("SELECT f FROM FormTemplate f JOIN FETCH f.uploader WHERE f.space = :space")
    List<FormTemplate> findBySpace(@Param("space") Space space);

    @Query("SELECT f FROM FormTemplate f JOIN FETCH f.uploader WHERE f.space = :space AND f.active = true")
    List<FormTemplate> findBySpaceAndActiveTrue(@Param("space") Space space);

    @Query("SELECT f FROM FormTemplate f JOIN FETCH f.uploader LEFT JOIN FETCH f.space WHERE f.id = :id")
    Optional<FormTemplate> findByIdWithUploader(@Param("id") Long id);
}
