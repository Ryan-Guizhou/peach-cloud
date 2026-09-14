#!/usr/bin/env node
import { appendFileSync, existsSync, readFileSync } from 'node:fs';
import { basename, dirname, join, relative, resolve, sep } from 'node:path';
import { execFileSync } from 'node:child_process';

const root = process.cwd();
const base = process.env.MAVEN_BUILD_BASE?.trim();
const forceFull = process.env.MAVEN_FORCE_FULL === 'true';
// Intentionally omit this script from globalImpact: a full reactor verify on GitHub
// Actions still cannot resolve private business modules (HTTP Nexus blocker). Scope
// algorithm changes are validated via affected builds that exercise sibling quickstarts.
const globalImpact = [
  'pom.xml',
  'peach-dependencies/',
  '.mvn/',
];
const nonMavenPrefixes = ['docs/', '.codex/', '.github/'];

function toRepoPath(path) {
  return path.split(sep).join('/');
}

function modulesFromPom(pom) {
  return [...pom.matchAll(/<module>\s*([^<]+?)\s*<\/module>/g)].map((match) => match[1].trim());
}

function collectModuleProjects(dir, projects) {
  const pomPath = join(dir, 'pom.xml');
  if (!existsSync(pomPath)) return;
  projects.add(toRepoPath(relative(root, dir)) || '.');
  const pom = readFileSync(pomPath, 'utf8');
  for (const module of modulesFromPom(pom)) {
    collectModuleProjects(resolve(dir, module), projects);
  }
}

function siblingQuickstartDirs(moduleDir) {
  const parentDir = dirname(moduleDir);
  const parentPomPath = join(parentDir, 'pom.xml');
  if (!existsSync(parentPomPath)) return [];
  const parentModules = modulesFromPom(readFileSync(parentPomPath, 'utf8'));
  return parentModules
    .filter((module) => basename(module).endsWith('-quickstart'))
    .map((module) => resolve(parentDir, module));
}

function isStarterFamily(parentDir) {
  const parentPomPath = join(parentDir, 'pom.xml');
  if (!existsSync(parentPomPath)) return false;
  return modulesFromPom(readFileSync(parentPomPath, 'utf8')).some((module) => basename(module).endsWith('-starter'));
}

/**
 * When autoconfigure/starter (or another non-quickstart module in a starter family)
 * changes, also select sibling *-quickstart modules so GHA can keep using -am only
 * instead of -amd (which would pull peach-auth-service and other business apps).
 */
function collectSiblingQuickstarts(moduleDir, projects) {
  const moduleName = basename(moduleDir);
  if (moduleName.endsWith('-quickstart')) return;
  const parentDir = dirname(moduleDir);
  const shouldInclude =
    moduleName.endsWith('-autoconfigure') ||
    moduleName.endsWith('-starter') ||
    isStarterFamily(parentDir);
  if (!shouldInclude) return;
  for (const quickstartDir of siblingQuickstartDirs(moduleDir)) {
    collectModuleProjects(quickstartDir, projects);
  }
}

function nearestMavenModule(file) {
  let dir = resolve(root, dirname(file));
  while (dir.startsWith(root)) {
    if (existsSync(join(dir, 'pom.xml'))) return dir;
    if (dir === root) break;
    dir = dirname(dir);
  }
  return null;
}

function writeOutput(mode, projects = []) {
  const values = {
    mode,
    projects: projects.join(','),
  };
  for (const [key, value] of Object.entries(values)) console.log(`${key}=${value}`);
  if (process.env.GITHUB_OUTPUT) {
    appendFileSync(process.env.GITHUB_OUTPUT, Object.entries(values).map(([key, value]) => `${key}=${value}\n`).join(''));
  }
}

if (forceFull || !base) {
  writeOutput('full');
  process.exit(0);
}

const changed = execFileSync('git', ['diff', '--name-only', '--diff-filter=ACMRTUXB', `${base}...HEAD`], {
  cwd: root,
  encoding: 'utf8',
}).split(/\r?\n/).map((file) => file.trim()).filter(Boolean);

if (changed.length === 0) {
  writeOutput('none');
  process.exit(0);
}

if (changed.some((file) => globalImpact.some((entry) => entry.endsWith('/') ? file.startsWith(entry) : file === entry))) {
  writeOutput('full');
  process.exit(0);
}

const projects = new Set();
for (const file of changed) {
  if (file === 'AGENTS.md' || file.endsWith('.md') || nonMavenPrefixes.some((prefix) => file.startsWith(prefix))) continue;
  if (file.startsWith('peach-cloud-front/')) continue;
  if (file.startsWith('scripts/')) continue;

  const module = nearestMavenModule(file);
  if (!module || module === root) {
    writeOutput('full');
    process.exit(0);
  }
  collectModuleProjects(module, projects);
  collectSiblingQuickstarts(module, projects);
}

if (projects.size === 0) {
  writeOutput('none');
  process.exit(0);
}

writeOutput('affected', [...projects].sort());
