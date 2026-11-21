import type { HotSearch } from '@/api/stats'

/** B 站热搜跳转地址：优先用数据源自带的 url/uri，否则回退到搜索结果页 */
export function hotSearchUrl(item: HotSearch): string {
  const candidates = [item.url, item.uri]
  for (const raw of candidates) {
    const value = raw?.trim()
    if (!value) continue
    if (value.startsWith('http://') || value.startsWith('https://')) return value
    if (value.startsWith('//')) return `https:${value}`
    if (value.startsWith('/')) return `https://www.bilibili.com${value}`
  }
  const word = encodeURIComponent(item.keyword || item.show_name || '')
  return `https://search.bilibili.com/all?keyword=${word}&from_source=webtop_search&spm_id_from=333.1007&search_source=4`
}
