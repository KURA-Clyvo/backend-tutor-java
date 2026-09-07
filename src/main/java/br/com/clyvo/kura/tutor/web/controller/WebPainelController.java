// KURA-WEB — camada de visualização servidor (Sprint 3, Java Advanced).
// Escopo isolado: depende do domínio; o domínio não depende dela.
// Ver docs/ADR-web.md.
package br.com.clyvo.kura.tutor.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Painel pós-login do chain {@code /web/**} (SJ3-05). Destino real de
 * {@code formLogin.defaultSuccessUrl(...)} em {@link
 * br.com.clyvo.kura.tutor.web.config.WebSecurityConfig} — qualquer perfil
 * autenticado (TUTOR ou SUPORTE) alcança esta rota; o conteúdo condicional
 * por perfil é decidido no template ({@code sec:authorize}), não aqui. Ver
 * {@code templates/web/painel.html}.
 */
@Controller
public class WebPainelController {

    @GetMapping("/web/painel")
    public String painel() {
        return "web/painel";
    }
}
