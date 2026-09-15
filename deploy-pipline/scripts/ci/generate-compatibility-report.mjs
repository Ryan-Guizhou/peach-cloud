import fs from 'node:fs';
import path from 'node:path';
import process from 'node:process';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const baseline = JSON.parse(fs.readFileSync(path.join(root, 'config/compatibility-baseline.json'), 'utf8'));
const applicationCompose = fs.readFileSync(path.join(root, 'compose/application/docker-compose.yml'), 'utf8');
function parseEnv(text) { const result = {}; for (const raw of text.split(/\r?\n/)) { const line = raw.trim(); if (!line || line.startsWith('#')) continue; const index = line.indexOf('='); if (index > 0) result[line.slice(0,index)] = line.slice(index+1).replace(/^['"]|['"]$/g,''); } return result; }
const defaults = parseEnv(fs.readFileSync(path.join(root, 'env/defaults.env'), 'utf8'));
const configured = {
 registry:`registry:${defaults.REGISTRY_VERSION}`, registryUi:`joxit/docker-registry-ui:${defaults.REGISTRY_UI_VERSION}`, gitlab:`gitlab/gitlab-ce:${defaults.GITLAB_VERSION}`, nexus:`sonatype/nexus3:${defaults.NEXUS_VERSION}`, jenkins:defaults.JENKINS_IMAGE, nginx:`nginx:${defaults.NGINX_VERSION}`, mysql:`mysql:${defaults.MYSQL_VERSION}`, redis:`redis:${defaults.REDIS_VERSION}`, nacos:`nacos/nacos-server:${defaults.NACOS_VERSION}`, mongodb:`mongo:${defaults.MONGO_VERSION}`, rocketmq:`apache/rocketmq:${defaults.ROCKETMQ_VERSION}`, rocketmqDashboard:`apacherocketmq/rocketmq-dashboard:${defaults.ROCKETMQ_DASHBOARD_VERSION}`, prometheus:`prom/prometheus:${defaults.PROMETHEUS_VERSION}`, tempo:`grafana/tempo:${defaults.TEMPO_VERSION}`, otelCollector:`otel/opentelemetry-collector-contrib:${defaults.OTEL_COLLECTOR_VERSION}`, loki:`grafana/loki:${defaults.LOKI_VERSION}`, alloy:`grafana/alloy:${defaults.ALLOY_VERSION}`, grafana:`grafana/grafana:${defaults.GRAFANA_VERSION}`
};
const errors=[];
for (const [key,expected] of Object.entries(baseline.images)) if (configured[key] !== expected) errors.push(`version drift: ${key} expected=${expected} configured=${configured[key]}`);
for (const service of baseline.applicationServices) if (!new RegExp(`^  ${service}:`,'m').test(applicationCompose)) errors.push(`application service missing from compose: ${service}`);
const containerMap={registry:'local-registry',registryUi:'registry-ui',gitlab:'gitlab',nexus:'nexus',jenkins:'jenkins',nginx:'peach-devops-nginx',mysql:'peach-mysql',redis:'peach-redis',nacos:'peach-nacos',mongodb:'peach-mongo',rocketmq:'peach-rocketmq-broker',rocketmqDashboard:'peach-rocketmq-dashboard',prometheus:'peach-prometheus',tempo:'peach-tempo',otelCollector:'peach-otel-collector',loki:'peach-loki',alloy:'peach-alloy',grafana:'peach-grafana'};
function dockerInspect(container,template){const result=spawnSync('docker',['inspect','-f',template,container],{encoding:'utf8'}); return result.status===0?result.stdout.trim():'not-present';}
const lines=['# Peach Cloud Compatibility Report','',`Generated: ${new Date().toISOString()}`,'','## Repository version parity','','| Component | Baseline | Configured | Result |','| --- | --- | --- | --- |'];
for(const [key,expected] of Object.entries(baseline.images)){const actual=configured[key]??'unknown';lines.push(`| ${key} | \`${expected}\` | \`${actual}\` | ${actual===expected?'PASS':'FAIL'} |`);}
lines.push('','## Runtime visibility (read-only)','','| Component | Container | Config.Image | State |','| --- | --- | --- | --- |');
for(const [key,container] of Object.entries(containerMap)) lines.push(`| ${key} | \`${container}\` | \`${dockerInspect(container,'{{.Config.Image}}')}\` | ${dockerInspect(container,'{{.State.Status}}')} |`);
lines.push('','## Protected DevOps data','','| Volume | Present |','| --- | --- |');
for(const volume of baseline.protectedVolumes){const result=spawnSync('docker',['volume','inspect',volume],{encoding:'utf8'});lines.push(`| \`${volume}\` | ${result.status===0?'YES':'NO'} |`);}
lines.push('','## Application service contract',''); for(const service of baseline.applicationServices) lines.push(`- ${service}: ${new RegExp(`^  ${service}:`,'m').test(applicationCompose)?'PRESENT':'MISSING'}`); lines.push('',`Overall repository compatibility: **${errors.length?'FAIL':'PASS'}**`,'');
const output=process.argv[2]?path.resolve(process.argv[2]):path.join(root,'runtime/reports/compatibility-report.md'); fs.mkdirSync(path.dirname(output),{recursive:true}); fs.writeFileSync(output,`${lines.join('\n')}\n`,'utf8'); console.log(output); if(errors.length){for(const error of errors) console.error(error);process.exit(1);}
