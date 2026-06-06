package br.com.ibmec.researchstars.professor.integration;

import br.com.ibmec.researchstars.common.security.AuthUser;
import br.com.ibmec.researchstars.professor.exception.ProfessorForbiddenException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Obtém o usuário autenticado a partir do SecurityContext (JWT). */
@Component
public class CurrentUserProviderImpl implements CurrentUserProvider {

    @Override
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthUser user) {
            return user.userId();
        }
        throw new ProfessorForbiddenException("Usuário não autenticado");
    }
}
