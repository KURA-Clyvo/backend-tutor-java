// KURA-WEB — camada de visualização servidor (Sprint 3, Java Advanced).
// Escopo isolado: depende do domínio; o domínio não depende dela.
// Ver docs/ADR-web.md.
package br.com.clyvo.kura.tutor.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Página de acesso negado do chain {@code /web/**} (SJ3-05). Servida por
 * FORWARD explícito de aplicação a partir do {@code accessDeniedHandler} em
 * {@link br.com.clyvo.kura.tutor.web.config.WebSecurityConfig} — não é o
 * {@code sendError()}/FORWARD de container que a fix wave da SJ3-03
 * encontrou quebrado (ver javadoc de {@code WebSecurityConfig} e
 * {@code docs/ADR-web.md}). O status HTTP 403 é setado ANTES do forward,
 * neste controller apenas o corpo é renderizado.
 *
 * Também alcançável por GET direto (bookmark, link) — nesse caso responde
 * 200, porque não passou pelo {@code AccessDeniedHandler}.
 */
@Controller
public class Web403Controller {

    @GetMapping("/web/403")
    public String acessoNegado() {
        return "web/403";
    }
}
