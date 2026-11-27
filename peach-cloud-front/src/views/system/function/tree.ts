import type { FunctionDefinition } from '../../../api/function'

export interface FunctionNode {
  key: string
  title: string
  record: FunctionDefinition
  children: FunctionNode[]
  anomaly?: string
}

/** Keep invalid historical nodes visible; never recurse through a cycle. */
export function buildFunctionTree(rows: FunctionDefinition[], keyword = ''): FunctionNode[] {
  const nodes = new Map(rows.map(row => [row.funcId, {
    key: row.funcId, title: `${row.funcCode}  ${row.funcName}`, record: row,
    children: [],
  } as FunctionNode]))
  const byCode = new Map<string, FunctionNode[]>()
  for (const node of nodes.values()) {
    byCode.set(node.record.funcCode, [...(byCode.get(node.record.funcCode) ?? []), node])
  }
  const roots: FunctionNode[] = []
  for (const node of nodes.values()) {
    let ancestor = node.record.parentFuncCode
    const seen = new Set([node.key])
    let invalid = false
    while (ancestor) {
      const matches = byCode.get(ancestor) ?? []
      const parent = matches[0]
      if (matches.length !== 1 || !parent || seen.has(parent.key)) {
        node.anomaly = matches.length === 0 ? '上级缺失' : '层级异常'
        invalid = true
        break
      }
      seen.add(parent.key)
      ancestor = parent.record.parentFuncCode
    }
    const parent = byCode.get(node.record.parentFuncCode ?? '')?.[0]
    if (invalid || !parent) roots.push(node)
    else parent.children.push(node)
  }
  const term = keyword.trim().toLocaleLowerCase()
  function filter(branches: FunctionNode[]): FunctionNode[] {
    return branches.flatMap(node => {
      const children = filter(node.children)
      return !term || node.title.toLocaleLowerCase().includes(term) || children.length
        ? [{ ...node, children }]
        : []
    })
  }
  return filter(roots)
}
