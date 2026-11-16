# Store Management REST API

A Spring Boot REST API for managing a store's **products**, **categories** and **inventory**.
It is built for the EEI6369 AWS Cloud Computing practical assessment. It runs in Docker on
**AWS EC2** and stores data in **Amazon RDS for PostgreSQL**.

Project memory and status: [`.plan`](.plan) · Background: [`PROJECT_CONTEXT.md`](PROJECT_CONTEXT.md) ·
Changes: [`CHANGELOG.md`](CHANGELOG.md)

---

## Contents
1. [Technology stack](#technology-stack)
2. [Architecture](#architecture)
3. [Prerequisites](#prerequisites)
4. [Environment variables](#environment-variables)
5. [Local setup](#local-setup)
6. [Maven commands](#maven-commands)
7. [Docker commands](#docker-commands)
8. [Docker Compose commands](#docker-compose-commands)
9. [API endpoints](#api-endpoints)
10. [Health monitoring](#health-monitoring)
11. [Testing](#testing)
12. [CI/CD (DevSecOps pipeline)](#cicd-devsecops-pipeline)
13. [AWS deployment considerations](#aws-deployment-considerations)

---

## Technology stack

| Area | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.3.5 (Web, Data JPA, Validation, Actuator) |
| Build | Maven (the `./mvnw` wrapper is included, so a local Maven install is optional) |
| Database | PostgreSQL 16 (Amazon RDS in production) |
| Schema migrations | Flyway. Hibernate only **validates** the schema |
| API docs | springdoc-openapi 2.6 (Swagger UI) |
| Tests | JUnit 5, Mockito, Spring MockMvc, H2 (PostgreSQL mode) |
| Container | Docker multi-stage build, `eclipse-temurin:17-jre-alpine`, non-root user |
| CI/CD | GitHub Actions: Gitleaks, Trivy, Maven tests, image scan, smoke test, GHCR publish |

## Architecture

Layered architecture with constructor-based dependency injection. DTOs are kept separate from JPA entities.

```
HTTP ─► Controller ─► Service ─► Repository ─► PostgreSQL
          │  (DTOs,      │ (business rules,   (Spring Data JPA,
          │  @Valid)     │  transactions)      Specifications, locks)
          ▼              ▼
   GlobalExceptionHandler (@RestControllerAdvice) ─► consistent JSON errors
```

```
src/main/java/com/store/management/
├── controller/   ProductController, CategoryController        (REST + OpenAPI annotations)
├── service/      ProductService, CategoryService              (business logic, @Transactional)
├── repository/   ProductRepository, CategoryRepository,
│                 ProductSpecifications                        (data access, search/filter)
├── entity/       Product, Category                            (JPA entities)
├── dto/          *Request / *Response records, ApiErrorResponse
├── exception/    custom exceptions + GlobalExceptionHandler
├── config/       OpenApiConfig
└── StoreManagementApplication.java
src/main/resources/
├── application.yml          default / production config (env vars only, ddl-auto=validate)
├── application-local.yml    optional local profile (localhost defaults, SQL logging)
└── db/migration/V1__create_store_schema.sql   Flyway schema
```

Key design points:
- **Schema safety:** Flyway creates and versions the schema. Hibernate uses `ddl-auto: validate`, so it never changes the production database.
- **SKU uniqueness:** SKUs are trimmed and stored in upper case. Duplicates are rejected case-insensitively by the service (HTTP 409) and by a DB unique constraint.
- **Safe inventory:** stock changes take a row-level lock (`PESSIMISTIC_WRITE`), so concurrent requests can't make stock negative. A DB `CHECK (quantity >= 0)` is the last line of defence.
- **No data leaks in errors:** unexpected errors are logged server-side and return a generic message.

## Prerequisites

| For | You need |
|---|---|
| Building and running tests | JDK 17 (Maven is optional because `./mvnw` downloads it) |
| Running locally with a DB | Docker Desktop (or a local PostgreSQL 14+) |
| Deployment | AWS account, EC2 instance with Docker, RDS PostgreSQL instance |

## Environment variables

| Variable | Required | Default | Description |
|---|---|---|---|
| `DB_HOST` | yes (prod) | `localhost` with the `local` profile | Database host, e.g. the RDS endpoint |
| `DB_PORT` | no | `5432` | Database port |
| `DB_NAME` | yes (prod) | `storedb` with the `local` profile | Database name |
| `DB_USERNAME` | **yes** | none | Database user |
| `DB_PASSWORD` | **yes** | none | Database password |
| `SPRING_PROFILES_ACTIVE` | no | none (production settings) | Set to `local` for local development |
| `SERVER_PORT` | no | `8080` | HTTP port |
| `DB_POOL_SIZE` | no | `10` | Hikari max pool size |
| `JAVA_OPTS` | no | `-XX:MaxRAMPercentage=75.0 ...` | JVM options (Docker image only) |

There are no credentials in the code. For local development, copy `.env.example` to `.env`
(`.env` is git-ignored) and set your own values.

## Local setup

### Option A: everything in Docker (easiest)
```bash
cp .env.example .env          # then edit DB_PASSWORD
docker compose up --build -d
curl http://localhost:8080/actuator/health
```
Swagger UI: <http://localhost:8080/swagger-ui/index.html>

### Option B: PostgreSQL in Docker, application from your IDE or Maven
```bash
cp .env.example .env
docker compose up -d db                    # only the database
# PowerShell:  $env:DB_USERNAME="store_user"; $env:DB_PASSWORD="change-me"; $env:SPRING_PROFILES_ACTIVE="local"
# bash:        export DB_USERNAME=store_user DB_PASSWORD=change-me SPRING_PROFILES_ACTIVE=local
./mvnw spring-boot:run
```

## Maven commands

Use `./mvnw` on Linux/macOS/Git Bash, `.\mvnw.cmd` on Windows PowerShell/cmd, or `mvn` if Maven is installed.

| Command | Purpose |
|---|---|
| `./mvnw clean verify` | Compile, run all tests, package the jar |
| `./mvnw test` | Run tests only |
| `./mvnw test -Dtest=ProductServiceTest` | Run one test class |
| `./mvnw clean package -DskipTests` | Build the jar without tests → `target/store-management-api.jar` |
| `./mvnw spring-boot:run` | Run the application (needs DB env vars) |
| `java -jar target/store-management-api.jar` | Run the packaged jar |

## Docker commands

```bash
# Build the image (multi-stage: Maven/JDK 17 build → JRE 17 Alpine runtime, non-root user)
docker build -t store-management-api:1.0.0 .

# Run against any PostgreSQL (e.g. Amazon RDS)
docker run -d --name store-api -p 8080:8080 \
  -e DB_HOST=<db-host> -e DB_PORT=5432 -e DB_NAME=storedb \
  -e DB_USERNAME=<user> -e DB_PASSWORD=<password> \
  --restart unless-stopped store-management-api:1.0.0

docker logs -f store-api                                    # follow logs
docker inspect --format '{{.State.Health.Status}}' store-api   # container health (healthy/unhealthy)
docker stop store-api && docker rm store-api
```

## Docker Compose commands

| Command | Purpose |
|---|---|
| `docker compose up --build -d` | Build and start the API and PostgreSQL |
| `docker compose up -d db` | Start only PostgreSQL |
| `docker compose ps` | Show status and health |
| `docker compose logs -f app` | Follow API logs |
| `docker compose down` | Stop containers (data is kept in the `postgres-data` volume) |
| `docker compose down -v` | Stop and **delete** the database volume |

## API endpoints

Base URL: `http://<host>:8080`. Interactive docs: **`/swagger-ui/index.html`**. OpenAPI JSON: `/v3/api-docs`.

### Products
| Method | Path | Description | Success |
|---|---|---|---|
| GET | `/api/products` | List all products | 200 |
| GET | `/api/products?name=mouse` | Search by name (contains, case-insensitive) | 200 |
| GET | `/api/products?category=Electronics` | Filter by category (case-insensitive) | 200 |
| GET | `/api/products/{id}` | Get a product by ID | 200 |
| POST | `/api/products` | Create a product | 201 + `Location` header |
| PUT | `/api/products/{id}` | Update a product | 200 |
| DELETE | `/api/products/{id}` | Delete a product | 204 |

### Inventory
| Method | Path | Body | Description | Success |
|---|---|---|---|---|
| GET | `/api/products/{id}/stock?required=10` | none | Check available stock (`required` defaults to 1) | 200 |
| POST | `/api/products/{id}/stock/increase` | `{"quantity": 5}` | Increase stock | 200 |
| POST | `/api/products/{id}/stock/decrease` | `{"quantity": 5}` | Decrease stock. Returns 409 if insufficient | 200 |

### Categories
| Method | Path | Description | Success |
|---|---|---|---|
| GET | `/api/categories` | List categories | 200 |
| GET | `/api/categories/{id}` | Get a category | 200 |
| POST | `/api/categories` | Create a category (unique name) | 201 |
| PUT | `/api/categories/{id}` | Update a category | 200 |
| DELETE | `/api/categories/{id}` | Delete a category | 204 |

### Examples
```bash
# Create a category
curl -X POST http://localhost:8080/api/categories -H "Content-Type: application/json" \
  -d '{"name":"Electronics","description":"Computers and accessories"}'

# Create a product
curl -X POST http://localhost:8080/api/products -H "Content-Type: application/json" \
  -d '{"name":"Wireless Mouse","description":"2.4 GHz mouse","sku":"WM-1001","category":"Electronics","price":24.99,"quantity":50}'
```
Response `201 Created`:
```json
{
  "id": 1, "name": "Wireless Mouse", "description": "2.4 GHz mouse", "sku": "WM-1001",
  "category": "Electronics", "price": 24.99, "quantity": 50,
  "createdAt": "2026-11-16T10:15:30.123Z", "updatedAt": "2026-11-16T10:15:30.123Z"
}
```
```bash
curl -X POST http://localhost:8080/api/products/1/stock/decrease -H "Content-Type: application/json" -d '{"quantity":5}'
curl "http://localhost:8080/api/products/1/stock?required=100"
```

### Error format
Every error uses the same JSON body:
```json
{
  "timestamp": "2026-11-16T10:15:30.123Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/products",
  "validationErrors": { "price": "price must be positive", "name": "name is required" }
}
```
| Status | When |
|---|---|
| 400 | Validation failure, malformed JSON, invalid parameter type |
| 404 | Resource or path not found |
| 405 / 415 | Wrong HTTP method / content type |
| 409 | Duplicate SKU or category name, insufficient stock |
| 500 | Unexpected error (details only in server logs) |

Validation rules: product `name` is required (max 150 characters). `price` is required, > 0, with at most 2 decimals.
`quantity` is required and ≥ 0. `sku` is optional, unique, and may contain only letters, digits, `.`, `_` and `-` (max 64).
Stock adjustment `quantity` is between 1 and 1,000,000.

## Health monitoring

Only `health` and `info` are exposed over HTTP. Health shows each component's status but no
sensitive details such as the DB URL, vendor or environment.

```bash
curl http://localhost:8080/actuator/health
```
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "diskSpace": { "status": "UP" },
    "livenessState": { "status": "UP" },
    "ping": { "status": "UP" },
    "readinessState": { "status": "UP" }
  }
}
```
If the database is unreachable, `db` and the overall status become `DOWN` and the endpoint returns HTTP 503.
Probe endpoints are also available: `/actuator/health/liveness` and `/actuator/health/readiness`.
`/actuator/info` returns the application name, version, build and Java information.

## Testing

```bash
./mvnw test
```
| Type | Classes | What they cover |
|---|---|---|
| Service unit tests (Mockito) | `ProductServiceTest`, `CategoryServiceTest` | CRUD, duplicate SKU or name, not found, stock increase/decrease, insufficient stock |
| Controller tests (`@WebMvcTest`) | `ProductControllerTest`, `CategoryControllerTest` | Status codes, JSON, validation errors, error mapping |
| Repository tests (`@DataJpaTest`) | `ProductRepositoryTest`, `CategoryRepositoryTest` | Flyway schema, unique and check constraints, search and filter |
| Integration tests (`@SpringBootTest`) | `StoreManagementIntegrationTest` | Full product, inventory and category lifecycle, actuator health and info, OpenAPI |

Tests use an in-memory H2 database in PostgreSQL mode, so no Docker is needed to run them.
The Docker smoke test in CI checks the image against real PostgreSQL.

## CI/CD (DevSecOps pipeline)

`.github/workflows/devsecops.yml` runs on pushes and pull requests to `main`:

1. **Secret scan:** Gitleaks over the full Git history.
2. **Build & test:** `mvn verify` on Java 17. Test reports and the jar are uploaded as artifacts.
3. **Dependency & config scan:** Trivy filesystem scan (vulnerable dependencies, Dockerfile misconfigurations, secrets). Fails on fixable CRITICAL/HIGH findings.
4. **Docker:** builds the image, scans it with Trivy, then smoke-tests it against a PostgreSQL container until `/actuator/health` is `UP`.
5. **Publish** (pushes to `main` only): pushes `ghcr.io/<owner>/store-management-api:latest` and `:sha-<commit>` to GitHub Container Registry using the built-in `GITHUB_TOKEN`, so no extra secrets are needed.

## AWS deployment considerations

**Target architecture**
```
Internet ─► [Security group: 80/443 or 8080] ─► EC2 (Docker: store-management-api)
                                                   │ 5432 (SG-to-SG rule only)
                                                   ▼
                                     Amazon RDS PostgreSQL (private subnet, not public)
```

**1. RDS PostgreSQL**
- Engine PostgreSQL 16, in a private subnet, with **Public access = No**.
- RDS security group: allow inbound **5432 only from the EC2 instance's security group**.
- Create the database (for example `storedb`). Flyway creates the tables on first start of the app.
- Enable automated backups and storage encryption.

**2. EC2**
- Amazon Linux 2023. Install Docker: `sudo dnf install -y docker && sudo systemctl enable --now docker && sudo usermod -aG docker ec2-user`.
- EC2 security group: SSH 22 from **your IP only**, and app port 8080 (or 80/443 behind a load balancer or reverse proxy).
- Get the image: either `docker pull ghcr.io/<owner>/store-management-api:latest` (log in to GHCR first if the package is private), or clone the repo and run `docker build`.

**3. Run the container**
```bash
docker run -d --name store-api -p 8080:8080 --restart unless-stopped \
  -e DB_HOST=<rds-endpoint>.rds.amazonaws.com -e DB_PORT=5432 -e DB_NAME=storedb \
  -e DB_USERNAME=<user> -e DB_PASSWORD=<password> \
  ghcr.io/<owner>/store-management-api:latest
curl http://localhost:8080/actuator/health     # expect "db": {"status": "UP"}
```
To keep the password out of shell history, use `--env-file /etc/store-api.env` (owned by root, `chmod 600`).
A better option is to store it in **AWS Secrets Manager** or **SSM Parameter Store** and read it with an EC2 **IAM role**.
Never put AWS access keys on the instance or in the repository.

**4. Hardening and operations**
- Put an Application Load Balancer with HTTPS (ACM certificate) in front, and point its health check at `/actuator/health`.
- Send container logs to CloudWatch (`--log-driver=awslogs`) and set CloudWatch alarms for CPU and RDS connections.
- The API has no authentication (assignment scope). For a real deployment, add Spring Security (JWT or OAuth2) or put it behind API Gateway or an authenticated ALB.
- To force TLS to RDS, set `SPRING_DATASOURCE_URL=jdbc:postgresql://<rds-endpoint>:5432/storedb?sslmode=require`. This overrides the URL built from `DB_HOST`, `DB_PORT` and `DB_NAME`.
- Schema changes go in new Flyway migration files (`V2__...sql`). Never edit an applied migration.
