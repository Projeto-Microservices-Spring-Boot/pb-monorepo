FROM eclipse-temurin:25-jdk-alpine AS deps

RUN apk add --no-cache maven
RUN apk add --no-cache curl

WORKDIR /build

COPY  pom.xml .

RUN mvn dependency:go-offline -B

FROM deps AS build

COPY src ./src

RUN mvn clean package -DskipTests 

FROM  eclipse-temurin:25-jre-alpine AS run

WORKDIR /app

COPY --from=build /build/target/*.jar app.jar

EXPOSE 8761

CMD [ "java" , "-jar", "app.jar" ]
