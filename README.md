# ElasticToDatamart



## Requerimentos
Para uso do projeto na sua totalidade é necessário os seguintes acessos:
 - Git no CNJ onde se encontra o Projeto (que inclusive você possui se estiver lendo este documento): https://git.cnj.jus.br/dpj/elastictodatamart
 - Repositório Maven do CNJ : https://nexus.cnj.jus.br/
- Banco PostgreSQL com os XMLs brutos do DataJud

Variáveis de ambiente necessárias para a conexão aos bancos de dados:
 - `xmlPostgresqlUrl`, `xmlPostgresqlUser`, `xmlPostgresqlPwd` referentes ao banco com os XMLs
 - `postgresqlUrl`, `postgresqlUser`, `postgresqlPwd` referentes ao banco destino com estrutura normalizada
 
Confecção de projeto ElasticToDatamart no Git para que os tribunais possam baixar os códigos-fonte de migração
e gerar o banco de dados via docker com carregamento de dados iniciais para conseguir executar o procedimento de processamento dos XMLs
(Necessário informar as variáveis de ambiente e ter o docker instalado)

## Nome
Conversor de registros em XML para o Data Mart em PostgreSQL Datajud

## Descrição
O sistema é composto por:
- **Banco datamart**: estrutura PostgreSQL criada via docker na pasta `DB`.
- **Módulo Java**: processa os XMLs e grava nas tabelas normalizadas.
- **Scripts R**: opcionalmente geram arquivos para BI a partir do banco local.
O projeto permite criar um ambiente local, via docker, com toda a estrutura de banco de dados do PostgreSQL utilizada no Datamart. A aplicação extrai os arquivos XML brutos, persiste seus dados de forma normalizada e em seguida calcula os indicadores para utilização em painéis de estatística.

## Instalação
Será necessária a instalação dos seguintes softwares:
 - Docker Desktop : https://www.docker.com/products/docker-desktop/
 - Java JDK 11+: https://www.oracle.com/br/java/technologies/downloads/
 - Maven : https://maven.apache.org/download.cgi
 
## Uso
Siga os passos abaixo para executar o projeto em sua estação:

1. **Clone o repositório**

```bash
git clone https://github.com/cnj-projects/ElasticToDatamart.git
cd ElasticToDatamart
```

2. **Suba o banco local**

```bash
cd DB
docker-compose up -d
```

O Docker cria o banco datamart e popula as tabelas iniciais.
Todos os arquivos `.sql` presentes em `DB/postgres` são montados no
diretório `/docker-entrypoint-initdb.d` do contêiner. O script
`init.sql` executa `structure.sql` e `data.sql`, que por sua vez criam o
schema `datamart` e inserem dados básicos. Assim a estrutura do banco
é criada automaticamente na primeira inicialização.

3. **Defina as variáveis de ambiente**

Configure as variáveis abaixo (via `export` ou editando `src/main/resources/application.properties`):


xmlPostgresqlUrl=jdbc:postgresql://localhost:5432/datajud_xml
xmlPostgresqlUser={usuário do banco que contém os XMLs}
xmlPostgresqlPwd={senha de acesso ao banco de XML}

postgresqlUrl=jdbc:postgresql://localhost:5432/datajud
postgresqlUser=postgres
postgresqlPwd=postgres
hmlPostgresqlUrl=jdbc:postgresql://10.11.1.88:5432/datajud_hml
hmlPostgresqlUser=datajud_user
hmlPostgresqlPwd=datajud

millisInsercao=0 

Dica: o millisInsercao igual a 0 executará todo o processamento desde o início (e continuará de onde parou na última execução até que não restem mais processos), mas com um volume elevado de dados pode se tornar extremamente demorado. Desta forma, se um valor for informado, o processamento seguirá a partir do tempo indicado sempre que for executado}

Em seguida execute os comandos abaixo:

-> mvn clean

-> mvn package

-> cd target

-> java -jar .\elastictodatajud-0.0.1-SNAPSHOT.jar
# o resultado dos indicadores é impresso no console

Durante a execução o sistema extrai os XMLs, realiza o parsing das informações, grava nas tabelas normalizadas e, por fim, calcula os indicadores. O tempo de conclusão é proporcional à capacidade de processamento da estação.

## Extração direta do datajud_hml

Para calcular os indicadores a partir do banco `datajud_hml` sem utilizar o Elastic, execute:

```
java -cp target/elastictodatajud-0.0.1-SNAPSHOT.jar br.jus.cnj.datajud.hml.HmlIndicatorsApplication
```

O aplicativo utilizará as variáveis de ambiente `hmlPostgresqlUrl`, `hmlPostgresqlUser` e `hmlPostgresqlPwd` para acessar o banco, extraindo os XMLs enviados entre `01/08/2024` e `31/07/2025` e apresentando os indicadores I, III e V da Portaria CNJ 238/2024.


## Suporte
A definir

## Próximas Etapas
A definir

## Contribuição
A definir

## Autores
CNJ e Equipe PNUD Eixo 4

## Licença
Disponibilização aos Tribunais para reprodução em seus ambientes da extração realizada no CNJ, tanto para validação dos dados quanto para identificação de problemas.

## Status do Projeto
O projeto será atualizado conforme novas regras, indicadores e novas informações sejam definidas e disponibilizadas. Esta versão permite processar os XMLs extraídos e gerar as tabelas e indicadores do Datamart em um ambiente dockerizado.
