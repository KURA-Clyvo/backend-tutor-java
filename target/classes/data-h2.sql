-- =============================================================================
-- Dados de seed para o profile H2 (desenvolvimento sem Oracle)
-- Executado automaticamente ao iniciar com -Dspring.profiles.active=h2
-- =============================================================================

-- Clínica de referência (lida pelo backend tutor, criada pelo .NET do Felipe)
INSERT INTO clinica (id_clinica, nm_clinica, nr_cnpj, ds_endereco, nm_cidade, sg_uf, nr_cep, dt_cadastro, st_ativa)
VALUES (1, 'Clyvo Vet São Paulo', '12.345.678/0001-90', 'Av. Paulista, 1000', 'São Paulo', 'SP', '01310-100', CURRENT_TIMESTAMP, 'S');

-- Espécies
INSERT INTO especie (id_especie, nm_especie) VALUES (1, 'Cão');
INSERT INTO especie (id_especie, nm_especie) VALUES (2, 'Gato');
INSERT INTO especie (id_especie, nm_especie) VALUES (3, 'Ave');

-- Raças
INSERT INTO raca (id_raca, id_especie, nm_raca) VALUES (1, 1, 'Labrador Retriever');
INSERT INTO raca (id_raca, id_especie, nm_raca) VALUES (2, 1, 'Golden Retriever');
INSERT INTO raca (id_raca, id_especie, nm_raca) VALUES (3, 2, 'Siamês');

-- Tutor de exemplo
INSERT INTO tutor (id_tutor, id_clinica, nm_tutor, nr_cpf, ds_email, ds_telefone, dt_cadastro, st_ativo, st_aviso_privacidade)
VALUES (1, 1, 'Felipe Ferrete', '123.456.789-00', 'felipe@clyvo.vet', '(11) 99999-0001', CURRENT_TIMESTAMP, 'S', 'S');

-- Conta do tutor (senha: Senha@123 — BCrypt)
INSERT INTO conta_tutor (id_conta, id_tutor, ds_email_login, ds_senha_hash, dt_criacao, st_ativa, st_email_verificado, nr_tentativas_login)
VALUES (1, 1, 'felipe@clyvo.vet', '$2a$12$vVp7I8T6Yw8fHpCuY6jOlOC./3xoTy3jBFb.e9BbZl9qDN5f4CUGC', CURRENT_TIMESTAMP, 'S', 'S', 0);
