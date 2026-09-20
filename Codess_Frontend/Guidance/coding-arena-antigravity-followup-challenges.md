# Coding Arena — Antigravity Follow-up: Direct Challenge / Invite

## Context
Matchmaking currently only supports random pairing by closest rating.
Adding a second path: challenge a specific player by username directly,
bypassing the queue. This is additive — random matchmaking stays exactly
as-is; this is a parallel feature.

## Requirements

### 1. New DB table — challenges
New Flyway migration:
```sql
CREATE TABLE challenges (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  challenger_id UUID NOT NULL REFERENCES users(id),
  challenged_id UUID NOT NULL REFERENCES users(id),
  status VARCHAR NOT NULL DEFAULT 'PENDING',  -- PENDING / ACCEPTED / DECLINED / EXPIRED
  match_id UUID REFERENCES matches(id),        -- set once accepted and a match is created
  created_at TIMESTAMP DEFAULT now(),
  responded_at TIMESTAMP
);
```

### 2. Endpoints

- `POST /api/challenges` — authenticated. Body: `{ "username":
  "opponent_username" }`.
  - Reject 404 if the target username doesn't exist.
  - Reject 400 if challenging yourself.
  - Reject 409 if there's already a `PENDING` challenge between these two
    users (in either direction).
  - Create a `challenges` row with `status = 'PENDING'`.
  - Notify the challenged user via the existing per-user WebSocket
    channel: send a `CHALLENGE_RECEIVED` event on
    `/topic/user/{challengedUserId}` with challenger info and the
    challenge id.
  - Return the created challenge.

- `POST /api/challenges/{challengeId}/accept` — authenticated. Only the
  `challenged_id` user may call this.
  - Reject 403 if the caller isn't the challenged user.
  - Reject 409 if the challenge isn't `PENDING` (already
    accepted/declined/expired).
  - On success: atomically update the challenge to `ACCEPTED` (same
    "only affects a row if still PENDING" guard pattern used elsewhere —
    `WHERE id = ? AND status = 'PENDING'`).
  - If the atomic update succeeded, create a `Match` the same way
    `MatchmakingService.createMatchAndNotify` does (pick random problem,
    set `IN_PROGRESS`, set `expires_at` using the existing
    `match.duration-minutes` config), link it via `challenges.match_id`,
    and broadcast `MATCH_FOUND` to both players' personal channels
    (`/topic/user/{id}`), exactly like random matchmaking does.

- `POST /api/challenges/{challengeId}/decline` — authenticated. Only the
  challenged user may call this. Atomically updates status to
  `DECLINED` (same guard pattern). Notify the challenger via their
  personal channel with a `CHALLENGE_DECLINED` event.

- `GET /api/challenges/pending` — authenticated. Returns all `PENDING`
  challenges where the current user is the `challenged_id` (their
  incoming challenge inbox).

### 3. Reuse existing match-creation logic
Do not duplicate the match-creation code. Extract the "create match +
assign problem + set expiry + broadcast MATCH_FOUND" logic from
`MatchmakingService.createMatchAndNotify` into a shared method (e.g. on a
`MatchCreationService` or as a package-private method both
`MatchmakingService` and the new `ChallengeService` can call) so random
matchmaking and accepted challenges both go through the same,
already-tested match-creation path.

### 4. Challenge expiry (reuse existing pattern)
Add pending challenges older than 5 minutes to the same kind of scheduled
cleanup as match expiry — a challenge left unanswered for 5 minutes
should auto-transition to `EXPIRED` (atomic, same guard pattern:
`WHERE status = 'PENDING'`). No need for a new scheduler class if you can
reasonably add this to the existing `MatchExpiryService` — your call,
note which you chose.

## What NOT to do
- Do not touch or duplicate the atomic winner-update or match-expiry
  logic already built — those stay exactly as-is.
- Do not remove or change random matchmaking — this is purely additive.
- Do not allow accepting/declining a challenge that isn't yours.

## After generation, I will manually verify
- That the match-creation logic is genuinely shared/reused, not
  duplicated with subtly different behavior.
- That accepting an already-accepted/declined/expired challenge
  correctly returns 409, not a duplicate match.
- That only the challenged user can accept/decline (403 for anyone else,
  including the challenger).
