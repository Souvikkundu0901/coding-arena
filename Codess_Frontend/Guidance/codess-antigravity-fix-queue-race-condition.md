# Codess — Antigravity Fix: Matchmaking WebSocket Race Condition

## Context
Random matchmaking intermittently fails to notify one of the two
matched players, while direct challenges (a separate feature) always
work correctly. The root cause has been identified in
`static/assests/js/queue.js`: the `startQueue()` function currently
calls `await api.joinQueue()` BEFORE connecting and subscribing to the
WebSocket via `connectToQueueSocket()`. Since the backend's matchmaking
scheduler can pair and broadcast a match within moments of a player
joining the queue, the `MATCH_FOUND` WebSocket event can be sent before
this player's subscription is active — and WebSocket pub-sub has no
replay, so a missed event is simply lost, leaving that player stuck on
the queue screen indefinitely while their opponent proceeds normally.

Direct challenges don't hit this bug because both players are already
connected and subscribed before any match-triggering action occurs.

## Fix required

In `static/assests/js/queue.js`, reorder `startQueue()` so the
WebSocket connection and subscription complete FIRST, and the app only
calls `api.joinQueue()` after that subscription is confirmed active.

Specifically:
1. Wrap the existing `ws.connect(...)` call (currently inside
   `connectToQueueSocket`) in a `Promise` that resolves only once the
   connect callback fires AND `ws.subscribeToUserTopic(...)` has been
   called.
2. `await` that promise before calling `await api.joinQueue()`.
3. Update the status message shown to the user during this phase (e.g.
   "Connecting..." while the socket connects, then "Finding an
   opponent..." once the queue join begins) so the UI doesn't feel
   stuck during the brief connection step.
4. The existing `connectToQueueSocket` function can be removed and its
   logic inlined into the promise-wrapped block inside `startQueue`, OR
   kept as a separate function that itself returns a Promise — whichever
   is a smaller, cleaner diff against the current file.
5. Keep all existing error handling (the try/catch, the error message
   shown, the "Back to Home" button state change) working exactly as
   before — just make sure a WebSocket connection failure is also
   caught and surfaced the same way a queue-join failure currently is.

## What NOT to do
- Do not change `handleMatchFound`, `handleUserEvent`, the timer logic,
  or `handleLeaveQueue` — this fix is scoped entirely to the ordering
  inside `startQueue`/`connectToQueueSocket`.
- Do not change anything in `websocket.js` unless `ws.connect` or
  `ws.subscribeToUserTopic` turn out to not reliably signal completion
  via their callback (check this file only if the promise-wrapping
  approach above doesn't have a clean callback to hook into — flag this
  explicitly rather than guessing).
- Do not touch any backend code — this is a frontend-only timing fix.

## After generation, I will manually verify
- Open two separate browser windows (or incognito + normal), log in as
  two different users, and queue both within a second or two of each
  other, repeated several times in a row.
- Confirm BOTH players are reliably redirected to the match screen every
  time, not just one of them.
