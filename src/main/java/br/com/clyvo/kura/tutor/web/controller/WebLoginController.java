// KURA-WEB — camada de visualização servidor (Sprint 3, Java Advanced).
// Escopo isolado: depende do domínio; o domínio não depende dela.
// Ver docs/ADR-web.md.
package br.com.clyvo.kura.tutor.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Página de login do chain {@code /web/**}. Fix do achado {@code G2-2}
 * (sj3-03-revisao.md, M5): a URL de processamento padrão do
 * {@code formLogin} ({@code /login}) não casa com {@code securityMatcher("/web/**")}
 * e por isso nunca era alcançada — {@link
 * br.com.clyvo.kura.tutor.web.security.WebAuthenticationProvider} era código
 * morto. {@link br.com.clyvo.kura.tutor.web.config.WebSecurityConfig} agora
 * declara {@code loginPage("/web/login")} e {@code loginProcessingUrl("/web/login")},
 * ambos dentro do matcher — este controller só serve o GET (formulário); o
 * POST é interceptado pelo {@code UsernamePasswordAuthenticationFilter} antes
 * de chegar aqui.
 *
 * SJ3-05 acrescentou um {@code <style>} inline curto e {@code lang="pt-br"}
 * ao template — continua sem framework de CSS e sem JS, por decisão
 * explícita do brief daquela task (não é regressão de escopo).
 */
@Controller
public class WebLoginController {

    @GetMapping("/web/login")
    public String login() {
        return "web/login";
    }
}
