package br.com.clyvo.kura.tutor.documento.bff.api;

import br.com.clyvo.kura.tutor.shared.exception.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * 413 para o teto do multipart ({@code spring.servlet.multipart.max-*}). Este advice NÃO é escopado
 * por pacote de propósito: a exceção nasce ao resolver o multipart, antes de existir handler
 * method, e um advice com {@code basePackages} não é consultado nesse momento. Trata só esta exceção.
 */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UploadExcedidoExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> excedido(MaxUploadSizeExceededException ex, HttpServletRequest req) {
        return ResponseEntity.status(413).body(ApiError.of(413, "ARQUIVO_MUITO_GRANDE",
                "Arquivo acima do limite de 5 MB.", req.getRequestURI()));
    }
}
