#!/usr/bin/env node
import { execFileSync } from 'node:child_process';
import { existsSync, readFileSync } from 'node:fs';
import { normalize } from 'node:path';

function git(args) {
  try {
    return execFileSync('git', args, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trim();
  } catch {
    return '';
  }
}

function changedJavaFiles() {
  if (process.argv.includes('--all')) {
    return git(['ls-files', '*.java']).split(/\r?\n/).filter(Boolean).map(normalize);
  }

  const requestedBase = process.env.DOC_BASE || 'origin/main';
  const base = git(['rev-parse', '--verify', requestedBase]) ? requestedBase : '';
  let out = base ? git(['diff', '--name-only', `${base}...HEAD`]) : '';
  if (!out) out = git(['diff', '--name-only', '--cached']);
  if (!out) out = git(['diff', '--name-only']);

  return out.split(/\r?\n/)
    .filter(Boolean)
    .map(normalize)
    .filter(file => file.endsWith('.java') && existsSync(file));
}

const declaration = /(?:^|\n)(?:public\s+|protected\s+|private\s+|abstract\s+|final\s+|sealed\s+|non-sealed\s+|static\s+|strictfp\s+)*(@interface|class|interface|enum|record)\s+[A-Za-z_$][\w$]*/m;
const requiredTags = ['@Author', '@Version', '@CreateTime'];
const errors = [];

for (const file of changedJavaFiles()) {
  const text = readFileSync(file, 'utf8');
  const match = declaration.exec(text);
  if (!match) continue;

  const beforeType = text.slice(0, match.index + (match[0].startsWith('\n') ? 1 : 0));
  const comments = [...beforeType.matchAll(/\/\*\*[\s\S]*?\*\//g)];
  const javadoc = comments.length ? comments.at(-1)[0] : '';

  if (!javadoc) {
    errors.push(`${file}: top-level ${match[1]} is missing type Javadoc`);
    continue;
  }

  for (const tag of requiredTags) {
    if (!javadoc.includes(tag)) errors.push(`${file}: type Javadoc is missing ${tag}`);
  }
}

if (errors.length) {
  for (const error of errors) console.error(`[java-style] ${error}`);
  process.exit(1);
}

console.log('[java-style] Changed Java type Javadoc metadata passed.');
