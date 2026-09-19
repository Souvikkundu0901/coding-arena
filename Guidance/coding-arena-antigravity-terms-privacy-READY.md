# Coding Arena — Antigravity Prompt: Terms & Privacy Pages + Registration Checkbox

## Context
Adding two static legal pages (Terms & Conditions, Privacy Policy) to
the site, plus a required "I agree" checkbox on the registration form.
Content for both pages is provided below — use it as-is, styled to
match the rest of the site (same fonts, colors, header/footer as the
existing pages under `static/assests/`).

## Requirements

### 1. Two new static pages
- `static/assests/terms.html` — Terms & Conditions
- `static/assests/privacy.html` — Privacy Policy

Both should use the same visual style/layout as the existing pages
(reuse the site's header/logo and general page structure — check
`login.html` or `index.html` for the pattern to match). Content is
provided in full below; render it as clean, readable HTML with
headings (`<h2>` for each numbered section) and paragraphs — don't
just dump it as a single unstyled text block.

### 2. Footer links (site-wide, if a shared footer/layout exists)
Add "Terms & Conditions" and "Privacy Policy" links pointing to
`/terms.html` and `/privacy.html`, placed wherever a footer or
site-wide link area already exists. If there's no shared footer
component across pages, just add the links at the bottom of
`login.html`, `register.html`, and `index.html` at minimum.

### 3. Required checkbox on the registration form
On `register.html`:
- Add a checkbox: "I agree to the [Terms & Conditions](/terms.html) and
  [Privacy Policy](/privacy.html)" (with the two phrases as actual links
  opening the respective pages, ideally in a new tab so the user doesn't
  lose their in-progress registration form).
- The registration submit button/action must be disabled (or blocked
  with a clear inline message) until this checkbox is checked — do not
  let registration proceed without it.
- This is frontend-only validation — no backend change needed, since
  agreement isn't something that needs to be stored per-user for this
  project's scope.

## What NOT to do
- Do not modify any backend code, endpoints, or the registration API
  call itself — this is a frontend-only addition (two static pages plus
  one checkbox with client-side validation).
- Do not change the actual registration request payload — the checkbox
  is a client-side gate before submission, not a new field sent to the
  server.

## Content for terms.html

# TERMS AND CONDITIONS

**Last updated: September 12, 2026**

Welcome to Coding Arena. By creating an account or using this platform,
you agree to the following terms.

### 1. What Coding Arena Is
Coding Arena is a competitive coding platform where users are matched
with an opponent of similar skill rating and race to solve a
programming problem. This is a student project built for educational
and demonstration purposes.

### 2. Accounts
- You must provide a valid email address and choose a username at
  registration.
- You are responsible for keeping your password secure. Do not share
  your account with others.
- You must be old enough to legally consent to these terms in your
  jurisdiction to create an account.

### 3. Fair Play
- Do not use external assistance, automated tools, or another person's
  help to solve problems during a rated match.
- Leaving or switching away from the active match screen may trigger a
  warning and, on a repeated violation, an automatic forfeit of the
  match in your opponent's favor.
- Attempting to exploit, disrupt, or abuse the matchmaking, judging, or
  rating systems is not permitted and may result in account suspension.

### 4. Code Submissions
- Code you submit is sent to a third-party code execution service
  (Judge0, via RapidAPI) solely to compile/run it and compare the output
  against the problem's expected answer.
- Do not submit malicious code intended to disrupt, exploit, or attack
  the judging service or this platform.
- Submitted code and its verdict are stored so you and your opponent can
  view match history and submission results.

### 5. Ratings
Your rating changes after each completed rated match based on the
outcome, using a standard Elo-style calculation. Ratings reset only if
we explicitly announce a reset; there is no guarantee of rating
preservation if the platform is significantly changed or reset during
development.

### 6. Account Termination
- You may delete your own account at any time from account settings.
- We may suspend or terminate accounts that violate these terms.

### 7. No Warranty
This is a student project provided "as is," without warranty of any
kind. We do not guarantee uninterrupted availability, and features may
change or be removed during development.

### 8. Changes to These Terms
These terms may be updated as the project develops. Continued use of
the platform after changes means you accept the updated terms.

### 9. Contact
Questions about these terms can be directed to kundusouvik54@gmail.com.

## Content for privacy.html

# PRIVACY POLICY

**Last updated: September 12, 2026**

This policy explains what data Coding Arena collects and how it is
used.

### 1. What We Collect
- **Account information**: username, email address, and a securely
  hashed password (we never store your password in plain text).
- **Gameplay data**: match history, submitted code, submission verdicts,
  ratings, wins/losses, and match timestamps.
- **Social data**: friend requests/connections and direct challenges you
  send or receive, if you use those features.
- **Preferences**: theme and notification settings, if you set them.

We do not collect payment information, government ID numbers, or
precise location data. This platform does not use third-party
advertising trackers.

### 2. How We Use Your Data
- To operate your account (login, authentication via secure tokens).
- To run matchmaking and pair you with opponents of similar rating.
- To send your submitted code to our code-execution provider (Judge0 via
  RapidAPI) so it can be run and judged.
- To calculate and display ratings, match history, and leaderboards
  where applicable.
- To let you connect with friends and send/receive match challenges.

### 3. Third-Party Services
Your submitted code is sent to **Judge0 (via RapidAPI)** to be executed
and evaluated. This is necessary for the platform's core function of
judging submissions. We do not send your email, password, or other
account details to this service — only the code, language, and problem
input/output needed to produce a verdict.

Your account data (email, password hash, match data) is stored using
**Supabase** (PostgreSQL) and **Upstash** (Redis, used only for
matchmaking queue state, not persistent storage).

### 4. Data Retention
- Account data is retained as long as your account exists.
- If you delete your account, it is deactivated and can no longer be
  used to log in; match history involving you may be retained in
  anonymized or reduced form to preserve the integrity of other
  players' match records, but your account is no longer active or
  accessible.

### 5. Your Rights
- You can update your username, email, and password from account
  settings.
- You can delete your account at any time, which requires confirming
  your password.
- You can request more detail about what data we hold by contacting us
  directly.

### 6. Security
Passwords are hashed using industry-standard hashing (BCrypt) and are
never stored or transmitted in plain text. Authentication uses
short-lived signed tokens (JWT). While we take reasonable measures to
protect your data, no system can guarantee absolute security, especially
during active development of a student project.

### 7. Changes to This Policy
This policy may be updated as the project develops. Material changes
will be reflected by updating the "last updated" date above.

### 8. Contact
Questions about this policy can be directed to kundusouvik54@gmail.com.

## After generation, I will manually verify
- `/terms.html` and `/privacy.html` load correctly and are styled
  consistently with the rest of the site
- The registration checkbox genuinely blocks submission when unchecked
- Footer/page links to both pages work from at least the login and
  register pages
