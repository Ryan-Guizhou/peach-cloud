#!/usr/bin/env node
import { existsSync, readFileSync, readdirSync } from 'node:fs';
import { join } from 'node:path';

const failures = [];
const projects = new Map();
let applications = 0;

function value(xml, tag, fallback = '') {
  return xml.match(new RegExp(`<${tag}>\\s*([^<]+?)\\s*</${tag}>`))?.[1] ?? fallback;
}

function javaFiles(dir) {
  if (!existsSync(dir)) return [];
  return readdirSync(dir, { withFileTypes: true }).flatMap(entry => {
    const path = join(dir, entry.name);
    return entry.isDirectory() ? javaFiles(path) : path.endsWith('.java') ? [path] : [];
  });
}

function visit(dir) {
  const pomPath = join(dir, 'pom.xml');
  const pom = readFileSync(pomPath, 'utf8').replace(/<!--[\s\S]*?-->/g, '');
  // These reactor modules declare their production dependencies directly. Do not
  // mistake dependencyManagement, plugin dependencies or profiles for runtime edges.
  const direct = pom.replace(/<(dependencyManagement|build|profiles|parent)>[\s\S]*?<\/\1>/g, '');
  const header = direct.split('<dependencies>')[0];
  const id = value(header, 'artifactId');
  const group = value(header, 'groupId', 'com.peach');
  const dependencies = [...direct.matchAll(/<dependency>([\s\S]*?)<\/dependency>/g)]
    .map(([, xml]) => ({
      id: `${value(xml, 'groupId')}:${value(xml, 'artifactId')}`,
      optional: value(xml, 'optional') === 'true',
      scope: value(xml, 'scope', 'compile'),
      exclusions: [...xml.matchAll(/<exclusion>([\s\S]*?)<\/exclusion>/g)]
        .map(([, exclusion]) => `${value(exclusion, 'groupId')}:${value(exclusion, 'artifactId')}`),
    }));
  projects.set(`${group}:${id}`, { dir, dependencies });

  for (const file of javaFiles(join(dir, 'src/main/java'))) {
    const source = readFileSync(file, 'utf8').replace(/\/\*[\s\S]*?\*\//g, '').replace(/\/\/[^\r\n]*/g, '');
    if (!/@SpringBootApplication\b/.test(source)) continue;
    applications++;
    if (/@ComponentScan\b/.test(source)) {
      failures.push(`${file}: application-level @ComponentScan overrides Boot filters; use @SpringBootApplication(scanBasePackages = ...)`);
    }
  }
  for (const [, module] of pom.matchAll(/<module>\s*([^<]+?)\s*<\/module>/g)) visit(join(dir, module));
}

function closure(id, exclusions = new Set(), visited = new Set()) {
  const result = new Set();
  const key = `${id}|${[...exclusions].sort().join(',')}`;
  if (visited.has(key)) return result;
  visited.add(key);
  for (const dependency of projects.get(id)?.dependencies ?? []) {
    if (dependency.optional || !['compile', 'runtime'].includes(dependency.scope)) continue;
    const [group, artifact] = dependency.id.split(':');
    if (exclusions.has(dependency.id) || exclusions.has(`${group}:*`)
        || exclusions.has(`*:${artifact}`) || exclusions.has('*:*')) continue;
    result.add(dependency.id);
    for (const transitive of closure(dependency.id,
      new Set([...exclusions, ...dependency.exclusions]), visited)) result.add(transitive);
  }
  return result;
}

visit('.');

const requirements = {
  'peach-redis-starter': ['peach-redis-autoconfigure'],
  'peach-redis-tool-starter': ['peach-redis-tool-autoconfigure', 'peach-redis-starter'],
  'peach-redis-multicache-starter': ['peach-redis-multicache-autoconfigure', 'peach-redis-tool-starter',
    'peach-virtual-thread-starter', 'com.github.ben-manes.caffeine:caffeine', 'cn.hutool:hutool-all',
    'com.fasterxml.jackson.core:jackson-databind'],
  'peach-redis-stream-starter': ['peach-redis-stream-autoconfigure', 'peach-redis-starter'],
  'peach-redisson-starter': ['peach-redisson-autoconfigure', 'org.redisson:redisson'],
  'peach-redission-distributedlock-starter': ['peach-redission-distributedlock-autoconfigure',
    'peach-redisson-starter', 'peach-redission-common', 'org.springframework.boot:spring-boot-starter-aop',
    'org.apache.commons:commons-lang3'],
  'peach-redission-repeat-starter': ['peach-redission-repeat-autoconfigure', 'peach-redission-distributedlock-starter'],
  'peach-redission-bloomfilter-starter': ['peach-redission-bloomfilter-autoconfigure', 'peach-redisson-starter'],
  'peach-redission-delayqueue-starter': ['peach-redission-delayqueue-autoconfigure', 'peach-redisson-starter',
    'peach-initialize-starter', 'peach-virtual-thread-starter'],
};

for (const [starter, required] of Object.entries(requirements)) {
  const reachable = closure(`com.peach:${starter}`);
  const foundation = starter.startsWith('peach-redis-')
    ? ['redis.clients:jedis', 'org.springframework.boot:spring-boot-starter-data-redis'] : ['org.redisson:redisson'];
  for (const artifact of [...required, ...foundation]) {
    const coordinate = artifact.includes(':') ? artifact : `com.peach:${artifact}`;
    if (!reachable.has(coordinate)) failures.push(`${starter}: missing non-optional runtime dependency path to ${coordinate}`);
  }
}

for (const [id, project] of projects) {
  if (!/com\.peach:peach-(redis|redission|redisson)/.test(id)) continue;
  if (id.endsWith('-autoconfigure')) {
    for (const dependency of project.dependencies) {
      if (dependency.id.startsWith('com.peach:') && dependency.id.endsWith('-starter')
          && dependency.id !== 'com.peach:peach-initialize-starter') {
        failures.push(`${id}: autoconfigure must reference contracts/autoconfigure rather than ${dependency.id}`);
      }
    }
    const importsPath = join(project.dir, 'src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports');
    if (!existsSync(importsPath)) {
      failures.push(`${id}: missing AutoConfiguration.imports`);
    } else {
      const imports = readFileSync(importsPath, 'utf8').split(/\r?\n/).map(line => line.trim())
        .filter(line => line && !line.startsWith('#'));
      if (!imports.length) failures.push(`${id}: empty AutoConfiguration.imports`);
      for (const type of imports) {
        const source = join(project.dir, 'src/main/java', ...type.split('.')) + '.java';
        if (!existsSync(source) || !/@AutoConfiguration\b/.test(readFileSync(source, 'utf8'))) {
          failures.push(`${importsPath}: ${type} must resolve to an @AutoConfiguration class`);
        }
      }
    }
  }
  const runtime = closure(id);
  if (id.startsWith('com.peach:peach-redis-') && runtime.has('org.redisson:redisson')) {
    failures.push(`${id}: Redis capabilities must not pull in Redisson`);
  }
  if (/com\.peach:peach-(redisson|redission)-/.test(id)
      && runtime.has('org.springframework.boot:spring-boot-starter-data-redis')) {
    failures.push(`${id}: Redisson capabilities must not pull in Spring Data Redis`);
  }
}

if (failures.length) {
  failures.forEach(failure => console.error(`[spring-wiring] ${failure}`));
  process.exit(1);
}
console.log(`[spring-wiring] ${applications} application scan boundaries and ${Object.keys(requirements).length} Redis/Redisson Starter dependency contracts passed.`);
