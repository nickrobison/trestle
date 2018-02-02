FROM docker.nickrobison.com:5000/graphdb:8.4.1-free

LABEL name="evaluation-data"
LABEL maintainer="Nick Robison <nick@nickrobison.com>"
LABEL description="GraphDB image which contains the data needed for the evaulation"

WORKDIR /opt

ADD statements.rdf .
ADD gaul_hadoop_world_bank-config.ttl ./config.ttl

RUN loadrdf -vm parallel -c config.ttl statements.rdf
