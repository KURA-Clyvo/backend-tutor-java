# ADR — Camada de visualização servidor (`KURA-WEB`)

> Branch `web-rubrica` (não mescla em `main` — ruling D-J4, `KURA_BACKLOG_SPRINT3_JAVA.md` §1.1).
> Escopo: Sprint 3, disciplina Java Advanced. Todo arquivo dentro de
> `br.com.clyvo.kura.tutor.web` carrega o marcador `KURA-WEB` e cita este documento.

## Por que dois `SecurityFilterChain`, não um

O chain de produto (`shared/config/SecurityConfig.java`) é uma API JSON stateless:
CSRF desligado, sem sessão, `Authorization: Bearer` + JWT. Um painel Thymeleaf
precisa exatamente do oposto nos três pontos — sessão HTTP, CSRF ligado (é
formulário, não API) e `formLogin`. Não há como um único
`SecurityFilterChain` servir as duas semânticas ao mesmo tempo; a solução
padrão do Spring Security para isso é `securityMatcher` + múltiplos beans
`SecurityFilterChain`, cada um com sua config, escolhidos por prefixo de rota.

`WebSecurityConfig` declara o segundo chain, com `@Order(1)` e
`securityMatcher("/web/**")`. Medido nesta task (não presumido): o chain de
produto não tem `@Order` nenhum (`grep -c "@Order" SecurityConfig.java` → 0),
o que o Spring já trata como `Ordered.LOWEST_PRECEDENCE` — exatamente o que o
Spring Security exige do chain que casa "qualquer requisição"
(`anyRequest()`, sem `securityMatcher` próprio). Um `@Order(1)` no chain novo
já garante a ordem correta sem editar o arquivo de produto. Ver o artefato da
task (`sj3-03-report.md`, repo de planejamento) para a evidência de boot
completo confirmando isso.

## Por que `AuthenticationProvider` próprio, e não um segundo `UserDetailsService`

Já existe `UserDetailsServiceImpl` (produto), e `JwtAuthenticationFilter` o
injeta **por tipo**, sem `@Qualifier`. Declarar um segundo bean de
`UserDetailsService` para o painel quebraria o boot inteiro com
`NoUniqueBeanDefinitionException` — só apareceria rodando a aplicação, não na
compilação.

A saída é `WebAuthenticationProvider`, que implementa `AuthenticationProvider`
diretamente (não `UserDetailsService`) e resolve os dois perfis à mão:
- **TUTOR**: por `ContaTutorRepository` (produto, só leitura — a seta aponta
  front → domínio, nunca o inverso);
- **SUPORTE**: por `WebUsuarioSuporteRepository` (tabela nova desta branch,
  `WEB_USUARIO_SUPORTE`, V20).

Ele **não é** `@Component`/`@Bean` avulso — é instanciado dentro do próprio
`WebSecurityConfig.webSecurityFilterChain(...)` e registrado só ali via
`http.authenticationProvider(...)`. Se fosse um bean gerenciado pelo Spring,
a auto-detecção global de `AuthenticationProvider`
(`AuthenticationConfiguration.getAuthenticationManager()`, que o bean
`authenticationManager` de `SecurityConfig` de produto usa) o incluiria no
`AuthenticationManager` **global** — contaminando um caminho de produto sem
que nenhum arquivo dele fosse editado. Escopar a instância ao chain evita
isso por construção, não por convenção.

## Por que a checagem de bloqueio usa a janela, não a flag crua

`ContaTutor.isBloqueada()` (sem argumentos) é a checagem **sem janela** —
inerte no login da API porque `JwtAuthenticationFilter` nunca passa por
`AuthenticationManager`/`AccountStatusUserDetailsChecker`. `formLogin` torna
esse caminho **vivo**: se `WebAuthenticationProvider` delegasse para
`UserDetailsServiceImpl` (que usa a flag sem janela), o bug que a SJ3-02
fechou — 5 senhas erradas = conta bloqueada para sempre — ressuscitaria pela
rota do painel. Por isso `WebAuthenticationProvider` usa
`ContaTutor.isBloqueada(LocalDateTime agora, int janelaMinutos)`, a mesma
sobrecarga e a mesma propriedade (`kura.auth.janela-bloqueio-minutos`) que
`AuthService` usa no login da API.

## Limite do pacote `web`

Tudo do painel mora em `br.com.clyvo.kura.tutor.web` (`config/`, `security/`,
`domain/`, `controller/`) — pacote único, para que a demolição pós-oral seja
`git branch -D web-rubrica` e nada mais. O pacote **depende** do domínio
(`ContaTutorRepository`, `ContaTutor`, `PasswordEncoder`) só para LEITURA; o
domínio não importa nada de `web`. Nenhum arquivo de produto precisou ser
editado para a SJ3-03 (ver §3.2 do brief e a evidência no artefato da task).

## Por que a tabela `WEB_USUARIO_SUPORTE` não tem FK para tabela de produto

FK de saída (de `WEB_USUARIO_SUPORTE` para uma tabela de produto) transforma
o `DROP TABLE` da demolição em negociação com o schema de produto. A tabela é
autocontida: nenhuma coluna referencia `CONTA_TUTOR`, `TUTOR` ou qualquer
outra tabela `.NET`/Java-owned de produto. Ver `V20__web_usuario_suporte.sql`.
