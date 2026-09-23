# 🛡️ Security Policy — Codess

The **Codess** team takes security, anti-cheat integrity, and player data privacy seriously. This document outlines our supported versions, vulnerability reporting process, and active defense mechanisms.

---

## 📦 Supported Versions

Security updates and patches are actively maintained for the following versions:

| Version | Supported | Status |
| :--- | :---: | :--- |
| `1.0.x` (`main`) | :white_check_mark: | **Current Active Release** — Receives all security patches and bug fixes |
| `< 1.0.0` | :x: | **End of Life** — Legacy development revisions |

---

## 🚨 Reporting a Vulnerability

If you discover a security vulnerability or exploit within Codess (including code evaluation bypasses, authentication flaws, matchmaking race conditions, or anti-cheat circumventions), please report it responsibly.

### How to Report
- **Contact Maintainer**: Email **Souvik Kundu** directly at [`kundusouvik54@gmail.com`](mailto:kundusouvik54@gmail.com) with the subject tag `[SECURITY] Codess Vulnerability Report`.
- **Details to Include**:
  - A clear description of the vulnerability and its potential impact.
  - Step-by-step reproduction steps or a minimal proof-of-concept (PoC).
  - Component affected (e.g., Auth Service, Matchmaking Queue, WebSocket broker, Sandbox execution).
  - Suggested remediation (if known).

### Response Timeline & SLA
- **Initial Response & Acknowledgment**: Within **48 hours**.
- **Assessment & Triage**: Within **3 business days**.
- **Fix & Deployment**: Security patches are prioritized and typically deployed to production within **5–7 days** of verification.
- **Public Disclosure**: We practice responsible disclosure. We kindly request that you do not publish details about the vulnerability until a fix has been verified and deployed.

---

## 🔒 Security Architecture & Defensive Controls

Codess implements multi-tiered defense-in-depth security:

### 1. Bot & Spam Registration Protection
- **Google reCAPTCHA v2**: Registration requires successful completion of the "I'm not a robot" challenge. Tokens are validated server-side against Google's `siteverify` API before any database writes.
- **Disposable Email Domain Filtering**: Real-time domain verification against an active blocklist of 15+ known throwaway/temporary email providers (e.g., `mailinator.com`, `tempmail.com`, `10minutemail.com`).
- **Input Sanitization & Normalization**: Emails are lowercase-normalized and trimmed; usernames and passwords undergo strict validation checks.

### 2. Match Integrity & Anti-Cheat
- **Session Defocus & Tab-Switch Forfeit**: The match client monitors browser `visibilitychange` and `blur` events. Players navigating away from the active coding battle tab trigger an immediate match forfeit.
- **WebSocket Disconnect Tracking**: The server-side `MatchSessionTracker` monitors WebSocket connection lifecycles. If a player abruptly closes their tab or loses connection, an auto-forfeit is triggered after a 6-second grace period (allowing legitimate network reconnects), awarding the win to the remaining player.
- **URL Privacy**: Match IDs are managed via private `sessionStorage` rather than visible URL parameters (`?matchId=...`), eliminating match URL link snooping.
- **Atomic CAS Winner Determination**: Match completion relies on atomic database Compare-And-Swap queries (`WHERE id = ? AND winner_id IS NULL`), preventing concurrent submission race conditions.

### 3. Authentication & Authorization
- **Stateless JWT**: Standardized Bearer token authentication signed with HS256 (minimum 256-bit secret) with 24-hour expiration.
- **Password Security**: Passwords are encrypted using BCrypt with adaptive salting. Plaintext passwords are never logged or stored.
- **STOMP Destination Security**: The `WebSocketSecurityInterceptor` validates JWT authentication before granting subscription access to `/topic/user/{userId}` or `/topic/match/{matchId}`.

### 4. Abuse & Denial of Service (DoS) Mitigation
- **Rate Limiting**: Built-in Bucket4j token bucket algorithms rate-limit sensitive endpoints (5 auth req/min, 10 code execution req/min, 15 matchmaking req/min) to prevent brute force and queue flooding.
- **Resource Sandboxing**: Untrusted user code is executed in isolated, multi-tenant sandboxes via Judge0 CE with constrained CPU, memory, and runtime limits.
