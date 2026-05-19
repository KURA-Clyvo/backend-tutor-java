package br.com.clyvo.kura.tutor.dto.response;

import br.com.clyvo.kura.tutor.entity.Tutor;
import java.time.LocalDate;

public record TutorResponse(
    Long idTutor, String nmTutor, String dsEmail,
    String nrTelefone, String dsWhatsapp, LocalDate dtNascimento,
    String nmCidade, String sgUf, boolean avisoPrivacidade
) {
    public static TutorResponse fromEntity(Tutor t) {
        return new TutorResponse(t.getIdTutor(), t.getNmTutor(), t.getDsEmail(),
            t.getNrTelefone(), t.getDsWhatsapp(), t.getDtNascimento(),
            t.getNmCidade(), t.getSgUf(), t.temAvisoPrivacidade());
    }
}
