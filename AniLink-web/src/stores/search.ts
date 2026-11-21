import { defineStore } from 'pinia'

export interface SearchHistoryItem {
  content: string
  time: number
}

const MAX_HISTORY = 20
const STORAGE_KEY = 'anilink-search'

/** 搜索历史：纯本地状态，最多 20 条，重复关键词提到最前 */
export const useSearchStore = defineStore('search', {
  state: () => ({
    history: [] as SearchHistoryItem[],
  }),

  actions: {
    add(keyword: string) {
      const content = keyword.trim()
      if (!content) return
      this.history = [
        { content, time: Date.now() },
        ...this.history.filter((item) => item.content !== content),
      ].slice(0, MAX_HISTORY)
    },

    remove(keyword: string) {
      this.history = this.history.filter((item) => item.content !== keyword)
    },

    clear() {
      this.history = []
    },
  },

  persist: {
    key: STORAGE_KEY,
  },
})
