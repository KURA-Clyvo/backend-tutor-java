package br.com.clyvo.kura.tutor.auth.api;

import br.com.clyvo.kura.tutor.auth.api.dto.LoginRequest;
import br.com.clyvo.kura.tutor.auth.api.dto.TokenResponse;
import br.com.clyvo.kura.tutor.auth.application.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Login de conta do tutor")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(
            summary = "Login do tutor",
            description = """
                    Autentica com e-mail e senha. Retorna access token (15 min) e refresh token (7 dias).
                    Após 5 tentativas erradas consecutivas, a conta é bloqueada (423).
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login bem-sucedido — tokens retornados"),
            @ApiResponse(responseCode = "400", description = "Payload inválido"),
            @ApiResponse(responseCode = "401", description = "E-mail ou senha inválidos"),
            @ApiResponse(responseCode = "403", description = "Conta desativada"),
            @ApiResponse(responseCode = "423", description = "Conta bloqueada — excesso de tentativas")
    })
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
