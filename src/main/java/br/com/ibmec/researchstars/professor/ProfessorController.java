package br.com.ibmec.researchstars.professor;

import br.com.ibmec.researchstars.professor.dto.PagedResponse;
import br.com.ibmec.researchstars.professor.dto.ProfessorApproveResponse;
import br.com.ibmec.researchstars.professor.dto.ProfessorDetailResponse;
import br.com.ibmec.researchstars.professor.dto.ProfessorListItemResponse;
import br.com.ibmec.researchstars.professor.dto.ProfessorPublicationsResponse;
import br.com.ibmec.researchstars.professor.dto.ProfessorUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/professors")
public class ProfessorController {

    private final ProfessorService service;

    public ProfessorController(ProfessorService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public PagedResponse<ProfessorListItemResponse> list(
            @RequestParam(required = false) Professor.Status status,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.list(status, q, page, size);
    }

    @GetMapping("/{id:[0-9]+}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProfessorDetailResponse getById(@PathVariable Long id) {
        return service.findById(id);
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ProfessorDetailResponse me() {
        return service.findMe();
    }

    @PostMapping("/{id:[0-9]+}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ProfessorApproveResponse approve(@PathVariable Long id) {
        return service.approve(id);
    }

    @PatchMapping("/{id:[0-9]+}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProfessorDetailResponse update(@PathVariable Long id, @Valid @RequestBody ProfessorUpdateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id:[0-9]+}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @GetMapping("/{id:[0-9]+}/publications")
    @PreAuthorize("hasRole('ADMIN')")
    public ProfessorPublicationsResponse publications(@PathVariable Long id) {
        return service.findProfessorPublications(id);
    }
}
