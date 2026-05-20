package br.com.clyvo.kura.tutor.tutor.api.dto;

import br.com.clyvo.kura.tutor.entity.Pet;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Dados resumidos de um pet do tutor")
public record PetResponse(
        @Schema(description = "ID do pet", example = "1") Long idPet,
        @Schema(description = "Nome do pet", example = "Marley") String nmPet,
        @Schema(description = "Espécie", example = "Cão") String nmEspecie,
        @Schema(description = "Raça — 'SRD' se sem raça definida", example = "Labrador") String nmRaca,
        @Schema(description = "Sexo: M=macho, F=fêmea", example = "M",
                allowableValues = {"M", "F"}) String sgSexo,
        @Schema(description = "Data de nascimento", example = "2020-03-15") LocalDate dtNascimento,
        @Schema(description = "Porte: P=pequeno, M=médio, G=grande", example = "M",
                allowableValues = {"P", "M", "G"}) String sgPorte
) {
    public static PetResponse fromEntity(Pet p) {
        return new PetResponse(
                p.getIdPet(),
                p.getNmPet(),
                p.getEspecie() != null ? p.getEspecie().getNmEspecie() : null,
                p.getRaca()    != null ? p.getRaca().getNmRaca()       : "SRD",
                p.getSgSexo(),
                p.getDtNascimento(),
                p.getSgPorte()
        );
    }
}
