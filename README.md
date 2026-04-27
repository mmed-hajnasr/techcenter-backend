# Techcenter Backend API

Spring Boot backend with PostgreSQL, Hibernate/JPA, JWT auth, and Argon2 password hashing.

## Base URL

- Local default: `http://localhost:8080`

## Auth Model (current)

- JWT-first authentication.
- User roles: `ADMIN`, `MODERATOR`, `USER`.
- Public endpoints: `POST /signup`, `POST /login`, `GET /health`.
- Authenticated `USER` can access read-only `GET` for `actualites`, `domains`, `researchers`, and `publications`.
- Protected endpoint: `GET /user/me`.
- Send JWT in header: `Authorization: Bearer <token>`.

## Quick Start

### 1) Start infrastructure (optional via Docker Compose)

```bash
docker compose up -d postgres redis minio
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

### User Actualites

`GET /user/actualites` (requires JWT)

Returns all actualites ordered by `estEnAvant` (featured first), then by `datePublication` descending.

```bash
TOKEN="<paste-access-token-here>"

curl -s http://localhost:8080/user/actualites \
  -H "Authorization: Bearer $TOKEN"
```

Example response:

```json
[
  {
    "actualiteId": "64e13d8d-4a5f-4a50-b2d7-fd787f7f2473",
    "titre": "Nouvelle annonce de recherche",
    "contenu": "Appel à candidatures pour un nouveau projet IA.",
    "datePublication": "2026-04-22T10:00:00Z",
    "estEnAvant": true,
    "moderateurId": "f2b4ad2e-ef2a-4eb0-96cf-b6ae4ebfd0ec"
  }
]
```

---

### Domains

`GET /admin/domains` (requires JWT: `USER` / `MODERATOR` / `ADMIN`)

```bash
TOKEN="<paste-access-token-here>"

curl -s http://localhost:8080/admin/domains \
  -H "Authorization: Bearer $TOKEN"
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

Requires JWT for role `ADMIN`.

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

Requires JWT for role `ADMIN`.

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

Requires JWT for role `ADMIN`.

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

### Researchers

Researchers are managed as a standalone resource (not a `users` account).

`GET /admin/researchers` (requires JWT: `USER` / `MODERATOR` / `ADMIN`)

Optional query params:

- `name`: filter by researcher name (contains, case-insensitive)
- `domainIds`: optional list of domain UUIDs; when provided, only researchers that specialize in **all** listed domains are returned

```bash
TOKEN="<paste-access-token-here>"

curl -s "http://localhost:8080/admin/researchers?name=alice&domainIds=57a070a0-8458-46ad-b6f6-f1c31dc7ec67&domainIds=4d4fc5f3-b6ab-42b4-9cc2-7c5a663e2a31" \
  -H "Authorization: Bearer $TOKEN"
```

Example response:

```json
[
  {
    "researcherId": "6f5e2a0b-95b6-4306-b263-4d5f4f2f1ef7",
    "name": "alice researcher",
    "biographie": "AI researcher",
    "domains": [
      {
        "domainId": "57a070a0-8458-46ad-b6f6-f1c31dc7ec67",
        "name": "Artificial Intelligence",
        "description": "AI domain"
      }
    ],
    "createdAt": "2026-04-22T10:00:00.000Z",
    "photoUrl": "http://localhost:9000/techcenter-photos/researchers/.../photo?..."
  }
]
```

Notes:

- `photoUrl` is a **MinIO presigned download URL**.
- It is generated at response time and may be `null` when no photo is uploaded.
- URL expiration is controlled by `MINIO_PRESIGN_EXPIRY_SECONDS` (default `3600`).

`POST /admin/researchers`

Requires JWT for role `ADMIN`.

```bash
curl -s -X POST "http://localhost:8080/admin/researchers" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name": "alice researcher",
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
  "name": "alice researcher",
  "biographie": "AI researcher",
  "domains": [
    {
      "domainId": "57a070a0-8458-46ad-b6f6-f1c31dc7ec67",
      "name": "Artificial Intelligence",
      "description": "AI domain"
    }
  ],
  "createdAt": "2026-04-22T10:00:00.000Z",
  "photoUrl": null
}
```

`PUT /admin/researchers/{researcherId}`

Requires JWT for role `ADMIN`.

```bash
RESEARCHER_ID="6f5e2a0b-95b6-4306-b263-4d5f4f2f1ef7"

curl -s -X PUT "http://localhost:8080/admin/researchers/$RESEARCHER_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "name": "alice updated",
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
  "biographie": "string (optional, max 4000 chars)",
  "domainIds": ["uuid", "..."]
}
```

Notes:

- If `domainIds` is omitted, existing domain links are kept.
- If `domainIds` is provided, existing links are replaced.
- If `domainIds` is an empty array, all researcher domain links are removed.

`DELETE /admin/researchers/{researcherId}`

Requires JWT for role `ADMIN`.

```bash
curl -i -X DELETE "http://localhost:8080/admin/researchers/$RESEARCHER_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Expected status: `204 No Content`

`PUT /admin/researchers/{researcherId}/photo`

Requires JWT for role `ADMIN`. Uploads/overwrites researcher photo in MinIO.

```bash
curl -s -X PUT "http://localhost:8080/admin/researchers/$RESEARCHER_ID/photo" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -F "photo=@/absolute/path/to/photo.jpg"
```

Example response:

```json
{
  "researcherId": "6f5e2a0b-95b6-4306-b263-4d5f4f2f1ef7",
  "name": "alice researcher",
  "biographie": "AI researcher",
  "domains": [],
  "createdAt": "2026-04-22T10:00:00.000Z",
  "photoUrl": "http://localhost:9000/techcenter-photos/researchers/.../photo?..."
}
```

`DELETE /admin/researchers/{researcherId}/photo`

Requires JWT for role `ADMIN`. Deletes researcher photo from MinIO and clears stored path.

```bash
curl -s -X DELETE "http://localhost:8080/admin/researchers/$RESEARCHER_ID/photo" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

---

### Publications

`GET /admin/publications` (requires JWT: `USER` / `MODERATOR` / `ADMIN`)

```bash
TOKEN="<paste-access-token-here>"

curl -s http://localhost:8080/admin/publications \
  -H "Authorization: Bearer $TOKEN"
```

Example response:

```json
[
  {
    "publicationId": "f1c3d6a8-6c43-4b4e-b4c2-8e0c2c4f09f2",
    "titre": "AI for Climate Modeling",
    "resume": "A survey of ML methods for climate simulations.",
    "doi": "10.1000/xyz123",
    "datePublication": "2026-04-22T10:00:00Z",
    "authors": [
      {
        "researcherId": "6f5e2a0b-95b6-4306-b263-4d5f4f2f1ef7",
        "name": "alice researcher"
      }
    ],
    "pdfUrl": "http://localhost:9000/techcenter-pdfs/publications/.../pdf?..."
  }
]
```

Notes:

- `pdfUrl` is a **MinIO presigned download URL**.
- It is generated at response time and may be `null` when no PDF is uploaded.
- URL expiration is controlled by `MINIO_PRESIGN_EXPIRY_SECONDS` (default `3600`).

`POST /admin/publications`

Requires JWT for role `ADMIN`.

```bash
curl -s -X POST "http://localhost:8080/admin/publications" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "titre": "AI for Climate Modeling",
    "resume": "A survey of ML methods for climate simulations.",
    "doi": "10.1000/xyz123",
    "datePublication": "2026-04-22T10:00:00Z",
    "researcherIds": [
      "6f5e2a0b-95b6-4306-b263-4d5f4f2f1ef7"
    ]
  }'
```

Example response (`201 Created`):

```json
{
  "publicationId": "f1c3d6a8-6c43-4b4e-b4c2-8e0c2c4f09f2",
  "titre": "AI for Climate Modeling",
  "resume": "A survey of ML methods for climate simulations.",
  "doi": "10.1000/xyz123",
  "datePublication": "2026-04-22T10:00:00Z",
  "authors": [
    {
      "researcherId": "6f5e2a0b-95b6-4306-b263-4d5f4f2f1ef7",
      "name": "alice researcher"
    }
  ],
  "pdfUrl": null
}
```

`PUT /admin/publications/{publicationId}`

Requires JWT for role `ADMIN`.

```bash
PUBLICATION_ID="f1c3d6a8-6c43-4b4e-b4c2-8e0c2c4f09f2"

curl -s -X PUT "http://localhost:8080/admin/publications/$PUBLICATION_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -d '{
    "titre": "AI for Climate Forecasting",
    "resume": "Updated abstract.",
    "doi": "10.1000/xyz123",
    "datePublication": "2026-04-23T10:00:00Z",
    "researcherIds": [
      "6f5e2a0b-95b6-4306-b263-4d5f4f2f1ef7"
    ]
  }'
```

Request payload for create/update:

```json
{
  "titre": "string (required, 2..255 chars)",
  "resume": "string (optional, max 4000 chars)",
  "doi": "string (optional, max 255 chars, unique case-insensitive)",
  "datePublication": "ISO-8601 datetime with offset (optional)",
  "researcherIds": ["uuid", "..."]
}
```

Notes:

- `researcherIds` is required (cannot be null).
- If `researcherIds` is an empty array, the publication is saved with no authors.
- If one or more `researcherIds` do not exist, request fails with `RESEARCHER_NOT_FOUND`.

`DELETE /admin/publications/{publicationId}`

Requires JWT for role `ADMIN`.

```bash
curl -i -X DELETE "http://localhost:8080/admin/publications/$PUBLICATION_ID" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

Expected status: `204 No Content`

`PUT /admin/publications/{publicationId}/pdf`

Requires JWT for role `ADMIN`. Uploads/overwrites publication PDF in MinIO.

```bash
curl -s -X PUT "http://localhost:8080/admin/publications/$PUBLICATION_ID/pdf" \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -F "pdf=@/absolute/path/to/document.pdf"
```

`DELETE /admin/publications/{publicationId}/pdf`

Requires JWT for role `ADMIN`. Deletes publication PDF from MinIO and clears stored path.

```bash
curl -s -X DELETE "http://localhost:8080/admin/publications/$PUBLICATION_ID/pdf" \
  -H "Authorization: Bearer $ADMIN_TOKEN"
```

---

### Moderator Actualites (MODERATOR/ADMIN only)

All routes below require a JWT for a user with role `MODERATOR` (or `ADMIN`, per security rules).

`POST /moderator/actualites`

```bash
MODERATOR_TOKEN="<paste-moderator-access-token-here>"

curl -s -X POST "http://localhost:8080/moderator/actualites" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MODERATOR_TOKEN" \
  -d '{
    "titre": "Nouvelle annonce de recherche",
    "contenu": "Appel à candidatures pour un nouveau projet IA.",
    "datePublication": "2026-04-22T10:00:00Z",
    "estEnAvant": true
  }'
```

Example response (`201 Created`):

```json
{
  "actualiteId": "64e13d8d-4a5f-4a50-b2d7-fd787f7f2473",
  "titre": "Nouvelle annonce de recherche",
  "contenu": "Appel à candidatures pour un nouveau projet IA.",
  "datePublication": "2026-04-22T10:00:00Z",
  "estEnAvant": true,
  "moderateurId": "f2b4ad2e-ef2a-4eb0-96cf-b6ae4ebfd0ec"
}
```

`PUT /moderator/actualites/{actualiteId}`

```bash
ACTUALITE_ID="64e13d8d-4a5f-4a50-b2d7-fd787f7f2473"

curl -s -X PUT "http://localhost:8080/moderator/actualites/$ACTUALITE_ID" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $MODERATOR_TOKEN" \
  -d '{
    "titre": "Annonce mise à jour",
    "contenu": "Contenu mis a jour de cette actualite.",
    "datePublication": "2026-04-23T09:30:00Z",
    "estEnAvant": false
  }'
```

Example response:

```json
{
  "actualiteId": "64e13d8d-4a5f-4a50-b2d7-fd787f7f2473",
  "titre": "Annonce mise à jour",
  "contenu": "Contenu mis à jour de l'actualité.",
  "datePublication": "2026-04-23T09:30:00Z",
  "estEnAvant": false,
  "moderateurId": "f2b4ad2e-ef2a-4eb0-96cf-b6ae4ebfd0ec"
}
```

`DELETE /moderator/actualites/{actualiteId}`

```bash
curl -i -X DELETE "http://localhost:8080/moderator/actualites/$ACTUALITE_ID" \
  -H "Authorization: Bearer $MODERATOR_TOKEN"
```

Expected status: `204 No Content`

Request payload for create/update:

```json
{
  "titre": "string (required, 2..255 chars)",
  "contenu": "string (required, 1..4000 chars)",
  "datePublication": "ISO-8601 datetime with offset (optional)",
  "estEnAvant": "boolean (optional, defaults to false on create)"
}
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

Admin publication errors may include:

- `PUBLICATION_NOT_FOUND`
- `PUBLICATION_DOI_ALREADY_EXISTS`
- `INVALID_PUBLICATION_TITLE`
- `RESEARCHER_NOT_FOUND`
- `INVALID_FILE`

Moderator actualite errors may include:

- `ACTUALITE_NOT_FOUND`
- `MODERATOR_NOT_FOUND`
- `INVALID_ACTUALITE_TITLE`
- `INVALID_ACTUALITE_CONTENT`

Admin write routes (`POST`, `PUT`, `DELETE`) may return `403 FORBIDDEN` when a non-admin token is used.

---

## Environment Variables

Key settings (from `src/main/resources/application.properties`):

- `DATABASE__HOST` (default `localhost`)
- `DATABASE__PORT` (default `5432`)
- `DATABASE__NAME` (default `techcenter`)
- `DATABASE__USERNAME` (default `postgres`)
- `DATABASE__PASSWORD` (default `postgres`)
- `JWT_SECRET` (must be at least 32 bytes in production)
- `JWT_EXPIRATION` (default `86400`)
- `JWT_ISSUER` (default `techcenter-backend`)
- `MINIO_ENDPOINT` (default `http://localhost:9000`)
- `MINIO_ROOT_USER` (default `minio`)
- `MINIO_ROOT_PASSWORD` (default `minio12345`)
- `MINIO_PHOTO_BUCKET` (default `techcenter-photos`)
- `MINIO_PDF_BUCKET` (default `techcenter-pdfs`)
- `MINIO_PRESIGN_EXPIRY_SECONDS` (default `3600`)

Example:

```bash
export JWT_SECRET="replace-with-a-long-random-secret-at-least-32-bytes"
export JWT_EXPIRATION="86400"
export JWT_ISSUER="techcenter-backend"
export MINIO_ENDPOINT="http://localhost:9000"
export MINIO_ROOT_USER="minio"
export MINIO_ROOT_PASSWORD="minio12345"
export MINIO_PHOTO_BUCKET="techcenter-photos"
export MINIO_PDF_BUCKET="techcenter-pdfs"
export MINIO_PRESIGN_EXPIRY_SECONDS="3600"
```
