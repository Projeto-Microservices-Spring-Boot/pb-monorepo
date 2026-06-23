ARG ECLIPSE_TEMURIN_VERSION_JDK=25-jdk-alpine
ARG ECLIPSE_TEMURIN_VERSION_JRE=25-jre-alpine

FROM eclipse-temurin:${ECLIPSE_TEMURIN_VERSION_JDK} AS build

RUN apk add --no-cache maven
RUN apk add --no-cache curl

WORKDIR /build

COPY pom.xml /build/pom.xml
COPY services/users /build/services/users
COPY services/shared /build/services/shared
# Remove stickers e outros que não existem
RUN sed -i '/services\/stickers/d' /build/pom.xml \
    && sed -i '/services\/payments/d' /build/pom.xml \
    && mvn clean package -pl services/users -am -DskipTests -B

FROM eclipse-temurin:${ECLIPSE_TEMURIN_VERSION_JRE} AS run

WORKDIR /app

COPY --from=build /build/services/users/target/*.jar app.jar

EXPOSE 8082

CMD [ "java" , "-jar", "app.jar" ]
