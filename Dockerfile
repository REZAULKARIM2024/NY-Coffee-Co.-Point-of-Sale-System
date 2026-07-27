# Containerizes only the REST API (com.possystem.api.ApiServer) — the Swing desktop app needs a
# display and isn't a meaningful thing to containerize. This exists mainly so `docker compose up`
# gives a one-command way to try the API + admin dashboard against a real MySQL instance without
# installing Java/Maven/MySQL locally first.

# ---- Build stage ----
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# Cache dependency resolution separately from source changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q -DskipTests package \
    && mvn -B -q dependency:copy-dependencies -DoutputDirectory=target/libs -DincludeScope=runtime

# ---- Runtime stage ----
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /build/target/classes ./classes
COPY --from=build /build/target/libs ./libs

EXPOSE 8081
ENTRYPOINT ["java", "-cp", "classes:libs/*", "com.possystem.api.ApiServer", "8081"]
