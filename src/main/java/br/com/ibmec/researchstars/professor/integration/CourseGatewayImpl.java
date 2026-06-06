package br.com.ibmec.researchstars.professor.integration;

import br.com.ibmec.researchstars.course.CourseRepository;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Filtra os IDs de curso mantendo apenas os que existem (RF-26). */
@Component
public class CourseGatewayImpl implements CourseGateway {

    private final CourseRepository courseRepository;

    public CourseGatewayImpl(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    @Override
    public Set<Long> keepOnlyExistingCourseIds(Set<Long> courseIds) {
        if (courseIds == null || courseIds.isEmpty()) {
            return Set.of();
        }
        Set<Long> existing = new LinkedHashSet<>();
        for (Long id : courseIds) {
            if (id != null && courseRepository.existsById(id)) {
                existing.add(id);
            }
        }
        return existing;
    }
}
