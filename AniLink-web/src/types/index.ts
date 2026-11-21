/** 通用响应结构（后端 Result / ResultData） */
export interface Result {
  code: number
  message: string
  success?: boolean
}

export interface ResultData<T = unknown> extends Result {
  data: T
}

export interface PageRes<T> {
  records: T[]
  total: number
  current?: number
  size?: number
}

export interface PageQuery {
  current?: number
  size?: number
}

/** 与视频的交互类型，取值必须与后端 Interaction 枚举一致 */
export enum Interaction {
  LIKE = 0,
  DISLIKE = 1,
  COIN_ONE = 2,
  COIN_TWO = 3,
  COLLECT = 4,
  SHARE = 5,
  PLAY = 6,
}

export enum DateStringType {
  ALL = 1,
  MON_D_H_MIN = 2,
  Y_M_D = 3,
  Y_M_D_H_M = 4,
  M_D = 5,
}

export interface Video {
  vid: number
  uid: number
  title: string
  type: number
  auth: number
  duration: number
  mcId: number
  scId: number
  status: number
  visible: number
  tags: string
  descr: string
  coverUrl: string
  videoUrl: string
  uploadDate?: string
  deleteDate?: string
}

export interface VideoStats {
  vid: number
  play: number
  danmu: number
  good: number
  bad?: number
  coin: number
  collect?: number
  share?: number
  comment?: number
}

/** 列表页用的视频卡片数据（VideoStats + 作者与封面信息） */
export interface VideoVo extends VideoStats {
  uid: number
  nickname: string
  title: string
  duration: number
  coverUrl: string
  uploadDate: string
}

export interface VideoQueryVo extends PageQuery {
  title?: string | null
  sortType?: number | null
  startTime?: string | null
  endTime?: string | null
  startDuration?: number | null
  endDuration?: number | null
  mcId?: number | null
  sortField?: string | null
  uid?: number
}

export interface RecoQueryVo {
  uid?: number
  count?: number
  vid?: number
  tag?: number
  tagName?: string
}

export enum Gender {
  MALE = 0,
  FEMALE = 1,
  UNKNOWN = 2,
}

export enum AccountStatus {
  NORMAL = 0,
  FORBIDDEN = 1,
  LOGOUT = 2,
}

export enum AccountRole {
  NORMAL = 0,
  ADMIN = 1,
  SUPER_ADMIN = 2,
}

export interface User {
  uid: number
  username: string
  nickname: string
  avatar: string
  background?: string
  gender: Gender
  description: string
  coin?: number
  status: AccountStatus
  role: AccountRole
  createDate: Date | string
  deleteDate?: Date | string
}

export interface UserStats {
  uid: number
  fansCount: number
  followCount: number
  likeCount: number
  playCount: number
  videoCount: number
  collectionCount: number
  collectCount: number
  birthDate: Date | string
  school: string
  notice: string
}

export interface UserVo {
  uid: number
  nickname: string
  description: string
  gender: number
  birthDate: string
  school: string
}

/** 用户对某视频的交互记录 */
export interface UserVideo {
  id: number
  uid: number
  vid: number
  play: number
  love: number
  unlove: number
  coin: number
  collect: number
  playTime: string
  loveTime: string
  coinTime: string
  share?: number
}

export interface Danmu {
  id?: number
  uid: number
  vid: number
  content: string
  fontsize: number
  mode: number
  color: string
  timePoint: number
  status: number
  createDate: string
}

export interface Comment {
  id: number
  vid: number
  uid: number
  rootId: number
  parentId: number
  toUserId: number
  content: string
  love: number
  bad: number
  createTime: string
  isTop: number
  isDeleted: number
}

export interface CommentVo extends Comment {
  nickname: string
  avatar: string
  replyCount: number
}

export interface CommentQuery extends PageQuery {
  id?: number
  sortField?: string
  vid: number
  uid?: number
  rootId?: number
  toUserId?: number
  parentId?: number
}

export interface Favorite {
  fid: number
  uid: number
  type: number
  visible: number
  cover: string
  title: string
  description: string
  count: number
  isDelete: number
}

export interface VideoFavoriteVo extends VideoVo {
  time: string
}

export interface Follow {
  id?: number
  followerId: number
  followeeId: number
  createdAt?: string
}

export interface BrowseHistory {
  id?: number
  uid: number
  vid: number
  viewTime: string
  duration: number
  isFinish: number
  device: string
  ip: string
  isDelete: number
}

/** 观看历史列表项：BrowseHistory + 视频与作者信息（后端 HistoryVo） */
export interface HistoryVo extends BrowseHistory {
  authorUid: number
  nickname: string
  title: string
  videoDuration: number
  coverUrl: string
}

export interface ChatVo {
  id: number
  senderId: number
  targetId: number
  latestTime: string
  unreadCount: number
  nickname: string
  avatar: string
}

export interface ChatMessage {
  id: number
  targetId: number
  senderId: number
  content: string
  senderDel: number
  targetDel: number
  withdraw: number
  messageType: number
  time: string
  readStatus: number
}

export interface Chat {
  id: number
  senderId: number
  targetId: number
  latestTime: string
}

/** WS 私信通道的信封 */
export interface ChatMessageDTO {
  from: number
  to: number
  type: 'auth' | 'private' | 'heartbeat'
  content?: string
}

/** WS 播放页在线人数的信封 */
export interface CountMessageDTO {
  vid: number
  type: 'join' | 'heartbeat'
}

export interface CountMessageVO {
  type: string
  count: number
}
