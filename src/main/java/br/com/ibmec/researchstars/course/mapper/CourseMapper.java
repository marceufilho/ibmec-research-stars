package br.com.ibmec.researchstars.course.mapper;

import br.com.ibmec.researchstars.course.Course;
import br.com.ibmec.researchstars.course.dto.CourseDto;
import br.com.ibmec.researchstars.course.dto.CreateCourseRequest;
import org.springframework.stereotype.Component;

/** Conversões manuais entre {@link Course} e seus DTOs (RNF-07). */
@Component
public class CourseMapper {

  public Course toEntity(CreateCourseRequest request) {
    return new Course(request.name().trim(), request.code().trim());
  }

  public CourseDto toDto(Course course) {
    return new CourseDto(course.getId(), course.getName(), course.getCode());
  }
}

