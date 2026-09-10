#!/usr/bin/env node
import { existsSync, readFileSync } from 'node:fs';
import { resolve } from 'node:path';

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
  const codex = `.codex/skills/${name}/SKILL.md`;
  const cursor = `.cursor/skills/${name}/SKILL.md`;
  if (!existsSync(codex)) fail.push(`Missing Codex skill: ${codex}`);
  if (!existsSync(cursor)) fail.push(`Missing Cursor skill: ${cursor}`);
  if (existsSync(codex) && existsSync(cursor) && read(codex) !== read(cursor)) {
    fail.push(`Skill definitions are not synchronized: ${name}`);
  }
}

for (const name of deprecated) {
  for (const base of ['.codex/skills', '.cursor/skills']) {
    if (existsSync(`${base}/${name}`)) fail.push(`Deprecated skill still exists: ${base}/${name}`);
  }
}

if (fail.length) {
  for (const item of fail) console.error(`[governance] ${item}`);
  process.exit(1);
}
console.log('[governance] Codex/Cursor rules and core skills are synchronized.');
