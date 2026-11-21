import { onBeforeUnmount, ref } from 'vue'

const WS_BASE = import.meta.env.VITE_WS_BASE ?? 'ws://localhost:8024'
const MAX_RETRY = 10
const HEARTBEAT_INTERVAL = 5000

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
    const ws = new WebSocket(`${WS_BASE}${path}`)
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
