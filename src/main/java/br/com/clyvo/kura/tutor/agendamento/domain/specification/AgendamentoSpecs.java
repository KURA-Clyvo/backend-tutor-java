package br.com.clyvo.kura.tutor.agendamento.domain.specification;

import br.com.clyvo.kura.tutor.agendamento.domain.Agendamento;
import br.com.clyvo.kura.tutor.agendamento.domain.StatusAgendamento;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

public class AgendamentoSpecs {

    private AgendamentoSpecs() {}

    public static Specification<Agendamento> pertenceAoTutor(Long idTutor) {
        return (root, query, cb) -> cb.equal(root.get("tutor").get("idTutor"), idTutor);
    }

    public static Specification<Agendamento> comStatus(StatusAgendamento status) {
        return (root, query, cb) -> cb.equal(root.get("stStatus"), status);
    }

    public static Specification<Agendamento> apartirDe(LocalDateTime inicio) {
        return (root, query, cb) ->
            cb.greaterThanOrEqualTo(root.get("dtAgendamento"), inicio);
    }

    public static Specification<Agendamento> ate(LocalDateTime fim) {
        return (root, query, cb) ->
            cb.lessThanOrEqualTo(root.get("dtAgendamento"), fim);
    }

    public static Specification<Agendamento> comTipo(String tipo) {
        return (root, query, cb) -> cb.equal(root.get("dsTipoConsulta"), tipo);
    }

    /**
     * Compõe todos os filtros opcionais sobre a âncora obrigatória {@code pertenceAoTutor}.
     * idTutor é sempre aplicado — os demais só entram se não-nulos/não-blank.
     */
    public static Specification<Agendamento> construir(Long idTutor,
                                                        StatusAgendamento status,
                                                        LocalDateTime dataInicio,
                                                        LocalDateTime dataFim,
                                                        String tipo) {
        Specification<Agendamento> spec = pertenceAoTutor(idTutor);
        if (status != null)                       spec = spec.and(comStatus(status));
        if (dataInicio != null)                   spec = spec.and(apartirDe(dataInicio));
        if (dataFim != null)                      spec = spec.and(ate(dataFim));
        if (tipo != null && !tipo.isBlank())      spec = spec.and(comTipo(tipo));
        return spec;
    }
}
