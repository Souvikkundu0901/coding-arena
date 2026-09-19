# Coding Arena — Antigravity Follow-up: Frontend Alignment

## Context
The frontend (built separately by a teammate) expects some fields and
endpoints that don't exist yet. This prompt covers the additions we're
committing to for this pass — see the accompanying audit doc for the
full list of what's deferred.

**Everything here is additive** — do not change any existing tested
behavior (atomic winner update, atomic expiry, Elo calculation, rate
limiting, WebSocket auth all stay exactly as they are).

## 1. Win/loss tracking

Add `wins` and `losses` columns to `users`:
```sql
ALTER TABLE users ADD COLUMN wins INT NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN losses INT NOT NULL DEFAULT 0;
```

Increment these in the same place Elo ratings are updated — i.e. inside
`EloService.updateRatings(winner, loser)`, alongside the existing rating
change, in the same transaction:
- `winner.wins += 1`
- `loser.losses += 1`

Do NOT increment anything on an expired match with no winner — matches
this exact rule already used for skipping rating changes on expiry.

Add `wins` and `losses` to `UserDto` so they appear in
`/api/auth/register`, `/api/auth/login`, and `/api/auth/me` responses.

## 2. Match history endpoint

New endpoint: `GET /api/users/me/matches?limit={n}` (authenticated,
default limit 4 if not specified).

Returns the current user's completed matches (status `COMPLETED` or
`EXPIRED`), most recent first, shaped as:
```json
[
  {
    "id": "uuid",
    "opponentUsername": "string",
    "result": "WIN | LOSS | EXPIRED",
    "ratingDelta": "number (signed; 0 if EXPIRED)"
  }
]
```
Determine `result` by comparing `winner_id` to the current user's id
(`WIN` if they won, `LOSS` if the opponent won, `EXPIRED` if
`winner_id IS NULL` and status is `EXPIRED`). `ratingDelta` requires
tracking what the rating was before/after — simplest approach: store the
delta at the time `EloService.updateRatings` runs (add a
`rating_delta_winner` / `rating_delta_loser` column to `matches`, or a
lightweight side table — your call on the cleanest implementation, just
make sure the returned `ratingDelta` is accurate per-match, not
recalculated after the fact from current ratings which would be wrong
after multiple matches).

## 3. Match response shape — add a `players` array

`GET /api/matches/{matchId}` currently exposes `playerA`/`playerB`
separately. Keep those fields as they are (other code may depend on
them), but ALSO add a `players` array to `MatchDto`:
```json
"players": [
  { "id": "uuid", "username": "string", "rating": "number" },
  { "id": "uuid", "username": "string", "rating": "number" }
]
```
Populated from the same playerA/playerB data, just also exposed as an
array for the frontend's opponent-matching logic.

## 4. Problem examples

Add an `examples` field to the problem data returned as part of a match
(`GET /api/matches/{matchId}`) and to the new problems endpoints (below):
```json
"examples": [
  { "input": "string", "output": "string" }
]
```
Populate from `test_cases` where `is_sample = true` for that problem
(map `input` → `input`, `expected_output` → `output`). Limit to a
reasonable number (e.g. first 2-3 sample cases) — don't expose every
test case, just the sample ones already marked `is_sample = true`.

## 5. Read-only problems browsing endpoints

- `GET /api/problems` (authenticated) — returns all problems:
  ```json
  [
    { "id": "uuid", "title": "string", "difficulty": "EASY|MEDIUM|HARD", "tags": [] }
  ]
  ```
  `tags` can be an empty array for now — we don't have a tags column;
  just return `[]` rather than omitting the field, so the frontend
  doesn't break on a missing key.

- `GET /api/problems/{problemId}` (authenticated) — full problem detail
  including `description` and `examples` (same shape as above).

## What NOT to do
- Do not build the friends-list system, profile editing, password
  change, or practice-mode standalone submissions — explicitly deferred,
  documented separately.
- Do not change match status values (`IN_PROGRESS`/`COMPLETED`/`EXPIRED`
  stay as they are) — the frontend will be told to adapt to these values
  rather than us renaming them.
- Do not change the `MATCH_END` WebSocket event's existing fields — just
  confirm it already includes `winnerId` (null on expiry), which the
  frontend will use instead of `winnerName`.
- Do not touch the atomic winner-update, atomic expiry, or rate limiting
  logic in any way.

## After generation, I will manually verify
- Win/loss counts increment correctly and only on genuine wins/losses,
  never on expiry.
- Match history returns correct WIN/LOSS/EXPIRED classification and
  accurate historical rating deltas (not recalculated from current
  rating).
- `players[]` array is present and correctly populated alongside the
  existing playerA/playerB fields.
- Problems endpoints return real data matching what's in the seeded
  problem bank.
