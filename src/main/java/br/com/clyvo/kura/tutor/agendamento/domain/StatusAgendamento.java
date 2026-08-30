package br.com.clyvo.kura.tutor.agendamento.domain;

/**
 * Domínio de {@code AGENDAMENTO.ST_STATUS} — os mesmos seis valores do
 * {@code CHECK CHK_AGEND_STATUS} (V1__initial_schema.sql).
 *
 * <p><b>A tabela tem dois donos.</b> O Java cria, reagenda e cancela; o {@code .NET} escreve
 * {@code ST_STATUS} pelo {@code PATCH /api/v1/agendamentos/{id}/status} (REALIZADO, CANCELADO,
 * NAO_COMPARECEU, CONFIRMADO). Regra de estado que exista só de um lado é divergência
 * silenciosa entre os dois — por isso {@link #isFinal()} vive aqui, no enum compartilhado, e
 * não repetida em cada guarda.
 */
public enum StatusAgendamento {
    INTENCAO,
    AGENDADO,
    CONFIRMADO,
    REALIZADO,
    CANCELADO,
    NAO_COMPARECEU;

    /**
     * Estados <b>finais</b>: o agendamento já registrou o que aconteceu, e reescrevê-lo apaga
     * fato em vez de corrigir rascunho.
     *
     * <p>🔴 <b>{@code NAO_COMPARECEU} entrou aqui na FD-06 do ciclo FIN, e o motivo importa.</b>
     * Até aquela task o {@code .NET} tinha um validator que só aceitava {@code REALIZADO} e
     * {@code CANCELADO}, e o Java <b>nunca escreve</b> {@code NAO_COMPARECEU} — ou seja,
     * <b>ninguém no sistema em execução conseguia produzir esse valor</b>, e a permissividade de
     * {@code cancelar()} em relação a ele era inalcançável. A FD-06 é exatamente o que torna esse
     * caminho alcançável pela primeira vez.
     *
     * <p>Sem esta mudança, um agendamento marcado como falta pela clínica poderia ser cancelado
     * pelo tutor logo depois, apagando o registro da ausência — e é sobre esse registro que uma
     * política de no-show (e o faturamento do ciclo FIN) se apoiaria. O contrato publicado deste
     * serviço, aliás, <b>já prometia</b> a regra estrita: o {@code @Operation} de
     * {@code PATCH /{id}/cancelar} diz «Só cancela se status for AGENDADO, CONFIRMADO ou
     * INTENCAO». O código é que não cumpria.
     */
    public boolean isFinal() {
        return this == REALIZADO || this == CANCELADO || this == NAO_COMPARECEU;
    }
}
