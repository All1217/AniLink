/** 读取本地视频文件时长（秒）：投稿提交时后端需要 duration，解析失败返回 0 */
export function readVideoDuration(file: File): Promise<number> {
  return new Promise((resolve) => {
    const url = URL.createObjectURL(file)
    const video = document.createElement('video')
    video.preload = 'metadata'
    video.onloadedmetadata = () => {
      const value = video.duration
      URL.revokeObjectURL(url)
      resolve(Number.isFinite(value) ? Math.round(value) : 0)
    }
    video.onerror = () => {
      URL.revokeObjectURL(url)
      resolve(0)
    }
    video.src = url
  })
}
