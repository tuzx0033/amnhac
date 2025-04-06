# Build stage
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package

# Run stage
FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/demo.music-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
CMD ["java", "-Dserver.port=$PORT", "-jar", "app.jar"]