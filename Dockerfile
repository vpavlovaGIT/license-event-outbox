FROM eclipse-temurin:17-jdk-alpine
WORKDIR /app
COPY target/license-event-outbox-1.0.0.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]