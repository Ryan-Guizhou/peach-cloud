#!/usr/bin/env node
import { readFileSync } from 'node:fs';

const authTables = [
  'APPLICATION', 'AUTH_FUNCTION', 'AUTH_LOG', 'AUTH_PARTY',
  'AUTH_RESOURCE', 'CODE_RULE', 'FUNCTION', 'MENU',
  'ORGANIZATION', 'RESOURCE', 'ROLE', 'ROUTER',
  'TENANT', 'USER', 'USER_AVATAR_HISTORY', 'USER_ORG',
];

const authMappers = [
  'Application', 'AuthFunction', 'AuthLog', 'AuthParty',
  'AuthResource', 'Function', 'Menu', 'Organization',
  'Resource', 'Role', 'Router', 'UserAvatarHistory',
  'User', 'UserOrg',
];

function assertCondition(condition, message) {
  if (!condition) {
    throw new Error(message);
  }
}

function read(path) {
  return readFileSync(path, 'utf8');
}

for (const name of authTables) {
  const path = `sql/PEACH_${name}.sql`;
  const sql = read(path);
  assertCondition(/\bID\s+VARCHAR\(32\)\s+NOT NULL/i.test(sql), `${path}: missing physical ID`);
  assertCondition(/PRIMARY KEY\s*\(\s*ID\s*\)/i.test(sql), `${path}: primary key must be ID`);
  assertCondition(/\bIS_DELETE\s+\w+\([^)]*\)\s+NOT NULL\s+DEFAULT\s+0/i.test(sql)
    || /\bIS_DELETE\s+\w+\s+NOT NULL\s+DEFAULT\s+0/i.test(sql),
  `${path}: IS_DELETE must be NOT NULL DEFAULT 0`);
  assertCondition(/\bDELETED_AT\b/.test(sql), `${path}: missing DELETED_AT`);
}

for (const name of authMappers) {
  const path = `peach-auth/peach-auth-service/src/main/resources/com/peach/auth/dao/${name}Dao.xml`;
  const xml = read(path);
  assertCondition(!/<delete\b/i.test(xml) && !/\bDELETE\s+FROM\s+PEACH_/i.test(xml),
    `${path}: physical deletion in authentication mapper`);

  const update = xml.match(/<update id="updateById"[^>]*>[\s\S]*?<\/update>/);
  assertCondition(update && /WHERE\s+ID\s*=\s*#\{id,jdbcType=VARCHAR\}/.test(update[0]),
    `${path}: updateById must use the physical id property`);
  assertCondition(/<sql id="allColumnAlias">[\s\S]*?\bID\s+as\s+id\b/i.test(xml),
    `${path}: missing physical ID result mapping`);

  for (const method of ['delById', 'delByIds', 'del']) {
    const block = xml.match(new RegExp(`<update id="${method}"[^>]*>[\\s\\S]*?<\\/update>`));
    assertCondition(block && /SET\s+IS_DELETE\s*=\s*1/.test(block[0]),
      `${path}: ${method} must be a logical deletion`);
  }
}

for (const name of ['TENANT', 'USER', 'ORGANIZATION', 'ROLE', 'RESOURCE']) {
  const sql = read(`sql/PEACH_${name}.sql`);
  const businessId = {TENANT:'TENANT_ID',USER:'USER_ID',ORGANIZATION:'ORG_ID',ROLE:'ROLE_ID',RESOURCE:'RESOURCE_ID'}[name];
  assertCondition(new RegExp(`UNIQUE\\s+(?:KEY|INDEX)\\s+\\w+\\s*\\(\\s*${businessId}\\s*\\)`, 'i').test(sql),
    `PEACH_${name}: permanent business ID needs its own unique constraint`);
}

const aggregate = read('sql/ALL_TABLE_CREATE.sql');
for (const name of authTables) {
  assertCondition(aggregate.includes(`-- PEACH_${name}.sql`), `aggregated DDL missing PEACH_${name}`);
}

console.log('Authentication ID and soft-delete invariant checks passed.');
