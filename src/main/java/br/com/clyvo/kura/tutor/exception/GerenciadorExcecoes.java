package br.com.clyvo.kura.tutor.exception;

import br.com.clyvo.kura.tutor.shared.exception.AccountInactiveException;
import br.com.clyvo.kura.tutor.shared.exception.AccountLockedException;
import br.com.clyvo.kura.tutor.shared.exception.ConflictException;
import br.com.clyvo.kura.tutor.shared.exception.ForbiddenException;
import br.com.clyvo.kura.tutor.shared.exception.GoneException;
import br.com.clyvo.kura.tutor.shared.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Centraliza o tratamento de exceções da API.
 *
 * Mapeamento:
 *   400 — MethodArgumentNotValidException (Bean Validation)
 *   401 — BadCredentialsException
 *   404 — NotFoundException (e subclasses, ex: RecursoNaoEncontradoException)
 *   409 — ConflictException (convite já utilizado, race condition UK)
 *   410 — GoneException (convite expirado)
 *   422 — RegraDeNegocioException
 *   500 — Exception genérica
 */
@RestControllerAdvice
public class GerenciadorExcecoes {

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> headerObrigatorio(
            MissingRequestHeaderException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(montarErro(400, "Header obrigatório ausente: " + ex.getHeaderName(), req.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> gerenciarValidacoes(
            MethodArgumentNotValidException ex) {
        Map<String, String> erros = new HashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach((FieldError e) -> erros.put(e.getField(), e.getDefaultMessage()));
        return ResponseEntity.badRequest().body(erros);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String, Object>> naoEncontrado(
            NotFoundException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(montarErro(404, ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String, Object>> conflito(
            ConflictException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(montarErro(409, ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<Map<String, Object>> versaoDesatualizada(
            ObjectOptimisticLockingFailureException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(montarErro(409, "Versão desatualizada. Recarregue o recurso e tente novamente.",
                        req.getRequestURI()));
    }

    @ExceptionHandler(GoneException.class)
    public ResponseEntity<Map<String, Object>> recursoExpirado(
            GoneException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(montarErro(410, ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<Map<String, Object>> regraDeNegocio(
            RegraDeNegocioException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(montarErro(422, ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Map<String, Object>> acessoNegado(
            ForbiddenException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(montarErro(403, ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(AccountInactiveException.class)
    public ResponseEntity<Map<String, Object>> contaDesativada(
            AccountInactiveException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(montarErro(403, ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<Map<String, Object>> contaBloqueada(
            AccountLockedException ex, HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.LOCKED)
                .body(montarErro(423, ex.getMessage(), req.getRequestURI()));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> credenciaisInvalidas(
            HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(montarErro(401, "E-mail ou senha invalidos.", req.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> generico(HttpServletRequest req) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(montarErro(500, "Erro interno. Tente novamente.", req.getRequestURI()));
    }

    private Map<String, Object> montarErro(int status, String mensagem, String path) {
        Map<String, Object> corpo = new HashMap<>();
        corpo.put("timestamp", LocalDateTime.now().toString());
        corpo.put("status", status);
        corpo.put("mensagem", mensagem);
        corpo.put("path", path);
        return corpo;
    }
}
