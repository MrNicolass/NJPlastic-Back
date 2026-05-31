# NJPlastic API

Spring Boot backend of the NJPlastic Web-IoT platform for monitoring plastic injection machines. Captures production cycle pulses over MQTT, persists them in PostgreSQL, exposes a REST API consumed by the React frontend, and synchronizes selected data with the Client's ERP database over plain JDBC.

Architectural decisions, requirements and roadmap live in the project RFC at `../../Doc/NJPlastic/README.md`.

## Tech Stack

| Layer | Technology | Version |
|-------|-----------|---------|
| Language | Java | 25 (LTS) |
| Framework | Spring Boot | 4.0.6 |
| Security | Spring Security + JJWT | 7.x / 0.12.6 |
| Persistence (local) | Spring Data JPA (Hibernate) | bundled |
| Persistence (ERP) | JDBC native (`JdbcTemplate`) | per vendor driver |
| MQTT client | Eclipse Paho v3 | 1.2.5 |
| API docs | SpringDoc OpenAPI | 3.0.2 |
| Migrations | Flyway | bundled |
| Database | PostgreSQL | 17 |
| MQTT broker | Mosquitto | 2.x |

## Prerequisites

Either:

- **Docker route** — Docker 26+ and Compose v2;
- **Local route** — JDK 25, Maven 3.9+, PostgreSQL 17 and Mosquitto 2 reachable on `localhost`.

## Quick Start (Docker Compose)

The Compose stack lives at `/home/xserver/Docker/NJPlastic/docker-compose.yml` and orchestrates PostgreSQL (application DB), a PostgreSQL ERP mock, Mosquitto, the backend and the frontend on a shared `dbnetw` bridge network.

```bash
# 1. Build the backend image (run from this directory)
docker build -t njplastic-backend:1.0.0 .

# 2. Build the frontend image
docker build -t njplastic-frontend:0.1.0 ../../Frontend/NJPlastic-Front

# 3. Bring the stack up
docker compose -f /home/xserver/Docker/NJPlastic/docker-compose.yml up -d
```

Service endpoints once running:

| Service | URL |
|---------|-----|
| Frontend | http://localhost:3000 |
| Backend REST | http://localhost:8111 |
| Swagger UI | http://localhost:8111/swagger-ui.html |
| OpenAPI JSON | http://localhost:8111/v3/api-docs |
| PostgreSQL (app) | localhost:8125 (db `njplastic`, user `admin`) |
| PostgreSQL (ERP mock) | localhost:8126 (db `meplas_erp`, user `meplas`) |
| Mosquitto MQTT | localhost:1883 (anonymous, single topic `njplastic/pulso`) |

The Compose file ships with the `dev` Spring profile and the local default credentials wired in. Override anything via `environment:` entries or a `.env` file alongside the compose.

## Local Development (without Docker)

Start the supporting containers only (Postgres + ERP mock + Mosquitto) and run the app from your IDE or Maven:

```bash
docker compose -f /home/xserver/Docker/NJPlastic/docker-compose.yml up -d \
  njplastic-api-postgresql njplastic-erp-mock-postgresql njplastic-mosquitto

mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

The `dev` profile loads the additional Flyway location `db/migration-dev` which seeds development users (see `V2__seed_dev_users.sql`).

## Spring Profiles

| Profile | When to use | What it does |
|---------|-------------|--------------|
| (none) | Library / test defaults | Reads `application.properties` placeholders; all secrets fall back to local defaults. |
| `dev` | Local development | Adds dev seed users, permissive CORS, relaxed JWT secret, points the ERP datasource at the local mock on port 8126. |
| `prod` | Real deployments | Requires `JWT_SECRET` and `CORS_ALLOWED_ORIGINS` to be set in the environment (startup fails fast otherwise). Disables devtools and the dev-only Flyway location. |

## Environment Variables

All settings are exposed as `${VAR:default}` in `application.properties`. The table lists what's worth knowing per deployment.

### Required in `prod`

| Variable | Description |
|----------|-------------|
| `JWT_SECRET` | Base64 secret used to sign HS256 JWTs (>= 64 bytes). No default in `prod`. |
| `CORS_ALLOWED_ORIGINS` | Comma-separated list of allowed origins for browser calls. No default in `prod`. |
| `POSTGRES_URL` | JDBC URL of the application database. |
| `POSTGRES_USER` / `POSTGRES_PASSWORD` | Credentials for the application database. |

### Datasources

| Variable | Default | Purpose |
|----------|---------|---------|
| `POSTGRES_URL` | `jdbc:postgresql://localhost:8125/njplastic` | Application DB. |
| `POSTGRES_USER` | `admin` | Application DB user. |
| `POSTGRES_PASSWORD` | `admin` | Application DB password. |
| `HIKARI_MAX_POOL_SIZE` | `30` | Max pool size for the application DB. |
| `ERP_DATASOURCE_ENABLED` | `false` | Enables the ERP `@ConditionalOnProperty` beans (repository, scheduler, row mapper). |
| `ERP_URL` / `ERP_USER` / `ERP_PASSWORD` / `ERP_DRIVER` | empty | ERP datasource — fill per Client. |
| `ERP_QUERY_FIND_OPEN_ORDERS` | empty (mock value in `dev`) | SELECT returning `erp_order_id, machine_code, product_code, target_quantity, status, payload_json`. |
| `ERP_QUERY_INSERT_CYCLE` | empty (mock value in `dev`) | Idempotent INSERT for production cycles. |
| `ERP_QUERY_INSERT_PAUSE` | empty (mock value in `dev`) | Idempotent INSERT for pause records. |
| `ERP_QUERY_PING` | `SELECT 1` | Health-check query used by `GET /erp/sync/status`. |

### ERP sync scheduler

| Variable | Default |
|----------|---------|
| `ERP_SYNC_FIXED_DELAY_MS` | `60000` |
| `ERP_SYNC_INITIAL_DELAY_MS` | `30000` |
| `ERP_SYNC_HISTORY_PAGE_SIZE` | `20` |
| `ERP_SYNC_BATCH_PAGE_SIZE` | `200` |
| `ERP_SYNC_KPI_WINDOW_HOURS` | `24` |

### Security and audit

| Variable | Default |
|----------|---------|
| `JWT_SECRET` | dev value in `application.properties`; **no default in `prod`**. |
| `JWT_EXPIRATION_MINUTES` | `480` |
| `JWT_ISSUER` | `NJPlastic` |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000`; **no default in `prod`**. |
| `SECURITY_PUBLIC_PATHS` | login + Swagger + versioning endpoints |
| `AUDIT_MAX_PAYLOAD_BYTES` | `10240` |

### MQTT

| Variable | Default |
|----------|---------|
| `MQTT_BROKER_URL` | `tcp://localhost:1883` |
| `MQTT_CLIENT_ID` | `njplastic-backend` |
| `MQTT_TOPIC` | `njplastic/pulso` |
| `MQTT_QOS` | `1` |
| `MQTT_USERNAME` / `MQTT_PASSWORD` | empty (anonymous) |
| `MQTT_RECONNECT_INTERVAL_MS` | `30000` |

### Production processing

| Variable | Default |
|----------|---------|
| `PRODUCTION_TIMEZONE` | `America/Sao_Paulo` |
| `CLOCK_TOLERANCE_MS` | `300000` |
| `WATCHDOG_INTERVAL_MS` | `30000` |
| `AUTO_STOP_MESSAGE` | default Portuguese message used when an auto stop is opened. |
| `PAUSE_SCAN_LIMIT` | `200` |

## Build Commands

```bash
# Run all tests
mvn test

# Run a single test class
mvn test -Dtest=ClassName

# Package a runnable layered JAR (skips tests)
mvn -B -DskipTests package

# Build the Docker image (uses the multi-stage Dockerfile in this directory)
docker build -t njplastic-backend:1.0.0 .
```

`mvn` is preferred over `./mvnw` because the Maven wrapper config in this repo lives under `wrapper/` instead of the standard `.mvn/wrapper/` and the wrapper script expects the latter. The Dockerfile remaps the path internally so `./mvnw` works inside the build container.

## Testing

Unit tests follow JUnit 5 + Mockito and are generated by the `java-tests` Claude Code skill on every push to a feature branch (see `.github/workflows/generate-tests.yml`). Refer to RFC §5.5 and the vault note `[[GitHub Actions - Geração Automática de Testes]]` for the full workflow.

Manual generation:

```bash
mvn test
```

## API Documentation

Swagger UI is exposed at `/swagger-ui.html` and the raw OpenAPI JSON at `/v3/api-docs`. The `bearerAuth` security scheme is pre-registered — paste a JWT obtained from `POST /auth/login` in the Swagger "Authorize" dialog.

## Health and Observability

Logging follows SLF4J + Logback (Spring Boot defaults); structured JSON output and shipment to Loki/Grafana are planned for a post-MVP release (RFC §7.5).

`spring-boot-starter-actuator` is intentionally not on the classpath today, so there's no `/actuator/health` endpoint — container health checks should rely on TCP probes or on hitting `/swagger-ui.html` until actuator is added in a later epic.

## Project Layout

Top-level packages under `com.njplastic.njplastic_api`:

| Package | Responsibility |
|---------|---------------|
| `auth/` | Authentication, JWT issuing and validation. |
| `audit/` | Append-only request auditing via `OncePerRequestFilter` (`audit_log` table). |
| `production/` | Domain logic: machines, cycles, pauses, OEE, MQTT subscriber. |
| `erp/` | JDBC ERP integration, scheduled sync and the `GET /erp/sync/status` endpoint. |
| `config/` | Datasources, OpenAPI, security configuration and base exception classes. |
| `common/` | Cross-cutting DTOs (e.g. `ErrorResponseDTO`). |

See the vault entry `[[Backend]]` for code conventions (Lombok pattern, DTO suffix rule, exception hierarchy, fail-fast guard clauses).
