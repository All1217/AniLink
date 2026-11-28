import { onBeforeUnmount, ref } from 'vue'

/** 各业务 WS 服务的默认地址：拆分后 /video 归 al-danmaku（8030），/chat 归 al-communication（8024）。 */
const WS_DEFAULT_BASE: Record<string, string> = {
  '/video': 'ws://localhost:8030',
  '/chat': 'ws://localhost:8024',
}
/** 未匹配到业务前缀时的兜底地址，保持旧默认值。 */
const WS_FALLBACK_BASE = 'ws://localhost:8024'
const MAX_RETRY = 10
const HEARTBEAT_INTERVAL = 5000

/**
 * 按 path 解析 WS 服务地址：VITE_WS_BASE 存在时整体覆盖（便于联调/部署），
 * 否则按业务前缀选择默认端口，避免所有房间都连到同一个服务。
 */
function resolveWsBase(path: string): string {
  if (import.meta.env.VITE_WS_BASE) return import.meta.env.VITE_WS_BASE
  const prefix = Object.keys(WS_DEFAULT_BASE).find((item) => path.startsWith(item))
  return prefix ? WS_DEFAULT_BASE[prefix] : WS_FALLBACK_BASE
}

interface Options<T> {
  /** 收到消息时回调（已 JSON.parse） */
  onMessage?: (message: T) => void
  /** 连接建立后回调：用于发送认证/加入房间的握手包 */
  onOpen?: (send: (payload: unknown) => void) => void
  /** 心跳包内容；不给则不心跳 */
  heartbeat?: () => unknown
}

/**
 * 统一 WebSocket 封装：连接、心跳、断线重连（500ms 起翻倍，上限 30s，最多 10 次）。
 * 旧工程在 Player 与 Whisper 各写了一份，这里只保留一份。
 */
export function useWebSocket<T = unknown>(path: string, options: Options<T> = {}) {
  const status = ref<'idle' | 'open' | 'closed'>('idle')
  const socket = ref<WebSocket | null>(null)
  let heartbeatTimer: number | undefined
  let retryTimer: number | undefined
  let retryCount = 0
  let manualClosed = false

  function clearTimers() {
    if (heartbeatTimer) window.clearInterval(heartbeatTimer)
    if (retryTimer) window.clearTimeout(retryTimer)
    heartbeatTimer = undefined
    retryTimer = undefined
  }

  function send(payload: unknown) {
    if (socket.value?.readyState === WebSocket.OPEN) {
      socket.value.send(JSON.stringify(payload))
    }
  }

  function connect() {
    manualClosed = false
    socket.value?.close()
    const ws = new WebSocket(`${resolveWsBase(path)}${path}`)
    socket.value = ws

    ws.onopen = () => {
      status.value = 'open'
      retryCount = 0
      options.onOpen?.(send)
      if (options.heartbeat) {
        heartbeatTimer = window.setInterval(() => send(options.heartbeat!()), HEARTBEAT_INTERVAL)
      }
    }

    ws.onmessage = (event) => {
      try {
        options.onMessage?.(JSON.parse(event.data as string) as T)
      } catch {
        // 非 JSON 消息直接忽略，避免污染业务
      }
    }

    ws.onclose = () => {
      status.value = 'closed'
      clearTimers()
      if (manualClosed || retryCount >= MAX_RETRY) return
      const delay = Math.min(500 * 2 ** retryCount, 30_000)
      retryCount += 1
      retryTimer = window.setTimeout(connect, delay)
    }

    ws.onerror = () => {
      ws.close()
    }
  }

  function close() {
    manualClosed = true
    clearTimers()
    socket.value?.close()
    socket.value = null
    status.value = 'closed'
  }

  onBeforeUnmount(close)

  return { status, connect, send, close }
}
