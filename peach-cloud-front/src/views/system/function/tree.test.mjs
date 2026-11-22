import { test } from 'node:test'
import assert from 'node:assert/strict'
import { buildFunctionTree } from './tree.ts'
const row = (funcId, funcCode, parentFuncCode = '', appId = 'app') => ({ funcId, funcCode, parentFuncCode, appId, funcName: funcCode })
test('keeps same codes in different applications separate and preserves ancestors during search', () => {
  const tree = buildFunctionTree([row('1', 'ROOT'), row('2', 'LEAF', 'ROOT'), row('3', 'ROOT', '', 'other')], 'leaf')
  assert.equal(tree.length, 1)
  assert.equal(tree[0].key, '1')
  assert.equal(tree[0].children[0].key, '2')
})
test('shows orphan and cycle nodes without recursion or data loss', () => {
  const tree = buildFunctionTree([row('1', 'A', 'B'), row('2', 'B', 'A'), row('3', 'C', 'MISSING')])
  assert.equal(tree.length, 3)
  assert.ok(tree.every(node => node.anomaly))
})
