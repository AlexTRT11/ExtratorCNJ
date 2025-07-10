-- Cria o schema do datamart
CREATE SCHEMA IF NOT EXISTS datamart;
SET search_path TO datamart;

-- Tabelas iniciais
\i /docker-entrypoint-initdb.d/create_indicadores_historico.sql
