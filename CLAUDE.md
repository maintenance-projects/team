# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Workspace Overview

This is a multi-project workspace containing two Spring Boot applications that work together:

- **GitNX** (`GitNX/`) — Self-hosted Git repository management platform (GitHub-like). Runs on **port 8080**.
- **SpaceNX** (`SpaceNX/`) — Project management system with Scrum/Kanban support. Runs on **port 8081**. Integrates with GitNX for code-related features.

Both projects have their own `CLAUDE.md` with project-specific details. Refer to `GitNX/CLAUDE.md` and `SpaceNX/.claude/worktrees/gracious-wu/CLAUDE.md` for in-depth architecture and workflows.

## Shared Infrastructure

Both applications share a single **PostgreSQL** database:
- Database: `gitnx` on `localhost:5432` (user: `root`)
- Started via Docker Compose (either project's `docker-compose.yml` works — GitNX uses postgres:16, SpaceNX uses postgres:17)
- Schema managed by Hibernate `ddl-auto: update` (no migration files)

```bash
# Start the shared database
docker-compose -f GitNX/docker-compose.yml up -d
# or
docker-compose -f SpaceNX/docker-compose.yml up -d
```

## Quick Start

```bash
# 1. Start database (from either project)
cd GitNX && ./run.sh db-check && cd ..

# 2. Start GitNX (port 8080)
cd GitNX && ./run.sh start && cd ..

# 3. Start SpaceNX (port 8081)
cd SpaceNX && ./run.sh start && cd ..
```

## Common Build Commands

Both projects use **Gradle 9.3.1** and **Java 21**:

```bash
# Build (in either project directory)
./gradlew build -x test        # Build without tests
./gradlew test                  # Run tests
./gradlew test --tests <Name>  # Run a specific test class
./gradlew bootRun              # Run with hot-reload
./gradlew clean build -x test  # Clean rebuild

# run.sh commands (same in both projects)
./run.sh start    # Build + start (manages DB + PID)
./run.sh stop     # Graceful shutdown
./run.sh restart
./run.sh status   # Check app, DB, HTTP connectivity
./run.sh logs     # Tail application log
./run.sh build    # Build only (no tests)
./run.sh clean    # Clean rebuild
./run.sh db-check # Verify/create PostgreSQL container
```

## Shared Tech Stack

- Java 21, Gradle 9.3.1
- Spring Boot (GitNX: 3.4.5, SpaceNX: 3.5.11)
- Spring Security 6 with form-based login + BCrypt
- Spring Data JPA + Hibernate (PostgreSQL)
- Thymeleaf with Layout Dialect for server-side rendering
- Lombok for boilerplate reduction

## Architecture Conventions

Both projects follow the same domain-driven layered architecture:

```
src/main/java/com/{project}/{domain}/
  ├── entity/        # JPA entities (extend BaseTimeEntity)
  ├── repository/    # Spring Data JPA repositories
  ├── service/       # Business logic (@Transactional)
  ├── controller/    # Web controllers (return Thymeleaf views)
  ├── dto/           # Request/response objects
  └── enums/         # Domain enums
```

Templates go in `src/main/resources/templates/{domain}/` with shared fragments in `fragments/` and a master layout in `layout/`.

## Integration Points

SpaceNX calls GitNX's HTTP API (configured via `spacenx.gitnx.base-url` in SpaceNX's `application.yml`). When developing features that span both services, both must be running.

## Key Differences Between Projects

| | GitNX | SpaceNX |
|---|---|---|
| Package | `com.gitnx` | `com.spacenx` |
| Port | 8080 | 8081 |
| Spring Boot | 3.4.5 | 3.5.11 |
| Unique dependency | JGit 7.1.0 | Jackson Databind |
| PID file | `.gitnx.pid` | `.spacenx.pid` |
| Log file | `gitnx.log` | `spacenx.log` |
| File storage | `~/gitnx-repos` (bare git repos) | `~/spacenx-uploads` (attachments) |
