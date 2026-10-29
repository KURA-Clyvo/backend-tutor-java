package br.com.clyvo.kura.tutor.shared.exception;

import com.mongodb.MongoSocketException;
import com.mongodb.MongoTimeoutException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Tratamento comum dos modulos que dependem do Mongo (loja, documento): "Mongo indisponivel" vira
 * 503 e parametro de tipo errado vira 400. Cada modulo estende, declara o PROPRIO
 * {@code @RestControllerAdvice(basePackages=...)} (isolamento: nenhum outro endpoint muda de
 * resposta) e informa o seu codigo de erro. Nunca ecoa URI nem credencial do Mongo: loga so a classe.
 */
public abstract class MongoExceptionHandlerBase {

    /** Codigo e mensagem de um erro de modulo. */
    public record Erro(String codigo, String mensagem) { }

    protected final Logger log = LoggerFactory.getLogger(getClass());

    protected abstract String modulo();

    protected abstract Erro erroIndisponivel();

    /** Por padrao: 400 VALIDACAO_INVALIDA. O modulo sobrescreve para codigos proprios por parametro. */
    protected Erro erroParametro(String nomeDoParametro) {
        return new Erro("VALIDACAO_INVALIDA", "Parâmetro inválido: " + nomeDoParametro);
    }

    @ExceptionHandler({MongoTimeoutException.class, MongoSocketException.class,
            DataAccessResourceFailureException.class})
    public ResponseEntity<ApiError> mongoIndisponivel(Exception ex, HttpServletRequest req) {
        log.warn("{}: banco de documentos indisponível ({})", modulo(), ex.getClass().getSimpleName());
        return resposta(503, erroIndisponivel(), req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> parametroInvalido(MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return resposta(400, erroParametro(ex.getName()), req);
    }

    protected static ResponseEntity<ApiError> resposta(int status, Erro erro, HttpServletRequest req) {
        return ResponseEntity.status(status)
                .body(ApiError.of(status, erro.codigo(), erro.mensagem(), req.getRequestURI()));
    }
}
