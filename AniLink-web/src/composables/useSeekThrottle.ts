import type Artplayer from 'artplayer'

/** 拖动进度条的回调：preview 在拖动中节流触发，commit 在松手后触发 */
export interface SeekThrottleOptions {
  /** 拖动过程中（节流 ~250ms）回调目标时间，用于预热落点分片 */
  onPreview?: (time: number) => void
  /** 松手真正 seek 之后回调目标时间，用于展示缓冲提示 */
  onCommit?: (time: number) => void
}

/** 拖动时预热分片的节流间隔（ms）：太密会白拉带宽，太稀松手前来不及 */
const PREVIEW_INTERVAL = 250

/**
 * 拖动进度条节流。
 *
 * Artplayer 默认在 document:mousemove 里「每动一次就 seek 一次」，而 HLS/MSE 每次 seek
 * 都要中断在飞请求、重新拉分片并 append，拖一次进度条就是几十次重载 → 肉眼可见的卡顿。
 * 这里在拖拽期间只让进度条跟手（emit setBar），松手时才真正 seek 一次。
 */
export function throttleProgressSeek(art: Artplayer, options: SeekThrottleOptions = {}): () => void {
  const bar = art.template.$progress
  let dragging = false
  let moved = false
  let ratio = 0
  let lastPreview = 0
  /** 松手后浏览器会补一个 click，Artplayer 会再 seek 一次并覆盖提示，这里吃掉它 */
  let suppressClick = false
  let suppressTimer: number | undefined

  /** 指针位置换算成 0~1 的进度 */
  function ratioOf(event: MouseEvent): number {
    const rect = bar.getBoundingClientRect()
    if (!rect.width) return 0
    return Math.min(1, Math.max(0, (event.clientX - rect.left) / rect.width))
  }

  function onDown(event: MouseEvent) {
    // 只接管左键拖动；单击的定位仍由 Artplayer 自己处理
    if (event.button !== 0) return
    dragging = true
    moved = false
  }

  function onMove(event: MouseEvent) {
    if (!dragging) return
    // 捕获阶段拦掉 Artplayer 的 document:mousemove 监听，避免连续 seek
    event.stopImmediatePropagation()
    ratio = ratioOf(event)
    moved = true
    art.emit('setBar', 'played', ratio, event)
    // 拖动中提前把落点分片拉进缓存，松手时 hls.js 直接命中
    if (options.onPreview && art.duration) {
      const now = performance.now()
      if (now - lastPreview >= PREVIEW_INTERVAL) {
        lastPreview = now
        options.onPreview(ratio * art.duration)
      }
    }
  }

  function onUp() {
    if (!dragging) return
    dragging = false
    // 松手才跳转；没拖动过则不覆盖单击定位的结果
    if (moved && Number.isFinite(art.duration) && art.duration > 0) {
      const target = ratio * art.duration
      art.seek = target
      options.onCommit?.(target)
      suppressClick = true
      if (suppressTimer) window.clearTimeout(suppressTimer)
      suppressTimer = window.setTimeout(() => {
        suppressClick = false
      }, 500)
    }
    moved = false
  }

  /** 吃掉拖动结束后补发的 click（否则 Artplayer 会重复 seek 并把缓冲提示覆盖成时间） */
  function onClick(event: MouseEvent) {
    if (!suppressClick) return
    suppressClick = false
    if (suppressTimer) window.clearTimeout(suppressTimer)
    event.stopImmediatePropagation()
    event.preventDefault()
  }

  bar.addEventListener('mousedown', onDown, true)
  // click 挂在 document 捕获阶段：比 Artplayer 挂在进度条上的处理更早，才能真正拦住
  document.addEventListener('click', onClick, true)
  document.addEventListener('mousemove', onMove, true)
  document.addEventListener('mouseup', onUp, true)
  // 指针移出窗口后不会再有 mouseup，失焦时复位，避免下一次移动被误判成拖拽
  window.addEventListener('blur', onUp)

  return () => {
    if (suppressTimer) window.clearTimeout(suppressTimer)
    bar.removeEventListener('mousedown', onDown, true)
    document.removeEventListener('click', onClick, true)
    document.removeEventListener('mousemove', onMove, true)
    document.removeEventListener('mouseup', onUp, true)
    window.removeEventListener('blur', onUp)
  }
}
