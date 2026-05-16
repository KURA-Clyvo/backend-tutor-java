package br.com.clyvo.kura.tutor.dto.response;

import br.com.clyvo.kura.tutor.entity.Pet;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;

@Schema(description = "Dados do pet")
public record PetResponse(
        @Schema(example = "1") Long idPet,
        @Schema(example = "Rex") String nmPet,
        @Schema(example = "Cão") String nmEspecie,
        @Schema(example = "Labrador Retriever") String nmRaca,
        @Schema(example = "M") String sgSexo,
        @Schema(example = "G") String sgPorte,
        @Schema(example = "S") String stCastrado,
        @Schema(example = "32.5") BigDecimal nrPesoKg,
        @Schema(example = "3 anos") String idadeFormatada,
        @Schema(example = "Alergia a frango") String dsAlergias,
        @Schema(example = "S") String stAtivo,
        LocalDate dtNascimento
) {
    public static PetResponse from(Pet pet) {
        String especie = pet.getEspecie() != null ? pet.getEspecie().getNmEspecie() : null;
        String raca = pet.getRaca() != null ? pet.getRaca().getNmRaca() : "SRD";

        String idade = null;
        if (pet.getDtNascimento() != null) {
            int anos = Period.between(pet.getDtNascimento(), LocalDate.now()).getYears();
            idade = anos == 0 ? "menos de 1 ano" : anos + (anos == 1 ? " ano" : " anos");
        }

        return new PetResponse(
                pet.getIdPet(), pet.getNmPet(), especie, raca,
                pet.getSgSexo(), pet.getSgPorte(), pet.getStCastrado(),
                pet.getNrPesoKg(), idade, pet.getDsAlergias(),
                pet.getStAtivo(), pet.getDtNascimento()
        );
    }
}
