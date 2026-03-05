# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

GitNX is a self-hosted Git repository management platform built with Spring Boot 3.4.5, JGit, and Thymeleaf. It provides GitHub-like functionality including repository hosting, issue tracking, merge requests, and code browsing.

**Tech Stack:**
- Java 21
- Spring Boot 3.4.5 (Web, Data JPA, Security, Thymeleaf)
- JGit 7.1.0 for Git operations
- PostgreSQL 16
- Gradle 9.3.1

## Development Commands

### Running the Application

**Linux/macOS:**
```bash
# Start app with database (recommended)
./run.sh start

# Stop the application
./run.sh stop

# Restart
./run.sh restart

# Check status
./run.sh status

# View logs
./run.sh logs
```

**Windows:**
```cmd
# Start app with database (recommended)
run.bat start

# Stop the application
run.bat stop

# Restart
run.bat restart

# Check status
run.bat status

# View logs
run.bat logs
```

The run scripts (run.sh/run.bat) handle:
- PostgreSQL container management (starts/creates if needed)
- Port availability checking (8080)
- Building with Gradle
- Process management via PID file (.gitnx.pid)
- Cross-platform support (Linux/macOS with run.sh, Windows with run.bat)

### Database Management

```bash
# Start PostgreSQL via Docker Compose
docker-compose up -d

# Or use the run script's DB check
./run.sh db-check
```

**Database Connection:**
- Host: localhost:5432
- Database: gitnx
- Username: root
- Password: ultari12#$

### Build Commands

```bash
# Build without tests
./gradlew build -x test

# Clean and rebuild
./gradlew clean build -x test

# Or use the run script
./run.sh build
./run.sh clean
```

### Testing

```bash
# Run all tests
./gradlew test

# Run specific test
./gradlew test --tests GitNxApplicationTests

# Run tests with JUnit Platform
./gradlew test --rerun-tasks
```

### Development Mode

Spring Boot DevTools is enabled for hot-reload during development. Run via IDE or:

```bash
./gradlew bootRun
```

## Architecture

### Domain Structure

GitNX follows a domain-driven design with 5 main domains:

1. **user** - Authentication and user management
   - Spring Security integration with BCrypt password encoding
   - `CustomUserDetailsService` for authentication
   - Form-based login for web UI

2. **repository** - Git repository management
   - Creates and manages bare Git repositories on disk
   - Supports repository import from external Git URLs
   - Repository member management with role-based access (OWNER, MAINTAINER, DEVELOPER, GUEST)
   - Default storage: `~/gitnx-repos/{username}/{repo}.git`

3. **issue** - Issue tracking system
   - Issues with labels, milestones, and comments
   - Issue state management (OPEN, CLOSED)

4. **mergerequest** - Pull request functionality
   - Merge requests with reviewer assignment
   - Review comments and state tracking
   - Multiple merge strategies supported

5. **common** - Shared utilities and configuration
   - Base entities (`BaseTimeEntity` with JPA auditing)
   - Exception handling (`GlobalExceptionHandler`)
   - Security configuration
   - JGit configuration

### JGit Integration

**Key Service Classes:**
- `GitRepositoryService` - Repository CRUD operations, import/export
- `CommitService` - Read commit history, diffs, file content
- `BranchService` - Branch operations

**Repository Storage:**
- All repositories are stored as **bare repositories** (`.git` suffix)
- Location configured via `gitnx.repos.base-path` (default: `~/gitnx-repos`)
- Format: `{base-path}/{owner-username}/{repo-name}.git`

**Important JGit Patterns:**
```java
// Opening a bare repository
Repository repo = new FileRepositoryBuilder()
    .setGitDir(new File(diskPath))
    .readEnvironment()
    .build();

// Always close Repository and Git objects
try (Git git = new Git(repo)) {
    // operations
}
```

### Security Configuration

GitNX uses **two separate Spring Security filter chains** (SecurityConfig.java):

1. **Git HTTP Protocol Chain** (`@Order(1)`, `/repo/**`)
   - Stateless HTTP Basic authentication
   - `git-receive-pack` (push) requires authentication
   - `git-upload-pack` (clone/fetch) is public
   - CSRF disabled for Git protocol compatibility

2. **Web Application Chain** (`@Order(2)`, all other requests)
   - Session-based form authentication
   - Login page: `/login`
   - Default success redirect: `/dashboard`
   - Public paths: `/login`, `/register`, `/css/**`, `/js/**`, `/img/**`, `/error/**`

### Repository Member Permissions

The `RepositoryMember` entity maps users to repositories with roles:
- **OWNER** - Full control (granted on creation)
- **MAINTAINER** - Manage settings and members
- **DEVELOPER** - Push access
- **GUEST** - Read-only access

Each repository has a unique constraint on `(repository_id, user_id)`.

### Template Structure

Thymeleaf templates are organized by domain:
- `templates/auth/` - Login, registration
- `templates/dashboard/` - User dashboard
- `templates/repository/` - Repository list, create, import
- `templates/code/` - File browser, commits, branches, diffs
- `templates/issue/` - Issue list, detail, creation, labels
- `templates/mergerequest/` - MR list, detail, creation
- `templates/fragments/` - Header, footer (reusable)
- `templates/layout/` - Default layout (Thymeleaf Layout Dialect)
- `templates/error/` - 400, 403, 404, 500

## Configuration

### Application Configuration (application.yml)

**Key Properties:**
- `gitnx.repos.base-path` - Where Git repositories are stored on disk
- `gitnx.clone.http-base-url` - Base URL for clone operations
- `spring.jpa.hibernate.ddl-auto: update` - Auto-creates/updates schema
- `spring.jpa.show-sql: true` - Log SQL statements (debug)
- `spring.jpa.open-in-view: false` - Disabled to prevent lazy-loading issues

### Logging

Application logs to:
- Console (controlled by `logging.level.com.gitnx: DEBUG`)
- `gitnx.log` file when using `./run.sh start`

JGit logging set to INFO level to reduce noise.

## Common Workflows

### Creating a Repository Feature

When adding repository-related features:
1. Check if JGit operation needs a `Repository` or `Git` instance
2. Use `GitRepositoryService.getRepoDiskPath(owner, name)` to get the bare repo directory
3. Always use try-with-resources for `Repository` and `Git` objects
4. Catch `GitAPIException` and wrap in `GitOperationException`
5. Update corresponding DTO and controller

### Adding a New Domain

Follow the existing pattern:
```
src/main/java/com/gitnx/{domain}/
  ├── entity/        # JPA entities
  ├── repository/    # JPA repositories (*JpaRepository)
  ├── service/       # Business logic
  ├── controller/    # Web controllers
  ├── dto/           # Data transfer objects
  └── enums/         # Domain enums
```

Add templates in `src/main/resources/templates/{domain}/`.

### Database Schema Changes

Since `ddl-auto: update` is enabled, JPA entities automatically update the schema. For production, consider using migration tools (Flyway/Liquibase).

## Important Notes

- **Bare Repositories**: All Git repositories are bare (no working directory), stored with `.git` suffix
- **PostgreSQL Required**: The application expects PostgreSQL running on port 5432
- **PID File**: `run.sh` uses `.gitnx.pid` to track the application process
- **Port 8080**: Default server port, configurable via `server.port`
- **Initial Branch**: New repositories use the branch specified in `CreateRepositoryRequest.defaultBranch` (typically "main")
- **Markdown Rendering**: Uses CommonMark with extensions (GFM tables, strikethrough, autolink, task lists)
