import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { syncDatabaseSql } from './sync-database-sql.mjs';

test('rollout generation is sorted, repeatable and detects mirror drift without writing', () => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'peach-sql-sync-'));
  try {
    fs.mkdirSync(path.join(root, 'sql'));
    for (const [name, text] of Object.entries({
      'Z.sql': 'CREATE TABLE Z (ID INT);\r\n',
      'A.sql': 'CREATE TABLE A (ID INT);\n',
      'INIT.sql': 'INSERT INTO A VALUES (1);\n',
      'notes.sql': '-- documentation only\n',
      'ALL_TABLE_CREATE.sql': 'old schema',
    })) fs.writeFileSync(path.join(root, 'sql', name), text);
    assert.throws(() => syncDatabaseSql(root, { check: true }), /SQL mirror drift/);
    assert.equal(fs.readFileSync(path.join(root, 'sql/ALL_TABLE_CREATE.sql'), 'utf8'), 'old schema');
    assert.equal(syncDatabaseSql(root).length, 3);
    const schema = fs.readFileSync(path.join(root, 'sql/ALL_TABLE_CREATE.sql'), 'utf8');
    assert.ok(schema.indexOf('-- A.sql') < schema.indexOf('-- Z.sql'));
    assert.ok(!schema.includes('notes.sql'));
    assert.deepEqual(syncDatabaseSql(root, { check: true }), []);
    const mirror = path.join(root, 'deploy-pipline/init/mysql/data/INIT.sql');
    fs.writeFileSync(mirror, 'drift');
    assert.throws(() => syncDatabaseSql(root, { check: true }), /data\/INIT.sql/);
    assert.equal(fs.readFileSync(mirror, 'utf8'), 'drift');
  } finally { fs.rmSync(root, { recursive: true, force: true }); }
});
