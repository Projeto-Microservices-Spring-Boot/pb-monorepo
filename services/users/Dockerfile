ARG ECLIPSE_TEMURIN_VERSION_JDK=25-jdk-alpine
ARG ECLIPSE_TEMURIN_VERSION_JRE=25-jre-alpine

FROM eclipse-temurin:${ECLIPSE_TEMURIN_VERSION_JDK} AS deps

RUN apk add --no-cache maven
RUN apk add --no-cache curl

WORKDIR /build

COPY  pom.xml .

RUN mvn dependency:go-offline -B

FROM deps AS build

COPY src ./src

RUN mvn clean package -DskipTests 

FROM eclipse-temurin:${ECLIPSE_TEMURIN_VERSION_JRE} AS run

WORKDIR /app

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8082

CMD [ "java" , "-jar", "app.jar" ]
