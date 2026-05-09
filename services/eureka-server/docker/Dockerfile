FROM amazoncorretto:25-alpine3.21

WORKDIR /app

COPY app.jar .

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]
