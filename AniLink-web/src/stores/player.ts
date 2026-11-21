import { defineStore } from 'pinia'
import type { UserVideo, VideoStats } from '@/types'

interface PlayerState {
  vid: number
  /** 当前用户对该视频的四态（赞/币/藏/关注），播放页所有面板共享这一份 */
  interaction: UserVideo
  stats: VideoStats | null
}

const emptyInteraction = (): UserVideo => ({
  id: 0,
  uid: 0,
  vid: 0,
  play: 0,
  love: 0,
  unlove: 0,
  coin: 0,
  collect: 0,
  playTime: '',
  loveTime: '',
  coinTime: '',
})

/** 播放页交互态：单一真相，避免旧代码里多个面板各改一份计数 */
export const usePlayerStore = defineStore('player', {
  state: (): PlayerState => ({
    vid: 0,
    interaction: emptyInteraction(),
    stats: null,
  }),

  getters: {
    liked: (state) => state.interaction.love > 0,
    coined: (state) => state.interaction.coin > 0,
    collected: (state) => state.interaction.collect > 0,
  },

  actions: {
    /** 进入新视频：重置交互态并写入统计数据 */
    init(vid: number) {
      this.vid = vid
      this.interaction = { ...emptyInteraction(), vid }
      this.stats = null
    },

    setStats(stats: VideoStats | null) {
      this.stats = stats
    },

    /** 乐观更新：先改本地，失败由调用方回滚 */
    optimisticLike(liked: boolean) {
      this.interaction.love = liked ? 1 : 0
      if (this.stats) this.stats.good = Math.max(0, this.stats.good + (liked ? 1 : -1))
    },

    optimisticCoin(count: number) {
      this.interaction.coin = count
      if (this.stats) this.stats.coin = Math.max(0, this.stats.coin + count)
    },

    setCollected(collected: boolean) {
      this.interaction.collect = collected ? 1 : 0
      if (this.stats) this.stats.collect = Math.max(0, (this.stats.collect ?? 0) + (collected ? 1 : -1))
    },
  },
})
