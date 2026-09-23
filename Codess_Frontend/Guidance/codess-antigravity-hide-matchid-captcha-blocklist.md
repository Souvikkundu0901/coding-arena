# Codess — Antigravity Prompt: Hide Match ID, Add CAPTCHA, Block Disposable Emails

Three independent, small features bundled together. Each is scoped
separately below — flag anything unclear before generating, and feel
free to note if these should be split into separate passes.

---

## Part 1 — Hide matchId from the URL

### Context
Currently `queue.js` redirects to `match.html?matchId=...`, exposing the
match ID as a visible URL query parameter. This is not a security issue
(the backend already restricts match access to the two actual players,
returning 403 to anyone else), but it's cleaner UX to not show it.

### Requirement
- In `queue.js` (all locations in the repo that currently do this),
  instead of `window.location.href = "match.html?matchId=" + ...`,
  store the matchId in `sessionStorage` (e.g.
  `sessionStorage.setItem('currentMatchId', matchId)`) and navigate to
  a plain `match.html` with no query string.
- In `match.js` (or wherever the match page currently reads `matchId`
  from `URLSearchParams`/the query string), change it to read from
  `sessionStorage.getItem('currentMatchId')` instead.
- Handle the case where `sessionStorage` doesn't have a matchId when
  `match.html` loads directly (e.g. a stale bookmark or manual URL
  visit) — redirect to the dashboard/home page with a clear message
  rather than erroring silently.
- This applies the same way to however challenge-acceptance currently
  navigates to the match page, if it also passes matchId via URL.

### What NOT to do
- Do not change any backend authorization logic — access control to a
  match is already correctly enforced server-side regardless of how the
  frontend passes the ID around.

---

## Part 2 — Add CAPTCHA to registration (stop bot signups)

### Context
Preventing automated bot registrations without requiring email
verification (which is explicitly out of scope). Using Google
reCAPTCHA v2 ("I'm not a robot" checkbox) — free, no email
infrastructure needed.

### Requirement
- Add the reCAPTCHA v2 widget to `register.html`, using Google's
  standard client-side script and site key (site key will be a
  configurable value — use a placeholder
  `YOUR_RECAPTCHA_SITE_KEY_HERE` and clearly flag that I need to
  replace it with a real key from
  https://www.google.com/recaptcha/admin after generation).
- On the frontend, block form submission until the CAPTCHA is completed
  (reCAPTCHA's own widget handles the "I'm not a robot" UX; just ensure
  the response token exists before calling the register API).
- On submission, include the CAPTCHA response token in the registration
  request body (e.g. add a `captchaToken` field alongside username/
  email/password).
- **Backend**: add a `captcha.secret-key: ${RECAPTCHA_SECRET_KEY:}`
  config property. In `AuthService`'s registration flow, before creating
  the user, verify the token by calling Google's verification endpoint
  (`POST https://www.google.com/recaptcha/api/siteverify` with the
  secret key and the token). If verification fails or the token is
  missing, reject registration with 400 and a clear error message
  ("CAPTCHA verification failed"). Do not create the user if this check
  fails.
- If `RECAPTCHA_SECRET_KEY` is not set (empty), skip the check entirely
  (log a warning) so local development without a real key still works —
  mirror the same "optional via env var" pattern already used for
  `RAPIDAPI_KEY`.

### What NOT to do
- Do not touch the login endpoint — CAPTCHA only applies to
  registration, not login.
- Do not change the actual user-creation logic beyond adding this one
  pre-check.

---

## Part 3 — Block disposable/throwaway email domains at registration

### Context
A lightweight second layer against bot/fake signups: reject registration
if the email's domain is a known disposable-email provider. No email is
ever sent — this is a simple domain-string check.

### Requirement
- Add a small, hardcoded list of common disposable email domains (e.g.
  `mailinator.com`, `10minutemail.com`, `tempmail.com`, `guerrillamail.com`,
  `throwawaymail.com`, `yopmail.com` — a reasonable starter list of
  10-20 well-known ones) as a constant in `AuthService` or a small
  dedicated helper class.
- Before creating a user, extract the domain from the submitted email
  (after the existing `.toLowerCase().trim()` normalization) and reject
  with 400 (`"This email provider is not allowed"`) if it matches the
  blocklist.
- Keep the list easy to find and extend later (a single `Set<String>` or
  similar, clearly named, not scattered across multiple files).

### What NOT to do
- Do not attempt to build or call any real-time disposable-email-
  detection API — a static list is sufficient and keeps this dependency-
  free.

---

## After generation, I will manually verify
- Navigating to `match.html` directly (no active match in
  sessionStorage) redirects cleanly instead of erroring.
- A completed match still correctly loads via sessionStorage-passed
  matchId end-to-end.
- Registration is blocked without completing the CAPTCHA (once I've
  swapped in a real site/secret key pair).
- Registering with a disposable email domain (e.g.
  `test@mailinator.com`) is rejected with a clear error.
- Registering with a normal email (e.g. Gmail) still works normally.
