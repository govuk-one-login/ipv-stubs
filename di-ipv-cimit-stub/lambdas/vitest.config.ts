import { defineConfig } from "vitest/config";

export default defineConfig({
  test: {
    globals: true,
    setupFiles: ["./test/setup.ts"],
    include: ["test/**/*.test.ts"],
    reporters: ["default"],
    coverage: {
      provider: "v8",
      include: ["src/**/*.ts"],
      exclude: ["**/types/*.ts", "**/config.ts"],
      reportsDirectory: "coverage",
      enabled: true,
      reportOnFailure: true,
      reporter: ["lcov", "text-summary"],
    },
    environment: "node",
    clearMocks: true,
  },
});
