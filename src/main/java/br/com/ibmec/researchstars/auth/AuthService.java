package br.com.ibmec.researchstars.auth;

import br.com.ibmec.researchstars.auth.dto.LoginRequest;
import br.com.ibmec.researchstars.auth.dto.LoginResponse;
import br.com.ibmec.researchstars.auth.dto.RegisterRequest;
import br.com.ibmec.researchstars.common.exception.DuplicateResourceException;
import br.com.ibmec.researchstars.common.security.AuthUser;
import br.com.ibmec.researchstars.common.security.JwtService;
import br.com.ibmec.researchstars.professor.Professor;
import br.com.ibmec.researchstars.professor.ProfessorRepository;
import br.com.ibmec.researchstars.professor.dto.ProfessorDetailResponse;
import br.com.ibmec.researchstars.professor.mapper.ProfessorMapper;
import br.com.ibmec.researchstars.user.User;
import br.com.ibmec.researchstars.user.UserRepository;
import br.com.ibmec.researchstars.user.UserRole;
import java.util.HashSet;
import java.util.Set;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Regras de autenticação e autocadastro (RF-01, RF-02). */
@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final ProfessorRepository professorRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            ProfessorRepository professorRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.professorRepository = professorRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    /**
     * RF-01 — cria o usuário (PROFESSOR) e o perfil de professor (status PENDING).
     */
    public ProfessorDetailResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("E-mail já cadastrado: " + request.email());
        }
        if (professorRepository.existsByLattesNumberAndIdNot(request.lattesNumber(), -1L)) {
            throw new DuplicateResourceException(
                    "Número Lattes já cadastrado: " + request.lattesNumber());
        }

        User user = userRepository.save(
                new User(
                        request.email(),
                        passwordEncoder.encode(request.password()),
                        UserRole.PROFESSOR));

        Professor professor = new Professor();
        professor.setUserId(user.getId());
        professor.setName(request.name());
        professor.setEmail(request.email());
        professor.setLattesNumber(request.lattesNumber());
        Set<Long> courseIds = request.courseIds() == null ? Set.of() : request.courseIds();
        professor.setCourseIds(new HashSet<>(courseIds));
        professor.setStatus(Professor.Status.PENDING);

        return ProfessorMapper.toDetail(professorRepository.save(professor));
    }

    /** RF-02 — autentica e emite o JWT. */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository
                .findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Credenciais inválidas"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Credenciais inválidas");
        }

        Long professorId = null;
        String name = user.getEmail();
        if (user.getRole() == UserRole.PROFESSOR) {
            Professor professor = professorRepository.findByUserId(user.getId()).orElse(null);
            if (professor != null) {
                professorId = professor.getId();
                name = professor.getName();
            }
        }

        AuthUser authUser = new AuthUser(user.getId(), professorId, user.getRole().name(), user.getEmail(), name);
        String token = jwtService.generateToken(authUser);

        return new LoginResponse(token, user.getRole().name(), name, user.getEmail(), professorId);
    }
}
