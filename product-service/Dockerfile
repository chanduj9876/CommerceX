# Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy parent POM + module POMs for dependency resolution
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY product-service/pom.xml product-service/pom.xml

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -pl common-lib,product-service -am -B

# Copy source and build
COPY common-lib/src common-lib/src
COPY product-service/src product-service/src
RUN mvn package -pl common-lib,product-service -am -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/product-service/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
