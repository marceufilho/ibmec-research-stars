package br.com.ibmec.researchstars.common.config;

import br.com.ibmec.researchstars.course.Course;
import br.com.ibmec.researchstars.course.CourseRepository;
import br.com.ibmec.researchstars.user.User;
import br.com.ibmec.researchstars.user.UserRepository;
import br.com.ibmec.researchstars.user.UserRole;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Popula dados mínimos para uso local: um administrador padrão e os cursos da
 * instituição.
 *
 * <p>
 * Idempotente — só insere o que ainda não existe.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private static final String ADMIN_EMAIL = "admin@ibmec.br";
    private static final String ADMIN_PASSWORD = "admin123";

    private static final List<Map.Entry<String, String>> COURSES = List.of(
            Map.entry("Administração — Bacharelado", "ADM"),
            Map.entry("Análise e Desenvolvimento de Sistemas — Tecnólogo", "ADS"),
            Map.entry("Arquitetura e Urbanismo — Bacharelado", "ARQ"),
            Map.entry("Ciência de Dados e Inteligência Artificial — Bacharelado", "CDIA"),
            Map.entry("Ciências Contábeis — Bacharelado", "CCO"),
            Map.entry("Ciências Econômicas — Bacharelado", "ECO"),
            Map.entry("Comunicação Social - Publicidade e Propaganda — Bacharelado", "PUB"),
            Map.entry("Direito — Bacharelado", "DIR"),
            Map.entry("Engenharia Civil — Bacharelado", "ECIV"),
            Map.entry("Engenharia da Computação — Bacharelado", "ECOMP"),
            Map.entry("Engenharia de Produção — Bacharelado", "EPROD"),
            Map.entry("Engenharia de Software — Bacharelado", "ESW"),
            Map.entry("Relações Internacionais — Bacharelado", "RI"),
            Map.entry("Mestrado Profissional em Administração - Mestrado", "MPADM"));

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            UserRepository userRepository,
            CourseRepository courseRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        seedAdmin();
        seedCourses();
    }

    private void seedAdmin() {
        if (userRepository.findByEmail(ADMIN_EMAIL).isEmpty()) {
            userRepository.save(
                    new User(ADMIN_EMAIL, passwordEncoder.encode(ADMIN_PASSWORD), UserRole.ADMIN));
            log.info("Usuário ADMIN padrão criado: {} (senha: {})", ADMIN_EMAIL, ADMIN_PASSWORD);
        }
    }

    private void seedCourses() {
        for (Map.Entry<String, String> entry : COURSES) {
            if (!courseRepository.existsByCodeIgnoreCase(entry.getValue())) {
                courseRepository.save(new Course(entry.getKey(), entry.getValue()));
            }
        }
    }
}
