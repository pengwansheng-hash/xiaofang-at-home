const fs = require("fs");
const path = require("path");
const vm = require("vm");
const { pathToFileURL } = require("url");

const context = vm.createContext({
  console,
  process,
  Buffer,
  setTimeout,
  clearTimeout,
  setInterval,
  clearInterval,
  queueMicrotask,
  URL,
  URLSearchParams,
  AbortController,
  fetch,
  Headers,
  Request,
  Response,
  FormData,
  performance: globalThis.performance,
  TextEncoder,
  TextDecoder,
});

const cache = new Map();

async function importBuiltin(specifier) {
  const namespace = await import(specifier);
  const exportNames = Object.keys(namespace);
  const module = new vm.SyntheticModule(
    exportNames,
    function initialize() {
      for (const name of exportNames) {
        this.setExport(name, namespace[name]);
      }
    },
    { context, identifier: specifier },
  );

  await module.link(() => {
    throw new Error(`Unexpected nested import for builtin ${specifier}`);
  });
  await module.evaluate();
  return module;
}

async function loadModule(filePath) {
  const resolvedPath = path.resolve(filePath);
  if (cache.has(resolvedPath)) {
    return cache.get(resolvedPath);
  }

  const source = fs.readFileSync(resolvedPath, "utf8");
  const module = new vm.SourceTextModule(source, {
    context,
    identifier: resolvedPath,
    initializeImportMeta(meta) {
      meta.url = pathToFileURL(resolvedPath).href;
    },
    async importModuleDynamically(specifier) {
      return linkModule(specifier, module);
    },
  });

  cache.set(resolvedPath, module);
  await module.link(linkModule);
  return module;
}

async function linkModule(specifier, referencingModule) {
  if (specifier.startsWith("node:")) {
    return importBuiltin(specifier);
  }

  const targetPath = path.resolve(path.dirname(referencingModule.identifier), specifier);
  return loadModule(targetPath);
}

(async () => {
  const serverModule = await loadModule("dist/server.js");
  await serverModule.evaluate();
})().catch((error) => {
  console.error(error);
  process.exit(1);
});
