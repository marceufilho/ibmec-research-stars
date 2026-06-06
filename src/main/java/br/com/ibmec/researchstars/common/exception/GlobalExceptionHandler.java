package br.com.ibmec.researchstars.common.exception;

import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Tradução de exceções comuns para o formato de erro esperado pelo frontend.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    public record FieldErrorEntry(String field, String message) {
    }

    public record ApiError(
            Instant timestamp, int status, String message, List<FieldErrorEntry> fieldErrors) {
    }

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ApiError> handleDuplicate(DuplicateResourceException ex) {
        return build(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(ResourceNotFoundException ex) {
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> handleBadCredentials(BadCredentialsException ex) {
        return build(HttpStatus.UNAUTHORIZED, ex.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldErrorEntry> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorEntry(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Erro de validação", fieldErrors);
    }

    private ResponseEntity<ApiError> build(
            HttpStatus status, String message, List<FieldErrorEntry> fieldErrors) {
        return ResponseEntity.status(status)
                .body(new ApiError(Instant.now(), status.value(), message, fieldErrors));
    }
}
