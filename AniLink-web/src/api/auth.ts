import { get, post } from './client'
import type { User } from '@/types'

export interface LoginPayload {
  username: string
  password: string
}

export interface RegisterPayload {
  /** 登录名（用户名），唯一；注册后用它登录 */
  username: string
  password: string
  /** 昵称，可重复；不填默认与用户名相同 */
  nickname?: string
}

/** 登录/注册返回：access-token 与用户基本信息（refresh-token 由服务端写 HttpOnly Cookie） */
export interface LoginResult {
  accessToken: string
  uid: number
  username: string
  nickname: string
}

export interface ChangePasswordPayload {
  oldPassword: string
  newPassword: string
}

/** 登录，返回 access-token */
export const login = (payload: LoginPayload) => post<string>('/main/login', payload)

/** 注册（成功即登录，直接返回 token） */
export const register = (payload: RegisterPayload) => post<LoginResult>('/main/user/register', payload)

/** 登出（不关心返回值） */
export const logout = () => post('/main/logout')

/** 当前登录用户信息 */
export const fetchLoginUser = () => get<User>('/main/info')

/** 主动续约 token */
export const refreshAccessToken = () => get<string>('/main/public/refresh')

/** 修改密码（需登录；成功后服务端会吊销全部会话） */
export const changePassword = (payload: ChangePasswordPayload) => post('/main/user/changePassword', payload)
