# Project Context: Store Management REST API (EEI6369)

> Stable background about this project for every AI model and human contributor.
> `.plan` tracks the changing state (tasks, progress, decisions). This file holds what rarely changes.
> Write only what the user stated or what you verified. Use `TBD` for anything else. Never put secrets here.

- **Created:** 2026-11-16
- **Owner / author:** Project owner (student)
- **Context:** Course module EEI6369, AWS Cloud Computing practical assessment (Degree year 4, semester 2)

## Purpose
A production-ready Store Management REST API (products, categories, inventory) deployed on AWS EC2
using Docker, with Amazon RDS PostgreSQL as the database. Priorities: clean architecture, security,
maintainability, Docker compatibility and easy AWS deployment.

## Audience / users
- Assessors of the EEI6369 practical assessment
- API clients (no frontend; tested via Swagger UI / curl / Postman)

## Scope
- **In scope:** Product CRUD + search by name + filter by category + stock check + unique SKU;
  Category CRUD (unique name); inventory increase/decrease without negative stock; centralized
  error handling; Jakarta validation; Actuator health (with DB) and info; Springdoc OpenAPI;
  multi-stage non-root Dockerfile; docker-compose (app + PostgreSQL + volume); unit, controller,
  repository and integration tests; GitHub Actions DevSecOps workflow; README.
- **Out of scope:** Frontend; authentication/authorization (not requested); hardcoded secrets or AWS credentials.

## Constraints
- **Deadline / milestones:** TBD
- **Required technologies / platforms:** Java 17, Spring Boot 3.x, Maven, Spring Web, Spring Data JPA,
  PostgreSQL, Actuator, Jakarta Validation, Lombok (optional), Docker, GitHub Actions, Springdoc OpenAPI,
  JUnit 5 + Mockito; AWS EC2 + Amazon RDS PostgreSQL.
- **Assessment or acceptance criteria:** Builds with Maven; working code (no pseudocode); DB config via
  env vars DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD; Hibernate schema validation in production;
  `/actuator/health` shows `components.db.status = UP` without sensitive info; Swagger UI at
  `/swagger-ui/index.html`; prescribed package structure `com.store.management.{controller,service,repository,entity,dto,exception,config}`.
- **Other constraints:** Individual academic assignment; keep it simple.

## Glossary
| Term | Meaning |
|---|---|
| SKU | Stock keeping unit, unique product code |
| RDS | Amazon Relational Database Service |
| EC2 | Amazon Elastic Compute Cloud (virtual server running Docker) |

## Key references
- Original requirements prompt supplied by the user on 2026-11-16 (summarised above and in `.plan` section 2)
