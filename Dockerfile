FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace
COPY gradlew settings.gradle.kts build.gradle.kts ./
COPY gradle gradle
RUN ./gradlew --no-daemon dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
COPY --from=build /workspace/build/agent/elastic-apm-agent.jar elastic-apm-agent.jar
# ELASTIC_APM_* env vars (server_url, service_name, ...) are read by the agent directly
ENTRYPOINT ["java", "-javaagent:/app/elastic-apm-agent.jar", "-jar", "/app/app.jar"]
