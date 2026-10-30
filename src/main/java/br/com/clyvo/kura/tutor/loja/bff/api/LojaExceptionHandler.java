// KURA-LOJA — catálogo MongoDB (Sprint 4). Escopo isolado: depende do domínio; o domínio não depende dele.
package br.com.clyvo.kura.tutor.loja.bff.api;

import br.com.clyvo.kura.tutor.loja.application.LojaException;
import br.com.clyvo.kura.tutor.shared.exception.ApiError;
import br.com.clyvo.kura.tutor.shared.exception.MongoExceptionHandlerBase;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Handlers do pacote loja (S4-JAVA-DESIGN §5.1). Escopo restrito ao pacote para não mudar
 * a resposta de nenhum outro endpoint (o GlobalExceptionHandler devolve 500 para estas).
 * O tratamento comum (503 / 400) vem de {@link MongoExceptionHandlerBase}; aqui só os códigos da loja.
 * Catálogo, carrinho e pedidos são um serviço só: Mongo fora é 503 CATALOGO_INDISPONIVEL nos três.
 */
@RestControllerAdvice(basePackages = "br.com.clyvo.kura.tutor.loja")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LojaExceptionHandler extends MongoExceptionHandlerBase {

    @Override
    protected String modulo() {
        return "Loja";
    }

    @Override
    protected Erro erroIndisponivel() {
        return new Erro("CATALOGO_INDISPONIVEL",
                "O catálogo da loja está temporariamente indisponível. Tente novamente em instantes.");
    }

    @Override
    protected Erro erroParametro(String nomeDoParametro) {
        if ("categoria".equals(nomeDoParametro)) {
            return new Erro("CATEGORIA_INVALIDA",
                    "Categoria inválida. Use ALIMENTACAO, HIGIENE, ACESSORIOS ou BRINQUEDOS.");
        }
        return super.erroParametro(nomeDoParametro);
    }

    /** Erros de regra do carrinho e dos pedidos: status, código e (opcional) detalhes vêm da própria exceção. */
    @ExceptionHandler(LojaException.class)
    public ResponseEntity<ApiError> regraDaLoja(LojaException ex, HttpServletRequest req) {
        return ResponseEntity.status(ex.status()).body(ApiError.of(
                ex.status(), ex.codigo(), ex.getMessage(), req.getRequestURI(), ex.detalhes()));
    }
}
