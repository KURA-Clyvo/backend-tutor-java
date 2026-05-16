package br.com.clyvo.kura.tutor.controller;

import br.com.clyvo.kura.tutor.dto.request.LoginRequest;
import br.com.clyvo.kura.tutor.dto.request.RegistroContaRequest;
import br.com.clyvo.kura.tutor.dto.response.TokenResponse;
import br.com.clyvo.kura.tutor.service.impl.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de autenticação — públicos (sem JWT).
 *
 * <p>Primeiro endpoint a testar no Swagger:
 * <ol>
 *   <li>{@code POST /auth/login} → retorna accessToken
 *   <li>Copie o token e clique em "Authorize" no Swagger → Bearer <token>
 *   <li>Agora os demais endpoints funcionam
 * </ol>
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "1. Autenticação", description = "Login e registro de conta do tutor")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(
            summary = "Login do tutor",
            description = "Autentica o tutor com e-mail e senha. Retorna o JWT para uso nos demais endpoints."
    )
    @ApiResponse(responseCode = "200", description = "Login bem-sucedido — retorna o token JWT")
    @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    @ApiResponse(responseCode = "422", description = "Conta bloqueada por excesso de tentativas")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/registro")
    @Operation(
            summary = "Criar conta no portal",
            description = """
                    Cria a CONTA_TUTOR para um tutor já cadastrado pela clínica (.NET).
                    O tutor deve existir na tabela TUTOR antes de usar este endpoint.
                    Após o registro, já retorna o JWT — sem precisar fazer login separado.
                    """
    )
    @ApiResponse(responseCode = "200", description = "Conta criada — já retorna o token JWT")
    @ApiResponse(responseCode = "404", description = "Tutor não encontrado")
    @ApiResponse(responseCode = "422", description = "E-mail já em uso ou tutor inativo")
    public ResponseEntity<TokenResponse> registrar(
            @Valid @RequestBody RegistroContaRequest request) {
        return ResponseEntity.ok(authService.registrar(request));
    }
}
