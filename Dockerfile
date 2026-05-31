# syntax=docker/dockerfile:1.7

############################# Stage 1 — build ####################################
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /workspace

# The repo keeps the Maven wrapper config under wrapper/ instead of the standard
# .mvn/wrapper/. Re-map it here so ./mvnw can resolve the distribution URL.
COPY mvnw pom.xml ./
COPY wrapper ./.mvn/wrapper

RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src ./src

RUN ./mvnw -B -DskipTests package \
    && mkdir -p target/extracted \
    && java -Djarmode=layertools -jar target/njplastic-api-*.jar extract --destination target/extracted

############################# Stage 2 — runtime ##################################
FROM eclipse-temurin:25-jre-alpine AS runtime
WORKDIR /app

ENV SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8111 \
    JAVA_OPTS=""

RUN addgroup --system --gid 1001 spring \
    && adduser --system --uid 1001 --ingroup spring spring

# Order matters: copy the layers least likely to change first to maximize cache reuse.
COPY --from=builder --chown=spring:spring /workspace/target/extracted/dependencies/ ./
COPY --from=builder --chown=spring:spring /workspace/target/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /workspace/target/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /workspace/target/extracted/application/ ./

USER spring

EXPOSE 8111

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS org.springframework.boot.loader.launch.JarLauncher"]
