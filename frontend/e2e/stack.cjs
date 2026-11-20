/* eslint-disable @typescript-eslint/no-require-imports -- Compartilhado entre o runner ESM e os testes TS carregados em CommonJS. */
const { spawnSync } = require("node:child_process");
const { join } = require("node:path");

const site = "http://localhost:62925";
const mail = "http://localhost:62926";
const composeFile = join(__dirname, "compose.yaml");

function compose(project, ...args) {
  if (!/^trampos-e2e-[a-f0-9]{12}$/.test(project ?? "")) {
    throw new Error("Execute os testes com npm run test:e2e.");
  }
  const result = spawnSync("docker", ["compose", "-f", composeFile, "-p", project, ...args], {
    env: { ...process.env, COMPOSE_DISABLE_ENV_FILE: "1" },
    stdio: "inherit",
    timeout: 300_000,
  });
  if (result.error) throw result.error;
  if (result.status !== 0) throw new Error("Docker Compose falhou: " + args.join(" "));
}


module.exports = { compose, site, mail };
