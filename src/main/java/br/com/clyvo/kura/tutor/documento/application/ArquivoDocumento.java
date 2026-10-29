package br.com.clyvo.kura.tutor.documento.application;

/** Bytes de um documento com o Content-Type gravado (detectado pela assinatura no upload). */
public record ArquivoDocumento(String contentType, byte[] conteudo) {
}
