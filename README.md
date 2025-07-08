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
O projeto permite criar um ambiente local, via docker, com toda a estrutura de banco de dados do PostgreSQL utilizada no Datamart. A aplicação extrai os arquivos XML brutos, persiste seus dados de forma normalizada e em seguida calcula os indicadores para utilização em painéis de estatística.

## Instalação
Será necessária a instalação dos seguintes softwares:
 - Docker Desktop : https://www.docker.com/products/docker-desktop/
 - Java JDK 11+: https://www.oracle.com/br/java/technologies/downloads/
 - Maven : https://maven.apache.org/download.cgi
 
## Uso
Após conseguir acesso ao Git, Maven e ao banco PostgreSQL com os XMLs do CNJ, e ter instalado o Docker, a JDK e o Maven na estação, o projeto deve ser baixado do Git.
Após isso deve-se abrir a Console/Terminal/Shell e ir até a raiz do projeto baixado e executar os seguintes comandos:

-> cd DB

-> docker-compose up -d

Após este último comando, o Docker irá montar a imagem do banco de dados, e popular com todos os registros necessários para o uso do projeto. Este procedimento demora em torno de 10 minutos.

Ao ser concluído, o banco de dados já estará acessível na estação. As configurações de acesso ao banco de dados estão localizadas no arquivo .env dentro da pasta DB do projeto e podem ser alteradas antes de executar o docker-compose.

Em seguida abra o projeto em sua IDE Java favorita e execute-o preenchendo as seguintes variáveis de ambiente ou preenchendo o arquivo application.properties:


xmlPostgresqlUrl=jdbc:postgresql://localhost:5432/datajud_xml
xmlPostgresqlUser={usuário do banco que contém os XMLs}
xmlPostgresqlPwd={senha de acesso ao banco de XML}

postgresqlUrl=jdbc:postgresql://localhost:5432/datajud
postgresqlUser=postgres
postgresqlPwd=postgres

millisInsercao=0 

Dica: o millisInsercao igual a 0 executará todo o processamento desde o início (e continuará de onde parou na última execução até que não restem mais processos), mas com um volume elevado de dados pode se tornar extremamente demorado. Desta forma, se um valor for informado, o processamento seguirá a partir do tempo indicado sempre que for executado}

Em seguida execute os comandos abaixo:

-> mvn clean

-> mvn package

-> cd target

-> java -jar .\elastictodatajud-0.0.1-SNAPSHOT.jar

Durante a execução o sistema extrai os XMLs, realiza o parsing das informações, grava nas tabelas normalizadas e, por fim, calcula os indicadores. O tempo de conclusão é proporcional à capacidade de processamento da estação.

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
