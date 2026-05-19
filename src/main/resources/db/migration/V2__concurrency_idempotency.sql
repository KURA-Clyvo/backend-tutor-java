-- =============================================================================
-- V2__concurrency_idempotency.sql
-- Reforço de concorrência: índice de limpeza em IDEMPOTENCY_KEY.
-- Referência: §3.4 do plano v5.
-- =============================================================================

-- Índice para job de limpeza TTL (query: WHERE DT_CRIACAO < :limiar)
CREATE INDEX IDX_IDEMPOT_CRIACAO ON IDEMPOTENCY_KEY(DT_CRIACAO);

COMMENT ON TABLE IDEMPOTENCY_KEY IS
    'Exactly-once para POSTs sensíveis (CONSENTIMENTO). TTL 24h. Limpeza via job agendado — IDX_IDEMPOT_CRIACAO otimiza o DELETE em lote.';
