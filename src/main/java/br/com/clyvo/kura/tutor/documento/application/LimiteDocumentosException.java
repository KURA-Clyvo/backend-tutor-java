package br.com.clyvo.kura.tutor.documento.application;

/** 409: teto de documentos por pet atingido. */
public class LimiteDocumentosException extends RuntimeException {
    public LimiteDocumentosException(int teto) {
        super("Limite de " + teto + " documentos por pet atingido. Apague um para enviar outro.");
    }
}
