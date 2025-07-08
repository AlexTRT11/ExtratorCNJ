# Datamart to Bi

## !!ATENÇÃO!!
Como o docker ElasticToDatamart processa os dados a partir dos XMLs mais recentes enviados pelo tribunal, as saídas podem divergir dos dados publicados em https://painel-estatistica.stg.cloud.cnj.jus.br/estatisticas.html devido à diferença na data de processamento do painel pelo DPJ/CNJ.

## Requerimentos
Antes de executar os scripts, é necessário estar com o docker ElasticToDatamart (https://git.cnj.jus.br/git-jus/datajud/elastictodatamart) rodando e com as dados do tribunal carregados.

Para rodar os scripts, sugerimos utilizar a versão 3.6.3 do R
- https://cran.r-project.org/bin/windows/base/old/3.6.3/
- https://cran.r-project.org/src/base/R-3/R-3.6.3.tar.gz

	E dos seguintes pacotes:
	
		- Pacotes para manipulação e análise de dados
		 
		  - data.table (https://cran.r-project.org/web/packages/data.table/index.html)
		  - lubridate (https://cran.r-project.org/web/packages/lubridate/index.html)
		  - stringr (https://cran.r-project.org/web/packages/stringr/index.html)
		  - dplyr (https://cran.r-project.org/web/packages/dplyr/index.html)
		  - tidyr (https://cran.r-project.org/web/packages/tidyr/index.html)
		  
		- Pacotes para manipulação de bancos de dados
		  
		  - RPostgres (https://cran.r-project.org/web/packages/RPostgres/index.html)
		  

## Nome
Scripts de cálculo de indicadores e geração das tabelas fato que alimentam os painéis de estatística (https://painel-estatistica.stg.cloud.cnj.jus.br/estatisticas.html)

## Descrição
O projeto irá permitir que sejam calculados os indicadores e geradas as tabelas que alimentam os painéis de estatística do CNJ em um ambiente local, a partir do docker ElasticToDatamart (https://git.cnj.jus.br/git-jus/datajud/elastictodatamart) com toda a estrutura de banco de dados do PostgreSQL utilizada no Datamart, e já populada com os valores iniciais, de forma a permitir que o projeto Java em questão seja executado e, por meio dele, os registros provenientes dos XMLs sejam convertidos e populem o banco gerado localmente.

## Instalação
Será necessária a instalação da versão 3.6.3 do R e dos pacotes relacionados na seção "Requerimentos"
 
## Uso
Com o Docker ElasticToDatamart rodando e as tabelas populadas

- Salvar os scripts VarAmbiente.R, PacotesFuncoes.R, DatamartToBi.R na pasta de trabalho
- Editar o caminho da pasta de trabalho no script Data.R-3/R-3
- Editar as variáveis de ambiente conforme dados do tribunal e do docker ElasticToDatamart (https://git.cnj.jus.br/git-jus/datajud/elastictodatamart)
- Executar o script DatamartToBi.R

Após este último passo, o script irá conectar o R ao banco Postgres do docker, calcular os indicadores e gerar as tabelas que alimentam os painéis de estatística (https://painel-estatistica.stg.cloud.cnj.jus.br/estatisticas.html) em formato .csv e .Rdata

## Suporte
estatistica@cnj.jus.br

## Próximas Etapas
Criação de docker com a versão do R e os pacotes utilizados pelo DPJ instalados.

## Contribuição
A definir

## Autores
CNJ e Equipe PNUD Eixo 4

## Licença
Disponibilização aos Tribunais para reprodução em seus ambientes da extração realizada no CNJ, tanto para validação dos dados quanto para identificação de problemas.

## Status do Projeto
O projeto será atualizado conforme novas regras, indicadores e novas informações sejam definidas e disponibilizadas.