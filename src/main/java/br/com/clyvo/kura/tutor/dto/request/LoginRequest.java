package br.com.clyvo.kura.tutor.dto.request;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
@Schema(description = "Credenciais de login")
public record LoginRequest(
    @Schema(example = "tutor@clyvo.vet") @NotBlank @Email String email,
    @Schema(example = "Senha@123")       @NotBlank         String senha
) {}
