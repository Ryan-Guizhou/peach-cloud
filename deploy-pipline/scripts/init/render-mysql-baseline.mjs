import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';

function parseArgs(argv) {
  const args = {};
  for (let index = 0; index < argv.length; index += 1) {
    const key = argv[index];
    if (!key.startsWith('--')) continue;
    args[key.slice(2)] = argv[index + 1] ?? '';
    index += 1;
  }
  return args;
}

function requiredFile(args, name) {
  const value = args[name];
  if (!value) throw new Error(`Missing --${name}.`);
  const resolved = path.resolve(value);
  if (!fs.existsSync(resolved)) throw new Error(`Missing file for --${name}: ${resolved}`);
  return resolved;
}

const args = parseArgs(process.argv.slice(2));
const schemaPath = requiredFile(args, 'schema');
const existingPath = requiredFile(args, 'existing');
const outputPath = path.resolve(args.output || '');
const metadataPath = path.resolve(args.metadata || '');
if (!args.output || !args.metadata) throw new Error('Both --output and --metadata are required.');

const schema = fs.readFileSync(schemaPath, 'utf8').replace(/\r\n/g, '\n');
const existing = new Set(
  fs.readFileSync(existingPath, 'utf8')
    .split(/\r?\n/)
    .map((value) => value.trim().replace(/^`|`$/g, '').toUpperCase())
    .filter(Boolean),
);

const matches = [...schema.matchAll(/^\s*CREATE\s+TABLE\s+(?:IF\s+NOT\s+EXISTS\s+)?`?([A-Z0-9_]+)`?/gmi)];
if (matches.length === 0) throw new Error(`No CREATE TABLE statements found in ${schemaPath}`);

const segments = matches.map((match, index) => {
  const table = match[1].toUpperCase();
  const next = matches[index + 1];
  const sql = schema.slice(match.index, next ? next.index : schema.length).trim();
  return { table, sql };
});

const seen = new Set();
for (const segment of segments) {
  if (seen.has(segment.table)) throw new Error(`Duplicate CREATE TABLE definition: ${segment.table}`);
  seen.add(segment.table);
}

const expectedTables = segments.map(({ table }) => table);
const missing = segments.filter(({ table }) => !existing.has(table));
const missingTables = missing.map(({ table }) => table);
const output = missing.length > 0
  ? `${missing.map(({ sql }) => sql).join('\n\n')}\n`
  : '-- All baseline tables already exist.\n';
const metadata = {
  version: 'baseline-v1',
  checksum: crypto.createHash('sha256').update(schema).digest('hex'),
  expectedTables,
  missingTables,
};

for (const target of [outputPath, metadataPath]) {
  fs.mkdirSync(path.dirname(target), { recursive: true, mode: 0o700 });
}
fs.writeFileSync(outputPath, output, { encoding: 'utf8', mode: 0o600 });
fs.writeFileSync(metadataPath, `${JSON.stringify(metadata, null, 2)}\n`, { encoding: 'utf8', mode: 0o600 });
fs.chmodSync(outputPath, 0o600);
fs.chmodSync(metadataPath, 0o600);
