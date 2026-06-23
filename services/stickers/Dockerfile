ARG ECLIPSE_TEMURIN_VERSION_JDK=25-jdk-alpine
ARG ECLIPSE_TEMURIN_VERSION_JRE=25-jre-alpine

FROM eclipse-temurin:${ECLIPSE_TEMURIN_VERSION_JDK} AS build

RUN apk add --no-cache maven
RUN apk add --no-cache curl

WORKDIR /build

COPY pom.xml /build/pom.xml
COPY services/stickers /build/services/stickers
COPY services/shared /build/services/shared
# Remove módulos que não existem nesse contexto
RUN sed -i '/services\/users/d' /build/pom.xml \
    && sed -i '/services\/payments/d' /build/pom.xml \
    && mvn clean package -pl services/stickers -am -DskipTests -B

FROM eclipse-temurin:${ECLIPSE_TEMURIN_VERSION_JRE} AS run

WORKDIR /app

COPY --from=build /build/services/stickers/target/*.jar app.jar

EXPOSE 8087

CMD [ "java" , "-jar", "app.jar" ]
