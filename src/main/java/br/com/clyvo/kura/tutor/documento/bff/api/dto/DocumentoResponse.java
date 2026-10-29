package br.com.clyvo.kura.tutor.documento.bff.api.dto;

import br.com.clyvo.kura.tutor.documento.application.DocumentoTutor;

import java.time.Instant;

/**
 * Contrato documentos_tutor v1 (G0 §5). {@code urlArquivo} é o caminho relativo da rota
 * {@code /arquivo}; o app baixa com o próprio header Authorization. Sem nome do arquivo original.
 */
public record DocumentoResponse(
        String id,
        long idPet,
        String tipo,
        String emissor,
        String numero,
        String contentType,
        long tamanhoBytes,
        Instant criadoEm,
        String urlArquivo) {

    public static DocumentoResponse de(DocumentoTutor d, String contextPath) {
        return new DocumentoResponse(
                d.id(),
                d.idPet(),
                d.tipo().name(),
                d.emissor() == null ? null : d.emissor().name(),
                d.numero(),
                d.contentType(),
                d.tamanhoBytes(),
                d.criadoEm(),
                caminhoArquivo(contextPath, d.idPet(), d.id()));
    }

    public static String caminhoArquivo(String contextPath, long idPet, String idDocumento) {
        return contextPath + "/v1/tutor/pets/" + idPet + "/documentos/" + idDocumento + "/arquivo";
    }
}
