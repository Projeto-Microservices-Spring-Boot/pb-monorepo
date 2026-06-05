ARG ECLIPSE_TEMURIN_VERSION_JDK=25-jdk-alpine
ARG ECLIPSE_TEMURIN_VERSION_JRE=25-jre-alpine

FROM eclipse-temurin:${ECLIPSE_TEMURIN_VERSION_JDK} AS build

RUN apk add --no-cache maven
RUN apk add --no-cache curl

WORKDIR /build

COPY pom.xml .

# Copia o módulo shared pois o Maven precisa do código fonte dele
COPY services/shared services/shared  
COPY services/users services/users

# -pl diz ao Maven qual módulo buildar
# -am faz o Maven compilar o shared antes do users
RUN mvn clean package -pl services/users -am -DskipTests -B  

FROM eclipse-temurin:${ECLIPSE_TEMURIN_VERSION_JRE} AS run

WORKDIR /app

COPY --from=build /build/services/users/target/*.jar app.jar

EXPOSE 8082

CMD [ "java" , "-jar", "app.jar" ]
