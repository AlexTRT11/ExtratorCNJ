FROM postgres:alpine
ADD scripts/structure.sql /docker-entrypoint-initdb.d
ADD scripts/data.sql /docker-entrypoint-initdb.d
RUN chmod a+r /docker-entrypoint-initdb.d/*
EXPOSE 6666