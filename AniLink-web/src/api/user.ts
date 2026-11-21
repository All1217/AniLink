import { get, post } from './client'
import type { PageRes, User, UserStats, UserVo } from '@/types'

export interface UserQueryVo {
  uid?: number
  id?: number
  current?: number
  size?: number
}

export interface FollowQueryVo {
  uid: number
  current?: number
  size?: number
}

export interface UserInfoUpdateVo {
  uid: number
  nickname?: string
  description?: string
  avatar?: string
  gender?: number
  birthDate?: string
  school?: string
  notice?: string
}

/** 公开主页：基础信息 */
export const fetchUserById = (query: UserQueryVo) => get<User>('/main/user/public/getById', query)

/** 公开主页：统计信息（粉丝/获赞/播放…） */
export const fetchUserStatsPublic = (query: UserQueryVo) =>
  get<UserStats>('/main/user/public/getUserStatById', query)

/** 登录态主页：基础信息 */
export const fetchUserInfo = (query: UserQueryVo) => get<User>('/main/user/getById', query)

/** 登录态主页：统计信息 */
export const fetchUserStats = (query: UserQueryVo) => get<UserStats>('/main/user/getUserStatsByUid', query)

/** 账号设置页用到的聚合信息 */
export const fetchUserVo = (query: UserQueryVo) => get<UserVo>('/main/user/getUserVoByUid', query)

/** 更新个性签名 */
export const updateDescription = (payload: UserInfoUpdateVo) => post('/main/user/updateDescription', payload)

/** 更新公告 */
export const updateNotice = (payload: UserInfoUpdateVo) => post('/main/user/updateNotice', payload)

/** 更新头像 */
export const updateAvatar = (payload: UserInfoUpdateVo) => post('/main/user/updateAvatar', payload)

/** 更新综合资料 */
export const updateUserInfo = (payload: UserInfoUpdateVo) => post('/main/user/updateUserInfo', payload)

/** 触发一次统计信息聚合刷新（后端 GET 语义，沿用旧契约） */
export const refreshUserStats = (payload: UserInfoUpdateVo) => get('/main/user/updateMultiStats', payload)

/** 关注列表 */
export const fetchFollowList = (query: FollowQueryVo) => get<PageRes<User>>('/main/fans/get/followList', query)

/** 粉丝列表 */
export const fetchFansList = (query: FollowQueryVo) => get<PageRes<User>>('/main/fans/get/fansList', query)
