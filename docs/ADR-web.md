# ADR — Camada de visualização servidor (`KURA-WEB`)

> Esta branch não mescla em `main` (ruling D-J4, `KURA_BACKLOG_SPRINT3_JAVA.md` §1.1).
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

## Por que a checagem de bloqueio usa a janela, não a flag crua — e por que isso SOZINHO não bastava

`ContaTutor.isBloqueada()` (sem argumentos) é a checagem **sem janela** —
inerte no login da API porque `JwtAuthenticationFilter` nunca passa por
`AuthenticationManager`/`AccountStatusUserDetailsChecker`. `formLogin` torna
esse caminho **vivo**. Por isso `WebAuthenticationProvider` usa
`ContaTutor.isBloqueada(LocalDateTime agora, int janelaMinutos)`, a mesma
sobrecarga e a mesma propriedade (`kura.auth.janela-bloqueio-minutos`) que
`AuthService` usa no login da API — quando ELE decide, ele decide certo.

🔴 **Isso não fechava o problema, e foi medido por revisão independente
(`sj3-03-revisao.md`, achado `G2-1`, M4).** `WebAuthenticationProvider` de
fato nunca *chama* `UserDetailsServiceImpl` — mas o `AuthenticationManager`
que hospeda os dois providers do chain `/web/**` tinha **parent**: o
`AuthenticationManager` GLOBAL de produto, montado por
`AuthenticationConfiguration`, contendo um `DaoAuthenticationProvider`
autoconfigurado sobre `UserDetailsServiceImpl` (o único
`UserDetailsService` do contexto). Quando `WebAuthenticationProvider` lança
`BadCredentialsException` — o que acontece quando o login não é resolvido
nem como e-mail de tutor nem como login de suporte, por exemplo um `idConta`
numérico puro — o `ProviderManager` cai no parent, que resolve esse login
como tutor por ID e usa `ContaTutor.isBloqueada()` **sem janela**. Medido:
conta com bloqueio expirado há 5 minutos recebeu `LockedException` por essa
rota, mesmo a janela de 15 minutos já ter passado — a trava permanente que a
SJ3-02 fechou, ressuscitada por fall-through, não por delegação explícita.

**Fix, medido e travado em teste:** `WebSecurityConfig.webSecurityFilterChain(...)`
constrói o `AuthenticationManager` deste chain com
`AuthenticationManagerBuilder.parentAuthenticationManager(null)` — sem
parent, o `ProviderManager` do chain `/web/**` só conhece
`WebAuthenticationProvider`; não há fall-through possível para
`UserDetailsServiceImpl` por nenhum login. Regressão travada em
`WebAuthenticationManagerParentIsolationTest` (falha contra o código antes do
fix, verde depois — inclusive controle positivo de que TUTOR com bloqueio
expirado e SUPORTE continuam autenticando normalmente).

## Limite do pacote `web` — e o que fica FORA dele, de verdade

A maior parte do painel mora em `br.com.clyvo.kura.tutor.web` (`config/`,
`security/`, `domain/`, `controller/`) — mas **nem tudo**: medido por
`git diff --numstat` contra `main`, **4 dos 10 arquivos da branch vivem fora
do pacote**:

| Arquivo | Fora do pacote porque | Custo de demolição, SE um dia mesclar |
|---|---|---|
| `pom.xml` | dependência é declaração de projeto, não de pacote | remover as 2 dependências Thymeleaf + o bloco de comentário — edição de arquivo de produto |
| `db/callback/afterMigrate__seeds_dev.sql` | seed de dev é arquivo compartilhado, pré-existente | remover a seção 11 — edição de arquivo de produto |
| `db/migration/V20__web_usuario_suporte.sql` | migration é numerada globalmente, não por pacote | `DROP TABLE` + remover a linha da `flyway_schema_history` |
| este `ADR-web.md` | documentação, não código | `rm` — trivial |

**A demolição continua sendo barata, mas não é só `git branch -D` e nada
mais** — essa frase descrevia só os arquivos dentro do pacote. Ela vale
porque esta branch **não mescla** (ver cabeçalho): enquanto isso for verdade,
o comando certo é mesmo `git branch -D <nome-da-branch-atual>`, apagando os
10 arquivos de uma vez, os 4 de fora incluídos — a ressalva importa só se a
ruling D-J4 mudar um dia.

O pacote **depende** do domínio (`ContaTutorRepository`, `ContaTutor`,
`PasswordEncoder`) só para LEITURA; o domínio não importa nada de `web`
(verificado: `grep -rn "tutor\.web\." src/main/java | grep -v "/web/"` → 0
linhas, com controle positivo — o mesmo grep acha os 5 arquivos dentro do
pacote).

**Nenhum arquivo de produto de RUNTIME precisou ser editado** para a SJ3-03 —
`SecurityConfig.java`, `UserDetailsServiceImpl.java`,
`JwtAuthenticationFilter.java`, `ContaTutor.java` não aparecem no diff. Dois
arquivos de produto **pré-existentes** receberam só ADIÇÃO (nenhuma linha
removida): `pom.xml` (as 2 dependências Thymeleaf) e o seed de dev (a seção
11). "Zero arquivo de produto modificado" seria impreciso; a frase certa é
"zero arquivo de produto de runtime editado, dois arquivos de configuração/dado
de dev só receberam adição".

## Por que a tabela `WEB_USUARIO_SUPORTE` não tem FK para tabela de produto

FK de saída (de `WEB_USUARIO_SUPORTE` para uma tabela de produto) transforma
o `DROP TABLE` da demolição em negociação com o schema de produto. A tabela é
autocontida: nenhuma coluna referencia `CONTA_TUTOR`, `TUTOR` ou qualquer
outra tabela `.NET`/Java-owned de produto. Ver `V20__web_usuario_suporte.sql`.
