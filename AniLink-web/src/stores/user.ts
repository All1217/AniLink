import { defineStore } from 'pinia'
import * as authApi from '@/api/auth'
import type { User } from '@/types'

interface UserState {
  /** 访问令牌：短期，放在内存 + localStorage（刷新令牌只在 HttpOnly Cookie 里，前端不可读） */
  token: string
  userInfo: User | null
  /** 登录弹窗是否可见（放在这里，任何页面都能唤起登录） */
  loginDialogVisible: boolean
}

/** 当前登录用户与鉴权态；access-token 持久化在 localStorage */
export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    token: '',
    userInfo: null,
    loginDialogVisible: false,
  }),

  getters: {
    isLogin: (state) => Boolean(state.token),
    uid: (state) => state.userInfo?.uid ?? 0,
    nickname: (state) => state.userInfo?.nickname ?? '',
    avatar: (state) => state.userInfo?.avatar ?? '',
  },

  actions: {
    setToken(token: string) {
      this.token = token
    },

    setUserInfo(userInfo: User) {
      this.userInfo = userInfo
    },

    openLoginDialog() {
      this.loginDialogVisible = true
    },

    closeLoginDialog() {
      this.loginDialogVisible = false
    },

    /** 登录成功后立刻拉一次用户信息，保证头像/昵称可用 */
    async signIn(payload: authApi.LoginPayload) {
      const token = await authApi.login(payload)
      await this.applySession(token)
    },

    /** 已有 access-token（如注册接口直接返回）时落库并拉取用户信息 */
    async applySession(accessToken: string) {
      this.setToken(accessToken)
      await this.loadUserInfo()
    },

    /** 拉取当前登录用户信息（有 token 时才会请求） */
    async loadUserInfo() {
      if (!this.token) return
      this.userInfo = await authApi.fetchLoginUser()
    },

    /** 登出：先通知后端，再无条件清本地状态 */
    async signOut() {
      try {
        await authApi.logout()
      } finally {
        this.reset()
      }
    },

    reset() {
      this.token = ''
      this.userInfo = null
      this.loginDialogVisible = false
    },
  },

  persist: {
    key: 'anilink-user',
    pick: ['token'],
  },
})
