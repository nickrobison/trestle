FROM openjdk:8-jdk-alpine

# Build time arguments
ARG JAR_FILE
ARG CONFIG_FILE

LABEL maintainer="Nick Robison <nick@nickrobison.com>"
LABEL description="Trestle Server image"

WORKDIR /opt/trestle

# Copy in server UberJar
ADD target/${JAR_FILE} ./trestle.jar
ADD src/main/resources/${CONFIG_FILE} config.yml
ADD target/trestle.owl .
ADD entrypoint.sh .

RUN chmod u+rx,g+rx,o+rx,a-w /opt/trestle/entrypoint.sh

ENTRYPOINT ["/opt/trestle/entrypoint.sh"]
CMD ["java", "-jar", "/opt/trestle/trestle.jar", "server", "/opt/trestle/config.yml"]



EXPOSE 8080
