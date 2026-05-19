-- =============================================================================
-- KURA · Dados de Seed para H2 (Sincronizado com a Entidade Java Pet)
-- =============================================================================

-- 1. CLINICA
INSERT INTO clinica (id_clinica, nm_clinica, nr_cnpj, ds_endereco, nm_cidade, sg_uf, nr_cep, dt_cadastro, st_ativa) VALUES (1, 'Clyvo Vet São Paulo', '12.345.678/0001-90', 'Av. Paulista, 1000', 'São Paulo', 'SP', '01310-100', CURRENT_TIMESTAMP, 'S');

-- 2. ESPECIE
INSERT INTO especie (id_especie, nm_especie) VALUES (1, 'Cao');
INSERT INTO especie (id_especie, nm_especie) VALUES (2, 'Gato');

-- 3. RACA
INSERT INTO raca (id_raca, id_especie, nm_raca, ds_predisposicao) VALUES (1, 1, 'Labrador', 'Predisposição a displasia coxofemoral.');
INSERT INTO raca (id_raca, id_especie, nm_raca, ds_predisposicao) VALUES (2, 2, 'Siames', 'Predisposição a problemas renais.');

-- 4. TUTOR
INSERT INTO tutor (id_tutor, id_clinica, nm_tutor, nr_cpf, ds_email, ds_telefone, ds_whatsapp, dt_cadastro, st_ativo, st_aviso_privacidade, ds_versao_aviso) VALUES (1, 1, 'Felipe Ferrete', '12345678900', 'felipe@clyvo.vet', '11999990001', '11999990001', CURRENT_TIMESTAMP, 'S', 'S', 'v1.0');
INSERT INTO tutor (id_tutor, id_clinica, nm_tutor, nr_cpf, ds_email, ds_telefone, ds_whatsapp, dt_cadastro, st_ativo, st_aviso_privacidade, ds_versao_aviso) VALUES (2, 1, 'Ana Silva', '98765432100', 'ana.silva@gmail.com', '11999990002', '11999990002', CURRENT_TIMESTAMP, 'S', 'S', 'v1.0');

-- 5. CONTA_TUTOR (Senha original: Senha@123 em BCrypt)
INSERT INTO conta_tutor (id_conta, id_tutor, ds_email_login, ds_senha_hash, dt_criacao, st_ativa, st_email_verificado, nr_tentativas_login) VALUES (1, 1, 'felipe@clyvo.vet', '$2a$12$wdGMtc18HWtAQohTkOBQx.6h9Crc7AODa60lIAJTx22nRqzVyrsQe', CURRENT_TIMESTAMP, 'S', 'S', 0);

-- 6. CONSENTIMENTO
INSERT INTO consentimento (id_consentimento, id_tutor, ds_tipo, ds_versao_termo, st_aceito, dt_aceite, ds_ip_aceite) VALUES (1, 1, 'TELEORIENTACAO', 'v1.0', 'S', CURRENT_TIMESTAMP, '192.168.0.15');

-- 7. PET (100% fiel à sua classe Java Pet, com alergias e observações para testar no Swagger)
INSERT INTO pet (id_pet, id_especie, id_raca, nm_pet, dt_nascimento, sg_sexo, nr_peso_kg, sg_porte, st_castrado, ds_alergias, ds_observacoes, dt_cadastro, st_ativo) VALUES (1, 1, 1, 'Marley', '2022-03-15', 'M', 32.50, 'G', 'S', 'Alergia a picada de pulga e dipirona.', 'Animal dócil, mas ansioso.', CURRENT_TIMESTAMP, 'S');

INSERT INTO pet (id_pet, id_especie, id_raca, nm_pet, dt_nascimento, sg_sexo, nr_peso_kg, sg_porte, st_castrado, ds_alergias, ds_observacoes, dt_cadastro, st_ativo) VALUES (2, 2, 2, 'Luna', '2023-08-20', 'F', 4.20, 'P', 'S', NULL, 'Gata assustada, manejo calmo.', CURRENT_TIMESTAMP, 'S');

-- 8. TUTOR_PET
INSERT INTO tutor_pet (id_tutor, id_pet, ds_vinculo, dt_vinculo, st_principal) VALUES (1, 1, 'PROPRIETARIO', CURRENT_TIMESTAMP, 'S');
INSERT INTO tutor_pet (id_tutor, id_pet, ds_vinculo, dt_vinculo, st_principal) VALUES (2, 2, 'PROPRIETARIO', CURRENT_TIMESTAMP, 'S');

-- 9. AGENDAMENTO
INSERT INTO agendamento (id_agendamento, id_tutor, id_pet, id_clinica, dt_agendamento, nr_duracao_minutos, ds_tipo, st_status, ds_origem, dt_criacao) VALUES (1, 1, 1, 1, TIMESTAMPADD('DAY', 1, CURRENT_TIMESTAMP), 30, 'CONSULTA', 'AGENDADO', 'PORTAL', CURRENT_TIMESTAMP);