package br.com.clyvo.kura.tutor.documento.application;

/** 415: a assinatura do arquivo não é JPEG, PNG, WEBP nem PDF. */
public class TipoArquivoNaoSuportadoException extends RuntimeException {
    public TipoArquivoNaoSuportadoException() {
        super("Formato não suportado. Envie JPEG, PNG, WEBP ou PDF.");
    }
}
