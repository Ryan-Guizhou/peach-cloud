#!/usr/bin/env node
import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs';
import { relative, resolve } from 'node:path';

const root = process.cwd();
const errors = [];
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
const forbiddenGatePatterns = [
  /node\s+scripts\/verify-changes\.mjs/i,
  /node\s+scripts\/check-[\w.-]+\.mjs/i,
];

function normalizeText(path) {
  return readFileSync(resolve(root, path), 'utf8').replace(/\r\n/g, '\n').trim();
}

function stripCursorFrontmatter(text) {
  if (!text.startsWith('---\n')) return text.trim();
  const end = text.indexOf('\n---\n', 4);
  return end < 0 ? text.trim() : text.slice(end + 5).trim();
}

function walkFiles(baseDir) {
  const absoluteBase = resolve(root, baseDir);
  if (!existsSync(absoluteBase)) return [];
  const result = [];
  const stack = [absoluteBase];
  while (stack.length) {
    const current = stack.pop();
    for (const name of readdirSync(current)) {
      const path = resolve(current, name);
      if (statSync(path).isDirectory()) stack.push(path);
      else result.push(relative(absoluteBase, path).replaceAll('\\', '/'));
    }
  }
  return result.sort();
}

function compareSkillTree(name) {
  const codexRoot = `.codex/skills/${name}`;
  const cursorRoot = `.cursor/skills/${name}`;
  if (!existsSync(codexRoot)) errors.push(`Missing Codex skill: ${codexRoot}`);
  if (!existsSync(cursorRoot)) errors.push(`Missing Cursor skill: ${cursorRoot}`);
  if (!existsSync(codexRoot) || !existsSync(cursorRoot)) return;

  const codexFiles = walkFiles(codexRoot);
  const cursorFiles = walkFiles(cursorRoot);
  if (JSON.stringify(codexFiles) !== JSON.stringify(cursorFiles)) {
    errors.push(`Skill file sets are not synchronized: ${name}`);
    return;
  }
  for (const file of codexFiles) {
    const codexPath = `${codexRoot}/${file}`;
    const cursorPath = `${cursorRoot}/${file}`;
    if (normalizeText(codexPath) !== normalizeText(cursorPath)) {
      errors.push(`Skill file is not synchronized: ${name}/${file}`);
    }
  }
}

function checkGateOwnership(baseDir) {
  for (const file of walkFiles(baseDir)) {
    if (!/\.(md|mdc)$/i.test(file)) continue;
    const path = `${baseDir}/${file}`;
    const text = normalizeText(path);
    for (const pattern of forbiddenGatePatterns) {
      if (pattern.test(text)) {
        errors.push(`Central gate command must stay in AGENTS.md/scripts only: ${path}`);
        break;
      }
    }
  }
}

for (const name of rules) {
  const codex = `.codex/rules/${name}.md`;
  const cursor = `.cursor/rules/${name}.mdc`;
  if (!existsSync(codex)) errors.push(`Missing Codex rule: ${codex}`);
  if (!existsSync(cursor)) errors.push(`Missing Cursor rule: ${cursor}`);
  if (existsSync(codex) && existsSync(cursor)
      && normalizeText(codex) !== stripCursorFrontmatter(normalizeText(cursor))) {
    errors.push(`Rule bodies are not synchronized: ${name}`);
  }
}

for (const name of skills) compareSkillTree(name);

for (const name of deprecated) {
  for (const base of ['.codex/skills', '.cursor/skills']) {
    if (existsSync(`${base}/${name}`)) errors.push(`Deprecated skill still exists: ${base}/${name}`);
  }
}

checkGateOwnership('.codex/rules');
checkGateOwnership('.codex/skills');
checkGateOwnership('.cursor/rules');
checkGateOwnership('.cursor/skills');

if (errors.length) {
  for (const error of errors) console.error(`[governance] ${error}`);
  process.exit(1);
}
console.log('[governance] Rule/Skill mirrors, deprecations and central gate ownership are valid.');
