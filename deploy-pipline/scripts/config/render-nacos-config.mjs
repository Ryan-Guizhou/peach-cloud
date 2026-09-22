import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';

function parseArgs(argv) {
  const args = {};
  for (let index = 0; index < argv.length; index += 1) {
    const key = argv[index];
    if (!key.startsWith('--')) continue;
    args[key.slice(2)] = argv[index + 1] ?? '';
    index += 1;
  }
  return args;
}

function isConfigured(value) {
  const normalized = String(value ?? '').trim();
  return Boolean(normalized && !/^change_me/i.test(normalized));
}

const credentialPairs = {
  OSS: ['OSS_ACCESS_KEY', 'OSS_SECRET_KEY'],
  COS: ['COS_ACCESS_KEY', 'COS_SECRET_KEY'],
  BOS: ['BOS_ACCESS_KEY', 'BOS_SECRET_KEY'],
  OBS: ['OBS_ACCESS_KEY', 'OBS_SECRET_KEY'],
};

function providerStates(env) {
  return Object.fromEntries(Object.entries(credentialPairs).map(([provider, keys]) => {
    const configured = keys.map((key) => isConfigured(env[key]));
    if (configured.some(Boolean) && !configured.every(Boolean)) {
      throw new Error(`Incomplete ${provider} credentials: configure both ${keys.join(' and ')} or leave both empty.`);
    }
    return [provider, configured.every(Boolean)];
  }));
}

function applyConditionalBlocks(text, states) {
  const output = [];
  const stack = [];
  for (const line of text.split(/\r?\n/)) {
    const start = line.match(/^\s*#\s*@if-configured\s+([A-Z0-9_]+)@\s*$/);
    if (start) {
      const name = start[1];
      if (!Object.prototype.hasOwnProperty.call(states, name)) {
        throw new Error(`Unknown Nacos template condition: ${name}`);
      }
      stack.push({ name, enabled: states[name] });
      continue;
    }
    const end = line.match(/^\s*#\s*@endif-configured\s+([A-Z0-9_]+)@\s*$/);
    if (end) {
      const current = stack.pop();
      if (!current || current.name !== end[1]) {
        throw new Error(`Unexpected Nacos template condition end: ${end[1]}`);
      }
      continue;
    }
    if (stack.every(({ enabled }) => enabled)) output.push(line);
  }
  if (stack.length > 0) {
    throw new Error(`Unclosed Nacos template condition block: ${stack.at(-1).name}`);
  }
  return output.join('\n');
}

function rawValue(env, key) {
  const defaults = {
    MYSQL_HOST: 'mysql:3306',
    MYSQL_DATABASE: 'peach_cloud',
    REDIS_HOST: 'redis:6379',
    PEACH_STORAGE_PRIMARY: 'local',
  };
  let value = env[key] ?? defaults[key] ?? '';
  if (key === 'PEACH_STORAGE_PRIMARY') value = String(value).trim().toLowerCase();
  if (['MYSQL_ROOT_PASSWORD', 'REDIS_PASSWORD'].includes(key) && !isConfigured(value)) {
    throw new Error(`Missing required Nacos placeholder value: ${key}`);
  }
  if (/[\r\n]/.test(value)) throw new Error(`Nacos placeholder ${key} must not contain line breaks.`);
  if (key === 'PEACH_STORAGE_PRIMARY' && !/^[a-z][a-z0-9_-]*$/.test(value)) {
    throw new Error(`Invalid PEACH_STORAGE_PRIMARY value: ${value}`);
  }
  return value;
}

function render(text, env, states) {
  let rendered = applyConditionalBlocks(text, states);
  rendered = rendered.replace(/"@([A-Z0-9_]+)@"/g, (_match, key) => JSON.stringify(rawValue(env, key)));
  rendered = rendered.replace(/@([A-Z0-9_]+)@/g, (_match, key) => rawValue(env, key));
  const unresolved = rendered.match(/@[A-Z0-9_]+@/g) || [];
  if (unresolved.length > 0) throw new Error(`Unresolved Nacos placeholders: ${unresolved.join(', ')}`);
  return `${rendered.replace(/\s+$/, '')}\n`;
}

const args = parseArgs(process.argv.slice(2));
const source = path.resolve(args.source || '');
const output = path.resolve(args.output || '');
if (!args.source || !fs.existsSync(source)) throw new Error(`Missing Nacos template: ${source}`);
if (!args.output) throw new Error('Missing --output for Nacos template rendering.');

const states = providerStates(process.env);
const primary = rawValue(process.env, 'PEACH_STORAGE_PRIMARY');
const primaryCondition = { aliyun: 'OSS', cos: 'COS', bos: 'BOS', obs: 'OBS' }[primary];
if (primary !== 'local' && (!primaryCondition || !states[primaryCondition])) {
  throw new Error(`Storage primary provider '${primary}' is not configured with complete credentials.`);
}

const rendered = render(fs.readFileSync(source, 'utf8'), process.env, states);
fs.mkdirSync(path.dirname(output), { recursive: true, mode: 0o700 });
fs.writeFileSync(output, rendered, { encoding: 'utf8', mode: 0o600 });
fs.chmodSync(output, 0o600);
