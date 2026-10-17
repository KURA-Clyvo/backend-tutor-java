// KURA-LOJA — catálogo MongoDB (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.bff.api;

import br.com.clyvo.kura.tutor.shared.exception.ApiError;
import com.mongodb.MongoSocketException;
import com.mongodb.MongoTimeoutException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Handlers do pacote loja (S4-JAVA-DESIGN §5.1). Escopo restrito ao pacote para não mudar
 * a resposta de nenhum outro endpoint (o GlobalExceptionHandler devolve 500 para estas).
 * Nunca ecoa URI nem credencial do Mongo: loga só o nome da classe da exceção.
 */
@Slf4j
@RestControllerAdvice(basePackages = "br.com.clyvo.kura.tutor.loja")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LojaExceptionHandler {

    @ExceptionHandler({MongoTimeoutException.class, MongoSocketException.class,
            DataAccessResourceFailureException.class})
    public ResponseEntity<ApiError> mongoIndisponivel(Exception ex, HttpServletRequest req) {
        log.warn("Loja: banco de documentos indisponível ({})", ex.getClass().getSimpleName());
        return ResponseEntity.status(503)
                .body(ApiError.of(503, "CATALOGO_INDISPONIVEL",
                        "O catálogo da loja está temporariamente indisponível. Tente novamente em instantes.",
                        req.getRequestURI()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> parametroInvalido(
            MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        if ("categoria".equals(ex.getName())) {
            return ResponseEntity.badRequest()
                    .body(ApiError.of(400, "CATEGORIA_INVALIDA",
                            "Categoria inválida. Use ALIMENTACAO, HIGIENE, ACESSORIOS ou BRINQUEDOS.",
                            req.getRequestURI()));
        }
        return ResponseEntity.badRequest()
                .body(ApiError.of(400, "VALIDACAO_INVALIDA",
                        "Parâmetro inválido: " + ex.getName(), req.getRequestURI()));
    }
}
