-- =============================================================================
-- V5__agendamento_observacoes.sql
-- Adiciona campos Java-específicos em AGENDAMENTO.
-- Referência: §3.7 do plano v5.
--
-- Contexto:
-- AGENDAMENTO é shared-write (Java cria/edita; .NET atualiza ST_STATUS).
-- Os campos abaixo são usados SOMENTE pelo Java — o .NET não os escreve.
-- ID_EVENTO_GERADO é preenchido pelo .NET quando ST_STATUS=REALIZADO.
-- =============================================================================

ALTER TABLE AGENDAMENTO ADD (
    DS_OBSERVACOES      VARCHAR2(1000),
    DT_CRIACAO          TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    DT_CONFIRMACAO      TIMESTAMP,
    DT_CANCELAMENTO     TIMESTAMP,
    DS_MOTIVO_CANCEL    VARCHAR2(500),
    ID_EVENTO_GERADO    NUMBER(10)
);

ALTER TABLE AGENDAMENTO
    ADD CONSTRAINT FK_AGEND_EVENTO
    FOREIGN KEY (ID_EVENTO_GERADO) REFERENCES EVENTO_CLINICO(ID_EVENTO);

CREATE INDEX IDX_AGEND_EVENTO ON AGENDAMENTO(ID_EVENTO_GERADO);

COMMENT ON COLUMN AGENDAMENTO.DS_OBSERVACOES   IS 'Observações do tutor ao criar/reagendar.';
COMMENT ON COLUMN AGENDAMENTO.DT_CRIACAO       IS 'Preenchido pelo @CreatedDate JPA Auditing.';
COMMENT ON COLUMN AGENDAMENTO.DT_CANCELAMENTO  IS 'Preenchido por Agendamento.cancelar(motivo).';
COMMENT ON COLUMN AGENDAMENTO.ID_EVENTO_GERADO IS 'Quando ST_STATUS=REALIZADO, .NET preenche com ID_EVENTO_CLINICO gerado.';
