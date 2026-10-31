// Import only fixture data and original artwork references; discard all mock streams.
const fs = require('fs');
const vm = require('vm');
const ts = require('D:/CineStreamTV_Native/.extractor/node_modules/typescript/lib/typescript.js');
const input = fs.readFileSync('D:/CineStreamTV/src/data/mockCatalog.ts', 'utf8');
const compiled = ts.transpileModule(input, {compilerOptions: {module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2020}}).outputText;
const sandbox = {exports:{}};
vm.runInNewContext(compiled, sandbox, {timeout:1000});
const strip = (x) => Array.isArray(x) ? x.map(strip) : x && typeof x === 'object' ? Object.fromEntries(Object.entries(x).filter(([k])=>k!=='streamSource').map(([k,v])=>[k,strip(v)])) : x;
const titles=strip(sandbox.exports.MOCK_CATALOG);
const rows=sandbox.exports.GET_CATALOG_ROWS().map(r=>({id:r.id,title:r.title,categoryTag:r.categoryTag,ids:r.items.map(m=>m.id)}));
fs.mkdirSync('D:/CineStreamTV/native-app/app/src/main/assets',{recursive:true});
fs.writeFileSync('D:/CineStreamTV/native-app/app/src/main/assets/catalog.json', JSON.stringify({titles,rows}, null, 2));
console.log(`Imported ${titles.length} original design fixtures. Removed every streamSource.`);
