#!/usr/bin/env node
import { existsSync, readFileSync, statSync } from 'node:fs';
import { dirname, normalize, resolve } from 'node:path';
import { execFileSync } from 'node:child_process';

function git(args) {
  try {
    return execFileSync('git', args, { encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trim();
  } catch {
    return '';
  }
}

function markdownFiles() {
  if (process.argv.includes('--all')) {
    return git(['ls-files', '*.md', '*.mdc']).split(/\r?\n/).filter(Boolean);
  }
  const base = process.env.DOC_BASE || 'origin/main';
  let out = git(['diff', '--name-only', `${base}...HEAD`]);
  if (!out) out = git(['diff', '--name-only', '--cached']);
  if (!out) out = git(['diff', '--name-only']);
  return out.split(/\r?\n/)
    .filter(Boolean)
    .map(normalize)
    .filter(file => file.endsWith('.md') || file.endsWith('.mdc'))
    .filter(file => existsSync(file));
}

const errors = [];
const linkRe = /\[[^\]]+\]\(([^)]+)\)/g;
for (const file of markdownFiles()) {
  const text = readFileSync(file, 'utf8');
  const fences = (text.match(/^```/gm) || []).length;
  if (fences % 2 !== 0) errors.push(`${file}: unbalanced fenced code blocks`);

  for (const match of text.matchAll(linkRe)) {
    let target = match[1].trim();
    if (!target || target.startsWith('#') || /^[a-z][a-z0-9+.-]*:/i.test(target)) continue;
    target = target.split('#')[0].split('?')[0];
    if (!target) continue;
    const path = resolve(dirname(file), decodeURIComponent(target));
    if (!existsSync(path)) errors.push(`${file}: broken relative link -> ${match[1]}`);
    else if (statSync(path).isDirectory() && match[1].includes('#')) errors.push(`${file}: directory link cannot resolve anchor -> ${match[1]}`);
  }

  const mermaidOpen = (text.match(/^```mermaid\s*$/gm) || []).length;
  if (mermaidOpen > 0 && fences < mermaidOpen * 2) errors.push(`${file}: invalid Mermaid fence structure`);
}

if (errors.length) {
  for (const e of errors) console.error(`[docs] ${e}`);
  process.exit(1);
}
console.log('[docs] Changed Markdown fences and relative links passed.');
