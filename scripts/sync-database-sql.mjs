import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// Prepare the initial rollout baseline. Once deployed, change only versioned migrations.
export function syncDatabaseSql(root, { check = false } = {}) {
  const sql = path.join(root, 'sql');
  const sources = fs.readdirSync(sql).filter(name => name.endsWith('.sql') && !['ALL_TABLE_CREATE.sql', 'INIT.sql'].includes(name))
    .sort().map(name => ({ name, text: fs.readFileSync(path.join(sql, name), 'utf8').replace(/\r\n?/g, '\n').trim() }))
    .filter(({ text }) => /\bCREATE\s+TABLE\b/i.test(text));
  if (!sources.length) throw new Error('No source table scripts found');
  const schema = '-- ALL_TABLE_CREATE.sql\n-- Generated from individual table scripts under /sql\n-- Frozen after first Flyway rollout; future schema changes require a new versioned migration.\n\n'
    + sources.map(({ name, text }) => `-- ${name}\n${text}\n`).join('\n');
  const seed = fs.readFileSync(path.join(sql, 'INIT.sql'), 'utf8').replace(/\r\n?/g, '\n');
  const outputs = new Map([
    ['sql/ALL_TABLE_CREATE.sql', schema],
    ['deploy-pipline/init/mysql/schema/ALL_TABLE_CREATE.sql', schema],
    ['deploy-pipline/init/mysql/data/INIT.sql', seed],
  ]);
  const drift = [];
  for (const [name, expected] of outputs) {
    const target = path.join(root, name);
    const actual = fs.existsSync(target) ? fs.readFileSync(target, 'utf8').replace(/\r\n?/g, '\n') : null;
    if (actual === expected) continue;
    drift.push(name);
    if (!check) {
      fs.mkdirSync(path.dirname(target), { recursive: true });
      fs.writeFileSync(target, expected, 'utf8');
    }
  }
  if (check && drift.length) throw new Error(`SQL mirror drift: ${drift.join(', ')}`);
  return drift;
}

if (process.argv[1] && path.resolve(process.argv[1]) === fileURLToPath(import.meta.url)) {
  const args = process.argv.slice(2);
  if (args.some(arg => arg !== '--check')) throw new Error('Usage: node scripts/sync-database-sql.mjs [--check]');
  const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
  const changed = syncDatabaseSql(root, { check: args.includes('--check') });
  console.log(`SQL mirrors ${args.includes('--check') ? 'verified' : 'synchronized'} (${changed.length} changed).`);
}
