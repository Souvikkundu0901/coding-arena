# Codess - Backend Microservices & Database Schema

This repository contains the backend microservices for **Codess**:
- **Auth Service**: Registration, Login, JWT verification, Profile management.
- **Matchmaking Service**: Redis-backed queue with atomic Lua pairing logic.
- **Match Service**: Match retrieval, STOMP WebSocket event broadcasting (`MATCH_FOUND`, `MATCH_START`, `MATCH_END`), and atomic winner update queries.

---

## 🛠️ Stack & Technologies
- **Java 17** / **Spring Boot 3.2**
- **Spring Security** + **BCrypt** Password Hashing
- **JSON Web Tokens (JWT)** (jjwt 0.12.5) with 24-hour expiration
- **Redis** for real-time matchmaking queue with atomic Lua script pairing
- **WebSocket (STOMP)** for real-time match events with subscription authorization
- **PostgreSQL** Database with **Flyway** Migrations
- **Maven** Build Tool

---

## 🗄️ Database Schema Migrations

The database migration script `V1__initial_schema.sql` located at `src/main/resources/db/migration/` automatically sets up all 5 core tables when the application starts:

1. **`users`**: User profiles, password hashes (BCrypt), ratings, and timestamps.
2. **`problems`**: Competitive coding problem catalog.
3. **`test_cases`**: Test case inputs and expected outputs.
4. **`matches`**: Real-time 1v1 match records.
   > **Note:** `matches.winner_id` must only ever be set using atomic conditional updates (`UPDATE matches SET winner_id = ?, status = 'COMPLETED', ended_at = now() WHERE id = ? AND winner_id IS NULL`) to avoid race conditions.
5. **`submissions`**: Code submissions and Judge0 execution verdicts.

---

## 🚀 Running Locally

### 1. Prerequisites
- JDK 17+ installed
- PostgreSQL database instance running (or Supabase / Neon free-tier credentials)
- Redis server instance running (port 6379)

### 2. Environment Variables
Set the following environment variables (or rely on defaults in `application.yml`):

| Environment Variable | Description | Default |
| :--- | :--- | :--- |
| `DB_HOST` | PostgreSQL Hostname | `localhost` |
| `DB_PORT` | PostgreSQL Port | `5432` |
| `DB_NAME` | Database Name | `coding_arena` |
| `DB_USERNAME` | Database Username | `postgres` |
| `DB_PASSWORD` | Database Password | `postgres` |
| `REDIS_HOST` | Redis Hostname | `localhost` |
| `REDIS_PORT` | Redis Port | `6379` |
| `JWT_SECRET` | Secret key for JWT signing (256+ bit Base64 string) | *(Development fallback provided)* |
| `PORT` | Application server port | `8080` |

### 3. Run Migrations & Start Service

Build and run tests:
```bash
mvn clean test
```

Start the Spring Boot application:
```bash
mvn spring-boot:run
```

---

## 📡 API Endpoints

### 🔑 Auth Service
- `POST /api/auth/register` — Register new user (normalizes email to lowercase).
- `POST /api/auth/login` — Login with email + password (returns 24h JWT token).
- `GET /api/auth/me` — Get current user profile from JWT token.

### ⚔️ Matchmaking & Challenge Service
- `POST /api/matchmaking/queue` — Add authenticated user to Redis matchmaking queue (sorted by rating).
- `DELETE /api/matchmaking/queue` — Remove authenticated user from matchmaking queue.
- `POST /api/challenges` — Challenge a player directly by username (`{ "username": "opponent" }`). Broadcasts `CHALLENGE_RECEIVED` event to opponent.
- `POST /api/challenges/{challengeId}/accept` — Accept incoming challenge (creates match and broadcasts `MATCH_FOUND`).
- `POST /api/challenges/{challengeId}/decline` — Decline incoming challenge (broadcasts `CHALLENGE_DECLINED` to challenger).
- `GET /api/challenges/pending` — Fetch pending incoming challenges for the authenticated user.

### 👤 User Service
- `GET /api/users/me/matches?limit={n}` — Fetch recent completed/expired matches for current user (`id`, `opponentUsername`, `result`, `ratingDelta`).

### 📚 Problem Service
- `GET /api/problems` — Browse all problems (`id`, `title`, `difficulty`, `tags: []`).
- `GET /api/problems/{problemId}` — Fetch problem detail with description and sample `examples: [{ input, output }]`.

### 💻 Submission Service
- `POST /api/matches/{matchId}/submissions` — Submit code for judging (restricted to match players, returns verdict, updates winner atomically, rejects `COMPLETED` and `EXPIRED` matches with 409).
- `GET /api/matches/{matchId}/submissions` — Fetch submission history for a match.

---

## ⏱️ Match Time Limit & Automatic Expiry

- **Configurable Duration**: `MATCH_DURATION_MINUTES` environment variable (default: `15` minutes).
- **Match Expiry Scheduler**: [`MatchExpiryService`](file:///c:/Users/Souvik/OneDrive/Desktop/Coding%20Arena/src/main/java/com/codingarena/match/service/MatchExpiryService.java) runs every 10 seconds, identifying matches with `expires_at < now()` and `status = 'IN_PROGRESS'`.
- **Atomic Expiry Update**: Updates `status = 'EXPIRED'` and `ended_at = now()` atomically with `winner_id IS NULL` check so winning submissions are never overwritten.
- **WebSocket Notification**: Broadcasts `MATCH_END` with `status: "EXPIRED"` and `winnerId: null` on `/topic/match/{matchId}`.
- **No Rating Changes**: Expired matches do not alter player Elo ratings.

---

## 🛑 In-Memory Rate Limiting (Bucket4j)

Lightweight in-memory token bucket rate limiting is applied to key endpoints:
- `POST /api/auth/login`: **5 req/min** per client IP.
- `POST /api/auth/register`: **3 req/min** per client IP.
- `POST /api/matchmaking/queue`: **10 req/min** per authenticated user ID.
- `POST /api/matches/{matchId}/submissions`: **10 req/min** per authenticated user ID.
- **Limit Exceeded Response**: Returns HTTP `429 Too Many Requests` with body `{ "error": "Rate limit exceeded, try again later" }`.
- **Refill Policy**: Continuous greedy token refill over a 1-minute sliding window.

---

## ⚡ WebSocket (STOMP) Integration & Flow

### Connection Endpoint
- **URL**: `/ws`

### Subscription Channels & Workflow
1. **Per-User Notification Channel (`/topic/user/{userId}`)**:
   - Every player subscribes to `/topic/user/{userId}` upon connecting.
   - Authorized via JWT interceptor (`WebSocketSecurityInterceptor`) ensuring players can only subscribe to their own personal user topic.
   - When 2 players are paired, a `MATCH_FOUND` event is sent to each player's per-user channel containing the newly created `matchId`, opponent profile, and problem details.

2. **Match Event Channel (`/topic/match/{matchId}`)**:
   - Once a player receives `MATCH_FOUND` on their per-user channel, the client subscribes to `/topic/match/{matchId}`.
   - Authorized via `WebSocketSecurityInterceptor` checking JWT identity against the match's `player_a_id` and `player_b_id`.
   - **Events**:
     - `MATCH_FOUND`: Initial match creation event.
     - `MATCH_START`: Match initialization event.
     - `MATCH_END`: Sent when match is completed.
