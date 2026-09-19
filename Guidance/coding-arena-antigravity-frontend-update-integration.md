# Coding Arena — Antigravity Prompt: Integrate Updated Frontend

## Context
An updated frontend build has been provided in a folder/zip named
`Coding Arena_Frontend` (note the underscore and capitalization — use
the exact name/path as provided, don't assume it matches the previous
`Coding Arena_frontend` folder name exactly). This is an updated version
of the frontend already integrated once before (which is currently
served from `src/main/resources/static/assests/`) — it should now
include the anti-cheat tab-switch detection and forfeit-call logic on
the match screen, plus any other updates your friend made.

**Do not redo the whole integration from scratch.** The static resource
routing, CORS config, and DTO alignment (`getFromUsername`/`getToUsername`
aliases, `players[]` array, etc.) from the previous integration pass are
already working — this is an update/replace of the frontend files, not a
first-time setup.

## Requirements

1. Replace the contents of `src/main/resources/static/` with the new
   files from `Coding Arena_Frontend`, preserving the same folder
   structure Spring is already configured to serve from (check whether
   the new zip uses the same `assests/`, `css/`, `js/` folder names as
   before — if the new zip uses different folder names, e.g. corrected
   to `assets/` without the typo, either rename to match the existing
   `WebMvcConfig` resource-handler paths, OR update `WebMvcConfig.java`'s
   `addResourceHandlers` and `addViewControllers` paths to match the new
   folder names — pick whichever requires the smaller change and note
   which you chose).

2. Specifically verify the new frontend's match screen includes:
   - A call to `POST /api/matches/{matchId}/forfeit` (the endpoint built
     and verified in the previous session) wired to the tab-switch
     detection logic.
   - If the forfeit-calling code isn't present yet in these files, flag
     this clearly rather than silently skipping it — this may mean the
     frontend update doesn't yet include the anti-cheat piece and that's
     fine to note, not something to fabricate.

3. Re-run the same verification the previous integration pass used:
   - `http://localhost:8080/` loads the login/landing page
   - `http://localhost:8080/login.html` (and other top-level `.html`
     routes) resolve correctly via the existing `/*.html` resource
     handler — don't remove or break that fix
   - Static CSS/JS assets load without 404s

4. Keep all backend DTO/field-alignment work from the previous pass
   intact — do not revert `getFromUsername`/`getToUsername` aliases,
   the `players[]` array on `MatchDto`, `@JsonAlias` on
   `SendFriendRequest`, or any other compatibility shim added earlier
   unless the new frontend files demonstrably no longer need them (in
   which case, note it, but leaving unused aliases in place is harmless
   and safer than removing something still relied upon elsewhere).

## What NOT to do
- Do not modify any business logic — `SubmissionService`, `EloService`,
  `MatchService`, the atomic winner/expiry/forfeit logic, rate limiting,
  or any already-verified backend code. This is a static-asset swap plus
  routing/config verification only.
- Do not change the CORS configuration beyond what's already set up,
  unless the new frontend's actual origin/port differs from before.
- Do not guess at missing files — if the provided `Coding Arena_Frontend`
  folder is missing a file the old integration expected (e.g. a
  referenced JS file that no longer exists), flag it rather than
  inventing a replacement.

## After generation, I will manually verify
- The site loads correctly in a browser at `http://localhost:8080/`
- Login/register still work end-to-end
- The match screen shows the new anti-cheat behavior (if included) —
  tab-switch triggers a warning, second switch calls `/forfeit`
- `mvn clean test` still passes with no regressions
