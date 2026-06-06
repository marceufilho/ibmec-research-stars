package br.com.ibmec.researchstars.professor;

import br.com.ibmec.researchstars.professor.dto.PagedResponse;
import br.com.ibmec.researchstars.professor.dto.ProfessorApproveResponse;
import br.com.ibmec.researchstars.professor.dto.ProfessorDetailResponse;
import br.com.ibmec.researchstars.professor.dto.ProfessorListItemResponse;
import br.com.ibmec.researchstars.professor.dto.ProfessorPublicationsResponse;
import br.com.ibmec.researchstars.professor.dto.ProfessorUpdateRequest;
import br.com.ibmec.researchstars.professor.exception.ProfessorConflictException;
import br.com.ibmec.researchstars.professor.exception.ProfessorIntegrationException;
import br.com.ibmec.researchstars.professor.exception.ProfessorNotFoundException;
import br.com.ibmec.researchstars.professor.exception.ProfessorStateException;
import br.com.ibmec.researchstars.professor.integration.CourseGateway;
import br.com.ibmec.researchstars.professor.integration.CurrentUserProvider;
import br.com.ibmec.researchstars.professor.integration.ProfessorPublicationsGateway;
import br.com.ibmec.researchstars.professor.mapper.ProfessorMapper;
import jakarta.transaction.Transactional;
import java.util.HashSet;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class ProfessorService {

    private final ProfessorRepository repository;
    private final CurrentUserProvider currentUserProvider;
    private final CourseGateway courseGateway;
    private final ProfessorPublicationsGateway publicationsGateway;

    public ProfessorService(
            ProfessorRepository repository,
            CurrentUserProvider currentUserProvider,
            CourseGateway courseGateway,
            ProfessorPublicationsGateway publicationsGateway) {
        this.repository = repository;
        this.currentUserProvider = currentUserProvider;
        this.courseGateway = courseGateway;
        this.publicationsGateway = publicationsGateway;
    }

    public PagedResponse<ProfessorListItemResponse> list(Professor.Status status, String q, int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by("id").ascending());
        var result = repository.findAll(ProfessorSpecifications.byFilters(status, q), pageable)
                .map(ProfessorMapper::toListItem);
        return new PagedResponse<>(result.getContent(), result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional
    public ProfessorDetailResponse findById(Long id) {
        return ProfessorMapper.toDetail(getProfessorOrThrow(id));
    }

    @Transactional
    public ProfessorDetailResponse findMe() {
        var userId = currentUserProvider.getCurrentUserId();
        var professor = repository.findByUserId(userId)
                .orElseThrow(() -> new ProfessorNotFoundException("Professor not found for user: " + userId));
        return ProfessorMapper.toDetail(professor);
    }

    @Transactional
    public ProfessorApproveResponse approve(Long id) {
        var professor = getProfessorOrThrow(id);
        if (professor.getStatus() == Professor.Status.APPROVED) {
            throw new ProfessorStateException("Professor is already approved");
        }
        professor.setStatus(Professor.Status.APPROVED);
        return ProfessorMapper.toApproveResponse(repository.save(professor));
    }

    @Transactional
    public ProfessorDetailResponse update(Long id, ProfessorUpdateRequest request) {
        var professor = getProfessorOrThrow(id);

        if (repository.existsByEmailAndIdNot(request.email(), id)) {
            throw new ProfessorConflictException("Email already in use");
        }
        if (repository.existsByLattesNumberAndIdNot(request.lattesNumber(), id)) {
            throw new ProfessorConflictException("Lattes number already in use");
        }

        professor.setName(request.name());
        professor.setEmail(request.email());
        professor.setMatricula(request.matricula());
        professor.setLattesNumber(request.lattesNumber());
        var validCourseIds = courseGateway.keepOnlyExistingCourseIds(request.courseIds());
        professor.setCourseIds(new HashSet<>(validCourseIds));

        return ProfessorMapper.toDetail(repository.save(professor));
    }

    @Transactional
    public void delete(Long id) {
        var professor = getProfessorOrThrow(id);
        repository.delete(professor);
    }

    public ProfessorPublicationsResponse findProfessorPublications(Long id) {
        getProfessorOrThrow(id);
        try {
            var publications = publicationsGateway.findPublicationsByProfessorId(id);
            return new ProfessorPublicationsResponse(id, publications);
        } catch (RuntimeException exception) {
            throw new ProfessorIntegrationException("Error querying professor publications", exception);
        }
    }

    private Professor getProfessorOrThrow(Long id) {
        return repository.findById(id).orElseThrow(() -> new ProfessorNotFoundException(id));
    }
}
