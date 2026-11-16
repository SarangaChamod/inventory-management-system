# Changelog

All notable changes to this project are recorded here.
Format based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/). Newest first.
Record only verified changes. Detailed progress and session notes live in `.plan`.

## [Unreleased]
### Added
- Project initialised: `.plan`, `PROJECT_CONTEXT.md`, `CHANGELOG.md`, `README.md`, `.gitignore` (2026-11-16).
- Store Management REST API v1.0.0 (2026-11-16):
  - Product CRUD, search by name, filter by category, stock check, case-insensitive unique SKU.
  - Category CRUD with unique names.
  - Inventory increase/decrease with row locking and no negative stock (409 on insufficient stock).
  - Consistent JSON error responses via `@RestControllerAdvice`; Jakarta validation.
  - Flyway migration `V1__create_store_schema.sql`; Hibernate `ddl-auto: validate`.
  - Actuator `/actuator/health` (with DB status, no details) and `/actuator/info`.
  - Springdoc OpenAPI / Swagger UI.
  - Multi-stage non-root Dockerfile with HEALTHCHECK, `.dockerignore`, `docker-compose.yml`, `.env.example`.
  - GitHub Actions `devsecops.yml`: Gitleaks, Maven tests, Trivy fs/image scans, Docker smoke test, GHCR publish.
  - 63 tests (service, controller, repository, integration), all passing with `mvn verify`.
  - Maven wrapper (`mvnw`, `mvnw.cmd`, Maven 3.9.9).

### Changed

### Fixed
