-- =============================================================================
-- V21__vacinas_v2_notificacao_triagem_luna.sql
-- LU-02 (KURA_BACKLOG_LUNA_AI). Contrato de dados da Luna — um único número
-- Flyway, três grupos de DDL:
--
-- 1) VW_VACINAS_VENCENDO v2 (CREATE OR REPLACE). As 7 colunas atuais
--    permanecem com o mesmo nome (TutorBffController:125 /
--    VacinaVencendo.java leem por nome de coluna, ddl-auto=validate em prod
--    ignora coluna extra não mapeada). Acrescenta: NM_TUTOR, DS_WHATSAPP,
--    DIAS_RESTANTES, ID_VACINA (nulo quando a origem é AGENDAMENTO),
--    DS_ORIGEM ('VACINA'|'AGENDAMENTO'), ST_CONSENTE_LEMBRETE ('S'|'N', D-L4).
--
--    Fonte = UNION ALL de:
--      (a) aplicação real de vacina: VACINA.DT_PROXIMA_DOSE, via
--          EVENTO_CLINICO → PET → TUTOR_PET → TUTOR (G0/N2 — hoje a view só
--          enxerga AGENDAMENTO, nunca VACINA). Fan-out por tutor: pet com 2
--          tutores gera 2 linhas (cada tutor é avisado — decisão do brief
--          LU-02, correto por design).
--      (b) a regra atual sobre AGENDAMENTO (DS_TIPO='VACINA', preservada tal
--          qual V6): 1 linha por agendamento — AGENDAMENTO.ID_TUTOR já é o
--          tutor único que agendou, sem fan-out via TUTOR_PET aqui.
--    Janela nos dois ramos: DT_PROXIMA_DOSE/DT_AGENDAMENTO em
--    [CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + 30 dias] — igual à V6.
--    DIAS_RESTANTES = diferença em dias de CALENDÁRIO (TRUNC de data),
--    não em horas — TRUNC(dt) - TRUNC(SYSDATE).
--    Exclui pet inativo (PET.ST_ATIVO <> 'S') e tutor soft-deletado
--    (TUTOR.ST_ATIVO <> 'S') — nomes de coluna confirmados em V1
--    (não existe "deleted_at"; o padrão do schema é ST_ATIVO CHAR(1) S/N).
--    NM_VACINA sai como VARCHAR2(200) nos dois lados do UNION (era
--    VARCHAR2(30), vindo de AGENDAMENTO.DS_TIPO — G0 flagou que
--    VACINA.NM_VACINA é VARCHAR2(200); widening em vez de truncar).
--    ST_CONSENTE_LEMBRETE: usa o registro mais recente de
--    CONSENTIMENTO(ID_TUTOR, DS_TIPO='LEMBRETES') por DT_ACEITE (pode haver
--    mais de um por tutor/tipo — histórico insert-only); 'S' só se esse
--    registro tiver ST_ACEITO='S' AND DT_REVOGACAO IS NULL, senão 'N'
--    (COALESCE cobre tutor sem nenhum consentimento LEMBRETES).
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
-- só a view diverge de sintaxe entre os dois motores (TRUNC/SYSDATE — Oracle
-- nativo; H2 MODE=Oracle usa DATEDIFF). Os grupos 2 e 3 seriam portáveis
-- sozinhos (ALTER TABLE ADD de uma coluna por vez + CHECK, já provado
-- portável em V7/V8/V11/V16), mas nenhuma versão pode conviver em
-- db/migration/ e nos irmãos ao mesmo tempo (mesmo motivo de V2/V3/V5/V12/
-- V15/V17/V18) — por isso o arquivo inteiro é duplicado, idêntico nos
-- grupos 2/3, entre esta pasta e db/migration-h2/. A variante H2 está em
-- db/migration-h2/V21__vacinas_v2_notificacao_triagem_luna.sql.
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
    TRUNC(v.DT_PROXIMA_DOSE) - TRUNC(SYSDATE)     AS DIAS_RESTANTES,
    v.ID_VACINA,
    CAST('VACINA' AS VARCHAR2(20))                AS DS_ORIGEM,
    COALESCE((
        SELECT MAX(CASE WHEN c.ST_ACEITO = 'S' AND c.DT_REVOGACAO IS NULL THEN 'S' ELSE 'N' END)
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
  AND  v.DT_PROXIMA_DOSE >= CURRENT_TIMESTAMP
  AND  v.DT_PROXIMA_DOSE <= CURRENT_TIMESTAMP + INTERVAL '30' DAY

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
    TRUNC(a.DT_AGENDAMENTO) - TRUNC(SYSDATE)      AS DIAS_RESTANTES,
    CAST(NULL AS NUMBER(10))                      AS ID_VACINA,
    CAST('AGENDAMENTO' AS VARCHAR2(20))           AS DS_ORIGEM,
    COALESCE((
        SELECT MAX(CASE WHEN c.ST_ACEITO = 'S' AND c.DT_REVOGACAO IS NULL THEN 'S' ELSE 'N' END)
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
JOIN   TUTOR       t  ON t.ID_TUTOR    = a.ID_TUTOR
WHERE  a.DS_TIPO    = 'VACINA'
  AND  a.ST_STATUS NOT IN ('CANCELADO','REALIZADO')
  AND  p.ST_ATIVO   = 'S'
  AND  t.ST_ATIVO   = 'S'
  AND  a.DT_AGENDAMENTO >= CURRENT_TIMESTAMP
  AND  a.DT_AGENDAMENTO <= CURRENT_TIMESTAMP + INTERVAL '30' DAY;

COMMENT ON TABLE VW_VACINAS_VENCENDO IS
    'v2 (V21/LU-02): UNION ALL de aplicação real (VACINA, via EVENTO_CLINICO/TUTOR_PET) + agendamento futuro (AGENDAMENTO, regra pré-V21 preservada). Pet com 2 tutores gera 2 linhas na origem VACINA (cada tutor é avisado). DIAS_RESTANTES em dias de calendário (TRUNC). Exclui pet inativo / tutor soft-deletado.';

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
