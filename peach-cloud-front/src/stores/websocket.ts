import { defineStore } from 'pinia'
import { message, notification } from 'ant-design-vue'

import { fetchUnreadCount } from '../api/message'
import { clearAuthSession } from '../utils/auth-storage'
import type { AuthSession } from '../types/auth'

interface WebSocketPayload {
  title?: string
  content?: string
  count?: number
  url?: string
}

interface WebSocketEvent {
  type?: string
  payload?: WebSocketPayload
}

interface WebSocketState {
  connected: boolean
  unreadCount: number
  lastEventType: string
}

let activeSocket: WebSocket | null = null

function parseMessage(data: string): WebSocketEvent | null {
  try {
    const value: unknown = JSON.parse(data)
    if (typeof value !== 'object' || value === null) {
      return null
    }
    return value as WebSocketEvent
  } catch {
    return null
  }
}

function buildWebSocketUrl(session: AuthSession): string {
  const baseUrl = import.meta.env.VITE_WS_BASE_URL
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const host = window.location.host
  const endpoint = '/api/webSocket/message'
  const url = baseUrl ? String(baseUrl) : `${protocol}//${host}${endpoint}`
  const separator = url.includes('?') ? '&' : '?'
  return `${url}${separator}token=${encodeURIComponent(session.token)}`
}

export const useWebSocketStore = defineStore('websocket', {
  state: (): WebSocketState => ({
    connected: false,
    unreadCount: 0,
    lastEventType: '',
  }),
  actions: {
    async refreshUnreadCount() {
      this.unreadCount = await fetchUnreadCount()
    },
    handleEvent(event: WebSocketEvent) {
      this.lastEventType = event.type ?? ''
      if (event.type === 'MESSAGE_CREATED') {
        notification.info({
          message: event.payload?.title ?? '收到新消息',
          description: event.payload?.content ?? '请前往消息中心查看详情。',
        })
        void this.refreshUnreadCount()
        return
      }
      if (event.type === 'UNREAD_COUNT_CHANGED') {
        this.unreadCount = Number(event.payload?.count ?? 0)
        return
      }
      if (event.type === 'KICK_OUT') {
        message.warning('当前账号已在其他位置登录，请重新登录。')
        clearAuthSession()
        window.setTimeout(() => {
          window.location.replace('/login')
        }, 600)
        return
      }
      if (event.type === 'PERMISSION_CHANGED') {
        notification.warning({
          message: '权限已变更',
          description: '请刷新页面或重新登录以获取最新菜单和按钮权限。',
        })
        return
      }
      if (event.type === 'SYSTEM_NOTICE') {
        notification.info({
          message: event.payload?.title ?? '系统通知',
          description: event.payload?.content ?? '请查看系统公告。',
        })
      }
    },
    connect(session: AuthSession | null) {
      if (!session?.token || this.connected || activeSocket) {
        return
      }
      const socket = new WebSocket(buildWebSocketUrl(session))
      activeSocket = socket
      socket.onopen = () => {
        this.connected = true
        void this.refreshUnreadCount()
      }
      socket.onmessage = (event: MessageEvent<string>) => {
        const parsed = parseMessage(event.data)
        if (parsed) {
          this.handleEvent(parsed)
        }
      }
      socket.onclose = () => {
        this.connected = false
        activeSocket = null
      }
      socket.onerror = () => {
        this.connected = false
      }
    },
    disconnect() {
      if (activeSocket) {
        activeSocket.close()
      }
      activeSocket = null
      this.connected = false
      this.unreadCount = 0
      this.lastEventType = ''
    },
  },
})
