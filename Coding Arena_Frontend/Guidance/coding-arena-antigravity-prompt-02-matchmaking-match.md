# Coding Arena — Antigravity Prompt 2: Matchmaking + Match Service

## Project context
Continuing the Coding Arena project. Auth Service and full DB schema already
exist (from Prompt 1) — `users`, `problems`, `test_cases`, `matches`,
`submissions` tables are live, JWT auth is working.

**Scope for this prompt: Matchmaking Service + Match Service only.**
Do not build Submission or Rating logic yet — matches will be created and
players will be paired, but code submission/judging comes in Prompt 3.

## Fix required from Prompt 1 first
In `AuthService.java`, normalize email to lowercase (`.toLowerCase().trim()`)
before every save and every lookup (register and login paths), so
`Test@x.com` and `test@x.com` are treated as the same account. Apply this fix
before starting new work.

## What Matchmaking Service does
1. `POST /api/matchmaking/queue` — authenticated user joins the matchmaking
   queue. Store queue entries in Redis (a sorted set or list), not Postgres —
   this is exactly what Redis is for here.
2. `DELETE /api/matchmaking/queue` — user leaves the queue (e.g. cancelled
   search).
3. Matching logic: when 2+ users are in the queue, pair the two with the
   closest `rating` values. Run this as a scheduled check (e.g. every 2-3
   seconds) or trigger it whenever someone joins — either approach is fine,
   but must handle the case of only 1 user in queue (no match yet, keep
   waiting).
4. When two users are paired:
   - Remove both from the Redis queue **atomically** — use a Redis
     transaction (`MULTI`/`EXEC`) or a Lua script so two scheduler runs can't
     both grab the same user and create two matches for them.
   - Pick a random problem from the `problems` table.
   - Create a row in `matches` with `status = 'IN_PROGRESS'`,
     `started_at = now()`.
   - Notify both users via WebSocket (`MATCH_FOUND` event) with the match id,
     opponent username, and problem details.

## What Match Service does
1. `GET /api/matches/{id}` — return match details (players, problem, status,
   timer info). Only accessible to the two players in that match.
2. WebSocket events to broadcast for a given match:
   - `MATCH_FOUND` — sent once, when the match is created (from Matchmaking).
   - `MATCH_START` — sent once both players' clients have acknowledged
     `MATCH_FOUND` (or immediately after creation if you want to keep it
     simpler — your call, note which you chose).
   - `MATCH_END` — sent when a match is completed (this will actually be
     triggered by the Submission Service in Prompt 3, but the event
     structure and broadcast mechanism should exist now).
3. `matches.winner_id` must only ever be set via an atomic conditional
   update: `UPDATE matches SET winner_id = ?, status = 'COMPLETED',
   ended_at = now() WHERE id = ? AND winner_id IS NULL`. Never
   check-then-set from application code. (This will actually be called by
   Submission Service in Prompt 3, but the repository method for it should
   be built now as part of Match Service.)

## WebSocket setup
- Use STOMP over WebSocket, consistent with the original system design.
- Each match gets its own topic, e.g. `/topic/match/{matchId}`.
- Only the two players in a match should be able to subscribe/receive
  events for that match — don't broadcast match events globally.

## What I want you to generate
- `MatchmakingController`, `MatchmakingService` (Redis queue + pairing logic)
- `MatchController`, `MatchService`, `MatchRepository`
- WebSocket config (STOMP) if not already set up in Prompt 1
- The atomic Redis pairing logic and the atomic Postgres winner-update query,
  both implemented exactly as described above — flag if you implement either
  differently and explain why

## What NOT to do
- Do not build code submission, judging, or Elo/rating calculation — that's
  Prompt 3.
- Do not use a simple Postgres row lock or application-level check-then-set
  for either the queue pairing or the winner update — both must be atomic at
  the data-layer level as specified.
- Do not broadcast match WebSocket events to all connected users.

## After generation, I will manually review
- The Redis atomic pairing logic (this is the highest-risk part of this
  prompt — most likely place for a race condition to sneak in)
- The atomic winner-update query
- WebSocket subscription authorization (make sure a random user can't
  subscribe to someone else's match topic)
