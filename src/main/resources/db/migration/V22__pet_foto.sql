-- =============================================================================
-- V22__pet_foto.sql
-- FT-01 (KURA_BACKLOG_FOTO_PET). Adiciona colunas de foto à tabela PET.
--
-- Decisão de arquitetura (G0 / adendo do maestro, 2026-09-24, regras A1/A2 do
-- backlog): binário NUNCA vai para o Oracle -- aqui só se grava a CHAVE do
-- arquivo (armazenamento fora do banco, resolvido por IArmazenamentoArquivos
-- no .NET, FT-02/FT-03) e o timestamp da última atualização. A URL servida
-- ao app (`dsFotoUrl`/`dsFotoThumbUrl`) é DERIVADA da chave no momento da
-- resposta -- nunca persistida como string fixa (evita repetir o anti-padrão
-- medido em DOCUMENTO.DS_CAMINHO, que guarda caminho absoluto completo em vez
-- de chave relativa -- ver G0 item 2).
--
-- DS_FOTO_CHAVE VARCHAR2(500): formato proposto
-- clinica/{idClinica}/pet/{idPet}/{uuid}.{ext} (chave relativa; ext = webp,
-- jpg ou png, detectado pelos magic bytes no upload; as variantes sao
-- derivadas: {uuid}_256.{ext} e {uuid}_1080.{ext} -- regra A2, ruling F7-a). Mesmo
-- tamanho de DOCUMENTO.DS_CAMINHO (V9__schema_drift_clinico.sql:193), que já
-- provou ser suficiente para path de arquivo. Nullable: pet sem foto
-- continua existindo (maioria dos pets, no dia da demo) -- fallback é o
-- avatar ilustrado atual (KPetPortrait/KCPetPortrait, item 1 do G0).
-- DT_FOTO_ATUALIZACAO TIMESTAMP: timestamp da última troca/upload de foto;
-- nullable pelo mesmo motivo (nunca houve upload).
--
-- Arquivo único em db/migration/ (portável, SEM split -oracle/-h2): o par
-- VARCHAR2/TIMESTAMP via ALTER TABLE ... ADD já é padrão portável comprovado
-- neste projeto -- V10__agendamento_teleconsulta.sql:7-20 faz exatamente
-- este mesmo par de ALTER TABLE ... ADD (VARCHAR2(512) e 2x TIMESTAMP) num
-- arquivo único, sem split, aplicado com sucesso nos dois profiles desde a
-- FEAT-01. PetFotoV22MigrationTest mede de novo aqui (não herda a alegação):
-- suíte "./mvnw test -Dspring.profiles.active=dev" (Flyway aplica a variante
-- -h2 + este arquivo comum) verde confirma que o padrão se repete para PET.
--
-- Ownership: PET é tabela .NET-owned (Flyway continua sendo a única
-- autoridade de DDL). Quem GRAVA a foto é o backend-clinica-dotnet (FT-03).
-- O backend-tutor-java só LÊ estas 2 colunas -- Pet.java continua anotado
-- @Immutable (FT-05) -- para derivar a URL assinada exposta ao tutor, com a
-- MESMA fórmula de assinatura usada pelo .NET (âncora obrigatória na FT-05).
-- =============================================================================

ALTER TABLE PET
    ADD DS_FOTO_CHAVE VARCHAR2(500);

ALTER TABLE PET
    ADD DT_FOTO_ATUALIZACAO TIMESTAMP;

COMMENT ON COLUMN PET.DS_FOTO_CHAVE IS 'Chave relativa do arquivo de foto no armazenamento externo (nunca o binário, nunca a URL) -- formato clinica/{idClinica}/pet/{idPet}/{uuid}.{ext} (ext webp, jpg ou png; variantes derivadas {uuid}_256.{ext} e {uuid}_1080.{ext}). Nullable: pet sem foto usa o avatar ilustrado. Gravada pelo backend-clinica-dotnet (FT-03); o backend-tutor-java só lê, para derivar a URL assinada (FT-05).';
COMMENT ON COLUMN PET.DT_FOTO_ATUALIZACAO IS 'Timestamp da última troca/upload de foto. Nullable até o primeiro upload.';
