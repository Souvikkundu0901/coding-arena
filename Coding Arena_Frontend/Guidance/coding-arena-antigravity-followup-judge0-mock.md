# Coding Arena — Antigravity Follow-up: Judge0 Mock Mode

## Context
Local Judge0 (self-hosted via Docker) is hitting a platform-specific sandboxing
failure on Windows/WSL2/Docker Desktop — the `isolate` sandbox used internally
by Judge0 workers fails with "No such file or directory" when creating
per-submission sandbox directories, regardless of whether Judge0 runs from
the Windows-mounted filesystem or WSL2's native filesystem. This is a known
class of issue with `isolate` + Docker Desktop's WSL2 backend, not a bug in
our code or config.

RapidAPI's hosted Judge0 CE also no longer has a genuinely free tier (their
current cheapest plan is pay-per-use and requires a card on file).

## What I need
Add a **mock/stub mode** to `Judge0Client` so the rest of the pipeline
(Submission Service, atomic winner update, Elo rating, WebSocket MATCH_END
broadcast) can be tested and demoed end-to-end without depending on a
working real Judge0 instance.

## Requirements

1. Add a new env var: `JUDGE0_MOCK_MODE` (boolean, default `false`).
   Wire it into `application.yml` as `judge0.mock-mode: ${JUDGE0_MOCK_MODE:false}`.

2. In `Judge0Client`, when mock mode is enabled, `execute(...)` should NOT
   call the real Judge0 HTTP endpoint at all. Instead, return a synthetic
   `Judge0Response` based on simple logic:
   - If the submitted `code` (case-insensitive, trimmed) contains the
     exact `expectedOutput` string as a literal substring, return a
     response with `status.id = 3` ("Accepted") and `stdout` set to
     exactly `expectedOutput`.
   - Otherwise, return `status.id = 4` ("Wrong Answer") with `stdout` set
     to an empty string.
   - Simulate a small delay (e.g. `Thread.sleep(300)`) so it doesn't feel
     instantaneous/fake during a live demo.

3. Log a clear, visible warning every time mock mode handles a request, so
   it's never mistaken for real judging:
   ```
   log.warn("JUDGE0_MOCK_MODE is enabled — returning simulated verdict, NOT real code execution");
   ```

4. Do not change any other part of `SubmissionService`, `EloService`, or
   the atomic winner-update logic — mock mode only replaces what
   `Judge0Client.execute()` returns, nothing downstream of it.

5. Update `run.ps1` guidance (just tell me what line to add, don't edit
   files outside the Java project) — I'll add `$env:JUDGE0_MOCK_MODE="true"`
   myself.

## What NOT to do
- Do not remove or modify the real Judge0 HTTP-calling code path — mock
  mode must be a toggle, not a replacement. Real Judge0 integration should
  still work exactly as before when `JUDGE0_MOCK_MODE=false`.
- Do not make mock mode the default — it must default to `false` so a
  future real-Judge0 run doesn't accidentally use fake verdicts.
- Do not touch matchmaking, match, or auth code — this is scoped only to
  `Judge0Client`.

## After generation, I will manually verify
- That `JUDGE0_MOCK_MODE=false` (or unset) still attempts the real HTTP
  call to Judge0, unchanged from before.
- That `JUDGE0_MOCK_MODE=true` returns a mock verdict without any network
  call, and logs the warning every time.
