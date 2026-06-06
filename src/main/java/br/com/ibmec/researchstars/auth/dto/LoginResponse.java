package br.com.ibmec.researchstars.auth.dto;

/** Resposta do login, consumida pelo frontend (token + dados de sessão). */
public record LoginResponse(
        String token, String role, String name, String email, Long professorId) {
}
