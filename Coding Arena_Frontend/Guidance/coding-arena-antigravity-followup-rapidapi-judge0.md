# Coding Arena — Antigravity Follow-up: Switch to RapidAPI-Hosted Judge0

## Context
Local self-hosted Judge0 (Docker Desktop on Windows/WSL2) cannot execute
code due to a confirmed, unfixable-on-this-machine kernel limitation
(cgroup v2 vs. isolate's cgroup v1 requirement — thoroughly diagnosed,
see project documentation). Switching to RapidAPI's hosted Judge0 CE,
which is the exact same Judge0 engine and API shape, just running on
RapidAPI's own Linux infrastructure instead of locally. This should be a
small, low-risk change since the request/response format is nearly
identical to what `Judge0Client` already sends.

**No need to remove or touch the local Docker/Judge0/WSL2 setup** — it
can stay installed and unused; this change only redirects where the
HTTP calls go.

## What changes vs. the original Judge0Client

1. **URL**: from `${JUDGE0_URL:http://localhost:2358}/submissions?wait=true`
   to `https://judge0-ce.p.rapidapi.com/submissions?wait=true`

2. **New required headers** on every request:
   ```
   X-RapidAPI-Key: ${RAPIDAPI_KEY}
   X-RapidAPI-Host: judge0-ce.p.rapidapi.com
   ```
   Add a new config property `rapidapi.key: ${RAPIDAPI_KEY:}` in
   `application.yml`, injected into `Judge0Client` via `@Value`.

3. **Request body**: same shape as before (`source_code`, `language_id`,
   `stdin`, `expected_output`, `cpu_time_limit`, `memory_limit`) — no
   change needed here, RapidAPI's Judge0 CE accepts the same payload.

4. **Response parsing**: same `Judge0Response` shape as before
   (`stdout`, `stderr`, `status.id`, `status.description`) — no change
   needed to `mapJudge0ResponseToVerdict` in `SubmissionService`.

## Requirements

- Update `Judge0Client.java`:
  - Change the base URL to the RapidAPI endpoint (keep it configurable
    via a property, don't hardcode, in case it needs to change later).
  - Add the two RapidAPI headers to every request alongside the existing
    `Content-Type: application/json` header.
  - Keep the existing timeout, retry/fallback-on-error behavior
    (returning a synthetic "Internal Error" `Judge0Response` on any
    exception) exactly as it was.
- Update `application.yml`:
  ```yaml
  judge0:
    url: ${JUDGE0_URL:https://judge0-ce.p.rapidapi.com}
    mock-mode: ${JUDGE0_MOCK_MODE:false}
  rapidapi:
    key: ${RAPIDAPI_KEY:}
  ```
- Do NOT change `SubmissionService`, `EloService`, the atomic
  winner-update, rate limiting, or any verdict-mapping logic — this
  change is scoped entirely to `Judge0Client` and its config.
- Keep `JUDGE0_MOCK_MODE` working exactly as before, as a fallback that
  bypasses the real HTTP call entirely when enabled.

## What NOT to do
- Do not remove the local `JUDGE0_URL` env var/default — just repoint
  its default value; someone could still run local Judge0 later if the
  Docker/WSL2 issue is ever resolved.
- Do not log or expose the RapidAPI key value anywhere (logs, error
  messages, responses).
- Do not touch the local Docker/docker-compose files.

## After generation, I will manually verify
- With `JUDGE0_MOCK_MODE=false` and a real `RAPIDAPI_KEY` set, a genuine
  correct algorithmic solution (not a substring trick) returns
  `ACCEPTED` for a seeded problem.
- An intentionally wrong solution returns `WRONG_ANSWER`.
- The RapidAPI key is never logged in plaintext anywhere.
