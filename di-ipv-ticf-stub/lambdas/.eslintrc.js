module.exports = {
  env: {
    node: true,
    es6: true,
    es2020: true,
    browser: true,
    jest: true,
  },
  globals: {
    expect: true,
  },
  root: true,
  extends: ["eslint:recommended", "plugin:@typescript-eslint/recommended"],
  parser: "@typescript-eslint/parser",
  plugins: ["@typescript-eslint"],
  rules: {
    "no-console": 0,
    "@typescript-eslint/no-explicit-any": ["warn"],
    '@typescript-eslint/no-var-requires': 0,
    "padding-line-between-statements": [
      "error",
      { blankLine: "any", prev: "*", next: "*" },
    ],
  },
};
