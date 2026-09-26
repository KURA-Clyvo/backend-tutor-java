-- =============================================================================
-- V23__agendamento_recepcao.sql (variante H2)
-- Equivalente funcional de db/migration-oracle/V23__agendamento_recepcao.sql.
-- REC-07 (KURA_BACKLOG_RECEPCAO) — split obrigatorio (G0 item 7): o H2 recusa a sintaxe Oracle
-- de conversao de IDENTITY para DEFAULT de sequence (passo 4). O resto da migration (CHECK de
-- origem, colunas novas, FK, CHECK da resposta D-1, indices, comentarios) e sintaxe portavel e
-- foi copiado sem alteracao.
--
-- Diferenca de sintaxe em relacao a variante Oracle (so sintaxe - o efeito no schema e o mesmo):
--   Oracle: ALTER TABLE AGENDAMENTO MODIFY (ID_AGENDAMENTO DROP IDENTITY)
--   H2    : ALTER TABLE AGENDAMENTO ALTER COLUMN ID_AGENDAMENTO DROP IDENTITY
--   Oracle: ALTER TABLE AGENDAMENTO MODIFY (ID_AGENDAMENTO DEFAULT SEQ_AGENDAMENTO.NEXTVAL)
--   H2    : ALTER TABLE AGENDAMENTO ALTER COLUMN ID_AGENDAMENTO SET DEFAULT NEXT VALUE FOR SEQ_AGENDAMENTO
-- `NEXT VALUE FOR` e a forma nativa do H2 (SQL:2003); SEQ_AGENDAMENTO.NEXTVAL so existe la via
-- MODE=Oracle e nao e aceito em expressao de DEFAULT (medido no G0: `Unknown data type: "DROP"`
-- com a sintaxe Oracle sobre o H2 real do profile dev).
-- =============================================================================

-- 1. CHECK primeiro: e o unico passo que pode falhar por dado pre-existente (DDL autocommita no Oracle).
ALTER TABLE AGENDAMENTO ADD CONSTRAINT CHK_AGEND_ORIGEM
    CHECK (DS_ORIGEM IN ('PORTAL', 'RECEPCAO', 'TRIAGEM_LUNA'));

-- 2. Colunas novas, todas nullable (o Java nao mapeia nenhuma - le a linha sem elas).
ALTER TABLE AGENDAMENTO ADD (
    DT_CHECKIN               TIMESTAMP     NULL,
    DT_INICIO_ATENDIMENTO    TIMESTAMP     NULL,
    ID_TRIAGEM_ORIGEM        NUMBER(10)    NULL,
    DT_LEMBRETE_CONFIRMACAO  TIMESTAMP     NULL,
    DS_RESPOSTA_CONFIRMACAO  VARCHAR2(20)  NULL,
    DT_RESPOSTA_CONFIRMACAO  TIMESTAMP     NULL
);

ALTER TABLE AGENDAMENTO ADD CONSTRAINT FK_AGEND_TRIAGEM
    FOREIGN KEY (ID_TRIAGEM_ORIGEM) REFERENCES TRIAGEM_LUNA (ID_TRIAGEM);

ALTER TABLE AGENDAMENTO ADD CONSTRAINT CHK_AGEND_RESP_CONF
    CHECK (DS_RESPOSTA_CONFIRMACAO IS NULL
           OR DS_RESPOSTA_CONFIRMACAO IN ('SIM', 'CANCELAR', 'REMARCAR'));

-- 3. Indices: FK (evita lock de tabela no DELETE do pai) e a consulta da tela "Hoje" / D-1.
CREATE INDEX IDX_AGEND_TRIAGEM    ON AGENDAMENTO (ID_TRIAGEM_ORIGEM);
CREATE INDEX IDX_AGEND_CLINICA_DT ON AGENDAMENTO (ID_CLINICA, DT_AGENDAMENTO);

-- 4. PK: um gerador so (sintaxe H2). O Java ja usa SEQ_AGENDAMENTO (id explicito); o .NET passa a omitir a coluna.
ALTER TABLE AGENDAMENTO ALTER COLUMN ID_AGENDAMENTO DROP IDENTITY;
ALTER TABLE AGENDAMENTO ALTER COLUMN ID_AGENDAMENTO SET DEFAULT NEXT VALUE FOR SEQ_AGENDAMENTO;

-- 5. Contrato da tabela compartilhada.
COMMENT ON TABLE AGENDAMENTO IS
    'Shared-write. Java cria (DS_ORIGEM=PORTAL), confirma e cancela. .NET cria (RECEPCAO / TRIAGEM_LUNA), muda ST_STATUS pela maquina de estados, grava DT_CHECKIN / DT_INICIO_ATENDIMENTO e a confirmacao D-1. Toda escrita incrementa NR_VERSION. PK unica: SEQ_AGENDAMENTO. Horarios em hora local America/Sao_Paulo.';
COMMENT ON COLUMN AGENDAMENTO.DS_ORIGEM               IS 'PORTAL (app do tutor) | RECEPCAO | TRIAGEM_LUNA. Registrada so para frente (R5).';
COMMENT ON COLUMN AGENDAMENTO.DT_CHECKIN              IS 'Chegada do paciente (hora local SP). Nao e ST_STATUS (A-2).';
COMMENT ON COLUMN AGENDAMENTO.DT_INICIO_ATENDIMENTO   IS 'Inicio do atendimento (hora local SP). Nao e ST_STATUS (A-2).';
COMMENT ON COLUMN AGENDAMENTO.ID_TRIAGEM_ORIGEM       IS 'Triagem da Luna que originou o agendamento (DS_ORIGEM=TRIAGEM_LUNA).';
COMMENT ON COLUMN AGENDAMENTO.DT_LEMBRETE_CONFIRMACAO IS 'Envio do lembrete D-1 (hora local SP). Nulo = nao enviado.';
COMMENT ON COLUMN AGENDAMENTO.DS_RESPOSTA_CONFIRMACAO IS 'SIM | CANCELAR | REMARCAR (A-10).';
COMMENT ON COLUMN AGENDAMENTO.DT_RESPOSTA_CONFIRMACAO IS 'Recebimento da resposta D-1 (hora local SP).';
