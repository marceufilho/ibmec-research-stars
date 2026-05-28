package br.com.ibmec.researchstars.course;

import br.com.ibmec.researchstars.common.exception.DuplicateResourceException;
import br.com.ibmec.researchstars.common.exception.ResourceNotFoundException;
import br.com.ibmec.researchstars.course.dto.CourseDto;
import br.com.ibmec.researchstars.course.dto.CreateCourseRequest;
import br.com.ibmec.researchstars.course.dto.UpdateCourseRequest;
import br.com.ibmec.researchstars.course.mapper.CourseMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/** Regras de negócio do recurso Curso (RF-25, RF-26). */
@Service
@Transactional
public class CourseService {

  private final CourseRepository courseRepository;
  private final CourseMapper courseMapper;

  public CourseService(CourseRepository courseRepository, CourseMapper courseMapper) {
    this.courseRepository = courseRepository;
    this.courseMapper = courseMapper;
  }

  /** Lista todos os cursos. Acessível a qualquer usuário autenticado (RF-26). */
  @Transactional(readOnly = true)
  public Page<CourseDto> listAll(String query, Pageable pageable) {
    Page<Course> page;
    if (StringUtils.hasText(query)) {
      page =
          courseRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(
              query, query, pageable);
    } else {
      page = courseRepository.findAll(pageable);
    }
    return page.map(courseMapper::toDto);
  }

  /** Cria um curso. Restrito a ADMIN (RF-25). */
  public CourseDto create(CreateCourseRequest request) {
    String code = request.code().trim();
    if (courseRepository.existsByCodeIgnoreCase(code)) {
      throw new DuplicateResourceException("Course code already in use: " + code);
    }
    Course saved = courseRepository.save(courseMapper.toEntity(request));
    return courseMapper.toDto(saved);
  }

  /** Edita um curso existente. Restrito a ADMIN (RF-25). */
  public CourseDto update(Long id, UpdateCourseRequest request) {
    Course course =
        courseRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Course not found: " + id));

    String code = request.code().trim();
    if (courseRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
      throw new DuplicateResourceException("Course code already in use: " + code);
    }

    course.setName(request.name().trim());
    course.setCode(code);
    return courseMapper.toDto(course);
  }

  /** Exclui um curso existente. Restrito a ADMIN (RF-25). */
  public void delete(Long id) {
    if (!courseRepository.existsById(id)) {
      throw new ResourceNotFoundException("Course not found: " + id);
    }
    courseRepository.deleteById(id);
  }
}

