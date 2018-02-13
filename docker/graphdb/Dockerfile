FROM openjdk:8-jdk-alpine

# Build time arguments
ARG version=8.4.1

LABEL name="graphdb"
LABEL maintainer="Nick Robison <nick@nickrobison.com>"
LABEL description="GraphDB Docker image, copied from the Ontotext Github"

ENV GRAPHDB_PARENT_DIR=/opt/graphdb
ENV GRAPHDB_HOME=${GRAPHDB_PARENT_DIR}/home

ENV GRAPHDB_INSTALL_DIR=${GRAPHDB_PARENT_DIR}/dist

# We need to install bash for the startup scripts
RUN apk update && \
    apk add bash

ADD graphdb-free-${version}-dist.zip /tmp

# Unzip and setup the directories
RUN mkdir -p ${GRAPHDB_PARENT_DIR} && \
 cd ${GRAPHDB_PARENT_DIR} && \
 unzip /tmp/graphdb-free-${version}-dist.zip && \
 rm /tmp/graphdb-free-${version}-dist.zip && \
 mv graphdb-free-${version} dist && \
 mkdir -p ${GRAPHDB_HOME} && \
# Add the correct home path into the config files, that way we can use loadrdf in other containers
 echo "graphdb.home=/opt/graphdb/home" >> ${GRAPHDB_INSTALL_DIR}/conf/graphdb.properties

ENV PATH=${GRAPHDB_INSTALL_DIR}/bin:$PATH

ENTRYPOINT ["graphdb"]

EXPOSE 7200
