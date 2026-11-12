FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

COPY pom.xml .
RUN mvn --batch-mode --no-transfer-progress dependency:go-offline

COPY src/ src/
RUN mvn --batch-mode --no-transfer-progress verify

FROM eclipse-temurin:21-jre-jammy AS runtime

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --gid 10001 app \
    && useradd --uid 10001 --gid app --no-create-home --shell /usr/sbin/nologin app

WORKDIR /app
COPY --from=build --chown=app:app /workspace/target/demo-tor-exit-nodes.jar app.jar
USER app:app

EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
    CMD curl --fail --silent --show-error --output /dev/null "http://localhost:${SERVER_PORT:-8080}/actuator/health" || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
