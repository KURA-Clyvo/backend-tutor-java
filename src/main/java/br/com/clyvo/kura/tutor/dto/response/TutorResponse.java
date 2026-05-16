package br.com.clyvo.kura.tutor.dto.response;

import br.com.clyvo.kura.tutor.entity.Tutor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO de saída com dados do tutor.
 * Nunca expõe a entidade JPA diretamente — isola o contrato da API do schema do banco.
 */
@Schema(description = "Dados do tutor")
public record TutorResponse(

        @Schema(example = "1") Long idTutor,
        @Schema(example = "Felipe Ferrete") String nmTutor,
        @Schema(example = "123.456.789-00") String nrCpf,
        @Schema(example = "tutor@email.com") String dsEmail,
        @Schema(example = "(11) 99999-0001") String dsTelefone,
        @Schema(example = "(11) 99999-0001") String dsWhatsapp,
        @Schema(example = "1990-05-15") LocalDate dtNascimento,
        @Schema(example = "São Paulo") String nmCidade,
        @Schema(example = "SP") String sgUf,
        @Schema(example = "S") String stAtivoDescricao,
        @Schema(example = "true") boolean possuiConta,
        LocalDateTime dtCadastro
) {
    /**
     * Converte entidade JPA para DTO — padrão DTO Pattern.
     * O boolean {@code possuiConta} indica se o tutor já tem CONTA_TUTOR criada.
     */
    public static TutorResponse from(Tutor tutor, boolean possuiConta) {
        return new TutorResponse(
                tutor.getIdTutor(),
                tutor.getNmTutor(),
                tutor.getNrCpf(),
                tutor.getDsEmail(),
                tutor.getDsTelefone(),
                tutor.getDsWhatsapp(),
                tutor.getDtNascimento(),
                tutor.getNmCidade(),
                tutor.getSgUf(),
                "S".equals(tutor.getStAtivo()) ? "Ativo" : "Inativo",
                possuiConta,
                tutor.getDtCadastro()
        );
    }
}
