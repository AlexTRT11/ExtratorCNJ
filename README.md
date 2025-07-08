# ElasticToDatamart



## Requerimentos
Para uso do projeto na sua totalidade é necessário os seguintes acessos:
 - Git no CNJ onde se encontra o Projeto (que inclusive você possui se estiver lendo este documento): https://git.cnj.jus.br/dpj/elastictodatamart
 - Repositório Maven do CNJ : https://nexus.cnj.jus.br/
 - Elastic-Search do CNJ : https://api.datajud.cnj.jus.br/
 
Confecção de projeto ElasticToDatamart no Git para que os tribunais possam baixar os códigos-fonte de migração do elastic para o datamart 
e gerar o banco de dados via docker com carregamento de dados iniciais para conseguir executar o procedimento de migração ao executar o projeto 
(Necessário informar as variáveis de ambiente, ter o docker instalado

## Nome
Conversor de registros do Elastic-Search para o Data mart em Postgresql Datajud

## Descrição
O projeto irá permitir que seja criado um ambiente local, via docker, com toda a estrutura de banco de dados do Postgresql utilizada no Datamart, e já populada com os valores iniciais, de forma a permitir que o projeto Java em questão, seja executado e por meio dele, os registros enviados pelo tribunal ao Elastic-Search sejam convertidos e populem o banco gerado localmente.

## Instalação
Será necessária a instalação dos seguintes softwares:
 - Docker Desktop : https://www.docker.com/products/docker-desktop/
 - Java JDK 11+: https://www.oracle.com/br/java/technologies/downloads/
 - Maven : https://maven.apache.org/download.cgi
 
## Uso
Após conseguir acesso ao Git, Maven e Elastic-Search no CNJ, e ter instalado o Docker, a JDK e o Maven na estação, o projeto deve ser baixado do Git.
Após isso deve-se abrir a Console/Terminal/Shell e ir até a raiz do projeto baixado e executar os seguintes comandos:

-> cd DB

-> docker-compose up -d

Após este último comando, o Docker irá montar a imagem do banco de dados, e popular com todos os registros necessários para o uso do projeto. Este procedimento demora em torno de 10 minutos.

Ao ser concluído, o banco de dados já estará acessível na estação. As configurações de acesso ao banco de dados estão localizadas no arquivo .env dentro da pasta DB do projeto e podem ser alteradas antes de executar o docker-compose.

Em seguida abra o projeto em sua IDE Java favorita e execute-o preenchendo as seguintes variáveis de ambiente ou preenchendo o arquivo application.properties:

elasticsearchHost=api.datajud.cnj.jus.br

elasticsearchPort=443

elasticsearchPwd={senha de acesso ao Elastic-Search do CNJ}

elasticsearchUser={usuário que tem acesso ao Elastic-Search do CNJ}

{senha definida do arquivo .env}
postgresqlPwd=postgres

{DB definido no arquivo .env}
postgresqlUrl=jdbc:postgresql://localhost:5432/datajud

{usuário definido do arquivo .env}
postgresqlUser=postgres

millisInsercao=0 

Dica: o millisInsercao igual a 0 executará todos os processos do elastic(e continuará de onde parou na última execução até que não restem mais processos), mas com um volume elevado de processos pode se tornar extremamente demorado. Desta forma se o valor for informado, o processamento seguirá a partir do tempo informado, sempre que executar}

Em seguida execute os comandos abaixo:

-> mvn clean

-> mvn package

-> cd target

-> java -jar .\elastictodatajud-0.0.1-SNAPSHOT.jar

Neste momento todos os processos do Tribunal do usuário informado no login do Elastic-Search serão carregados, em lotes de mil em mil para o datajud, sendo o tempo de conclusão proporcional a capacidade da processamento e memória da estação.
Nossos testes indicam performance de até 120 mil processos por hora em estações com 32GB de RAM e 8 Cores.

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
O projeto será atualizado conforme novas regras, indicadores e novas informações sejam definidas e disponibilizadas. Nesta prévia, o projeto está capaz de migrar do elastic para a estrutura do datamart em docker disponibilizada e pré-populada todos os processos existentes no Elastic-Seardh do CNJ, do específico tribunal.
