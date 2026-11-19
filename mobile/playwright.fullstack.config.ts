import { existsSync } from 'node:fs';
import { defineConfig, devices } from '@playwright/test';

// Full stack: web build -> real Spring Boot backend (:8080) -> local Docker Postgres (:55433).
// Prereq: `docker run --rm -d --name urip-qa-db -e POSTGRES_PASSWORD=qa -p 55433:5432 postgres:16`.
// The spec starts/stops the backend itself via e2e/backend-control.sh.
const PORT = 4174;

export default defineConfig({
  testDir: './e2e',
  testMatch: 'fullstack.spec.ts',
  timeout: 180_000,
  workers: 1,
  use: { baseURL: `http://localhost:${PORT}`, trace: 'retain-on-failure' },
  projects: [
    {
      name: 'chrome',
      use: { ...devices['Desktop Chrome'], ...(existsSync('/usr/bin/google-chrome') ? { channel: 'chrome' } : {}) },
    },
  ],
  webServer: {
    command: `EXPO_PUBLIC_API_URL=http://localhost:8080 npx expo export --platform web && npx expo serve --port ${PORT}`,
    url: `http://localhost:${PORT}`,
    timeout: 180_000,
    reuseExistingServer: false,
  },
});
