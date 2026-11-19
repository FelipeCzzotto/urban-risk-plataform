# Security and Data Protection

## Personal data (LGPD)
- The only personal data accepted is the optional `contact` on `POST /events/community`.
- It is normalized (trim, lower-case) and stored only as `SHA-256(REPORTER_HASH_PEPPER + contact)` in `reporter_hash`. The raw value is never persisted, logged, published over WebSocket or returned.
- `reporterHash` is not part of `EventDTO` (REST or STOMP). `toString()` of request/command objects omits `contact`.
- The pepper lives only in env (`REPORTER_HASH_PEPPER`); startup fails without it. If the pepper leaks, phone numbers are brute-forceable: rotate it (old hashes stop correlating, which is acceptable: the hash only links reports from the same reporter).
- Data minimization: no IP, user-agent or contact is stored. The rate limiter keeps client IPs in memory for at most one minute.
- Supabase: `risk_events` and `flyway_schema_history` have RLS enabled with no policies, and grants to `anon`/`authenticated` are revoked (V3 migration + `afterMigrate.sql` Flyway callback). Only the backend role (which bypasses RLS) can access them, so PostgREST cannot expose `reporter_hash`.

## Threat model (summary)
| Asset / entry point | Threat | Control |
|---|---|---|
| `POST /events/sensor`, `/events/public`, `PATCH /events/{id}/resolve` | Forged ingest, mass resolve | `X-API-Key`, constant-time compare, fails closed when unset. Path is normalized like MVC routing, so `;params`, `%xx` and `//` cannot bypass it. |
| `POST /events/community` (public) | Spam, priority manipulation | Bean Validation bounds (lengths, lat/lng ranges, severity 1..5, enums), DB CHECK constraints, per-IP fixed-window rate limit with bounded memory. Community confidence is low (0.5). |
| `/ws` STOMP | Spoofed alerts broadcast to every client | Client `SEND` frames are dropped; only the server publishes to `/topic/**`. Browser origins are checked against `CORS_ALLOWED_ORIGINS`. |
| Errors | Stack trace / internals leak | RFC 7807 bodies with generic messages; unhandled errors give `"Unexpected error"` and are logged server-side only. |
| Actuator | Env/heap/config disclosure | Only `health` is exposed, `show-details: never`. |
| DB | SQL injection | JPA with bound parameters only; no string-built SQL. |
| Secrets | Leak via repo/defaults | No defaults for `DB_*`, `INGEST_API_KEY`, `REPORTER_HASH_PEPPER`; `.env` is gitignored. |
| Browser clients | Clickjacking, MIME sniffing | `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy: no-referrer`. HSTS belongs on the TLS-terminating proxy. |

Accepted / out of scope:
- No user accounts: one shared ingest key. Rotate it by redeploying; move to per-client keys or OAuth2 client credentials when there is more than one ingest partner.
- Swagger UI and `/v3/api-docs` are public. They hold no secrets or real examples; set `springdoc.api-docs.enabled=false` in production if wanted.
- Read endpoints and WebSocket subscriptions are public by design (public risk information).

## Availability and scalability
- Stateless REST API (no HTTP session), so it scales horizontally behind a load balancer, except for the two items below.
- Hikari pool capped at 5 connections (Supabase pooler limits), 10s connection timeout.
- Graceful shutdown (`server.shutdown: graceful`, 20s per phase) drains in-flight requests on deploy.
- **Single-instance limitations:**
  - The rate limiter is in-memory, per instance, keyed on `remoteAddr`. Behind a proxy, configure trusted `X-Forwarded-For` handling (`server.forward-headers-strategy`) or every client shares the proxy's IP. With several replicas, move it to Redis (e.g. bucket4j-redis). When the table is full of live windows (IP-rotation flood), new clients get 429 so memory stays bounded.
  - The STOMP simple broker is in-memory: events published on replica A do not reach clients on replica B. Upgrade path: `enableStompBrokerRelay` to RabbitMQ (STOMP plugin), or Redis pub/sub fan-out.

## Dependencies
Spring Boot 3.5.x BOM manages versions. Run `./mvnw org.owasp:dependency-check-maven:check` in CI (needs an NVD API key; the first database download is slow).
