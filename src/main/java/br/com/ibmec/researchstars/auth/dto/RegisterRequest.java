package br.com.ibmec.researchstars.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Set;

/** Autocadastro de professor (POST /api/v1/auth/register) — RF-01. */
public record RegisterRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(max = 100) String lattesNumber,
        @NotBlank @Size(min = 6, max = 100) String password,
        Set<Long> courseIds) {
}
