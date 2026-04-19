const fs = require("fs");
const path = require("path");
const vm = require("vm");

function loadTypeScript() {
  const tsPath = path.resolve("node_modules/typescript/lib/typescript.js");
  const source = fs.readFileSync(tsPath, "utf8");
  const sandbox = {
    module: { exports: {} },
    exports: {},
    require: (name) => require(name),
    process,
    console,
    setTimeout,
    clearTimeout,
    setInterval,
    clearInterval,
    Buffer,
    __dirname: path.dirname(tsPath),
    __filename: tsPath,
  };
  sandbox.exports = sandbox.module.exports;
  vm.runInNewContext(source, sandbox, { filename: tsPath });
  return sandbox.module.exports;
}

function printDiagnostics(ts, diagnostics) {
  if (diagnostics.length === 0) {
    return;
  }

  const host = {
    getCanonicalFileName: (fileName) => fileName,
    getCurrentDirectory: () => process.cwd(),
    getNewLine: () => ts.sys.newLine,
  };
  console.error(ts.formatDiagnosticsWithColorAndContext(diagnostics, host));
}

(function main() {
  const ts = loadTypeScript();
  const configPath = path.resolve("tsconfig.json");
  const configFile = ts.readConfigFile(configPath, ts.sys.readFile);

  if (configFile.error) {
    printDiagnostics(ts, [configFile.error]);
    process.exit(1);
  }

  const parsed = ts.parseJsonConfigFileContent(configFile.config, ts.sys, path.dirname(configPath));
  const program = ts.createProgram({
    rootNames: parsed.fileNames,
    options: parsed.options,
  });
  const emitResult = program.emit();
  const diagnostics = ts.getPreEmitDiagnostics(program).concat(emitResult.diagnostics);

  if (diagnostics.length > 0) {
    printDiagnostics(ts, diagnostics);
    process.exit(1);
  }

  console.log("BUILD SUCCESSFUL");
})();
