# Shared Contract (source of truth for all sub-sessions)

Monorepo:
```
backend/   Spring Boot 3.x, Java 21, Maven, Flyway, JPA, WebSocket/STOMP, springdoc-openapi
mobile/    Expo (SDK latest stable) + expo-router + TypeScript, Jest + RNTL, Playwright (web target)
docs/      this contract, architecture notes
```

## Git rules (all sessions)
- Author ONLY `FelipeCzzotto <felipechiozzotto01@gmail.com>`. No `Co-Authored-By` lines.
- Conventional commits, NO scope parens: `feat: add event ingestion endpoint`, `test: ...`, `fix: ...`, `chore: ...`, `docs: ...`.
- Atomic commits. TDD: commit failing test (`test: ...`) then implementation (`feat: ...`).
- Work on feature branch (`feat/backend-core`, `feat/mobile-app`, `feat/db-migrations`, `feat/security-hardening`). Orchestrator merges to `main`.
- Never commit `.env`.

## Domain
RiskEvent:
| field | type | notes |
|---|---|---|
| id | UUID | |
| type | enum `FLOOD`, `HEAT_ISLAND`, `SERVICE_OUTAGE` | |
| title | string ≤120 | |
| description | string ≤1000 | |
| latitude / longitude | double | validated ranges |
| neighborhood | string ≤80 | |
| severity | int 1..5 | raw reported severity |
| source | enum `SENSOR`, `PUBLIC_DATA`, `COMMUNITY` | origin of data |
| sourceRef | string ≤120 | sensor id / dataset name / "community" |
| confidence | double 0..1 | computed by backend from source + corroboration |
| priorityScore | double 0..100 | computed by AI/analysis service |
| priorityLevel | enum `LOW`,`MEDIUM`,`HIGH`,`CRITICAL` | derived from score |
| status | enum `ACTIVE`,`RESOLVED` | |
| reporterHash | string, nullable | SHA-256(pepper+contact) — NEVER return raw PII, field not exposed in API |
| occurredAt / createdAt | ISO-8601 instant | |

### Confidence (base per source, then +0.1 per corroborating event of same type within 1km/1h, cap 1.0)
SENSOR 0.9, PUBLIC_DATA 0.8, COMMUNITY 0.5.

### Priority analysis (the "AI/analysis" feature)
Weighted scoring + recency decay + spatial clustering:
`score = 100 * clamp( 0.45*(severity/5) + 0.25*confidence + 0.20*clusterFactor + 0.10*recency )`
- clusterFactor = min(1, nearbySameTypeLast1h / 5)  (radius 1km, haversine)
- recency = exp(-ageHours / 6)
- level: <40 LOW, <60 MEDIUM, <80 HIGH, else CRITICAL.
Implemented as a pure domain service, fully unit tested.

## REST API (base `/api/v1`, JSON, Swagger UI at `/swagger-ui.html`, spec `/v3/api-docs`)
- `POST /events/sensor`     header `X-API-Key` required. body: type,title,description,latitude,longitude,neighborhood,severity,sourceRef,occurredAt
- `POST /events/public`     header `X-API-Key` required. same body.
- `POST /events/community`  public, rate-limited. same body minus sourceRef, plus optional `contact` (hashed, never stored raw).
- `GET  /events?type=&level=&status=ACTIVE&page=0&size=20` → page sorted by priorityScore desc, then occurredAt desc.
- `GET  /events/{id}`
- `GET  /alerts` → ACTIVE events with level HIGH|CRITICAL, sorted by priorityScore desc.
- `PATCH /events/{id}/resolve` header `X-API-Key`.
- `GET  /dashboard/summary` → `{ totalActive, byType:{FLOOD:n,...}, byLevel:{LOW:n,...}, lastUpdatedAt }`
- `GET  /actuator/health` → liveness for app connectivity check.
- Errors: RFC 7807 ProblemDetail.

Response EventDTO = all domain fields except reporterHash.

## Realtime (WebSocket/STOMP)
- Endpoint `/ws` (native WebSocket, no SockJS required; allow SockJS optional).
- Topic `/topic/events` → EventDTO on create/update.
- Topic `/topic/alerts` → EventDTO when level HIGH|CRITICAL.
- Simulator: `@Scheduled` sensor simulator (toggle `app.simulator.enabled`, default true in dev profile) generating events every 10s around São Paulo.

## Mobile behavior
- Tabs: Painel (dashboard summary + top alerts), Eventos (list w/ filters + map toggle; map via react-native-maps on native, list fallback on web), Alertas.
- STOMP client (@stomp/stompjs) with reconnect backoff; heartbeat.
- Connection state machine: `connected | reconnecting | offline`. Offline banner (accessibilityLiveRegion/role="alert"), shows last cached data (AsyncStorage), never crashes. On reconnect: refetch REST + resubscribe, banner "Reconectado".
- Local notifications (expo-notifications) for incoming `/topic/alerts`; on web, in-app toast.
- Accessibility: labels on all touchables, min 44pt targets, contrast AA, font scaling, pt-BR UI copy.
- Each element used by QA has `testID`.
- API base URL from `EXPO_PUBLIC_API_URL`.

## Env vars (see `.env.example`)
DB_URL, DB_USERNAME, DB_PASSWORD, INGEST_API_KEY, REPORTER_HASH_PEPPER, CORS_ALLOWED_ORIGINS, EXPO_PUBLIC_API_URL.
Tests use Testcontainers Postgres (Docker available) — never hit the real Supabase DB in tests.

## Clarifications (v1.1)
- `GET /events` returns `{ content: EventDTO[], page, size, totalElements, totalPages }` (custom PageResponse, not raw Spring Page).
- `GET /alerts` returns a plain `EventDTO[]` (max 50).
- `/actuator/health` returns `{ "status": "UP" }`; app treats any non-2xx/timeout (3s) as down.
- Community events: `sourceRef` = `"community"`.
- `/topic/events` also publishes on resolve (status `RESOLVED`); clients remove/update by `id`.
- CORS default: `http://localhost:8081,http://localhost:19006,http://localhost:4173`.
- DB constraints: `title` 1..120 required; `description`, `neighborhood`, `source_ref` NOT NULL (empty string allowed for description); `severity` smallint; `status` default `ACTIVE`; `reporter_hash` ≤128.

## Sub-session operational rules
- Run git as single plain commands from inside the worktree (no `git -C`, no chaining).
- Any wait/poll loop MUST sleep ≥1s and cap retries (≤60). Never print unbounded output.
