# Spring Boot + Elastic Observability

A local, runnable version of InfoWorld's
[Bring Elastic Observability to your Java application](https://www.infoworld.com/article/2258527/bring-elastic-observability-to-your-java-application.html).

The article runs Spring's `accessing-data-mysql` guide on a GCP VM against MariaDB and ships
everything to Elastic Cloud. Here the whole setup runs in Docker Compose:

| Article (Elastic Cloud + VM)        | This repo                                              |
|-------------------------------------|--------------------------------------------------------|
| Elastic Cloud deployment            | `elasticsearch` + `kibana` (8.19, security disabled)   |
| Cloud APM endpoint                  | `apm-server` on `:8200`                                |
| `mvnw` patched with `-javaagent`    | `./gradlew bootRun` (agent jar pulled from Maven)      |
| Filebeat `mysql system auditd`      | `filebeat`: MariaDB error/slow logs + app ECS JSON logs |
| Metricbeat `mysql system`           | `metricbeat`: mysql, system, docker                   |
| Heartbeat POST `/demo/add`          | `heartbeat`: same monitor, plus `/actuator/health`     |
| MariaDB slow log `50-server.cnf`    | `docker/mariadb/50-server.cnf` (verbatim)              |

Beyond the article: the app also uses the APM public API (`@CaptureSpan`, transaction labels)
and logs in ECS JSON, so Kibana can link a trace to its log lines through `trace.id`.

## Run everything

```bash
docker compose up -d --build     # needs about 3 GB of free Docker memory
```

Then open:

- Kibana: <http://localhost:5601>
  - **Observability → APM → Services → winter-mysql**: transactions like `MainController#addNewUser`,
    each with an `addUser` span wrapping the JDBC `INSERT INTO user` span
  - **Observability → Uptime / Synthetics**: the `SpringToDoApp` monitor
  - **Dashboards**: `[Metricbeat MySQL] Database Metrics`, `[Filebeat MySQL] Overview`
  - **Discover → logs-\***: app logs, filter by `trace.id`
- App: `curl -X POST localhost:8080/demo/add -d name=ed -d email=ed@example.com`,
  `curl localhost:8080/demo/all`

## Run the app from your IDE or shell instead

```bash
docker compose up -d elasticsearch kibana apm-server mariadb
./gradlew bootRun
```

`bootRun` attaches `build/agent/elastic-apm-agent.jar` and reads the same variables the article
sets. To point at Elastic Cloud instead of the local stack:

```bash
export ELASTIC_APM_SERVER_URL=https://<deployment>.apm.<region>.cloud.es.io:443
export ELASTIC_APM_SECRET_TOKEN=<token>
./gradlew bootRun
```

## Layout

```
src/main/java/com/example/demo/   User, UserRepository, UserService (@CaptureSpan), MainController
src/main/resources/               application.yml, logback-spring.xml (ECS encoder)
docker/                           mariadb / filebeat / metricbeat / heartbeat configs
Dockerfile, docker-compose.yml
```

## Notes

- Security is turned off on ES, Kibana and APM Server so the demo starts without setup.
  Don't expose these ports outside your machine.
- Filebeat's `system` and `auditd` modules from the article are left out because they need a real
  Linux host's `/var/log` and audit subsystem. Enable them when Filebeat runs on the VM itself.
- Elasticsearch runs with a 512 MB heap. If it exits with code 137, Docker is out of memory.
