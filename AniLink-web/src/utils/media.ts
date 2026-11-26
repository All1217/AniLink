/** 图片取址与占位：列表用 480w 缩略图，详情/播放器海报用 960w 主封面。 */

export interface CoverSource {
  coverUrl?: string | null
  coverThumbUrl?: string | null
}

/** 列表、历史、相关推荐共用的封面占位图（16:10 灰块，避免裂图与布局跳动） */
export const COVER_PLACEHOLDER =
  'data:image/svg+xml;charset=utf-8,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="320" height="200"><rect width="320" height="200" fill="#e3e5e7"/><text x="160" y="105" font-size="14" fill="#9499a0" text-anchor="middle">暂无封面</text></svg>',
  )

/** 默认头像占位图：本地 SVG，不依赖外站或内网地址 */
export const AVATAR_PLACEHOLDER =
  'data:image/svg+xml;charset=utf-8,' +
  encodeURIComponent(
    '<svg xmlns="http://www.w3.org/2000/svg" width="96" height="96"><rect width="96" height="96" fill="#e3e5e7"/><circle cx="48" cy="38" r="16" fill="#c9ccd0"/><path d="M20 84c4-16 18-24 28-24s24 8 28 24z" fill="#c9ccd0"/></svg>',
  )

/** 列表封面：优先 480w 缩略图，旧数据没有缩略图时回退主封面 */
export function coverThumb(source: CoverSource): string {
  return source.coverThumbUrl || source.coverUrl || COVER_PLACEHOLDER
}

/** 480/960 两档 srcset；两档相同或缺失时返回 undefined，让浏览器只用 src */
export function coverSrcset(source: CoverSource): string | undefined {
  const thumb = source.coverThumbUrl
  const large = source.coverUrl
  if (!thumb || !large || thumb === large) return undefined
  return `${thumb} 480w, ${large} 960w`
}
