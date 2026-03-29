# Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy parent POM + module POMs for dependency resolution
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY shipping-service/pom.xml shipping-service/pom.xml

# Download dependencies (cached layer)
RUN mvn dependency:go-offline -pl common-lib,shipping-service -am -B

# Copy source and build
COPY common-lib/src common-lib/src
COPY shipping-service/src shipping-service/src
RUN mvn package -pl common-lib,shipping-service -am -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/shipping-service/target/*.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
