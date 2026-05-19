package br.com.clyvo.kura.tutor.controller;

import br.com.clyvo.kura.tutor.dto.request.LoginRequest;
import br.com.clyvo.kura.tutor.dto.request.RegistroContaRequest;
import br.com.clyvo.kura.tutor.dto.response.TokenResponse;
import br.com.clyvo.kura.tutor.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Controller de autenticacao — rotas publicas (sem JWT).
 *
 * Equivalente ao AutenticacaoController do projeto de aula.
 * Diferenca: a aula usava @RequestParam para usuario/senha.
 * KURA usa @RequestBody com record LoginRequest — mais seguro
 * pois nao expoe credenciais na URL/logs do servidor.
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticacao", description = "Login e registro de conta do tutor")
public class AutenticacaoController {

    private final AuthService authService;

    public AutenticacaoController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login do tutor",
               description = "Autentica com email e senha. Retorna JWT Bearer para usar nas demais rotas.")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/registro")
    @Operation(summary = "Cria conta no portal",
               description = "O tutor deve estar cadastrado no sistema pelo .NET antes de criar conta aqui.")
    public ResponseEntity<TokenResponse> registro(@Valid @RequestBody RegistroContaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }
}
