package br.com.ibmec.researchstars.course.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Payload para a criação de um novo curso (POST /api/v1/courses). */
public record CreateCourseRequest(
    @NotBlank @Size(max = 120) String name, @NotBlank @Size(max = 32) String code) {}

