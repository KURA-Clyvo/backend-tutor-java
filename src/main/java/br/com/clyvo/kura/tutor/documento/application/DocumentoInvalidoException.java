package br.com.clyvo.kura.tutor.documento.application;

/** 400: arquivo vazio ou número fora do padrão do contrato. A mensagem nunca ecoa o valor enviado. */
public class DocumentoInvalidoException extends RuntimeException {
    public DocumentoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
