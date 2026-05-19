package br.com.clyvo.kura.tutor.dto.request;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
@Schema(description = "Criacao de conta — tutor deve existir no sistema (.NET)")
public record RegistroContaRequest(
    @Schema(description = "ID do tutor cadastrado pela clinica", example = "1") @NotNull Long idTutor,
    @Schema(example = "tutor@clyvo.vet") @NotBlank @Email String email,
    @Schema(example = "Senha@123") @NotBlank @Size(min = 8, message = "Senha: minimo 8 caracteres") String senha
) {}
