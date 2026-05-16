package br.com.clyvo.kura.tutor.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para autenticação do tutor.
 * Usado em {@code POST /auth/login}.
 */
@Schema(description = "Credenciais de login do tutor")
public record LoginRequest(

        @Schema(description = "E-mail cadastrado na conta", example = "tutor@email.com")
        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Formato de e-mail inválido.")
        String email,

        @Schema(description = "Senha da conta (mínimo 6 caracteres)", example = "Senha@123")
        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres.")
        String senha
) {}
