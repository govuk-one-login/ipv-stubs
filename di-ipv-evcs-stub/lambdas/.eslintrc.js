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
  extends: [
    "eslint:recommended",
    "plugin:@typescript-eslint/recommended",
    "prettier",
  ],
  parser: "@typescript-eslint/parser",
  plugins: ["@typescript-eslint", "eslint-plugin-prettier"],
  rules: {
    // indent: [2, 2, { SwitchCase: 1 }],
    "prettier/prettier": ["error", { endOfLine: "auto" }],
    "no-console": 0,
    "@typescript-eslint/no-explicit-any": ["error"],
    "@typescript-eslint/no-var-requires": 0,
    "padding-line-between-statements": [
      "error",
      { blankLine: "any", prev: "*", next: "*" },
    ],
  },
};
