package br.com.clyvo.kura.tutor.documento.application;

/** 413: arquivo acima do teto de 5 MiB. */
public class ArquivoGrandeException extends RuntimeException {
    public ArquivoGrandeException() {
        super("Arquivo acima do limite de 5 MB.");
    }
}
