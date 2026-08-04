# ============================================================================
# backend/Dockerfile — TICKET-I125 (Day 10)
# ============================================================================
# WHAT:    Multi-stage Dockerfile for the Spring Boot recon-service.
# HOW:     Stage 1 builds the JAR with Maven. Stage 2 runs it on a slim JRE.
# WHY:     Keeps the runtime image < 250 MB and isolates build tools from
#          the final container.
# OBSERVE: `docker build .` succeeds; `docker run -p 8080:8080 <image>`
#          boots and `/actuator/health` returns UP.
# ============================================================================
# ============================================================================

# ---- Stage 1: build -------------------------------------------------------
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy pom.xml FIRST so the dependency layer caches independently of source —
# a one-line code change no longer re-downloads all of Maven Central.
COPY pom.xml .
RUN mvn -B -e dependency:go-offline

COPY src ./src
RUN mvn -B -e clean package -DskipTests

# ---- Stage 2: runtime -----------------------------------------------------
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# wget is needed by HEALTHCHECK below (alpine ships without curl/wget by default).
RUN apk add --no-cache wget

# Non-root user for security
RUN addgroup -S app && adduser -S app -G app
USER app

COPY --from=build /workspace/target/*.jar /app/app.jar

EXPOSE 8080

# Spring Boot probes
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=5 \
    CMD wget -qO- http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-jar", "/app/app.jar"]
