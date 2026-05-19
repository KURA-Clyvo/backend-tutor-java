package br.com.clyvo.kura.tutor.dto.response;

import br.com.clyvo.kura.tutor.entity.Pet;
import java.time.LocalDate;

public record PetResponse(
    Long idPet, String nmPet, String nmEspecie, String nmRaca,
    String sgSexo, LocalDate dtNascimento, String sgPorte
) {
    public static PetResponse fromEntity(Pet p) {
        return new PetResponse(
            p.getIdPet(), p.getNmPet(),
            p.getEspecie() != null ? p.getEspecie().getNmEspecie() : null,
            p.getRaca() != null ? p.getRaca().getNmRaca() : "SRD",
            p.getSgSexo(), p.getDtNascimento(), p.getSgPorte());
    }
}
