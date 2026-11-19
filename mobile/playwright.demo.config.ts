import { defineConfig } from '@playwright/test';
import base from './playwright.fullstack.config';

// Demo recording: same stack as fullstack e2e, slowed down, phone-sized viewport, video always on.
const phone = { width: 430, height: 860 };
export default defineConfig({
  ...base,
  testMatch: 'demo.spec.ts',
  timeout: 300_000,
  outputDir: 'demo-video',
  projects: [
    {
      name: 'chrome',
      // viewport must sit at project level: the base project's Desktop Chrome device would override it.
      use: { ...base.projects![0]!.use, viewport: phone, deviceScaleFactor: 2, video: { mode: 'on', size: phone }, launchOptions: { slowMo: 250 } },
    },
  ],
});
