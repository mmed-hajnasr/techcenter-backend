# Techcenter Backend API

Spring Boot backend with PostgreSQL, Hibernate/JPA, JWT auth, and Argon2 password hashing.

## Base URL

- Local default: `http://localhost:8080`

## Auth Model (current)

- JWT-first authentication.
- Public endpoints: `POST /signup`, `POST /login`, `GET /health`.
- Protected endpoint: `GET /user/me`.
- Send JWT in header: `Authorization: Bearer <token>`.

## Quick Start

### 1) Start PostgreSQL (optional via Docker Compose)

```bash
docker compose up -d postgres
```

### 2) Run the app

```bash
./mvnw spring-boot:run
```

### 3) Run regression tests

```bash
./mvnw test
```

Tests run with the `test` profile and use an isolated in-memory H2 database (`techcenter_regression_test`) configured in `src/test/resources/application-test.properties`.

---

## API Endpoints

### Health Check

`GET /health`

```bash
curl -s http://localhost:8080/health
```

Expected response:

```json
{
  "status": "UP"
}
```

---

### Sign Up

`POST /signup`

Creates a user, hashes password using Argon2id, and returns a JWT access token.

```bash
curl -s -X POST http://localhost:8080/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "username": "alice",
    "password": "StrongPass123!"
  }'
```

Example response:

```json
{
  "userId": "7f3f8b34-3dc8-4cc8-b4d8-7cb55fd4d80f",
  "email": "alice@example.com",
  "username": "alice",
  "createdAt": "2026-04-21T20:10:11.222Z",
  "accessToken": "<jwt-token>",
  "expiresInSeconds": 900
}
```

---

### Login

`POST /login`

`identifier` accepts either email or username.

```bash
curl -s -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "alice@example.com",
    "password": "StrongPass123!"
  }'
```

Or using username:

```bash
curl -s -X POST http://localhost:8080/login \
  -H "Content-Type: application/json" \
  -d '{
    "identifier": "alice",
    "password": "StrongPass123!"
  }'
```

Example response:

```json
{
  "userId": "7f3f8b34-3dc8-4cc8-b4d8-7cb55fd4d80f",
  "email": "alice@example.com",
  "username": "alice",
  "createdAt": "2026-04-21T20:10:11.222Z",
  "accessToken": "<jwt-token>",
  "expiresInSeconds": 900
}
```

---

### Get Current User

`GET /user/me` (requires JWT)

```bash
TOKEN="<paste-access-token-here>"

curl -s http://localhost:8080/user/me \
  -H "Authorization: Bearer $TOKEN"
```

Example response:

```json
{
  "userId": "7f3f8b34-3dc8-4cc8-b4d8-7cb55fd4d80f",
  "email": "alice@example.com",
  "username": "alice",
  "createdAt": "2026-04-21T20:10:11.222Z",
  "accessToken": null,
  "expiresInSeconds": 0
}
```

---

## Full cURL Flow (copy/paste)

```bash
BASE_URL="http://localhost:8080"

# 1) Signup
SIGNUP_RESPONSE=$(curl -s -X POST "$BASE_URL/signup" \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","username":"alice","password":"StrongPass123!"}')

echo "$SIGNUP_RESPONSE"

# 2) Login
LOGIN_RESPONSE=$(curl -s -X POST "$BASE_URL/login" \
  -H "Content-Type: application/json" \
  -d '{"identifier":"alice@example.com","password":"StrongPass123!"}')

echo "$LOGIN_RESPONSE"

# 3) Extract token (requires jq)
TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.accessToken')

# 4) Call protected route
curl -s "$BASE_URL/user/me" \
  -H "Authorization: Bearer $TOKEN"
```

---

## Error Responses

Common format:

```json
{
  "error": "UNAUTHORIZED",
  "message": "Authentication is required"
}
```

Other auth errors may include:

- `INCORRECT_LOGIN`
- `INVALID_EMAIL`
- `INVALID_USERNAME`
- `INVALID_PASSWORD`
- `NOT_LOGGED_IN`

---

## Environment Variables

Key settings (from `src/main/resources/application.properties`):

- `DATABASE__HOST` (default `localhost`)
- `DATABASE__PORT` (default `5432`)
- `DATABASE__NAME` (default `techcenter`)
- `DATABASE__USERNAME` (default `postgres`)
- `DATABASE__PASSWORD` (default `postgres`)
- `APP_AUTH_JWT_SECRET` (must be at least 32 bytes in production)
- `APP_AUTH_JWT_EXPIRATION_SECONDS` (default `900`)
- `APP_AUTH_JWT_ISSUER` (default `techcenter-backend`)

Example:

```bash
export APP_AUTH_JWT_SECRET="replace-with-a-long-random-secret-at-least-32-bytes"
export APP_AUTH_JWT_EXPIRATION_SECONDS="900"
export APP_AUTH_JWT_ISSUER="techcenter-backend"
```
