package br.com.clyvo.kura.tutor.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para criação de conta do tutor no portal.
 * O tutor já deve existir em TUTOR (cadastrado pela clínica via .NET).
 * Este endpoint cria apenas a CONTA_TUTOR (credenciais de acesso).
 */
@Schema(description = "Dados para criação de conta no portal do tutor")
public record RegistroContaRequest(

        @Schema(description = "ID do tutor já cadastrado pela clínica", example = "1")
        @NotNull(message = "O ID do tutor é obrigatório.")
        Long idTutor,

        @Schema(description = "E-mail para login (pode ser o mesmo do cadastro na clínica)", example = "tutor@email.com")
        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Formato de e-mail inválido.")
        String email,

        @Schema(description = "Senha de acesso — mínimo 8 caracteres, ao menos 1 número e 1 especial", example = "Senha@123")
        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 8, message = "A senha deve ter pelo menos 8 caracteres.")
        String senha
) {}
