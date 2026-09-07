-- =============================================================================
-- V20__web_usuario_suporte.sql
-- KURA-WEB — camada de visualização servidor (Sprint 3, Java Advanced).
-- Ver docs/ADR-web.md. Tabela nasce e morre com a branch `web-rubrica`
-- (ruling D-J4, KURA_BACKLOG_SPRINT3_JAVA.md §1.1) — o número V20 NÃO é
-- queimado em `main`, porque esta branch nunca mescla.
--
-- WEB_USUARIO_SUPORTE guarda o segundo perfil exigido pela rubrica de
-- Spring Security (SUPORTE, ao lado de TUTOR). Prefixo WEB_ de propósito
-- (§0.5 D-J1 do backlog): não é USUARIO_CLINICA (rejeitada — é .NET-owned,
-- contexto B2B) nem CONTA_TUTOR (é o outro perfil). Zero FK saindo para
-- tabela de produto (§3.1.4 da dívida acadêmica): FK de saída transformaria
-- o DROP desta tabela, na demolição pós-oral, em negociação com o schema de
-- produto — o objetivo aqui é o oposto, um `DROP TABLE` de uma linha só.
--
-- ARQUIVO ÚNICO, SEM SPLIT -oracle/-h2 — MEDIDO, NÃO PRESUMIDO.
-- A V13 (LOG_ERRO) já provou que `NUMBER(n) DEFAULT SEQ_x.NEXTVAL NOT NULL`
-- dentro de um CREATE TABLE roda sem ajuste no H2 2.2.224 MODE=Oracle desta
-- suíte, e a V16 provou o mesmo para `ALTER TABLE ... MODIFY`. Esta migration
-- usa exatamente o mesmo padrão (CREATE SEQUENCE + CREATE TABLE com
-- DEFAULT SEQ_x.NEXTVAL, CHAR(1) para booleano, VARCHAR2 para texto — nenhuma
-- sintaxe nova em relação à V13). A prova empírica é a suíte completa rodando
-- Flyway do profile dev sobre este arquivo único: se
-- `./mvnw test -Dspring.profiles.active=dev` continuar verde, o H2 aceitou a
-- sintaxe Oracle sem split — registrado literalmente no artefato desta task
-- (sj3-03-report.md), não deduzido do DDL.
--
-- PK POR SEQUENCE, NÃO IDENTITY — mesma convenção .NET-owned/Java-owned do
-- resto do schema não se aplica aqui (esta tabela não é escrita por nenhum
-- backend fora deste): a escolha é só por consistência com o padrão já
-- estabelecido em LOG_ERRO (V13) para tabela nova de infraestrutura.
--
-- ST_ATIVA CHAR(1) 'S'/'N' — convenção deste banco (ver V1, todas as demais
-- tabelas). DS_SENHA_HASH guarda hash BCrypt (60 chars reais; 256 é a mesma
-- folga usada em CONTA_TUTOR.DS_SENHA_HASH e USUARIO_CLINICA.DS_SENHA_HASH).
-- =============================================================================

CREATE SEQUENCE SEQ_WEB_USUARIO_SUPORTE START WITH 100 INCREMENT BY 1;

CREATE TABLE WEB_USUARIO_SUPORTE (
    ID              NUMBER(15)    DEFAULT SEQ_WEB_USUARIO_SUPORTE.NEXTVAL NOT NULL,
    DS_LOGIN        VARCHAR2(120) NOT NULL,
    DS_SENHA_HASH   VARCHAR2(256) NOT NULL,
    ST_ATIVA        CHAR(1)       DEFAULT 'S' NOT NULL,
    CONSTRAINT PK_WEB_USUARIO_SUPORTE PRIMARY KEY (ID),
    CONSTRAINT UK_WEB_USUARIO_SUPORTE_LOGIN UNIQUE (DS_LOGIN),
    CONSTRAINT CHK_WEB_USUARIO_SUPORTE_ATIVA CHECK (ST_ATIVA IN ('S', 'N'))
);

COMMENT ON TABLE WEB_USUARIO_SUPORTE IS 'KURA-WEB — usuário do perfil SUPORTE do painel Thymeleaf descartável (Sprint 3 Java Advanced, branch web-rubrica). Sem FK de/para tabela de produto de propósito. Ver docs/ADR-web.md.';
