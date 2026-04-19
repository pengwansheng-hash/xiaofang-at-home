import { existsSync, mkdirSync, readFileSync, writeFileSync } from "node:fs";
import { dirname, resolve } from "node:path";
import { defaultStore } from "./default-store.js";
import type { ServiceStore } from "./types.js";

const storePath = resolve(process.cwd(), "data", "store.json");

function normalizeStore(raw: Partial<ServiceStore> | null | undefined): ServiceStore {
  return {
    bindings: Array.isArray(raw?.bindings) ? raw!.bindings : [],
    profiles: Array.isArray(raw?.profiles) ? raw!.profiles : [],
    carePlans: Array.isArray(raw?.carePlans) ? raw!.carePlans : [],
    carePlanEvents: Array.isArray(raw?.carePlanEvents) ? raw!.carePlanEvents : [],
    topicBriefs: Array.isArray(raw?.topicBriefs) ? raw!.topicBriefs : [],
    semanticMemories: Array.isArray(raw?.semanticMemories) ? raw!.semanticMemories : [],
  };
}

function ensureStoreFile() {
  if (existsSync(storePath)) {
    return;
  }

  mkdirSync(dirname(storePath), { recursive: true });
  writeFileSync(storePath, JSON.stringify(defaultStore, null, 2), "utf8");
}

export function readStore(): ServiceStore {
  ensureStoreFile();
  return normalizeStore(JSON.parse(readFileSync(storePath, "utf8")) as Partial<ServiceStore>);
}

export function writeStore(nextStore: ServiceStore) {
  ensureStoreFile();
  writeFileSync(storePath, JSON.stringify(nextStore, null, 2), "utf8");
}
