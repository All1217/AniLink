export interface Result<T> {
  code: number
  message: string
  data: T
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

export interface AdminMe {
  uid: number
  username: string
  nickname: string
  role: number
}

export interface AdminVideo {
  vid: number
  uid: number
  title: string
  status: number
  visible: number
  transcodeStatus: number
  coverUrl: string
  videoUrl: string
  hlsUrl: string
  authorName: string
  uploadDate: string
  deleteDate: string | null
}

export interface AdminUser {
  uid: number
  username: string
  nickname: string
  avatar: string
  status: number
  role: number
  createDate: string
}

export interface AdminResetPassword {
  uid: number
  username: string
  tempPassword: string
}

export interface AdminObject {
  bucket: string
  key: string
  size: number
  lastModified: string
}

export interface AdminObjectPage {
  items: AdminObject[]
  nextCursor: string | null
  hasMore: boolean
}

export interface AdminRef {
  type: string
  id: number
  name: string
  field: string
}

export interface AdminCleanupResult {
  scanned: number
  deleted: number
  skipped: number
  failed: number
  failedKeys: string[]
}
