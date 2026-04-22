# Techcenter Backend API

Spring Boot backend with PostgreSQL, Hibernate/JPA, JWT auth, and Argon2 password hashing.

## Base URL

- Local default: `http://localhost:8080`

## Auth Model (current)

- JWT-first authentication.
- User roles: `ADMIN`, `MODERATOR`, `USER`.
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

### Admin Domains (ADMIN only)

All routes below require a JWT for a user with role `ADMIN`.

`GET /admin/domains`

```bash
ADMIN_TOKEN="<paste-admin-access-token-here>"

curl -s http://localhost:8080/admin/domains \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Example response:

```json
[
  {
    "domainId": "6a7e8f84-9b8f-41e8-a1e4-bf7f9f3f6f3b",
    "name": "Artificial Intelligence",
    "description": "Research and development in AI systems"
  }
]
```

`POST /admin/domains`

```bash
curl -s -X POST http://localhost:8080/admin/domains \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name": "Cybersecurity",
    "description": "Security research and best practices"
  }'
```

Example response (`201 Created`):

```json
{
  "domainId": "7b03ce18-e1f6-4e52-9f4c-0e4b575d2f6d",
  "name": "Cybersecurity",
  "description": "Security research and best practices"
}
```

`PUT /admin/domains/{domainId}`

```bash
DOMAIN_ID="7b03ce18-e1f6-4e52-9f4c-0e4b575d2f6d"

curl -s -X PUT "http://localhost:8080/admin/domains/$DOMAIN_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name": "Cyber Security",
    "description": "Updated domain description"
  }'
```

Example response:

```json
{
  "domainId": "7b03ce18-e1f6-4e52-9f4c-0e4b575d2f6d",
  "name": "Cyber Security",
  "description": "Updated domain description"
}
```

`DELETE /admin/domains/{domainId}`

```bash
curl -i -X DELETE "http://localhost:8080/admin/domains/$DOMAIN_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Expected status: `204 No Content`

Request payload for create/update:

```json
{
  "name": "string (required, 2..255 chars)",
  "description": "string (optional, max 4000 chars)"
}
```

---

### Admin Users (ADMIN only)

All routes below require a JWT for a user with role `ADMIN`.

`GET /admin/users`

```bash
curl -s http://localhost:8080/admin/users \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Example response:

```json
[
  {
    "userId": "f90ff7b6-c8c4-45f6-9f26-7f67f65d4b07",
    "email": "user@example.com",
    "username": "user1",
    "role": "USER",
    "createdAt": "2026-04-22T10:00:00.000Z"
  }
]
```

`PUT /admin/users/{userId}/role`

```bash
USER_ID="f90ff7b6-c8c4-45f6-9f26-7f67f65d4b07"

curl -s -X PUT "http://localhost:8080/admin/users/$USER_ID/role" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "role": "MODERATOR"
  }'
```

Request payload:

```json
{
  "role": "ADMIN | MODERATOR | USER"
}
```

`DELETE /admin/users/{userId}`

```bash
curl -i -X DELETE "http://localhost:8080/admin/users/$USER_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Expected status: `204 No Content`

---

### Admin Researchers (ADMIN only)

All routes below require a JWT for a user with role `ADMIN`.

Researchers are managed as a standalone resource (not a `users` account).

`GET /admin/researchers`

Optional query params:

- `name`: filter by researcher name (contains, case-insensitive)
- `domainIds`: optional list of domain UUIDs; when provided, only researchers that specialize in **all** listed domains are returned

```bash
curl -s "http://localhost:8080/admin/researchers?name=alice&domainIds=57a070a0-8458-46ad-b6f6-f1c31dc7ec67&domainIds=4d4fc5f3-b6ab-42b4-9cc2-7c5a663e2a31" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Example response:

```json
[
  {
    "researcherId": "6f5e2a0b-95b6-4306-b263-4d5f4f2f1ef7",
    "email": "researcher@example.com",
    "name": "alice researcher",
    "biographie": "AI researcher",
    "domains": [
      {
        "domainId": "57a070a0-8458-46ad-b6f6-f1c31dc7ec67",
        "name": "Artificial Intelligence",
        "description": "AI domain"
      }
    ],
    "createdAt": "2026-04-22T10:00:00.000Z"
  }
]
```

`POST /admin/researchers`

```bash
curl -s -X POST "http://localhost:8080/admin/researchers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name": "alice researcher",
    "email": "researcher@example.com",
    "biographie": "AI researcher",
    "domainIds": [
      "57a070a0-8458-46ad-b6f6-f1c31dc7ec67"
    ]
  }'
```

Example response (`201 Created`):

```json
{
  "researcherId": "6f5e2a0b-95b6-4306-b263-4d5f4f2f1ef7",
  "email": "researcher@example.com",
  "name": "alice researcher",
  "biographie": "AI researcher",
  "domains": [
    {
      "domainId": "57a070a0-8458-46ad-b6f6-f1c31dc7ec67",
      "name": "Artificial Intelligence",
      "description": "AI domain"
    }
  ],
  "createdAt": "2026-04-22T10:00:00.000Z"
}
```

`PUT /admin/researchers/{researcherId}`

```bash
RESEARCHER_ID="6f5e2a0b-95b6-4306-b263-4d5f4f2f1ef7"

curl -s -X PUT "http://localhost:8080/admin/researchers/$RESEARCHER_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name": "alice updated",
    "email": "alice.updated@example.com",
    "biographie": "Updated bio",
    "domainIds": [
      "57a070a0-8458-46ad-b6f6-f1c31dc7ec67"
    ]
  }'
```

Request payload:

```json
{
  "name": "string (required, 3..255 chars)",
  "email": "string (required, valid email, max 255 chars)",
  "biographie": "string (optional, max 4000 chars)",
  "domainIds": ["uuid", "..."]
}
```

Notes:

- If `domainIds` is omitted, existing domain links are kept.
- If `domainIds` is provided, existing links are replaced.
- If `domainIds` is an empty array, all researcher domain links are removed.

`DELETE /admin/researchers/{researcherId}`

```bash
curl -i -X DELETE "http://localhost:8080/admin/researchers/$RESEARCHER_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Expected status: `204 No Content`

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

Domain admin errors may include:

- `DOMAIN_NOT_FOUND`
- `DOMAIN_NAME_ALREADY_EXISTS`
- `INVALID_DOMAIN_NAME`

Admin user/researcher errors may include:

- `USER_NOT_FOUND`
- `RESEARCHER_NOT_FOUND`
- `INVALID_ROLE`

Admin routes may also return `403 FORBIDDEN` when a non-admin token is used.

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
