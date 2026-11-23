import type Artplayer from 'artplayer'
import { onBeforeUnmount, ref } from 'vue'
import { formatDuration } from '@/utils/format'

/**
 * 拖动松手后的「缓冲中」提示：
 * - 立刻在播放器上显示「缓冲中 mm:ss」，避免 0.5~2s 的定格看起来像卡死；
 * - 真正出画面（video:playing）或 seeked 后已就绪时自动收起；
 * - 8 秒兜底，异常情况下不会一直挂着。
 * 用组件自己的浮层而不是 Artplayer 的 notice：notice 会被它内部的 seek 提示覆盖。
 */
export function useSeekFeedback() {
  const busy = ref(false)
  const text = ref('')
  let art: Artplayer | null = null
  let timer: number | undefined
  /** 就绪轮询句柄：seeked 之后可能还要几百毫秒才真正能播 */
  let readyTimer: number | undefined
  let detachFns: Array<() => void> = []

  /** 收起提示 */
  function end() {
    if (timer) window.clearTimeout(timer)
    if (readyTimer) window.clearTimeout(readyTimer)
    timer = undefined
    readyTimer = undefined
    if (!busy.value) return
    busy.value = false
    text.value = ''
    if (art) art.loading.show = false
  }

  /** 松手后调用：显示「缓冲中 + 目标时间」 */
  function show(time: number) {
    if (!art) return
    busy.value = true
    text.value = `缓冲中 ${formatDuration(Math.round(time))}`
    art.loading.show = true
    if (timer) window.clearTimeout(timer)
    timer = window.setTimeout(end, 8000)
  }

  /** 快进时不一定有 playing 事件，所以轮询到「不再 seeking 且已就绪」就收起 */
  function endIfReady() {
    if (readyTimer) window.clearTimeout(readyTimer)
    let tries = 0
    const check = () => {
      readyTimer = undefined
      if (!art || !busy.value) return
      if (!art.video.seeking && art.video.readyState >= 3) {
        end()
        return
      }
      if (++tries > 10) return
      readyTimer = window.setTimeout(check, 200)
    }
    readyTimer = window.setTimeout(check, 200)
  }

  /** 绑定播放器事件（实例创建后调用一次） */
  function attach(instance: Artplayer) {
    art = instance
    const onSeeked = () => {
      if (!busy.value) return
      // Artplayer 在 seeked 就收起 spinner，这里补回来
      instance.loading.show = true
      endIfReady()
    }
    const onCanPlay = () => {
      if (busy.value) instance.loading.show = true
    }
    instance.on('video:seeked', onSeeked)
    instance.on('video:canplay', onCanPlay)
    instance.on('video:playing', end)
    detachFns = [
      () => instance.off('video:seeked', onSeeked),
      () => instance.off('video:canplay', onCanPlay),
      () => instance.off('video:playing', end),
    ]
  }

  function detach() {
    if (timer) window.clearTimeout(timer)
    if (readyTimer) window.clearTimeout(readyTimer)
    timer = undefined
    readyTimer = undefined
    detachFns.forEach((fn) => fn())
    detachFns = []
    art = null
    busy.value = false
    text.value = ''
  }

  onBeforeUnmount(detach)

  return { busy, text, show, attach, detach }
}
