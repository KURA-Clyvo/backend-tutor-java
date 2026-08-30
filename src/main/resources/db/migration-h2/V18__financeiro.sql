-- =============================================================================
-- V18__financeiro.sql (variante H2)
-- Equivalente funcional de db/migration-oracle/V18__financeiro.sql.
-- FD-07 (KURA_BACKLOG_FIN_DOTNET). Cria SERVICO_PRECO (catálogo de preços da
-- clínica, ruling D-2) e COBRANCA (lançamento pendurado no atendimento, ruling
-- D-3). Ver o cabeçalho da variante Oracle para o contexto completo — por que
-- ID_SERVICO_PRECO é nullable E VL_COBRADO é coluna própria (o override da D-2:
-- o valor é COPIADO, não lido por FK, senão mudar o preço de tabela reescreve o
-- histórico financeiro), por que COBRANCA.ID_CLINICA é denormalizado, por que
-- DT_COBRANCA é NOT NULL com DEFAULT, por que DS_FORMA_PAGAMENTO é nullable e
-- sem CHECK, por que a PK é sequence e não IDENTITY, o argumento de cada índice
-- (e dos dois recusados), e o escopo negativo (D-1/D-6). Nada disso é repetido
-- neste arquivo.
--
-- Diferença de sintaxe em relação à variante Oracle (só sintaxe — o efeito no
-- schema é o mesmo), em DUAS linhas, uma por tabela:
--   Oracle: DEFAULT SEQ_SERVICO_PRECO.NEXTVAL   /  DEFAULT SEQ_COBRANCA.NEXTVAL
--   H2    : DEFAULT NEXT VALUE FOR SEQ_SERVICO_PRECO
--           DEFAULT NEXT VALUE FOR SEQ_COBRANCA
-- `NEXT VALUE FOR` é a forma nativa do H2 (SQL:2003).
--
-- ⚠️ Não escreva aqui que o H2 "não aceita SEQ_X.NEXTVAL em DEFAULT" — essa
-- frase existe no cabeçalho da V15, é FALSA, e foi refutada por medição na
-- FD-01 (UsuarioClinicaV17MigrationTest#h2AceitaNextvalEmExpressaoDeDefault, H2
-- 2.2.224 MODE=Oracle). O split se mantém pelo lado NÃO MEDIDO — se o Oracle
-- aceita `NEXT VALUE FOR` —, e o argumento completo está na variante Oracle.
--
-- NUMBER(10,2) é aceito pelo H2 (mapeia para NUMERIC(10,2)) e preserva escala 2
-- de verdade: medido nesta task em
-- FinanceiroV18MigrationTest#dinheiroTemEscalaDeDoisDecimaisDeVerdade, em vez de
-- assumido.
--
-- Essas 2 linhas do DEFAULT da PK são as ÚNICAS que divergem entre as
-- variantes — verificado e travado por
-- FinanceiroV18MigrationTest#variantesDiferemApenasNasLinhasDoDefaultDaPk, que
-- compara as duas versões linha a linha (ignorando comentários, normalizando
-- CRLF/LF) e falha se aparecer uma TERCEIRA divergência. Editar um lado só
-- quebra a suíte em vez de virar dívida silenciosa.
-- =============================================================================

CREATE SEQUENCE SEQ_SERVICO_PRECO START WITH 100 INCREMENT BY 1;

CREATE TABLE SERVICO_PRECO (
    ID_SERVICO_PRECO NUMBER(10)    DEFAULT NEXT VALUE FOR SEQ_SERVICO_PRECO PRIMARY KEY,
    ID_CLINICA       NUMBER(10)    NOT NULL,
    NM_SERVICO       VARCHAR2(200) NOT NULL,
    VL_PRECO         NUMBER(10,2)  NOT NULL,
    ST_ATIVA         CHAR(1)       DEFAULT 'S' NOT NULL,
    DT_CRIACAO       TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    DT_ATUALIZACAO   TIMESTAMP,
    CONSTRAINT FK_SERVICO_PRECO_CLINICA FOREIGN KEY (ID_CLINICA) REFERENCES CLINICA(ID_CLINICA),
    CONSTRAINT CHK_SERVICO_PRECO_ATIVA  CHECK (ST_ATIVA IN ('S','N')),
    CONSTRAINT CHK_SERVICO_PRECO_VALOR  CHECK (VL_PRECO >= 0)
);

COMMENT ON TABLE  SERVICO_PRECO                IS '.NET owned. Catalogo de precos da clinica (ruling D-2): quanto a clinica cobra por cada servico. NAO e fonte de valor de cobranca ja lancada -- ver COBRANCA.VL_COBRADO.';
COMMENT ON COLUMN SERVICO_PRECO.ID_CLINICA     IS 'Clinica dona do item de catalogo. NOT NULL: tabela de preco e sempre de uma clinica -- e a chave do isolamento multi-tenant do .NET (FD-08).';
COMMENT ON COLUMN SERVICO_PRECO.NM_SERVICO     IS 'Nome do servico como o gestor o cadastra. Sem UNIQUE por clinica de proposito: com soft delete, uma unique impediria recadastrar um servico depois de desativado.';
COMMENT ON COLUMN SERVICO_PRECO.VL_PRECO       IS 'Preco de tabela vigente. Alterar esta coluna NAO altera cobranca ja lancada -- COBRANCA.VL_COBRADO guarda copia do valor no momento do lancamento.';
COMMENT ON COLUMN SERVICO_PRECO.ST_ATIVA       IS 'Soft delete no padrao do projeto (S/N). DELETE fisico nunca acontece.';

CREATE SEQUENCE SEQ_COBRANCA START WITH 100 INCREMENT BY 1;

CREATE TABLE COBRANCA (
    ID_COBRANCA        NUMBER(10)   DEFAULT NEXT VALUE FOR SEQ_COBRANCA PRIMARY KEY,
    ID_EVENTO_CLINICO  NUMBER(10)   NOT NULL,
    ID_CLINICA         NUMBER(10)   NOT NULL,
    ID_SERVICO_PRECO   NUMBER(10),
    VL_COBRADO         NUMBER(10,2) NOT NULL,
    DS_FORMA_PAGAMENTO VARCHAR2(30),
    DT_COBRANCA        TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    ST_ATIVA           CHAR(1)      DEFAULT 'S' NOT NULL,
    DT_CRIACAO         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP NOT NULL,
    DT_ATUALIZACAO     TIMESTAMP,
    CONSTRAINT FK_COBRANCA_EVENTO  FOREIGN KEY (ID_EVENTO_CLINICO) REFERENCES EVENTO_CLINICO(ID_EVENTO),
    CONSTRAINT FK_COBRANCA_CLINICA FOREIGN KEY (ID_CLINICA)        REFERENCES CLINICA(ID_CLINICA),
    CONSTRAINT FK_COBRANCA_SERVICO FOREIGN KEY (ID_SERVICO_PRECO)  REFERENCES SERVICO_PRECO(ID_SERVICO_PRECO),
    CONSTRAINT CHK_COBRANCA_ATIVA  CHECK (ST_ATIVA IN ('S','N')),
    CONSTRAINT CHK_COBRANCA_VALOR  CHECK (VL_COBRADO >= 0)
);

COMMENT ON TABLE  COBRANCA                    IS '.NET owned. Lancamento financeiro (ruling D-3), pendurado no atendimento que aconteceu. Sem gateway, transacao externa, status de processamento ou conciliacao (D-1); sem imposto, repasse ou margem (D-6).';
COMMENT ON COLUMN COBRANCA.ID_EVENTO_CLINICO  IS 'Atendimento que originou a cobranca. NOT NULL: nao existe lancamento sem atendimento (D-3). Referencia EVENTO_CLINICO(ID_EVENTO) -- a PK tem nome diferente da coluna, ver cabecalho.';
COMMENT ON COLUMN COBRANCA.ID_CLINICA         IS 'Clinica do lancamento. Denormalizado de EVENTO_CLINICO de proposito: e a coluna que o ApplyTenantFilters do .NET exige (FD-08) e a que os KPI da FD-11 agrupam. Manter coerente com o evento e responsabilidade do service.';
COMMENT ON COLUMN COBRANCA.ID_SERVICO_PRECO   IS 'Item de catalogo que originou o lancamento, quando houve um. NULLABLE: valor avulso sem servico tabelado e lancamento legitimo (D-2). E rastreabilidade de ORIGEM (mix por servico da FD-11), nunca fonte de valor.';
COMMENT ON COLUMN COBRANCA.VL_COBRADO         IS 'Valor efetivamente cobrado, COPIADO no momento do lancamento. Coluna propria de proposito: ler o valor por FK faria mudar preco de tabela reescrever o historico financeiro retroativamente. NAO remover por parecer redundante.';
COMMENT ON COLUMN COBRANCA.DS_FORMA_PAGAMENTO IS 'Descritor do meio usado pelo cliente. Nullable e sem CHECK: exigi-lo forcaria o veterinario a preenche-lo no meio do atendimento, e lista fechada em schema e inventario manual que apodrece. NAO e status de processamento (D-1).';
COMMENT ON COLUMN COBRANCA.DT_COBRANCA        IS 'Data do lancamento. NOT NULL com DEFAULT: linha com data nula seria invisivel a todo KPI por periodo (FD-11) -- receita lancada que nenhum relatorio enxerga.';
COMMENT ON COLUMN COBRANCA.ST_ATIVA           IS 'Soft delete no padrao do projeto (S/N). DELETE fisico nunca acontece.';

CREATE INDEX IDX_COBRANCA_CLINICA_DATA ON COBRANCA(ID_CLINICA, DT_COBRANCA);
CREATE INDEX IDX_COBRANCA_EVENTO       ON COBRANCA(ID_EVENTO_CLINICO);
