import { get, post } from './client'
import type { User } from '@/types'

export interface LoginPayload {
  username: string
  password: string
}

export interface RegisterPayload {
  nickname: string
  password: string
}

/** 登录，返回 access-token */
export const login = (payload: LoginPayload) => post<string>('/main/login', payload)

/** 登出（不关心返回值） */
export const logout = () => post('/main/logout')

/** 当前登录用户信息 */
export const fetchLoginUser = () => get<User>('/main/info')

/** 主动续约 token */
export const refreshAccessToken = () => get<string>('/main/public/refresh')

/** 注册 */
export const register = (payload: RegisterPayload) => post<string>('/main/user/register', payload)
