package br.com.clyvo.kura.tutor.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Handler global de exceções.
 * <p>
 * Retorna JSON padronizado conforme exigido pelo briefing FIAP:
 * {@code timestamp}, {@code status}, {@code message}, {@code path}.
 * <p>
 * Design Pattern aplicado: Chain of Responsibility implícito do Spring MVC.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    // ── Entidade não encontrada (404) ────────────────────────────────────

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> handleNaoEncontrado(
            RecursoNaoEncontradoException ex, HttpServletRequest req) {

        return build(HttpStatus.NOT_FOUND, ex.getMessage(), req.getRequestURI());
    }

    // ── Regra de negócio violada (422) ──────────────────────────────────

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroResponse> handleRegraDeNegocio(
            RegraDeNegocioException ex, HttpServletRequest req) {

        return build(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), req.getRequestURI());
    }

    // ── Credenciais inválidas (401) ──────────────────────────────────────

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErroResponse> handleCredenciais(
            BadCredentialsException ex, HttpServletRequest req) {

        return build(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.", req.getRequestURI());
    }

    // ── Bean Validation — @RequestBody com @Valid (400) ─────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroValidacaoResponse> handleValidacao(
            MethodArgumentNotValidException ex, HttpServletRequest req) {

        Map<String, String> campos = new HashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            campos.put(fe.getField(), fe.getDefaultMessage());
        }

        var body = new ErroValidacaoResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Erro de validação nos campos enviados.",
                req.getRequestURI(),
                campos
        );
        return ResponseEntity.badRequest().body(body);
    }

    // ── Bean Validation — @PathVariable / @RequestParam (400) ───────────

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErroResponse> handleConstraint(
            ConstraintViolationException ex, HttpServletRequest req) {

        return build(HttpStatus.BAD_REQUEST, ex.getMessage(), req.getRequestURI());
    }

    // ── Fallback genérico (500) ──────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleGenerico(
            Exception ex, HttpServletRequest req) {

        // Não expõe stack trace ao cliente — loga internamente
        return build(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno. Tente novamente ou contate o suporte.", req.getRequestURI());
    }

    // ── Helpers ──────────────────────────────────────────────────────────

    private ResponseEntity<ErroResponse> build(HttpStatus status, String message, String path) {
        var body = new ErroResponse(LocalDateTime.now(), status.value(), message, path);
        return ResponseEntity.status(status).body(body);
    }

    // ── Records de resposta ───────────────────────────────────────────────

    public record ErroResponse(
            LocalDateTime timestamp,
            int status,
            String message,
            String path
    ) {}

    public record ErroValidacaoResponse(
            LocalDateTime timestamp,
            int status,
            String message,
            String path,
            Map<String, String> campos
    ) {}
}
