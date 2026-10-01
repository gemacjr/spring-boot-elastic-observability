plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "1.0-SNAPSHOT"

java {
    toolchain { languageVersion = JavaLanguageVersion.of(17) }
}

repositories {
    mavenCentral()
}

// The Elastic APM agent is attached with -javaagent, not put on the classpath.
val apmAgent: Configuration by configurations.creating
val elasticApmVersion = "1.57.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    // Public API: @CaptureSpan, labels on the current transaction
    implementation("co.elastic.apm:apm-agent-api:$elasticApmVersion")
    // ECS-formatted JSON logs so Filebeat logs correlate with APM traces
    implementation("co.elastic.logging:logback-ecs-encoder:1.8.0")
    runtimeOnly("org.mariadb.jdbc:mariadb-java-client")

    apmAgent("co.elastic.apm:elastic-apm-agent:$elasticApmVersion")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Copies the agent jar to build/agent/elastic-apm-agent.jar (used by bootRun and the Dockerfile)
val copyApmAgent by tasks.registering(Copy::class) {
    from(apmAgent)
    into(layout.buildDirectory.dir("agent"))
    rename { "elastic-apm-agent.jar" }
}

tasks.bootJar { dependsOn(copyApmAgent) }

// Equivalent of the article's patched mvnw: run the app with the agent attached.
tasks.bootRun {
    dependsOn(copyApmAgent)
    jvmArgs(
        "-javaagent:${layout.buildDirectory.file("agent/elastic-apm-agent.jar").get().asFile}",
        "-Delastic.apm.service_name=${System.getenv("ELASTIC_APM_SERVICE_NAME") ?: "winter-mysql"}",
        "-Delastic.apm.server_url=${System.getenv("ELASTIC_APM_SERVER_URL") ?: "http://localhost:8200"}",
        "-Delastic.apm.secret_token=${System.getenv("ELASTIC_APM_SECRET_TOKEN") ?: ""}",
        "-Delastic.apm.application_packages=com.example",
        "-Delastic.apm.environment=local",
    )
}

tasks.test {
    useJUnitPlatform()
}
