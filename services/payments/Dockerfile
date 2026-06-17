FROM eclipse-temurin:25-jdk-alpine AS deps

RUN apk add --no-cache maven
RUN apk add --no-cache curl

WORKDIR /build

# Instala o módulo shared no repositório Maven local do container
COPY shared/pom.xml ./shared/pom.xml
COPY shared/src ./shared/src
RUN mvn -f shared/pom.xml install -DskipTests -B

# Baixa dependências do payments
COPY payments/pom.xml ./payments/pom.xml
RUN mvn -f payments/pom.xml dependency:go-offline -B

FROM deps AS build

COPY payments/src ./payments/src

RUN mvn -f payments/pom.xml clean package -DskipTests

FROM eclipse-temurin:25-jre-alpine AS run

WORKDIR /app

COPY --from=build /build/payments/target/*.jar app.jar

EXPOSE 8085

CMD [ "java", "-jar", "app.jar" ]
