# Cria view persistente com os indicadores calculados a partir do datamart
source('VarAmbiente.R', encoding = 'latin1')
source('PacotesFuncoes.R', encoding = 'latin1')

library(RPostgres)

con <- dbConnect(RPostgres::Postgres(),
                 dbname   = db,
                 host     = host_db,
                 port     = db_port,
                 user     = db_user,
                 password = db_password)

# Exemplo simples de criacao de view utilizando a tabela fato dos processos
query <- "CREATE OR REPLACE VIEW vw_processos_indicadores AS\n";
query <- paste0(query, "SELECT id_processo, indicador, valor, data_calculo\n",
                "FROM indicadores_historico;")

try(dbExecute(con, query))

dbDisconnect(con)
