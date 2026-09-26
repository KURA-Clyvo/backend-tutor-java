-- =============================================================================
-- V23__agendamento_recepcao.sql  (ciclo KURA_BACKLOG_RECEPCAO, REC-07; desenho: G0 item 7)
-- Recepcao: origem com CHECK, check-in / inicio de atendimento (A-2), triagem de origem (F-3),
-- confirmacao D-1 (A-10) e PK unica por SEQ_AGENDAMENTO (A-4 - fecha a "estrategia dupla" que a V12 deixou).
-- Todas as colunas TIMESTAMP novas guardam hora LOCAL de America/Sao_Paulo (F-4 / A-5), como DT_AGENDAMENTO.
--
-- PRE-CONDICAO (rodar ANTES em base com dado acumulado; tem de devolver 0 linhas):
--   SELECT DS_ORIGEM, COUNT(*) FROM AGENDAMENTO
--    WHERE DS_ORIGEM NOT IN ('PORTAL','RECEPCAO','TRIAGEM_LUNA') GROUP BY DS_ORIGEM;
--   Controle positivo obrigatorio: SELECT DS_ORIGEM, COUNT(*) FROM AGENDAMENTO GROUP BY DS_ORIGEM (tem de listar PORTAL).
--   Medido no compose em 2026-09-26: PORTAL 10, nenhum outro valor.
-- Split: o H2 nao aceita MODIFY (col DROP IDENTITY) - variante em db/migration-h2/V23__agendamento_recepcao.sql.
-- =============================================================================

-- 1. CHECK primeiro: e o unico passo que pode falhar por dado pre-existente (DDL autocommita).
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

-- 4. PK: um gerador so. O Java ja usa SEQ_AGENDAMENTO (id explicito); o .NET passa a omitir a coluna.
ALTER TABLE AGENDAMENTO MODIFY (ID_AGENDAMENTO DROP IDENTITY);
ALTER TABLE AGENDAMENTO MODIFY (ID_AGENDAMENTO DEFAULT SEQ_AGENDAMENTO.NEXTVAL);

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
