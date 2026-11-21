import { get } from './client'

export interface HotSearch {
  keyword: string
  show_name: string
  heat_score: number
  icon?: string
  uri?: string
  goto?: string
}

/** 数据中心返回：本期/上期的总量与粉丝量序列（后端 DataResVo） */
export interface DataResVo {
  mainRes: number[]
  subRes: number[]
  lastMainRes: number[]
  lastSubRes: number[]
}

/** 首页热搜榜 */
export const fetchHotSearch = () => get<HotSearch[]>('/main/dataCenter/mainWindow/get/hotSearch')

/** 数据中心：总播放量趋势（begin/end 为 yyyy-MM-dd） */
export const fetchPlayTotal = (uid: number, begin: string, end: string) =>
  get<DataResVo>(`/main/dataCenter/total/get/play?uid=${uid}&begin=${begin}&end=${end}`)

/** 数据中心：总点赞趋势 */
export const fetchLikeTotal = (uid: number, begin: string, end: string) =>
  get<DataResVo>(`/main/dataCenter/total/get/like?uid=${uid}&begin=${begin}&end=${end}`)
