package br.com.clyvo.kura.tutor.tutor.api.dto;

import br.com.clyvo.kura.tutor.entity.Pet;
import br.com.clyvo.kura.tutor.shared.foto.ChaveFotoPet;
import br.com.clyvo.kura.tutor.shared.foto.GeradorUrlFotoPet;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Detalhe de um pet do tutor autenticado")
public record PetDetalheResponse(
        @Schema(description = "ID do pet", example = "1") Long idPet,
        @Schema(description = "Nome do pet", example = "Marley") String nmPet,
        @Schema(description = "Espécie", example = "Cão") String nmEspecie,
        @Schema(description = "Raça — 'SRD' se sem raça definida", example = "Labrador") String nmRaca,
        @Schema(description = "Sexo: M=macho, F=fêmea", example = "M",
                allowableValues = {"M", "F"}) String sgSexo,
        @Schema(description = "Data de nascimento", example = "2020-03-15") LocalDate dtNascimento,
        @Schema(description = "Porte: P=pequeno, M=médio, G=grande", example = "M",
                allowableValues = {"P", "M", "G"}) String sgPorte,
        @Schema(description = "Clínica responsável", example = "Clyvo Vet São Paulo") String nmClinica,
        @Schema(description = "Veterinário responsável — null se não atribuído", example = "Dra. Ana Souza")
                String nmVeterinarioResponsavel,
        @Schema(description = "Quantidade de eventos na timeline (VW_TIMELINE_PET)", example = "5")
                long nrConsultas,
        // FT-05 (KURA_BACKLOG_FOTO_PET): DTO de DETALHE ganha as 2 variantes (regra A5 do
        // backlog — só a lista fica restrita à thumb). Null nos 2 quando o pet não tem foto
        // OU quando a assinatura de URL está desabilitada.
        @Schema(description = "URL assinada da foto em tamanho de detalhe (1080px) — null sem foto",
                example = "https://kura-clinica.example/api/v1/fotos/clinica/1/pet/2/abc_1080.webp?exp=...&sig=...")
                String dsFotoUrl,
        @Schema(description = "URL assinada da foto em miniatura (256px) — null sem foto",
                example = "https://kura-clinica.example/api/v1/fotos/clinica/1/pet/2/abc_256.webp?exp=...&sig=...")
                String dsFotoThumbUrl
) {
    public static PetDetalheResponse fromEntity(Pet p, long nrConsultas, GeradorUrlFotoPet geradorUrlFotoPet) {
        return new PetDetalheResponse(
                p.getIdPet(),
                p.getNmPet(),
                p.getEspecie() != null ? p.getEspecie().getNmEspecie() : null,
                p.getRaca()    != null ? p.getRaca().getNmRaca()       : "SRD",
                p.getSgSexo(),
                p.getDtNascimento(),
                p.getSgPorte(),
                p.getClinica() != null ? p.getClinica().getNmClinica() : null,
                p.getVeterinarioResponsavel() != null ? p.getVeterinarioResponsavel().getNmVeterinario() : null,
                nrConsultas,
                geradorUrlFotoPet.gerarUrl(p.getDsFotoChave(), ChaveFotoPet.SUFIXO_MEDIA),
                geradorUrlFotoPet.gerarUrl(p.getDsFotoChave(), ChaveFotoPet.SUFIXO_THUMB)
        );
    }
}
