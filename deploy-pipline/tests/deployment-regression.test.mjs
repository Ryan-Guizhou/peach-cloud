import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import test from 'node:test';
import { execFileSync, spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const deployRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const read = (relativePath) => fs.readFileSync(path.join(deployRoot, relativePath), 'utf8');

const launchModules = {
  'peach-gateway': 'peach-gateway/peach-gateway-launch',
  'peach-auth': 'peach-auth/peach-auth-launch',
  'peach-monitor': 'peach-monitor/peach-monitor-launch',
  'peach-fileservice': 'peach-fileservice/peach-fileservice-launch',
  'peach-message': 'peach-message/peach-message-launch',
  'peach-setting': 'peach-setting/peach-setting-launch',
  'peach-generator': 'peach-generator/peach-generator-launch',
  'peach-scheduled': 'peach-scheduled/peach-scheduled-launch',
};

function storageEnv(overrides = {}) {
  const env = { ...process.env };
  for (const key of [
    'OSS_ACCESS_KEY', 'OSS_SECRET_KEY',
    'COS_ACCESS_KEY', 'COS_SECRET_KEY',
    'BOS_ACCESS_KEY', 'BOS_SECRET_KEY',
    'OBS_ACCESS_KEY', 'OBS_SECRET_KEY',
  ]) {
    delete env[key];
  }
  return { ...env, ...overrides };
}

test('selective Maven builds target runnable launch modules', () => {
  const catalog = JSON.parse(read('config/services.json'));
  for (const [service, module] of Object.entries(launchModules)) {
    assert.equal(catalog.services[service].module, module, service);
  }
  assert.ok(catalog.globalBackendPaths.includes('.mvn/'));
});

test('scheduler deployment keeps the canonical peach-scheduler discovery and config name', () => {
  const compose = read('compose/application/docker-compose.yml');
  assert.match(
    compose,
    /peach-scheduled:[\s\S]*?SPRING_APPLICATION_NAME:\s*peach-scheduler(?:\s|$)/,
  );
  assert.ok(fs.existsSync(path.join(deployRoot, 'init/nacos/config/peach-scheduler.yml')));
  assert.ok(!fs.existsSync(path.join(deployRoot, 'init/nacos/config/peach-scheduled.yml')));
});

test('storage rendering defaults to local and omits cloud providers without complete credentials', () => {
  const renderer = path.join(deployRoot, 'scripts/config/render-nacos-config.mjs');
  const template = path.join(deployRoot, 'init/nacos/config/peach-store.yml');
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'peach-store-'));
  const localOutput = path.join(tmp, 'local.yml');

  execFileSync('node', [renderer, '--source', template, '--output', localOutput], {
    env: storageEnv({ PEACH_STORAGE_PRIMARY: 'local' }),
  });
  const local = fs.readFileSync(localOutput, 'utf8');
  assert.match(local, /primary:\s*local/);
  assert.match(local, /^\s{4}local:/m);
  for (const provider of ['aliyun', 'cos', 'bos', 'obs']) {
    assert.doesNotMatch(local, new RegExp(`^\\s{4}${provider}:`, 'm'));
  }
  assert.equal(fs.statSync(localOutput).mode & 0o777, 0o600);

  const ossOutput = path.join(tmp, 'oss.yml');
  execFileSync('node', [renderer, '--source', template, '--output', ossOutput], {
    env: storageEnv({
      PEACH_STORAGE_PRIMARY: 'aliyun',
      OSS_ACCESS_KEY: 'access-key',
      OSS_SECRET_KEY: 'secret"with\\special',
    }),
  });
  const oss = fs.readFileSync(ossOutput, 'utf8');
  assert.match(oss, /primary:\s*aliyun/);
  assert.match(oss, /^\s{4}aliyun:/m);
  assert.match(oss, /secret\\"with\\\\special/);

  const partial = spawnSync('node', [renderer, '--source', template, '--output', path.join(tmp, 'partial.yml')], {
    env: storageEnv({ PEACH_STORAGE_PRIMARY: 'local', COS_ACCESS_KEY: 'only-one-half' }),
    encoding: 'utf8',
  });
  assert.notEqual(partial.status, 0);
  assert.match(partial.stderr, /Incomplete COS credentials/);
});

test('runtime reconciliation reapplies Compose definitions and only replaces legacy runtime containers', () => {
  const script = read('scripts/bootstrap/start-runtime.sh');
  assert.doesNotMatch(script, /docker\s+start\s+"?\$container/);
  assert.match(script, /com\.docker\.compose\.project/);
  assert.match(script, /docker\s+rm\s+-f\s+"\$container"/);
  assert.match(script, /docker\s+compose[\s\S]*up\s+-d\s+--no-deps/);
  assert.doesNotMatch(script, /docker\s+rm\s+-f[^\n]*(?:jenkins|gitlab|nexus|local-registry|registry-ui)/);
});

test('MySQL baseline rendering emits only missing table segments', () => {
  const renderer = path.join(deployRoot, 'scripts/init/render-mysql-baseline.mjs');
  const tmp = fs.mkdtempSync(path.join(os.tmpdir(), 'peach-mysql-'));
  const schema = path.join(tmp, 'schema.sql');
  const existing = path.join(tmp, 'existing.txt');
  const output = path.join(tmp, 'missing.sql');
  const metadata = path.join(tmp, 'metadata.json');

  fs.writeFileSync(schema, [
    'CREATE TABLE PEACH_A (ID INT PRIMARY KEY);',
    'CREATE INDEX IDX_A ON PEACH_A (ID);',
    'CREATE TABLE `PEACH_B` (ID INT PRIMARY KEY);',
    'CREATE INDEX IDX_B ON PEACH_B (ID);',
    '',
  ].join('\n'));
  fs.writeFileSync(existing, 'PEACH_A\n');

  execFileSync('node', [renderer, '--schema', schema, '--existing', existing, '--output', output, '--metadata', metadata]);
  const rendered = fs.readFileSync(output, 'utf8');
  const details = JSON.parse(fs.readFileSync(metadata, 'utf8'));
  assert.doesNotMatch(rendered, /CREATE TABLE PEACH_A/);
  assert.doesNotMatch(rendered, /IDX_A/);
  assert.match(rendered, /CREATE TABLE `PEACH_B`/);
  assert.match(rendered, /IDX_B/);
  assert.deepEqual(details.expectedTables, ['PEACH_A', 'PEACH_B']);
  assert.deepEqual(details.missingTables, ['PEACH_B']);
});

test('MySQL initialization tracks completion and verifies the expected schema instead of trusting any table', () => {
  const script = read('scripts/init/init-mysql.sh');
  assert.match(script, /PEACH_DEPLOY_SCHEMA_HISTORY/);
  assert.match(script, /render-mysql-baseline\.mjs/);
  assert.match(script, /missingTables/);
  assert.match(script, /baseline-v1/);
  assert.doesNotMatch(script, /existing schema detected[\s\S]*skipping baseline initialization/i);
});

test('secret-bearing generated files are private and removed on every exit path', () => {
  const nacos = read('scripts/init/init-nacos.sh');
  assert.match(nacos, /umask\s+077/);
  assert.match(nacos, /mktemp\s+-d/);
  assert.match(nacos, /cleanup\(\)[\s\S]*rm\s+-rf/);
  assert.match(nacos, /trap\s+cleanup/);
  assert.match(nacos, /render-nacos-config\.mjs/);

  const maven = read('scripts/ci/maven-build.sh');
  assert.match(maven, /umask\s+077/);
  assert.match(maven, /runtime\/generated\/secrets/);
  assert.match(maven, /cleanup\(\)[\s\S]*MAVEN_SETTINGS_GENERATED/);
  assert.match(maven, /trap\s+cleanup/);

  const jenkins = read('Jenkinsfile');
  assert.match(jenkins, /runtime\/generated\/secrets/);
});
