#!/usr/bin/env node
import { readFileSync, readdirSync } from 'node:fs';

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


// 列级一致性校验：既要覆盖实体映射，也要覆盖历史 Mapper XML，避免 Maven
// 编译能通过但真实 MySQL 执行查询时出现 Unknown column。
function tableColumns(sql, tableName, source) {
  const statement = sql.match(new RegExp(
    'CREATE\\s+TABLE\\s+' + tableName + '\\s*\\(([\\s\\S]*?)\\)\\s*(?:COMMENT|ENGINE|;)', 'i'));
  assertCondition(statement, source + ': missing CREATE TABLE ' + tableName);
  return new Set([...statement[1].matchAll(
    /^\s*([A-Z][A-Z0-9_]*)\s+(?:VARCHAR|CHAR|TINYINT|SMALLINT|INT|BIGINT|DATETIME|TIMESTAMP|DATE|DECIMAL|TEXT|MEDIUMTEXT|LONGTEXT|BLOB|DOUBLE|FLOAT|JSON|BIT|BOOLEAN)\b/gm)]
    .map(match => match[1]));
}

const tableCache = new Map();
function authTableColumns(tableName) {
  if (tableCache.has(tableName)) return tableCache.get(tableName);
  assertCondition(/^PEACH_[A-Z][A-Z0-9_]*$/.test(tableName),
    tableName + ': unsupported authentication table name');
  const path = 'sql/' + tableName + '.sql';
  const sourceColumns = tableColumns(read(path), tableName, path);
  const aggregateColumns = tableColumns(aggregate, tableName, 'sql/ALL_TABLE_CREATE.sql');
  const sourceOnly = [...sourceColumns].filter(column => !aggregateColumns.has(column));
  const aggregateOnly = [...aggregateColumns].filter(column => !sourceColumns.has(column));
  assertCondition(sourceOnly.length === 0 && aggregateOnly.length === 0,
    tableName + ': source and aggregate DDL columns diverge: sourceOnly='
      + sourceOnly.join(',') + ', aggregateOnly=' + aggregateOnly.join(','));
  tableCache.set(tableName, sourceColumns);
  return sourceColumns;
}

for (const name of authTables) {
  authTableColumns('PEACH_' + name);
}

const entityDir = 'peach-auth/peach-auth-entity/src/main/java/com/peach/auth/entity/';
for (const fileName of readdirSync(entityDir).filter(name => name.endsWith('DO.java'))) {
  const path = entityDir + fileName;
  const java = read(path);
  const tableName = java.match(/@Table\s*\(\s*name\s*=\s*"([A-Z][A-Z0-9_]*)"/)?.[1];
  if (!tableName) continue;
  const columns = authTableColumns(tableName);
  for (const match of java.matchAll(/@Column\s*\(\s*name\s*=\s*"([A-Z][A-Z0-9_]*)"/g)) {
    assertCondition(columns.has(match[1]),
      path + ': @Column(' + match[1] + ') is missing from ' + tableName);
  }
}

for (const name of authMappers) {
  const path = 'peach-auth/peach-auth-service/src/main/resources/com/peach/auth/dao/'
    + name + 'Dao.xml';
  const xml = read(path);
  const tableName = 'PEACH_' + name.replace(/([a-z0-9])([A-Z])/g, '$1_$2').toUpperCase();
  const columns = authTableColumns(tableName);
  const allColumn = xml.match(/<sql id="allColumn">([\s\S]*?)<\/sql>/);
  assertCondition(allColumn, path + ': missing allColumn SQL fragment');
  for (const column of allColumn[1].split(',').map(x => x.trim()).filter(Boolean)) {
    assertCondition(/^[A-Z][A-Z0-9_]*$/.test(column),
      path + ': unsupported allColumn token ' + column);
    assertCondition(columns.has(column),
      path + ': mapper field ' + column + ' is missing from ' + tableName);
  }
}

console.log('Authentication ID, soft-delete and DDL/entity/Mapper column checks passed.');
