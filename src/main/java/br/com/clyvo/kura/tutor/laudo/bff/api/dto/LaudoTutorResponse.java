package br.com.clyvo.kura.tutor.laudo.bff.api.dto;

import br.com.clyvo.kura.tutor.laudo.application.LaudoTutor;

import java.time.Instant;

/** Laudo como o tutor vê. Sem observacao, dados, autoria nem clínica (G0 §5). */
public record LaudoTutorResponse(String id, String tipo, String titulo, Instant dataExame, String laboratorio,
                                 String contentType, long tamanhoBytes, String urlArquivo) {

    public static LaudoTutorResponse de(LaudoTutor l, String urlArquivo) {
        return new LaudoTutorResponse(l.id(), l.tipo(), l.titulo(), l.dataExame(), l.laboratorio(),
                l.contentType(), l.tamanhoBytes(), urlArquivo);
    }
}
