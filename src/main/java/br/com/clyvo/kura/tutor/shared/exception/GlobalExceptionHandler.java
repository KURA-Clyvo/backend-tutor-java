package br.com.clyvo.kura.tutor.shared.exception;

import br.com.clyvo.kura.tutor.exception.RegraDeNegocioException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

/**
 * Centraliza o tratamento de exceções da API.
 *
 * Mapeamento:
 *   400 — MethodArgumentNotValidException, ConstraintViolationException,
 *          MissingRequestHeaderException, IllegalArgumentException
 *   401 — BadCredentialsException
 *   403 — AccessDeniedException, ForbiddenException, AccountInactiveException
 *   404 — NotFoundException
 *   409 — ConflictException, ObjectOptimisticLockingFailureException,
 *          IllegalStateException, DataIntegrityViolationException
 *   410 — GoneException
 *   422 — RegraDeNegocioException
 *   423 — AccountLockedException
 *   500 — Exception (loga stack, nunca vaza para o cliente)
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─── 400 ─────────────────────────────────────────────────────────────────

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validacaoInvalida(
            MethodArgumentNotValidException ex, HttpServletRequest req) {
        List<String> detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "VALIDACAO_INVALIDA",
                        "Campos inválidos na requisição.", req.getRequestURI(), detalhes));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiError> constraintViolation(
            ConstraintViolationException ex, HttpServletRequest req) {
        List<String> detalhes = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList();
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "VALIDACAO_INVALIDA",
                        "Parâmetros inválidos.", req.getRequestURI(), detalhes));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiError> headerAusente(
            MissingRequestHeaderException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "HEADER_AUSENTE",
                        "Header obrigatório ausente: " + ex.getHeaderName(), req.getRequestURI()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> argumentoInvalido(
            IllegalArgumentException ex, HttpServletRequest req) {
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "ARGUMENTO_INVALIDO", ex.getMessage(), req.getRequestURI()));
    }

    // ─── 401 ─────────────────────────────────────────────────────────────────

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiError> credenciaisInvalidas(HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of(401, "CREDENCIAIS_INVALIDAS",
                        "E-mail ou senha inválidos.", req.getRequestURI()));
    }

    // ─── 403 ─────────────────────────────────────────────────────────────────

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiError> accessDenied(HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(403, "ACESSO_NEGADO", "Acesso negado.", req.getRequestURI()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiError> proibido(ForbiddenException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(403, "ACESSO_NEGADO", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(AccountInactiveException.class)
    public ResponseEntity<ApiError> contaDesativada(
            AccountInactiveException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of(403, "CONTA_DESATIVADA", ex.getMessage(), req.getRequestURI()));
    }

    // ─── 404 ─────────────────────────────────────────────────────────────────

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> naoEncontrado(NotFoundException ex, HttpServletRequest req) {
        log.warn("404 on [{} {}]: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(404, "NAO_ENCONTRADO", ex.getMessage(), req.getRequestURI()));
    }

    /**
     * Spring 6.1+ (Boot 3.2) resolve rota sem handler tentando servir como recurso
     * estático antes de desistir — sem este handler específico, cai no catch-all
     * de 500 abaixo, mascarando um simples "rota inexistente" como erro interno.
     * Descoberto na TASK-31 testando que endpoints deliberadamente não implementados
     * (ex.: PATCH .../notificacoes/{id}/lida) respondem 404, não 500.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> rotaInexistente(NoResourceFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiError.of(404, "NAO_ENCONTRADO", "Rota inexistente.", req.getRequestURI()));
    }

    // ─── 409 ─────────────────────────────────────────────────────────────────

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> conflito(ConflictException ex, HttpServletRequest req) {
        log.warn("409 on [{} {}]: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, "CONFLITO", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> versaoDesatualizada(HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, "VERSAO_DESATUALIZADA",
                        "Versão desatualizada. Recarregue o recurso e tente novamente.",
                        req.getRequestURI()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> estadoInvalido(
            IllegalStateException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, "ESTADO_INVALIDO", ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> integridadeViolada(HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiError.of(409, "INTEGRIDADE_VIOLADA",
                        "Operação viola restrição de integridade de dados.",
                        req.getRequestURI()));
    }

    // ─── 410 ─────────────────────────────────────────────────────────────────

    @ExceptionHandler(GoneException.class)
    public ResponseEntity<ApiError> recursoExpirado(GoneException ex, HttpServletRequest req) {
        log.warn("410 on [{} {}]: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.GONE)
                .body(ApiError.of(410, "RECURSO_EXPIRADO", ex.getMessage(), req.getRequestURI()));
    }

    // ─── 422 ─────────────────────────────────────────────────────────────────

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ApiError> regraDeNegocio(
            RegraDeNegocioException ex, HttpServletRequest req) {
        log.warn("422 on [{} {}]: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ApiError.of(422, "REGRA_DE_NEGOCIO", ex.getMessage(), req.getRequestURI()));
    }

    // ─── 423 ─────────────────────────────────────────────────────────────────

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiError> contaBloqueada(
            AccountLockedException ex, HttpServletRequest req) {
        log.warn("423 on [{} {}]: {}", req.getMethod(), req.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.LOCKED)
                .body(ApiError.of(423, "CONTA_BLOQUEADA", ex.getMessage(), req.getRequestURI()));
    }

    // ─── 500 ─────────────────────────────────────────────────────────────────

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> erroInterno(Exception ex, HttpServletRequest req) {
        log.error("Unhandled exception on [{}] {}: {}", req.getMethod(), req.getRequestURI(), ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of(500, "ERRO_INTERNO", "Erro interno. Tente novamente.",
                        req.getRequestURI()));
    }
}
