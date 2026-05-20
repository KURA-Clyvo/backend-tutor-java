package br.com.clyvo.kura.tutor.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciais de login do tutor")
public record LoginRequest(
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Informe um e-mail válido")
        @Schema(description = "E-mail de acesso cadastrado no convite", example = "tutor@clyvo.vet")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Schema(description = "Senha definida no cadastro por convite", example = "Senha@123")
        String senha
) {}
