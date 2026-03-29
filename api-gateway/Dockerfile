# Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy parent POM + module POM for dependency resolution
# api-gateway does NOT depend on common-lib (WebFlux/servlet conflict)
COPY pom.xml .
COPY api-gateway/pom.xml api-gateway/pom.xml

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -pl api-gateway -B

# Copy source and build
COPY api-gateway/src api-gateway/src
RUN mvn package -pl api-gateway -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
