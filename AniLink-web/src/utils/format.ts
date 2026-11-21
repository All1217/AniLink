import { DateStringType } from '@/types'

const pad = (n: number): string => String(n).padStart(2, '0')

/** 播放量/弹幕数格式化：12345 -> 1.2 万，123456789 -> 1.2 亿 */
export function formatCount(num?: number | null): string {
  if (!num || num < 0) return '0'
  if (num < 10000) return String(num)
  if (num < 100000000) return `${trim(num / 10000)} 万`
  return `${trim(num / 100000000)} 亿`
}

function trim(n: number): string {
  const v = n >= 100 ? Math.round(n) : Math.round(n * 10) / 10
  return String(v)
}

/** 秒 -> mm:ss / hh:mm:ss */
export function formatDuration(sec?: number | null): string {
  if (!sec || sec <= 0) return '00:00'
  const s = Math.floor(sec % 60)
  const m = Math.floor((sec / 60) % 60)
  const h = Math.floor(sec / 3600)
  return h > 0 ? `${pad(h)}:${pad(m)}:${pad(s)}` : `${pad(m)}:${pad(s)}`
}

/** 按后端需要的格式输出时间字符串 */
export function formatDate(input: number | Date | string, type: DateStringType): string {
  const d = input instanceof Date ? input : new Date(input)
  if (Number.isNaN(d.getTime())) return ''
  const Y = d.getFullYear()
  const M = pad(d.getMonth() + 1)
  const D = pad(d.getDate())
  const h = pad(d.getHours())
  const mi = pad(d.getMinutes())
  const s = pad(d.getSeconds())
  switch (type) {
    case DateStringType.Y_M_D:
      return `${Y}-${M}-${D}`
    case DateStringType.Y_M_D_H_M:
      return `${Y}-${M}-${D} ${h}:${mi}`
    case DateStringType.M_D:
      return `${M}-${D}`
    case DateStringType.MON_D_H_MIN:
      return `${M}-${D} ${h}:${mi}`
    case DateStringType.ALL:
    default:
      return `${Y}-${M}-${D} ${h}:${mi}:${s}`
  }
}

/** 相对时间：刚刚 / x 分钟前 / x 小时前 / x 天前 / 日期 */
export function fromNow(input?: string | number | Date): string {
  if (!input) return ''
  const t = new Date(input).getTime()
  if (Number.isNaN(t)) return ''
  const diff = Date.now() - t
  if (diff < 60_000) return '刚刚'
  if (diff < 3_600_000) return `${Math.floor(diff / 60_000)} 分钟前`
  if (diff < 86_400_000) return `${Math.floor(diff / 3_600_000)} 小时前`
  if (diff < 30 * 86_400_000) return `${Math.floor(diff / 86_400_000)} 天前`
  return formatDate(t, DateStringType.Y_M_D)
}

/** 路由参数转数字（非法值返回 0） */
export function toNumber(v: unknown): number {
  const n = Number(v)
  return Number.isFinite(n) ? n : 0
}
