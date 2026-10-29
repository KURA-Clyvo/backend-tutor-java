package br.com.clyvo.kura.tutor.documento.application;

import java.time.Instant;

/** Metadados de um documento do pet; nunca carrega os bytes do arquivo. */
public record DocumentoTutor(
        String id,
        long idPet,
        TipoDocumento tipo,
        EmissorDocumento emissor,
        String numero,
        String contentType,
        long tamanhoBytes,
        Instant criadoEm) {
}
