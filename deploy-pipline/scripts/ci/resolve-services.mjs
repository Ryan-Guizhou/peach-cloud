import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const catalog = JSON.parse(fs.readFileSync(path.join(root, 'config/services.json'), 'utf8'));
const serviceOrder = Object.keys(catalog.services);

function args(argv) { const out = {}; for (let i = 0; i < argv.length; i += 1) { const key = argv[i]; if (!key.startsWith('--')) continue; out[key.slice(2)] = argv[i + 1] ?? ''; i += 1; } return out; }
function normalizeSelection(raw) { const requested = [...new Set(String(raw ?? '').split(/[\s,]+/).filter(Boolean))]; const invalid = requested.filter((name) => !catalog.services[name]); if (invalid.length) throw new Error(`Unsupported services: ${invalid.join(', ')}`); return serviceOrder.filter((name) => requested.includes(name)); }
function matches(file, prefixes) { return prefixes.some((prefix) => file === prefix || file.startsWith(prefix)); }
function resolveAuto(changedFiles) {
  if (!changedFiles.length) return [];
  if (changedFiles.some((file) => matches(file, catalog.globalAllPaths ?? []))) return [...serviceOrder];
  const selected = new Set();
  if (changedFiles.some((file) => matches(file, catalog.globalBackendPaths ?? []))) for (const [name, config] of Object.entries(catalog.services)) if (config.type === 'backend') selected.add(name);
  for (const file of changedFiles) for (const [name, config] of Object.entries(catalog.services)) if (matches(file, config.paths ?? [])) selected.add(name);
  return serviceOrder.filter((name) => selected.has(name));
}
function shell(value) { return `'${String(value).replace(/'/g, `'"'"'`)}'`; }

const options = args(process.argv.slice(2));
const mode = String(options.mode ?? 'AUTO').toUpperCase();
let selected;
if (mode === 'ALL') selected = [...serviceOrder];
else if (mode === 'SELECTED') { selected = normalizeSelection(options.selected); if (!selected.length) throw new Error('SELECTED mode requires at least one service.'); }
else if (mode === 'AUTO') { const file = options['changed-file-list']; const changed = file && fs.existsSync(file) ? fs.readFileSync(file, 'utf8').split(/\r?\n/).map((v) => v.trim()).filter(Boolean) : []; selected = resolveAuto(changed); }
else throw new Error(`Unsupported build mode: ${mode}`);

const backend = selected.filter((name) => catalog.services[name].type === 'backend');
const frontend = selected.includes('peach-front');
const mavenProjects = mode === 'ALL' ? '__ALL__' : backend.map((name) => catalog.services[name].module).filter(Boolean).join(',');
if ((options.format ?? 'shell') === 'json') process.stdout.write(`${JSON.stringify({ selectedServices: selected, backendServices: backend, mavenProjects, hasBackend: backend.length > 0, hasFrontend: frontend })}\n`);
else {
  process.stdout.write(`SELECTED_SERVICES=${shell(selected.join(' '))}\n`);
  process.stdout.write(`MAVEN_PROJECTS=${shell(mavenProjects)}\n`);
  process.stdout.write(`HAS_BACKEND=${shell(backend.length > 0 ? 'true' : 'false')}\n`);
  process.stdout.write(`HAS_FRONTEND=${shell(frontend ? 'true' : 'false')}\n`);
}
