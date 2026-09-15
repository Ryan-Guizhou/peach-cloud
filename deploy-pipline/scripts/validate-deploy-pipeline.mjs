import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import { fileURLToPath } from 'node:url';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const failures = [];
const required = [
  'compose/devops/docker-compose.yml',
  'compose/middleware/docker-compose.yml',
  'compose/observability/docker-compose.yml',
  'compose/application/docker-compose.yml',
  'config/maven/settings.xml',
  'config/mongodb/mongod.conf',
  'scripts/bootstrap/bootstrap.sh',
  'scripts/init/init-mysql.sh',
  'scripts/init/ensure-mongodb-user.sh',
  'scripts/init/init-nacos.sh',
  'scripts/ci/maven-build.sh',
  'scripts/deploy/deploy-services.sh',
  'README.md',
  'README.en-US.md',
  'docs/getting-started.md',
  'docs/architecture-and-operations.md',
  'docs/ci-cd.md'
];
for (const rel of required) if (!fs.existsSync(path.join(root, rel))) failures.push(`missing required file: ${rel}`);
if (fs.existsSync(path.join(root, 'scripts/init/init-mongodb.sh'))) failures.push('obsolete MongoDB data initialization script must not exist');
if (fs.existsSync(path.join(root, 'init/mongodb'))) failures.push('MongoDB schema/index/seed initialization directories must not exist');

const protectedNames = ['peach-registry-data','peach-gitlab-config','peach-gitlab-data','peach-jenkins-data','peach-nexus-data','peach-mysql-data','peach-redis-data','peach-nacos-data','peach-rocketmq-store','peach-prometheus-data','peach-tempo-data','peach-loki-data','peach-alloy-data','peach-grafana-data','peach-devops','peach-cloud-runtime'];
const composeText = fs.readdirSync(path.join(root,'compose'),{withFileTypes:true}).filter(e=>e.isDirectory()).map(e=>fs.readFileSync(path.join(root,'compose',e.name,'docker-compose.yml'),'utf8')).join('\n');
for (const name of protectedNames) if (!composeText.includes(name)) failures.push(`protected Docker identity missing from compose definitions: ${name}`);
if (!composeText.includes('peach-mongo-data') || !composeText.includes('peach-mongo')) failures.push('MongoDB container/volume identity is missing');
if (!composeText.includes('peach-rocketmq-dashboard') || !composeText.includes('apacherocketmq/rocketmq-dashboard')) failures.push('RocketMQ Dashboard is missing');

const walk=(dir)=>fs.readdirSync(dir,{withFileTypes:true}).flatMap(e=>{const f=path.join(dir,e.name);return e.isDirectory()?walk(f):[f];});
const danger=[/docker\s+compose[^\n]*\sdown\s+-v\b/,/docker\s+volume\s+prune\b/,/docker\s+system\s+prune[^\n]*--volumes\b/,/docker\s+volume\s+rm\b/];
for(const file of walk(root)){if(!/\.(sh|mjs|groovy)$/.test(file)&&path.basename(file)!=='Jenkinsfile')continue;const text=fs.readFileSync(file,'utf8');for(const p of danger)if(p.test(text))failures.push(`dangerous destructive Docker command in ${path.relative(root,file)}: ${p}`);}
for(const file of walk(root).filter(f=>f.endsWith('.md'))){const text=fs.readFileSync(file,'utf8');const links=[...text.matchAll(/\[[^\]]+\]\(([^)]+)\)/g)].map(m=>m[1]);for(const link of links){if(/^(https?:|mailto:|#)/.test(link))continue;const target=link.split('#')[0];if(target&&!fs.existsSync(path.resolve(path.dirname(file),target)))failures.push(`broken relative link in ${path.relative(root,file)}: ${link}`);}}
const jenkins=fs.readFileSync(path.join(root,'Jenkinsfile'),'utf8');for(const forbidden of ['up -d mysql','init-mysql.sh','init-nacos.sh','init-mongodb.sh','ensure-mongodb-user.sh'])if(jenkins.includes(forbidden))failures.push(`Jenkinsfile must not manage middleware lifecycle: ${forbidden}`);for(const expected of ['preflight.sh','maven-build.sh','push-image.sh','deploy-services.sh','verify-services.sh'])if(!jenkins.includes(expected))failures.push(`Jenkinsfile missing expected delivery step: ${expected}`);
const settings=fs.readFileSync(path.join(root,'config/maven/settings.xml'),'utf8');if(!settings.includes('<mirrorOf>*</mirrorOf>'))failures.push('CI Maven settings must route all repositories through Nexus');
if(failures.length){console.error(`deploy-pipeline validation failed with ${failures.length} issue(s):`);for(const failure of failures)console.error(`- ${failure}`);process.exit(1);}console.log('deploy-pipeline structural validation passed');
