// KURA-WEB — camada de visualização servidor (Sprint 3, Java Advanced).
// Escopo isolado: depende do domínio; o domínio não depende dela.
// Ver docs/ADR-web.md.
package br.com.clyvo.kura.tutor.web.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint mínimo, só para a prova de mutação de autorização da SJ3-03 —
 * exclusivo do perfil SUPORTE ({@code .hasRole("SUPORTE")} em
 * {@link br.com.clyvo.kura.tutor.web.config.WebSecurityConfig}). A tela de
 * verdade (busca de conta, destravar, reenviar convite) é escopo da SJ3-07;
 * este método só precisa existir e estar sob {@code /web/suporte/**} para
 * que a regra de autorização tenha uma rota real para proteger.
 */
@RestController
public class SuporteController {

    @GetMapping("/web/suporte/contas")
    public String contas(Authentication authentication) {
        return "painel de suporte — autenticado como " + authentication.getName();
    }
}
