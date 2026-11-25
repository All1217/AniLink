import { defineStore } from 'pinia'
import { adminApi } from '@/api/admin'
import { TOKEN_KEY } from '@/api/client'
import type { AdminMe } from '@/api/types'

/** 管理端登录态：只持久化 access token，profile 每次刷新重新校验。 */
export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem(TOKEN_KEY) || '',
    profile: null as AdminMe | null,
    loading: false,
  }),
  getters: {
    isLogin: (state) => !!state.token,
    isSuper: (state) => state.profile?.role === 2,
  },
  actions: {
    async signIn(username: string, password: string) {
      this.loading = true
      try {
        const result = await adminApi.login(username, password)
        if (result.code !== 200 || !result.data) {
          throw new Error(result.message || '登录失败')
        }
        localStorage.setItem(TOKEN_KEY, result.data)
        this.token = result.data
        if (!(await this.loadMe())) {
          throw new Error('无管理员权限')
        }
      } finally {
        this.loading = false
      }
    },
    async loadMe(): Promise<boolean> {
      if (!this.token) return false
      const result = await adminApi.me()
      if (result.code !== 200 || !result.data || result.data.role < 1) {
        this.clear()
        return false
      }
      this.profile = result.data
      return true
    },
    clear() {
      localStorage.removeItem(TOKEN_KEY)
      this.token = ''
      this.profile = null
    },
    async signOut() {
      try {
        await adminApi.logout()
      } catch {
        // 后端登出失败也要清本地状态
      }
      this.clear()
    },
  },
})
