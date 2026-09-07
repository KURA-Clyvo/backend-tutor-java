// KURA-WEB — camada de visualização servidor (Sprint 3, Java Advanced).
// Escopo isolado: depende do domínio; o domínio não depende dela.
// Ver docs/ADR-web.md.
package br.com.clyvo.kura.tutor.web.security;

import br.com.clyvo.kura.tutor.auth.domain.repository.ContaTutorRepository;
import br.com.clyvo.kura.tutor.entity.ContaTutor;
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

import java.time.LocalDateTime;
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
 * um caminho de produto sem editar nenhum arquivo dele — exatamente o que a
 * branch descartável existe para evitar.
 *
 * SÓ LEITURA (§3.1 do brief): não chama nenhum método que persista estado em
 * {@code ContaTutor} (nem {@code registrarLoginFalha}, nem
 * {@code limparBloqueioExpirado}, nem {@code save}). A checagem de bloqueio
 * usa a sobrecarga COM JANELA — {@link ContaTutor#isBloqueada(LocalDateTime, int)}
 * — que já ignora um bloqueio expirado sem precisar limpá-lo antes; é a MESMA
 * proteção que {@code AuthService} aplica no login da API, reaproveitando a
 * mesma propriedade {@code kura.auth.janela-bloqueio-minutos} (SJ3-02).
 */
public class WebAuthenticationProvider implements AuthenticationProvider {

    private static final String MENSAGEM_CREDENCIAIS_INVALIDAS = "Login ou senha inválidos.";

    private final ContaTutorRepository contaTutorRepository;
    private final WebUsuarioSuporteRepository suporteRepository;
    private final PasswordEncoder passwordEncoder;
    private final int janelaBloqueioMinutos;

    public WebAuthenticationProvider(ContaTutorRepository contaTutorRepository,
                                     WebUsuarioSuporteRepository suporteRepository,
                                     PasswordEncoder passwordEncoder,
                                     int janelaBloqueioMinutos) {
        this.contaTutorRepository = contaTutorRepository;
        this.suporteRepository = suporteRepository;
        this.passwordEncoder = passwordEncoder;
        this.janelaBloqueioMinutos = janelaBloqueioMinutos;
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

    private Authentication autenticarTutor(ContaTutor conta, String senhaDigitada) {
        if (!conta.isAtiva()) {
            throw new DisabledException("Conta desativada.");
        }

        LocalDateTime agora = LocalDateTime.now();
        if (conta.isBloqueada(agora, janelaBloqueioMinutos)) {
            throw new LockedException("Conta temporariamente bloqueada.");
        }

        if (!passwordEncoder.matches(senhaDigitada, conta.getDsSenhaHash())) {
            throw new BadCredentialsException(MENSAGEM_CREDENCIAIS_INVALIDAS);
        }

        UserDetails principal = User.withUsername(conta.getDsEmailLogin())
                .password(conta.getDsSenhaHash())
                .authorities("ROLE_TUTOR")
                .build();

        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private Authentication autenticarSuporte(WebUsuarioSuporte usuario, String senhaDigitada) {
        if (!usuario.isAtiva()) {
            throw new DisabledException("Conta desativada.");
        }

        if (!passwordEncoder.matches(senhaDigitada, usuario.getDsSenhaHash())) {
            throw new BadCredentialsException(MENSAGEM_CREDENCIAIS_INVALIDAS);
        }

        UserDetails principal = User.withUsername(usuario.getDsLogin())
                .password(usuario.getDsSenhaHash())
                .authorities("ROLE_SUPORTE")
                .build();

        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
