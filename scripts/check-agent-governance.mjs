#!/usr/bin/env node
import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs';
import { relative, resolve } from 'node:path';

const root = process.cwd();
const fail = [];
const rules = ['java', 'frontend', 'documentation', 'security', 'quality'];
const skills = [
  'project-doc-engineer',
  'using-peach-virtual-thread',
  'using-peach-scheduler',
  'using-peach-storage',
  'using-peach-rocket',
  'using-peach-redis',
  'using-peach-email',
  'using-peach-front',
];
const deprecated = ['using-peach-code-skeleton', 'using-peach-readme-writer'];

function read(path) {
  return readFileSync(resolve(root, path), 'utf8').replace(/\r\n/g, '\n').trim();
}

function stripCursorFrontmatter(text) {
  if (!text.startsWith('---\n')) return text.trim();
  const end = text.indexOf('\n---\n', 4);
  return end < 0 ? text.trim() : text.slice(end + 5).trim();
}

function filesUnder(path) {
  const base = resolve(root, path);
  if (!existsSync(base)) return [];
  const result = [];
  const visit = current => {
    for (const entry of readdirSync(current)) {
      const full = resolve(current, entry);
      if (statSync(full).isDirectory()) visit(full);
      else result.push(relative(base, full).replaceAll('\\', '/'));
    }
  };
  visit(base);
  return result.sort();
}

for (const name of rules) {
  const codex = `.codex/rules/${name}.md`;
  const cursor = `.cursor/rules/${name}.mdc`;
  if (!existsSync(codex)) fail.push(`Missing Codex rule: ${codex}`);
  if (!existsSync(cursor)) fail.push(`Missing Cursor rule: ${cursor}`);
  if (existsSync(codex) && existsSync(cursor) && read(codex) !== stripCursorFrontmatter(read(cursor))) {
    fail.push(`Rule bodies are not synchronized: ${name}`);
  }
}

for (const name of skills) {
  const codexRoot = `.codex/skills/${name}`;
  const cursorRoot = `.cursor/skills/${name}`;
  if (!existsSync(`${codexRoot}/SKILL.md`)) fail.push(`Missing Codex skill: ${codexRoot}/SKILL.md`);
  if (!existsSync(`${cursorRoot}/SKILL.md`)) fail.push(`Missing Cursor skill: ${cursorRoot}/SKILL.md`);

  const codexFiles = filesUnder(codexRoot);
  const cursorFiles = filesUnder(cursorRoot);
  if (JSON.stringify(codexFiles) !== JSON.stringify(cursorFiles)) {
    fail.push(`Skill file sets are not synchronized: ${name}`);
    continue;
  }

  for (const file of codexFiles) {
    if (read(`${codexRoot}/${file}`) !== read(`${cursorRoot}/${file}`)) {
      fail.push(`Skill file is not synchronized: ${name}/${file}`);
    }
  }
}

for (const name of deprecated) {
  for (const base of ['.codex/skills', '.cursor/skills']) {
    if (existsSync(`${base}/${name}`)) fail.push(`Deprecated skill still exists: ${base}/${name}`);
  }
}

const governanceRoots = [
  '.codex/rules',
  '.cursor/rules',
  ...skills.flatMap(name => [`.codex/skills/${name}`, `.cursor/skills/${name}`]),
];
for (const base of governanceRoots) {
  for (const file of filesUnder(base)) {
    const path = `${base}/${file}`;
    const text = read(path);
    if (/scripts\/(?:check-[\w.-]+|verify-changes\.mjs)/.test(text)) {
      fail.push(`Gate command must be owned by AGENTS.md, not ${path}`);
    }
  }
}

if (fail.length) {
  for (const item of fail) console.error(`[governance] ${item}`);
  process.exit(1);
}
console.log('[governance] Codex/Cursor rules, core skill contents, and gate ownership are synchronized.');
