package br.com.ibmec.researchstars.professor.integration;

import br.com.ibmec.researchstars.publication.mapper.PublicationMapper;
import br.com.ibmec.researchstars.publication.repository.PublicationRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Recupera as publicações de um professor para o módulo de professores (RF-19).
 */
@Component
public class ProfessorPublicationsGatewayImpl implements ProfessorPublicationsGateway {

    private final PublicationRepository publicationRepository;
    private final PublicationMapper publicationMapper;

    public ProfessorPublicationsGatewayImpl(
            PublicationRepository publicationRepository, PublicationMapper publicationMapper) {
        this.publicationRepository = publicationRepository;
        this.publicationMapper = publicationMapper;
    }

    @Override
    public List<Object> findPublicationsByProfessorId(Long professorId) {
        return publicationRepository.findAllByProfessorId(professorId).stream()
                .map(publicationMapper::toResponse)
                .map(Object.class::cast)
                .toList();
    }
}
