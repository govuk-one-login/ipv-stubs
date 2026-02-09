module.exports = {
  clearMocks: true,
  collectCoverage: true,
  coverageDirectory: "coverage",
  coverageProvider: "v8",

  testEnvironment: "node",
  testMatch: ["**/test/**/*.test.ts"],
};
