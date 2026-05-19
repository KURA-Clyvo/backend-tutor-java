package br.com.clyvo.kura.tutor.onboarding.api;

import br.com.clyvo.kura.tutor.onboarding.api.dto.RegisterInviteRequest;
import br.com.clyvo.kura.tutor.onboarding.api.dto.TokenResponse;
import br.com.clyvo.kura.tutor.onboarding.application.OnboardingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Onboarding por convite — cria conta e retorna JWT")
public class OnboardingController {

    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @PostMapping("/register-invite")
    @Operation(
            summary = "Cria conta do tutor por convite",
            description = """
                    Valida o token de convite enviado pela clínica (.NET) e, em uma única transação:
                    - cria a conta (CONTA_TUTOR),
                    - persiste os aceites LGPD (CONSENTIMENTO),
                    - gera e retorna access token (15 min) + refresh token (7 dias).
                    HTTP 409 se o convite já foi utilizado (inclui race condition via UK).
                    HTTP 410 se o convite estiver expirado.
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Conta criada e tokens retornados"),
            @ApiResponse(responseCode = "400", description = "Payload inválido (senha fraca, token em branco)"),
            @ApiResponse(responseCode = "404", description = "Token de convite não encontrado"),
            @ApiResponse(responseCode = "409", description = "Convite já utilizado ou cancelado"),
            @ApiResponse(responseCode = "410", description = "Convite expirado"),
            @ApiResponse(responseCode = "422", description = "Tutor inativo ou sem aviso de privacidade")
    })
    public ResponseEntity<TokenResponse> registerInvite(
            @Valid @RequestBody RegisterInviteRequest request,
            HttpServletRequest httpRequest) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(onboardingService.registrarPorInvite(request, httpRequest));
    }
}
