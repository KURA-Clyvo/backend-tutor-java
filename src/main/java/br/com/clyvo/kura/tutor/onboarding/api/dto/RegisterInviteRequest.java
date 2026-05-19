package br.com.clyvo.kura.tutor.onboarding.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

@Schema(description = "Cria conta do tutor a partir de convite válido enviado pela clínica")
public record RegisterInviteRequest(

        @NotBlank(message = "O token do convite é obrigatório")
        @Schema(description = "Token UUID do convite", example = "550e8400-e29b-41d4-a716-446655440000")
        String token,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
        @Pattern(
                regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).+$",
                message = "A senha deve conter ao menos uma letra maiúscula, uma minúscula e um número"
        )
        @Schema(description = "Mínimo 8 chars, 1 maiúscula, 1 minúscula, 1 número")
        String senha,

        @NotNull(message = "A lista de aceites não pode ser nula")
        @Valid
        @Schema(description = "Aceites/revogações LGPD — pode ser lista vazia")
        List<AceiteRequest> aceites
) {}
