-- =============================================================================
-- V17__usuario_clinica.sql (variante H2)
-- Equivalente funcional de db/migration-oracle/V17__usuario_clinica.sql.
-- FD-01 (KURA_BACKLOG_FIN_DOTNET). Cria USUARIO_CLINICA, a linha por humano
-- que introduz identidade individual no lado clínico (hoje o login do app da
-- clínica é POR CLÍNICA, contra CLINICA.DS_EMAIL_ACESSO/DS_SENHA_HASH, com o
-- veterinário escolhido por heurística de fallback). Ver o cabeçalho da
-- variante Oracle para o contexto completo — por que a tabela existe, por que
-- ID_VETERINARIO é nullable, por que a PK é sequence e não IDENTITY, por que a
-- unicidade de e-mail é por clínica e não global, o custo de acrescentar um
-- papel novo, e o escopo negativo (CLINICA.DS_EMAIL_ACESSO/DS_SENHA_HASH NÃO
-- são removidas aqui). Nada disso é repetido neste arquivo.
--
-- Diferença de sintaxe em relação à variante Oracle (só sintaxe — o efeito no
-- schema é o mesmo):
--   Oracle: DEFAULT SEQ_USUARIO_CLINICA.NEXTVAL
--   H2    : DEFAULT NEXT VALUE FOR SEQ_USUARIO_CLINICA
-- `NEXT VALUE FOR` é a forma nativa do H2 (SQL:2003).
--
-- ⚠️ CORREÇÃO DA REVISÃO G2 — não repita a frase antiga. Este cabeçalho dizia
-- que o H2 "não aceita SEQ_X.NEXTVAL em expressão de DEFAULT nem sob
-- MODE=Oracle", frase herdada da V15 e nunca executada por ninguém. É FALSA:
-- medido nesta task contra o H2 desta suíte (2.2.224, MODE=Oracle), o H2
-- ACEITA `DEFAULT SEQ_X.NEXTVAL` — travado em
-- UsuarioClinicaV17MigrationTest#h2AceitaNextvalEmExpressaoDeDefault. O que NÃO
-- foi medido é o inverso (Oracle aceitar `NEXT VALUE FOR`), porque nenhum teste
-- desta suíte toca Oracle. O split se mantém por causa do lado NÃO MEDIDO, e o
-- argumento completo está no cabeçalho da variante Oracle.
--
-- Essa é a ÚNICA linha que diverge entre as duas variantes — verificado e
-- travado por teste: UsuarioClinicaV17MigrationTest compara as duas versões
-- linha a linha (ignorando comentários, normalizando CRLF/LF) e falha se
-- aparecer uma segunda divergência. Editar um lado só quebra a suíte em vez de
-- virar dívida silenciosa.
-- =============================================================================

CREATE SEQUENCE SEQ_USUARIO_CLINICA START WITH 100 INCREMENT BY 1;

CREATE TABLE USUARIO_CLINICA (
    ID_USUARIO_CLINICA NUMBER(10)    DEFAULT NEXT VALUE FOR SEQ_USUARIO_CLINICA PRIMARY KEY,
    ID_CLINICA         NUMBER(10)    NOT NULL,
    ID_VETERINARIO     NUMBER(10),
    DS_EMAIL           VARCHAR2(120) NOT NULL,
    DS_SENHA_HASH      VARCHAR2(256) NOT NULL,
    TP_PERFIL          VARCHAR2(20)  NOT NULL,
    ST_ATIVA           CHAR(1)       DEFAULT 'S' NOT NULL,
    DT_CRIACAO         TIMESTAMP     DEFAULT CURRENT_TIMESTAMP NOT NULL,
    DT_ATUALIZACAO     TIMESTAMP,
    CONSTRAINT FK_USUARIO_CLINICA_CLINICA FOREIGN KEY (ID_CLINICA)     REFERENCES CLINICA(ID_CLINICA),
    CONSTRAINT FK_USUARIO_CLINICA_VET     FOREIGN KEY (ID_VETERINARIO) REFERENCES VETERINARIO(ID_VETERINARIO),
    CONSTRAINT UK_USUARIO_CLINICA_EMAIL   UNIQUE (ID_CLINICA, DS_EMAIL),
    CONSTRAINT CHK_USUARIO_CLINICA_PERFIL CHECK (TP_PERFIL IN ('GESTOR','VETERINARIO')),
    CONSTRAINT CHK_USUARIO_CLINICA_ATIVA  CHECK (ST_ATIVA  IN ('S','N'))
);

COMMENT ON TABLE  USUARIO_CLINICA                    IS '.NET owned. Identidade individual do lado clinico: uma linha por humano da clinica (login proprio + papel), substituindo o login por clinica de CLINICA.DS_EMAIL_ACESSO.';
COMMENT ON COLUMN USUARIO_CLINICA.ID_CLINICA         IS 'Clinica a que o usuario pertence. NOT NULL: nao existe usuario sem tenant -- e a chave do isolamento multi-tenant do .NET.';
COMMENT ON COLUMN USUARIO_CLINICA.ID_VETERINARIO     IS 'Vinculo explicito com o registro de VETERINARIO, quando o usuario for veterinario. NULL para GESTOR que nao atende. Nunca derivado por heuristica.';
COMMENT ON COLUMN USUARIO_CLINICA.DS_EMAIL           IS 'E-mail de login. Unico POR CLINICA (UK_USUARIO_CLINICA_EMAIL), nao globalmente -- a mesma pessoa pode existir em duas clinicas.';
COMMENT ON COLUMN USUARIO_CLINICA.DS_SENHA_HASH      IS 'Hash BCrypt da senha individual. Dimensao em paridade com CLINICA.DS_SENHA_HASH para a conversao da V17 nao truncar.';
COMMENT ON COLUMN USUARIO_CLINICA.TP_PERFIL          IS 'Papel: GESTOR ou VETERINARIO (CHK_USUARIO_CLINICA_PERFIL). Constraint nomeada e de proposito unico para que acrescentar papel futuro (ex.: RECEPCIONISTA) toque so ela.';
COMMENT ON COLUMN USUARIO_CLINICA.ST_ATIVA           IS 'Soft delete no padrao do projeto (S/N). DELETE fisico nunca acontece.';

-- -----------------------------------------------------------------------------
-- CONVERSÃO DO DADO EXISTENTE (ruling D-10) — bloco idêntico ao da variante
-- Oracle. A justificativa completa de cada predicado (por que exige
-- DS_SENHA_HASH, por que há NOT EXISTS, por que ST_ATIVA é herdado, por que
-- ID_VETERINARIO fica NULL, e por que isso converte ZERO linha em ambiente do
-- zero) está no cabeçalho do bloco na variante Oracle — não repetida aqui.
--
-- Os marcadores >>> / <<< abaixo são lidos por
-- UsuarioClinicaV17MigrationTest, que extrai este bloco do arquivo e o executa
-- contra dado plantado. NÃO renomeie nem remova os marcadores — o teste passa a
-- não achar o bloco e a prova de conversão morre.
-- -----------------------------------------------------------------------------
-- >>> BEGIN CONVERSAO_D10
INSERT INTO USUARIO_CLINICA (ID_CLINICA, ID_VETERINARIO, DS_EMAIL, DS_SENHA_HASH, TP_PERFIL, ST_ATIVA)
SELECT c.ID_CLINICA,
       NULL,
       c.DS_EMAIL_ACESSO,
       c.DS_SENHA_HASH,
       'GESTOR',
       c.ST_ATIVA
  FROM CLINICA c
 WHERE c.DS_EMAIL_ACESSO IS NOT NULL
   AND c.DS_SENHA_HASH   IS NOT NULL
   AND NOT EXISTS (SELECT 1
                     FROM USUARIO_CLINICA u
                    WHERE u.ID_CLINICA = c.ID_CLINICA
                      AND u.DS_EMAIL   = c.DS_EMAIL_ACESSO);
-- <<< END CONVERSAO_D10
