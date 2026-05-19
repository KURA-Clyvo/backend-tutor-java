package br.com.clyvo.kura.tutor.auth.security;

import br.com.clyvo.kura.tutor.auth.application.JwtTokenProvider;
import io.jsonwebtoken.Claims;
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
import java.util.Optional;

/**
 * Filtro JWT stateless — executa uma vez por request ({@link OncePerRequestFilter}).
 *
 * Fluxo:
 *   1. Extrai "Bearer <token>" do header Authorization.
 *   2. Valida o token via JwtTokenProvider.validar() → Optional<Claims>.
 *   3. Se válido: carrega ContaTutor por idConta e popula o SecurityContext.
 *   4. Se inválido (token presente mas rejeitado): grava atributo "jwt_error" = TOKEN_INVALIDO
 *      para que JwtAuthenticationEntryPoint distinga do caso de token ausente.
 *
 * Token NUNCA é logado — evitar exposição de credenciais em logs.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider,
                                   UserDetailsService userDetailsService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            Optional<Claims> claimsOpt = jwtTokenProvider.validar(token);

            if (claimsOpt.isPresent()) {
                String idContaStr = claimsOpt.get().getSubject();

                if (idContaStr != null
                        && SecurityContextHolder.getContext().getAuthentication() == null) {
                    try {
                        UserDetails usuario = userDetailsService.loadUserByUsername(idContaStr);
                        var autenticacao = new UsernamePasswordAuthenticationToken(
                                usuario, null, usuario.getAuthorities());
                        autenticacao.setDetails(
                                new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(autenticacao);
                    } catch (Exception e) {
                        // Conta não encontrada para o ID do token — tratar como inválido
                        request.setAttribute(JwtAuthenticationEntryPoint.JWT_ERROR_ATTR,
                                JwtAuthenticationEntryPoint.TOKEN_INVALIDO);
                    }
                }
            } else {
                // Token presente mas inválido/expirado — sinaliza para EntryPoint
                request.setAttribute(JwtAuthenticationEntryPoint.JWT_ERROR_ATTR,
                        JwtAuthenticationEntryPoint.TOKEN_INVALIDO);
            }
        }

        filterChain.doFilter(request, response);
    }
}
