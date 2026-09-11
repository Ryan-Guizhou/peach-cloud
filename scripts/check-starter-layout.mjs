#!/usr/bin/env node
import { existsSync, readFileSync, readdirSync } from 'node:fs';
import { basename, join } from 'node:path';

const roots = ['peach-component', 'peach-middleware'];
const failures = [];
const visited = new Set();
let families = 0;
let starters = 0;
let quickstarts = 0;

function modulesFromPom(pom) {
  return [...pom.matchAll(/<module>\s*([^<]+?)\s*<\/module>/g)].map((match) => match[1].trim());
}

function hasJavaTest(dir) {
  if (!existsSync(dir)) return false;
  return readdirSync(dir, { withFileTypes: true }).some((entry) => {
    const path = join(dir, entry.name);
    return entry.isDirectory() ? hasJavaTest(path) : entry.isFile() && entry.name.endsWith('.java');
  });
}

function quickstartReadmes(dir) {
  if (!existsSync(dir)) return [];
  return readdirSync(dir, { withFileTypes: true })
    .filter((entry) => entry.isFile() && /^README(?:\.[^.]+)*\.md$/i.test(entry.name))
    .map((entry) => entry.name);
}

function scanMavenModule(dir) {
  if (visited.has(dir)) return;
  visited.add(dir);

  const pomPath = join(dir, 'pom.xml');
  if (!existsSync(pomPath)) return;

  const pom = readFileSync(pomPath, 'utf8');
  const modules = modulesFromPom(pom);
  const moduleName = basename(dir).toLowerCase();

  for (const module of modules) {
    const name = basename(module).toLowerCase();
    if (name === 'example' || name.endsWith('-example')) {
      failures.push(`${dir}: non-business Maven module ${module} must use quickstart instead of example`);
    }
  }

  if (!moduleName.endsWith('-quickstart') && /<artifactId>[^<]*-quickstart<\/artifactId>/.test(pom)) {
    failures.push(`${pomPath}: production/starter modules must not depend on a quickstart artifact`);
  }

  const starterModules = modules.filter((module) => module.endsWith('-starter'));
  if (starterModules.length > 0) {
    families += 1;
    if (!existsSync(join(dir, 'README.md')) || !existsSync(join(dir, 'README.en-US.md'))) {
      failures.push(`${dir}: starter family requires README.md and README.en-US.md`);
    }
  }

  for (const starter of starterModules) {
    starters += 1;
    const base = starter.slice(0, -'-starter'.length);
    const autoconfigure = `${base}-autoconfigure`;
    const quickstart = `${base}-quickstart`;
    const starterDir = join(dir, starter);
    const autoconfigureDir = join(dir, autoconfigure);
    const quickstartDir = join(dir, quickstart);

    if (!modules.includes(autoconfigure)) failures.push(`${dir}: ${starter} is missing sibling module ${autoconfigure}`);
    if (!modules.includes(quickstart)) failures.push(`${dir}: ${starter} is missing sibling module ${quickstart}`);
    if (!existsSync(starterDir)) failures.push(`${dir}: module directory does not exist: ${starter}`);
    if (!existsSync(autoconfigureDir)) failures.push(`${dir}: module directory does not exist: ${autoconfigure}`);
    if (!existsSync(quickstartDir)) failures.push(`${dir}: module directory does not exist: ${quickstart}`);

    const quickPomPath = join(quickstartDir, 'pom.xml');
    if (existsSync(quickPomPath)) {
      const quickPom = readFileSync(quickPomPath, 'utf8');
      if (!quickPom.includes(`<artifactId>${starter}</artifactId>`)) {
        failures.push(`${quickPomPath}: quickstart must depend on ${starter}`);
      }
    }

    if (existsSync(quickstartDir)) {
      quickstarts += 1;
      const readmes = quickstartReadmes(quickstartDir);
      if (readmes.length > 0) {
        failures.push(`${quickstartDir}: quickstart documentation belongs to the family root; remove ${readmes.join(', ')}`);
      }
      if (!hasJavaTest(join(quickstartDir, 'src', 'test', 'java'))) {
        failures.push(`${quickstartDir}: quickstart requires at least one Java test under src/test/java`);
      }
    }
  }

  for (const module of modules) {
    scanMavenModule(join(dir, module));
  }
}

for (const root of roots) scanMavenModule(root);

if (failures.length > 0) {
  console.error(`[starter-layout] failed with ${failures.length} issue(s):`);
  for (const failure of failures) console.error(`- ${failure}`);
  process.exit(1);
}

console.log(`[starter-layout] passed: ${families} starter families, ${starters} starter modules, ${quickstarts} quickstarts`);
