module.exports = {
  clearMocks: true,
  collectCoverage: true,
  coverageDirectory: "coverage",
  coverageProvider: "v8",

  testEnvironment: "node",
  testMatch: ["**/unit/**/*.test.ts"],
  setupFiles: ["<rootDir>test/setEnvVars.js"],
};
