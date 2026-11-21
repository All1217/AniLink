import { get } from './client'
import type { PageRes, RecoQueryVo, Video, VideoQueryVo, VideoStats, VideoVo } from '@/types'

/** 视频详情 */
export const fetchVideo = (vid: number) => get<Video>('/main/video/public/getVideoById', { vid })

/** 视频统计数据（播放/点赞/投币/收藏…） */
export const fetchVideoStats = (vid: number) => get<VideoStats>('/main/video/public/getVideoStatsById', { vid })

/** 首页/搜索：分页视频列表 */
export const fetchVideoPage = (query: VideoQueryVo) => get<PageRes<VideoVo>>('/main/video/public/pageVideo', query)

/** 实时推荐（按标签/权重） */
export const fetchRecommendVideos = (query: RecoQueryVo) =>
  get<VideoVo[]>('/main/video/public/getRealTimeRecommend', query)

/** 某个用户的投稿分页 */
export const fetchUserVideoPage = (query: VideoQueryVo) => get<PageRes<VideoVo>>('/main/video/pageVideo', query)
