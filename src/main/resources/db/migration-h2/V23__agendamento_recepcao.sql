-- =============================================================================
-- V23__agendamento_recepcao.sql (variante H2)
-- Equivalente funcional de db/migration-oracle/V23__agendamento_recepcao.sql.
-- REC-07 (KURA_BACKLOG_RECEPCAO) — split obrigatorio (G0 item 7): o H2 recusa a sintaxe Oracle
-- de conversao de IDENTITY para DEFAULT de sequence (passo 4). O resto da migration (CHECK de
-- origem, colunas novas, FK, CHECK da resposta D-1, indices, comentarios) e sintaxe portavel e
-- foi copiado sem alteracao.
--
-- Diferenca real (corrigida na revisao G2 / g2-rec07.md M2 - o texto anterior atribuia a
-- evidencia errada): o H2 recusa a clausula `MODIFY (...)` do Oracle por INTEIRO — tanto para
-- DROP IDENTITY quanto para SET DEFAULT —, com `Unknown data type: "DROP"` ja no DROP IDENTITY
-- (medido no G0 sobre o H2 real do profile dev). O valor do DEFAULT em si NAO e o problema:
-- `SET DEFAULT SEQ_AGENDAMENTO.NEXTVAL` TAMBEM e aceito pelo H2 MODE=Oracle (sonda da G2:
-- `ALTER TABLE t ALTER COLUMN id SET DEFAULT seq.NEXTVAL` + insert sem id -> valor da sequence,
-- EXIT=0). Usamos `NEXT VALUE FOR`, a forma nativa do H2 (SQL:2003), por ser a idiomatica desta
-- sintaxe (`ALTER COLUMN ... SET DEFAULT ...`), nao porque `NEXTVAL` falhasse:
--   Oracle: ALTER TABLE AGENDAMENTO MODIFY (ID_AGENDAMENTO DROP IDENTITY)
--   H2    : ALTER TABLE AGENDAMENTO ALTER COLUMN ID_AGENDAMENTO DROP IDENTITY
--   Oracle: ALTER TABLE AGENDAMENTO MODIFY (ID_AGENDAMENTO DEFAULT SEQ_AGENDAMENTO.NEXTVAL)
--   H2    : ALTER TABLE AGENDAMENTO ALTER COLUMN ID_AGENDAMENTO SET DEFAULT NEXT VALUE FOR SEQ_AGENDAMENTO
-- =============================================================================
--
-- PRE-CONDICAO 2 (m2 da revisao G2 / g2-rec07.md M5 - residuo da IDENTITY antiga, G0 item 1):
--   se algum insert historico usou a IDENTITY sem id explicito, o proximo valor de
--   SEQ_AGENDAMENTO pode ja ter sido ocupado por ela - o ORA-00001 so apareceria DEPOIS, em
--   runtime, quando a sequence alcancasse esse id (teorico nesta base: nenhum insert de producao
--   usa a IDENTITY sem id - ver g2-rec07.md M5). Verificar ANTES de aplicar (no Oracle real; esta
--   variante -h2 nunca ve dado acumulado, roda so contra o H2 vazio do profile dev):
--     SELECT MAX(ID_AGENDAMENTO) FROM AGENDAMENTO;                                            -- MX
--     SELECT LAST_NUMBER - CACHE_SIZE FROM USER_SEQUENCES
--      WHERE SEQUENCE_NAME = 'SEQ_AGENDAMENTO';                                                -- PISO
--   MX tem de ser MENOR que PISO; senao, avancar a sequence (SELECT SEQ_AGENDAMENTO.NEXTVAL
--   repetidas vezes ate passar de MX) antes de aplicar esta migration.
--   Controle positivo (a mesma consulta MAX enxerga dado real, nao 0 por tabela vazia):
--     SELECT COUNT(*) FROM AGENDAMENTO; -- tem de ser > 0 nesta base (G0/G2 mediram 10).

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
    'Shared-write. Java cria (DS_ORIGEM=PORTAL), remarca e cancela. .NET cria (RECEPCAO / TRIAGEM_LUNA), muda ST_STATUS pela maquina de estados, grava DT_CHECKIN / DT_INICIO_ATENDIMENTO e a confirmacao D-1. Toda escrita incrementa NR_VERSION. PK unica: SEQ_AGENDAMENTO. Horarios em hora local America/Sao_Paulo.';
COMMENT ON COLUMN AGENDAMENTO.DS_ORIGEM               IS 'PORTAL (app do tutor) | RECEPCAO | TRIAGEM_LUNA. Registrada so para frente (R5).';
COMMENT ON COLUMN AGENDAMENTO.DT_CHECKIN              IS 'Chegada do paciente (hora local SP). Nao e ST_STATUS (A-2).';
COMMENT ON COLUMN AGENDAMENTO.DT_INICIO_ATENDIMENTO   IS 'Inicio do atendimento (hora local SP). Nao e ST_STATUS (A-2).';
COMMENT ON COLUMN AGENDAMENTO.ID_TRIAGEM_ORIGEM       IS 'Triagem da Luna que originou o agendamento (DS_ORIGEM=TRIAGEM_LUNA).';
COMMENT ON COLUMN AGENDAMENTO.DT_LEMBRETE_CONFIRMACAO IS 'Envio do lembrete D-1 (hora local SP). Nulo = nao enviado.';
COMMENT ON COLUMN AGENDAMENTO.DS_RESPOSTA_CONFIRMACAO IS 'SIM | CANCELAR | REMARCAR (A-10).';
COMMENT ON COLUMN AGENDAMENTO.DT_RESPOSTA_CONFIRMACAO IS 'Recebimento da resposta D-1 (hora local SP).';
