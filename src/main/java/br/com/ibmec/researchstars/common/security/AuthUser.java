package br.com.ibmec.researchstars.common.security;

/**
 * Principal autenticado, derivado das claims do JWT.
 *
 * <p>
 * {@code professorId} é nulo para usuários ADMIN.
 */
public record AuthUser(Long userId, Long professorId, String role, String email, String name) {
}
