package br.com.clyvo.kura.tutor.laudo.application;

import java.time.Instant;

/**
 * Metadados de um laudo (contrato {@code laudos} v1, dono .NET) que o tutor pode ver. Só os campos
 * de exibição + a chave do arquivo (usada apenas para assinar a URL, nunca exposta). Nota, dados
 * clínicos, autoria e clínica ficam de fora de propósito.
 */
public record LaudoTutor(String id, String tipo, String titulo, Instant dataExame, String laboratorio,
                         String contentType, long tamanhoBytes, String chaveArquivo) {
}
