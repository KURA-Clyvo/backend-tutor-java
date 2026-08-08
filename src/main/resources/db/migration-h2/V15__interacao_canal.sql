-- =============================================================================
-- V15__interacao_canal.sql (variante H2)
-- Equivalente funcional de db/migration-oracle/V15__interacao_canal.sql.
-- TASK-66 (KURA_BACKLOG_FIX_6). Cria INTERACAO_CANAL, tabela .NET-owned que
-- falta no schema — a Luna (kura-luna-ai) chama POST /api/luna/interactions
-- hoje e recebe 404, porque o endpoint nunca foi implementado e a tabela em
-- que ele escreveria não existe. Ver o cabeçalho da variante Oracle para o
-- contexto completo (DTO de origem, divergência de rota vs. o brief,
-- justificativa de CLOB e VARCHAR2(4000)) — não repetido aqui.
--
-- Diferença de sintaxe em relação à variante Oracle (só sintaxe — o efeito no
-- schema é o mesmo):
--   Oracle: DEFAULT SEQ_INTERACAO_CANAL.NEXTVAL
--   H2    : DEFAULT NEXT VALUE FOR SEQ_INTERACAO_CANAL
-- `NEXT VALUE FOR` é a forma nativa do H2 (SQL:2003); SEQ_X.NEXTVAL só existe
-- lá via MODE=Oracle e não é aceito em expressão de DEFAULT (mesmo motivo
-- documentado em V12__sequences_dotnet.sql).
-- =============================================================================

CREATE SEQUENCE SEQ_INTERACAO_CANAL START WITH 100 INCREMENT BY 1;

CREATE TABLE INTERACAO_CANAL (
    ID_INTERACAO   NUMBER(10)     DEFAULT NEXT VALUE FOR SEQ_INTERACAO_CANAL PRIMARY KEY,
    ID_CLINICA     NUMBER(10)     NOT NULL REFERENCES CLINICA(ID_CLINICA),
    ID_TUTOR       NUMBER(10)     REFERENCES TUTOR(ID_TUTOR),
    DS_CANAL       VARCHAR2(20)   NOT NULL,
    DS_DIRECAO     VARCHAR2(20)   NOT NULL,
    DS_CONTEUDO    VARCHAR2(4000) NOT NULL,
    DT_RECEBIMENTO TIMESTAMP      NOT NULL,
    DS_METADADOS   CLOB,
    ST_ATIVA       CHAR(1)        DEFAULT 'S' NOT NULL,
    DT_CRIACAO     TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL,
    DT_ATUALIZACAO TIMESTAMP,
    CONSTRAINT CHK_INTERACAO_CANAL   CHECK (DS_CANAL   IN ('WHATSAPP','EMAIL','SMS')),
    CONSTRAINT CHK_INTERACAO_DIRECAO CHECK (DS_DIRECAO IN ('INBOUND','OUTBOUND')),
    CONSTRAINT CHK_INTERACAO_ATIVA   CHECK (ST_ATIVA   IN ('S','N'))
);
COMMENT ON TABLE INTERACAO_CANAL IS '.NET owned. Interação de canal (WhatsApp/email/SMS) registrada pela Luna via POST /api/luna/interactions.';

-- TRIAGEM_LUNA é pré-existente (V9) — FK nova nasce nullable, a migration não
-- pode assumir que a tabela está vazia. TriageRequestDTO envia id_interacao,
-- ligando triagem à interação que a originou.
ALTER TABLE TRIAGEM_LUNA ADD (
    ID_INTERACAO NUMBER(10) REFERENCES INTERACAO_CANAL(ID_INTERACAO)
);
