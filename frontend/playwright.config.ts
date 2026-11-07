import { defineConfig } from "@playwright/test";
export default defineConfig({
  testDir: "./tests",
  workers: 1,
  timeout: 60000,
  use: {
    baseURL: "http://localhost:5174",
    trace: "off",
    screenshot: "only-on-failure",
  },
  webServer: {
    command:
      "API_PROXY_TARGET=http://127.0.0.1:8081 npm run dev -- --port 5174",
    url: "http://localhost:5174",
    reuseExistingServer: false,
  },
});
