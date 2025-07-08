oldw <- getOption("warn")
options(scipen = 999, warn = -1)

#BIBLIOTECAS

# Pacotes para manipulação e análise de dados

  
  # data.table
  if (!requireNamespace('data.table', quietly = T)) {install.packages('data.table')}
  library('data.table')
  
  # lubridate
  if (!requireNamespace('lubridate', quietly = T)) {install.packages('lubridate')}
  library('lubridate')
  
  # stringr
  if (!requireNamespace('stringr', quietly = T)) {install.packages('stringr')}
  library('stringr')
  
  # dplyr
  if (!requireNamespace('dplyr', quietly = T)) {install.packages('dplyr')}
  library('dplyr')
  
  # tidyr
  if (!requireNamespace('tidyr', quietly = T)) {install.packages('tidyr')}
  library('tidyr')

# Pacotes para manipulação de bancos de dados
  
  # RPostgres
  if (!requireNamespace('RPostgres', quietly = T)) {install.packages('RPostgres')}
  library('RPostgres')  


  #FUNÇÕES

# Função para conectar ao banco de dados

func.connect<- function(base, usuario, banco) 
  {
  dbConnect(RPostgres::Postgres(),
                      dbname = db,
                      host=host_db,
                      port=db_port,
                      user=db_user,
                      password=db_password)
                }


# Funcao para dividir banco de dados 
func.quebra <- function(tribunal, quebra){
  bd.quebra = dbGetQuery(con, paste0("select ",
                                     paste0("PERCENTILE_CONT(",((1:(quebra-1))/quebra),") WITHIN GROUP(ORDER BY id)", collapse= ", "),
                                     " FROM processo_",
                                     str_pad(toString(sprintf("%s", tribunais$id[tribunais$sigla==tribunal])),"0",width = 3, side="left")))
  return(round(as.numeric(bd.quebra),0))
}


# Funcao para extrar os dados do Painel de Estatística


func.extrair.painel <- function(tribunal, quebra = 0, parte=1, lista.processo = NULL){
  
  if(!is.null(lista.processo)) lista.processo <- str_pad(gsub("\\D", "",lista.processo),20,"left","0")
  
  query.processo <- paste0("WITH assuntos AS (
    select id, criminal, privativa_liberdade, nome, nivel FROM assunto WHERE criminal or privativa_liberdade  in ('S')), 
    classes AS (
    select id, criminal, privativa_liberdade, originario, novo FROM classe 
    where criminal or privativa_liberdade  in ('S','D', 'I') or originario = 'R' or novo = TRUE
    order by id
)
select sigla_tribunal, sigla_grau, 
               case when id_tipo_procedimento = 3 then 'fiscal'
                      when id_tipo_procedimento = 4 then 'nao fiscal'
                      when (id_fase = 1 and id_tipo_procedimento in (1,5,7)) or (
                      id_fase = 2 and id_tipo_procedimento in (1) and
                      id_classe_ultima_fase_2 IN (select id FROM classes where criminal = TRUE)
                      ) then 'conhecimento' 
                      when (id_fase =  1 and id_tipo_procedimento = 2) or (
                      id_fase = 2 and id_tipo_procedimento in (1,2,5,7)) then 'execucao'
                      when id_fase = 5 or id_tipo_procedimento = 9 then 'pre-processual'
                      when id_fase = 3 and id_tipo_procedimento in (1,5) then 'investigatoria'
                      when id_tipo_procedimento = 10 then 'administrativo'
                      else 'outros' end as procedimento, 
               id_formato,
		case when id_nivel_sigilo = 0 
			then 0 else 1 end as sigiloso,
		nome_sistema as sistema, pi.id_processo,
		concat(substring(numero,  1,7),'-',substring(numero,  8,2),'.',
      	substring(numero,  10,4),'.',substring(numero,  14,1),'.',substring(numero,  15,2),'.',
      	substring(numero,  17,4)) as numero,
    case when id_nivel_sigilo = 0 
			then concat(substring(numero,  1,7),'-',substring(numero,  8,2),'.',
      	substring(numero,  10,4),'.',substring(numero,  14,1),'.',substring(numero,  15,2),'.',
      	substring(numero,  17,4)) 
      else concat('sigiloso(',pi.id_processo,')') end as numero_sigilo,
      	id_orgao_julgador_ultimo as id_ultimo_oj, valor_causa,
      	id_classe, id_assunto, id_orgao_julgador as ids_orgao_julgador,
      	(t.doc ->> 'idOrgaoJulgador')::INT4 as id_orgao_julgador, recurso,
      	case when (array_agg((t.doc ->> 'idClasse')::INT4) && (select array_agg(id) FROM classes where criminal = TRUE)) then 1
      	     when (array_agg((t.doc ->> 'idClasse')::INT4) && (select array_agg(id) FROM classes where criminal = FALSE)) then 0
      	     when(id_assunto && (select array_agg(id) FROM assuntos)) then 1 else 0 END criminal,    
        case when array_agg((t.doc ->> 'idClasse')::INT4) && (select array_agg(id) FROM classes where privativa_liberdade = 'S')
         		or (
                  array_agg((t.doc ->> 'idClasse')::INT4) && (select array_agg(id) FROM classes where privativa_liberdade IN ('D', 'I'))
                and id_assunto && (select array_agg(id) FROM assuntos where privativa_liberdade = 'S')) 
              then 1 else 0 end privativa_liberdade,
        case when array_agg((t.doc ->> 'idClasse')::INT4) && (select array_agg(id) FROM classes where originario = 'R')
              then 0 else 1 end id_originario,
              coalesce(array_agg(distinct (t.doc ->> 'idClasse')::INT4)
				filter (WHERE (t.doc ->> 'idClasse')::INT4 IN (select id FROM classes where novo = TRUE)), 
				'{}') as id_classes_cn,  
              case when id_classe_ultima_fase_2 IS NULL AND id_classe_ultima_fase_1 IS NULL 
              then id_classe[array_length(id_classe, 1)] 
                  when id_classe_ultima_fase_2 IS NULL then id_classe_ultima_fase_1 
              else id_classe_ultima_fase_2 end as id_ultima_classe,
        max(data_ultima_movimentacao) as data_ultima_movimentacao, 
        min(data_ajuizamento) as data_ajuizamento,
         max(case when (id_tipo = 11 
 				AND (t.doc ->> 'dataFim')::INT4 = 0) 
 					then (t.doc ->> 'dataInicio')::INT4
      			else null end) as dt_ultima_conclusao,
      	max(case when (id_tipo = 15 
 				AND (t.doc ->> 'dataFim')::INT4 = 0) 
 					then (t.doc ->> 'dataInicio')::INT4
      			else null end) as dt_ultimo_pendente_liquido,

-- Indicador 1 - Casos Novos // #situacao 88 e que foi iniciada (id_situacao_iniciar) com 81, 9, 24, 26, 65, 91, 61 Indicadores 1 novo // #situacao 88 e que foi iniciada (id_situacao_iniciar) com 81, 9, 24, 26, 65, 91, 61
 		min(case when (id_tipo IN (1,8) 
 				AND (t.doc ->> 'dataInicio')::INT4 > 0 
 					AND (t.doc ->> 'idSituacaoIniciar')::INT4 IN (9, 24, 26, 61, 65, 81, 91)
            ) then  (t.doc ->> 'dataInicio')::INT4
      			else NULL 
      	end) as dt_recebimento,

-- Indicador 2 - Processos Pendentes // Situacao 88, id_situacao_iniciar e id_situacao_finalizar 
		coalesce(array_agg(DISTINCT concat(t.doc ->> 'dataInicio',':',t.doc ->> 'dataFim')) 
				filter (WHERE id_tipo in (1,6,8,18) 
						AND (t.doc ->> 'dataInicio')::INT4 > 0), 
				'{}' ) as dt_pendente,
				
						coalesce(array_agg(DISTINCT concat(t.doc ->> 'dataInicio',':',t.doc ->> 'dataFim')) 
				filter (WHERE id_tipo in (1,6,8,18) 
            AND (t.doc ->> 'idSituacaoIniciar')::INT4 NOT IN (38)
						AND (t.doc ->> 'dataInicio')::INT4 > 0), 
				'{}' ) as dt_pendente_nremetido,

-- Indicador 3  - Processos Baixados // #situacao 2, 10, 23 e 41 ou situacao 88 finalizada que nao foi finalizada (id_situacao_finalizar) com 9, 40, 81, 88 118, 120
      	min(case when id_tipo in (4,5) 
      			AND dt_primeiro_inicio > 0 
      			AND pi.id_grau NOT IN (2,5) 
      		then dt_primeiro_inicio 
        when id_tipo in (1,6,8,18) 
				AND (t.doc ->> 'dataFim')::INT4 > 0 
                AND (((pi.id_grau IN (2,5)) 
                	AND (t.doc ->> 'idSituacaoFinalizar')::INT4 IN (2, 10, 23, 41,65, 26, 91))
                	OR ((pi.id_grau NOT IN (2,5) AND pi.id_fase NOT IN (2) AND NOT (
                	pi.id_fase IN (1) AND id_classe_ultima_fase_2 IN (select id FROM classes where criminal = TRUE)
                	))
                    AND (t.doc ->> 'idSituacaoFinalizar')::INT4 IN (65, 26, 91))) 
           	then (t.doc ->> 'dataFim')::INT4 + 1
           	else NULL 
       	end) as dt_baixa,
       	min(case when id_tipo in (4) 
      		AND (t.doc ->> 'idSituacao')::INT4 IN (41)
      		AND (t.doc ->> 'dataInicio')::INT4 > 0
      		then (t.doc ->> 'dataInicio')::INT4
      		end) as dt_remessa,
      		min(case when id_tipo in (5) 
      			AND dt_primeiro_inicio > 0 
      		then dt_primeiro_inicio 
      		end) as dt_arquivamento,

-- Indicador 5 - Processos Pendentes Liquidos // Situacao 15, id_situacao_iniciar e id_situacao_finalizar 
		coalesce(array_agg(DISTINCT concat(t.doc ->> 'dataInicio',':',t.doc ->> 'dataFim')) 
				filter (WHERE id_tipo in (15) 
						AND (t.doc ->> 'dataInicio')::INT4 > 0), 
				'{}' ) as dt_pendente_liquido,
		coalesce(array_agg(DISTINCT concat(t.doc ->> 'dataInicio',':',t.doc ->> 'dataFim')) 
				filter (WHERE id_tipo in (15) 
						AND (t.doc ->> 'idSituacaoIniciar')::INT4 NOT IN (38)
						AND (t.doc ->> 'dataInicio')::INT4 > 0), 
				'{}' ) as dt_pendente_liquido_nremetido,

-- Indicador 6 - Processos Conclusos // Situacao da hierarquia 12
		coalesce(array_agg(DISTINCT concat(t.doc ->> 'dataInicio',':',t.doc ->> 'dataFim',':',t.doc ->> 'idSituacao')) 
				filter (WHERE id_tipo = 11 
 				AND (t.doc ->> 'dataInicio')::INT4 > 0), 
				'{}' ) as dt_concluso,

-- Indicador 8 - Processos Julgados // Situacoes das hierarquias  27, 28 e 62
      	coalesce(array_agg(DISTINCT (t.doc ->> 'dataInicio')::INT4) 
				filter (WHERE id_tipo = 2 
 				AND (t.doc ->> 'dataInicio')::INT4 > 0), 
				'{}' ) as dt_julgamento,
				coalesce(array_agg(DISTINCT concat(t.doc ->> 'dataInicio',':',t.doc ->> 'idSituacao',':',t.doc ->> 'idMovimento')) 
				filter (WHERE id_tipo = 2 
 				AND (t.doc ->> 'dataInicio')::INT4 > 0), 
				'{}' ) as dt_julgamento_movimento,

-- Indicador 8b - Processos Julgados com merito // Situacoes das hierarquias  27
      	coalesce(array_agg(DISTINCT (t.doc ->> 'dataInicio')::INT4) 
				filter (WHERE id_tipo = 2 
 				AND (t.doc ->> 'dataInicio')::INT4 > 0
 				and (t.doc ->> 'idSituacao')::INT4 in (27,18,29,90,129)), 
				'{}' ) as dt_julgamento_merito,

-- Indicador 8c - Processos Julgados sem merito // Situacoes das hierarquias  28
     	coalesce(array_agg(DISTINCT (t.doc ->> 'dataInicio')::INT4) 
				filter (WHERE id_tipo = 2 
 				AND (t.doc ->> 'dataInicio')::INT4 > 0 
 				and (t.doc ->> 'idSituacao')::INT4 in (28,72)), 
				'{}' ) as dt_julgamento_sem_merito,

-- Indicador 8d - Processos Homologatorios Julgados // Situacoes das hierarquias  29
				     	coalesce(array_agg(DISTINCT (t.doc ->> 'dataInicio')::INT4) 
				filter (WHERE id_tipo = 2 
 				AND (t.doc ->> 'dataInicio')::INT4 > 0 
 				and (t.doc ->> 'idSituacao')::INT4 in (29)), 
				'{}' ) as dt_julgamento_homologatorio,

-- Indicador 17 - Primeiro Julgamento // Situacoes das hierarquias  27, 28 e 62
      	min(case when id_tipo = 2 
 				AND (t.doc ->> 'dataInicio')::INT4 > 0
 				then (t.doc ->> 'dataInicio')::INT4
 				else null
				end) as dt_primeiro_julgamento,

-- Indicador 17a - Primeiro Julgamento com merito // Situacao da hierarquia  27
      	min(case when id_tipo = 2 
      	and (t.doc ->> 'idSituacao')::INT4 in (27,18,29,90,129) 
 				AND (t.doc ->> 'dataInicio')::INT4 > 0
 				then (t.doc ->> 'dataInicio')::INT4
 				else null
				end) as dt_primeiro_julgamento_merito,

-- Indicador 17 - Primeiro Julgamento Homologatorio // Situacao 29
      	min(case when id_tipo = 2 
      	and (t.doc ->> 'idSituacao')::INT4 in (29)
 				AND (t.doc ->> 'dataInicio')::INT4 > 0
 				then (t.doc ->> 'dataInicio')::INT4
 				else null
				end) as dt_primeiro_julgamento_homologatorio,

-- Indicador 9 - Despachos // Situacao da hierarquia 21
      	coalesce(array_agg(DISTINCT (t.doc ->> 'dataInicio')::INT4) 
				filter (WHERE (id_tipo in (14)
				OR (id_tipo in (3) AND (t.doc ->> 'idSituacao')::INT4 in (45))
				OR (id_tipo in (7) AND (t.doc ->> 'idSituacao')::INT4 in (107))
				)
 				AND (t.doc ->> 'dataInicio')::INT4 > 0), 
				'{}' ) as dt_despacho,

-- Indicador 10 - Decisoes // Situacao da hierarquia 17      	
      	coalesce(array_agg(DISTINCT (t.doc ->> 'dataInicio')::INT4) 
				filter (WHERE (id_tipo in (13, 17)
				OR (id_tipo in (12) AND (t.doc ->> 'idSituacao')::INT4 not in (15))
				OR (id_tipo in (3) AND (t.doc ->> 'idSituacao')::INT4 not in (45))
				OR (id_tipo in (7) AND (t.doc ->> 'idSituacao')::INT4 in (106))
				)
 				AND (t.doc ->> 'dataInicio')::INT4 > 0), 
				'{}' ) as dt_decisao,

-- Indicador 11 - Audiencias realizadas // Situacoes 6, 8 e 44      	     	
      	coalesce(array_agg(DISTINCT (t.doc ->> 'dataInicio')::INT4) 
				filter (WHERE id_tipo in (10) 
 				AND (t.doc ->> 'dataInicio')::INT4 > 0), 
				'{}' ) as dt_audiencia,

-- Indicador 12 - Audiencias conciliatorias realizadas // Situacao 6	
      	coalesce(array_agg(DISTINCT (t.doc ->> 'dataInicio')::INT4) 
				filter (WHERE id_tipo in (10) 
 				AND (t.doc ->> 'dataInicio')::INT4 > 0
 				and (t.doc ->> 'idSituacao')::INT4 in (6)), 
				'{}' ) as dt_audiencia_conc,

-- Indicador 13 - Liminares deferidas e indeferidas // Situacao 33, 89	      	
      	coalesce(array_agg(DISTINCT (t.doc ->> 'dataInicio')::INT4) 
				filter (WHERE id_tipo in (13) 
 				AND (t.doc ->> 'dataInicio')::INT4 > 0
 				and (t.doc ->> 'idSituacao')::INT4 in (33)), 
				'{}' ) as dt_liminar_deferida,    	
      	coalesce(array_agg(DISTINCT (t.doc ->> 'dataInicio')::INT4) 
				filter (WHERE id_tipo in (13) 
 				AND (t.doc ->> 'dataInicio')::INT4 > 0
 				and (t.doc ->> 'idSituacao')::INT4 in (89)), 
				'{}' ) as dt_liminar_indeferida,      	

-- Indicador 20 - Processos redistribuidos de entrada // 88, desde que seja iniciada pelas situacoes 40, 118, 119, 120, 130, 131, 153	      	
       	min(case when id_tipo in (18) 
				AND (t.doc ->> 'dataInicio')::INT4 > 0  
           	then (t.doc ->> 'dataInicio')::INT4
           	else NULL 
       	end) as dt_redistribuido_entrada,

-- Indicador 21 - Processos redistribuidos de saida // 134 ou 88, desde que seja finalizada pelas situacoes 40, 118, 119, 130, 134, 153, 154      	
       	max(case when id_tipo in (1,6,8,15,18) 
				AND (t.doc ->> 'dataFim')::INT4 > 0 
                AND (t.doc ->> 'idSituacaoFinalizar')::INT4 IN (40, 118,119,130,134,153,154) 
           	then (t.doc ->> 'dataFim')::INT4 + 1
           	else NULL 
       	end) as dt_redistribuido_saida,

-- Indicador 22 - Processo Reativado de entrada // Situacao 37   	
      	min(case when id_tipo IN (6) 
 				AND (t.doc ->> 'dataInicio')::INT4 > 0 
      		then  (t.doc ->> 'dataInicio')::INT4
      		else NULL 
      	end) as dt_reativacao,

-- Indicador 23 - Processo Reativado de saida // Situacao 37   	
      	max(case when id_tipo IN (6) 
 					AND (t.doc ->> 'dataFim')::INT4 = 0 
      			then  99999999
      			when id_tipo IN (6) 
 					AND (t.doc ->> 'dataFim')::INT4 > 0
 					AND (t.doc ->> 'idSituacaoFinalizar')::INT4 IN (2, 10, 23, 41)
      			then  (t.doc ->> 'dataFim')::INT4 + 1
      		else NULL 
      	end) as dt_reativacao_fim,

-- Indicador 24 - Recurso Interno Novo // Situacao 39        	
      	coalesce(array_agg(DISTINCT (t.doc ->> 'dataInicio')) 
				filter (WHERE id_tipo in (19)
 				AND (t.doc ->> 'dataInicio')::INT4 > 0), 
				'{}' ) as dt_rec_inc_novo,                       

-- Indicador 25 - Recurso Interno Pendente // Situacao 39        	
      	coalesce(array_agg(DISTINCT concat(t.doc ->> 'dataInicio',':',t.doc ->> 'dataFim')) 
				filter (WHERE id_tipo in (19) 
 				AND (t.doc ->> 'dataInicio')::INT4 > 0), 
				'{}' ) as dt_rec_inc_pendente,                       

-- Indicador 26 - Recurso Interno Julgado // Situacao 39        	
      	coalesce(array_agg(DISTINCT (t.doc ->> 'dataFim')) 
				filter (WHERE id_tipo in (19) 
 				AND (t.doc ->> 'dataInicio')::INT4 > 0), 
				'{}' ) as dt_rec_inc_julgado,

-- Indicador 27 - Julgados e decididos em embargo de declaracao // Situacao 15        	
      	coalesce(array_agg(DISTINCT (t.doc ->> 'dataInicio')) 
				filter (WHERE id_tipo in (12) 
				AND (t.doc ->> 'idSituacao')::INT4 in (15)
 				AND (t.doc ->> 'dataInicio')::INT4 > 0), 
				'{}' ) as dt_julg_decisao_ed,

-- Indicador 28 - Concessao ou denegacao da medida protetiva, // Situacoes 17 e 34, movimentos 11423,11424,11425,12476,12479        	
      	min(case when id_tipo = 12 
      	and (t.doc ->> 'idSituacao')::INT4 in (17,34) 
        AND (t.doc ->> 'idMovimento')::INT4 in (11423,11424,11425,12476,12479)
 				AND (t.doc ->> 'dataInicio')::INT4 > 0
 				then (t.doc ->> 'dataInicio')::INT4
 				else null
				end) as dt_primeira_medida_protetiva,
				coalesce(array_agg(DISTINCT (t.doc ->> 'dataInicio')) 
				filter (WHERE id_tipo in (12) 
				and (t.doc ->> 'idSituacao')::INT4 in (17,34) 
        AND (t.doc ->> 'idMovimento')::INT4 in (11423,11424,11425,12476,12479)
 				AND (t.doc ->> 'dataInicio')::INT4 > 0),
				'{}' ) as dt_medida_protetiva,
				
-- Indicador 29 - Sessão do Júri, // Situações 43, 44 e 87, Movimento 313        	
				coalesce(array_agg(DISTINCT concat(t.doc ->> 'dataInicio',':',t.doc ->> 'dataFim',':',t.doc ->> 'idSituacaoFinalizar')) 
		    filter (WHERE id_tipo = 16 
 				AND (t.doc ->> 'dataInicio')::INT4 > 0
				AND (t.doc ->> 'idSituacao')::INT4 in (43,87)), 
				'{}' ) as dt_juri_designado,
				min(case when id_tipo = 2 
 				AND (t.doc ->> 'dataInicio')::INT4 > 0
 				AND (t.doc ->> 'idSituacao')::INT4 in (72)
 			then (t.doc ->> 'dataInicio')::INT4
 				else null
				end) as dt_primeira_pronuncia,
				min(case when id_tipo = 16 
 				AND (t.doc ->> 'dataInicio')::INT4 > 0
				AND (t.doc ->> 'idSituacao')::INT4 in (43,87) 
				then (t.doc ->> 'dataInicio')::INT4
 				else null
				end) as dt_primeira_sessao_juri_designada,
	      min(case when id_tipo = 10 
 				AND (t.doc ->> 'dataInicio')::INT4 > 0
 			  AND (t.doc ->> 'idSituacao')::INT4 in (44) 
				then (t.doc ->> 'dataInicio')::INT4
 				else null
				end) as dt_primeira_sessao_juri_realizada

				--Adicionar quantidades de polo passivo e polo ativo
				
FROM processo_indicador_", str_pad(toString(sprintf("%s", tribunais$id[tribunais$sigla==tribunal])),"0",width = 3, side="left"), 
  " as pi, 
      jsonb_array_elements(situacoes) as t(doc), 
      processo_",str_pad(toString(sprintf("%s", tribunais$id[tribunais$sigla==tribunal])),"0",width = 3, side="left"), 
  " as p
WHERE pi.id_processo = p.id
  AND p.flg_fora_recorte = FALSE ",
  if(!is.null(lista.processo)){
    paste0(" AND p.numero IN (",
           paste0("'",lista.processo,"'",collapse = "','"),
           ")")
  },
  if(quebra > 1){
    if(parte == 1){paste0(" AND p.id < ",round(cortes[parte],0))
    } else if(parte <= (quebra-1)){
      paste0(" AND p.id < ",round(cortes[parte],0)," AND p.id >= ",round(cortes[parte-1],0))
    } else {paste0(" AND p.id >= ",round(cortes[parte-1],0))}
  },
  " group by sigla_tribunal, sigla_grau, id_formato, id_nivel_sigilo, nome_sistema,
  id_orgao_julgador_ultimo, valor_causa,pi.id_processo,
numero, id_classe, id_assunto, id_orgao_julgador, (t.doc ->> 'idOrgaoJulgador'), recurso,
case when id_classe_ultima_fase_2 IS NULL AND id_classe_ultima_fase_1 IS NULL 
              then id_classe[array_length(id_classe, 1)] 
                  when id_classe_ultima_fase_2 IS NULL then id_classe_ultima_fase_1 
              else id_classe_ultima_fase_2 end,
               case when id_tipo_procedimento = 3 then 'fiscal'
                      when id_tipo_procedimento = 4 then 'nao fiscal'
                      when (id_fase = 1 and id_tipo_procedimento in (1,5,7)) or (
                      id_fase = 2 and id_tipo_procedimento in (1) and
                      id_classe_ultima_fase_2 IN (select id FROM classes where criminal = TRUE)
                      ) then 'conhecimento' 
                      when (id_fase =  1 and id_tipo_procedimento = 2) or (
                      id_fase = 2 and id_tipo_procedimento in (1,2,5,7)) then 'execucao'
                      when id_fase = 5 or id_tipo_procedimento = 9 then 'pre-processual'
                      when id_fase = 3 and id_tipo_procedimento in (1,5) then 'investigatoria'
                      when id_tipo_procedimento = 10 then 'administrativo'
                      else 'outros' end")


t1.1 <- Sys.time()
bd <- dbGetQuery(con, query.processo)
tp <- if(quebra>1){paste0(tribunal,"_",parte,": Tempo de extracao: ",round(difftime(Sys.time(), t1.1, units = "mins"),2), " minutos.")
} else paste0(tribunal,": Tempo de extracao: ",round(difftime(Sys.time(), t1.1, units = "mins"),2), " minutos.")
print(tp)
return(bd)

}


# Vetor de Datas - Funcao para contar o numero de datas em cada mes e ano
func.contar.anomes <- function(base, variavel, nome_var, ind_tp = F, variavel_tp = NULL, salvar.csv = F, recorte.cn = T,
                               periodo.anomes = NULL, r.classes = F, r.assuntos = F, nome.quebra = ""){
  
  v <- base %>% dplyr::mutate(variavel = get({{variavel}}),
                              anomes = str_sub(gsub("-","",variavel),1,6),
                              ano = str_sub(variavel,1,4),
                              mes = str_sub(variavel,6,7)) %>%
    dplyr::filter(!is.na(variavel) & as.numeric(ano) >= 2020)
  
  list.tp <- as.numeric(gsub("-","",tempo$ultimo_dia))
  max.dt.ult.dia <- max(list.tp)
  max.data.ult.dia <- ymd(max.dt.ult.dia)
  max.ano <- year(max.data.ult.dia)
  max.mes <- month(max.data.ult.dia)
  
  if(r.classes) {
    v <- v %>% dplyr::mutate(chave = chave_classe,
                             anomes = case_when(ano == max.ano | (ano == max.ano-1 & as.numeric(mes) > mes & as.numeric(mes) != 12) ~ paste0(ano,"01"),
                                                TRUE ~ paste0(ano,"00")))
  }
  if(r.assuntos) {
    v <- v %>% dplyr::mutate(chave = chave_assunto,
                             anomes = case_when(ano == max.ano | (ano == max.ano-1 & as.numeric(mes) > mes & as.numeric(mes) != 12) ~ paste0(ano,"01"),
                                                TRUE ~ paste0(ano,"00")))
  }
  if(ind_tp){
    
    if(salvar.csv){
      
      if(is.null(periodo.anomes)){periodo.anomes <- c(paste0(year(max.data.ult.dia)-1,"01"),str_sub(max.dt.ult.dia,1,6))}
      
      if(recorte.cn){
        lista <- v %>%
          dplyr::filter(proc_cn == 1 & as.numeric(anomes) >= periodo.anomes[1] & as.numeric(anomes) <= periodo.anomes[2]) %>%
          dplyr::select(ano, mes, data_total_inicio, variavel, sigla_tribunal, orgao_julgador, id_orgao_julgador, id_municipio, municipio_oj,
                        uf_oj, sigla_grau, formato, procedimento, originario, numero_sigilo, id_assunto, id_classe, id_ultima_classe, classe)
        
      } else {
        lista <- v %>%
          dplyr::filter(as.numeric(anomes) >= periodo.anomes[1] & as.numeric(anomes) <= periodo.anomes[2]) %>%
          dplyr::select(ano, mes, data_total_inicio, variavel, sigla_tribunal, orgao_julgador, id_orgao_julgador, id_municipio, municipio_oj,
                        uf_oj, sigla_grau, formato, procedimento, originario, numero_sigilo, id_assunto, id_classe, id_ultima_classe, classe)
      } 
      
      colnames(lista) <- c("Ano",	"Mes", "Data de inicio"	,"Data de Referencia",	"Tribunal",	"Nome orgao",	"Codigo orgao",	"id_municipio", "Municipio",	"UF",	"Grau",	"Formato",	"Procedimento",	"Recurso Originario",	"Processo", "Codigos assuntos", "Codigos Classes",	"Codigo da Ultima classe CN",	"Nome da ultima classe CN")
      
      data.table::fwrite(lista, paste0("csv_d_",trib,"_",nome_var[1],nome.quebra,".csv"), sep = ";")

      rm(lista)
    }
    
    v <- v %>%
      dplyr::mutate(tp = get({{variavel_tp}})) %>%
      dplyr::select(chave, anomes, tp) %>%
      dplyr::mutate(chave_ano = paste(chave,anomes,sep="_")) %>%
      dplyr::select(-chave,-anomes) %>%
      dplyr::group_by(chave_ano) %>%
      dplyr::summarise(ind = n(), tp_st = sum(tp,na.rm=T), tp_p = sum(!is.na(tp)))
    colnames(v) = c("chave_ano",nome_var)
    
  } else{
    
    if(salvar.csv){
      list.tp <- as.numeric(gsub("-","",tempo$ultimo_dia))
      max.dt.ult.dia <- max(list.tp)
      max.data.ult.dia <- ymd(max.dt.ult.dia)
      
      if(is.null(periodo.anomes)){periodo.anomes <- c(paste0(year(max.data.ult.dia)-1,"01"),str_sub(max.dt.ult.dia,1,6))}
      
      if(recorte.cn){
        lista <- v %>%
          dplyr::filter(proc_cn == 1 & as.numeric(anomes) >= periodo.anomes[1] & as.numeric(anomes) <= periodo.anomes[2]) %>%
          dplyr::select(ano, mes, variavel, sigla_tribunal, orgao_julgador, id_orgao_julgador, id_municipio, municipio_oj,
                        uf_oj, sigla_grau, formato, procedimento, numero_sigilo, id_assunto, id_classe, id_ultima_classe, classe)
        
      } else {
        lista <- v %>%
          dplyr::filter(as.numeric(anomes) >= periodo.anomes[1] & as.numeric(anomes) <= periodo.anomes[2]) %>%
          dplyr::select(ano, mes, variavel, sigla_tribunal, orgao_julgador, id_orgao_julgador, id_municipio, municipio_oj,
                        uf_oj, sigla_grau, formato, procedimento, numero_sigilo, id_assunto, id_classe, id_ultima_classe, classe)
        
      }
      
      colnames(lista) <- c("Ano",	"Mes",	"Data de Referencia",	"Tribunal",	"Nome Orgao",	"Codigo Orgao",	"id_municipio", "Municipio",	"UF",	"Grau",	"Formato",	"Procedimento",	"Processo",	"Codigos assuntos",	"Codigos classes",	"Codigo da Ultima classe CN",	"Nome da Ultima classe CN")
      
      data.table::fwrite(lista, paste0("csv_d_",trib,"_",nome_var[1],nome.quebra,".csv"), sep = ";")

      rm(lista)
    }
    
    
    v <- v %>% dplyr::select(chave, anomes) %>%
      plyr::count() %>%
      dplyr::mutate(chave_ano = paste(chave,anomes,sep="_")) %>%
      dplyr::select(chave_ano,freq) %>%
      group_by(chave_ano) %>%
      dplyr::summarise(valor = sum(freq, na.rm=T))
    colnames(v)[2] =nome_var
  }
  return(v)
}

# Array de Datas - Funcao para contar o numero de datas em cada Mes e ano

func.contar.datas <- function(base, variavel, nome_var, salvar.csv = F, recorte.cn = T, periodo.anomes = NULL, 
                              r.classes = F, r.assuntos = F,nome.quebra=""){
  
  a <- base %>%
    dplyr::mutate(v = gsub("\\{|\\}","", gsub("\\{\\}","",get({{variavel}})))) 
  
  if(r.classes) {
    a <- a %>% dplyr::mutate(chave = chave_classe)
  }
  if(r.assuntos) {
    a <- a %>% dplyr::mutate(chave = chave_assunto)
  }
  
  
  list.tp <- as.numeric(gsub("-","",tempo$ultimo_dia))
  max.dt.ult.dia <- max(list.tp)
  max.data.ult.dia <- ymd(max.dt.ult.dia)
  max.ano = as.numeric(str_sub(max.dt.ult.dia,1,4))
  max.mes <- month(max.data.ult.dia)
  
  if(salvar.csv){
    if(recorte.cn){
      a1 <- a %>%
        dplyr::filter(proc_cn == 1) %>% 
        dplyr::select(chave,numero_sigilo, v) %>%
        dplyr::filter(v != "")
    } else {
      a1 <- a %>% dplyr::select(chave,numero_sigilo, v) %>%
        dplyr::filter(v != "")
    }
    
    
    if(nrow(a1) > 0){
      b= a1$v %>%
        str_split(",")
      
      names(b) <- paste(a1$chave, a1$numero_sigilo,sep=";")
      b1=setNames(unlist(b, use.names=F),rep(names(b), lengths(b)))
      
      if(is.null(periodo.anomes)){periodo.anomes <- c(paste0(year(max.data.ult.dia)-1,"01"),str_sub(max.dt.ult.dia,1,6))}
      
      if(variavel == "dt_julgamento"){
        
        lista <- data.frame(chave = names(b1), nb = b1) %>%
          dplyr::mutate(anomes = str_sub(nb,1,6)) %>%
          dplyr::filter(as.numeric(anomes) >= as.numeric(periodo.anomes[1]) & 
                          as.numeric(anomes) <= as.numeric(periodo.anomes[2])) %>%
          dplyr::mutate(ano = str_sub(anomes,1,4),
                        mes = str_sub(anomes,5,6)) %>% 
          inner_join(a %>% dplyr::mutate(chave = paste(chave, numero_sigilo, sep=";"), by = "chave")) %>%
          dplyr::mutate(nb = ymd(nb)
                        #,numero = case_when(sigiloso == 0 ~ numero,TRUE ~ "sigiloso")
          ) %>%
          dplyr::select(ano, mes, nb, sigla_tribunal, orgao_julgador, id_orgao_julgador, id_municipio, municipio_oj,
                        uf_oj, sigla_grau, formato, procedimento, numero_sigilo,dt_julgamento_movimento, id_assunto, id_classe, id_ultima_classe, classe)
        
        colnames(lista) <- c("Ano",	"Mes",	"Data de Referencia",	"Tribunal",	"Nome Orgao",	"Codigo Orgao",	"id_municipio", "Municipio",	"UF",	"Grau",	"Formato",	"Procedimento",	"Processo",	"Lista Data_Situacao_Movimento","Codigos assuntos",	"Codigos classes",	"Codigo da Ultima classe CN",	"Nome da Ultima classe CN")
        
      } else {
        lista <- data.frame(chave = names(b1), nb = b1) %>%
          dplyr::mutate(anomes = str_sub(nb,1,6)) %>%
          dplyr::filter(as.numeric(anomes) >= as.numeric(periodo.anomes[1]) & 
                          as.numeric(anomes) <= as.numeric(periodo.anomes[2])) %>%
          dplyr::mutate(ano = str_sub(anomes,1,4),
                        mes = str_sub(anomes,5,6)) %>% 
          inner_join(a %>% dplyr::mutate(chave = paste(chave, numero_sigilo, sep=";"), by = "chave")) %>%
          dplyr::mutate(nb = ymd(nb)
          ) %>%
          dplyr::select(ano, mes, nb, sigla_tribunal, orgao_julgador, id_orgao_julgador, id_municipio, municipio_oj,
                        uf_oj, sigla_grau, formato, procedimento, numero_sigilo, id_assunto, id_classe, id_ultima_classe, classe)
        colnames(lista) <- c("Ano",	"Mes",	"Data de Referencia",	"Tribunal",	"Nome Orgao",	"Codigo Orgao",	"id_municipio", "Municipio",	"UF",	"Grau",	"Formato",	"Procedimento",	"Processo",	"Codigos assuntos",	"Codigos classes",	"Codigo da Ultima classe CN",	"Nome da Ultima classe CN")
        
      }
      
      data.table::fwrite(lista, paste0("csv_d_",trib,"_",nome_var[1],nome.quebra,".csv"), sep = ";")

      rm(lista)
    }
  }
  
  
  a1 <- a %>% dplyr::select(chave,numero_sigilo, v) %>%
    dplyr::filter(v != "")
  
  if(nrow(a1) > 0){
    b= a1$v %>%
      str_split(",")
    
    names(b) <- paste(a1$chave,sep=";")
    b=setNames(unlist(b, use.names=F),rep(names(b), lengths(b)))
    
    v <- data.frame(chave = names(b), nb = b) %>%
      dplyr::mutate(anomes = str_sub(nb,1,6),
                    anomes = case_when((r.classes | r.assuntos) &  
                                         as.numeric(anomes) > as.numeric(paste0(max.ano-1,str_pad(max.mes,2,"left","0"))) ~ 
                                         paste0(str_sub(anomes,1,4),"01"),
                                       (r.classes | r.assuntos) ~ paste0(str_sub(anomes,1,4),"00"),
                                       TRUE ~ anomes)) %>% 
      dplyr::select(chave, anomes) %>%
      dplyr::filter(!is.na(anomes),
                    as.numeric(str_sub(anomes,1,4)) >= 2020) %>%
      plyr::count() %>%
      dplyr::mutate(chave_ano = paste(chave,anomes,sep="_")) %>%
      dplyr::select(-chave,-anomes) %>%
      group_by(chave_ano) %>%
      dplyr::summarise(valor = sum(freq, na.rm=T))
    
    colnames(v)[2] =nome_var
    
    return(v)
  } else return(data.frame(chave_ano = NA))
}


# Array com intervalo de Datas - Funcao para contar o numero de datas em cada Mes e ano
func.contar.entre.datas <- function(base, variavel, nome_var, ind_tpcp = F, conclusos = F, 
                                    variavel_tot = NULL, salvar.csv = F, recorte.cn = T, periodo.anomes = NULL, 
                                    correg = F, r.classes = F, r.assuntos = F,filtrar.julg = F,nome.quebra=""){
  list.tp <- as.numeric(gsub("-","",tempo$ultimo_dia))
  max.dt.ult.dia <- max(list.tp)
  max.ano = as.numeric(str_sub(max.dt.ult.dia,1,4))
  max.mes <- month(max.data.ult.dia)
  max.data.ult.dia <- ymd(max.dt.ult.dia)
  max.dt.ult.dia_1 <- as.numeric(gsub("-","",max.data.ult.dia+1))
  
  if(r.classes) {
    base <- base %>% dplyr::mutate(chave = chave_classe)
    list.tp1 <- list.tp
    list.tp <- tempo %>% dplyr::select(ano) %>% dplyr::distinct() %>% dplyr::mutate(ano = paste0(ano,"1231")) %>% pull() 
    max.dt.ult.dia <- max(list.tp1)
    max.data.ult.dia <- ymd(max.dt.ult.dia)
    list.tp[length(list.tp)] <- max.dt.ult.dia
  }
  if(r.assuntos) {
    base <- base %>% dplyr::mutate(chave = chave_assunto)
    list.tp1 <- list.tp
    list.tp <- tempo %>% dplyr::select(ano) %>% dplyr::distinct() %>% dplyr::mutate(ano = paste0(ano,"1231")) %>% pull() 
    max.dt.ult.dia <- max(list.tp1)
    max.data.ult.dia <- ymd(max.dt.ult.dia)
    list.tp[length(list.tp)] <- max.dt.ult.dia
  }
  
  # Filtrar processos que ingressaram após o período de referência
  base <- base[is.na(base$data_ajuizamento) | base$data_ajuizamento <= max.data.ult.dia,]
  
  # Filtrar processos julgados
  if(filtrar.julg){
    base <- base %>%
      filter(is.na(data_total_primeiro_julgamento) | data_total_primeiro_julgamento > max.data.ult.dia)
  }
  
  if(salvar.csv | (variavel=="dt_pendente_liquido" & !filtrar.julg)){
    #if(is.null(periodo.anomes)){periodo.anomes <- str_sub(max.dt.ult.dia,1,6)}
    v <- base %>% dplyr::mutate(variavel = get({{variavel}}),
                                variavel =  gsub(paste0(str_sub(max.dt.ult.dia_1,1,6),"..:0|",str_sub(max.dt.ult.dia_1,1,6),"..:......,"),"", gsub(paste0(max.dt.ult.dia,":",max.dt.ult.dia),paste0(max.dt.ult.dia-1,":",max.dt.ult.dia-1),variavel)),
                                anomes = case_when(r.classes | r.assuntos ~ paste0(str_sub(max.dt.ult.dia,1,4),"01"),
                                                   TRUE ~ str_sub(max.dt.ult.dia,1,6)),
                                ano = str_sub(max.dt.ult.dia,1,4),
                                mes = str_sub(max.dt.ult.dia,5,6))
    
    
    if(recorte.cn){
      lista <- v %>%
        dplyr::mutate(variavel =  gsub(paste0(":",max.dt.ult.dia),":0",variavel),
                      variavel =  gsub(paste0("(,|\\{)",format(seq(ym(as.numeric(str_sub(max.dt.ult.dia_1,1,6))),ym(as.numeric(str_sub(gsub("-","",today()),1,6))),by='months'),"%Y%m"),"..:........", collapse="|"),"",
                                       gsub(paste0("(,|\\{)",format(seq(ym(as.numeric(str_sub(max.dt.ult.dia_1,1,6))),ym(as.numeric(str_sub(gsub("-","",today()),1,6))),by='months'),"%Y%m"),"..:0", collapse="|"),"",
                                            variavel)),
                      data_ultima_movimentacao = as.Date.character(data_ultima_movimentacao)) %>%
        dplyr::filter(proc_cn == 1 & str_detect(variavel,paste0(":0|",paste0(":",format(seq(ym(as.numeric(str_sub(max.dt.ult.dia_1,1,6))),ym(as.numeric(str_sub(gsub("-","",today()),1,6))),by='months'),"%Y%m"), collapse="|")))) %>%
        dplyr::select(sigla_tribunal,sigla_grau,orgao_julgador, id_orgao_julgador,uf_oj, id_municipio, municipio_oj, 
                      ano, mes, numero_sigilo, formato,procedimento, originario,
                      data_ajuizamento, data_total_inicio, data_total_primeiro_julgamento, data_ultima_movimentacao, variavel, 
                      antigos, id_assunto, id_classe, id_ultima_classe, classe,chave,anomes)
      colnames(lista) <- c("Tribunal", "Grau",	"Nome Orgao",	"Codigo Orgao",	"UF","id_municipio", "Municipio",
                           "Ano",	"Mes","Processo","Formato","Procedimento",	"Recurso Originario",	
                           "Data de ajuizamento", "Data de inicio", "Data do primeiro julgamento", "Data da ultima movimentacao",	
                           "Datas inicio:fim", "Dias de antiguidade", "Codigos assuntos",	
                           "Codigos classes","Codigo da Ultima classe",	"Nome da Ultima classe","chave","anomes")
      chave_lista <- tidyr::unite_(lista[,c("Tribunal", "Grau",	"Processo","Procedimento")], 
                                   gsub(" ","_",paste(c("Tribunal", "Grau",	"Processo","Procedimento"), 
                                                      collapse="_")), 
                                   c("Tribunal", "Grau",	"Processo","Procedimento"))
      chave_lista.1 <- tidyr::unite_(lista[,c("Tribunal", "Grau",	"Processo","Procedimento","Recurso Originario")], 
                                     gsub(" ","_",paste(c("Tribunal", "Grau",	"Processo","Procedimento","Recurso Originario"), 
                                                        collapse="_")), 
                                     c("Tribunal", "Grau",	"Processo","Procedimento","Recurso Originario"))
      
      
      if(variavel=="dt_pendente_liquido" & !filtrar.julg){
        lista1 <- v %>%
          dplyr::mutate(variavel =  gsub(paste0(":",max.dt.ult.dia),":0",dt_pendente),
                        variavel =  gsub(paste0("(,|\\{)",format(seq(ym(as.numeric(str_sub(max.dt.ult.dia_1,1,6))),ym(as.numeric(str_sub(gsub("-","",today()),1,6))),by='months'),"%Y%m"),"..:........", collapse="|"),"",
                                         gsub(paste0("(,|\\{)",format(seq(ym(as.numeric(str_sub(max.dt.ult.dia_1,1,6))),ym(as.numeric(str_sub(gsub("-","",today()),1,6))),by='months'),"%Y%m"),"..:0", collapse="|"),"",
                                              variavel)),
                        data_ultima_movimentacao = as.Date.character(data_ultima_movimentacao)) %>%
          dplyr::filter(proc_cn == 1 & str_detect(variavel,paste0(":0|",paste0(":",format(seq(ym(as.numeric(str_sub(max.dt.ult.dia_1,1,6))),ym(as.numeric(str_sub(gsub("-","",today()),1,6))),by='months'),"%Y%m"), collapse="|")))) %>%
          dplyr::select(sigla_tribunal,sigla_grau,orgao_julgador, id_orgao_julgador,uf_oj, id_municipio, municipio_oj, 
                        ano, mes, numero_sigilo, formato,procedimento, originario,
                        data_ajuizamento, data_total_inicio, data_total_primeiro_julgamento, data_ultima_movimentacao, variavel, 
                        antigos, id_assunto, id_classe, id_ultima_classe, classe,chave,anomes)
        colnames(lista1) <- c("Tribunal", "Grau",	"Nome Orgao",	"Codigo Orgao",	"UF","id_municipio", "Municipio",
                              "Ano",	"Mes","Processo","Formato","Procedimento",	"Recurso Originario",	
                              "Data de ajuizamento", "Data de inicio", "Data do primeiro julgamento",	
                              "Data da ultima movimentacao",  "Datas inicio:fim", "Dias de antiguidade", "Codigos assuntos",	
                              "Codigos classes","Codigo da Ultima classe",	"Nome da Ultima classe","chave","anomes")
        
        chave_lista1 <- tidyr::unite_(lista1[,c("Tribunal", "Grau",	"Processo","Procedimento")], 
                                      gsub(" ","_",paste(c("Tribunal", "Grau",	"Processo","Procedimento"), 
                                                         collapse="_")), 
                                      c("Tribunal", "Grau",	"Processo","Procedimento"))
        chave_lista1.1 <- tidyr::unite_(lista1[,c("Tribunal", "Grau",	"Processo","Procedimento","Recurso Originario")], 
                                        gsub(" ","_",paste(c("Tribunal", "Grau",	"Processo","Procedimento","Recurso Originario"), 
                                                           collapse="_")), 
                                        c("Tribunal", "Grau",	"Processo","Procedimento","Recurso Originario"))
        
        
        lista1 <- lista1[!chave_lista1.1$Tribunal_Grau_Processo_Procedimento %in% chave_lista.1$Tribunal_Grau_Processo_Procedimento,]
        
        lista <- rbind(lista %>% dplyr::mutate(Liquido =  "S"),
                       lista1 %>%
                         dplyr::mutate(Liquido =  "N")) %>%
          dplyr::mutate(`Mais 15 anos` = case_when(`Dias de antiguidade` > 15*365.4 ~ 'S',
                                                   TRUE ~ 'N')) %>%
          arrange(desc(`Dias de antiguidade`)) 
        
        lista2 <- lista %>% dplyr::filter(`Mais 15 anos` == 'S')
        
        if(salvar.csv) data.table::fwrite(lista2 %>% dplyr::select(-c(chave,anomes)), paste0("csv_d_",trib,"_CPL_15anos",nome.quebra,".csv"), sep = ";")
        
        
        # Salva arquivos consolidados
        cp_15anos <<- lista2 %>%
          dplyr::mutate(chave_ano = paste0(chave,"_",anomes)) %>%
          group_by(chave_ano) %>%
          dplyr::summarise(cp.15anos = n(),
                           cpl.15anos = sum(Liquido == 'S'),
                           cpnjulg.15anos = sum(is.na(`Data do primeiro julgamento`)),
                           cplnjulg.15anos = sum(is.na(`Data do primeiro julgamento`) & Liquido == 'S'))
        
        rm(lista1,lista2)
        gc()
        
      }
      
    } else {
      lista <- v %>%
        dplyr::mutate(variavel =  gsub(paste0(":",max.dt.ult.dia),":0",variavel),
                      variavel =  gsub(paste0("(,|\\{)",format(seq(ym(as.numeric(str_sub(max.dt.ult.dia_1,1,6))),ym(as.numeric(str_sub(gsub("-","",today()),1,6))),by='months'),"%Y%m"),"..:........", collapse="|"),"",
                                       gsub(paste0("(,|\\{)",format(seq(ym(as.numeric(str_sub(max.dt.ult.dia_1,1,6))),ym(as.numeric(str_sub(gsub("-","",today()),1,6))),by='months'),"%Y%m"),"..:0", collapse="|"),"",
                                            variavel)),
                      data_ultima_movimentacao = as.Date.character(data_ultima_movimentacao)) %>%
        dplyr::filter(str_detect(variavel,paste0(":0|",paste0(":",format(seq(ym(as.numeric(str_sub(max.dt.ult.dia_1,1,6))),ym(as.numeric(str_sub(gsub("-","",today()),1,6))),by='months'),"%Y%m"), collapse="|")))) %>%
        dplyr::select(sigla_tribunal,sigla_grau,orgao_julgador, id_orgao_julgador,uf_oj, id_municipio, municipio_oj, 
                      ano, mes, numero_sigilo, formato,procedimento, originario,
                      data_ajuizamento, data_total_inicio, data_total_primeiro_julgamento, data_ultima_movimentacao, variavel, 
                      antigos, id_assunto, id_classe, id_ultima_classe, classe,chave,anomes)
      colnames(lista) <- c("Tribunal", "Grau",	"Nome Orgao",	"Codigo Orgao",	"UF","id_municipio", "Municipio",
                           "Ano",	"Mes","Processo","Formato","Procedimento",	"Recurso Originario",	
                           "Data de ajuizamento", "Data de inicio", "Data do primeiro julgamento", "Data da ultima movimentacao",	
                           "Datas inicio:fim", "Dias de antiguidade", "Codigos assuntos",	
                           "Codigos classes","Codigo da Ultima classe",	"Nome da Ultima classe","chave","anomes")
      
      
      if(variavel=="dt_pendente_liquido" & !filtrar.julg){
        lista1 <- v %>%
          dplyr::mutate(variavel =  gsub(paste0(":",max.dt.ult.dia),":0",dt_pendente),
                        variavel =  gsub(paste0("(,|\\{)",format(seq(ym(as.numeric(str_sub(max.dt.ult.dia_1,1,6))),ym(as.numeric(str_sub(gsub("-","",today()),1,6))),by='months'),"%Y%m"),"..:........", collapse="|"),"",
                                         gsub(paste0("(,|\\{)",format(seq(ym(as.numeric(str_sub(max.dt.ult.dia_1,1,6))),ym(as.numeric(str_sub(gsub("-","",today()),1,6))),by='months'),"%Y%m"),"..:0", collapse="|"),"",
                                              variavel)),
                        data_ultima_movimentacao = as.Date.character(data_ultima_movimentacao)) %>%
          dplyr::filter(str_detect(variavel,paste0(":0|",paste0(":",format(seq(ym(as.numeric(str_sub(max.dt.ult.dia_1,1,6))),ym(as.numeric(str_sub(gsub("-","",today()),1,6))),by='months'),"%Y%m"), collapse="|")))) %>%
          dplyr::select(sigla_tribunal,sigla_grau,orgao_julgador, id_orgao_julgador,uf_oj, id_municipio, municipio_oj, 
                        ano, mes, numero_sigilo, formato,procedimento, originario,
                        data_ajuizamento, data_total_inicio, data_total_primeiro_julgamento, data_ultima_movimentacao, variavel, 
                        antigos, id_assunto, id_classe, id_ultima_classe, classe,chave,anomes)
        colnames(lista1) <- c("Tribunal", "Grau",	"Nome Orgao",	"Codigo Orgao",	"UF","id_municipio", "Municipio",
                              "Ano",	"Mes","Processo","Formato","Procedimento",	"Recurso Originario",	
                              "Data de ajuizamento", "Data de inicio", "Data do primeiro julgamento",	"Data da ultima movimentacao",
                              "Datas inicio:fim",	"Dias de antiguidade", "Codigos assuntos",	
                              "Codigos classes","Codigo da Ultima classe",	"Nome da Ultima classe","chave","anomes")
        
        lista <- rbind(lista %>% dplyr::mutate(Liquido =  "S"),
                       lista1 %>%
                         dplyr::filter(!lista1$Processo %in% lista$Processo) %>%
                         dplyr::mutate(Liquido =  "N")) %>%
          dplyr::mutate(`Mais 15 anos` = case_when(`Dias de antiguidade` > 15*365.4 ~ 'S',
                                                   TRUE ~ 'N')) %>%
          arrange(desc(`Dias de antiguidade`)) 
        
        lista2 <- lista %>% dplyr::filter(`Mais 15 anos` == 'S')
        
        if(salvar.csv) data.table::fwrite(lista2 %>% dplyr::select(-c(chave,anomes)), paste0("csv_d_",trib,"_CPL_15anos",nome.quebra,".csv"), sep = ";")
        
        
        # Salva arquivos consolidados
        cp_15anos <<- lista2 %>%
          dplyr::mutate(chave_ano =paste0(chave,"_",anomes)) %>%
          group_by(chave_ano) %>%
          dplyr::summarise(cp.15anos = n(),
                           cpl.15anos = sum(Liquido == 'S'),
                           cpnjulg.15anos = sum(is.na(`Data do primeiro julgamento`)),
                           cplnjulg.15anos = sum(is.na(`Data do primeiro julgamento`) & Liquido == 'S'))
        
        rm(lista1,lista2)
        gc()
        
      }
    } 
    
    if(salvar.csv) {
      data.table::fwrite(lista %>% dplyr::select(-c(chave,anomes)), paste0("csv_d_",trib,"_",nome_var[1],nome.quebra,".csv"), sep = ";")
    }
    
    rm(lista)
    
    
    
  }
  
  
  
  if(is.null(variavel_tot)){
    
    a = base %>%
      dplyr::mutate(v = as.character(gsub("^,|,$","",gsub(paste0(str_sub(max.dt.ult.dia_1,1,6),"..:0|",str_sub(max.dt.ult.dia_1,1,6),"..:......,"),"",gsub(":0",paste0(":",max.dt.ult.dia),gsub(paste0(max.dt.ult.dia,":",max.dt.ult.dia),paste0(max.dt.ult.dia-1,":",max.dt.ult.dia-1),gsub("\\{|\\}",",", gsub("\\{\\}","",get({{variavel}}))))))))) %>%
      dplyr::select(chave,numero_sigilo, v) %>%
      dplyr::filter(v != "")
    
  } else {
    a = base %>%
      dplyr::mutate(v = as.character(gsub("^,|,$","",gsub(paste0(str_sub(max.dt.ult.dia_1,1,6),"..:0|",str_sub(max.dt.ult.dia_1,1,6),"..:......,"),"",gsub(":0",paste0(":",max.dt.ult.dia),gsub(paste0(max.dt.ult.dia,":",max.dt.ult.dia),paste0(max.dt.ult.dia-1,":",max.dt.ult.dia-1),gsub("\\{|\\}",",", gsub("\\{\\}","",get({{variavel}}))))))))) %>%
      dplyr::mutate(vtot = as.character(gsub("^,|,$","",gsub(":0",paste0(":",max.dt.ult.dia),gsub(paste0(max.dt.ult.dia,":",max.dt.ult.dia),paste0(max.dt.ult.dia-1,":",max.dt.ult.dia-1),gsub("\\{|\\}",",", gsub("\\{\\}","",get({{variavel_tot}})))))))) %>%
      dplyr::filter(!(v == "" & is.na(vtot)))
    
    a = bind_rows(a %>% dplyr::filter(v != "") %>% dplyr::select(chave,numero_sigilo, v),
                  a %>% dplyr::filter(!is.na(vtot)) %>%
                    dplyr::mutate(chave = gsub(paste0("^.*_",sigla_tribunal[1]),paste0("999991_",sigla_tribunal[1]),chave))%>% 
                    dplyr::select(chave,numero_sigilo, vtot) %>%
                    dplyr::rename(v = vtot) %>%
                    distinct_all())
  }
  
  rm(base)
  gc()
  
  # Funcao para calcular as somas dos tempos
  func.soma.tp = function(x) {rowSums(apply(as.data.frame(str_split(x,",",simplify = T)),2,function(y) ymd(gsub(".*:","",y))) - 
                                        apply(as.data.frame(str_split(x,",",simplify = T)),2,function(y) ymd(gsub(":.*","",y))), na.rm=T)}
  # Funcao para calcular a maior data
  func.max.tp = function(x) {apply(apply(as.data.frame(str_split(x,",",simplify = T)),2,function(y) as.numeric(replace_na(as.numeric(gsub(".*:","",y)),0))),1, max, na.rm=T)}
  
  if(nrow(a) > 1){
    b= a$v %>%
      str_split(",")
    
    names(b) <- paste(a$chave, a$numero_sigilo, sep=";")

    if(!ind_tpcp){
      rm(a)
      gc()
    }
    
    b1=setNames(unlist(b, use.names=F),rep(names(b), lengths(b)))
    
    c=data.frame(str_split_fixed(b1,":", n = str_count(b1[1],":")+1), stringsAsFactors = F)
    
    df <- if(str_count(b1[1],":")+1 == 2){
      data.frame(chave=names(b1), c, stringsAsFactors = F) %>%
        dplyr::mutate(X1 = as.numeric(X1),
                      X2 = as.numeric(X2))
      
    } else {
      data.frame(chave=names(b1), c, stringsAsFactors = F) %>%
        dplyr::mutate(X1 = as.numeric(X1),
                      X2 = as.numeric(X2),
                      X3 = case_when(X3 == "68" ~ "ind6b",
                                     X3 == "69" ~ "ind6c.d",
                                     X3 == "66" ~ "ind6c.b",
                                     X3 == "67" ~ "ind6c.c",
                                     TRUE ~ "ind6c.a"))
    }
    
    df1 <- sapply(list.tp, 
                  function(x) ifelse(df$X2 >= x & !is.na(df$X2) & df$X1 <= x  & !is.na(df$X1), 1, 0),simplify = T)
    
    
    
    
    if(conclusos){
      rm(b,b1,c)
      gc()
      
      colnames(df1) <- if(r.classes | r.assuntos){
        if(max.mes != 12) {
          c(paste0(str_sub(list.tp,1,4)[1:(length(list.tp)-2)],"00"),paste0(str_sub(list.tp,1,4)[(length(list.tp)-1):length(list.tp)],"01"))
        } else c(paste0(str_sub(list.tp,1,4)[1:(length(list.tp)-1)],"00"),paste0(str_sub(list.tp,1,4)[length(list.tp)],"01"))
      } else {
        str_sub(list.tp,1,6)
      }
      
      df1 <- df1 %>% 
        as.data.frame %>% 
        bind_cols(df[,c("chave","X3")]) %>% 
        dplyr::mutate(chave=gsub(";.*","",chave)) %>%
        setDT %>%
        data.table::dcast(chave + X3 ~ ., 
                          fun.agg = function(x) sum(x, na.rm=T), 
                          value.var = grep("20",colnames(.),value = T)) %>%
        data.table::melt(id.vars = c("chave","X3"), variable.name = "ano_mes",value.name = "valor") %>%
        as.data.frame %>%
        dplyr::filter(valor > 0 & !is.na(valor)) %>% 
        dplyr::mutate(chave_ano = paste(chave,ano_mes,sep="_")) %>%
        dplyr::select(-chave,-ano_mes) %>%
        setDT %>%
        data.table::dcast(chave_ano ~ X3, value.var = "valor") %>%
        as.data.frame %>%
        dplyr::mutate(ind6a = rowSums(dplyr::across(starts_with("ind")), na.rm=T))
      colnames(df1) <- gsub("ind6",nome_var[1],colnames(df1))
      
    } else {
      
      colnames(df1) <- if(r.classes | r.assuntos ){
        if(max.mes != 12) {
          c(paste0(str_sub(list.tp,1,4)[1:(length(list.tp)-2)],"00"),paste0(str_sub(list.tp,1,4)[(length(list.tp)-1):length(list.tp)],"01"))
        } else c(paste0(str_sub(list.tp,1,4)[1:(length(list.tp)-1)],"00"),paste0(str_sub(list.tp,1,4)[length(list.tp)],"01"))
      } else {
        str_sub(list.tp,1,6)
      }
      
      df1 <- df1 %>% 
        as.data.frame %>% 
        bind_cols(data.frame(chave=df$chave, stringsAsFactors = F))
      
      if(ind_tpcp){
        df.tempo <- setDT(df1)[, lapply(.SD, max, na.rm=TRUE), by = chave] %>%
          ungroup %>%
          dplyr::select(-chave) %>%
          as.data.frame
      }
      df1 <- df1 %>%
        as.data.frame %>%
        dplyr::mutate(chave = gsub(";.*","",chave)) %>%
        setDT %>%
        data.table::dcast(chave ~ ., 
                          fun.agg = function(x) sum(x, na.rm=T), 
                          value.var = grep("20",colnames(.),value = T)) %>%
        data.table::melt(id = "chave", variable.name = "ano_mes",value.name = "valor") %>%
        as.data.frame %>%
        dplyr::filter(valor > 0 & !is.na(valor)) %>% 
        dplyr::mutate(chave_ano = paste(chave,ano_mes,sep="_")) %>%
        dplyr::select(chave_ano, valor) 
      colnames(df1) <- c("chave_ano", nome_var[1])
    }
    

    if(ind_tpcp){
      df <- df %>%
        as.data.frame %>%
        dplyr::mutate(X1 = ymd(X1),
                      X2 = ymd(X2),
                      tp = difftime(X2, X1, units = "days")) %>%
        setDT %>%
        .[, .(dt_fim = max(X2,na.rm=T), tp = sum(tp, na.rm=T)), by = chave] %>%
        dplyr::mutate(dt_inicio = dt_fim - tp,
                      chave = gsub(";.*","",chave)
        ) %>%
        as.data.frame
      
      rm(b,c)
      gc()

      df2 <- sapply(ymd(list.tp), 
                    function(x) ifelse(df$dt_fim >= x & !is.na(df$dt_fim) & x >= df$dt_inicio & !is.na(df$dt_inicio), 
                                       as.numeric(round(difftime(x, df$dt_inicio, units = "days"),0)),0),
                    simplify = T)      
      
      colnames(df2) <- if(r.classes | r.assuntos ){
        if(max.mes != 12) {
          c(paste0(str_sub(list.tp,1,4)[1:(length(list.tp)-2)],"00"),paste0(str_sub(list.tp,1,4)[(length(list.tp)-1):length(list.tp)],"01"))
        } else c(paste0(str_sub(list.tp,1,4)[1:(length(list.tp)-1)],"00"),paste0(str_sub(list.tp,1,4)[length(list.tp)],"01"))
      } else {
        str_sub(list.tp,1,6)
      }
      
      df2 <- df2 %>% 
        as.data.frame
      
      # Desconsidera os tempos dos processos que nao estao pendentes
      df2 <- df2 * df.tempo 
      df2 <- df2 %>%
        bind_cols(data.frame(chave=df$chave, stringsAsFactors = F)) %>% 
        setDT %>%
        data.table::dcast(chave ~ ., 
                          fun.agg = function(x) sum(x, na.rm=T), 
                          value.var = grep("20",colnames(.),value = T)) %>%
        data.table::melt(id = "chave", variable.name = "ano_mes",value.name = "valor") %>%
        as.data.frame %>%
        dplyr::filter(valor > 0 & !is.na(valor)) %>% 
        dplyr::mutate(chave_ano = paste(chave,ano_mes,sep="_")) %>%
        dplyr::select(chave_ano,valor)

      colnames(df2) <- c("chave_ano", nome_var[2])
      
      return(if(variavel == "dt_pendente_liquido" & !filtrar.julg){
        df1 %>% 
          left_join(df2, by = "chave_ano") %>%
          left_join(cp_15anos, by = "chave_ano")
      } else df1 %>% left_join(df2, by = "chave_ano"))
      
    } else return(if(variavel == "dt_pendente_liquido" & !filtrar.julg){
      df1 %>% left_join(cp_15anos, by = "chave_ano")
    } else df1)
  } else return(data.frame(chave_ano = NA))
  print(paste0("Indicador ",variavel,": ", round(difftime(Sys.time(), tp.ind, units = "mins"),2), " minutos."))
  
}


# Funcao para calcular os processos antigos
func.premio.antigos <- function(base, ano.ingresso, dt.pendente = NA, liquido = T, salvar.csv = F,nome.quebra=""){
  list.tp <- as.numeric(gsub("-","",tempo$ultimo_dia))
  max.dt.ult.dia <- max(list.tp)
  max.ano = as.numeric(str_sub(max.dt.ult.dia,1,4))
  max.mes <- month(max.data.ult.dia)
  max.data.ult.dia <- ymd(max.dt.ult.dia)
  max.dt.ult.dia_1 <- as.numeric(gsub("-","",max.data.ult.dia+1))
  
  if(liquido) base$var <- base$dt_pendente_liquido else base$var <- base$dt_pendente
  if(is.na(dt.pendente)) dt.pendente <- max.dt.ult.dia
  data.pendente <- ymd(dt.pendente)

  df = base %>%
    dplyr::mutate(v =  gsub(paste0(":",max.dt.ult.dia),":0",var),
                  v =  gsub(paste0("(,|\\{)",(as.numeric(str_sub(max.dt.ult.dia,1,6))+1):as.numeric(str_sub(gsub("-","",today()),1,6)),"..:........", collapse="|"),"",
                            gsub(paste0("(,|\\{)",(as.numeric(str_sub(max.dt.ult.dia,1,6))+1):as.numeric(str_sub(gsub("-","",today()),1,6)),"..:0", collapse="|"),"",
                                 v)),
                  chave_ano = paste(chave,"_",max.ano,str_pad(max.mes,2,"left","0") , sep=""),
                  cp.antigos = case_when(data_ajuizamento < ymd(paste0(ano.ingresso+1,"0101")) &
                                           (is.na(data_total_primeiro_julgamento) | data_total_primeiro_julgamento > data.pendente) ~ 1,
                                         TRUE ~ 0),
                  cp.antigos.julg = case_when(data_ajuizamento < ymd(paste0(ano.ingresso+1,"0101")) &
                                                !is.na(data_total_primeiro_julgamento) &
                                                data_total_primeiro_julgamento > data.pendente & data_total_primeiro_julgamento <= data.pendente+366 ~ 1,
                                              TRUE ~ 0)) %>%
    dplyr::filter(str_detect(v,paste0(":0|",paste0(":",(as.numeric(str_sub(dt.pendente,1,6))+1):as.numeric(str_sub(gsub("-","",today()),1,6)), collapse="|")))) %>%
    group_by(chave_ano) %>%
    dplyr::summarise(cp.antigos = sum(cp.antigos, na.rm=T),
                     cp.antigos.julg = sum(cp.antigos.julg, na.rm=T))
  
  return(df)
}
