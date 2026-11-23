import type Artplayer from 'artplayer'

/** Artplayer customType 回调：返回的函数会在切源/销毁时被调用 */
export type CustomTypeHandler = (
  video: HTMLVideoElement,
  url: string,
  art: Artplayer,
) => (() => void) | void

/** 播放器句柄：customType 交给 Artplayer，destroy 由组件在卸载时调用 */
export interface HlsPlayer {
  customType: Record<string, CustomTypeHandler>
  destroy: () => void
}

/**
 * hls.js 播放参数（VOD 场景）：
 * - maxBufferLength/maxMaxBufferLength 控制缓冲时长，比默认 30/600 省内存；
 * - startLevel: -1 从带宽探测的首个分片起播，不做全量码率探测；
 * - capLevelToPlayerSize 按播放器尺寸限制码率，避免小窗拉 1080p。
 */
const HLS_CONFIG = {
  maxBufferLength: 30,
  maxMaxBufferLength: 60,
  backBufferLength: 30,
  maxBufferSize: 30 * 1000 * 1000,
  startLevel: -1,
  capLevelToPlayerSize: true,
  enableWorker: true,
  lowLatencyMode: false,
}

/**
 * 生成 HLS 播放器句柄：
 * 1) Safari / iOS 原生支持 HLS，直接把 m3u8 交给 video 元素（还能省掉 hls.js）；
 * 2) 其它浏览器动态 import('hls.js')，只进播放页 chunk，不影响首屏体积；
 * 3) hls.js 不可用或遇到致命错误时回退原始 MP4（Range 伪流式），保证不白屏。
 *
 * 注意：Artplayer 不会调用 customType 的返回值，所以必须由组件在卸载时调 destroy()，
 * 否则每进一个播放页就泄漏一个 Hls 实例（后台继续拉流，快速切换视频时会互相抢带宽）。
 */
export function createHlsPlayer(fallbackUrl?: string): HlsPlayer {
  let destroyActive: (() => void) | null = null

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
        destroyActive = null
      }

      const fallbackToMp4 = () => {
        if (!alive || !fallbackUrl) return
        console.warn('[player] HLS 不可用，回退 MP4 播放', fallbackUrl)
        teardown()
        video.src = fallbackUrl
        video.load()
      }

      // Safari / iOS 原生支持 HLS：直接把地址给 video 元素，不加载 hls.js
      if (video.canPlayType('application/vnd.apple.mpegurl')) {
        video.src = url
        destroyActive = teardown
        return teardown
      }

      void import('hls.js')
        .then(({ default: Hls }) => {
          if (!alive) return
          if (!Hls.isSupported()) {
            fallbackToMp4()
            return
          }
          hls = new Hls(HLS_CONFIG)
          hls.on(Hls.Events.ERROR, (_event, data) => {
            if (data.fatal) fallbackToMp4()
          })
          hls.loadSource(url)
          hls.attachMedia(video)
        })
        .catch(fallbackToMp4)

      destroyActive = teardown
      return teardown
    },
  }

  return {
    customType,
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
