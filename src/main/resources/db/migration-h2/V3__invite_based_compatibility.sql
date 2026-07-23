-- =============================================================================
-- V3__invite_based_compatibility.sql (variante H2)
-- Equivalente funcional de db/migration/oracle/V3__invite_based_compatibility.sql.
-- Sem checks defensivos — ver nota em h2/V2__concurrency_idempotency.sql.
-- =============================================================================

ALTER TABLE CONTA_TUTOR ADD CONSTRAINT UK_CONTA_INVITE_USED UNIQUE (ID_INVITE_USADO);

CREATE INDEX IDX_INVITE_TOKEN_ATIVO ON INVITE_TUTOR (NR_TOKEN, ST_UTILIZADO, ST_ATIVO);

COMMENT ON COLUMN CONTA_TUTOR.ID_INVITE_USADO IS
    'UK_CONTA_INVITE_USED: race condition → ORA-00001 → DataIntegrityViolationException → HTTP 409.';
