package br.com.ibmec.researchstars.course;

import br.com.ibmec.researchstars.course.dto.CourseDto;
import br.com.ibmec.researchstars.course.dto.CreateCourseRequest;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints REST de cursos.
 *
 * <ul>
 *   <li>{@code GET /api/v1/courses} — qualquer usuário autenticado (RF-26).
 *   <li>{@code POST /api/v1/courses} — somente ADMIN (RF-25).
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/courses")
public class CourseController {

  private final CourseService courseService;

  public CourseController(CourseService courseService) {
    this.courseService = courseService;
  }

  /** RF-26 — Listar todos os cursos. Requer JWT válido. */
  @GetMapping
  public Page<CourseDto> listAll(
      @RequestParam(name = "q", required = false) String query,
      @PageableDefault(size = 20, sort = "name") Pageable pageable) {
    return courseService.listAll(query, pageable);
  }

  /** RF-25 — Criar um curso. Restrito a ADMIN. */
  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<CourseDto> create(@Valid @RequestBody CreateCourseRequest request) {
    CourseDto created = courseService.create(request);
    return ResponseEntity.created(URI.create("/api/v1/courses/" + created.id())).body(created);
  }
}

