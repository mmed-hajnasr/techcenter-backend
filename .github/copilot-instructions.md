# Copilot Instructions — Techcenter Backend

## Architecture Overview

Spring Boot REST API with PostgreSQL (prod) / H2 (tests), JWT auth, Argon2 passwords, MinIO file storage, and Micrometer distributed tracing.

**Package layout** (`com.isi.techcenter_backend`):
- `controller/` — one controller per role+domain slice (e.g. `ActualiteModeratorController`, `DomainAdminController`)
- `service/` — business logic, one service per controller
- `entity/` — JPA entities (French naming: `ChercheurEntity`, `DomaineEntity`, `ActualiteEntity`, `PublicationEntity`)
- `model/` — request/response DTOs
- `repository/` — Spring Data JPA repositories
- `security/` — `JwtAuthenticationFilter`, `JwtService`, `SecurityConfig`
- `error/` — `AppErrorType` enum, `AuthException`, `GlobalExceptionHandler`
- `tracing/` — `EndpointTraceSupport` (wraps every controller action in a trace span)

## Developer Workflows

```bash
# Start external services
docker compose up -d postgres redis minio

# Run the app (requires JWT_SECRET env var or will fail)
./mvnw spring-boot:run

# Run all tests (uses in-memory H2, no infrastructure needed)
./mvnw test

# Compile only
./mvnw compile
```

Tests use `@ActiveProfiles("test")` and `src/test/resources/application-test.properties` (H2 in-memory, `MODE=PostgreSQL`). No Docker is needed for tests.

## Key Conventions

### Error Handling
All business errors use `AuthException(AppErrorType, message)`. The `GlobalExceptionHandler` maps `AppErrorType` variants to HTTP status codes. **Never throw raw exceptions for expected client failures** — always use `AuthException` with the appropriate `AppErrorType`.

```java
throw new AuthException(AppErrorType.USER_NOT_FOUND, "User not found: " + userId);
```

### Endpoint Instrumentation (mandatory)
Every controller method **must** wrap its return value in `EndpointTraceSupport.inSpan(...)`:

```java
return endpointTraceSupport.inSpan(
    "admin.users.list",      // span name: <domain>.<action>
    "/admin/users",          // endpoint tag
    "list-users",            // operation tag
    () -> ResponseEntity.ok(service.listUsers()),
    "optionalTagKey", "optionalTagValue"  // extra context tags
);
```

See `.github/prompts/endpoint-instrument.prompt.md` for full instrumentation rules.

### Test Structure
All regression tests extend `ApiRegressionTestSupport` (base class with `MockMvc` helpers: `getWithToken`, `postJson`, `putJson`, `deleteWithToken`, `signupAndLogin`). Test classes are in `src/test/java/.../auth/controller/`, one per API surface area.

The original monolithic `AuthApiRegressionTest` is `@Disabled` — new tests go in the split suite files.

### Security Model
- Roles: `ADMIN`, `MODERATOR`, `USER`
- All `GET` endpoints are public **except** `GET /admin/users`
- JWT sent as `Authorization: Bearer <token>`
- `Authentication.getName()` returns the user's UUID string

### Configuration / Environment Variables
Key env vars (with defaults): `DATABASE__HOST`, `JWT_SECRET` (required, no default), `JWT_EXPIRATION` (86400s), `MINIO_ENDPOINT`, `MINIO_ROOT_USER/PASSWORD`, `AI_SERVER_URL` (http://localhost:8001), `FRONTEND_URL` (http://localhost:3000).

## External Integrations
- **MinIO**: stores researcher photos and publication PDFs; configured via `MinioStorageProperties`
- **AI server**: external service at `AI_SERVER_URL`; see `AI_server_api.md` for the contract
- **Redis**: available in compose but not yet wired in application code
