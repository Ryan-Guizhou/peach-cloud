import { test } from 'node:test'
import assert from 'node:assert/strict'
import { resourceKey, mergeVisibleChecks } from './state.ts'
test('filter checks preserve grants outside the visible tree', () => {
  assert.deepEqual(mergeVisibleChecks(['visible', 'hidden'], ['visible'], []), ['hidden'])
})
test('resource identities do not collide when codes contain delimiters', () => {
  assert.notEqual(resourceKey('a:', 'b', 'c'), resourceKey('a', ':b', 'c'))
})

test('migrated function codes retain unchanged resource permission identities in grant drafts', () => {
  const loginGrant = resourceKey('F0001', 'BUTTON', 'loginLog:view')
  const tenantGrant = resourceKey('F0002', 'BUTTON', 'tenant:view')
  assert.equal(loginGrant, '5:F00016:BUTTON13:loginLog:view')
  assert.notEqual(loginGrant, resourceKey('F0002', 'BUTTON', 'loginLog:view'))
  assert.deepEqual(mergeVisibleChecks([loginGrant, tenantGrant], [loginGrant], []), [tenantGrant])
})
