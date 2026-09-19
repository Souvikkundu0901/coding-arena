# Coding Arena — Antigravity Follow-up: Rate Limiting

## Context
No rate limiting exists anywhere in the app currently. Adding lightweight,
in-memory rate limiting to protect against brute-force login attempts,
matchmaking spam, and submission flooding (which would waste Judge0/DB
resources). This is a college project — no need for distributed rate
limiting (Redis-backed) infrastructure; per-instance in-memory limits are
sufficient.

## Library
Use **Bucket4j** (in-memory token bucket, no external infra needed):
```xml
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.10.1</version>
</dependency>
```

## Requirements

1. Create a `RateLimitInterceptor` (Spring `HandlerInterceptor`) or a
   servlet `Filter` that applies different limits per endpoint group,
   keyed by client IP address (use `X-Forwarded-For` header if present,
   else `request.getRemoteAddr()`):

   - `POST /api/auth/login` — **5 requests per minute** per IP
   - `POST /api/auth/register` — **3 requests per minute** per IP
   - `POST /api/matchmaking/queue` — **10 requests per minute** per
     authenticated user (key by user id from JWT, not IP, since
     legitimate users may share an IP/NAT)
   - `POST /api/matches/{matchId}/submissions` — **10 requests per
     minute** per authenticated user (same reasoning — key by user id)

2. When a client exceeds their limit, return **429 Too Many Requests**
   with the standard error shape: `{ "error": "Rate limit exceeded, try
   again later" }`.

3. Buckets should refill continuously (standard token bucket), not reset
   as a hard once-per-minute window — e.g. Bucket4j's
   `Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1)))` pattern.

4. Store buckets in a simple in-memory `ConcurrentHashMap<String, Bucket>`
   keyed by IP or user id as appropriate — no need for Redis-backed
   buckets for this project's scale.

5. Apply this to the two IP-keyed auth endpoints and the two user-keyed
   endpoints listed above only. Do not rate-limit `GET` endpoints
   (`/api/auth/me`, `/api/matches/{id}`, etc.) — those are read-only and
   not a meaningful abuse vector for this app.

## What NOT to do
- Do not add Redis-backed distributed rate limiting — unnecessary
  complexity for this project's scale and deployment target.
- Do not rate-limit WebSocket connections/subscriptions — out of scope
  for this pass.
- Do not change any existing business logic in Auth, Matchmaking, or
  Submission services — this is purely an additive interceptor/filter
  layer sitting in front of the existing controllers.

## After generation, I will manually verify
- That hitting an endpoint past its limit actually returns 429, not a
  silent pass-through.
- That the bucket correctly refills after the time window (not just
  permanently locked out after one burst).
- That IP-keyed and user-keyed buckets are correctly scoped (two
  different users behind the same IP shouldn't share a submission-rate
  bucket, for example).
