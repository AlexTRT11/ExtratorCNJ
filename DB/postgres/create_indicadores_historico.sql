CREATE TABLE IF NOT EXISTS indicadores_historico (
    id SERIAL PRIMARY KEY,
    nr_processo      VARCHAR(30),
    indicador        VARCHAR(50),
    valor            NUMERIC,
    data_calculo     DATE DEFAULT CURRENT_DATE
);
