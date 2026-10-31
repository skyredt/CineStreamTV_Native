const fs = require("fs");
const path = require("path");
const { createRequire } = require("module");
try {
    const sourceDir = path.resolve(process.argv[2]);
    const buildPath = path.resolve(process.argv[3]);
    const projectRequire = createRequire(path.join(sourceDir, "package.json"));
    let ts;
    const compilerCandidates = [
        () => require(path.join(buildPath, ".extractor", "node_modules", "typescript", "lib", "typescript.js")),
        () => projectRequire("typescript/lib/typescript.js"),
        () => projectRequire("typescript")
    ];
    for (const load of compilerCandidates) {
        try {
            const loaded = load();
            const candidate = loaded.default || loaded;
            if (candidate.ScriptTarget && candidate.ScriptKind &&
                typeof candidate.createSourceFile === "function" &&
                typeof candidate.forEachChild === "function" &&
                typeof candidate.isNoSubstitutionTemplateLiteral === "function") {
                ts = candidate;
                break;
            }
        } catch (_) { /* Try the next compiler location. */ }
    }
    if (!ts) {
        throw new Error('A compatible TypeScript JavaScript compiler API was not found. Run: npm.cmd install --prefix "' +
            path.join(buildPath, ".extractor") +
            '" --no-save --package-lock=false --ignore-scripts typescript@5.9.3');
    }
    const input = path.join(sourceDir, "src/data/androidKotlinCodebase.ts");
    const source = ts.createSourceFile(input, fs.readFileSync(input, "utf8"), ts.ScriptTarget.Latest, true, ts.ScriptKind.TS);
    if (source.parseDiagnostics.length) throw new Error(ts.flattenDiagnosticMessageText(source.parseDiagnostics[0].messageText, "\n"));
    const files = [];
    const seen = new Set();
    function visit(node) {
        if (ts.isObjectLiteralExpression(node)) {
            const props = new Map();
            for (const p of node.properties) {
                if (ts.isPropertyAssignment(p) && (ts.isIdentifier(p.name) || ts.isStringLiteral(p.name))) props.set(p.name.text, p.initializer);
            }
            if (props.has("path") && props.has("content")) {
                const literal = x => ts.isStringLiteral(x) || ts.isNoSubstitutionTemplateLiteral(x);
                if (!literal(props.get("path")) || !literal(props.get("content"))) throw new Error("File path and content must be literal strings or template strings without interpolation.");
                const relative = props.get("path").text;
                if (!relative || path.isAbsolute(relative) || relative.includes(":")) throw new Error("Invalid output path: " + relative);
                const target = path.resolve(buildPath, relative);
                const rel = path.relative(buildPath, target);
                if (!rel || rel === ".." || rel.startsWith(".." + path.sep) || path.isAbsolute(rel)) throw new Error("Output path escapes build directory: " + relative);
                const key = target.toLowerCase();
                if (seen.has(key)) throw new Error("Duplicate output path: " + relative);
                seen.add(key);
                files.push({ target, content: props.get("content").text });
            }
        }
        ts.forEachChild(node, visit);
    }
    visit(source);
    if (!files.length) throw new Error("No objects with literal path and content fields found.");
    for (const file of files) {
        fs.mkdirSync(path.dirname(file.target), { recursive: true });
        fs.writeFileSync(file.target, file.content, "utf8");
    }
    console.log("Successfully extracted " + files.length + " project files.");
} catch (error) {
    console.log("Node extraction error: " + error.message);
    process.exitCode = 1;
}
