import { get, post, put } from './client'
import type { Favorite, PageRes, VideoFavoriteVo, VideoVo } from '@/types'

export interface FavoriteQueryVo {
  current?: number
  size?: number
  uid?: number
}

export interface BatchCollectVo {
  uid: number
  vid: number
  fids: number[]
}

export interface FavoriteUpdateVo {
  fid: number
  title?: string
  description?: string
  visible?: number
}

export interface BatchUpdateVo {
  uid: number
  srcFid: number
  targetFid: number
  vids: number[]
}

/** 我的收藏夹分页 */
export const fetchFavoriteList = (query: FavoriteQueryVo) =>
  get<PageRes<Favorite>>('/main/favorite/getFavoriteListPage', query)

/** 收藏夹内的视频（不分页，用于播放页弹窗计数） */
export const fetchVideosInFavorite = (fid: number) => get<VideoVo[]>(`/main/favorite/getVideoByFavor?fid=${fid}`)

/** 某用户收藏夹内的视频分页（个人空间） */
export const fetchFavoriteVideoPage = (fid: number, current: number, sortField = 'time') =>
  get<PageRes<VideoFavoriteVo>>(
    `/main/favorite/getVideoFavoritePage?current=${current}&size=20&sortField=${sortField}&fid=${fid}`,
  )

/** 当前用户把该视频放进了哪些收藏夹 */
export const fetchFavoriteIds = (uid: number, vid: number) =>
  get<number[]>(`/main/favorite/getFVByUidAndVid?uid=${uid}&vid=${vid}`)

/** 收藏到指定收藏夹集合（fids 为空表示全部取消） */
export const collectVideo = (payload: BatchCollectVo) => put('/main/favorite/collect', payload)

/** 新建收藏夹 */
export const createFavorite = (payload: Partial<Favorite>) => post('/main/favorite/add', payload)

/** 修改收藏夹信息 */
export const updateFavorite = (payload: FavoriteUpdateVo) => put('/main/favorite/update', payload)

/** 批量取消收藏 */
export const uncollectVideos = (payload: BatchUpdateVo) => put('/main/favorite/unCollect', payload)

/** 批量移动 */
export const batchMove = (payload: BatchUpdateVo) => put<number>('/main/favorite/update/batchMove', payload)

/** 批量复制 */
export const batchCopy = (payload: BatchUpdateVo) => put<number>('/main/favorite/update/batchCopy', payload)
