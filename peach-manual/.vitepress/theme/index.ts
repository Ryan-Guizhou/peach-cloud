import { h } from 'vue'
import type { Theme } from 'vitepress'
import DefaultTheme from 'vitepress/theme'

import AuthGate from './AuthGate.vue'
import ManualLayout from './ManualLayout.vue'
import './tokens.css'
import './shell.css'
import './doc.css'

export default {
  extends: DefaultTheme,
  Layout: () => h(AuthGate, null, { default: () => h(ManualLayout) }),
} satisfies Theme
