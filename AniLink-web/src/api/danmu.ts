import { get, post } from './client'
import type { Danmu, RecoQueryVo, User } from '@/types'

/** 某视频的全部弹幕（用于播放器装填） */
export const fetchDanmuList = (vid: number | string) => get<Danmu[]>(`/main/danmu/public/getDanmuList?vid=${vid}`)

/** 发送弹幕 */
export const createDanmu = (payload: Danmu) => post('/main/danmu/post', payload)

/** 标签相近的用户（弹幕画像功能） */
export const fetchSimilarUsers = (query: RecoQueryVo) => get<User[]>('/main/danmu/getSimilarUsers', query)

/** 按标签筛选弹幕 */
export const filterDanmuByTags = (query: RecoQueryVo) => post<Danmu[]>('/main/danmu/filterUserByTags', query)

/** 两个用户的共同标签 */
export const fetchCommonTags = (query: RecoQueryVo) => get<string[]>('/main/danmu/getCommonTags', query)

export interface TagRank {
  name: string
  value: number
}

/** 某视频频率最高的 8 个标签 */
export const fetchTagRank = (vid: number) => get<TagRank[]>(`/main/danmu/getTagRankList?vid=${vid}`)
