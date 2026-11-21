import { del, get, post } from './client'
import type { Follow, Interaction, UserVideo } from '@/types'

export interface UserVideoQuery {
  id?: number
  uid: number
  vid: number
  actionType?: Interaction
  actionTime?: string
}

export interface LikePayload {
  bizId: number
  bizType: number
  liked: boolean
}

// 说明：后端没有"记录播放量"的接口（VideoController 里不存在 /main/video/**/play），
// 播放量目前由 Redis 计数 + UpdateStats 回写，没有对外入口，因此前端不提供该调用。

/** 投币 */
export const coinVideo = (query: UserVideoQuery) => post<UserVideo>('/main/video/interact/coin', query)

/** 当前用户对该视频的投币状态 */
export const fetchCoinState = (query: UserVideoQuery) => get<UserVideo>('/main/video/getInterActionCoin', query)

/** 当前用户对该视频的收藏状态 */
export const fetchCollectState = (query: UserVideoQuery) =>
  get<UserVideo>('/main/favorite/getInterActionFavorite', query)

/** 该视频被当前用户点赞的状态（0 未赞 / 1 已赞） */
export const fetchLikeState = (bizId: number) => get<number>(`/remark/get/like?bizId=${bizId}`)

/** 点赞/取消点赞 */
export const likeVideo = (payload: LikePayload) => post('/remark/like', payload)

/** 关注状态 */
export const fetchFollowState = (query: Follow) => get<Follow>('/main/fans/get/follow', query)

/** 关注 */
export const followUser = (query: Follow) => post<Follow>('/main/fans/follow', query)

/** 取消关注 */
export const unfollowUser = (query: Follow) => del<Follow>('/main/fans/unfollow', query)
