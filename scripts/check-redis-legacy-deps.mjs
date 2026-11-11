#!/usr/bin/env node
import { execFileSync } from 'node:child_process';
import { existsSync, readFileSync } from 'node:fs';

const obsoleteDir = 'peach-middleware/peach-redis/peach-redis-common';
const files = execFileSync('git', ['ls-files', '--cached'], { encoding: 'utf8' })
  .split(/\r?\n/).filter(Boolean);
const failures = [];

if (existsSync(obsoleteDir) || files.some(file => file.startsWith(`${obsoleteDir}/`))) {
  failures.push(`${obsoleteDir}: obsolete mixed-client module must be deleted`);
}
for (const file of files) {
  if (!/\.java$|(^|\/)pom\.xml$|AutoConfiguration\.imports$/.test(file)) continue;
  if (!existsSync(file)) continue;
  const source = readFileSync(file, 'utf8');
  if (source.includes('<artifactId>peach-redis-common</artifactId>') ||
      source.includes('com.peach.redis.common.RedisConfig') ||
      source.includes('com.peach.redis.constant.RedisConstant')) {
    failures.push(`${file}: obsolete Redis Common dependency or class reference`);
  }
}
if (failures.length > 0) {
  console.error('Retired Redis Common references remain:');
  failures.forEach(failure => console.error(`  ${failure}`));
  process.exit(1);
}
console.log('Retired peach-redis-common is absent from tracked modules, POMs and Java source.');
