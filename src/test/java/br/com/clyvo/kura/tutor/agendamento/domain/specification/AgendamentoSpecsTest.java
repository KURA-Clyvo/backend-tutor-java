package br.com.clyvo.kura.tutor.agendamento.domain.specification;

import br.com.clyvo.kura.tutor.agendamento.domain.Agendamento;
import br.com.clyvo.kura.tutor.agendamento.domain.StatusAgendamento;
import jakarta.persistence.criteria.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Testes unitários de AgendamentoSpecs.
 *
 * filtraPorStatus — verifica que o predicado de status invoca cb.equal corretamente.
 * combinaFiltros  — verifica que construir() compõe todos os filtros sem lançar exceção
 *                   e que cada predicado atômico é não-nulo.
 */
@SuppressWarnings("unchecked")
class AgendamentoSpecsTest {

    private final CriteriaBuilder  cb   = mock(CriteriaBuilder.class);
    private final CriteriaQuery<?> cq   = mock(CriteriaQuery.class);
    private final Root<Agendamento> root =
            Mockito.mock(Root.class, Mockito.RETURNS_DEEP_STUBS);
    private final Predicate predicate   = mock(Predicate.class);

    // ─── filtraPorStatus ────────────────────────────────────────────────────

    @Test
    void filtraPorStatus() {
        when(cb.equal(any(), eq(StatusAgendamento.AGENDADO))).thenReturn(predicate);

        Specification<Agendamento> spec = AgendamentoSpecs.comStatus(StatusAgendamento.AGENDADO);
        Predicate result = spec.toPredicate(root, cq, cb);

        assertNotNull(result);
        verify(cb).equal(root.get("stStatus"), StatusAgendamento.AGENDADO);
    }

    // ─── combinaFiltros ─────────────────────────────────────────────────────

    @Test
    void combinaFiltros() {
        LocalDateTime inicio = LocalDateTime.now().plusDays(1);
        LocalDateTime fim    = LocalDateTime.now().plusDays(7);

        // Verifica que cada spec atômica é criada corretamente
        assertNotNull(AgendamentoSpecs.pertenceAoTutor(42L));
        assertNotNull(AgendamentoSpecs.comStatus(StatusAgendamento.CONFIRMADO));
        assertNotNull(AgendamentoSpecs.apartirDe(inicio));
        assertNotNull(AgendamentoSpecs.ate(fim));
        assertNotNull(AgendamentoSpecs.comTipo("CONSULTA"));

        // Verifica que construir() retorna um Specification composto sem lançar exceção
        Specification<Agendamento> spec = AgendamentoSpecs.construir(
                42L, StatusAgendamento.AGENDADO, inicio, fim, "CONSULTA");
        assertNotNull(spec);

        // Verifica que a composição sem filtros opcionais também funciona
        Specification<Agendamento> apenasOwnership =
                AgendamentoSpecs.construir(42L, null, null, null, null);
        assertNotNull(apenasOwnership);

        // Predicado atômico de tutor isolado deve executar sem null
        when(cb.equal(any(), eq(42L))).thenReturn(predicate);
        Predicate tutorPredicate = AgendamentoSpecs.pertenceAoTutor(42L)
                .toPredicate(root, cq, cb);
        assertNotNull(tutorPredicate);
        verify(cb).equal(root.get("tutor").get("idTutor"), 42L);
    }
}
