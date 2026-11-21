import { del, get, post } from './client'
import type { BrowseHistory, HistoryVo, PageRes } from '@/types'

export interface HistoryQueryVo {
  uid?: number
  current?: number
  size?: number
  keyword?: string
  startDuration?: number | null
  endDuration?: number | null
  startTime?: string | null
  endTime?: string | null
  vids?: number[]
  id?: number
}

export interface VideoRecordFormDTO {
  vid: number
  duration: number
  moment: number
  commitTime: string
}

/** 观看历史分页 */
export const fetchHistoryPage = (query: HistoryQueryVo) =>
  get<PageRes<HistoryVo>>('/main/history/getHistory/page', query)

/** 新增一条观看历史（开始播放时调用一次） */
export const addHistory = (payload: BrowseHistory) => post('/main/history/add', payload)

/** 上报播放进度（播放中每 3 秒一次） */
export const updateProgress = (payload: VideoRecordFormDTO) => post('/main/video/interact/record/add', payload)

/** 清空当前用户全部历史 */
export const clearHistory = (query: HistoryQueryVo) => del('/main/history/deleteByUid', query)

/** 删除单条历史 */
export const deleteHistory = (query: HistoryQueryVo) => del('/main/history/deleteSingle', query)
