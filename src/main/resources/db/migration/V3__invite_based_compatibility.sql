-- =============================================================================
-- V3__invite_based_compatibility.sql
-- Garante UK_CONTA_INVITE_USED e índice de busca por token.
-- Referência: §3.5 do plano v5.
--
-- NOTA ARQUITETURAL — Oracle FIAP (schema parcial):
-- Em ambientes onde o schema foi criado parcialmente antes do Flyway,
-- execute o bloco PL/SQL abaixo manualmente ANTES da primeira run do Flyway,
-- ou use `flyway.baselineOnMigrate=true` + `flyway.baselineVersion=2`:
--
--   DECLARE v NUMBER; BEGIN
--     SELECT COUNT(*) INTO v FROM USER_CONSTRAINTS
--       WHERE CONSTRAINT_NAME = 'UK_CONTA_INVITE_USED';
--     IF v = 0 THEN
--       EXECUTE IMMEDIATE
--         'ALTER TABLE CONTA_TUTOR ADD CONSTRAINT UK_CONTA_INVITE_USED UNIQUE (ID_INVITE_USADO)';
--     END IF;
--   END;
--   /
--   DECLARE v NUMBER; BEGIN
--     SELECT COUNT(*) INTO v FROM USER_INDEXES
--       WHERE INDEX_NAME = 'IDX_INVITE_TOKEN_ATIVO';
--     IF v = 0 THEN
--       EXECUTE IMMEDIATE
--         'CREATE INDEX IDX_INVITE_TOKEN_ATIVO ON INVITE_TUTOR (NR_TOKEN, ST_UTILIZADO, ST_ATIVO)';
--     END IF;
--   END;
--   /
--
-- Em dev (H2 Oracle mode) e em ambientes Flyway puro, as instruções DDL abaixo
-- são suficientes — Flyway garante execução exatamente uma vez.
-- =============================================================================

-- Defense-in-depth: UK garante anti-reuso mesmo sob race condition.
-- (OnboardingService verifica ST_UTILIZADO='N' na app; banco bloqueia o segundo INSERT.)
ALTER TABLE CONTA_TUTOR
    ADD CONSTRAINT UK_CONTA_INVITE_USED UNIQUE (ID_INVITE_USADO);

-- Índice composto para findByNrToken filtrando ativos + não utilizados
-- (query: WHERE NR_TOKEN = ? AND ST_UTILIZADO = 'N' AND ST_ATIVO = 'S')
CREATE INDEX IDX_INVITE_TOKEN_ATIVO
    ON INVITE_TUTOR (NR_TOKEN, ST_UTILIZADO, ST_ATIVO);

COMMENT ON COLUMN CONTA_TUTOR.ID_INVITE_USADO IS
    'UK_CONTA_INVITE_USED: race condition → ORA-00001 → DataIntegrityViolationException → HTTP 409.';
