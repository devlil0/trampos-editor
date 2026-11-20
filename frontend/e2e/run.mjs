import { randomBytes } from "node:crypto";
import { spawn } from "node:child_process";
import { compose } from "./stack.cjs";

const project = "trampos-e2e-" + randomBytes(6).toString("hex");
try {
  compose(project, "up", "-d", "--build", "--wait", "--wait-timeout", "120");
  const child = spawn(process.execPath, ["node_modules/@playwright/test/cli.js", "test", ...process.argv.slice(2)], {
    stdio: "inherit",
    env: { ...process.env, TRAMPOS_E2E_PROJECT: project },
  });
  const interrupt = () => child.kill();
  process.on("SIGINT", interrupt);
  process.on("SIGTERM", interrupt);
  try {
    process.exitCode = await new Promise((resolve, reject) => {
      child.on("error", reject);
      child.on("exit", (code) => resolve(code ?? 1));
    });
  } finally {
    process.off("SIGINT", interrupt);
    process.off("SIGTERM", interrupt);
  }
} finally {
  compose(project, "down", "--volumes", "--remove-orphans");
}

