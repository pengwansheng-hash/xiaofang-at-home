import { existsSync, readFileSync } from "node:fs";
import { resolve } from "node:path";

let loaded = false;

function parseEnvFile(text: string) {
  text.split(/\r?\n/).forEach((line) => {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) {
      return;
    }

    const separatorIndex = trimmed.indexOf("=");
    if (separatorIndex <= 0) {
      return;
    }

    const key = trimmed.slice(0, separatorIndex).trim();
    let value = trimmed.slice(separatorIndex + 1).trim();
    if (
      (value.startsWith("\"") && value.endsWith("\"")) ||
      (value.startsWith("'") && value.endsWith("'"))
    ) {
      value = value.slice(1, -1);
    }

    if (!(key in process.env)) {
      process.env[key] = value;
    }
  });
}

export function ensureLocalEnvLoaded() {
  if (loaded) {
    return;
  }

  const candidates = [".env.local", ".env"];
  candidates.forEach((name) => {
    const filePath = resolve(process.cwd(), name);
    if (existsSync(filePath)) {
      parseEnvFile(readFileSync(filePath, "utf8"));
    }
  });

  loaded = true;
}
