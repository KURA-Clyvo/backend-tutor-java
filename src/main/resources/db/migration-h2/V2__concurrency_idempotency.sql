-- =============================================================================
-- V2__concurrency_idempotency.sql (variante H2)
-- Equivalente funcional de db/migration/oracle/V2__concurrency_idempotency.sql.
-- O H2 in-memory de dev é sempre recriado do zero a cada boot, então os checks
-- defensivos de "já existe?" do bloco PL/SQL Oracle são desnecessários aqui —
-- DDL direto. Ver docs/INT-01-contract-map.md (fix do blocker H2/Flyway).
-- =============================================================================

CREATE INDEX IDX_IDEMPOT_CRIACAO ON IDEMPOTENCY_KEY(DT_CRIACAO);

COMMENT ON TABLE IDEMPOTENCY_KEY IS
    'Exactly-once para POSTs sensíveis (CONSENTIMENTO). TTL 24h. Limpeza via job agendado — IDX_IDEMPOT_CRIACAO otimiza o DELETE em lote.';
