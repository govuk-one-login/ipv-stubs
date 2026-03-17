const globals = require("globals");
const parser = require("@typescript-eslint/parser");
const tseslint = require("typescript-eslint");
const eslint = require("@eslint/js");

module.exports = [
  {
    // This ignore should be removed once migrated to ES module
    // and this file should be renamed to 'eslint.config.mjs'
    ignores: ["eslint.config.cjs"],
  },
  eslint.configs.recommended,
  ...tseslint.configs.recommended,
  {
    languageOptions: {
      parser: parser,
      ecmaVersion: 2020,
      sourceType: "module",
      globals: {
        ...globals.node,
        ...globals.browser,
        ...globals.jest,
        expect: true,
      },
    },
    rules: {
      "no-console": "off",
      "@typescript-eslint/no-explicit-any": "error",
      "@typescript-eslint/no-var-requires": "off",
      "padding-line-between-statements": [
        "error",
        {
          blankLine: "any",
          prev: "*",
          next: "*",
        },
      ],
    },
  },
];
