package br.com.clyvo.kura.tutor.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro JWT — estrutura identica ao JWTAuthFilter do projeto de aula.
 *
 * CORRECAO EM RELACAO A V1:
 * - v1 usava @RequiredArgsConstructor (Lombok) → campos final nao inicializados no Eclipse
 * - v2 usa construtor explicito igual ao padrao do projeto de aula (@Autowired implicito)
 *
 * CORRECAO DE SEGURANCA EM RELACAO AO PROJETO AULA:
 * - Aula: chama extrairUsername() antes de validarToken() → excecao se token malformado
 * - KURA: valida o token PRIMEIRO, so depois extrai o username
 */
@Component
public class JWTAuthFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final UserDetailsService detailsService;

    // Construtor explicito — compativel com Eclipse sem plugin Lombok
    public JWTAuthFilter(JWTUtil jwtUtil, UserDetailsService detailsService) {
        this.jwtUtil = jwtUtil;
        this.detailsService = detailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {

            String token = header.substring(7);

            // CORRECAO: valida antes de extrair (evita excecao em tokens malformados)
            if (jwtUtil.validarToken(token)) {

                String username = jwtUtil.extrairUsername(token);

                if (username != null &&
                    SecurityContextHolder.getContext().getAuthentication() == null) {

                    UserDetails usuario = detailsService.loadUserByUsername(username);

                    var autenticacao = new UsernamePasswordAuthenticationToken(
                            usuario, null, usuario.getAuthorities());
                    autenticacao.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(autenticacao);
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
