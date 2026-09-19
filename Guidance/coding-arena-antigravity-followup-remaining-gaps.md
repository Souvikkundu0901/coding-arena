# Coding Arena — Antigravity Follow-up: Remaining Feature Gaps

## Context
Closing out the last deferred items from the frontend audit: profile
management, standalone practice submissions, and the full friends
system. Everything else (auth, matchmaking, matches, submissions,
challenges, rate limiting, match expiry, Elo, win/loss tracking, match
history, problems browsing) is already built and verified — do not
touch any of that.

This is a larger prompt covering three feature areas. If it's easier to
review/verify in pieces, let me know and we can split it — otherwise
proceed with all three.

---

## Part 1 — Profile management

### Endpoints
- `PATCH /api/users/me` (authenticated) — body: `{ "username":
  "optional new username" }`. Update username only for now (avatar seed
  can be added later if the frontend needs it — flag if so). Reject 409
  if the new username is already taken by someone else.
- `PATCH /api/users/me/email` — body: `{ "newEmail": "...", "password":
  "current password for confirmation" }`. Verify password matches
  before changing email (BCrypt check, same pattern as login). Reject
  409 if the new email is already registered. Apply the same
  `.toLowerCase().trim()` normalization used elsewhere for email.
- `PATCH /api/users/me/password` — body: `{ "currentPassword": "...",
  "newPassword": "..." }`. Verify current password matches before
  updating (BCrypt), hash the new password the same way registration
  does.
- `GET /api/users/me/preferences` — return a simple preferences object.
  Since no preferences schema exists yet, create a minimal one: a new
  `user_preferences` table with `user_id` (PK, FK to users), `theme`
  VARCHAR DEFAULT 'dark', `email_notifications` BOOLEAN DEFAULT true.
  Auto-create a row with defaults on first access if none exists.
- `PATCH /api/users/me/preferences` — update `theme` and/or
  `email_notifications`.
- `DELETE /api/users/me` (authenticated) — requires password
  confirmation in the request body (`{ "password": "..." }`) for
  safety. Soft-delete preferred over hard delete: add an `is_deleted
  BOOLEAN DEFAULT false` column to `users`, set it true rather than
  actually removing the row (preserves match history integrity — other
  players' match records shouldn't break if an opponent deletes their
  account). Deleted users should no longer be able to log in (check
  `is_deleted` in the login flow) and should not appear in
  matchmaking/challenges.

## Part 2 — Standalone practice submissions

- `POST /api/problems/{problemId}/submissions` (authenticated) — same
  request shape as match submissions (`{ "code", "language" }`), but
  NOT tied to a match. Runs the same judging pipeline
  (`Judge0Client`/mock mode, same verdict mapping) against that
  problem's test cases, but:
  - Does NOT create or require a `matches` row.
  - Does NOT trigger any winner declaration, Elo update, or win/loss
    change — this is solo practice, no opponent, no rating stakes.
  - Store the submission with `match_id = NULL` (the `submissions`
    table's `match_id` FK needs to become nullable — new migration).
  - Return the verdict the same way match submissions do.
- `GET /api/problems/{problemId}/submissions/me` (authenticated,
  optional but useful) — return the current user's past practice
  submissions for that problem, most recent first.

## Part 3 — Friends system

### New table
```sql
CREATE TABLE friendships (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  requester_id UUID NOT NULL REFERENCES users(id),
  addressee_id UUID NOT NULL REFERENCES users(id),
  status VARCHAR NOT NULL DEFAULT 'PENDING', -- PENDING / ACCEPTED / DECLINED
  created_at TIMESTAMP DEFAULT now(),
  responded_at TIMESTAMP
);
```

### Endpoints
- `POST /api/friends/requests` — body `{ "username": "..." }`. Same
  validation pattern as challenges: 404 if user not found, 400 if
  self-request, 409 if a pending or already-accepted friendship exists
  between these two users in either direction.
- `GET /api/friends/requests/pending` — incoming pending requests for
  the current user (where they're the `addressee`).
- `POST /api/friends/requests/{id}/accept` — only the addressee may
  accept. Atomic guard: `WHERE id = ? AND status = 'PENDING'`, same
  pattern used everywhere else in this codebase.
- `POST /api/friends/requests/{id}/decline` — same pattern, sets
  `DECLINED`.
- `GET /api/friends` — list of accepted friends for the current user
  (friendships where they're either requester or addressee and status
  is `ACCEPTED`), returned as a flat list of `{ id, username, rating }`
  for the *other* person in each friendship.

### Relationship to existing challenge feature
This is separate from `/api/challenges` (direct match invite by
username), which already works and stays as-is. Friends and challenges
are independent — you don't need to be friends to challenge someone.
Do not merge or change the existing challenge feature.

---

## What NOT to do (applies to all three parts)
- Do not touch any already-verified atomic logic (winner update, match
  expiry, challenge accept/decline) — these are separate, new features.
- Do not hard-delete users — soft delete only, as specified.
- Do not let practice submissions affect ratings, win/loss counts, or
  create match rows.
- Do not require friendship to send a challenge, or vice versa — keep
  these features independent.

## After generation, I will manually verify
- Password confirmation is actually required and checked (BCrypt) before
  email/password changes and account deletion — not just accepted
  blindly.
- Deleted (`is_deleted = true`) users cannot log in and don't appear in
  matchmaking.
- Practice submissions genuinely don't touch `matches`, ratings, or
  win/loss counts.
- Friend request accept/decline atomic guards work the same way
  challenges do (test double-accept returns 409, wrong-user accept
  returns 403).
