FROM openjdk:8-jdk

# Build time arguments
ARG version="0.8.0-SNAPSHOT"

LABEL name="trestle-server" \
 maintainer="Nick Robison <nick@nickrobison.com>" \
 description="Trestle Server image"

ENV TRESTLE_HOME=/opt/trestle

# Copy in server UberJar
ADD target/trestle-server-${version}.jar /tmp
ADD src/main/resources/config.prod.yml /tmp
ADD trestle.owl /tmp

# Move the UberJar into the correct location
RUN mkdir -p ${TRESTLE_HOME} && \
 cd ${TRESTLE_HOME} && \
 mv /tmp/trestle-server-${version}.jar . && \
 mv trestle-server-${version}.jar trestle.jar && \
 mv /tmp/config.prod.yml . && \
 java -jar trestle.jar db migrate config.prod.yml && \
 mv /tmp/trestle.owl .

CMD ["-jar", "/opt/trestle/trestle.jar", "server", "/opt/trestle/config.prod.yml"]

ENTRYPOINT ["java"]

EXPOSE 8080