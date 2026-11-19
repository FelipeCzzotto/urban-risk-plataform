# Urban Risk Intelligence Platform

Plataforma de monitoramento e priorização de riscos urbanos (alagamentos, ilhas de calor, interrupções de serviços essenciais).

- `backend/` — Spring Boot (API REST + WebSocket, Swagger, Flyway, Supabase Postgres)
- `mobile/` — React Native (Expo)
- `docs/CONTRACT.md` — contrato de API e domínio

## Integrantes

| Nome | RM |
|---|---|
| Felipe Chiozzotto Gozzani | RM554715 |

## Pré-requisitos

- Java 21, Node 20+, Docker (testes de integração e E2E)
- Copiar `.env.example` para `.env` na raiz e preencher `DB_*` (Supabase pooler), `INGEST_API_KEY` e `REPORTER_HASH_PEPPER`. Sem esses dois últimos o backend recusa ingestão / não sobe.

## Backend

```sh
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev   # perfil dev liga o simulador de sensores (1 evento/10s)
./mvnw clean verify                                     # testes unitários + integração (Testcontainers)
```

- Swagger: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health
- WebSocket STOMP: `ws://localhost:8080/ws`, tópicos `/topic/events` e `/topic/alerts`

## Mobile

```sh
cd mobile
npm install && cp .env.example .env
npm run web          # ou: npx expo start (Android/iOS via Expo Go)
npm test             # jest
npm run test:e2e     # smoke Playwright (primeira vez: npx playwright install chromium)
```

E2E full stack (backend real + Postgres local em Docker, nunca o Supabase):

```sh
docker run --rm -d --name urip-qa-db -e POSTGRES_PASSWORD=qa -p 55433:5432 postgres:16
cd mobile && npm run test:e2e:fullstack
docker stop urip-qa-db
```

## Documentação

- `docs/CONTRACT.md`: contrato de API e regras de domínio (confiança, priorização)
- `docs/MOBILE.md`: arquitetura do app, comportamento offline/reconexão, testIDs
- `docs/SECURITY.md`: LGPD, modelo de ameaças, escalabilidade e disponibilidade

## Indicadores medidos (E2E)

| Indicador | Resultado |
|---|---|
| Tempo de atualização (POST até a tela) | média 18 ms (15–24 ms, 5 eventos) |
| Detecção de queda do backend | ~2,3 s até o banner offline |
| Recuperação após religar | ~6,8 s até "Reconectado" |
