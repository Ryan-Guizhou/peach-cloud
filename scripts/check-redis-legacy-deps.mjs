#!/usr/bin/env node
import { execFileSync } from 'node:child_process';
import { readFileSync } from 'node:fs';

const allowed = new Set([
  'peach-dependencies/pom.xml',
  'peach-middleware/peach-redis/peach-redis-common/pom.xml',
]);
const paths = execFileSync('git', ['ls-files', '**/pom.xml', 'pom.xml'], {
  encoding: 'utf8',
}).split(/\r?\n/).filter(Boolean);
const violations = [];
for (const file of paths) {
  if (allowed.has(file)) continue;
  const pom = readFileSync(file, 'utf8');
  if (/<artifactId>\s*peach-redis-common\s*<\/artifactId>/.test(pom)) {
    violations.push(file);
  }
}
if (violations.length) {
  console.error('Legacy Redis Common must not be a direct project dependency:');
  violations.forEach(file => console.error('  ' + file));
  console.error('Use peach-redis-starter and/or peach-redisson-starter instead.');
  process.exit(1);
}
console.log('No remaining direct legacy Redis Common consumers.');
