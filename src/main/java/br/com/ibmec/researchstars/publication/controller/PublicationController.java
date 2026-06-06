package br.com.ibmec.researchstars.publication.controller;

import br.com.ibmec.researchstars.publication.PublicationStatus;
import br.com.ibmec.researchstars.publication.dto.PublicationCreateRequest;
import br.com.ibmec.researchstars.publication.dto.PublicationResponse;
import br.com.ibmec.researchstars.publication.service.PublicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/publications")
@RequiredArgsConstructor
@Tag(name = "Publications", description = "Gerenciamento de publicações")
public class PublicationController {

    private final PublicationService publicationService;

    // GET /publications — Admin (RF-14, RF-20)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar todas as publicações (Admin)", description = "Suporta filtros ?status=, ?professorId=, ?q=, ?page=, ?size=, ?sort=")
    public ResponseEntity<Page<PublicationResponse>> findAll(
            @RequestParam(required = false) PublicationStatus status,
            @RequestParam(required = false) Long professorId,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(publicationService.findAll(status, professorId, q, pageable));
    }

    // GET /publications/me — Professor (RF-12)
    @GetMapping("/me")
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Listar as próprias publicações (Professor)")
    public ResponseEntity<Page<PublicationResponse>> findMyPublications(
            @AuthenticationPrincipal(expression = "professorId") Long professorId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(publicationService.findMyPublications(professorId, pageable));
    }

    // GET /publications/{id} — Admin / dono (RF-12)
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('PROFESSOR')")
    @Operation(summary = "Visualizar uma publicação (Admin / dono)")
    public ResponseEntity<PublicationResponse> findById(
            @PathVariable Long id) {
        return ResponseEntity.ok(publicationService.findById(id));
    }

    // POST /publications — Professor (RF-10, RF-11)
    @PostMapping
    @PreAuthorize("hasRole('PROFESSOR')")
    @Operation(summary = "Cadastrar publicação (Professor)")
    public ResponseEntity<PublicationResponse> create(
            @AuthenticationPrincipal(expression = "professorId") Long professorId,
            @Valid @RequestBody PublicationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(publicationService.create(professorId, request));
    }
}
