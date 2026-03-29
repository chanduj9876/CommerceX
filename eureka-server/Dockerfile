# Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy parent POM + module POM for dependency resolution
COPY pom.xml .
COPY eureka-server/pom.xml eureka-server/pom.xml

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -pl eureka-server -B

# Copy source and build
COPY eureka-server/src eureka-server/src
RUN mvn package -pl eureka-server -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/eureka-server/target/*.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
