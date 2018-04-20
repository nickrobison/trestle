FROM docker.nickrobison.com:5000/graphdb:8.5.0-free

LABEL name="evaluation-data"
LABEL maintainer="Nick Robison <nick@nickrobison.com>"
LABEL description="GraphDB image which contains the data needed for the evaulation"

WORKDIR /opt

ADD statements.rdf .
ADD trestle_demo-config.ttl ./config.ttl

RUN loadrdf -vm parallel -c config.ttl statements.rdf && \
    rm statements.rdf config.ttl
