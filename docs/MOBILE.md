# Mobile

Expo SDK 58 + expo-router + TypeScript (strict). Code lives in `mobile/`, routes in `mobile/src/app/`.

## Run

```sh
cd mobile
npm install
cp .env.example .env          # EXPO_PUBLIC_API_URL, default http://localhost:8080
npm run web                   # or: npx expo start (Expo Go / dev build)
```

| Script | What it does |
|---|---|
| `npm test` | Jest (jest-expo + RNTL), network fully mocked |
| `npm run typecheck` | `tsc --noEmit` |
| `npm run lint` | `expo lint` (eslint-config-expo flat) |
| `npm run build:web` | `expo export --platform web` into `dist/` |
| `npm run test:e2e` | Playwright: builds web against an unreachable API, serves on :4173, checks tabs + offline banner. Set `E2E_API_URL` to test against a live backend. First run: `npx playwright install chromium`. |

The WS URL is derived from the API URL: `http(s)://host` becomes `ws(s)://host/ws`.

## Layers (`mobile/src`)

- `domain/`: types (EventDTO), pt-BR labels and AA colors for priority/source/type, `connectionReducer`, event list ops (sort, upsert, remove on RESOLVED, filters).
- `data/`: `apiClient` (fetch + AbortController timeout, typed `ApiError` network/timeout/http/parse), `cacheRepository` (AsyncStorage, never throws, `fetchWithCache` fallback), `realtimeClient` (@stomp/stompjs, exponential backoff 1s to 30s, 10s heartbeats, resubscribe on every connect), `notifier` (expo-notifications on native, no-op on web).
- `presentation/`: `AppDataProvider` + hooks (`useConnection`, `useEvents`, `useAlerts`, `useSummary`, `useToast`), components, screens.
- `app/`: tabs Painel (`index`), Eventos (`eventos`), Alertas (`alertas`).

## Connection behaviour

`reconnecting` (initial / socket dropped) goes to `offline` after 3 failed attempts, or immediately on a REST network/timeout error. It goes back to `connected` on STOMP connect. Recovering from a drop or from offline triggers a REST refetch and shows the "Reconectado" banner for 3s. The cached data stays on screen the whole time.

## testIDs for QA

`connection-banner`, `tab-painel`, `tab-eventos`, `tab-alertas`, `event-card-<id>`, `filter-type-{all|FLOOD|HEAT_ISLAND|SERVICE_OUTAGE}`, `filter-level-{all|LOW|MEDIUM|HIGH|CRITICAL}`, `toggle-map`, `events-map`, `events-list`, `alerts-list`, `dashboard-list`, `summary-total`, `alert-toast`, `alert-toast-close`.

## Known limits

- The Eventos tab loads the first 100 ACTIVE events and filters on the client, so filters also work on cached data. Add server paging if there are more than 100 active events.
- Local notifications use the default Android channel.
- `react-native-maps` needs a dev build (not Expo Go) on Android to use Google Maps API keys.
