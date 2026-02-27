# Use Eclipse Temurin Java 17 JRE on Ubuntu 22.04
FROM eclipse-temurin:17-jre-jammy AS builder
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN ./mvnw dependency:go-offline -B

# Build
COPY src src
RUN ./mvnw package -DskipTests

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Create a non-root user
RUN groupadd --system --gid 1000 appuser && \
    useradd --system --gid appuser --uid 1000 appuser

# Copy the built JAR from the builder stage
COPY --from=builder --chown=appuser:appuser /app/target/*.jar app.jar

# Switch to non-root user
USER appuser

EXPOSE 8080

# Optimize
ENTRYPOINT ["java", \
            "-XX:+UseContainerSupport", \
            "-XX:MaxRAMPercentage=75.0", \
            "-Djava.security.egd=file:/dev/./urandom", \
            "-jar", "app.jar"]