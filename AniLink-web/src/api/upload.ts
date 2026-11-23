import { post } from './client'
import type { Video } from '@/types'

/** 上传视频文件（大文件，超时 10 分钟，带进度回调；signal 用于更换视频时中断旧请求） */
export function uploadVideo(file: File, onProgress?: (percent: number) => void, signal?: AbortSignal) {
  const form = new FormData()
  form.append('file', file)
  return post<string>('/main/file/upload/video', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 10 * 60 * 1000,
    signal,
    onUploadProgress: (event) => {
      if (!onProgress || !event.total) return
      onProgress(Math.round((event.loaded * 100) / event.total))
    },
  })
}

/** 上传视频封面 */
export function uploadCover(file: File) {
  const form = new FormData()
  form.append('file', file)
  return post<string>('/main/file/upload/video/cover', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 上传头像 */
export function uploadAvatar(file: File) {
  const form = new FormData()
  form.append('file', file)
  return post<string>('/main/file/upload/avatar', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 提交投稿（视频 URL + 封面 + 标题/分区/标签/简介） */
export const submitVideo = (payload: Partial<Video>) => post<string>('/main/file/submit', payload)
