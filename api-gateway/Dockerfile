# Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder
WORKDIR /app

# Copy parent POM + all module POMs for dependency resolution
# api-gateway does NOT depend on common-lib (WebFlux/servlet conflict)
COPY pom.xml .
COPY common-lib/pom.xml common-lib/pom.xml
COPY eureka-server/pom.xml eureka-server/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY product-service/pom.xml product-service/pom.xml
COPY order-service/pom.xml order-service/pom.xml
COPY payment-service/pom.xml payment-service/pom.xml
COPY shipping-service/pom.xml shipping-service/pom.xml

# Copy source and build
COPY api-gateway/src api-gateway/src
RUN mvn package -pl api-gateway -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder /app/api-gateway/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
