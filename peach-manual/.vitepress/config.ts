import { defineConfig } from 'vitepress'

const guideSidebar = [
  {
    text: '开始使用',
    items: [
      { text: '快速开始', link: '/guide/getting-started' },
      { text: '平台功能概览', link: '/guide/platform-overview' },
      { text: '登录与机构上下文', link: '/guide/login-and-context' },
    ],
  },
  {
    text: '功能导引',
    items: [
      { text: '工作台与消息', link: '/guide/workspace' },
      { text: '授权与数据权限', link: '/guide/authorization' },
      { text: '文件与存储', link: '/guide/file-storage' },
      { text: '日志与审计', link: '/guide/logs-and-audit' },
    ],
  },
]

const faqSidebar = [
  { text: '常见问题', link: '/faq/' },
]

const changelogSidebar = [
  { text: '版本总览', link: '/changelog/' },
  { text: 'v1.0.0', link: '/changelog/v1.0.0' },
]

const metaSidebar = [
  { text: '维护指南', link: '/meta/maintenance' },
  { text: '配置说明', link: '/meta/configuration' },
]

export default defineConfig({
  base: '/manual/',
  lang: 'zh-CN',
  title: 'Peach Cloud 用户手册',
  description: 'Peach Cloud 控制台使用指南：快速上手、功能说明、常见问题与版本记录',
  head: [
    ['link', { rel: 'icon', href: '/manual/logo-mark.svg' }],
    ['script', {}, `(function(){try{var k='peach-manual-theme';var m=localStorage.getItem(k)||'light';var d=m==='dark'||(m==='auto'&&window.matchMedia('(prefers-color-scheme: dark)').matches);if(d)document.documentElement.classList.add('dark');document.documentElement.dataset.manualTheme=m;}catch(e){}})();`],
  ],
  themeConfig: {
    logo: '/logo-mark.svg',
    siteTitle: 'Peach Cloud',
    appearance: false,
    nav: [
      { text: '指南', link: '/guide/getting-started', activeMatch: '/guide/' },
      { text: '更新记录', link: '/changelog/', activeMatch: '/changelog/' },
      { text: '常见问题', link: '/faq/', activeMatch: '/faq/' },
      { text: '维护', link: '/meta/maintenance', activeMatch: '/meta/' },
    ],
    sidebar: {
      '/guide/': guideSidebar,
      '/changelog/': changelogSidebar,
      '/faq/': faqSidebar,
      '/meta/': metaSidebar,
    },
    socialLinks: [],
    search: {
      provider: 'local',
    },
    outline: {
      label: '本页目录',
      level: [2, 3],
    },
    docFooter: {
      prev: '上一页',
      next: '下一页',
    },
    sidebarMenuLabel: '目录',
    returnToTopLabel: '回到顶部',
  },
  vite: {
    server: {
      port: 5174,
      strictPort: true,
      proxy: {
        '/api': {
          target: 'http://127.0.0.1:18080',
          changeOrigin: true,
        },
        '/login': {
          target: 'http://127.0.0.1:5173',
          changeOrigin: true,
        },
      },
    },
  },
})
