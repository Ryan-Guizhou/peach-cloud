#!/usr/bin/env node
import { execFileSync } from 'node:child_process';
import { existsSync, readFileSync } from 'node:fs';
import { dirname, join, normalize } from 'node:path';

function git(args) {
  try { return execFileSync('git', args, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trim(); }
  catch { return ''; }
}

const requestedBase = process.env.DOC_BASE || 'origin/main';
const resolvedBase = git(['rev-parse', '--verify', requestedBase]) ? requestedBase : '';

function changedFiles() {
  let out = resolvedBase ? git(['diff', '--name-only', `${resolvedBase}...HEAD`]) : '';
  if (!out) out = git(['diff', '--name-only']);
  return new Set(out.split(/\r?\n/).filter(Boolean).map(normalize));
}

function existedAtBase(path) {
  if (!resolvedBase) return null;
  try {
    execFileSync('git', ['cat-file', '-e', `${resolvedBase}:${path.replaceAll('\\', '/')}`], { stdio: 'ignore' });
    return true;
  } catch {
    return false;
  }
}

function markers(path) {
  const text = readFileSync(path, 'utf8');
  return [...text.matchAll(/<!--\s*doc-sync:([a-zA-Z0-9._-]+)\s*-->/g)].map((match) => match[1]);
}

const changed = changedFiles();
const errors = [];

for (const file of changed) {
  if (!file.endsWith('README.md') && !file.endsWith('README.en-US.md')) continue;

  const dir = dirname(file);
  const zh = normalize(join(dir, 'README.md'));
  const en = normalize(join(dir, 'README.en-US.md'));
  if (!existsSync(zh) || !existsSync(en)) continue;

  const zhChanged = changed.has(zh);
  const enChanged = changed.has(en);

  if (zhChanged !== enChanged) {
    const zhExisted = existedAtBase(zh);
    const enExisted = existedAtBase(en);
    const completingMissingPair =
      (zhChanged && zhExisted === false && enExisted === true) ||
      (enChanged && enExisted === false && zhExisted === true);

    if (!completingMissingPair) {
      errors.push(`README pair must change together: ${zh} <-> ${en}`);
      continue;
    }
  }

  const zhMarkers = markers(zh);
  const enMarkers = markers(en);
  if ((zhMarkers.length || enMarkers.length) && JSON.stringify(zhMarkers) !== JSON.stringify(enMarkers)) {
    errors.push(`doc-sync markers differ: ${zh} <-> ${en}`);
  }
}

if (errors.length) {
  for (const error of errors) console.error(`[readme-sync] ${error}`);
  process.exit(1);
}

console.log('[readme-sync] README bilingual change check passed.');
