#!/usr/bin/env node
import { existsSync, readFileSync, readdirSync, statSync } from 'node:fs';
import { relative, resolve } from 'node:path';

const root = process.cwd();
const fail = [];

const requiredSkills = [
  'peach-java-engineering',
  'project-doc-engineer',
  'grill-me',
  'grilling',
  'using-peach-virtual-thread',
  'using-peach-scheduler',
  'using-peach-storage',
  'using-peach-rocket',
  'using-peach-redis',
  'using-peach-email',
  'using-peach-front',
];

const allowedCursorRules = new Set([
  'java.mdc',
  'frontend.mdc',
  'documentation.mdc',
]);

const forbiddenSkillPathPatterns = [
  /\.cursor\/skills\//,
  /\.codex\/skills\//,
  /(?:^|[^\w])~?\/?\.claude\/skills\//,
];

function read(path) {
  return readFileSync(resolve(root, path), 'utf8').replace(/\r\n/g, '\n').trim();
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

if (!existsSync('AGENTS.md')) fail.push('Missing repository AGENTS.md');
if (!existsSync('.agents/README.md')) fail.push('Missing .agents/README.md');
if (!existsSync('.agents/evals/golden-tasks.md')) fail.push('Missing shared golden tasks');

for (const legacy of ['.codex/skills', '.codex/rules', '.cursor/skills']) {
  if (existsSync(legacy)) fail.push(`Legacy duplicated agent directory must not exist: ${legacy}`);
}

for (const name of requiredSkills) {
  const entry = `.agents/skills/${name}/SKILL.md`;
  if (!existsSync(entry)) fail.push(`Missing required shared skill: ${entry}`);
}

const skillRoot = resolve(root, '.agents/skills');
if (existsSync(skillRoot)) {
  for (const entry of readdirSync(skillRoot)) {
    const dir = resolve(skillRoot, entry);
    if (!statSync(dir).isDirectory()) continue;
    if (!existsSync(resolve(dir, 'SKILL.md'))) {
      fail.push(`Skill directory is missing SKILL.md: .agents/skills/${entry}`);
    }
  }

  for (const file of filesUnder('.agents/skills')) {
    const path = `.agents/skills/${file}`;
    const text = read(path);
    for (const pattern of forbiddenSkillPathPatterns) {
      if (pattern.test(text)) {
        fail.push(`Shared skill contains agent-specific skill path: ${path}`);
        break;
      }
    }
  }
}

const cursorRuleRoot = resolve(root, '.cursor/rules');
if (existsSync(cursorRuleRoot)) {
  for (const entry of readdirSync(cursorRuleRoot)) {
    if (!allowedCursorRules.has(entry)) {
      fail.push(`Cursor rule must be a thin routing adapter or be removed: .cursor/rules/${entry}`);
    }
  }

  for (const entry of allowedCursorRules) {
    const path = `.cursor/rules/${entry}`;
    if (!existsSync(path)) {
      fail.push(`Missing Cursor routing adapter: ${path}`);
      continue;
    }
    const text = read(path);
    if (!text.includes('.agents/skills/')) {
      fail.push(`Cursor routing adapter does not reference shared skills: ${path}`);
    }
    if (text.length > 1200) {
      fail.push(`Cursor routing adapter is too large and likely duplicates shared rules: ${path}`);
    }
  }
}

const agents = existsSync('AGENTS.md') ? read('AGENTS.md') : '';
for (const name of ['grill-me', 'peach-java-engineering', 'project-doc-engineer', 'using-peach-front']) {
  if (!agents.includes(`${name}`)) {
    fail.push(`AGENTS.md does not route required skill: ${name}`);
  }
}

if (!agents.includes('Complexity Gate') || !agents.includes('Grill Gate')) {
  fail.push('AGENTS.md is missing the task complexity / Grill work engine gates');
}

for (const path of ['.codex/config.toml', '.cursor/mcp.json']) {
  if (!existsSync(path)) fail.push(`Missing agent runtime adapter: ${path}`);
}

if (fail.length) {
  for (const item of fail) console.error(`[governance] ${item}`);
  process.exit(1);
}

console.log('[governance] shared agent skills, work engine, Cursor adapters, and runtime boundaries are valid.');
