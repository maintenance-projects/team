# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SpaceNX is a Spring Boot project management system supporting Scrum and Kanban methodologies. It provides issue tracking, sprint management, Kanban boards, calendar/timeline views, and team collaboration within multi-member spaces. Built as a server-rendered MVC app with Thymeleaf templates.

## Build & Run Commands

```bash
# Build (skips tests)
./gradlew build -x test

# Build with tests
./gradlew build

# Run tests
./gradlew test

# Run application directly
./gradlew bootRun

# Clean
./gradlew clean
```

**`run.sh` helper script** (preferred for local dev):
```bash
./run.sh start      # Build + check DB + start on port 8081
./run.sh stop       # Stop application
./run.sh restart    # Restart
./run.sh status     # Check if running
./run.sh logs       # Tail application log
./run.sh build      # Build only (no tests)
./run.sh db-check   # Verify PostgreSQL container
```

## Tech Stack

- **Java 21**, Spring Boot 3.5.11, Gradle 9.3.1
- **PostgreSQL 17** (Docker container `postgres-container`, database `gitnx`, port 5432)
- **Spring Security 6** with form-based login and BCrypt
- **Thymeleaf** with Layout Dialect for server-side rendering
- **Spring Data JPA** with Hibernate (ddl-auto: update)
- **Lombok** for boilerplate reduction
- Application runs on **port 8081**

## Architecture

Standard layered Spring MVC: **Controller → Service → Repository → Entity**.

### Domain Modules (under `src/main/java/com/spacenx/`)

| Module | Purpose |
|--------|---------|
| `common/` | Shared config (SecurityConfig, WebConfig), BaseTimeEntity, GlobalExceptionHandler, MentionUtils |
| `user/` | Authentication, registration, user profiles (AuthController) |
| `space/` | Workspaces with SpaceType (SCRUM/KANBAN) and MemberRole (ADMIN/MEMBER/VIEWER) |
| `issue/` | Core issue tracking - includes BoardController (Kanban), CalendarController, TimelineController |
| `sprint/` | Sprint management, BacklogController |
| `form/` | Dynamic form templates and submissions |
| `shortcut/` | Quick action shortcuts |

Each module follows the pattern: `entity/`, `service/`, `controller/`, `repository/`, `dto/`, `enums/`.

### Key Patterns

- **BaseTimeEntity**: All entities extend this for automatic `createdAt`/`updatedAt` auditing
- **Space-scoped routes**: Most resources are accessed under `/spaces/{spaceKey}/...`
- **CSRF disabled for `/api/**`**: API endpoints skip CSRF for AJAX calls
- **Public routes**: `/login`, `/register`, `/css/**`, `/js/**`, `/img/**`, `/error/**`, `/api/public/**`

### Frontend

- **Templates**: `src/main/resources/templates/` with Thymeleaf Layout Dialect (base layout in `layout/`)
- **Reusable fragments**: `fragments/` directory (header, sidebar)
- **Static assets**: Single `css/style.css` and `js/app.js` (vanilla JS handling drag-and-drop, calendar, timeline, charts)
- **File uploads**: Stored at `~/spacenx-uploads`, max 10MB per file

## Database

PostgreSQL shared with GitNX project. Start via Docker:
```bash
docker-compose up -d    # or: ./run.sh db-check
```
Schema is auto-managed by Hibernate (`ddl-auto: update`). No migration files.

## Integration

SpaceNX integrates with a companion GitNX service at `http://localhost:8080` (configured in `application.yml` under `spacenx.gitnx`).
