# Coding Arena — Antigravity Follow-up: Match Time Limit

## Context
Matches currently stay `IN_PROGRESS` indefinitely until someone submits a
correct solution — there's no time limit. Adding a match duration so
games behave like a real timed competitive match (similar to chess.com's
time controls), with automatic expiry if nobody solves it in time.

## Requirements

1. Add a `MATCH_DURATION_MINUTES` config value (env var, default `15`):
   ```yaml
   match:
     duration-minutes: ${MATCH_DURATION_MINUTES:15}
   ```

2. When a match is created (in `MatchmakingService.createMatchAndNotify`),
   also compute and store an expiry time. Add a new column to the
   `matches` table via a new Flyway migration:
   ```sql
   ALTER TABLE matches ADD COLUMN expires_at TIMESTAMP;
   ```
   Set `expires_at = started_at + duration` at match creation time.

3. Add a new scheduled task (similar pattern to
   `MatchmakingService.processQueue`) — e.g. `MatchExpiryService` with a
   `@Scheduled(fixedDelay = 10000)` method that runs every 10 seconds:
   - Finds all matches where `status = 'IN_PROGRESS'` AND
     `expires_at < now()`.
   - For each expired match, atomically update it — reuse the same
     "only affects a row if not already completed" pattern used for the
     winner update:
     ```sql
     UPDATE matches SET status = 'EXPIRED', ended_at = now()
     WHERE id = ? AND status = 'IN_PROGRESS' AND winner_id IS NULL
     ```
     This must NOT overwrite a match that was already completed with a
     winner between the last check and now — the `WHERE` clause guards
     against that race the same way the existing winner-update does.
   - If the update actually affected a row (row count == 1), broadcast a
     `MATCH_END` event on `/topic/match/{matchId}` with a payload
     indicating no winner, e.g. `{ "type": "MATCH_END", "data": {
     "matchId": ..., "winnerId": null, "reason": "TIME_EXPIRED" } }`.

4. `GET /api/matches/{matchId}` response should include `expiresAt` so
   the frontend can render a countdown timer.

5. `SubmissionService.createSubmission` should reject new submissions to
   an `EXPIRED` match the same way it already rejects `COMPLETED` matches
   (409 Conflict) — reuse or extend the existing
   `MatchCompletedException` check to also cover `EXPIRED` status.

## What NOT to do
- Do not change the Elo rating logic — an expired match with no winner
  should NOT trigger any rating change for either player.
- Do not change the existing winner-declaration atomic update logic —
  this is a separate, parallel atomic check for expiry, not a
  replacement.
- Do not add a countdown timer UI — that's frontend work, out of scope
  for this backend-only change. Just expose `expiresAt` in the API
  response.

## After generation, I will manually verify
- That an expired match's atomic update correctly refuses to overwrite a
  match that got a real winner in the same moment (test by manually
  setting a short `MATCH_DURATION_MINUTES` for testing and racing a
  submission against expiry).
- That submitting to an expired match returns 409, not a stale success.
- That no rating change occurs for an expired/unsolved match.
