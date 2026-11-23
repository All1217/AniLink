import type Artplayer from 'artplayer'
import { onBeforeUnmount } from 'vue'
import { updateProgress } from '@/api/history'
import { DateStringType, type Video } from '@/types'
import { formatDate } from '@/utils/format'

/**
 * 播放进度上报：
 * - 开始播放时立刻上报一次，之后每 3 秒一次；
 * - 后端把 moment 写成「上次看到哪个节点」（`bh_video.duration`），用于断点续播；
 * - 只调 `record/add`（本身是 upsert），不要再调 `history/add`，
 *   `bh_video` 上有唯一索引 (uid, vid)，两个接口都调会撞唯一键。
 */
export function usePlayProgress(getArt: () => Artplayer | null, getVideo: () => Video) {
  let timer: number | undefined

  /** 上报当前位置；失败静默，避免打断播放 */
  function report() {
    const video = getVideo()
    if (!video.vid) return
    void updateProgress({
      vid: video.vid,
      duration: video.duration,
      moment: Math.floor(getArt()?.currentTime ?? 0),
      commitTime: formatDate(Date.now(), DateStringType.ALL),
    }).catch(() => undefined)
  }

  /** 每 3 秒上报一次（currentTime 为 0 时不报，避免覆盖已看进度） */
  function start() {
    if (timer) return
    timer = window.setInterval(() => {
      if (getArt()?.currentTime) report()
    }, 3000)
  }

  function stop() {
    if (timer) window.clearInterval(timer)
    timer = undefined
  }

  onBeforeUnmount(stop)

  return { report, start, stop }
}
