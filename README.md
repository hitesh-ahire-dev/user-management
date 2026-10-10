# User Management REST API

A production-style Spring Boot REST API for CRUD operations on users.

---

## Table of Contents
1. [Architecture](#architecture)
2. [Prerequisites](#prerequisites)
3. [Project Structure](#project-structure)
4. [Environment Variables](#environment-variables)
5. [Running Locally (IDE / Maven)](#running-locally)
6. [Running Locally with Docker Compose (MySQL only)](#running-with-docker-compose)
7. [API Endpoints](#api-endpoints)
8. [Sample Requests & Responses](#sample-requests--responses)
9. [Running Tests](#running-tests)
10. [Common Troubleshooting](#common-troubleshooting)
11. [Cloud Deployment](#cloud-deployment)
12. [Security Considerations](#security-considerations)
13. [Production Database Migrations (Flyway)](#production-database-migrations)

---

## Architecture

```
HTTP Client
    │  (JSON: UserRequest)
    ▼
UserController          — HTTP only: @Valid, ResponseEntity, status codes
    ▼
UserService (interface) — contract, swappable implementation
    ▼
UserServiceImpl         — business rules, @Transactional, logging
    ▼  (UserMapper: entity ⟷ DTO)
UserRepository          — Spring Data JPA
    ▼
MySQL 8 (user_management database)
```

**Key design decisions:**
- DTOs (`UserRequest`, `UserResponse`, `PagedResponse`) keep the JPA entity off the wire.
- `GlobalExceptionHandler` catches every error so no stack trace ever reaches the client.
- `JpaAuditingConfig` is separate from the main class so `@WebMvcTest` slices don't try to wire JPA auditing beans.
- Credentials are **always** read from environment variables — nothing is hardcoded.

---

## Prerequisites

| Tool | Minimum version |
|------|----------------|
| JDK | 17 |
| Maven | 3.9 |
| MySQL | 8.0 (local run only) |
| Docker + Compose | any recent (Docker Compose run only) |

---

## Project Structure

```
user-management/
├── src/main/java/com/example/usermanagement/
│   ├── UserManagementApplication.java
│   ├── config/
│   │   └── JpaAuditingConfig.java
│   ├── controller/
│   │   └── UserController.java
│   ├── dto/
│   │   ├── PagedResponse.java
│   │   ├── UserRequest.java
│   │   └── UserResponse.java
│   ├── entity/
│   │   └── User.java
│   ├── exception/
│   │   ├── DuplicateResourceException.java
│   │   ├── ErrorResponse.java
│   │   ├── GlobalExceptionHandler.java
│   │   └── ResourceNotFoundException.java
│   ├── mapper/
│   │   └── UserMapper.java
│   ├── repository/
│   │   └── UserRepository.java
│   └── service/
│       ├── UserService.java
│       └── impl/
│           └── UserServiceImpl.java
├── src/main/resources/
│   ├── application.yml          ← common config
│   ├── application-local.yml    ← local MySQL (env-var driven)
│   ├── application-docker.yml   ← docker-compose profile
│   └── logback-spring.xml
├── src/test/
│   ├── java/.../
│   │   ├── controller/UserControllerTest.java   (MockMvc, 10 tests)
│   │   ├── repository/UserRepositoryTest.java   (H2, 4 tests)
│   │   ├── repository/UserRepositoryContainerTest.java  (Testcontainers, Docker-gated)
│   │   └── service/UserServiceImplTest.java     (Mockito, 10 tests)
│   └── resources/application-test.yml
├── Dockerfile
├── docker-compose.yml
├── .env.example
├── .dockerignore
├── .gitignore
└── README.md
```

---

## Environment Variables

| Variable | Default (local profile) | Required in Docker |
|----------|------------------------|--------------------|
| `SPRING_PROFILES_ACTIVE` | `local` | `docker` |
| `DB_URL` | **required** | ✅ |
| `DB_USERNAME` | **required** | ✅ |
| `DB_PASSWORD` | **required** | ✅ |
| `MYSQL_ROOT_PASSWORD` | — | ✅ (MySQL container init) |
| `MYSQL_USER` | `appuser` | ✅ |
| `MYSQL_PASSWORD` | — | ✅ |

---

## Running Locally

### 1. MySQL setup

```sql
CREATE DATABASE IF NOT EXISTS user_management;
-- If you use a dedicated user:
CREATE USER 'appuser'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON user_management.* TO 'appuser'@'localhost';
FLUSH PRIVILEGES;
```

### 2. Set environment variables

All three variables are now **required** — there are no hardcoded defaults.

#### Option A — IntelliJ IDEA Run Configuration

1. Click the **dropdown** next to the Run button (top-right) → **Edit Configurations...**
2. Select your Spring Boot run config (e.g. `UserManagementApplication`).
3. Find the **Environment variables** field → click the **📋 icon** on the right.
4. Add the following variables:

   | Name | Example value |
   |------|---------------|
   | `DB_URL` | `jdbc:mysql://localhost:3306/user_management?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC` |
   | `DB_USERNAME` | `root` |
   | `DB_PASSWORD` | `your_mysql_password` |

5. Click **OK** → click **Run ▶**.

#### Option B — Terminal (Maven CLI)

```bash
export DB_URL=jdbc:mysql://localhost:3306/user_management?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
export DB_USERNAME=root
export DB_PASSWORD=your_mysql_password
```

### 3a. Run from IntelliJ IDEA

After configuring env vars via **Option A** above, open `UserManagementApplication.java` and click **Run ▶**. The active profile defaults to `local`.

### 3b. Run with Maven

```bash
mvn spring-boot:run
```

The app starts on **http://localhost:8080**.

---

## Running Locally with Docker Compose (MySQL only)

`docker-compose.yml` starts a MySQL 8 container for local development.
The Spring Boot app runs locally with `mvn spring-boot:run` (no Docker image needed).

```bash
# 1. Copy and fill in credentials
cp .env.example .env
# Edit .env: set MYSQL_ROOT_PASSWORD and MYSQL_PASSWORD

# 2. Start only MySQL in the background
docker compose up -d

# 3. Run the Spring Boot app locally (in a separate terminal)
export DB_USERNAME=appuser DB_PASSWORD=your_password
mvn spring-boot:run

# 4. Stop MySQL when done
docker compose down

# 5. Wipe the data volume too (fresh start)
docker compose down -v
```

---

## API Endpoints

| Method | Path | Description | Success |
|--------|------|-------------|---------|
| `POST` | `/api/users` | Create a user | 201 Created |
| `GET` | `/api/users` | List users (paginated) | 200 OK |
| `GET` | `/api/users/{id}` | Get user by ID | 200 OK |
| `PUT` | `/api/users/{id}` | Update a user | 200 OK |
| `DELETE` | `/api/users/{id}` | Delete a user | 204 No Content |
| `GET` | `/actuator/health` | Health check | 200 OK |

### Pagination parameters (GET /api/users)

| Param | Default | Example |
|-------|---------|---------|
| `page` | 0 | `?page=1` |
| `size` | 10 | `?size=5` |
| `sort` | `id,asc` | `?sort=email,desc` |

---

## Sample Requests & Responses

### POST /api/users — Create

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Jane",
    "lastName": "Doe",
    "email": "jane.doe@example.com",
    "phone": "1234567890"
  }'
```

**201 Created**
```json
{
  "id": 1,
  "firstName": "Jane",
  "lastName": "Doe",
  "email": "jane.doe@example.com",
  "phone": "1234567890",
  "createdAt": "2026-10-10T12:00:00",
  "updatedAt": "2026-10-10T12:00:00"
}
```

**400 Validation failure**
```json
{
  "timestamp": "2026-10-10T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed for one or more fields",
  "path": "/api/users",
  "fieldErrors": {
    "email": "Email must be a valid email address",
    "firstName": "First name is required"
  }
}
```

**409 Duplicate email**
```json
{
  "timestamp": "2026-10-10T12:00:00",
  "status": 409,
  "error": "Conflict",
  "message": "A user with email 'jane.doe@example.com' already exists",
  "path": "/api/users"
}
```

---

### GET /api/users — List (paginated)

```bash
curl "http://localhost:8080/api/users?page=0&size=5&sort=email,asc"
```

**200 OK**
```json
{
  "content": [
    {
      "id": 1,
      "firstName": "Jane",
      "lastName": "Doe",
      "email": "jane.doe@example.com",
      "phone": "1234567890",
      "createdAt": "2026-10-10T12:00:00",
      "updatedAt": "2026-10-10T12:00:00"
    }
  ],
  "page": 0,
  "size": 5,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

---

### GET /api/users/{id} — Get by ID

```bash
curl http://localhost:8080/api/users/1
```

**404 Not found**
```json
{
  "timestamp": "2026-10-10T12:00:00",
  "status": 404,
  "error": "Not Found",
  "message": "User not found with id 99",
  "path": "/api/users/99"
}
```

---

### PUT /api/users/{id} — Update

```bash
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "Janet",
    "lastName": "Doe",
    "email": "janet.doe@example.com",
    "phone": "0987654321"
  }'
```

---

### DELETE /api/users/{id} — Delete

```bash
curl -X DELETE http://localhost:8080/api/users/1
# → 204 No Content (empty body)
```

---

## Running Tests

```bash
# All tests (Testcontainers test auto-skipped if Docker absent)
mvn test

# Only unit tests (fast, no DB)
mvn test -pl . -Dtest="UserServiceImplTest"

# Only controller tests
mvn test -Dtest="UserControllerTest"

# Only repository tests (H2)
mvn test -Dtest="UserRepositoryTest"

# Full build + package
mvn clean package
```

---

## Common Troubleshooting

| Symptom | Likely cause | Fix |
|---------|-------------|-----|
| `Access denied for user 'root'` | Wrong DB credentials | Check `DB_PASSWORD` env var |
| `Communications link failure` | MySQL not running | Start MySQL or use Docker Compose |
| `Table 'user_management.users' doesn't exist` | Schema not created | Set `spring.jpa.hibernate.ddl-auto=update` (local only) |
| Port 8080 already in use | Another process | `lsof -i :8080` then kill or change `server.port` |
| `MYSQL_PASSWORD is not set` (compose) | `.env` missing | Copy `.env.example` → `.env` and fill values |

---

## Cloud Deployment — Render.com

Render builds and runs the Spring Boot jar directly from your Git repository.
**No Docker image required.** Deployment is driven by `render.yaml` at the project root.

---

### Step 1 — Get a free external MySQL database

Render provides managed **PostgreSQL** only. For MySQL, use one of these free providers:

| Provider | Free tier | Notes |
|----------|-----------|-------|
| **PlanetScale** | ✅ Generous free tier | Serverless MySQL, great for learning |
| **Railway** | ✅ Small free allowance | MySQL with a dashboard UI |
| **Aiven** | ✅ Free trial | Managed MySQL |

**PlanetScale (recommended for beginners):**

1. Sign up at [planetscale.com](https://planetscale.com).
2. Create a database named `user_management`.
3. Go to **Connect → Java / JDBC** and copy the connection string.  
   It looks like:
   ```
   jdbc:mysql://aws.connect.psdb.cloud/user_management?sslMode=VERIFY_IDENTITY&...
   ```
4. Create a **password** (PlanetScale calls them "passwords", not users).  
   Note: the `username` and `password` are shown only once — save them.

---

### Step 2 — Push the project to GitHub

```bash
cd /path/to/user-management
git init
git add .
git commit -m "Initial commit"
# create a repo on github.com, then:
git remote add origin https://github.com/YOUR_USERNAME/user-management.git
git push -u origin main
```

> Make sure `.gitignore` excludes `.env` (it does — already configured).

---

### Step 3 — Create a Web Service on Render

1. Log in at [dashboard.render.com](https://dashboard.render.com).
2. Click **New → Blueprint** (Render reads `render.yaml` automatically).
3. Connect your GitHub repository.
4. Render detects `render.yaml` and shows a preview of the service.
5. Click **Apply**.

**Or** create it manually (New → Web Service → connect repo):

| Setting | Value |
|---------|-------|
| Runtime | **Java** |
| Build Command | `mvn clean package -DskipTests` |
| Start Command | `java -Xmx256m -jar target/user-management-1.0.0.jar` |
| Instance Type | Free |

---

### Step 4 — Set Secret Environment Variables

In the Render dashboard → your service → **Environment** tab, add:

| Key | Value |
|-----|-------|
| `SPRING_PROFILES_ACTIVE` | `render` |
| `DB_URL` | JDBC URL from PlanetScale / your MySQL provider |
| `DB_USERNAME` | MySQL username |
| `DB_PASSWORD` | MySQL password |

> **Never** put real credentials in `render.yaml` or `.env.example`. Use the Render dashboard's secret environment variables.

---

### Step 5 — Deploy and verify

Render automatically deploys on every push to `main`.  
Watch the build logs in the Render dashboard. A successful deploy shows:

```
Started UserManagementApplication in X.XXX seconds
```

Verify with:

```bash
# Replace YOUR_APP with your Render service URL (e.g. user-management.onrender.com)
curl https://YOUR_APP.onrender.com/actuator/health
# → {"status":"UP","components":{"db":{"status":"UP"}}}

curl -X POST https://YOUR_APP.onrender.com/api/users \
  -H "Content-Type: application/json" \
  -d '{"firstName":"Jane","lastName":"Doe","email":"jane@example.com"}'
```

---

### Free tier notes

- Render's **free web service** spins down after **15 minutes of inactivity**. The first request after sleep takes ~30 seconds to wake up. This is normal on the free tier.
- Upgrade to a paid instance (`starter` plan) to avoid cold starts.

---

## Security Considerations

- **Never commit** `.env` or real credentials to version control.
- Always use Render's **Secret environment variables** — not plain env vars in `render.yaml`.
- Set `spring.jpa.hibernate.ddl-auto=validate` in production; use **Flyway** for schema changes (see below).
- Enable **SSL** on your MySQL connection string (`sslMode=VERIFY_IDENTITY` for PlanetScale).
- Restrict the Actuator: only `/health` and `/info` are exposed; `show-details: never` is set on the render profile.
- Render provides **HTTPS** automatically via its edge — your app receives plain HTTP on port 8080 internally.

---

## Production Database Migrations (Flyway)

`spring.jpa.hibernate.ddl-auto=update` is fine for local learning but **not safe** for production (Hibernate may lose data on column changes, and you lose an audit trail of schema history).

**Switching to Flyway:**

1. Add the dependency:
   ```xml
   <dependency>
       <groupId>org.flywaydb</groupId>
       <artifactId>flyway-mysql</artifactId>
   </dependency>
   ```

2. Set `spring.jpa.hibernate.ddl-auto=validate` in production profile.

3. Create SQL migration files:
   ```
   src/main/resources/db/migration/
   └── V1__create_users_table.sql
   └── V2__add_phone_column.sql   ← each change is a new versioned file
   ```

4. Flyway runs migrations automatically at startup before Hibernate validates the schema.

This gives you a versioned, repeatable, and safe schema history — the industry standard approach.
