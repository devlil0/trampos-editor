import { defineConfig, globalIgnores } from "eslint/config";
import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";

const eslintConfig = defineConfig([
  ...nextVitals,
  ...nextTs,
  {
    rules: {
      // Server Action tem assinatura fixa `(prevState, formData)`: quem não usa o estado
      // anterior ainda precisa declará-lo pra receber o formData. O prefixo `_` já é a
      // convenção do projeto pra "existe porque a assinatura exige" — aqui a regra passa
      // a lê-la, em vez de acusar todo action que ignora um dos dois.
      "@typescript-eslint/no-unused-vars": [
        "warn",
        { argsIgnorePattern: "^_", varsIgnorePattern: "^_", caughtErrors: "none" },
      ],
    },
  },
  // Override default ignores of eslint-config-next.
  globalIgnores([
    // Default ignores of eslint-config-next:
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
  ]),
]);

export default eslintConfig;
