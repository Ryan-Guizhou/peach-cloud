#!/usr/bin/env node
import { execFileSync } from 'node:child_process';
import { existsSync, readFileSync } from 'node:fs';
import { dirname, join, normalize } from 'node:path';

function git(args) {
  try { return execFileSync('git', args, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trim(); }
  catch { return ''; }
}

function changedFiles() {
  const base = process.env.DOC_BASE || 'origin/main';
  let out = git(['diff', '--name-only', `${base}...HEAD`]);
  if (!out) out = git(['diff', '--name-only']);
  return new Set(out.split(/\r?\n/).filter(Boolean).map(normalize));
}

function markers(path) {
  const text = readFileSync(path, 'utf8');
  return [...text.matchAll(/<!--\s*doc-sync:([a-zA-Z0-9._-]+)\s*-->/g)].map(m => m[1]);
}

const changed = changedFiles();
const errors = [];
for (const file of changed) {
  if (!file.endsWith('README.md') && !file.endsWith('README.en-US.md')) continue;
  const dir = dirname(file);
  const zh = normalize(join(dir, 'README.md'));
  const en = normalize(join(dir, 'README.en-US.md'));
  if (!existsSync(zh) || !existsSync(en)) continue;
  if (changed.has(zh) !== changed.has(en)) {
    errors.push(`README pair must change together: ${zh} <-> ${en}`);
    continue;
  }
  const zhMarkers = markers(zh);
  const enMarkers = markers(en);
  if ((zhMarkers.length || enMarkers.length) && JSON.stringify(zhMarkers) !== JSON.stringify(enMarkers)) {
    errors.push(`doc-sync markers differ: ${zh} <-> ${en}`);
  }
}

if (errors.length) {
  for (const e of errors) console.error(`[readme-sync] ${e}`);
  process.exit(1);
}
console.log('[readme-sync] README bilingual change check passed.');
