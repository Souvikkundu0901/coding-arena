# Coding Arena — Antigravity Prompt 3: Submission Service + Judge0 + Rating

## Project context
Continuing Coding Arena. Auth Service, full DB schema, Matchmaking Service,
and Match Service all exist and are verified working (WebSocket auth,
atomic Redis pairing, and the atomic winner-update repository method are
already built and tested).

**Scope for this prompt: Submission Service + Judge0 integration + Elo
rating update.** This is the last core backend piece — after this, a full
match (queue → pair → submit code → judge → winner → rating update) works
end-to-end.

## Judge0 setup (self-hosted, free)
Assume Judge0 CE is running locally via Docker on `http://localhost:2358`
(standard Judge0 default port). If it's not running yet, note in your
response that I need to start it separately with:
```
docker run -d -p 2358:2358 judge0/judge0:1.13.0
```
Do not attempt to install/run Docker yourself — just build the service to
call that URL, configurable via an env var (`JUDGE0_URL`, default
`http://localhost:2358`).

## What Submission Service does

1. `POST /api/matches/{matchId}/submissions` — authenticated user submits
   code for a match they're part of.
   - Request body: `{ "code": "...", "language": "..." }`
   - Reject with 403 if the user isn't `playerA` or `playerB` for that match.
   - Reject with 409 if the match is already `COMPLETED`.
   - Save a `submissions` row immediately with `verdict = 'PENDING'`.

2. Send the code + test cases (fetch from `test_cases` table for the
   match's `problem_id`) to Judge0 for execution:
   - Only run against test cases where `is_sample = true` OR run against
     all test cases — your call, note which you chose and why (running all
     is more correct for a "did they actually solve it" check; sample-only
     is faster/simpler for a college project demo).
   - Enforce a time limit and memory limit on the Judge0 submission request
     (Judge0 supports `cpu_time_limit` and `memory_limit` parameters) —
     don't let a submission run unbounded.

3. Determine verdict by comparing Judge0's actual output against
   `expected_output` for each test case:
   - All pass → `ACCEPTED`
   - Any mismatch → `WRONG_ANSWER`
   - Judge0 reports timeout → `TLE`
   - Judge0 reports runtime error → `RE`
   - Update the `submissions` row with the final verdict.

4. If verdict is `ACCEPTED` and the match isn't already completed:
   - Call the atomic winner-update method already built in
     `MatchRepository` (`UPDATE matches SET winner_id = ?, status =
     'COMPLETED', ended_at = now() WHERE id = ? AND winner_id IS NULL`).
   - Only if that update actually affected a row (i.e. this submission
     really was first) — proceed to step 5. If it affected zero rows,
     someone else already won; don't update ratings twice.
   - Broadcast `MATCH_END` on `/topic/match/{matchId}` with the winner's
     id and username.

5. Update Elo ratings for both players:
   - Standard Elo formula, K-factor = 32.
   - Winner's expected score: `1 / (1 + 10^((loserRating - winnerRating) / 400))`
   - `newWinnerRating = winnerRating + K * (1 - expectedWinnerScore)`
   - `newLoserRating = loserRating + K * (0 - expectedLoserScore)`
   - Update both users' `rating` column in a single transaction alongside
     the match completion.

6. `GET /api/matches/{matchId}/submissions` — return all submissions for a
   match (both players' attempts), restricted to the two players in that
   match (reuse the same authorization pattern as Match Service).

## What I want you to generate
- `SubmissionController`, `SubmissionService`, `SubmissionRepository`
- `Judge0Client` — a simple REST client wrapping calls to Judge0's
  `/submissions` endpoint (create + poll for result, since Judge0 execution
  is asynchronous by default — use their synchronous `?wait=true` param to
  keep this simple for now)
- `EloService` (or a method within `SubmissionService`) implementing the
  rating calculation above
- Wire `MATCH_END` broadcasting into the existing WebSocket setup from
  Prompt 2

## What NOT to do
- Do not let two submissions both be declared the winner — the atomic
  update + "did it affect a row" check is mandatory, not optional.
- Do not update ratings before confirming the atomic winner-update
  actually succeeded for this submission.
- Do not skip the CPU/memory/time limit parameters on the Judge0 request —
  unbounded user code execution is a real risk even for a college project
  demo.
- Do not build a frontend code editor — that's a separate, later task.

## After generation, I will manually review
- The atomic winner-update usage — specifically that rating updates are
  gated behind "did this update actually affect a row"
- The Judge0 client's timeout/memory limit parameters
- The Elo calculation correctness (spot-check the math against a known
  example)
