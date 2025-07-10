FROM postgres:alpine
ADD postgres/init.sql /docker-entrypoint-initdb.d/
ADD postgres/structure.sql /docker-entrypoint-initdb.d/
ADD postgres/data.sql /docker-entrypoint-initdb.d/
ADD postgres/create_indicadores_historico.sql /docker-entrypoint-initdb.d/
RUN chmod a+r /docker-entrypoint-initdb.d/*
EXPOSE 5432
