package br.com.clyvo.kura.tutor.bff.api;

import br.com.clyvo.kura.tutor.auth.api.dto.LoginRequest;
import br.com.clyvo.kura.tutor.auth.api.dto.RefreshRequest;
import br.com.clyvo.kura.tutor.auth.api.dto.TokenResponse;
import br.com.clyvo.kura.tutor.auth.application.AuthService;
import br.com.clyvo.kura.tutor.onboarding.api.dto.RegisterInviteRequest;
import br.com.clyvo.kura.tutor.onboarding.application.OnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * BFF alias de auth para o mobile-tutor-rn.
 * Expõe /api/v1/auth/** delegando aos serviços de auth/onboarding existentes.
 * Os endpoints /api/auth/** legados permanecem inalterados.
 */
@RestController
@RequestMapping("/v1/auth")
@Tag(name = "Auth BFF", description = "Alias /api/v1/auth/** para mobile-tutor-rn (login/refresh delegam a /api/auth/**; register-invite delega a /api/onboarding/register-invite, TASK-82)")
public class AuthBffController {

    private final AuthService authService;
    private final OnboardingService onboardingService;

    public AuthBffController(AuthService authService, OnboardingService onboardingService) {
        this.authService = authService;
        this.onboardingService = onboardingService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login do tutor (alias BFF)", description = "Delega a /api/auth/login.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login bem-sucedido"),
        @ApiResponse(responseCode = "401", description = "Credenciais inválidas"),
        @ApiResponse(responseCode = "423", description = "Conta bloqueada")
    })
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renovar tokens (alias BFF)", description = "Delega a /api/auth/refresh.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Novo par de tokens emitido"),
        @ApiResponse(responseCode = "401", description = "Refresh token inválido ou expirado")
    })
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/register-invite")
    @Operation(summary = "Criar conta por convite (alias BFF)", description = "Delega a /api/onboarding/register-invite.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Conta criada"),
        @ApiResponse(responseCode = "404", description = "Convite não encontrado"),
        @ApiResponse(responseCode = "409", description = "Convite já utilizado"),
        @ApiResponse(responseCode = "410", description = "Convite expirado")
    })
    public ResponseEntity<br.com.clyvo.kura.tutor.onboarding.api.dto.TokenResponse> registerInvite(
            @Valid @RequestBody RegisterInviteRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(onboardingService.registrarPorInvite(request, httpRequest));
    }
}
