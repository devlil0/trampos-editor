import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./e2e",
  workers: 1,
  retries: 0,
  forbidOnly: Boolean(process.env.CI),
  timeout: 240_000,
  expect: { timeout: 12_000 },
  use: {
    ...devices["Desktop Chrome"],
    navigationTimeout: 20_000,
    actionTimeout: 12_000,
    screenshot: "off",
    trace: "off",
  },
});

