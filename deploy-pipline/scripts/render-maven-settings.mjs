import fs from 'node:fs';

const env = process.env;

const required = (name) => {
  const value = env[name];
  if (!value) {
    throw new Error(`Missing required environment variable: ${name}`);
  }
  return value;
};

const escapeXml = (value) => String(value || '')
  .replace(/&/g, '&amp;')
  .replace(/</g, '&lt;')
  .replace(/>/g, '&gt;')
  .replace(/"/g, '&quot;')
  .replace(/'/g, '&apos;');

const templatePath = required('MAVEN_SETTINGS_TEMPLATE');
const outputPath = required('MAVEN_SETTINGS_GENERATED');

let text = fs.readFileSync(templatePath, 'utf8');

const hasNexusAuth = Boolean(env.MAVEN_NEXUS_USERNAME && env.MAVEN_NEXUS_PASSWORD);
const hasProxy = Boolean(env.MAVEN_PROXY_HOST && env.MAVEN_PROXY_PORT);
const hasProxyAuth = Boolean(env.MAVEN_PROXY_USERNAME && env.MAVEN_PROXY_PASSWORD);

const proxyAuth = hasProxyAuth
  ? `            <username>${escapeXml(env.MAVEN_PROXY_USERNAME)}</username>\n            <password>${escapeXml(env.MAVEN_PROXY_PASSWORD)}</password>`
  : '';

const values = {
  MAVEN_LOCAL_REPOSITORY: env.MAVEN_LOCAL_REPOSITORY,
  MAVEN_NEXUS_URL: String(env.MAVEN_NEXUS_URL || '').replace(/\/+$/, ''),
  MAVEN_ALIYUN_PUBLIC_URL: env.MAVEN_ALIYUN_PUBLIC_URL,
  MAVEN_NEXUS_USERNAME: env.MAVEN_NEXUS_USERNAME,
  MAVEN_NEXUS_PASSWORD: env.MAVEN_NEXUS_PASSWORD,
  MAVEN_PROXY_ID: env.MAVEN_PROXY_ID,
  MAVEN_PROXY_ACTIVE: env.MAVEN_PROXY_ACTIVE,
  MAVEN_PROXY_PROTOCOL: env.MAVEN_PROXY_PROTOCOL,
  MAVEN_PROXY_HOST: env.MAVEN_PROXY_HOST,
  MAVEN_PROXY_PORT: env.MAVEN_PROXY_PORT,
  MAVEN_PROXY_NON_PROXY_HOSTS: env.MAVEN_PROXY_NON_PROXY_HOSTS,
  MAVEN_PROXY_AUTH: proxyAuth,
};

const optional = (name, enabled) => {
  const pattern = new RegExp(`\\n?<!-- @optional ${name}\\n([\\s\\S]*?)\\n@optional-end -->`, 'g');
  text = text.replace(pattern, enabled ? '\n$1' : '');
};

optional('NEXUS_AUTH', hasNexusAuth);
optional('MAVEN_PROXY', hasProxy);

text = text.replace(/@([A-Z0-9_]+)@/g, (match, key) => {
  if (!Object.prototype.hasOwnProperty.call(values, key)) {
    throw new Error(`Missing Maven settings placeholder: ${key}`);
  }
  if (key === 'MAVEN_PROXY_AUTH') {
    return values[key];
  }
  return escapeXml(values[key]);
});

const remainingOptional = text.match(/<!-- @optional [A-Z0-9_]+/g) || [];
if (remainingOptional.length > 0) {
  throw new Error(`Unresolved optional block marker in Maven settings: ${remainingOptional.join(', ')}`);
}

if (text.includes('@')) {
  const remainingPlaceholders = text.match(/@[A-Z0-9_]+@/g) || [];
  if (remainingPlaceholders.length > 0) {
    throw new Error(`Unresolved Maven settings placeholders: ${remainingPlaceholders.join(', ')}`);
  }
}

fs.writeFileSync(outputPath, text, { encoding: 'utf8', mode: 0o600 });
