FROM openjdk:8-jdk

# Build time arguments
ARG JAR_FILE

LABEL maintainer="Nick Robison <nick@nickrobison.com>"
LABEL description="Trestle Server image"

WORKDIR /opt/trestle

# Copy in server UberJar
ADD target/${JAR_FILE} ./trestle.jar
ADD src/main/resources/config.prod.yml .
ADD target/trestle.owl .

CMD ["-jar", "/opt/trestle/trestle.jar", "server", "/opt/trestle/config.prod.yml"]

ENTRYPOINT ["java"]

EXPOSE 8080
