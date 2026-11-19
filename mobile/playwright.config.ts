import { defineConfig, devices } from '@playwright/test';

const PORT = 4173;
// Unreachable API baked into the build so the smoke test exercises the offline path.
// Point E2E_API_URL at a running backend to test the online path instead.
const API = process.env.E2E_API_URL ?? 'http://127.0.0.1:9';

export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  retries: process.env.CI ? 1 : 0,
  use: { baseURL: `http://localhost:${PORT}`, trace: 'retain-on-failure' },
  projects: [{ name: 'mobile-chrome', use: { ...devices['Pixel 7'] } }],
  webServer: {
    command: `EXPO_PUBLIC_API_URL=${API} npx expo export --platform web && npx expo serve --port ${PORT}`,
    url: `http://localhost:${PORT}`,
    timeout: 180_000,
    reuseExistingServer: !process.env.CI,
  },
});
