## 1) Auth Architecture at a Glance

The backend should use a **JWT-first model** and split auth into these parts:

- **Auth handlers**: login, signup, confirm, forgot password, reset password, me, logout
- **Token service**: JWT issuance + verification (and optional refresh-token rotation)
- **Minimal session wrapper**: server session stores only `userId` (optional compatibility/state hook)
- **Auth middleware**:
  - JWT auth filter/middleware for authenticated-user routes
  - project guard middleware for project-scoped routes
- **Password service**: Argon2 hashing and credential verification
- **OTP service**: Redis-backed one-time codes for account verification and password reset
- **Persistence layer**: `db_*` functions for users/credentials operations
- **Error model**: public `AppErrorType` vs internal `InternalError`

## 2) Route Segmentation (Auth Boundaries)

Routes are grouped by authentication requirement:

- **Open routes** (`configure_open_routes`):
  - `POST /login`
  - `POST /signup`
  - `POST /confirm`
  - `POST /forgot-password`
  - `POST /reset-password`
- **User routes** (`/user` scope + `reject_anonymous_users`):
  - `GET /user/me`
  - `POST /user/logout`
- **Project routes** (`/project` scope + `reject_projectless_users`):
  - requires logged-in user + selected project in session

## 3) Token + Session Model

### JWT model

- Access token is JWT (short TTL) and carries auth claims.
- Suggested base claims:
  - `sub` = `userId`
  - `iat`, `exp`
  - optional `jti`, `iss`, `aud`
- Token transport:
  - `Authorization: Bearer <token>` (recommended)
  - or HttpOnly cookie if you prefer cookie transport

### Server session model (minimal)

If a server session exists, it must contain only:

- `user_id`

Do **not** store project/role/author flags in session.

Session API should be reduced to:

- `insert_user_id(user_id)`
- `get_user_id()`
- `log_out()` (purge/invalidate)

## 4) Authentication Flows

### 4.1 Signup

`POST /signup`

1. Validate input: username, email, password.
2. Hash password with Argon2 (blocking task offloaded).
3. `db_insert_user(...)`:
   - if inserted: create OTP with purpose `account_verification`, send verification email
   - if email already exists: create OTP with purpose `password_reset`, send forgot-password email with `is_new_account=true`
4. Return `200 OK` regardless of branch.

### 4.2 Confirm Account

`POST /confirm`

1. Verify OTP with purpose `account_verification`.
2. If invalid OTP: return `InvalidConfirmation`.
3. Mark user verified in DB (`db_update_user_verification`).
4. Issue JWT with `sub=userId`.
5. Optionally persist `user_id` in server session.
6. Return user identity payload (+ token or set auth cookie).

### 4.3 Login

`POST /login`

1. Validate password format.
2. Validate credentials:
   - read user hash by username/email (`db_find_credentials`)
   - verify Argon2 hash
   - uses default hash fallback when user not found (timing attack mitigation)
3. Load user record (`db_get_user`).
4. If `verified == false`: return `AccountNotVerified`.
5. Issue JWT with `sub=userId`.
6. Optionally persist `user_id` in server session.
7. Return user identity payload (+ token or set auth cookie).

### 4.4 Forgot Password

`POST /forgot-password`

1. Validate email format.
2. If verified account exists for email:
   - create OTP with purpose `password_reset`
   - send forgot-password email
3. If no account: do not fail publicly.
4. Enforce fixed minimum response time (~2 seconds) to reduce account enumeration/timing signals.
5. Return `200 OK`.

### 4.5 Reset Password

`POST /reset-password`

1. Validate new password format.
2. Verify OTP with purpose `password_reset`.
3. If invalid OTP: return `InvalidConfirmation`.
4. Hash and update password (`db_update_password_hash`).
5. Return `200 OK`.

### 4.6 Me / Logout

- `GET /user/me`: requires valid JWT; middleware extracts `userId` from token claims.
- `POST /user/logout`: client discards token; server may also purge minimal session and revoke refresh token/jti if implemented.

## 5) Middleware Behavior

### JWT auth middleware/filter

- Validates JWT signature, expiry, issuer/audience (if configured).
- Extracts `sub` as `userId`.
- If token missing/invalid/expired: returns `NotLoggedIn` or `Unauthorized`.
- Injects `UserId` into request context for handlers.

### `reject_projectless_users`

- First enforces authenticated user from JWT (`userId`).
- Then enforces project context from request/path/database lookup (not session state).
- Missing user -> `NotLoggedIn`.
- Missing project context -> `ProjectNotSelected`.

## 6) Password and OTP Rules

### Password

- Algorithm: **Argon2id**
- Parameters used: memory cost `15000`, iterations `2`, parallelism `1`
- Hash/verify operations are executed in blocking tasks

### OTP

- Format: 6-digit numeric code
- Storage: Redis key `otp:{purpose}:{email}`
- TTL: configured by Redis settings (`otp_ttl`)
- Purpose enum:
  - `account_verification`
  - `password_reset`
- OTP is deleted after successful verification

## 7) Error Contract (Auth-Relevant)

Public auth errors include:

- `IncorrectLogin`
- `NotLoggedIn`
- `AccountNotVerified`
- `Unauthorized`
- `InvalidEmail`
- `InvalidUsername`
- `InvalidConfirmation`
- `InvalidPassword`
- `ProjectNotSelected`

Status mapping:

- `IncorrectLogin`, `Unauthorized`, `NotLoggedIn`, `AccountNotVerified` -> `401`
- Validation/auth flow errors (e.g., invalid email/otp/password) -> `400`

Internal failures (DB/Redis/session/etc.) are mapped to `InternalServerError` in client responses, while details are logged server-side.

## 8) Spring Boot Mapping (Same Structure)

Use this one-to-one mapping to keep behavior equivalent:

- **Actix handlers** -> `@RestController` methods
- **Route scopes + middleware** -> Spring Security filter chain + endpoint authorization rules
- **JWT auth middleware** -> `OncePerRequestFilter` that validates token and sets `SecurityContext`
- **`UserSession` wrapper** -> optional service over `HttpSession`, storing only `userId`
- **Redis session store** -> optional (only if you keep minimal session state)
- **OTP handler** -> `OtpService` using `StringRedisTemplate`
- **Password module** -> `PasswordService` with Argon2PasswordEncoder
- **`db_*` persistence functions** -> repository/service methods
- **Typed app errors (`AppErrorType`)** -> custom exception hierarchy + `@ControllerAdvice`

Recommended Spring packages (auth only):

- `auth.controller` (login/signup/confirm/forgot/reset/logout/me)
- `auth.service` (auth orchestration, password, otp, session)
- `auth.security` (filters, config, auth entry points)
- `auth.model` (request/response DTOs)
- `auth.error` (error enum + exception mapping)

## 9) Minimal Behavior Checklist for a Port

To match this backend’s auth behavior in Spring Boot, preserve these invariants:

1. JWT is the primary authentication mechanism.
2. JWT `sub` contains `userId`.
3. Email verification gate before allowing login success.
4. OTP purposes separated (`account_verification` vs `password_reset`).
5. Forgot-password endpoint returns success even when account does not exist.
6. Timing-attack mitigation in forgot-password response path.
7. Middleware/filter-level auth checks that inject `userId` for handlers.
8. Public-safe error responses with internal detail logging.
9. If server session is used, it stores only `userId`.
