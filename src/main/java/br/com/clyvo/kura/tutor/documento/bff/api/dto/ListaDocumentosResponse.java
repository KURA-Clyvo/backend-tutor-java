package br.com.clyvo.kura.tutor.documento.bff.api.dto;

import java.util.List;

/** Envelope da lista de documentos do pet (no máximo 20, criadoEm decrescente). */
public record ListaDocumentosResponse(List<DocumentoResponse> itens) {
}
