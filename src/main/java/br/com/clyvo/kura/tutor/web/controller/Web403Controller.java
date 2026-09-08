// KURA-WEB — camada de visualização servidor (Sprint 3, Java Advanced).
// Escopo isolado: depende do domínio; o domínio não depende dela.
// Ver docs/ADR-web.md.
package br.com.clyvo.kura.tutor.web.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Página de acesso negado do chain {@code /web/**} (SJ3-05). Servida por
 * FORWARD explícito de aplicação a partir do {@code accessDeniedHandler} em
 * {@link br.com.clyvo.kura.tutor.web.config.WebSecurityConfig} — não é o
 * {@code sendError()}/FORWARD de container que a fix wave da SJ3-03
 * encontrou quebrado (ver javadoc de {@code WebSecurityConfig} e
 * {@code docs/ADR-web.md}). O status HTTP 403 é setado ANTES do forward,
 * neste controller apenas o corpo é renderizado.
 *
 * {@code @RequestMapping} sem restrição de verbo — não {@code @GetMapping} —
 * de propósito: o forward de aplicação PRESERVA o método HTTP da requisição
 * original (fix wave da SJ3-05, achado G2-A). Um {@code POST}/{@code PUT}/
 * {@code DELETE} negado (a regra {@code /web/suporte/**} casa por PATH, não
 * por método — já protege qualquer verbo hoje) ou uma falha de CSRF em
 * {@code /web/login} com sessão autenticada chegam aqui como o MESMO verbo.
 * Com {@code @GetMapping}, isso lançava
 * {@code HttpRequestMethodNotSupportedException} dentro do forward, que
 * escapava do chain {@code /web/**} e caía no {@code GlobalExceptionHandler}
 * de produto como {@code 500 ERRO_INTERNO} — sobrescrevendo o {@code 403}
 * setado antes pelo {@code accessDeniedHandler}. Medido por HTTP real
 * (4 vetores: POST, PUT, DELETE em {@code /web/suporte/**} e CSRF inválido
 * autenticado em {@code /web/login}) — ver {@code sj3-05-revisao.md} (frente
 * A2) e {@code sj3-05-fixwave.md} no repo de planejamento.
 *
 * Também alcançável por GET direto (bookmark, link), mas SÓ AUTENTICADO:
 * a rota não é {@code permitAll}, então um GET anônimo é interceptado pelo
 * chain e vira {@code 302} para {@code /web/login}. Medido nos 3 estados
 * (fix wave da SJ3-05): anônimo {@code 302}, TUTOR {@code 200}, SUPORTE
 * {@code 200} — nos dois autenticados responde 200, não 403, porque não
 * passou pelo {@code AccessDeniedHandler}. A versão anterior deste javadoc
 * dizia apenas "responde 200", sem a condição, e a medição a refutou no
 * caso anônimo.
 */
@Controller
public class Web403Controller {

    @RequestMapping("/web/403")
    public String acessoNegado() {
        return "web/403";
    }
}
