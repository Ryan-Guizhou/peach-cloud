import { test } from 'node:test'
import assert from 'node:assert/strict'
import { resourceKey, mergeVisibleChecks } from './state.ts'
test('filter checks preserve grants outside the visible tree', () => {
  assert.deepEqual(mergeVisibleChecks(['visible', 'hidden'], ['visible'], []), ['hidden'])
})
test('resource identities do not collide when codes contain delimiters', () => {
  assert.notEqual(resourceKey('a:', 'b', 'c'), resourceKey('a', ':b', 'c'))
})
