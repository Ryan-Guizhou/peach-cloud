import { test } from 'node:test'
import assert from 'node:assert/strict'
import { buildFunctionTree } from './tree.ts'
const row = (funcId, funcCode, parentFuncCode = '') => ({ funcId, funcCode, parentFuncCode, funcName: funcCode })
test('preserves ancestors during search', () => {
  const tree = buildFunctionTree([row('1', 'ROOT'), row('2', 'LEAF', 'ROOT')], 'leaf')
  assert.equal(tree.length, 1)
  assert.equal(tree[0].key, '1')
  assert.equal(tree[0].children[0].key, '2')
})
test('shows orphan and cycle nodes without recursion or data loss', () => {
  const tree = buildFunctionTree([row('1', 'A', 'B'), row('2', 'B', 'A'), row('3', 'C', 'MISSING')])
  assert.equal(tree.length, 3)
  assert.ok(tree.every(node => node.anomaly))
})

test('migrated function codes preserve actual parent links and remain searchable', () => {
  const tree = buildFunctionTree([
    row('root-id', 'F0001'), row('leaf-id', 'F0002', 'F0001'),
    row('other-id', 'F0003'),
  ], 'F0002')
  assert.equal(tree.length, 1)
  assert.equal(tree[0].record.funcCode, 'F0001')
  assert.equal(tree[0].children[0].record.funcCode, 'F0002')
  assert.equal(tree[0].children[0].record.parentFuncCode, 'F0001')
  assert.equal(tree[0].children[0].key, 'leaf-id')
})
