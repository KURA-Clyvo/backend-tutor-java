-- =============================================================================
-- V5__agendamento_observacoes.sql (variante H2)
-- Equivalente funcional de db/migration/oracle/V5__agendamento_observacoes.sql.
-- Sem checks defensivos — ver nota em h2/V2__concurrency_idempotency.sql.
-- ADD COLUMN uma coluna por vez (mais portável que o ADD (...) multi-coluna
-- do dialeto Oracle) em vez de depender do suporte a essa sintaxe no modo
-- MODE=Oracle do H2.
-- =============================================================================

ALTER TABLE AGENDAMENTO ADD COLUMN NM_PACIENTE      VARCHAR2(200);
ALTER TABLE AGENDAMENTO ADD COLUMN DS_SERVICO       VARCHAR2(200);
ALTER TABLE AGENDAMENTO ADD COLUMN DS_OBSERVACOES   VARCHAR2(1000);
ALTER TABLE AGENDAMENTO ADD COLUMN DT_CRIACAO       TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL;
ALTER TABLE AGENDAMENTO ADD COLUMN DT_CONFIRMACAO   TIMESTAMP;
ALTER TABLE AGENDAMENTO ADD COLUMN DT_CANCELAMENTO  TIMESTAMP;
ALTER TABLE AGENDAMENTO ADD COLUMN DS_MOTIVO_CANCEL VARCHAR2(500);
ALTER TABLE AGENDAMENTO ADD COLUMN ID_EVENTO_GERADO NUMBER(10);

ALTER TABLE AGENDAMENTO ADD CONSTRAINT FK_AGEND_EVENTO FOREIGN KEY (ID_EVENTO_GERADO) REFERENCES EVENTO_CLINICO(ID_EVENTO);

CREATE INDEX IDX_AGEND_EVENTO ON AGENDAMENTO(ID_EVENTO_GERADO);

COMMENT ON COLUMN AGENDAMENTO.NM_PACIENTE      IS 'Nome do pet snapshot ao criar agendamento — denormalizado.';
COMMENT ON COLUMN AGENDAMENTO.DS_SERVICO       IS 'Descrição livre do serviço solicitado pelo tutor.';
COMMENT ON COLUMN AGENDAMENTO.DS_OBSERVACOES   IS 'Observações do tutor ao criar/reagendar.';
COMMENT ON COLUMN AGENDAMENTO.DT_CRIACAO       IS 'Preenchido pelo @CreatedDate JPA Auditing.';
COMMENT ON COLUMN AGENDAMENTO.DT_CANCELAMENTO  IS 'Preenchido por Agendamento.cancelar(motivo).';
COMMENT ON COLUMN AGENDAMENTO.ID_EVENTO_GERADO IS 'Quando ST_STATUS=REALIZADO, .NET preenche com ID_EVENTO_CLINICO gerado.';
