-- =============================================================================
-- V21__vacinas_v2_notificacao_triagem_luna.sql
-- LU-02 (KURA_BACKLOG_LUNA_AI). Contrato de dados da Luna — um único número
-- Flyway, três grupos de DDL.
--
-- FIX WAVE 1 (G2 REPROVOU a versão original deste arquivo — achados 1-5,
-- lu-02-revisao.md). Este cabeçalho já descreve a view CORRIGIDA; não há
-- registro do "antes" aqui — o histórico do achado está no ledger.
--
-- 1) VW_VACINAS_VENCENDO v2 (CREATE OR REPLACE). As 7 colunas atuais
--    permanecem com o mesmo nome (TutorBffController:125 /
--    VacinaVencendo.java leem por nome de coluna; ddl-auto é `none` nos dois
--    profiles — não há validação de schema no boot em nenhum ambiente,
--    achado 5). Acrescenta: NM_TUTOR, DS_WHATSAPP, DIAS_RESTANTES, ID_VACINA
--    (nulo quando a origem é AGENDAMENTO), DS_ORIGEM ('VACINA'|'AGENDAMENTO'),
--    ST_CONSENTE_LEMBRETE ('S'|'N', D-L4).
--
--    Fonte = UNION ALL de:
--      (a) aplicação real de vacina: VACINA.DT_PROXIMA_DOSE, via
--          EVENTO_CLINICO → PET → TUTOR_PET → TUTOR (G0/N2 — hoje a view só
--          enxerga AGENDAMENTO, nunca VACINA). Fan-out por tutor: pet com 2
--          tutores gera 2 linhas (cada tutor é avisado — decisão do brief
--          LU-02, correto por design). O endpoint Java pet-scoped
--          (TimelineService.listarVacinasPet/statusVacinasPet) deduplica essas
--          2 linhas por (ID_PET,NM_VACINA,DT_PROXIMA_DOSE) — achado 1/G2,
--          corrigido no Java, não na view (a Luna precisa das 2 linhas).
--      (b) AGENDAMENTO (DS_TIPO='VACINA') — **DIFERENÇAS EXPLÍCITAS em
--          relação à v1/V6** (achado 2/G2 — o comentário original desta
--          migration alegava "preservada tal qual V6", o que era falso):
--            • v1/V6 não fazia JOIN em TUTOR nenhum (só PET/CLINICA); a
--              primeira versão desta V21 introduziu INNER JOIN TUTOR, e como
--              AGENDAMENTO.ID_TUTOR é NULLABLE (V1:269) isso derrubava o
--              agendamento de vacina sem tutor. Corrigido para LEFT JOIN
--              TUTOR: o agendamento sem tutor CONTINUA aparecendo
--              (NM_TUTOR/DS_WHATSAPP nulos, ST_CONSENTE_LEMBRETE='N' —
--              ninguém para consentir).
--            • v1 não excluía tutor inativo (não tinha JOIN TUTOR); esta V21
--              exclui (pedido explícito do backlog LU-02) — SÓ quando existe
--              tutor: `t.ID_TUTOR IS NULL OR t.ST_ATIVO='S'`.
--            • v1 não excluía pet inativo; esta V21 exclui (pedido do
--              backlog) — sem mudança nesta correção.
--          1 linha por agendamento — AGENDAMENTO.ID_TUTOR já é o tutor único
--          que agendou (ou nulo), sem fan-out via TUTOR_PET aqui.
--
--    Janela e DIAS_RESTANTES — RELÓGIO ÚNICO (achado 4/G2, corrigido):
--    a versão original comparava a janela por INSTANTE
--    (`>= CURRENT_TIMESTAMP`, fuso da sessão) e calculava DIAS_RESTANTES por
--    `TRUNC(dt) - TRUNC(SYSDATE)` (SYSDATE = fuso do SO do banco, UTC no
--    container) — dois relógios diferentes na mesma view, e dose gravada à
--    meia-noite do próprio dia saía da janela antes de DIAS_RESTANTES=0 ser
--    alcançável. Corrigido: uma ÚNICA referência de "hoje" — data civil em
--    America/Sao_Paulo, `TRUNC(CAST(SYSTIMESTAMP AT TIME ZONE
--    'America/Sao_Paulo' AS TIMESTAMP))` — usada tanto na janela quanto em
--    DIAS_RESTANTES, e a janela agora compara DATA (TRUNC), não instante:
--    `TRUNC(DT_PROXIMA_DOSE) >= HOJE AND TRUNC(DT_PROXIMA_DOSE) <= HOJE+30`.
--    Efeito: dose de hoje (qualquer hora) aparece com DIAS_RESTANTES=0; dose
--    de ontem não aparece; dose em hoje+30 às 23:30 aparece com
--    DIAS_RESTANTES=30 (antes sumia, por comparar instante em vez de data).
--    Exclui pet inativo (PET.ST_ATIVO <> 'S') e tutor soft-deletado
--    (TUTOR.ST_ATIVO <> 'S', só quando há tutor) — nomes de coluna
--    confirmados em V1 (não existe "deleted_at"; o padrão do schema é
--    ST_ATIVO CHAR(1) S/N).
--    NM_VACINA sai como VARCHAR2(200) nos dois lados do UNION (era
--    VARCHAR2(30), vindo de AGENDAMENTO.DS_TIPO — G0 flagou que
--    VACINA.NM_VACINA é VARCHAR2(200); widening em vez de truncar).
--
--    ST_CONSENTE_LEMBRETE (D-L4) — DESEMPATE CORRIGIDO (achado 3/G2): usa o
--    registro mais recente de CONSENTIMENTO(ID_TUTOR, DS_TIPO='LEMBRETES')
--    por DT_ACEITE (pode haver mais de um por tutor/tipo — histórico
--    insert-only). Em caso de EMPATE de DT_ACEITE entre um aceite e uma
--    revogação (mesmo instante — raro, mas possível), a versão original
--    resolvia com MAX(CASE...'S'...) — 'S' > 'N' lexicamente, então o empate
--    favorecia o ENVIO. Errado para LGPD: corrigido para MIN(CASE...), que
--    escolhe 'N' no empate — a revogação vence. 'S' só se, dentre os
--    registros empatados no DT_ACEITE mais recente, TODOS tiverem
--    ST_ACEITO='S' AND DT_REVOGACAO IS NULL; qualquer um deles sendo
--    recusa/revogação já derruba para 'N'. Sem tutor (ramo AGENDAMENTO com
--    ID_TUTOR nulo) ou sem nenhum consentimento LEMBRETES → 'N' (COALESCE).
--
-- 2) NOTIFICACAO (.NET owns, criada em V9) ganha 6 colunas NULAS de envio
--    (G0/N1 — a Luna hoje grava em colunas que não existem: ORA-00904):
--    ID_PET, DS_CANAL VARCHAR2(20), DS_TIPO VARCHAR2(40),
--    ST_ENVIO VARCHAR2(20) com CHECK IN ('PENDENTE','ENVIADA','FALHA'),
--    DT_ENVIO TIMESTAMP, DS_ERRO_ENVIO VARCHAR2(500). Todas nulas — nem o
--    .NET (dono) nem o Java (leitor) quebram. Índice de idempotência
--    (ID_TUTOR, ID_PET, DS_TIPO, DT_CRIACAO).
--
-- 3) TRIAGEM_LUNA (.NET owns, criada em V9) ganha 3 colunas NULAS
--    estruturadas (G0/E4): NR_SCORE NUMBER(5), DS_SINTOMAS VARCHAR2(1000),
--    DS_REGRAS_VERSAO VARCHAR2(10). DS_DESCRICAO (texto livre) continua
--    sendo escrita — compatibilidade.
--
-- Fora de escopo (LU-02): tocar VacinaService/Notificacao.cs no .NET (EF
-- ignora coluna não mapeada) e mapear as colunas novas em entidades Java
-- (nenhum endpoint Java precisa lê-las hoje).
--
-- Por que este arquivo mora em db/migration-oracle/ e não em db/migration/:
-- só a view diverge de sintaxe entre os dois motores (Oracle usa
-- SYSTIMESTAMP AT TIME ZONE / TRUNC; H2 MODE=Oracle usa CURRENT_DATE —
-- reflete o fuso da JVM, não tem AT TIME ZONE com região IANA confiável — e
-- DATEDIFF). Os grupos 2 e 3 seriam portáveis sozinhos (ALTER TABLE ADD de
-- uma coluna por vez + CHECK, já provado portável em V7/V8/V11/V16), mas
-- nenhuma versão pode conviver em db/migration/ e nos irmãos ao mesmo tempo
-- (mesmo motivo de V2/V3/V5/V12/V15/V17/V18) — por isso o arquivo inteiro é
-- duplicado, idêntico nos grupos 2/3, entre esta pasta e db/migration-h2/. A
-- variante H2 está em db/migration-h2/V21__vacinas_v2_notificacao_triagem_luna.sql.
-- =============================================================================

CREATE OR REPLACE VIEW VW_VACINAS_VENCENDO AS
SELECT
    p.ID_PET,
    p.NM_PET,
    tp.ID_TUTOR,
    CAST(v.NM_VACINA AS VARCHAR2(200))            AS NM_VACINA,
    v.DT_PROXIMA_DOSE,
    ec.ID_CLINICA,
    cl.NM_CLINICA,
    t.NM_TUTOR,
    t.DS_WHATSAPP,
    TRUNC(v.DT_PROXIMA_DOSE)
        - TRUNC(CAST(SYSTIMESTAMP AT TIME ZONE 'America/Sao_Paulo' AS TIMESTAMP))
                                                   AS DIAS_RESTANTES,
    v.ID_VACINA,
    CAST('VACINA' AS VARCHAR2(20))                AS DS_ORIGEM,
    COALESCE((
        SELECT MIN(CASE WHEN c.ST_ACEITO = 'S' AND c.DT_REVOGACAO IS NULL THEN 'S' ELSE 'N' END)
        FROM   CONSENTIMENTO c
        WHERE  c.ID_TUTOR = t.ID_TUTOR
          AND  c.DS_TIPO  = 'LEMBRETES'
          AND  c.DT_ACEITE = (
                   SELECT MAX(c2.DT_ACEITE)
                   FROM   CONSENTIMENTO c2
                   WHERE  c2.ID_TUTOR = t.ID_TUTOR
                     AND  c2.DS_TIPO  = 'LEMBRETES'
               )
    ), 'N')                                       AS ST_CONSENTE_LEMBRETE
FROM   VACINA         v
JOIN   EVENTO_CLINICO ec ON ec.ID_EVENTO  = v.ID_EVENTO_CLINICO
JOIN   PET             p ON p.ID_PET      = ec.ID_PET
JOIN   TUTOR_PET       tp ON tp.ID_PET    = p.ID_PET
JOIN   TUTOR            t ON t.ID_TUTOR   = tp.ID_TUTOR
JOIN   CLINICA         cl ON cl.ID_CLINICA = ec.ID_CLINICA
WHERE  p.ST_ATIVO = 'S'
  AND  t.ST_ATIVO = 'S'
  AND  v.DT_PROXIMA_DOSE IS NOT NULL
  AND  TRUNC(v.DT_PROXIMA_DOSE) >= TRUNC(CAST(SYSTIMESTAMP AT TIME ZONE 'America/Sao_Paulo' AS TIMESTAMP))
  AND  TRUNC(v.DT_PROXIMA_DOSE) <= TRUNC(CAST(SYSTIMESTAMP AT TIME ZONE 'America/Sao_Paulo' AS TIMESTAMP)) + 30

UNION ALL

SELECT
    p.ID_PET,
    p.NM_PET,
    a.ID_TUTOR,
    CAST(a.DS_TIPO AS VARCHAR2(200))              AS NM_VACINA,
    a.DT_AGENDAMENTO                              AS DT_PROXIMA_DOSE,
    a.ID_CLINICA,
    cl.NM_CLINICA,
    t.NM_TUTOR,
    t.DS_WHATSAPP,
    TRUNC(a.DT_AGENDAMENTO)
        - TRUNC(CAST(SYSTIMESTAMP AT TIME ZONE 'America/Sao_Paulo' AS TIMESTAMP))
                                                   AS DIAS_RESTANTES,
    CAST(NULL AS NUMBER(10))                      AS ID_VACINA,
    CAST('AGENDAMENTO' AS VARCHAR2(20))           AS DS_ORIGEM,
    COALESCE((
        SELECT MIN(CASE WHEN c.ST_ACEITO = 'S' AND c.DT_REVOGACAO IS NULL THEN 'S' ELSE 'N' END)
        FROM   CONSENTIMENTO c
        WHERE  c.ID_TUTOR = t.ID_TUTOR
          AND  c.DS_TIPO  = 'LEMBRETES'
          AND  c.DT_ACEITE = (
                   SELECT MAX(c2.DT_ACEITE)
                   FROM   CONSENTIMENTO c2
                   WHERE  c2.ID_TUTOR = t.ID_TUTOR
                     AND  c2.DS_TIPO  = 'LEMBRETES'
               )
    ), 'N')                                       AS ST_CONSENTE_LEMBRETE
FROM   AGENDAMENTO a
JOIN   PET         p  ON p.ID_PET      = a.ID_PET
JOIN   CLINICA     cl ON cl.ID_CLINICA = a.ID_CLINICA
LEFT JOIN TUTOR    t  ON t.ID_TUTOR    = a.ID_TUTOR
WHERE  a.DS_TIPO    = 'VACINA'
  AND  a.ST_STATUS NOT IN ('CANCELADO','REALIZADO')
  AND  p.ST_ATIVO   = 'S'
  AND  (t.ID_TUTOR IS NULL OR t.ST_ATIVO = 'S')
  AND  TRUNC(a.DT_AGENDAMENTO) >= TRUNC(CAST(SYSTIMESTAMP AT TIME ZONE 'America/Sao_Paulo' AS TIMESTAMP))
  AND  TRUNC(a.DT_AGENDAMENTO) <= TRUNC(CAST(SYSTIMESTAMP AT TIME ZONE 'America/Sao_Paulo' AS TIMESTAMP)) + 30;

COMMENT ON TABLE VW_VACINAS_VENCENDO IS
    'v2 (V21/LU-02, fix wave 1). UNION ALL de aplicação real (VACINA, via EVENTO_CLINICO/TUTOR_PET, fan-out por tutor) + agendamento futuro (AGENDAMENTO, LEFT JOIN TUTOR — agendamento sem tutor continua visível). DIAS_RESTANTES e janela usam a MESMA referencia de calendario (TRUNC, America/Sao_Paulo). Empate de DT_ACEITE em ST_CONSENTE_LEMBRETE favorece revogacao (MIN). Exclui pet inativo / tutor soft-deletado (quando ha tutor).';

-- ─── Grupo 2: NOTIFICACAO — colunas nulas de envio (.NET owns, G0/N1) ────────
ALTER TABLE NOTIFICACAO ADD ID_PET NUMBER(10);
ALTER TABLE NOTIFICACAO ADD CONSTRAINT FK_NOTIF_PET FOREIGN KEY (ID_PET) REFERENCES PET(ID_PET);
ALTER TABLE NOTIFICACAO ADD DS_CANAL VARCHAR2(20);
ALTER TABLE NOTIFICACAO ADD DS_TIPO VARCHAR2(40);
ALTER TABLE NOTIFICACAO ADD ST_ENVIO VARCHAR2(20);
ALTER TABLE NOTIFICACAO ADD CONSTRAINT CHK_NOTIF_ST_ENVIO CHECK (ST_ENVIO IN ('PENDENTE','ENVIADA','FALHA') OR ST_ENVIO IS NULL);
ALTER TABLE NOTIFICACAO ADD DT_ENVIO TIMESTAMP;
ALTER TABLE NOTIFICACAO ADD DS_ERRO_ENVIO VARCHAR2(500);

CREATE INDEX IDX_NOTIF_TUTOR_PET_TIPO_DT ON NOTIFICACAO(ID_TUTOR, ID_PET, DS_TIPO, DT_CRIACAO);

COMMENT ON COLUMN NOTIFICACAO.ID_PET        IS 'Pet relacionado à notificação (ex.: lembrete de vacina). Nulo para notificações não vinculadas a um pet.';
COMMENT ON COLUMN NOTIFICACAO.DS_CANAL      IS 'Canal de envio (ex.: WHATSAPP, EMAIL, PUSH) — livre, sem CHECK (G0/N1).';
COMMENT ON COLUMN NOTIFICACAO.DS_TIPO       IS 'Tipo lógico da notificação (ex.: LEMBRETE_VACINA) — compõe a chave de idempotência com ID_TUTOR/ID_PET/DT_CRIACAO.';
COMMENT ON COLUMN NOTIFICACAO.ST_ENVIO      IS 'PENDENTE|ENVIADA|FALHA — nulo até a Luna processar o envio.';
COMMENT ON COLUMN NOTIFICACAO.DT_ENVIO      IS 'Timestamp do envio efetivo (ex.: Twilio/WhatsApp).';
COMMENT ON COLUMN NOTIFICACAO.DS_ERRO_ENVIO IS 'Mensagem de erro sanitizada (LGPD: nunca telefone/conteúdo/transcrição) quando ST_ENVIO=FALHA.';

-- ─── Grupo 3: TRIAGEM_LUNA — colunas estruturadas (.NET owns, G0/E4) ─────────
ALTER TABLE TRIAGEM_LUNA ADD NR_SCORE NUMBER(5);
ALTER TABLE TRIAGEM_LUNA ADD DS_SINTOMAS VARCHAR2(1000);
ALTER TABLE TRIAGEM_LUNA ADD DS_REGRAS_VERSAO VARCHAR2(10);

COMMENT ON COLUMN TRIAGEM_LUNA.NR_SCORE         IS 'Score numérico de urgência calculado pela heurística de regras da Luna.';
COMMENT ON COLUMN TRIAGEM_LUNA.DS_SINTOMAS      IS 'Sintomas relatados/estruturados que geraram o score — não substitui DS_DESCRICAO (compatibilidade).';
COMMENT ON COLUMN TRIAGEM_LUNA.DS_REGRAS_VERSAO IS 'Versão do conjunto de regras/heurística aplicado — rastreabilidade (IA sugere, veterinário decide).';
