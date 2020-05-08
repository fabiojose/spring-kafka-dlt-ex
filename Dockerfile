FROM maven:3.6.3-jdk-8-slim as build-env

############
# Quando se utiliza um repositório privado p/ dependências
# ou existe proxy http
############
# COPY maven.xml /root/.m2/settings.xml

COPY pom.xml /usr/src/app/pom.xml

# Truque para cache das dependências
RUN mvn -f /usr/src/app/pom.xml \
        -B dependency:resolve-plugins dependency:resolve

COPY src /usr/src/app/src

RUN mvn -f /usr/src/app/pom.xml clean package

FROM openjdk:8-jdk-alpine
WORKDIR /usr/app
COPY --from=build-env /usr/src/app/target .
EXPOSE 8080
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/usr/app/app-spring-boot.jar"]

