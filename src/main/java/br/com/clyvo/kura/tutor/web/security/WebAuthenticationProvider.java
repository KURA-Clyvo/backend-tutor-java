// KURA-WEB — camada de visualização servidor (Sprint 3, Java Advanced).
// Escopo isolado: depende do domínio; o domínio não depende dela.
// Ver docs/ADR-web.md.
package br.com.clyvo.kura.tutor.web.security;

import br.com.clyvo.kura.tutor.auth.api.dto.LoginRequest;
import br.com.clyvo.kura.tutor.auth.application.AuthService;
import br.com.clyvo.kura.tutor.auth.domain.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.entity.ContaTutor;
import br.com.clyvo.kura.tutor.shared.exception.AccountInactiveException;
import br.com.clyvo.kura.tutor.shared.exception.AccountLockedException;
import br.com.clyvo.kura.tutor.web.domain.WebUsuarioSuporte;
import br.com.clyvo.kura.tutor.web.domain.WebUsuarioSuporteRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

/**
 * {@link AuthenticationProvider} próprio do painel Thymeleaf — NÃO é um
 * {@code UserDetailsService}, de propósito (bloqueador do §3 do brief da
 * SJ3-03): já existe {@code UserDetailsServiceImpl} em produto, injetado POR
 * TIPO em {@code JwtAuthenticationFilter}; um segundo bean do mesmo tipo
 * quebraria o boot com {@code NoUniqueBeanDefinitionException}.
 *
 * Registrado SÓ no {@code SecurityFilterChain} de {@code /web/**}
 * ({@link br.com.clyvo.kura.tutor.web.config.WebSecurityConfig}) — não é
 * {@code @Component}/{@code @Bean} de propósito: se fosse bean gerenciado
 * pelo Spring, a auto-detecção global de {@code AuthenticationProvider}
 * ({@code AuthenticationConfiguration.getAuthenticationManager()}, usada
 * pelo bean {@code authenticationManager} de {@code SecurityConfig} de
 * produto) o incluiria no {@code AuthenticationManager} GLOBAL, contaminando
 * um caminho de produto sem editar nenhum arquivo dele — exatamente o que
 * este isolamento existe para evitar. (Reforçado desde o fix do achado
 * {@code G2-1}: o {@code AuthenticationManager} do chain {@code /web/**}
 * também não tem parent — ver {@link br.com.clyvo.kura.tutor.web.config.WebSecurityConfig}.)
 *
 * <h2>Uma porta a mais, a mesma regra</h2>
 * A versão anterior desta classe reimplementava o login do tutor — ativa,
 * bloqueio, senha — e era declaradamente SÓ LEITURA: nunca chamava
 * {@code registrarLoginFalha()}. Consequência medida: <b>errar a senha no
 * formulário web, quantas vezes fosse, não travava a conta</b>; só as rotas
 * REST (que passam por {@link AuthService}) contavam tentativa. Duas portas
 * de autenticação, uma regra de negócio, e só uma das portas passava por
 * ela — além da regra duplicada em dois lugares.
 *
 * Hoje o tutor é autenticado <b>delegando</b> a {@link AuthService#login},
 * o MESMO serviço de domínio que a API usa. A camada web decide apenas QUEM
 * está tentando entrar (tutor do produto ou usuário de suporte desta branch)
 * e TRADUZ a exceção de domínio para a exceção equivalente do Spring
 * Security, que o formulário sabe renderizar. A regra de negócio continua no
 * domínio, e nenhum arquivo de produto foi editado.
 *
 * O par de tokens devolvido por {@code login} é descartado de propósito: o
 * painel é sessão + CSRF, não Bearer. O que interessa aqui é o efeito
 * correto sobre o estado da conta — contador de tentativas, bloqueio
 * temporário de {@code kura.auth.janela-bloqueio-minutos} (SJ3-02) e reset
 * no acerto.
 */
public class WebAuthenticationProvider implements AuthenticationProvider {

    private static final String MENSAGEM_CREDENCIAIS_INVALIDAS = "Login ou senha inválidos.";

    private final ContaTutorRepository contaTutorRepository;
    private final WebUsuarioSuporteRepository suporteRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;

    public WebAuthenticationProvider(ContaTutorRepository contaTutorRepository,
                                     WebUsuarioSuporteRepository suporteRepository,
                                     PasswordEncoder passwordEncoder,
                                     AuthService authService) {
        this.contaTutorRepository = contaTutorRepository;
        this.suporteRepository = suporteRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String login = authentication.getName();
        String senhaDigitada = String.valueOf(authentication.getCredentials());

        Optional<ContaTutor> conta = contaTutorRepository.findByDsEmailLogin(login);
        if (conta.isPresent()) {
            return autenticarTutor(conta.get(), senhaDigitada);
        }

        Optional<WebUsuarioSuporte> suporte = suporteRepository.findByDsLogin(login);
        if (suporte.isPresent()) {
            return autenticarSuporte(suporte.get(), senhaDigitada);
        }

        // Não distingue "login inexistente" de "senha errada" na mensagem —
        // mesma política anti-enumeração de AuthService (401 genérico da API).
        throw new BadCredentialsException(MENSAGEM_CREDENCIAIS_INVALIDAS);
    }

    /**
     * Delega ao serviço de domínio e traduz o resultado para o vocabulário do
     * Spring Security — a tradução é a ÚNICA responsabilidade desta camada:
     * o 423 do domínio vira {@link LockedException} (o formulário mostra
     * "conta bloqueada"), o 403 vira {@link DisabledException}, e o 401 já
     * chega como {@link BadCredentialsException}.
     */
    private Authentication autenticarTutor(ContaTutor conta, String senhaDigitada) {
        try {
            authService.login(new LoginRequest(conta.getDsEmailLogin(), senhaDigitada));
        } catch (AccountLockedException e) {
            throw new LockedException(e.getMessage(), e);
        } catch (AccountInactiveException e) {
            throw new DisabledException(e.getMessage(), e);
        } catch (BadCredentialsException e) {
            throw new BadCredentialsException(MENSAGEM_CREDENCIAIS_INVALIDAS, e);
        }

        return autenticado(conta.getDsEmailLogin(), conta.getDsSenhaHash(), "ROLE_TUTOR");
    }

    private Authentication autenticarSuporte(WebUsuarioSuporte usuario, String senhaDigitada) {
        if (!usuario.isAtiva()) {
            throw new DisabledException("Conta desativada.");
        }

        if (!passwordEncoder.matches(senhaDigitada, usuario.getDsSenhaHash())) {
            throw new BadCredentialsException(MENSAGEM_CREDENCIAIS_INVALIDAS);
        }

        return autenticado(usuario.getDsLogin(), usuario.getDsSenhaHash(), "ROLE_SUPORTE");
    }

    private Authentication autenticado(String username, String senhaHash, String papel) {
        UserDetails principal = User.withUsername(username)
                .password(senhaHash)
                .authorities(papel)
                .build();

        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
