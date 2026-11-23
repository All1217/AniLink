import type Artplayer from 'artplayer'

/**
 * 拖动进度条节流。
 *
 * Artplayer 默认在 document:mousemove 里「每动一次就 seek 一次」，而 HLS/MSE 每次 seek
 * 都要中断在飞请求、重新拉分片并 append，拖一次进度条就是几十次重载 → 肉眼可见的卡顿。
 * 这里在拖拽期间只让进度条跟手（emit setBar），松手时才真正 seek 一次。
 */
export function throttleProgressSeek(art: Artplayer): () => void {
  const bar = art.template.$progress
  let dragging = false
  let moved = false
  let ratio = 0

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
  }

  function onUp() {
    if (!dragging) return
    dragging = false
    // 松手才跳转；没拖动过则不覆盖单击定位的结果
    if (moved && Number.isFinite(art.duration) && art.duration > 0) {
      art.seek = ratio * art.duration
    }
    moved = false
  }

  bar.addEventListener('mousedown', onDown, true)
  document.addEventListener('mousemove', onMove, true)
  document.addEventListener('mouseup', onUp, true)
  // 指针移出窗口后不会再有 mouseup，失焦时复位，避免下一次移动被误判成拖拽
  window.addEventListener('blur', onUp)

  return () => {
    bar.removeEventListener('mousedown', onDown, true)
    document.removeEventListener('mousemove', onMove, true)
    document.removeEventListener('mouseup', onUp, true)
    window.removeEventListener('blur', onUp)
  }
}
