package br.com.clyvo.kura.tutor.laudo.bff.api.dto;

import java.util.List;

/** Envelope da lista de laudos do pet (dataExame decrescente). */
public record ListaLaudosResponse(List<LaudoTutorResponse> itens) {
}
