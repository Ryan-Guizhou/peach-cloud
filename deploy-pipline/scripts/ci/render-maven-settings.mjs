import fs from 'node:fs';
import path from 'node:path';

const env = process.env;
const required = (name) => {
  const value = env[name];
  if (!value) throw new Error(`Missing required environment variable: ${name}`);
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
const hasNexusAuth = Boolean(env.MAVEN_NEXUS_USERNAME && env.MAVEN_NEXUS_PASSWORD);
const hasProxy = Boolean(env.MAVEN_PROXY_HOST && env.MAVEN_PROXY_PORT);
const hasProxyAuth = Boolean(env.MAVEN_PROXY_USERNAME && env.MAVEN_PROXY_PASSWORD);
let text = fs.readFileSync(templatePath, 'utf8');

const optional = (name, enabled) => {
  const pattern = new RegExp(`\\n?<!-- @optional ${name}\\n([\\s\\S]*?)\\n@optional-end -->`, 'g');
  text = text.replace(pattern, enabled ? '\n$1' : '');
};
optional('NEXUS_AUTH', hasNexusAuth);
optional('MAVEN_PROXY', hasProxy);

const values = {
  MAVEN_LOCAL_REPOSITORY: required('MAVEN_LOCAL_REPOSITORY'),
  MAVEN_NEXUS_URL: required('MAVEN_NEXUS_URL').replace(/\/+$/, ''),
  MAVEN_NEXUS_USERNAME: env.MAVEN_NEXUS_USERNAME,
  MAVEN_NEXUS_PASSWORD: env.MAVEN_NEXUS_PASSWORD,
  MAVEN_PROXY_ID: env.MAVEN_PROXY_ID || 'corp-proxy',
  MAVEN_PROXY_ACTIVE: env.MAVEN_PROXY_ACTIVE || 'true',
  MAVEN_PROXY_PROTOCOL: env.MAVEN_PROXY_PROTOCOL || 'http',
  MAVEN_PROXY_HOST: env.MAVEN_PROXY_HOST,
  MAVEN_PROXY_PORT: env.MAVEN_PROXY_PORT,
  MAVEN_PROXY_NON_PROXY_HOSTS: env.MAVEN_PROXY_NON_PROXY_HOSTS || 'localhost|127.0.0.1|nexus|registry',
  MAVEN_PROXY_AUTH: hasProxyAuth
    ? `            <username>${escapeXml(env.MAVEN_PROXY_USERNAME)}</username>\n            <password>${escapeXml(env.MAVEN_PROXY_PASSWORD)}</password>`
    : '',
};

text = text.replace(/@([A-Z0-9_]+)@/g, (match, key) => {
  if (!Object.prototype.hasOwnProperty.call(values, key)) throw new Error(`Missing Maven settings placeholder: ${key}`);
  return key === 'MAVEN_PROXY_AUTH' ? values[key] : escapeXml(values[key]);
});
const unresolved = text.match(/@[A-Z0-9_]+@/g) || [];
if (unresolved.length > 0) throw new Error(`Unresolved Maven settings placeholders: ${unresolved.join(', ')}`);
fs.mkdirSync(path.dirname(outputPath), { recursive: true });
fs.writeFileSync(outputPath, text, { encoding: 'utf8', mode: 0o600 });
