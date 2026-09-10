#!/usr/bin/env node
import { spawnSync } from 'node:child_process';

const commands = [
  ['node', ['scripts/check-utf8.mjs']],
  ['node', ['scripts/check-agent-governance.mjs']],
  ['node', ['scripts/check-starter-layout.mjs']],
  ['node', ['scripts/check-readme-sync.mjs']],
  ['node', ['scripts/check-docs.mjs']],
  ['git', ['diff', '--check']],
];

for (const [cmd, args] of commands) {
  console.log(`\n[verify] ${cmd} ${args.join(' ')}`);
  const result = spawnSync(cmd, args, { stdio: 'inherit', shell: process.platform === 'win32' });
  if (result.status !== 0) process.exit(result.status ?? 1);
}

console.log('\n[verify] repository gates passed. Run the affected Maven/npm behavior checks when source behavior changed.');
