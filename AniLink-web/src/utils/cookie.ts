/**
 * 极简 cookie 工具：只用于 refresh-token（后端按 cookie 回填），
 * 不放业务数据——业务状态一律走 pinia + localStorage。
 */
export const cookie = {
  get(name: string): string {
    const target = document.cookie
      .split('; ')
      .find((item) => item.startsWith(`${name}=`))
    return target ? decodeURIComponent(target.slice(name.length + 1)) : ''
  },
  set(name: string, value: string, days = 7): void {
    const expires = new Date(Date.now() + days * 86_400_000).toUTCString()
    document.cookie = `${name}=${encodeURIComponent(value)}; expires=${expires}; path=/`
  },
  remove(name: string): void {
    document.cookie = `${name}=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/`
  },
}
