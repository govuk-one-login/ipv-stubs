module.exports = {
    env: {
      node: true,
      es6: true,
      es2020: true,
      mocha: true,
      browser: true
    },
    globals: {
      sinon: true,
      expect: true,
    },
    root: true,
    extends: ["eslint:recommended", "prettier"],
    rules: {
      "no-console": 2,
      "padding-line-between-statements": [
        "error",
        { blankLine: "any", prev: "*", next: "*" },
      ],
    },
  };

