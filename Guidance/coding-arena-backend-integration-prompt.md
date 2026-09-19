# Coding Arena — Frontend Integration Specification
### (Send this file to your backend developer along with your backend code)

---

## WHO YOU ARE / WHAT YOU ARE READING

You are a backend developer receiving this document from a frontend developer.
This file describes the **complete Coding Arena frontend** — every page, every
HTML element ID, every API call, every WebSocket event, and every data field
the frontend reads. Your job is to audit your backend code against this spec
and confirm: which endpoints exist, which are missing, and whether your response
shapes match exactly what the frontend expects. Do not guess — check line by
line.

---

## TECH STACK (FRONTEND)

- **Language:** Vanilla JavaScript (ES2020+), no framework
- **UI:** HTML5 + Bootstrap 5.3 + Bootstrap Icons 1.11
- **Code Editor:** CodeMirror 5 (CDN)
- **WebSocket:** `@stomp/stompjs` + `sockjs-client`
- **Avatar:** DiceBear API (`https://api.dicebear.com/7.x/bottts/svg?seed=<seed>`)
- **Auth storage:** `localStorage` — keys listed below
- **Base URL:** `http://localhost:8080`
- **API prefix:** `/api` — all REST calls go to `http://localhost:8080/api/*`
- **WebSocket endpoint:** `http://localhost:8080/ws` (SockJS)

---

## PAGES & FILES

| Page | HTML File | JS File | Purpose |
|---|---|---|---|
| Login | `login.html` | `auth.js` | User login |
| Register | `register.html` | `auth.js` | User registration |
| Dashboard | `index.html` | `dashboard.js`, `friends.js` | Main hub — stats, matches, problems, friends |
| Queue | `queue.html` | `queue.js` | Matchmaking waiting room |
| Match | `match.html` | `match.js` | Live 1v1 coding match |
| Practice List | `practice.html` | `practice.js` | Browse practice problems |
| Practice Solve | `practice-solve.html` | `practice-solve.js` | Solve a single practice problem |
| Profile | `profile.html` | `profile.js` | View/edit user profile |
| Settings | `settings.html` | `settings.js` | Account/password/preferences |

All API functions live in `api.js` and are exposed globally as `window.CodingArenaAPI`.
`MOCK_MODE = true` is currently set — flip it to `false` to hit the real backend.

---

## LOCALSTORAGE KEYS

The frontend reads and writes these keys. Your backend does not touch
localStorage, but you need to know the shape of data stored so you
understand what the frontend caches:

| Key | What is stored |
|---|---|
| `ca_token` | JWT string — set after login/register, read on every authenticated request |
| `ca_user` | Full user JSON object — cached after login and after `GET /api/auth/me` |
| `ca_preferences` | User preferences JSON object (mock only — real data from `/api/users/me/preferences`) |
| `ca_mock_friend_requests` | Mock only — not needed when backend is live |
| `ca_mock_challenges` | Mock only — not needed when backend is live |

---

## ALL REST API ENDPOINTS CALLED BY THE FRONTEND

### AUTH

#### POST /api/auth/register
- **Auth required:** No
- **Request body:**
```json
{ "username": "string", "email": "string", "password": "string" }
```
- **Expected response:**
```json
{
  "token": "string (JWT)",
  "user": {
    "id": "number",
    "username": "string",
    "email": "string",
    "rating": "number",
    "wins": "number",
    "losses": "number",
    "createdAt": "ISO date string"
  }
}
```
- **On success:** token saved to `ca_token`, user saved to `ca_user`, redirect to `index.html`
- **On error:** display `error.message` in `#registerError` element
- **Error shape:** `{ "error": "message string" }`

---

#### POST /api/auth/login
- **Auth required:** No
- **Request body:**
```json
{ "email": "string", "password": "string" }
```
- **Expected response:** same shape as `/api/auth/register`
- **On success:** token saved to `ca_token`, user saved to `ca_user`, redirect to `index.html`
- **On error:** display `error.message` in `#loginError` element

---

#### GET /api/auth/me
- **Auth required:** Yes (`Authorization: Bearer <token>`)
- **Expected response:** user object (same shape as register response's `user` field)
- **Used by:** `dashboard.js`, `profile.js`, `settings.js` — called on every page load to get fresh user data
- **On 401/error:** `clearToken()` called, redirect to `login.html`

---

### MATCHMAKING

#### POST /api/matchmaking/queue
- **Auth required:** Yes
- **Request body:** none
- **Expected response:**
```json
{ "message": "Joined matchmaking queue successfully" }
```
- **Frontend action:** starts queue timer, connects WebSocket, waits for `MATCH_FOUND` event

---

#### DELETE /api/matchmaking/queue
- **Auth required:** Yes
- **Request body:** none
- **Expected response:**
```json
{ "message": "Left queue" }
```
- **Frontend action:** disconnect WebSocket, redirect to `index.html`

---

### MATCHES

#### GET /api/matches/{matchId}
- **Auth required:** Yes
- **URL param:** `matchId` from URL query string `match.html?matchId=X`
- **Expected response:**
```json
{
  "id": "number",
  "status": "string (IN_PROGRESS | FINISHED)",
  "players": [
    { "id": "number", "username": "string", "rating": "number" },
    { "id": "number", "username": "string", "rating": "number" }
  ],
  "problem": {
    "title": "string",
    "difficulty": "EASY | MEDIUM | HARD",
    "description": "string",
    "examples": [
      { "input": "string", "output": "string" }
    ]
  },
  "createdAt": "ISO date string"
}
```
- **CRITICAL:** `players` array must contain both players. Frontend finds the
  opponent by comparing each `player.id` against `currentUser.id` (stored in
  `ca_user`). The player whose ID does NOT match is rendered as the opponent.
- **On error:** show error in `#problemDescription`, redirect to `index.html` after 1.5s

---

#### POST /api/matches/{matchId}/submissions
- **Auth required:** Yes
- **Request body:**
```json
{
  "code": "string",
  "language": "python | java | cpp | c | javascript | typescript"
}
```
- **Expected response:**
```json
{
  "verdict": "ACCEPTED | WRONG_ANSWER | TLE | RE | PENDING",
  "language": "string",
  "submittedAt": "ISO date string"
}
```
- **OR** wrapped shape (both supported by frontend):
```json
{
  "submission": {
    "verdict": "string",
    "language": "string",
    "submittedAt": "ISO date string"
  }
}
```
- **On success:** verdict displayed in `#verdictDisplay`, submission prepended to `#submissionListItems`
- **On empty code:** frontend blocks submission before calling API

---

#### GET /api/matches/{matchId}/submissions
- **Auth required:** Yes
- **Expected response:**
```json
[
  {
    "id": "number",
    "matchId": "number",
    "language": "string",
    "verdict": "ACCEPTED | WRONG_ANSWER | TLE | RE | PENDING",
    "submittedAt": "ISO date string"
  }
]
```
- **OR** wrapped: `{ "submissions": [ ... ] }` — frontend handles both
- **Used by:** `match.js` on page load to populate submission history panel

---

### USERS / PROFILE

#### GET /api/users/me/matches?limit={n}
- **Auth required:** Yes
- **Query param:** `limit` (integer, default 4)
- **Expected response:**
```json
[
  {
    "id": "number",
    "opponentUsername": "string",
    "result": "WIN | LOSS",
    "ratingDelta": "number (positive or negative)"
  }
]
```
- **Used by:** `dashboard.js` (limit 4), `profile.js` (limit 6) to render recent matches list

---

#### PATCH /api/users/me
- **Auth required:** Yes
- **Request body:**
```json
{ "username": "string", "avatarSeed": "string" }
```
- **Expected response:** updated user object (same shape as `GET /api/auth/me`)
- **Used by:** `profile.js` — Edit Profile modal save button

---

#### PATCH /api/users/me/email
- **Auth required:** Yes
- **Request body:** `{ "email": "string" }`
- **Expected response:** `{ "message": "string", "email": "string" }`
- **Used by:** `settings.js`

---

#### PATCH /api/users/me/password
- **Auth required:** Yes
- **Request body:** `{ "currentPassword": "string", "newPassword": "string" }`
- **Expected response:** `{ "message": "string" }`
- **Used by:** `settings.js`

---

#### GET /api/users/me/preferences
- **Auth required:** Yes
- **Expected response:**
```json
{
  "defaultLanguage": "string (optional)",
  "darkTheme": "boolean (optional)",
  "notifications": "boolean (optional)"
}
```

---

#### PATCH /api/users/me/preferences
- **Auth required:** Yes
- **Request body:** any subset of the preferences object above
- **Expected response:** full updated preferences object
- **Used by:** `settings.js` — fires on every toggle/select change

---

#### DELETE /api/users/me
- **Auth required:** Yes
- **Expected response:** `{ "message": "string" }`
- **On success:** token cleared, `ca_user` removed, redirect to `login.html`

---

### PRACTICE PROBLEMS

#### GET /api/problems
- **Auth required:** Yes
- **Query params (all optional):**
  - `difficulty=EASY|MEDIUM|HARD`
  - `tag=string`
  - `search=string`
  - `limit=number`
- **Expected response:**
```json
[
  {
    "id": "number",
    "title": "string",
    "difficulty": "EASY | MEDIUM | HARD",
    "tags": ["string"],
    "description": "string",
    "examples": [
      { "input": "string", "output": "string" }
    ]
  }
]
```
- **Used by:** `practice.js` (full list with filters), `dashboard.js` (limit 6 for recommended)

---

#### GET /api/problems/{problemId}
- **Auth required:** Yes
- **URL param:** `problemId` from URL query string `practice-solve.html?id=X`
- **Expected response:** single problem object (same shape as array item above)
- **On error:** show error in `#solveProblemTitle` and `#solveProblemDescription`

---

#### POST /api/problems/{problemId}/submissions
- **Auth required:** Yes
- **Request body:**
```json
{ "code": "string", "language": "python | java | cpp | c | javascript" }
```
- **Expected response:**
```json
{
  "verdict": "ACCEPTED | WRONG_ANSWER | TLE | RE | PENDING",
  "problemId": "number",
  "language": "string",
  "submittedAt": "ISO date string"
}
```
- **Verdict display:** `#solveVerdictDisplay` — green for ACCEPTED, red for anything else

---

### FRIENDS SYSTEM

#### POST /api/friends/requests
- **Auth required:** Yes
- **Request body:** `{ "targetUsername": "string" }`
- **Expected response:** `{ "message": "string", "targetUsername": "string" }`
- **On error:** `error.message` shown in `#addFriendStatus`

---

#### GET /api/friends/requests/pending
- **Auth required:** Yes
- **Expected response:**
```json
[
  { "id": "number", "fromUsername": "string", "toUsername": "string", "status": "PENDING" }
]
```
- **Used by:** `friends.js` — renders pending requests list in social modal

---

#### POST /api/friends/requests/{requestId}/accept
- **Auth required:** Yes
- **Expected response:** `{ "message": "string" }`

---

#### POST /api/friends/requests/{requestId}/decline
- **Auth required:** Yes
- **Expected response:** `{ "message": "string" }`

---

#### GET /api/friends
- **Auth required:** Yes
- **Expected response:**
```json
[
  { "username": "string", "rating": "number" }
]
```
- **Used by:** `friends.js` — renders confirmed friends list in social modal

---

#### POST /api/friends/{friendUsername}/challenge
- **Auth required:** Yes
- **URL param:** `friendUsername`
- **Request body:** none
- **Expected response:** `{ "message": "string" }`

---

#### GET /api/friends/challenges/pending
- **Auth required:** Yes
- **Expected response:**
```json
[
  { "id": "number", "fromUsername": "string", "toUsername": "string", "status": "PENDING", "matchId": "number|null" }
]
```
- **Used by:** `friends.js` — polled every 5 seconds via `setInterval`. First challenge
  in the array triggers a popup overlay (`#challengePopupOverlay`)

---

#### POST /api/friends/challenges/{challengeId}/accept
#### POST /api/friends/challenges/{challengeId}/decline
- **Auth required:** Yes
- **On accept response:** `{ "message": "string", "matchId": "number" }`
  → frontend redirects to `match.html?matchId=<matchId>`
- **On decline response:** `{ "message": "string" }`

---

## WEBSOCKET SPECIFICATION

### Connection
- **URL:** `http://localhost:8080/ws` (SockJS transport)
- **Protocol:** STOMP over SockJS
- **Auth:** `Authorization: Bearer <token>` sent in BOTH:
  1. `connectHeaders` on the STOMP `CONNECT` frame
  2. Per-subscription headers on every `SUBSCRIBE` frame

### Topics

#### `/topic/user/{userId}`
- **Subscribed by:** `queue.js` after joining queue
- **Receives:** `MATCH_FOUND` event
- **Message shape:**
```json
{
  "type": "MATCH_FOUND",
  "data": {
    "matchId": "number",
    "id": "number",
    "opponentName": "string",
    "opponent": {
      "username": "string",
      "rating": "number"
    }
  }
}
```
- **CRITICAL — matchId field:** frontend reads `data.matchId` first, then falls
  back to `data.id`. One of these MUST be present. If both are missing, the
  frontend logs an error and does NOT navigate.
- **On receive:** frontend immediately navigates to `match.html?matchId=<matchId>`
  with NO delay.

---

#### `/topic/match/{matchId}`
- **Subscribed by:** `match.js` after loading the match page
- **Receives:** `MATCH_START` and `MATCH_END` events

**MATCH_START shape:**
```json
{
  "type": "MATCH_START",
  "data": {
    "id": "number",
    "status": "IN_PROGRESS",
    "durationSeconds": "number (e.g. 1800 for 30 minutes)"
  }
}
```
- **Frontend action:** feeds `durationSeconds` into the match countdown timer.
  NOTE: the frontend ALSO starts a 5-second pre-match countdown immediately
  on page load (before this event). `MATCH_START` does NOT restart it —
  it only updates the duration if not already set.

**MATCH_END shape:**
```json
{
  "type": "MATCH_END",
  "data": {
    "winnerName": "string",
    "winnerId": "number",
    "winner": { "username": "string" },
    "ratingChanges": {
      "currentUser": "number (delta, e.g. +12 or -8)"
    },
    "ratingChange": "number (fallback field)",
    "ratings": { "<userId>": "number" }
  }
}
```
- **Frontend reads (in priority order):**
  1. `data.winnerName`
  2. `data.winner.username`
  3. Compares `data.winnerId` to `currentUser.id` → shows "You" or "Opponent"
- **Rating delta reads (in priority order):**
  1. `data.ratingChanges.currentUser`
  2. `data.ratingChanges[String(currentUser.id)]`
  3. `data.ratingChange`
- **On receive:** match end modal shown (`#matchEndModal`), editor disabled,
  submit button disabled

---

## HTML ELEMENT IDs — COMPLETE REFERENCE

Below are every DOM element ID the JavaScript reads or writes.
Your backend does not use these directly, but this confirms what data
must be populated for the UI to work.

### login.html
| ID | Purpose |
|---|---|
| `#loginForm` | Form element — submit triggers login |
| `#email` | Email input |
| `#password` | Password input |
| `#loginBtn` | Submit button |
| `#loginError` | Error message display |
| `#togglePassword` | Show/hide password button |

### register.html
| ID | Purpose |
|---|---|
| `#registerForm` | Form element — submit triggers register |
| `#username` | Username input |
| `#email` | Email input |
| `#password` | Password input |
| `#registerBtn` | Submit button |
| `#registerError` | Error message display |
| `#togglePassword` | Show/hide password button |

### index.html (Dashboard)
| ID | Purpose | Populated by |
|---|---|---|
| `#userRatingValue` | Shows user's current rating | `GET /api/auth/me` → `user.rating` |
| `#userWinsCount` | Shows total wins | `GET /api/auth/me` → `user.wins` |
| `#userLossesCount` | Shows total losses | `GET /api/auth/me` → `user.losses` |
| `#userAvatarImg` | Avatar `<img>` tag | DiceBear URL built from `user.avatarSeed || user.username` |
| `#userDisplayName` | Username text | `GET /api/auth/me` → `user.username` |
| `#recentMatchesList` | Recent matches `<ul>` | `GET /api/users/me/matches?limit=4` |
| `#recommendedProblemsScroller` | Problem cards container | `GET /api/problems?limit=6` |
| `#logoutBtn` | Logout button | Clears token, redirects to `login.html` |
| `#addFriendForm` | Add friend form | `POST /api/friends/requests` |
| `#addFriendUsernameInput` | Friend username input | — |
| `#addFriendBtn` | Add friend submit | — |
| `#addFriendStatus` | Status message | Shows success/error text |
| `#pendingRequestsList` | Pending requests `<ul>` | `GET /api/friends/requests/pending` |
| `#friendsList` | Friends `<ul>` | `GET /api/friends` |
| `#challengePopupOverlay` | Challenge received popup | `GET /api/friends/challenges/pending` (polled every 5s) |
| `#challengePopupText` | Popup text | Shows `"{fromUsername} wants to battle you."` |
| `#challengeAcceptBtn` | Accept challenge button | `POST /api/friends/challenges/{id}/accept` |
| `#challengeDeclineBtn` | Decline challenge button | `POST /api/friends/challenges/{id}/decline` |

### queue.html
| ID | Purpose | Populated by |
|---|---|---|
| `#queueStatus` | Status heading text | Updated by JS state |
| `#queueTimer` | Elapsed time display `MM:SS` | JS interval |
| `#queueRating` | Shows user's rating | `ca_user.rating` from localStorage |
| `#leaveQueueBtn` | Leave queue button | `DELETE /api/matchmaking/queue` |
| `#matchFoundBanner` | Match found panel (hidden by default) | Shown on `MATCH_FOUND` WS event |
| `#opponentName` | Opponent username in banner | `MATCH_FOUND` event → `data.opponentName` or `data.opponent.username` |

### match.html
| ID | Purpose | Populated by |
|---|---|---|
| `#problemTitle` | Problem title `<h1>` | `GET /api/matches/{id}` → `match.problem.title` |
| `#problemDescription` | Problem description | `match.problem.description` |
| `#problemDifficulty` | Difficulty badge | `match.problem.difficulty` (CSS class applied) |
| `#problemExamples` | Examples container | `match.problem.examples[]` |
| `#matchTimer` | Countdown timer `MM:SS` | Starts after 5s pre-match countdown |
| `#matchLiveStatus` | "LIVE" chip | Set to "LIVE" on `MATCH_START` |
| `#opponentName` | Opponent name in header chip | From `match.players[]` — player whose `id ≠ currentUser.id` |
| `#opponentStatus` | Opponent status text | `opponent.status` or `"coding..."` |
| `#yourSubmitStatus` | Your submission status | Updated after you submit |
| `#languageSelect` | Language dropdown | Values: `python, java, cpp, c, javascript, typescript` |
| `#codeEditorContainer` | CodeMirror mount point | Initialized by `match.js` |
| `#submitBtn` | Submit code button | `POST /api/matches/{id}/submissions` |
| `#verdictDisplay` | Verdict result text | `submission.verdict` |
| `#submissionListItems` | Submission history list | `GET /api/matches/{id}/submissions` |
| `#matchCountdownOverlay` | Pre-match 5-second overlay | JS countdown |
| `#matchCountdownNumber` | Countdown number `5,4,3...` | JS countdown |
| `#startCodingOverlay` | "Start Coding!" flash | Shown for 1s after countdown |
| `#matchEndModal` | Match end modal overlay | Shown on `MATCH_END` WS event |
| `#matchWinnerText` | Winner display text | `MATCH_END` → `data.winnerName` |
| `#player1RatingChange` | Your rating change | `MATCH_END` → `data.ratingChanges.currentUser` |
| `#player2RatingChange` | Opponent rating change | `MATCH_END` event |
| `#backToDashboardBtn` | Back to dashboard button | Redirects to `index.html` |

### practice.html
| ID | Purpose | Populated by |
|---|---|---|
| `#problemListContainer` | Problems list container | `GET /api/problems` |
| `#searchInput` | Search by title input | Client-side filter |
| `#difficultyFilter` | Difficulty dropdown | Client-side filter |
| `#tagFilter` | Tag dropdown | Auto-populated from problem tags |

### practice-solve.html
| ID | Purpose | Populated by |
|---|---|---|
| `#solveProblemTitle` | Problem title | `GET /api/problems/{id}` → `problem.title` |
| `#solveProblemDifficulty` | Difficulty pill | `problem.difficulty` |
| `#solveProblemTags` | Tags container | `problem.tags[]` |
| `#solveProblemDescription` | Description text | `problem.description` |
| `#solveProblemExamples` | Examples container | `problem.examples[]` |
| `#solveLanguageSelect` | Language dropdown | Values: `python, java, cpp, c, javascript` |
| `#solveCodeInput` | CodeMirror textarea | Initialized by `practice-solve.js` |
| `#solveSubmitBtn` | Submit button | `POST /api/problems/{id}/submissions` |
| `#solveVerdictDisplay` | Verdict display | `result.verdict` |

### profile.html
| ID | Purpose | Populated by |
|---|---|---|
| `#profileAvatar` | Avatar `<img>` | DiceBear from `user.avatarSeed || user.username` |
| `#profileUsername` | Username heading | `GET /api/auth/me` → `user.username` |
| `#userRatingValue` | Rating stat box | `user.rating` |
| `#userWinsValue` | Wins stat box | `user.wins` |
| `#userLossesValue` | Losses stat box | `user.losses` |
| `#userWinRateValue` | Win rate (computed) | `wins / (wins + losses) * 100` |
| `#userJoinDate` | Join date | `user.createdAt` formatted |
| `#recentMatchesList` | Match history `<ul>` | `GET /api/users/me/matches?limit=6` |
| `#headerAvatar` | Header avatar | `user.avatarSeed || user.username` |
| `#headerUsername` | Header username | `user.username` |
| `#editProfileModal` | Edit modal (Bootstrap) | — |
| `#editProfileForm` | Edit form | `PATCH /api/users/me` |
| `#editUsernameInput` | New username input | Prefilled with `user.username` |
| `#editAvatarSeedInput` | New avatar seed input | Prefilled with `user.avatarSeed` |
| `#saveProfileBtn` | Save changes button | Triggers `PATCH /api/users/me` |

### settings.html
| ID | Purpose | Populated by |
|---|---|---|
| `#settingsAvatar` | Header avatar | `user.avatarSeed || user.username` |
| `#settingsUsername` | Header username | `GET /api/auth/me` → `user.username` |
| `#changeEmailForm` | Email change form | `PATCH /api/users/me/email` |
| `#emailInput` | New email input | Placeholder is current email |
| `#changeEmailBtn` | Update email button | — |
| `#changePasswordForm` | Password change form | `PATCH /api/users/me/password` |
| `#currentPasswordInput` | Current password | — |
| `#newPasswordInput` | New password | — |
| `#confirmPasswordInput` | Confirm new password | Validated client-side (must match) |
| `#changePasswordBtn` | Change password button | — |
| `#defaultLanguageSelect` | Language preference | `GET /api/users/me/preferences` → `preferences.defaultLanguage` |
| `#darkThemeToggle` | Dark theme toggle | `preferences.darkTheme` |
| `#notificationToggle` | Notifications toggle | `preferences.notifications` (default: ON) |
| `#logoutBtn` | Logout | Clears token, redirects to `login.html` |
| `#deleteAccountBtn` | Delete account | `DELETE /api/users/me` |

---

## DATA MODELS — EXACT FIELD NAMES REQUIRED

The frontend reads these exact field names. If your backend uses different
naming, the UI will break silently (show "—" or empty).

### User Object
```json
{
  "id": "number — REQUIRED for WebSocket identity matching",
  "username": "string — REQUIRED",
  "email": "string",
  "rating": "number",
  "wins": "number",
  "losses": "number",
  "avatarSeed": "string (optional — falls back to username for DiceBear)",
  "createdAt": "ISO 8601 date string"
}
```

### Match Object (`GET /api/matches/{id}`)
```json
{
  "id": "number",
  "status": "IN_PROGRESS | FINISHED",
  "players": [
    { "id": "number", "username": "string", "rating": "number" }
  ],
  "problem": {
    "title": "string",
    "difficulty": "EASY | MEDIUM | HARD",
    "description": "string",
    "examples": [{ "input": "string", "output": "string" }]
  },
  "createdAt": "ISO 8601 date string"
}
```

### Recent Match Object (`GET /api/users/me/matches`)
```json
{
  "id": "number",
  "opponentUsername": "string",
  "result": "WIN | LOSS",
  "ratingDelta": "number (signed — positive for win, negative for loss)"
}
```

### Problem Object
```json
{
  "id": "number",
  "title": "string",
  "difficulty": "EASY | MEDIUM | HARD",
  "tags": ["string"],
  "description": "string",
  "examples": [{ "input": "string", "output": "string" }]
}
```

### Submission Object
```json
{
  "id": "number",
  "matchId": "number (or problemId for practice)",
  "language": "string",
  "verdict": "ACCEPTED | WRONG_ANSWER | TLE | RE | PENDING",
  "submittedAt": "ISO 8601 date string"
}
```

### Friend Object
```json
{ "username": "string", "rating": "number" }
```

### Friend Request Object
```json
{
  "id": "number",
  "fromUsername": "string",
  "toUsername": "string",
  "status": "PENDING | ACCEPTED | DECLINED"
}
```

### Challenge Object
```json
{
  "id": "number",
  "fromUsername": "string",
  "toUsername": "string",
  "status": "PENDING | ACCEPTED | DECLINED",
  "matchId": "number | null"
}
```

---

## LANGUAGE VALUES SENT TO BACKEND

These are the exact string values sent in the `language` field of
submission requests. Your backend judge must accept all of these:

```
python
java
cpp
c
javascript
typescript
```

---

## ERROR RESPONSE FORMAT

All errors must follow this shape:
```json
{ "error": "human-readable message string" }
```
The frontend reads `error.message` (which maps to the thrown `Error` object's
message — populated from `data.error` in `api.js`). Any other shape means
the UI shows a generic "Something went wrong."

---

## NAVIGATION FLOW (for context)

```
login.html / register.html
    → on success → index.html

index.html
    → PLAY button → queue.html

queue.html
    → joins POST /api/matchmaking/queue
    → WebSocket MATCH_FOUND event
    → immediately → match.html?matchId=<id>
    → Leave Queue → DELETE /api/matchmaking/queue → index.html

match.html
    → loads match data
    → starts 5-second countdown immediately on page load
    → MATCH_END event → shows end modal
    → Back to Dashboard → index.html

Friends challenge accepted
    → POST /api/friends/challenges/{id}/accept
    → response.matchId → match.html?matchId=<matchId>
```

---

## WHAT THE FRONTEND DOES NOT HANDLE YET

These features have UI built but may not have backend endpoints — confirm
before wiring up:

1. **Leaderboard** — no page built, no endpoint called
2. **Password reset** — no UI exists
3. **Problem bank seeding** — frontend renders whatever `/api/problems` returns;
   ensure your DB has problems loaded
4. **Avatar upload** — frontend uses DiceBear only; no file upload endpoint called

---

## CHECKLIST FOR YOUR BACKEND REVIEW

Go through this list and mark each ✅ exists / ❌ missing / ⚠️ exists but different shape:

- [ ] `POST /api/auth/register`
- [ ] `POST /api/auth/login`
- [ ] `GET /api/auth/me`
- [ ] `POST /api/matchmaking/queue`
- [ ] `DELETE /api/matchmaking/queue`
- [ ] `GET /api/matches/{matchId}`
- [ ] `POST /api/matches/{matchId}/submissions`
- [ ] `GET /api/matches/{matchId}/submissions`
- [ ] `GET /api/users/me/matches?limit=n`
- [ ] `PATCH /api/users/me`
- [ ] `PATCH /api/users/me/email`
- [ ] `PATCH /api/users/me/password`
- [ ] `GET /api/users/me/preferences`
- [ ] `PATCH /api/users/me/preferences`
- [ ] `DELETE /api/users/me`
- [ ] `GET /api/problems`
- [ ] `GET /api/problems/{problemId}`
- [ ] `POST /api/problems/{problemId}/submissions`
- [ ] `POST /api/friends/requests`
- [ ] `GET /api/friends/requests/pending`
- [ ] `POST /api/friends/requests/{requestId}/accept`
- [ ] `POST /api/friends/requests/{requestId}/decline`
- [ ] `GET /api/friends`
- [ ] `POST /api/friends/{friendUsername}/challenge`
- [ ] `GET /api/friends/challenges/pending`
- [ ] `POST /api/friends/challenges/{challengeId}/accept`
- [ ] `POST /api/friends/challenges/{challengeId}/decline`
- [ ] WebSocket at `ws://localhost:8080/ws` (SockJS)
- [ ] STOMP topic `/topic/user/{userId}` → `MATCH_FOUND` event
- [ ] STOMP topic `/topic/match/{matchId}` → `MATCH_START` event
- [ ] STOMP topic `/topic/match/{matchId}` → `MATCH_END` event
- [ ] Auth header accepted on STOMP `SUBSCRIBE` frame (not just `CONNECT`)
- [ ] `MATCH_FOUND` data contains `matchId` or `id` field
- [ ] `MATCH_END` data contains `winnerName` or `winnerId` or `winner.username`
- [ ] `MATCH_END` data contains `ratingChanges.currentUser` or `ratingChange`
- [ ] `match.players[]` uses field name `id` (not `userId` or `playerId`)
- [ ] `result` field in match history is `"WIN"` or `"LOSS"` (uppercase)
- [ ] `ratingDelta` field name in match history (not `delta` or `ratingChange`)
- [ ] `difficulty` field is `"EASY"`, `"MEDIUM"`, or `"HARD"` (uppercase)
- [ ] `examples` is an array of `{ input, output }` objects (not strings)

---

*Generated from Coding Arena frontend source — api.js, match.js, queue.js,
dashboard.js, friends.js, practice.js, practice-solve.js, profile.js,
settings.js, websocket.js and all HTML files.*
