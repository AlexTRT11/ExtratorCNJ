#VARIÁVEIS DE AMBIENTE

# Define Char set
Sys.setlocale(locale= "Portuguese_Brazil.1252")

#Pasta de trabalho 
#Endereço da pasta estão os scrips "datamart_to_bi.R", "lib_fc_datamart_to_bi.R" e "var_ambiente_datamart_to_bi.R" e onde os outputs serão salvos

pasta.trabalho<-pasta.trabalho #Definida no arquivo datamart_to_bi.R

#Tribunal
tribunal<-trib<-toupper('XXXX') #Substituir pela sigla do seu tribunal

#Partes - Em quantas partes rodar o processamento (para tribunais com grande número de processos)
partes<-1

#salvar tabela fato de indicacores
salvar.indicadores<-T

#salvar tabela fato de classes
salvar.classes<-T

#salvar tabela fato de assuntos
salvar.assuntos<-T

#Salvar arquivos em formato csv
salvar.csvs<-T

#ATENÇÃO#
#Lag dias para calculo de indicadores
dt.corr <- 60

#Container elastictodatamart
  # O valores atuais estão conforme os parâmetros padrão do arquivo "DB/.env" da aplicação 
  # https://git.cnj.jus.br/git-jus/datajud/elastictodatamart
  # caso tenha alterado esses valores na montagem do container, informar os parâmetros utilizados na criação

db<- 'datajud' #nome do banco Postgres criado pelo container elastictodadajud
host_db<- 'localhost' #host do banco criado pelo container elastictodadajud
db_port<- '5432' #porta do banco criado pelo container elastictodadajud
db_user<- "postgres" #usuário do banco criado pelo container elastictodadajud
db_password<- "postgres" #senha do banco criado pelo container elastictodadajud