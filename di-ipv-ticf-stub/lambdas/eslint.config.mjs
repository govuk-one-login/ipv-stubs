import globals from "globals";
import parser from "@typescript-eslint/parser";
import tseslint from "typescript-eslint";
import eslint from "@eslint/js";
import prettier from "eslint-config-prettier";
import prettierPlugin from "eslint-plugin-prettier";

export default [
  eslint.configs.recommended,
  ...tseslint.configs.recommended,
  prettier,
  {
    plugins: {
      prettier: prettierPlugin,
    },
    languageOptions: {
      parser: parser,
      ecmaVersion: 2020,
      sourceType: "module",
      globals: {
        ...globals.node,
        ...globals.browser,
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
  {
    ignores: ["lib/**", "coverage/**"],
  },
];
