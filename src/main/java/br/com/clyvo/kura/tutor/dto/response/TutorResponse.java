package br.com.clyvo.kura.tutor.dto.response;

import br.com.clyvo.kura.tutor.entity.Tutor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Dados do tutor")
public record TutorResponse(
    @Schema(description = "ID do tutor", example = "1") Long idTutor,
    @Schema(description = "Nome completo", example = "Felipe Ferretel") String nmTutor,
    @Schema(description = "E-mail cadastrado", example = "tutor@clyvo.vet") String dsEmail,
    @Schema(description = "Telefone", example = "11999999999") String nrTelefone,
    @Schema(description = "WhatsApp (opcional)", example = "11999999999") String dsWhatsapp,
    @Schema(description = "Data de nascimento", example = "1990-05-20") LocalDate dtNascimento,
    @Schema(description = "Cidade") String nmCidade,
    @Schema(description = "UF", example = "SP") String sgUf,
    @Schema(description = "true = aviso de privacidade aceito") boolean avisoPrivacidade
) {
    public static TutorResponse fromEntity(Tutor t) {
        return new TutorResponse(t.getIdTutor(), t.getNmTutor(), t.getDsEmail(),
            t.getNrTelefone(), t.getDsWhatsapp(), t.getDtNascimento(),
            t.getNmCidade(), t.getSgUf(), t.temAvisoPrivacidade());
    }
}
