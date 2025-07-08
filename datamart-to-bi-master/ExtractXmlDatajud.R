# Extrai XMLs do banco datajud_hml e realiza parsing basico

Sys.setlocale(locale = "Portuguese_Brazil.1252")

# Caminho da pasta de trabalho
setwd(pasta.trabalho)

# Carrega variaveis de ambiente e funcoes
source(paste0(pasta.trabalho,'VarAmbiente.R'), encoding = 'latin1')
library(RPostgres)
library(xml2)

# Conecta ao banco que contem os XMLs
con_hml <- dbConnect(RPostgres::Postgres(),
                     dbname   = db_hml,
                     host     = host_db_hml,
                     port     = db_port_hml,
                     user     = db_user_hml,
                     password = db_password_hml)

# Funcao para buscar os XMLs no periodo informado
fetch_xmls <- function(con, dt_ini, dt_fim, limite = NULL) {
  query <- paste0(
    "SELECT chave.nr_processo,",
    " chave.cd_classe_judicial,",
    " chave.nm_grau,",
    " chave.cd_orgao_julgador,",
    " lote.dh_envio_local,",
    " convert_from(xml.conteudo_xml, 'UTF8') AS xml_text",
    " FROM datajud_hml.tb_lote_processo lote",
    " JOIN datajud_hml.tb_chave_processo_cnj chave ON lote.id_chave_processo_cnj = chave.id_chave_processo_cnj",
    " JOIN datajud_hml.tb_xml_processo xml ON lote.id_xml_processo = xml.id_xml_processo",
    " WHERE lote.dh_envio_local BETWEEN '", dt_ini, "' AND '", dt_fim, "'" ,
    if(!is.null(limite)) paste0(" LIMIT ", limite) else "",
    ";"
  )
  dbGetQuery(con, query)
}

# Exemplo de uso
registros <- fetch_xmls(con_hml, '2024-08-01', '2025-07-31', limite = 100)

# Parseia cada XML
registros$xml_parsed <- lapply(registros$xml_text, xml2::read_xml)

# Aqui pode-se adicionar o calculo dos indicadores desejados a partir do xml_parsed
# ...

# Opcional: salvar cada XML em arquivo separado
salvar_xmls <- function(df, pasta = 'xml_extraidos') {
  dir.create(pasta, showWarnings = FALSE)
  mapply(function(xml_obj, num_proc) {
    caminho <- file.path(pasta, paste0(num_proc, '.xml'))
    xml2::write_xml(xml_obj, caminho)
  }, df$xml_parsed, df$nr_processo)
}

salvar_xmls(registros)

# Encerra conexao
dbDisconnect(con_hml)
