# Define Char set
Sys.setlocale(locale= "Portuguese_Brazil.1252")

# Define Pasta de trabalho
#pasta.trabalho<-"C:/datamarttobi/" ##Endereço da pasta estão os scrips "DatamartToBi.R", "VarAmbiente.R" e "PacotesFuncoes.R" e onde os outputs serão salvos
setwd(pasta.trabalho)
getwd()

# Carrega variáveis de ambiente
source(paste0(pasta.trabalho,'VarAmbiente.R'),encoding = "latin1")

# Carrega funções e bibliotecas
source(paste0(pasta.trabalho,'PacotesFuncoes.R'),encoding = "latin1")

# Conecta ao datamart
con <- func.connect()

# gera objetos auxiliares para cálculos de data
tempo <<- dbGetQuery(con, paste0("select id, data, ano, mes from tempo where dia = 1 and id >= 20200101 and data < '",today()-dt.corr,"'")) %>% 
  dplyr::mutate(ultimo_dia = data %>% rollforward()) %>%
  dplyr::mutate_if(is.character, iconv, "utf-8","latin1")

list.tp <- as.numeric(gsub("-","",tempo$ultimo_dia))
max.dt.ult.dia <- max(list.tp)
max.data.ult.dia <- ymd(max.dt.ult.dia)
max.dt.ult.dia_1 <- as.numeric(gsub("-","",max.data.ult.dia+1))
max.ano <- year(max.data.ult.dia)
max.mes <- month(max.data.ult.dia)

# Último registro
ult.reg <<- dbGetQuery(con, paste0("select chave, valor as ultimo_registro from parametro")) %>%
  dplyr::mutate(sigla_tribunal = toupper(str_sub(chave, str_locate(chave,"nsercao:")[,2]+1)),
                data_envio = as.POSIXct(as.numeric(ultimo_registro) / 1000, origin = "1970-01-01")) %>%
  dplyr::mutate_if(is.character, iconv, "utf-8","latin1")
#classes - datamart
classes <<- dbGetQuery(con, paste0("select id as id_classe, nome as classe, nome_completo from classe order by id")) %>%
  separate(nome_completo,into = paste("arvore",1:6, sep="_"),sep = "\\|",remove = F )  %>%
  dplyr::mutate_if(is.character, iconv, "utf-8","latin1")

#assuntos - datamart
assuntos <<- dbGetQuery(con, paste0("select id as id_assunto, nome as assunto, nome_completo from assunto order by id"))%>%
  separate(nome_completo,into = paste("arvore",1:8, sep="_"),sep = "\\|",remove = F)  %>%
  dplyr::mutate_if(is.character, iconv, "utf-8","latin1")

#movimentos - datamart
movimentos <<- dbGetQuery(con, paste0("select id_origem as cod_movimento, nome as movimento, nome_completo from movimento order by id"))  %>%
  dplyr::mutate_if(is.character, iconv, "utf-8","latin1")

# Órgãos julgadores - banco tribunal
orgaos <<- dbGetQuery(con, paste0("select sigla_tribunal as tribunal, id as id_orgao_julgador, nome as orgao_julgador, uf as uf_oj, id_municipio, nome_municipio as municipio_oj, 
                         sig_tipo_unidade_judiciaria as tipo_unidade, dsc_classificacao_unid_judiciaria as classificacao_unidade, 
                         flg_balcao_virtual, flg_juizo_digital, dat_adesao_juizo_digital, dat_termino_juizo_digital, dat_instalacao, seq_lista_competencia_juizo, 
                         competencia, exclusiva as competencia_exclusiva, tipo as competencia_tipo, situacao 
                         from orgao_julgador oj
                         left join tipo_unidade_judiciaria tuj on tuj.seq_tipo_unidade_judiciaria = oj.seq_tipo_unidade_judiciaria
                         left join classificacao_unid_judiciaria cuj on cuj.seq_classificacao_unid_judiciaria = oj.seq_classificacao_unid_judiciaria
left join (select id_orgao_julgador, seq_competencia, competencia,
case when juizo = 1 then 'Juízo Único'
	 when num_competencia = 1 then competencia[1]
	 when criminal = 1 and não_criminal = 0 and depende = 0 then 'Criminal'
	 when criminal = 0 and não_criminal = 1 and depende = 0 then 'Não Criminal'
	 else 'Cumulativa' end as exclusiva,
	 case when juizo = 1 then 'Juízo único'
     	  when criminal = 1 and não_criminal = 0 and depende = 0 then 'Criminal'
	 	  when criminal = 0 and não_criminal = 1 and depende = 0 then 'Não Criminal'
	      else 'Criminal/Não criminal' end as tipo
from(
select id_orgao_julgador, array_agg(seq_competencia) as seq_competencia, 
array_agg(distinct competencia) as competencia, 
array_length(array_agg(distinct competencia),1) as num_competencia, 
max(case when seq_competencia = 1 then 1 else 0 end) as juizo,
max(case when seq_competencia in (11,13,12,35,32) then 1 else 0 end) as juizado_especial,
max(case when seq_competencia in (44,3,20,12,15,39,40,43,16) then 1 else 0 end) as criminal,
max(case when seq_competencia in (2,7,8,5,10,6,45,11,35,4,17,19,26,28,30,33,34,41,42) then 1 else 0 end) as não_criminal,
max(case when seq_competencia in (25,29,27,13,1,9,18,32,22) then 1 else 0 end) as depende
from (
select id_orgao_julgador, seq_competencia,
	case when seq_competencia in (25,29) then 'Ambiental' 
		 when seq_competencia  in (7,8) then 'Execução Fiscal / Fazenda Pública'
		 when seq_competencia  in (5,10) then 'Família / Órfãos e sucessões'
		 when seq_competencia  in (6,45) then 'Infância e Juventude'
		 else dsc_competencia_juizo
		 end as competencia
from (
select distinct id as id_orgao_julgador, 
		unnest(case when seq_lista_competencia_juizo && array[1] then array[1] else seq_lista_competencia_juizo end) as seq_competencia
from orgao_julgador oj) as comp
join competencia_juizo cj on cj.seq_competencia_juizo = comp.seq_competencia
where seq_competencia not in (999)
) as comp1
group by id_orgao_julgador
) as comp_final) as comp on comp.id_orgao_julgador = oj.id")) %>%
  dplyr::mutate_if(is.character, iconv, "utf-8","latin1")

#Carrega Tribunais - Datamart
tribunais <<- dbGetQuery(con, "select t.id, t.sigla, t.nome, t.id_origem, id_porte,
case when sj.nome IN ('Justiça dos Estados e do Distrito Federal e Territórios') then 'Justiça Estadual'
                                                 when t.sigla IN ('STJ','STM','TST','TSE') then 'Tribunais Superiores' 
                                                 else sj.nome end as ramo_justica
                              from tribunal t join segmento_justica sj 
                        on sj.id = t.id_segmento_justica") %>% 
                arrange(id_porte, desc(ramo_justica), sigla) %>%
  dplyr::mutate_if(is.character, iconv, "utf-8","latin1")


############# EXTRAI DADOS DA TABELA PROCESSO_INDICADOR E CALCULA INDICADORES #########################

juntar.fatos <- NULL
juntar.classes <- NULL
juntar.assuntos <- NULL

t1.2 <- Sys.time()

  tp.trib <- Sys.time()
  
  #processamento em partes
    
  if(!exists("partes")){
    partes <- 1
    nome.parte = ""
  } else if(is.null(partes) | is.na(partes) | partes <= 1){
    partes <- 1
    nome.parte = ""
  }
  
  if(!exists("parte")){
      parte <- 1
    } else if(is.null(parte) | is.na(parte)){
      parte <- 1
    }
  p.parte <- ifelse(parte <= partes, parte, partes)
  
  
  for(parte in p.parte:partes){
    if(partes > 1){
      nome.parte <- paste0("_",parte,"-",partes)
    }
      
    
    
  # Extrai dados
        
      if(partes > 1){
          cortes <<- func.quebra(trib, partes)
        }
      
      
      bd <- func.extrair.painel(trib, quebra = partes, parte=parte) %>%
              dplyr::mutate_if(is.character, iconv, "utf-8","latin1") %>% 
              dplyr::mutate(dt_baixa = case_when(is.na(ymd(dt_baixa)) ~ ymd(dt_baixa - 1) + 1,
                                           TRUE ~ ymd(dt_baixa)),
                            procedimento = case_when(
                              procedimento == "fiscal" ~ "Execução fiscal",
                              procedimento == "nao fiscal" ~ "Execução extrajudicial não fiscal",
                              procedimento == "conhecimento" & criminal == 1 ~ "Conhecimento criminal",
                              procedimento == "conhecimento" ~ "Conhecimento não criminal",
                              procedimento == "execucao" & criminal == 1 & privativa_liberdade == 1 ~ "Execução penal privativa de liberdade",
                              procedimento == "execucao" & criminal == 1 & privativa_liberdade == 0 ~ "Execução penal não privativa de liberdade",
                              procedimento == "execucao" ~ "Execução judicial",
                              procedimento == "pre-processual"  ~ "Pré-processual", 
                              procedimento == "investigatoria"  ~ "Fase investigatória",
                              procedimento == "administrativo"  ~ "Administrativo eleitoral",
                              TRUE ~ "Outros",
                            ),
                            id_procedimento = case_when(
                              procedimento == "Execução fiscal" ~ 3,
                              procedimento == "Execução extrajudicial não fiscal" ~ 4,
                              procedimento == "Conhecimento criminal" ~ 1,
                              procedimento == "Conhecimento não criminal" ~ 2,
                              procedimento == "Execução penal privativa de liberdade" ~ 5,
                              procedimento == "Execução penal não privativa de liberdade" ~ 6,
                              procedimento == "Execução judicial" ~ 7,
                              procedimento == "Pré-processual" ~ 9, 
                              procedimento == "Fase investigatória" ~ 8,
                              procedimento == "Administrativo eleitoral" ~ 10,
                              TRUE ~ 11,
                            ), 
                            proc_cn = case_when(id_procedimento %in% 1:7 ~ 1,
                                                TRUE ~ 0),
                            originario = case_when(id_originario == 1 ~ "Originário",
                                                   id_originario == 0 ~ "Recursal"),
                            recurso = replace_na(recurso,0),
                            
                            grau_dup = case_when(sigla_grau %in% c("G1","JE") ~ "G1",
                                                 TRUE ~ sigla_grau),
                            formato = case_when(id_formato == 1 ~ "Eletrônico",
                                                TRUE ~ "Físico"),
                            id_ultima_classe_cn = as.numeric(gsub("\\}","",str_extract(id_classes_cn, "\\d+\\}"))),
                            id_ultima_classe = as.numeric(id_ultima_classe)) %>%
              dplyr::left_join(tribunais, by = c("sigla_tribunal"="sigla")) %>%
              dplyr::mutate(ramo_justica = case_when(sigla_tribunal ==  "STM" & sigla_grau == "G1" ~ "Justiça Militar da União",
                                                     TRUE ~ ramo_justica),
                            chave = paste(id_orgao_julgador,sigla_tribunal,
                                          sigla_grau,recurso,id_formato,id_procedimento,id_originario, sep="_"),
                            chave_trib = paste(sigla_tribunal,grau_dup,recurso,id_procedimento,numero, sep="_"),
                            chave_classe = paste(id_orgao_julgador,sigla_tribunal,
                                                 sigla_grau,recurso,id_formato,id_procedimento,id_originario, id_ultima_classe, sep="_")) %>%
              dplyr::arrange(chave) %>%
              dplyr::mutate(id = 1:n(),
                            id_chave = as.numeric(as.factor(chave)),
                            id_chave_trib = as.numeric(as.factor(chave_trib)))
      
      tt <- data.frame(Tribunal = trib, processos = nrow(bd), ultimo_registro = ult.reg$ultimo_registro[ult.reg$sigla_tribunal == trib][1],
                       ultima_movimentacao = max(bd$data_ultima_movimentacao[!is.na(bd$data_ultima_movimentacao)], na.rm=T),  
                       extracao = round(difftime(Sys.time(), tp.trib, units = "mins"),2))
  
      print(paste0(trib,": Inserir chaves: ", tt$extracao, " minutos."))
      
      
      
      
      
    # Corrige processos redistribuídos entre 1º Grau e Juizados Especiais e adiciona a data de início do processo para cálculo dos tempos
    bd.tot <- bd %>%
      dplyr::mutate(dt_recebimento = as.numeric(replace_na(dt_recebimento,99999999)),
                    dt_arquivamento = as.numeric(replace_na(dt_arquivamento,99999999)),
                    dt_remessa = as.numeric(replace_na(dt_remessa,99999999)),
                    dt_primeira_medida_protetiva = as.numeric(replace_na(dt_primeira_medida_protetiva,99999999)),
                    dt_primeira_pronuncia = as.numeric(replace_na(dt_primeira_pronuncia,99999999)),
                    dt_primeira_sessao_juri_designada = as.numeric(replace_na(dt_primeira_sessao_juri_designada,99999999)),
                    dt_primeira_sessao_juri_realizada = as.numeric(replace_na(dt_primeira_sessao_juri_realizada,99999999)),
                    dt_primeiro_julgamento = as.numeric(replace_na(dt_primeiro_julgamento,99999999)),
                    dt_primeiro_julgamento_merito = as.numeric(replace_na(dt_primeiro_julgamento_merito,99999999)),
                    dt_primeiro_julgamento_homologatorio = as.numeric(replace_na(dt_primeiro_julgamento_homologatorio,99999999)))%>%
      dplyr::group_by(id_chave_trib) %>%
      dplyr::summarise(data_total_inicio = min(dt_recebimento, na.rm=T),
                       data_total_primeira_baixa = min(dt_baixa, na.rm=T),
                       data_total_primeiro_arquivamento = min(dt_arquivamento, na.rm=T),
                       data_total_primeira_remessa = min(dt_remessa, na.rm=T),
                       data_total_primeira_medida_protetiva = min(dt_primeira_medida_protetiva, na.rm=T),
                       data_total_primeira_pronuncia = min(dt_primeira_pronuncia, na.rm=T),
                       data_total_primeira_sessao_juri_designada = min(dt_primeira_sessao_juri_designada, na.rm=T),
                       data_total_primeira_sessao_juri_realizada = min(dt_primeira_sessao_juri_realizada, na.rm=T),
                       data_total_primeiro_julgamento = min(dt_primeiro_julgamento, na.rm=T),
                       data_total_primeiro_julgamento_merito = min(dt_primeiro_julgamento_merito, na.rm=T),
                       data_total_primeiro_julgamento_homologatorio = min(dt_primeiro_julgamento_homologatorio, na.rm=T),
                       data_total_pendente = paste0(dt_pendente, collapse=","),
                       data_total_pendente_liquido = paste0(dt_pendente_liquido, collapse=",")) %>%
      dplyr::mutate(data_total_inicio = case_when(data_total_inicio < 99999999 ~ ymd(data_total_inicio)),
                    data_total_primeira_baixa = case_when(data_total_primeira_baixa < 99999999 ~ ymd(data_total_primeira_baixa)),
                    data_total_primeiro_arquivamento = case_when(data_total_primeiro_arquivamento < 99999999 ~ ymd(data_total_primeiro_arquivamento)),
                    data_total_primeira_remessa = case_when(data_total_primeira_remessa < 99999999 ~ ymd(data_total_primeira_remessa)),
                    data_total_primeira_medida_protetiva = case_when(data_total_primeira_medida_protetiva < 99999999 ~ ymd(data_total_primeira_medida_protetiva)),
                    data_total_primeira_pronuncia = case_when(data_total_primeira_pronuncia < 99999999 ~ ymd(data_total_primeira_pronuncia)),
                    data_total_primeira_sessao_juri_designada = case_when(data_total_primeira_sessao_juri_designada < 99999999 ~ ymd(data_total_primeira_sessao_juri_designada)),
                    data_total_primeira_sessao_juri_realizada = case_when(data_total_primeira_sessao_juri_realizada < 99999999 ~ ymd(data_total_primeira_sessao_juri_realizada)),
                    
                    data_total_primeiro_julgamento = case_when(data_total_primeiro_julgamento < 99999999 ~ ymd(data_total_primeiro_julgamento)),
                    data_total_primeiro_julgamento_merito = case_when(data_total_primeiro_julgamento_merito < 99999999 ~ ymd(data_total_primeiro_julgamento_merito)),
                    data_total_primeiro_julgamento_homologatorio = case_when(data_total_primeiro_julgamento_homologatorio < 99999999 ~ ymd(data_total_primeiro_julgamento_homologatorio)))
    
    print(paste0(trib,": Consolidar totais em outra base: ", round(difftime(Sys.time(), tp.trib, units = "mins"),2), " minutos."))
    
    bd <- bd %>% dplyr::left_join(bd.tot, by="id_chave_trib") %>%
      dplyr::mutate(
        dt_recebimento = ymd(dt_recebimento),
        dt_redistribuido_entrada = ymd(dt_redistribuido_entrada),
        data_ajuizamento = case_when(is.na(data_ajuizamento) | data_ajuizamento < as.Date("1901-01-01") | data_ajuizamento > today() |
                                       (criminal == 1 & !is.na(data_total_inicio)) ~
                                       data_total_inicio,
                                     TRUE ~ as.Date(data_ajuizamento)),
        sem_tramitacao = case_when(grepl(":0}|:0,",dt_pendente_liquido) & is.na(dt_ultima_conclusao) ~
                                     as.numeric(round(difftime(max(tempo$ultimo_dia, na.rm=T), data_ultima_movimentacao, units = "days"),0))),
        sem_tramitacao = case_when(sem_tramitacao >=0 ~ sem_tramitacao),
        sem_conclusao = as.numeric(round(difftime(max(tempo$ultimo_dia, na.rm=T), ymd(dt_ultima_conclusao), units = "days"),0)),
        sem_conclusao = case_when(sem_conclusao >=0 ~ sem_conclusao),
        antigos = case_when(grepl(":0}|:0,",dt_pendente_liquido) | grepl(":0}|:0,",dt_pendente) ~
                              as.numeric(round(difftime(max(tempo$ultimo_dia, na.rm=T), data_ajuizamento, units = "days"),0))),
        antigos = case_when(antigos >=0 ~ antigos),
        dt_arquivamento = ymd(dt_arquivamento),
        dt_remessa = ymd(dt_remessa),
        dt_primeiro_julgamento = ymd(dt_primeiro_julgamento),
        dt_primeiro_julgamento_merito = ymd(dt_primeiro_julgamento_merito),
        dt_primeiro_julgamento_homologatorio = ymd(dt_primeiro_julgamento_homologatorio),
        dt_primeira_medida_protetiva = ymd(dt_primeira_medida_protetiva),
        dt_primeira_pronuncia = ymd(dt_primeira_pronuncia),
        dt_primeira_sessao_juri_designada = ymd(dt_primeira_sessao_juri_designada),
        dt_primeira_sessao_juri_realizada = ymd(dt_primeira_sessao_juri_realizada),
        dt_ultimo_pendente_liquido = ymd(dt_ultimo_pendente_liquido),
        dt_redistribuido_entrada = ymd(dt_redistribuido_entrada),
        dt_redistribuido_saida = case_when(is.na(ymd(dt_redistribuido_saida)) ~ ymd(dt_redistribuido_saida - 1) + 1,
                                           TRUE ~ ymd(dt_redistribuido_saida)),
        dt_reativacao = ymd(dt_reativacao),
        dt_reativacao_fim = case_when(is.na(ymd(dt_reativacao_fim)) ~ ymd(dt_reativacao_fim - 1) + 1,
                                      TRUE ~ ymd(dt_reativacao_fim)),
        dt_recebimento = case_when(data_total_inicio == dt_recebimento ~ dt_recebimento),
        dt_baixa = case_when(data_total_primeira_baixa == dt_baixa ~ dt_baixa),
        dt_arquivamento = case_when(data_total_primeiro_arquivamento == dt_arquivamento ~ dt_arquivamento),
        dt_remessa = case_when(data_total_primeira_remessa == dt_remessa ~ dt_remessa),
        dt_primeiro_julgamento = case_when(data_total_primeiro_julgamento == dt_primeiro_julgamento ~ dt_primeiro_julgamento),
        dt_primeiro_julgamento_merito = case_when(data_total_primeiro_julgamento_merito == dt_primeiro_julgamento_merito ~ dt_primeiro_julgamento_merito),
        dt_primeiro_julgamento_homologatorio = case_when(data_total_primeiro_julgamento_homologatorio == dt_primeiro_julgamento_homologatorio ~ dt_primeiro_julgamento_homologatorio),
        dt_primeira_medida_protetiva = case_when(data_total_primeira_medida_protetiva == dt_primeira_medida_protetiva ~ dt_primeira_medida_protetiva),
        dt_primeira_pronuncia = case_when(data_total_primeira_pronuncia == dt_primeira_pronuncia ~ dt_primeira_pronuncia),
        dt_primeira_sessao_juri_designada = case_when(data_total_primeira_sessao_juri_designada == dt_primeira_sessao_juri_designada ~ dt_primeira_sessao_juri_designada),
        dt_primeira_sessao_juri_realizada = case_when(data_total_primeira_sessao_juri_realizada == dt_primeira_sessao_juri_realizada ~ dt_primeira_sessao_juri_realizada),
        data_total_pendente = case_when(data_total_inicio == dt_recebimento ~ gsub(",,",",",gsub("\\},\\{",",",data_total_pendente))),
        data_total_pendente_liquido = case_when(data_total_inicio == dt_recebimento ~ gsub(",,",",",gsub("\\},\\{",",",data_total_pendente_liquido))),
        tp_julg_st = case_when(!is.na(data_total_inicio) & !is.na(dt_primeiro_julgamento) & dt_primeiro_julgamento >= data_total_inicio ~
                                 as.numeric(round(difftime(dt_primeiro_julgamento, data_total_inicio, units = "days"),0))),
        tp_julg_merito_st = case_when(!is.na(data_total_inicio) & !is.na(dt_primeiro_julgamento_merito) & dt_primeiro_julgamento_merito >= data_total_inicio ~
                                        as.numeric(round(difftime(dt_primeiro_julgamento_merito, data_total_inicio, units = "days"),0))),
        tp_medprot_st = case_when(!is.na(data_total_inicio) & !is.na(dt_primeira_medida_protetiva) & dt_primeira_medida_protetiva >= data_total_inicio ~
                                    as.numeric(round(difftime(dt_primeira_medida_protetiva, data_total_inicio, units = "days"),0))),
        tp_pronuncia_st = case_when(!is.na(data_total_inicio) & !is.na(dt_primeira_pronuncia) & dt_primeira_pronuncia >= data_total_inicio ~
                                      as.numeric(round(difftime(dt_primeira_pronuncia, data_total_inicio, units = "days"),0))),
        tp_juri_desig_st = case_when(!is.na(data_total_inicio) & !is.na(dt_primeira_sessao_juri_designada) & dt_primeira_sessao_juri_designada >= data_total_inicio ~
                                       as.numeric(round(difftime(dt_primeira_sessao_juri_designada, data_total_inicio, units = "days"),0))),
        tp_juri_realiz_st = case_when(!is.na(data_total_inicio) & !is.na(dt_primeira_sessao_juri_realizada) & dt_primeira_sessao_juri_realizada >= data_total_inicio ~
                                        as.numeric(round(difftime(dt_primeira_sessao_juri_realizada, data_total_inicio, units = "days"),0))),
        tp_baix_st = case_when(!is.na(data_total_inicio) & !is.na(dt_baixa) & dt_baixa >= data_total_inicio ~ 
                                 as.numeric(round(difftime(dt_baixa, data_total_inicio, units = "days"),0))),
        tp_arq_st = case_when(!is.na(data_total_inicio) & !is.na(dt_arquivamento) & dt_arquivamento >= data_total_inicio ~ 
                                as.numeric(round(difftime(dt_arquivamento, data_total_inicio, units = "days"),0))),) %>%
          dplyr::left_join(orgaos, by = "id_orgao_julgador") %>%
              dplyr::left_join(classes %>% select(id_classe, classe), by = c("id_ultima_classe"="id_classe"))
    
    rm(bd.tot)
    gc()
    
    tt <- data.frame(tt, calcular_totais = round(difftime(Sys.time(), tp.trib, units = "mins"),2))
    print(paste0(trib,": Calcular variáveis totais: ", tt$calcular_totais, " minutos."))
      
      #Salva arquivo Rdata e csv
      save(bd, file = paste0("1)Extrai_Datamart_",trib,nome.parte,".RData"))
      data.table::fwrite(bd %>%
                           dplyr::select(sigla_tribunal,sigla_grau,procedimento,sistema,numero_sigilo,id_ultimo_oj,valor_causa,id_classe,id_assunto,
                                         ids_orgao_julgador,id_orgao_julgador,recurso,id_classes_cn,id_ultima_classe,data_ultima_movimentacao,
                                         data_ajuizamento,dt_recebimento,dt_pendente,dt_baixa,dt_remessa,dt_arquivamento,dt_pendente_liquido,
                                         dt_concluso,dt_julgamento,dt_julgamento_merito,dt_julgamento_sem_merito,dt_julgamento_homologatorio,
                                         dt_primeiro_julgamento,dt_primeiro_julgamento_merito,dt_primeiro_julgamento_homologatorio,dt_despacho,dt_decisao,
                                         dt_audiencia,dt_audiencia_conc,dt_liminar_deferida,dt_liminar_indeferida,dt_redistribuido_entrada,dt_redistribuido_saida,
                                         dt_reativacao,dt_reativacao_fim,dt_rec_inc_novo,dt_rec_inc_pendente,dt_rec_inc_julgado,dt_julg_decisao_ed,
                                         dt_primeira_medida_protetiva,dt_medida_protetiva,id_procedimento,proc_cn,originario,formato,id_ultima_classe_cn,nome,
                                         ramo_justica,data_total_inicio,data_total_primeira_baixa,data_total_primeiro_arquivamento,data_total_primeira_remessa,
                                         data_total_primeira_medida_protetiva,data_total_primeiro_julgamento,data_total_primeiro_julgamento_merito,
                                         data_total_primeiro_julgamento_homologatorio,data_total_pendente,data_total_pendente_liquido,sem_tramitacao,sem_conclusao,
                                         antigos,tp_julg_st,tp_julg_merito_st,tp_medprot_st,tp_baix_st,tp_arq_st,orgao_julgador,uf_oj,id_municipio,municipio_oj,tipo_unidade,
                                         classificacao_unidade,flg_balcao_virtual,flg_juizo_digital,dat_adesao_juizo_digital,dat_termino_juizo_digital,dat_instalacao,
                                         seq_lista_competencia_juizo,competencia,competencia_exclusiva,competencia_tipo,situacao,classe), 
                         paste0("1)Extrai_Datamart_",trib,nome.parte,".csv"), sep = ";")
      
    tt <- data.frame(tt, salvar_bd = round(difftime(Sys.time(), tp.trib, units = "mins"),2))
    print(paste0(trib,": Salvar arquivo por processo: ", tt$salvar_bd, " minutos."))
    
    
    
    
      # Cria o formato da tabela fato com todos os anos e meses
      tbl_fato <- setDT(bd[,c("chave","id_orgao_julgador","id_origem","sigla_tribunal","ramo_justica",
                              "sigla_grau","id_formato","id_procedimento","procedimento","originario")]) %>%
        unique %>%
        as.data.frame
      
      tbl_fato <- tbl_fato %>% full_join(tempo %>% dplyr::mutate(anomes = str_sub(id,1,6)) %>% dplyr::select(anomes, ano, mes, ultimo_dia), 
                                         by=character()) %>%
                                dplyr::mutate(chave_ano = paste(chave,anomes,sep="_"))
      
      ag_fato = colnames(tbl_fato)
      
      tbl_fato <- tbl_fato %>%
        left_join(func.contar.anomes(bd, "dt_recebimento", "CN", salvar.csv = salvar.csvs, nome.quebra = nome.parte), by = "chave_ano") %>%
        left_join(func.contar.entre.datas(bd, "dt_pendente", c("CP","TPCP_Dias"), ind_tpcp=T, variavel_tot = "data_total_pendente", nome.quebra = nome.parte), by = "chave_ano") %>%
        left_join(func.contar.entre.datas(bd, "dt_pendente", "CPJulg", filtrar.julg=T, nome.quebra = nome.parte), by = "chave_ano") %>%
        left_join(func.contar.anomes(bd, "dt_baixa", c("TBaix","TPBaix_Dias","TPBaix_Proc","tpbaixst","tpbaixp"),
                                     ind_tp = T, variavel_tp = "tp_baix_st", salvar.csv = salvar.csvs , nome.quebra = nome.parte), by = "chave_ano") %>%
        left_join(func.contar.anomes(bd, "dt_arquivamento", c("TBaixArq","TPArq_Dias","TPArq_Proc","tparqst","tparqp"),
                                     ind_tp = T, variavel_tp = "tp_arq_st", nome.quebra = nome.parte), by = "chave_ano") %>%
        left_join(func.contar.anomes(bd, "dt_primeiro_julgamento", c("SentPri","TPSent_Dias","TPSent_Proc","tpsentst","tpsentp"),
                                     ind_tp = T, variavel_tp = "tp_julg_st"), by = "chave_ano") %>%
        left_join(func.contar.entre.datas(bd, "dt_pendente_liquido", c("CPL","TPCPL_Dias"), ind_tpcp=T, variavel_tot = "data_total_pendente_liquido", salvar.csv = salvar.csvs , nome.quebra = nome.parte), by = "chave_ano") %>%
        left_join(func.contar.entre.datas(bd, "dt_pendente_liquido", "CPLJulg", filtrar.julg=T, nome.quebra = nome.parte), by = "chave_ano") %>%
        left_join(func.contar.entre.datas(bd, "dt_concluso", "Concl", concluso = T,  nome.quebra = nome.parte), by = "chave_ano") %>%
        left_join(func.contar.datas(bd, "dt_julgamento", "Sent",  salvar.csv = salvar.csvs , nome.quebra = nome.parte), by = "chave_ano") %>%
        left_join(func.contar.datas(bd, "dt_julgamento_merito", "SentCM"), by = "chave_ano") %>%
        left_join(func.contar.datas(bd, "dt_julgamento_sem_merito", "SentSM"), by = "chave_ano") %>%
        left_join(func.contar.datas(bd, "dt_julg_decisao_ed", "SentDecED"), by = "chave_ano") %>%
        left_join(func.contar.datas(bd, "dt_julgamento_homologatorio", "SentH",  nome.quebra = nome.parte), by = "chave_ano") %>%
        left_join(func.contar.anomes(bd, "dt_primeiro_julgamento_homologatorio", "SentH1st"), by = "chave_ano") %>%
        left_join(func.contar.anomes(bd, "dt_remessa", "RSup"), by = "chave_ano") %>%
        left_join(func.contar.datas(bd, "dt_despacho", "Despac", recorte.cn = F), by = "chave_ano") %>%
        left_join(func.contar.datas(bd, "dt_decisao", "DecInt", recorte.cn = F), by = "chave_ano") %>%
        left_join(func.contar.datas(bd, "dt_audiencia", "Aud", recorte.cn = F), by = "chave_ano") %>%
        left_join(func.contar.datas(bd, "dt_audiencia_conc", "AudConc", recorte.cn = F), by = "chave_ano") %>%
        left_join(func.contar.datas(bd, "dt_liminar_deferida", "LimDef", recorte.cn = F), by = "chave_ano") %>%
        left_join(func.contar.datas(bd, "dt_liminar_indeferida", "LimIndef", recorte.cn = F), by = "chave_ano") %>%
        left_join(func.contar.anomes(bd, "dt_redistribuido_entrada", "PRedE"), by = "chave_ano") %>%
        left_join(func.contar.anomes(bd, "dt_redistribuido_saida", "PRedS"), by = "chave_ano") %>%
        left_join(func.contar.anomes(bd, "dt_reativacao", "Reat"), by = "chave_ano") %>%
        left_join(func.contar.anomes(bd, "dt_reativacao_fim", "ReatS"), by = "chave_ano") %>%
        left_join(func.contar.datas(bd, "dt_rec_inc_novo", "RInt"), by = "chave_ano") %>%
        left_join(func.contar.entre.datas(bd, "dt_rec_inc_pendente", "RIntP"), by = "chave_ano") %>%
        left_join(func.contar.datas(bd, "dt_rec_inc_julgado", "RIntJ"), by = "chave_ano") %>%
        left_join(func.contar.datas(bd, "dt_primeira_medida_protetiva", "MedProt1st"), by = "chave_ano") %>%
        left_join(func.contar.anomes(bd, "dt_primeira_pronuncia", c("Pronuncia1st","TPPronun_Dias","TPPronun_Proc","tppronst","tppronp"),
                                     ind_tp = T, variavel_tp = "tp_pronuncia_st"), by = "chave_ano") %>%
        left_join(func.contar.anomes(bd, "dt_primeira_sessao_juri_designada", c("JuriDesig1st","TPJuriDesig_Dias","TPJuriDesig_Proc","tpjuridesigst","tpjuridesigp"),
                                     ind_tp = T, variavel_tp = "tp_juri_desig_st"), by = "chave_ano") %>%
        left_join(func.contar.anomes(bd, "dt_primeira_sessao_juri_realizada", c("JuriRealiz1st","TPJuriRealiz_Dias","TPJuriRealiz_Proc","tpjurirealizst","tpjurirealizp"),
                                     ind_tp = T, variavel_tp = "tp_juri_realiz_st"), by = "chave_ano") %>%
        left_join(func.premio.antigos(bd, ano.ingresso=2021), by = "chave_ano") %>%
        left_join(
          bd %>% dplyr::select(chave, sem_tramitacao, proc_cn) %>%
            dplyr::filter(!is.na(sem_tramitacao) & sem_tramitacao > 100 & proc_cn == 1) %>%
            dplyr::mutate(chave_ano = paste(chave,str_sub(gsub("-","",max(tempo$ultimo_dia)),1,6), sep="_")) %>%
            dplyr::select(chave_ano) %>%
            plyr::count() %>% dplyr::rename(SemMov100Dias = freq), by = "chave_ano") %>%
        left_join(
          bd %>% dplyr::select(chave, sem_conclusao, proc_cn) %>%
            dplyr::filter(!is.na(sem_conclusao) & sem_conclusao > 100 & proc_cn == 1) %>%
            dplyr::mutate(chave_ano = paste(chave,str_sub(gsub("-","",max(tempo$ultimo_dia)),1,6), sep="_")) %>%
            dplyr::select(chave_ano) %>%
            plyr::count() %>% dplyr::rename(Concl100Dias = freq), by = "chave_ano") %>%
        left_join(
          bd %>% dplyr::select(chave, antigos, proc_cn) %>%
            dplyr::filter(!is.na(antigos) & proc_cn == 1) %>%
            arrange(-antigos) %>%
            slice(1:round(nrow(.)*0.05)) %>%
            dplyr::mutate(chave_ano = paste(chave,str_sub(gsub("-","",max(tempo$ultimo_dia)),1,6), sep="_")) %>%
            dplyr::group_by(chave_ano) %>%
            dplyr::summarise(Antigos5Porc = n(),
                             Antigos5Porc_Min = min(antigos, na.rm=T),
                             Antigos5Porc_Max = max(antigos, na.rm=T)), by = "chave_ano") %>%
        left_join(
          bd %>% dplyr::select(chave, antigos, proc_cn,id_procedimento) %>%
            dplyr::filter(!is.na(antigos) & id_procedimento %in% 1:2) %>%
            arrange(-antigos) %>%
            slice(1:round(nrow(.)*0.05)) %>%
            dplyr::mutate(chave_ano = paste(chave,str_sub(gsub("-","",max(tempo$ultimo_dia)),1,6), sep="_")) %>%
            dplyr::group_by(chave_ano) %>%
            dplyr::summarise(Antigos5PorcConhec = n(),
                             Antigos5PorcConhec_Min = min(antigos, na.rm=T),
                             Antigos5PorcConhec_Max = max(antigos, na.rm=T)), by = "chave_ano") %>%
        dplyr::mutate(TPCPL_Proc = CPL,
                      TPCP_Proc = CP,
                      sus = case_when(CP - CPL >= 0 & !is.na(CP) ~ CP - replace_na(CPL,0)),
                      sumInds = rowSums(dplyr::select(., CN:last_col()), na.rm=T)) %>%
        dplyr::filter(sumInds != 0)
      

      juntar.fatos <- bind_rows(juntar.fatos, tbl_fato)
      
      # Salva tabela fato
      if(partes == 1){
          data.table::fwrite(tbl_fato, paste0("2)Nova_tabela_fato_",trib,".csv"), sep = ";")

        
        # Tempo de cálculo dos indicadores
        tt <- data.frame(tt, calcular_indicadores = round(difftime(Sys.time(), tp.trib, units = "mins"),2))
        print(paste0(trib,": Calcular indicadores",nome.parte,": ", tt$calcular_indicadores, " minutos."))
        
        
      } else {
        print(paste0(trib,": Calcular indicadores",nome.parte,": ", round(difftime(Sys.time(), tp.trib, units = "mins"),2), " minutos."))
        data.table::fwrite(tbl_fato, paste0("2)Parcial_fato_",trib,nome.parte,".csv"), sep = ";")
      }


      ############# SALVA TABELA FATO DE CLASSES ######################### 

      if(salvar.classes){
      
      ############# CÁLCULO DOS INDICADORES #########################
      
      # Cria o formato da tabela fato com todos os anos e meses
      tbl_fato <- setDT(bd[,c("chave_classe","id_orgao_julgador","id_origem","sigla_tribunal",
                              "ramo_justica","sigla_grau","id_formato","procedimento","originario",
                              "id_ultima_classe")]) %>%
        filter(!is.na(id_ultima_classe)) %>%
        unique %>%
        as.data.frame %>%
        dplyr::rename(id_classe = id_ultima_classe)
      
      list.tp <- as.numeric(gsub("-","",tempo$ultimo_dia))
      max.dt.ult.dia <- max(list.tp)
      max.data.ult.dia <- ymd(max.dt.ult.dia)
      max.dt.ult.dia_1 <- as.numeric(gsub("-","",max.data.ult.dia+1))
      max.ano <- year(max.data.ult.dia)
      max.mes <- month(max.data.ult.dia)
      
      tbl_fato <- tbl_fato %>%
        full_join(tempo %>% dplyr::mutate(anomes = str_sub(id,1,6),
                                          anomes = case_when(as.numeric(anomes) > as.numeric(paste0(max.ano-1,str_pad(max.mes,2,"left","0"))) ~ 
                                                               paste0(str_sub(anomes,1,4),"01"),
                                                             TRUE ~ paste0(str_sub(anomes,1,4),"00")),
                                          ano = as.numeric(str_sub(anomes,1,4)),
                                          p.12m = as.numeric(str_sub(anomes,6,6))) %>% 
                    dplyr::select(ano,p.12m,anomes) %>% distinct(),
                  by=character()) %>%
        dplyr::mutate(chave_classe_ano = paste(chave_classe,anomes,sep="_"))
      
      ag_classe = colnames(tbl_fato)
      
      tbl_fato <- tbl_fato %>%
        left_join(func.contar.anomes(bd, "dt_recebimento", "ind1", r.classes = salvar.classes), by = c("chave_classe_ano" = "chave_ano")) %>%
        left_join(func.contar.entre.datas(bd, "dt_pendente", c("ind2","ind19_dias"), ind_tpcp=T, variavel_tot = "data_total_pendente", r.classes = salvar.classes), by = c("chave_classe_ano" = "chave_ano")) %>%
        left_join(func.contar.anomes(bd, "dt_baixa", c("ind3","ind16_dias","ind16_proc","tpbaixst","tpbaixp"), 
                                     ind_tp = T, variavel_tp = "tp_baix_st", r.classes = salvar.classes), by = c("chave_classe_ano" = "chave_ano")) %>%
        left_join(func.contar.anomes(bd, "dt_primeiro_julgamento", c("ind8a1","ind17_dias","ind17_proc","tpsentst","tpsentp"), 
                                     ind_tp = T, variavel_tp = "tp_julg_st", r.classes = salvar.classes), by = c("chave_classe_ano" = "chave_ano")) %>%
        left_join(func.contar.entre.datas(bd, "dt_pendente_liquido", c("ind5","ind18_dias"), ind_tpcp=T, variavel_tot = "data_total_pendente_liquido", r.classes = salvar.classes), by = c("chave_classe_ano" = "chave_ano")) %>%
        left_join(func.contar.datas(bd, "dt_julgamento", "ind8a", r.classes = salvar.classes), by = c("chave_classe_ano" = "chave_ano")) %>%
        left_join(func.contar.anomes(bd, "dt_redistribuido_entrada", "ind20", r.classes = salvar.classes), by = c("chave_classe_ano" = "chave_ano")) %>%
        left_join(func.contar.anomes(bd, "dt_redistribuido_saida", "ind21", r.classes = salvar.classes), by = c("chave_classe_ano" = "chave_ano")) %>%
          left_join(
            bd %>% dplyr::select(chave, antigos) %>%
              dplyr::filter(!is.na(antigos)) %>%
              arrange(-antigos) %>%
              slice(1:round(nrow(.)*0.05)) %>%
              dplyr::mutate(chave_ano = paste(chave,str_sub(gsub("-","",max(tempo$ultimo_dia)),1,6), sep="_")) %>%
              dplyr::group_by(chave_ano) %>%
              dplyr::summarise(ind15total = n(),
                               ind15min = min(antigos, na.rm=T),
                               ind15max = max(antigos, na.rm=T)), by = c("chave_classe_ano" = "chave_ano")) %>%
                dplyr::mutate(ind18_proc = ind5,
                              ind19_proc = ind2,
                              sumInds = rowSums(dplyr::select(., contains("ind")), na.rm=T)) %>%
                dplyr::filter(sumInds != 0)
          
      juntar.classes <- bind_rows(juntar.classes, tbl_fato)
      
      # Salva tabela fato
      if(partes ==1) {
            data.table::fwrite(tbl_fato, paste0("3)Nova_tbl_classe_",trib,".csv"), sep = ";")
        tt <- data.frame(tt, tempo_classes = round(difftime(Sys.time(), tp.trib, units = "mins"),2))
        print(paste0(trib,": Tempo classes",": ", tt$tempo_classes, " minutos."))
      } 
      else {
        print(paste0(trib,": Tempo classes",nome.parte,": ", round(difftime(Sys.time(), tp.trib, units = "mins"),2), " minutos."))
            data.table::fwrite(tbl_fato, paste0("3)Parcial_tbl_classe_",trib,nome.parte,".csv"), sep = ";")
      }
    }
    
      ############# SALVA TABELA FATO DE ASSUNTOS ######################### 

      if(salvar.assuntos){

      ############# CÁLCULO DOS INDICADORES #########################
      l.assuntos= gsub("\\{|\\}","", gsub("\\{\\}","",bd$id_assunto)) %>%
        str_split(",")
      
      names(l.assuntos) <- paste(bd$chave, bd$numero, sep="_")
      l.assuntos=setNames(unlist(l.assuntos, use.names=F),rep(names(l.assuntos), lengths(l.assuntos)))
      l.assuntos=data.frame(chave.proc = names(l.assuntos), l.assuntos, stringsAsFactors = F)
      
      bd.a <- l.assuntos %>% 
        filter(!is.na(as.numeric(l.assuntos))) %>%
        left_join(bd %>% dplyr::mutate(chave.proc = paste(chave, numero, sep="_")), by = "chave.proc") %>%
        dplyr::mutate(chave_assunto = paste(chave, l.assuntos, sep = "_"))
      
      # Cria o formato da tabela fato com todos os anos e meses
      tbl_fato <- setDT(bd.a[,c("chave_assunto","id_orgao_julgador","id_origem","sigla_tribunal",
                                "ramo_justica","sigla_grau","id_formato","procedimento","originario",
                                "l.assuntos")]) %>%
                          unique %>%
                          as.data.frame %>%
                          dplyr::rename(id_assunto = l.assuntos) %>%
                          dplyr::mutate(id_assunto = as.numeric(id_assunto))
      
      tbl_fato <- tbl_fato %>%
        full_join(tempo %>% dplyr::mutate(anomes = str_sub(id,1,6),
                                          anomes = case_when(as.numeric(anomes) > as.numeric(paste0(max.ano-1,str_pad(max.mes,2,"left","0"))) ~ 
                                                               paste0(str_sub(anomes,1,4),"01"),
                                                             TRUE ~ paste0(str_sub(anomes,1,4),"00")),
                                          ano = as.numeric(str_sub(anomes,1,4)),
                                          p.12m = as.numeric(str_sub(anomes,6,6))) %>% 
                    dplyr::select(ano,p.12m,anomes) %>% distinct(),
                  by=character()) %>%
        dplyr::mutate(chave_assunto_ano = paste(chave_assunto,anomes,sep="_"))
      
      ag_assunto = colnames(tbl_fato)
      
      tbl_fato <- tbl_fato %>%
        left_join(func.contar.anomes(bd.a, "dt_recebimento", "ind1", r.assuntos = salvar.assuntos), by = c("chave_assunto_ano" = "chave_ano")) %>%
        left_join(func.contar.entre.datas(bd.a, "dt_pendente", c("ind2","ind19_dias"), ind_tpcp=T, variavel_tot = "data_total_pendente", r.assuntos = salvar.assuntos), by = c("chave_assunto_ano" = "chave_ano")) %>%
        left_join(func.contar.anomes(bd.a, "dt_baixa", c("ind3","ind16_dias","ind16_proc","tpbaixst","tpbaixp"), 
                                     ind_tp = T, variavel_tp = "tp_baix_st", r.assuntos = salvar.assuntos), by = c("chave_assunto_ano" = "chave_ano")) %>%
        left_join(func.contar.anomes(bd.a, "dt_primeiro_julgamento", c("ind8a1","ind17_dias","ind17_proc","tpsentst","tpsentp"), 
                                     ind_tp = T, variavel_tp = "tp_julg_st", r.assuntos = salvar.assuntos), by = c("chave_assunto_ano" = "chave_ano")) %>%
        left_join(func.contar.entre.datas(bd.a, "dt_pendente_liquido", c("ind5","ind18_dias"), ind_tpcp=T, variavel_tot = "data_total_pendente_liquido", r.assuntos = salvar.assuntos), by = c("chave_assunto_ano" = "chave_ano")) %>%
        left_join(func.contar.datas(bd.a, "dt_julgamento", "ind8a", r.assuntos = salvar.assuntos), by = c("chave_assunto_ano" = "chave_ano")) %>%
        left_join(func.contar.anomes(bd.a, "dt_redistribuido_entrada", "ind20", r.assuntos = salvar.assuntos), by = c("chave_assunto_ano" = "chave_ano")) %>%
        left_join(func.contar.anomes(bd.a, "dt_redistribuido_saida", "ind21", r.assuntos = salvar.assuntos), by = c("chave_assunto_ano" = "chave_ano")) %>%
        left_join(
          bd.a %>% dplyr::select(chave, antigos) %>%
            dplyr::filter(!is.na(antigos)) %>%
            arrange(-antigos) %>%
            slice(1:round(nrow(.)*0.05)) %>%
            dplyr::mutate(chave_ano = paste(chave,str_sub(gsub("-","",max(tempo$ultimo_dia)),1,6), sep="_")) %>%
            dplyr::group_by(chave_ano) %>%
            dplyr::summarise(ind15total = n(),
                             ind15min = min(antigos, na.rm=T),
                             ind15max = max(antigos, na.rm=T)), by = c("chave_assunto_ano" = "chave_ano")) %>%
          dplyr::mutate(
            ind18_proc = ind5,
            ind19_proc = ind2,
            sumInds = rowSums(dplyr::select(., contains("ind")), na.rm=T)) %>%
          dplyr::filter(sumInds != 0)
        
      juntar.assuntos <- bind_rows(juntar.assuntos, tbl_fato)
      
      # Salva tabela fato
      if(partes == 1) {
            data.table::fwrite(tbl_fato, paste0("4)Nova_tbl_assunto_",trib,".csv"), sep = ";")
        tt <- data.frame(tt, tempo_assuntos = round(difftime(Sys.time(), tp.trib, units = "mins"),2))
        print(paste0(trib,": Tempo assuntos",": ", tt$tempo_assuntos, " minutos."))
      } else {
        print(paste0(trib,": Tempo assuntos",nome.parte,": ", round(difftime(Sys.time(), tp.trib, units = "mins"),2), " minutos."))
            data.table::fwrite(tbl_fato, paste0("4)Parcial_tbl_assunto_",trib,nome.parte,".csv"), sep = ";")
      }
      rm(bd.a)
    }
    
    rm(bd)
    gc()
    
    if(partes == 1){
      tt <- data.frame(tt, tempo_total = round(difftime(Sys.time(), tp.trib, units = "mins"),2),versao = R.Version()$version.string)
      print(paste0(trib,": Tempo total",": ", tt$tempo_total, " minutos."))
      data.table::fwrite(tt, paste0("100)Tempo_tblfato_",trib,nome.parte,".csv"), sep = ";")
    } else {
      print(paste0(trib,": Tempo total",nome.parte,": ", round(difftime(Sys.time(), tp.trib, units = "mins"),2), " minutos."))
      data.table::fwrite(data.frame(tt, tempo_total = round(difftime(Sys.time(), tp.trib, units = "mins"),2),versao = R.Version()$version.string), paste0("100)Tempo_tblfato_",trib,nome.parte,".csv"), sep = ";")
    }
  
  
    # Se for a última parte, agrupa as partes
    
    if(partes > 1 & parte == partes){
    
    if(p.parte > 1 & salvar.indicadores){
      juntar.fatos <- data.table::setDT(plyr::ldply(paste0("2)Parcial_fato_",trib,"_",1:partes,"-",partes,".csv"),
                                                    fread, sep=";", stringsAsFactors=F)) %>% as.data.frame
    }
    
    if(!exists("ag_fato")) ag_fato <- c("chave","id_orgao_julgador","id_origem","sigla_tribunal","ramo_justica","sigla_grau","id_formato","id_procedimento","procedimento","originario","anomes","ano","mes","ultimo_dia","chave_ano")
    
    if(nrow(juntar.fatos)>0 & !is.null(juntar.fatos)){
      juntar.fatos <- juntar.fatos %>%
        group_by_at(ag_fato) %>%
        summarise_all(sum, na.rm=T)
        data.table::fwrite(juntar.fatos, paste0("2)Nova_tabela_fato_",trib,".csv"), sep = ";")
    }
    
    rm(juntar.fatos)
    gc()
    

    # ag_classe=colnames(juntar.classes)[1:14]
    if(p.parte > 1 & salvar.classes){
      juntar.classes <- data.table::setDT(plyr::ldply(paste0("3)Parcial_tbl_classe_",trib,"_",1:partes,"-",partes,".csv"),
                                                      fread, sep=";", stringsAsFactors=F)) %>% as.data.frame
    }
    if(!exists("ag_classe")) ag_classe <- colnames(juntar.classes)[1:14]
    
    if(nrow(juntar.classes)> 0 & !is.null(juntar.classes)){
        juntar.classes <- setDT(juntar.classes)[, lapply(.SD, sum,na.rm=T), by=ag_classe] %>%
          as.data.frame
        data.table::fwrite(juntar.classes, paste0("3)Nova_tbl_classe_",trib,".csv"), sep = ";")
    }
    
    
    # ag_assunto=colnames(juntar.assuntos)[1:14]
    if(p.parte > 1 & salvar.assuntos){
      juntar.assuntos <- data.table::setDT(plyr::ldply(paste0("4)Parcial_tbl_assunto_",trib,"_",1:partes,"-",partes,".csv"),
                                                       fread, sep=";", stringsAsFactors=F)) %>% as.data.frame
    }
    
    if(!exists("ag_assunto")) ag_assunto <- colnames(juntar.assuntos)[1:14]
    
    if(nrow(juntar.assuntos)> 0 & !is.null(juntar.assuntos)){
      juntar.assuntos <- setDT(juntar.assuntos)[, lapply(.SD, sum,na.rm=T), by=ag_assunto] %>%
        as.data.frame
          data.table::fwrite(juntar.assuntos, paste0("4)Nova_tbl_assunto_",trib,".csv"), sep = ";")
    }
    }
  }
  