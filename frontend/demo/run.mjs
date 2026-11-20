import { spawnSync } from "node:child_process";
import { mkdtempSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { fileURLToPath } from "node:url";

const root = fileURLToPath(new URL("../../", import.meta.url));
const base = fileURLToPath(new URL("../e2e/compose.yaml", import.meta.url));
const override = fileURLToPath(new URL("./compose.yaml", import.meta.url));
const release = "f6120e9db208f4dfa5088bfc1cc304d22ad25fef";
const project = "trampos-demo-local-t26";
const env = { ...process.env, COMPOSE_DISABLE_ENV_FILE: "1" };
delete env.DOCKER_HOST;
delete env.DOCKER_TLS_VERIFY;
delete env.DOCKER_CERT_PATH;
delete env.COMPOSE_ENV_FILES;
let source = root;

function run(command, args, capture = false) {
  const result = spawnSync(command, args, {
    cwd: root,
    env: { ...env, TRAMPOS_DEMO_SOURCE: source },
    encoding: "utf8",
    stdio: capture ? ["ignore", "pipe", "inherit"] : "inherit",
    timeout: 600_000,
  });
  if (result.error) throw result.error;
  if (result.status !== 0) throw new Error(`Falha em ${command} (saída ${result.status}).`);
  return result.stdout?.trim();
}

function compose(...args) {
  return run("docker", ["compose", "-f", base, "-f", override, "-p", project, ...args], args[0] === "ps");
}

function build() {
  const staging = mkdtempSync(join(tmpdir(), "trampos-demo-"));
  try {
    const archive = join(staging, "release.tar");
    run("git", ["archive", "--format=tar", "--output", archive, release, "--",
      "Dockerfile", ".dockerignore", "pom.xml", "src/main", "frontend/Dockerfile", "frontend/.dockerignore",
      "frontend/package.json", "frontend/package-lock.json", "frontend/next.config.ts",
      "frontend/postcss.config.mjs", "frontend/tsconfig.json", "frontend/src", "frontend/public"]);
    run("tar", ["-xf", archive, "-C", staging]);
    source = staging;
    compose("pull", "db", "mailpit");
    compose("build", "api", "web");
  } finally {
    source = root;
    rmSync(staging, { recursive: true, force: true });
  }
}

async function main() {
  const [action, portfolioUrl, extra] = process.argv.slice(2);
  if (!["prepare", "reset", "stop", "rehearse"].includes(action) || extra ||
      (portfolioUrl && !["prepare", "reset"].includes(action))) {
    throw new Error("Uso: node frontend/demo/run.mjs <prepare|reset|stop|rehearse> [URL de vídeo autorizada]");
  }
  const context = run("docker", ["context", "show"], true);
  const endpoint = run("docker", ["context", "inspect", context, "--format", "{{.Endpoints.docker.Host}}"], true);
  if (!/^(?:npipe:|unix:)/.test(endpoint)) {
    throw new Error("Selecione um contexto Docker local para a demonstração.");
  }
  env.DOCKER_CONTEXT = context;

  if (action === "stop") {
    compose("down");
    return;
  }

  const { fixture, rehearse, validatePortfolio } = await import("./scenario.mjs");
  validatePortfolio(portfolioUrl);
  const containers = compose("ps", "--all", "--quiet");
  if (action === "rehearse") {
    if (!containers) throw new Error("Prepare o ambiente local antes do ensaio.");
    await rehearse();
    return;
  }

  if (action === "prepare") {
    if (containers) throw new Error("O ambiente já existe. Use reset para refazer os dados ou stop para remover a stack.");
    console.log(`Construindo as imagens da revisão ${release}.`);
    build();
  } else {
    compose("down");
  }
  compose("up", "-d", "--wait", "--wait-timeout", "150", "--no-build", "--pull", "never");
  await fixture(portfolioUrl);
  console.log("Demonstração pronta em http://localhost:62935");
}

main().catch((error) => {
  console.error(error.message);
  process.exitCode = 1;
});
