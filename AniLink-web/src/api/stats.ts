import { get } from './client'

export interface HotSearch {
  keyword: string
  show_name: string
  heat_score: number
  icon?: string
  uri?: string
  goto?: string
}

export interface DataResVo {
  /** 统计值的序列（按天的数量或数值） */
  data: number[]
  /** 对应的日期标签 */
  dates?: string[]
  total?: number
}

/** 首页热搜榜 */
export const fetchHotSearch = () => get<HotSearch[]>('/main/dataCenter/mainWindow/get/hotSearch')

/** 数据中心：总播放量趋势 */
export const fetchPlayTotal = (uid: number, begin: string, end: string) =>
  get<DataResVo>(`/main/dataCenter/total/get/play?uid=${uid}&begin=${begin}&end=${end}`)

/** 数据中心：总点赞趋势 */
export const fetchLikeTotal = (uid: number, begin: string, end: string) =>
  get<DataResVo>(`/main/dataCenter/total/get/like?uid=${uid}&begin=${begin}&end=${end}`)
