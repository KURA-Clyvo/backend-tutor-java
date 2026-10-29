package br.com.clyvo.kura.tutor.documento.application;

import java.time.Instant;

/** Documento já validado, pronto para gravar. sha256 e contentType são calculados no servidor. */
public record NovoDocumento(
        long idPet,
        TipoDocumento tipo,
        EmissorDocumento emissor,
        String numero,
        byte[] conteudo,
        String contentType,
        String sha256,
        Instant criadoEm) {
}
