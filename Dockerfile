# Multi-stage Dockerfile for University Management System
# Build stage
FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

# Copy dependency definition first for caching
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build production jar (skip tests in docker build for speed)
COPY src ./src
RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine AS runner

WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl tzdata

# Security: run application as a non-privileged user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser:appgroup

# Copy built artifact from builder stage
COPY --from=builder --chown=appuser:appgroup /build/target/*.jar app.jar

# Configuration via environment variables
ENV SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
  CMD curl -f http://localhost:8080/api-docs || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
