package br.com.clyvo.kura.tutor.laudo.bff.api;

import br.com.clyvo.kura.tutor.shared.exception.MongoExceptionHandlerBase;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 503 {@code LAUDOS_INDISPONIVEL} e 400 de parâmetro, escopados ao pacote laudo. */
@RestControllerAdvice(basePackages = "br.com.clyvo.kura.tutor.laudo")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LaudoExceptionHandler extends MongoExceptionHandlerBase {

    @Override
    protected String modulo() {
        return "Laudos";
    }

    @Override
    protected Erro erroIndisponivel() {
        return new Erro("LAUDOS_INDISPONIVEL",
                "Os laudos estão temporariamente indisponíveis. Tente novamente em instantes.");
    }
}
