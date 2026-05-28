package br.com.ibmec.researchstars.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload para a edicao de um curso existente (PATCH /api/v1/courses/{id}). */
public record UpdateCourseRequest(
    @NotBlank @Size(max = 120) String name, @NotBlank @Size(max = 32) String code) {}
