-- =============================================================================
-- afterMigrate__seeds_dev.sql  — Flyway SQL Callback
-- Executa APÓS cada run de migration — somente em dev.
-- Por que só em dev: o profile dev inclui classpath:db/callback em
--   spring.flyway.locations; prod usa apenas classpath:db/migration.
--
-- IDEMPOTENTE: todo INSERT usa MERGE com ON (PK) WHEN NOT MATCHED,
-- garantindo que execuções repetidas não dupliquem dados.
-- =============================================================================

-- ─── 1. CLINICA ───────────────────────────────────────────────────────────────
MERGE INTO CLINICA t
USING (SELECT 1 FROM DUAL) SRC ON (t.ID_CLINICA = 1)
WHEN NOT MATCHED THEN INSERT (
    ID_CLINICA, NM_CLINICA, NR_CNPJ, DS_ENDERECO,
    NM_CIDADE, SG_UF, NR_CEP, DS_TELEFONE,
    DS_EMAIL, DT_CADASTRO, ST_ATIVA
) VALUES (
    1, 'Clyvo Vet São Paulo', '12345678000190',
    'Av. Paulista, 1000', 'São Paulo', 'SP', '01310100',
    '1130001000', 'contato@clyvovet.com.br',
    CURRENT_TIMESTAMP, 'S'
);

-- ─── 2. ESPECIE ───────────────────────────────────────────────────────────────
MERGE INTO ESPECIE t
USING (SELECT 1 FROM DUAL) SRC ON (t.ID_ESPECIE = 1)
WHEN NOT MATCHED THEN INSERT (ID_ESPECIE, NM_ESPECIE)
VALUES (1, 'Cao');

MERGE INTO ESPECIE t
USING (SELECT 1 FROM DUAL) SRC ON (t.ID_ESPECIE = 2)
WHEN NOT MATCHED THEN INSERT (ID_ESPECIE, NM_ESPECIE)
VALUES (2, 'Gato');

-- ─── 3. TIPO_EVENTO ───────────────────────────────────────────────────────────
MERGE INTO TIPO_EVENTO t
USING (SELECT 1 FROM DUAL) SRC ON (t.ID_TIPO_EVENTO = 1)
WHEN NOT MATCHED THEN INSERT (ID_TIPO_EVENTO, NM_TIPO, DS_TIPO, ST_ATIVO)
VALUES (1, 'CONSULTA', 'Consulta veterinária presencial', 'S');

MERGE INTO TIPO_EVENTO t
USING (SELECT 1 FROM DUAL) SRC ON (t.ID_TIPO_EVENTO = 2)
WHEN NOT MATCHED THEN INSERT (ID_TIPO_EVENTO, NM_TIPO, DS_TIPO, ST_ATIVO)
VALUES (2, 'TELEORIENTACAO', 'Orientação veterinária via videochamada', 'S');

MERGE INTO TIPO_EVENTO t
USING (SELECT 1 FROM DUAL) SRC ON (t.ID_TIPO_EVENTO = 3)
WHEN NOT MATCHED THEN INSERT (ID_TIPO_EVENTO, NM_TIPO, DS_TIPO, ST_ATIVO)
VALUES (3, 'VACINA', 'Aplicação de vacina', 'S');

-- ─── 4. RACA ──────────────────────────────────────────────────────────────────
MERGE INTO RACA t
USING (SELECT 1 FROM DUAL) SRC ON (t.ID_RACA = 1)
WHEN NOT MATCHED THEN INSERT (ID_RACA, ID_ESPECIE, NM_RACA, DS_PREDISPOSICAO)
VALUES (1, 1, 'Labrador', 'Predisposição a displasia coxofemoral e obesidade.');

MERGE INTO RACA t
USING (SELECT 1 FROM DUAL) SRC ON (t.ID_RACA = 2)
WHEN NOT MATCHED THEN INSERT (ID_RACA, ID_ESPECIE, NM_RACA, DS_PREDISPOSICAO)
VALUES (2, 1, 'Poodle', 'Predisposição a problemas dentários e otite.');

MERGE INTO RACA t
USING (SELECT 1 FROM DUAL) SRC ON (t.ID_RACA = 3)
WHEN NOT MATCHED THEN INSERT (ID_RACA, ID_ESPECIE, NM_RACA, DS_PREDISPOSICAO)
VALUES (3, 2, 'Siames', 'Predisposição a problemas renais e respiratórios.');

MERGE INTO RACA t
USING (SELECT 1 FROM DUAL) SRC ON (t.ID_RACA = 4)
WHEN NOT MATCHED THEN INSERT (ID_RACA, ID_ESPECIE, NM_RACA, DS_PREDISPOSICAO)
VALUES (4, 2, 'SRD-felino', 'Sem raça definida — felino. Boa resistência geral.');

-- ─── 5. VETERINARIO ───────────────────────────────────────────────────────────
MERGE INTO VETERINARIO t
USING (SELECT 1 FROM DUAL) SRC ON (t.ID_VETERINARIO = 1)
WHEN NOT MATCHED THEN INSERT (
    ID_VETERINARIO, ID_CLINICA, NM_VETERINARIO,
    NR_CRMV, DS_EMAIL, NR_TELEFONE, ST_ATIVO
) VALUES (
    1, 1, 'Dr. Carlos Medeiro',
    'SP-12345', 'carlos.medeiro@clyvovet.com.br', '11912345678', 'S'
);

-- ─── 6. TUTOR (ativo com aviso de privacidade aceito) ─────────────────────────
-- NR_CPF: 11 dígitos sem máscara (constraint schema v4)
-- ST_AVISO_PRIVACIDADE='S': pré-requisito para o fluxo register-invite funcionar
MERGE INTO TUTOR t
USING (SELECT 1 FROM DUAL) SRC ON (t.ID_TUTOR = 1)
WHEN NOT MATCHED THEN INSERT (
    ID_TUTOR, ID_CLINICA, NM_TUTOR, NR_CPF, DS_EMAIL,
    DS_TELEFONE, DS_WHATSAPP, DT_CADASTRO,
    ST_AVISO_PRIVACIDADE, DT_AVISO_PRIVACIDADE, DS_VERSAO_AVISO, ST_ATIVO
) VALUES (
    1, 1, 'Felipe Ferrete', '12345678900', 'felipe@clyvo.vet',
    '11999990001', '11999990001', CURRENT_TIMESTAMP,
    'S', CURRENT_TIMESTAMP, 'v1.0', 'S'
);

-- ─── 7. INVITE_TUTOR (válido por 7 dias) ──────────────────────────────────────
-- Token seed: usado pelo teste de integração POST /auth/register-invite
-- ST_UTILIZADO='N' + ST_ATIVO='S' + DT_EXPIRACAO futura = invite válido
MERGE INTO INVITE_TUTOR t
USING (SELECT 1 FROM DUAL) SRC ON (t.ID_INVITE = 1)
WHEN NOT MATCHED THEN INSERT (
    ID_INVITE, ID_TUTOR, NR_TOKEN, DT_EXPIRACAO,
    DS_CANAL, ST_UTILIZADO, ST_ATIVO
) VALUES (
    1, 1,
    '550e8400-e29b-41d4-a716-446655440000',
    CURRENT_TIMESTAMP + INTERVAL '7' DAY,
    'WHATSAPP', 'N', 'S'
);

-- ─── 8. PET ───────────────────────────────────────────────────────────────────
MERGE INTO PET t
USING (SELECT 1 FROM DUAL) SRC ON (t.ID_PET = 1)
WHEN NOT MATCHED THEN INSERT (
    ID_PET, ID_CLINICA, ID_ESPECIE, ID_RACA, ID_VETERINARIO_RESP,
    NM_PET, DT_NASCIMENTO, SG_SEXO, SG_PORTE,
    NR_PESO_KG, ST_CASTRADO, ST_ATIVO, DT_CADASTRO
) VALUES (
    1, 1, 1, 1, 1,
    'Marley', DATE '2022-03-15', 'M', 'G',
    32.50, 'S', 'S', CURRENT_TIMESTAMP
);

-- ─── 9. TUTOR_PET ─────────────────────────────────────────────────────────────
MERGE INTO TUTOR_PET t
USING (SELECT 1 FROM DUAL) SRC ON (t.ID_TUTOR = 1 AND t.ID_PET = 1)
WHEN NOT MATCHED THEN INSERT (
    ID_TUTOR, ID_PET, DS_VINCULO, DT_VINCULO, ST_PRINCIPAL
) VALUES (
    1, 1, 'PROPRIETARIO', CURRENT_TIMESTAMP, 'S'
);

-- ─── 10. AGENDAMENTO (futuro — 7 dias à frente) ───────────────────────────────
-- NR_VERSION=0 (DEFAULT — não precisa ser informado explicitamente)
MERGE INTO AGENDAMENTO t
USING (SELECT 1 FROM DUAL) SRC ON (t.ID_AGENDAMENTO = 1)
WHEN NOT MATCHED THEN INSERT (
    ID_AGENDAMENTO, ID_CLINICA, ID_TUTOR, ID_PET, ID_VETERINARIO,
    DT_AGENDAMENTO, NR_DURACAO_MINUTOS, DS_TIPO,
    ST_STATUS, DS_ORIGEM, NR_VERSION
) VALUES (
    1, 1, 1, 1, 1,
    CURRENT_TIMESTAMP + INTERVAL '7' DAY,
    30, 'CONSULTA',
    'AGENDADO', 'PORTAL', 0
);
