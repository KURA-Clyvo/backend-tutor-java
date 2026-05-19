package br.com.clyvo.kura.tutor.auth.security;

import br.com.clyvo.kura.tutor.shared.exception.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Retorna 401 Unauthorized com corpo ApiError JSON quando um request não autenticado
 * tenta acessar um recurso protegido.
 *
 * Sem este componente, Spring Security retorna 403 Forbidden por padrão — comportamento
 * incorreto para recursos que exigem autenticação (403 é para recursos onde o usuário
 * está autenticado mas não tem permissão).
 *
 * Distingue dois cenários via atributo de request gravado pelo JwtAuthenticationFilter:
 *   TOKEN_INVALIDO — header Authorization presente mas token inválido/expirado
 *   TOKEN_AUSENTE  — header Authorization ausente
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    static final String JWT_ERROR_ATTR = "jwt_error";
    static final String TOKEN_INVALIDO = "TOKEN_INVALIDO";
    static final String TOKEN_AUSENTE  = "TOKEN_AUSENTE";

    private final ObjectMapper objectMapper;

    public JwtAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {

        String codigo = TOKEN_INVALIDO.equals(request.getAttribute(JWT_ERROR_ATTR))
                ? TOKEN_INVALIDO : TOKEN_AUSENTE;

        String mensagem = TOKEN_INVALIDO.equals(codigo)
                ? "Token JWT inválido ou expirado."
                : "Token JWT não fornecido. Inclua o header Authorization: Bearer <token>.";

        ApiError apiError = ApiError.ofAuth(mensagem, request.getRequestURI(), codigo);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getOutputStream(), apiError);
    }
}
