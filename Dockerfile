# ─── Stage 1: Builder ─────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Cache Maven dependencies as a separate layer.
# This layer is only invalidated when pom.xml changes, not when source changes.
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

# ─── Stage 2: Runtime ─────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-jammy AS runtime

# curl is required by HEALTHCHECK
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Non-root user: spring uid=1000 gid=1000
RUN groupadd --gid 1000 spring \
    && useradd --uid 1000 --gid spring --shell /bin/bash --create-home spring

WORKDIR /app

# --chown avoids a separate RUN chown layer
COPY --chown=spring:spring --from=builder \
    /build/target/kura-backend-tutor-*.jar app.jar

USER spring

EXPOSE 8081

# start-period gives Spring Boot time to establish the Oracle FIAP connection
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
    CMD curl -f http://localhost:8081/api/actuator/health || exit 1

ENTRYPOINT ["java", \
    "-XX:MaxRAMPercentage=75.0", \
    "-XX:+UseG1GC", \
    "-jar", "app.jar"]
