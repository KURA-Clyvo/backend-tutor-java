package br.com.clyvo.kura.tutor.tutor.api.dto;

import br.com.clyvo.kura.tutor.entity.Pet;
import br.com.clyvo.kura.tutor.shared.foto.ChaveFotoPet;
import br.com.clyvo.kura.tutor.shared.foto.GeradorUrlFotoPet;
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
                allowableValues = {"P", "M", "G"}) String sgPorte,
        // FT-05 (KURA_BACKLOG_FOTO_PET), regra A5 do backlog: DTO de LISTA nunca baixa a
        // variante grande — só a thumb (256px). Null quando o pet não tem foto OU quando a
        // assinatura de URL está desabilitada (GeradorUrlFotoPet.gerarUrl trata os 2 casos
        // igual: o app mostra a ilustração padrão).
        @Schema(description = "URL assinada da foto em miniatura (256px) — null sem foto",
                example = "https://kura-clinica.example/api/v1/fotos/clinica/1/pet/2/abc_256.webp?exp=...&sig=...")
                String dsFotoThumbUrl
) {
    public static PetResponse fromEntity(Pet p, GeradorUrlFotoPet geradorUrlFotoPet) {
        return new PetResponse(
                p.getIdPet(),
                p.getNmPet(),
                p.getEspecie() != null ? p.getEspecie().getNmEspecie() : null,
                p.getRaca()    != null ? p.getRaca().getNmRaca()       : "SRD",
                p.getSgSexo(),
                p.getDtNascimento(),
                p.getSgPorte(),
                geradorUrlFotoPet.gerarUrl(p.getDsFotoChave(), ChaveFotoPet.SUFIXO_THUMB)
        );
    }
}
