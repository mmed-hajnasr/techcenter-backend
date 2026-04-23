---
agent: agent
---

instrument the mentioned endpoint using this guide: do not run the tests after you are done.
This guide explains how to instrument endpoints in this project with tracing and logs, following the conventions used in this repository.

## Goals

- Keep traces **comprehensive** (request flow + important internal steps).
- Keep logs **non-repetitive** and actionable.
- Keep expected client/auth failures at **info** level.
- Keep server failures at **error** level.
- Attach logs/events to the **lowest relevant span**.

## 1) Instrument the Controller Endpoint

Use `EndpointTraceSupport` in controllers to create the top-level endpoint span.

### Pattern

```java
return endpointTraceSupport.inSpan(
    "auth.login",                 // span name
    "/login",                     // endpoint tag
    "login",                      // operation tag
    () -> ResponseEntity.ok(authService.login(request))
);
```

### Rules

- Use a clear, stable span name: `<domain>.<action>` (example: `auth.signup`, `admin.users.list`).
- Always provide:
  - `endpoint` (route pattern)
  - `operation` (business action)
- Add contextual tags only when useful (e.g. `userId`, `domainId`, filter counts).

## 2) JWT Filter Span Structure

Use exactly one JWT filter span named `auth.jwt.filter`, and keep it as a **child span** of the endpoint span when an endpoint span exists.

### Rules

- Keep JWT verification semantics in one span only:
    - ✅ `auth.jwt.filter`
    - ❌ `auth.jwt.extract-bearer-token`
    - ❌ `auth.jwt.verify-token`
    - ❌ `auth.jwt.set-security-context`
- Emit exactly one JWT validation event in that span with validity + role.
- Keep one concise JWT validation log line with validity + role.
- For requests that do not reach a controller endpoint (for example, blocked early), no endpoint span may exist; in that case, do not force an invalid parent-child relation.

### Example event/log semantics

- Event: `auth.jwt.validation result=valid role=ADMIN`
- Event: `auth.jwt.validation result=invalid role=unknown`
- Log: `auth.jwt.validation result=<valid|invalid> role=<ROLE|unknown|none> ...`

## 3) Add Deep Service Spans for Important Steps

Inside services (like `AuthService`), create step spans for meaningful work.

### Required naming conventions

- DB-access spans must include `db-` in the step name:
  - ✅ `auth.login.db-lookup-user`
  - ✅ `auth.signup.db-check-email-exists`
  - ✅ `auth.signup.db-save-user`
  - ✅ `auth.user.db-find-by-id`

- Non-DB computational/business steps:
  - `auth.signup.hash-password`
  - `auth.login.verify-password`
  - `auth.login.generate-token`

### Pattern (helper)

```java
private <T> T inStep(String stepSpanName, Supplier<T> action, String... tagPairs) {
    Span span = tracer.nextSpan().name(stepSpanName).start();
    try (Tracer.SpanInScope ws = tracer.withSpan(span)) {
        applyTags(span, tagPairs);
        return action.get();
    } finally {
        span.end();
    }
}
```

## 4) Put Events in the Lowest Relevant Span

If you want to observe an event in Jaeger, emit it **inside the span that represents that exact step**.

### Example: password verification event

```java
boolean passwordMatches = inStep(
    "auth.login.verify-password",
    () -> {
        boolean verified = passwordService.verifyPassword(request.password(), existingUser.getPasswordHash());
        String verificationResult = verified ? "match" : "mismatch";

        Span currentSpan = tracer.currentSpan();
        if (currentSpan != null) {
            currentSpan.tag("password.verification.result", verificationResult);
            currentSpan.event("auth.login.password_verification." + verificationResult);
        }

        log.info("auth.login.password_verification result={} userId={}", verificationResult, existingUser.getUserId());
        return verified;
    },
    "step", "verify-password",
    "userId", existingUser.getUserId().toString()
);
```

This ensures the event appears under `auth.login.verify-password` (not just in terminal logs).

## 5) Logging Severity Policy

Implemented in `EndpointTraceSupport`:

- Success: `info`
- Expected client/auth failures (`AuthException`): `info` with `result=client_error`
- Unexpected server/runtime failures: `error` and `span.error(exception)`

### Why

- Avoid noisy false alarms in logs for expected invalid credentials.
- Keep real server issues visible and alert-worthy.

## 6) Incorrect Login Behavior

For incorrect login, log an informative `info` event, for example:

- `auth.login.incorrect_credentials reason=user-not-found`
- `auth.login.password_verification result=mismatch userId=...`

Do **not** log these as server errors.

## 7) Endpoint Instrumentation Checklist

When adding a new endpoint:

1. Wrap controller method with `EndpointTraceSupport.inSpan(...)`.
2. Add deep service `inStep(...)` spans for major steps.
3. Prefix all DB-access step spans with `db-`.
4. Emit step events/tags in the lowest relevant span.
5. Keep JWT tracing as one span (`auth.jwt.filter`) with one validation event/log.
6. Ensure JWT filter span is nested under endpoint span when endpoint span exists.
7. Keep expected business/auth failures at `info` level.
8. Reserve `error` + `span.error` for true server failures.
9. Run focused regression tests for affected routes.

## 8) Files to Use as Reference

- `src/main/java/com/isi/techcenter_backend/tracing/EndpointTraceSupport.java`
- `src/main/java/com/isi/techcenter_backend/controller/AuthController.java`
- `src/main/java/com/isi/techcenter_backend/service/AuthService.java`
- `src/main/java/com/isi/techcenter_backend/security/JwtAuthenticationFilter.java`
- `src/main/java/com/isi/techcenter_backend/HealthController.java`
