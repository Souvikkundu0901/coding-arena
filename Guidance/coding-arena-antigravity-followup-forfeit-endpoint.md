# Coding Arena — Antigravity Follow-up: Match Forfeit Endpoint (Anti-Cheat)

## Context
Adding a forfeit mechanism so the frontend can declare a player's loss
when they violate an anti-cheat rule (leaving/switching away from the
match tab twice). The frontend handles detecting tab-switches and
showing the warning; this prompt covers only the backend endpoint that
records the forfeit and declares the opponent the winner.

## Requirement

`POST /api/matches/{matchId}/forfeit` (authenticated, must be one of the
two players in the match).

Behavior:
1. Validate the match exists (404 if not) and the caller is a player in
   it (403 if not).
2. Reject with 409 if the match is already `COMPLETED` or `EXPIRED`.
3. The forfeiting player is whoever calls this endpoint — they lose. The
   other player wins.
4. Atomic guard, same pattern used everywhere else in this codebase
   (winner update, match expiry, challenge accept):
   ```sql
   UPDATE matches SET status = 'COMPLETED', winner_id = :opponentId,
   ended_at = now() WHERE id = :matchId AND status = 'IN_PROGRESS'
   AND winner_id IS NULL
   ```
   Only proceed with Elo/win-loss updates and the broadcast if this
   update actually affected a row (handles the edge case where the
   opponent submitted a correct solution in the same instant — a
   genuine win should never be overwritten by a forfeit that arrives a
   moment later).
5. If the atomic update succeeded: call the same `EloService.updateRatings`
   used for normal wins (winner = opponent, loser = the forfeiting
   player), which already handles win/loss increments and rating deltas.
6. Broadcast `MATCH_END` on `/topic/match/{matchId}` — reuse the existing
   `MatchEvent`/`MatchDto` shape used by the normal win and expiry paths,
   so the frontend doesn't need special-case handling. Optionally add a
   `reason: "FORFEIT"` field to the event data if that's a small,
   non-breaking addition — otherwise the existing shape (winnerId set,
   status COMPLETED) is sufficient signal on its own.
7. If the atomic update did NOT succeed (opponent already won or match
   already ended some other way), return a clear response indicating the
   match was already over — do not throw an error, this is a legitimate
   race outcome, just report the match's actual current state.

## What NOT to do
- Do not add any tab-detection or "violation counting" logic in the
  backend — that's entirely a frontend concern. This endpoint's only job
  is: "the calling player forfeits, right now, unconditionally" — the
  decision of *when* to call it (after a 2nd tab-switch) lives in the
  frontend.
- Do not duplicate the Elo/win-loss logic — reuse `EloService.updateRatings`
  exactly as the existing submission-based win path does.
- Do not change any existing atomic logic elsewhere (winner update on
  ACCEPTED verdict, match expiry) — this is a new, independent code path
  that happens to follow the same proven pattern.

## After generation, I will manually verify
- A normal forfeit call correctly declares the opponent the winner,
  updates ratings, and broadcasts MATCH_END.
- Calling forfeit on an already-completed match returns a clear
  "already over" response, not an error or a double win.
- A non-player calling this endpoint gets 403.
