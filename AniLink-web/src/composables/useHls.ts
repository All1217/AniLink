import type Artplayer from 'artplayer'

/** Artplayer customType 回调：返回的函数会在切源/销毁时被调用 */
export type CustomTypeHandler = (
  video: HTMLVideoElement,
  url: string,
  art: Artplayer,
) => (() => void) | void

/** 播放器句柄：customType 交给 Artplayer，destroy/prefetch 由组件调用 */
export interface HlsPlayer {
  customType: Record<string, CustomTypeHandler>
  destroy: () => void
  /** 预取指定时间点所在分片（拖动时用，松手后 hls.js 直接命中浏览器缓存） */
  prefetch: (time: number) => void
}

/**
 * hls.js 播放参数（VOD 场景）：
 * - 缓冲给得比较足（前 120s / 保留 300s）：拖进度条时大概率落在已缓冲区间；
 * - maxBufferSize 限制内存占用，避免长片把内存吃满；
 * - startFragPrefetch 起播时先预取下个分片，减少起播等待；
 * - abrEwmaDefaultEstimate 提高初始带宽估计，避免首个分片按 500kbps 保守起播。
 */
const HLS_CONFIG = {
  maxBufferLength: 120,
  maxMaxBufferLength: 300,
  backBufferLength: 300,
  maxBufferSize: 120 * 1000 * 1000,
  startLevel: -1,
  capLevelToPlayerSize: true,
  enableWorker: true,
  lowLatencyMode: false,
  startFragPrefetch: true,
  abrEwmaDefaultEstimate: 3_000_000,
}

/** 分片信息（hls.js 内部结构，只取需要的字段） */
interface HlsFragment {
  start: number
  duration: number
  url: string
}

/**
 * 生成 HLS 播放器句柄：
 * 1) 优先 hls.js（MSE）：新版 Chrome/Edge 的 canPlayType 也返回 "maybe"，
 *    但原生实现的缓冲与 seek 策略不可控，拖进度条会明显卡顿；
 * 2) 只有 MSE 不受支持（iOS Safari 等）时才把地址交给浏览器原生 HLS；
 * 3) 都不行（或分片致命错误）回退原始 MP4，保证不白屏。
 *
 * 注意：Artplayer 不会调用 customType 的返回值，所以必须由组件在卸载时调 destroy()，
 * 否则每进一个播放页就泄漏一个 Hls 实例。
 */
export function createHlsPlayer(fallbackUrl?: string): HlsPlayer {
  let destroyActive: (() => void) | null = null
  /** 当前活跃实例，供 prefetch 读取分片列表 */
  let active: import('hls.js').default | null = null
  let prefetchedUrl = ''
  let prefetching = false

  const customType: Record<string, CustomTypeHandler> = {
    m3u8: (video, url) => {
      destroyActive?.()
      let hls: import('hls.js').default | null = null
      let alive = true

      /** 收尾：销毁 hls 实例并解绑 MSE，切源/卸载都走这里 */
      const teardown = () => {
        alive = false
        hls?.destroy()
        hls = null
        active = null
        destroyActive = null
      }

      const fallbackToMp4 = () => {
        if (!alive || !fallbackUrl) return
        console.warn('[player] HLS 不可用，回退 MP4 播放', fallbackUrl)
        teardown()
        video.src = fallbackUrl
        video.load()
      }

      void import('hls.js')
        .then(({ default: Hls }) => {
          if (!alive) return
          if (Hls.isSupported()) {
            hls = new Hls(HLS_CONFIG)
            active = hls
            // 开发环境暴露实例，方便在控制台排查缓冲/码率问题
            if (import.meta.env.DEV) (window as unknown as Record<string, unknown>).__hls = hls
            hls.on(Hls.Events.ERROR, (_event, data) => {
              if (data.fatal) fallbackToMp4()
            })
            hls.loadSource(url)
            hls.attachMedia(video)
            return
          }
          if (video.canPlayType('application/vnd.apple.mpegurl')) {
            video.src = url
            return
          }
          fallbackToMp4()
        })
        .catch(fallbackToMp4)

      destroyActive = teardown
      return teardown
    },
  }

  /** 目标时间所在分片的绝对地址（拿不到返回空串） */
  function fragmentUrlAt(time: number): string {
    const details = active?.latestLevelDetails
    if (!details || !details.fragments?.length) return ''
    const fragments = details.fragments as unknown as HlsFragment[]
    const frag = fragments.find((item) => time >= item.start && time < item.start + item.duration)
    return frag?.url ?? ''
  }

  /**
   * 拖动时预热落点分片：分片带 `Cache-Control: immutable`，这次 fetch 会把内容写进
   * 浏览器 HTTP 缓存，松手后 hls.js 请求同一 URL 基本是 0 成本。
   * 同一时刻只允许一个预取在飞，避免快速拖动把带宽打满。
   */
  function prefetch(time: number) {
    if (prefetching) return
    const url = fragmentUrlAt(time)
    if (!url || url === prefetchedUrl) return
    prefetchedUrl = url
    prefetching = true
    void fetch(url, { priority: 'low' } as RequestInit)
      .then((response) => response.arrayBuffer())
      .catch(() => undefined)
      .finally(() => {
        prefetching = false
      })
  }

  return {
    customType,
    prefetch,
    destroy: () => {
      destroyActive?.()
      destroyActive = null
    },
  }
}

/**
 * 给媒体源做一次预连接（DNS + TCP + TLS 预热）。
 * 媒体地址由后端下发，不能写死在 index.html，所以运行时按需插入。
 */
export function preconnect(url?: string | null): void {
  if (!url) return
  let origin: string
  try {
    origin = new URL(url, window.location.href).origin
  } catch {
    return
  }
  if (origin === window.location.origin) return
  const selector = `link[rel="preconnect"][href="${origin}"]`
  if (document.head.querySelector(selector)) return
  const link = document.createElement('link')
  link.rel = 'preconnect'
  link.href = origin
  link.crossOrigin = 'anonymous'
  document.head.appendChild(link)
}
