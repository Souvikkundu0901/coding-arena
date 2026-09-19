# Coding Arena — Antigravity Prompt 1: Auth Service + Core Schema

## Project context
Building "Coding Arena" — a real-time 1v1 competitive coding platform (chess.com
for coding). Two students, college project, zero budget. All free-tier hosting.

**Stack:** Java 17, Spring Boot 3, PostgreSQL, Redis, WebSocket (STOMP), JWT auth,
React + Vite + TypeScript frontend, self-hosted Judge0 for code execution.

**Scope for this prompt: Auth Service only + the full shared DB schema.**
Do not build Matchmaking, Match, Submission, or Rating services yet — those come
in later prompts. Getting the schema right now matters because every later
service depends on it.

## Database schema (create as Flyway/SQL migration)

```sql
users
  id UUID PK
  username VARCHAR UNIQUE NOT NULL
  email VARCHAR UNIQUE NOT NULL
  password_hash VARCHAR NOT NULL
  rating INT DEFAULT 1200
  created_at TIMESTAMP DEFAULT now()

problems
  id UUID PK
  title VARCHAR NOT NULL
  difficulty VARCHAR NOT NULL   -- EASY / MEDIUM / HARD
  description TEXT NOT NULL
  created_at TIMESTAMP DEFAULT now()

test_cases
  id UUID PK
  problem_id UUID FK -> problems.id
  input TEXT NOT NULL
  expected_output TEXT NOT NULL
  is_sample BOOLEAN DEFAULT false

matches
  id UUID PK
  player_a_id UUID FK -> users.id
  player_b_id UUID FK -> users.id
  problem_id UUID FK -> problems.id
  winner_id UUID FK -> users.id NULLABLE
  status VARCHAR NOT NULL        -- WAITING / IN_PROGRESS / COMPLETED
  started_at TIMESTAMP
  ended_at TIMESTAMP

submissions
  id UUID PK
  match_id UUID FK -> matches.id
  user_id UUID FK -> users.id
  code TEXT NOT NULL
  language VARCHAR NOT NULL
  verdict VARCHAR NOT NULL       -- ACCEPTED / WRONG_ANSWER / TLE / RE / PENDING
  submitted_at TIMESTAMP DEFAULT now()
```

Important: `matches.winner_id` must only ever be set through an atomic
conditional update (`UPDATE matches SET winner_id = ? WHERE id = ? AND
winner_id IS NULL`), never a check-then-set from application code. This
prevents two near-simultaneous correct submissions from both being declared
the winner.

## Auth Service requirements

1. `POST /api/auth/register` — username, email, password. Hash password with
   BCrypt. Return 409 if username/email already exists.
2. `POST /api/auth/login` — email + password. Return JWT access token
   (expiry: 24h) on success, 401 on bad credentials.
3. `GET /api/auth/me` — return current user profile from a valid JWT.
4. JWT filter/interceptor that validates the token on protected routes and
   rejects expired/invalid tokens with 401.
5. Standard error response shape: `{ "error": "message" }` for all 4xx/5xx.

## What I want you to generate
- Spring Boot project structure (entities, repositories, services,
  controllers) for `users` table and the Auth Service endpoints above.
- Flyway migration file for the full schema above (all 5 tables), even
  though only `users` is used by this service — the other tables need to
  exist for later services.
- application.yml with placeholders for Postgres connection (I'll fill in
  Supabase/Neon free-tier credentials myself — do not hardcode secrets).
- A short README section explaining how to run migrations and start the
  service locally.

## What NOT to do
- Do not build Matchmaking, Match, Submission, or Rating logic yet.
- Do not use plain-text or reversible password storage — BCrypt only.
- Do not invent extra tables or fields beyond the schema above without
  flagging it to me first.

## After generation, I will manually review
- Password hashing correctness
- JWT expiry handling
- That the migration matches the schema exactly
