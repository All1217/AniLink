import http from './client'
import type {
  AdminCleanupResult,
  AdminMe,
  AdminObjectPage,
  AdminRef,
  AdminUser,
  AdminVideo,
  PageResult,
  Result,
} from './types'

type Params = Record<string, string | number | undefined>

export const adminApi = {
  login: (username: string, password: string) =>
    http.post<Result<string>>('/main/login', { username, password }).then((res) => res.data),
  me: () => http.get<Result<AdminMe>>('/main/admin/me').then((res) => res.data),
  logout: () => http.post('/main/logout'),

  videos: (params: Params) =>
    http.get<Result<PageResult<AdminVideo>>>('/main/admin/videos', { params }).then((res) => res.data),
  videoDetail: (vid: number) =>
    http.get<Result<AdminVideo>>(`/main/admin/videos/${vid}`).then((res) => res.data),
  videoStatus: (vid: number, status: number) =>
    http.put<Result<void>>(`/main/admin/videos/${vid}/status`, { status }).then((res) => res.data),
  videoVisible: (vid: number, visible: number) =>
    http.put<Result<void>>(`/main/admin/videos/${vid}/visible`, { visible }).then((res) => res.data),

  users: (params: Params) =>
    http.get<Result<PageResult<AdminUser>>>('/main/admin/users', { params }).then((res) => res.data),
  userStatus: (uid: number, status: number) =>
    http.put<Result<void>>(`/main/admin/users/${uid}/status`, { status }).then((res) => res.data),
  userRole: (uid: number, role: number) =>
    http.put<Result<void>>(`/main/admin/users/${uid}/role`, { role }).then((res) => res.data),

  buckets: () => http.get<Result<string[]>>('/main/admin/storage/buckets').then((res) => res.data),
  objects: (params: Params) =>
    http.get<Result<AdminObjectPage>>('/main/admin/storage/objects', { params }).then((res) => res.data),
  presign: (bucket: string, key: string) =>
    http
      .get<Result<string>>('/main/admin/storage/objects/presign', { params: { bucket, key } })
      .then((res) => res.data),
  deleteObject: (bucket: string, key: string) =>
    http
      .delete<Result<AdminRef[] | null>>('/main/admin/storage/objects', { params: { bucket, key } })
      .then((res) => res.data),
  cleanup: (bucket: string, prefix?: string) =>
    http
      .post<Result<AdminCleanupResult>>('/main/admin/storage/cleanup', null, { params: { bucket, prefix } })
      .then((res) => res.data),
}
