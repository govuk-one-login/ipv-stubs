module.exports = {
  clearMocks: true,
  collectCoverage: true,
  coverageDirectory: "coverage",
  coverageProvider: "v8",

  preset: "@shelf/jest-dynamodb",

  testEnvironment: "node",
  testMatch: ["**/unit/**/*.test.ts"],
};
