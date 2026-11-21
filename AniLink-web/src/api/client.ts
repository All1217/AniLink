import axios, { type AxiosError, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { cookie } from '@/utils/cookie'
import type { ResultData } from '@/types'

const API_BASE = import.meta.env.VITE_API_BASE ?? '/api'
const CODE_SUCCESS = 200
/** 后端约定的 token 失效码 */
const CODE_EXPIRE = [305, 601, 602]
const TIMEOUT = 25_000

export const http = axios.create({
  baseURL: API_BASE,
  withCredentials: true,
  timeout: TIMEOUT,
})

/** 401 时的外部回调（避免 client 直接依赖 router 造成循环引用） */
let unauthorizedHandler: (() => void) | null = null
export function onUnauthorized(handler: () => void): void {
  unauthorizedHandler = handler
}

/** 请求拦截：注入 access-token，并把 refresh-token 回写成 cookie（后端按 cookie 续约） */
http.interceptors.request.use((config) => {
  const store = useUserStore()
  if (store.token) config.headers['access-token'] = store.token
  if (store.refreshToken) cookie.set('refresh-token', store.refreshToken)
  return config
})

/** 单飞的 token 续约：并发请求只触发一次刷新 */
let refreshing: Promise<boolean> | null = null
function refreshOnce(): Promise<boolean> {
  if (!refreshing) {
    refreshing = axios
      .get<ResultData<string>>(`${API_BASE}/main/public/refresh`, { withCredentials: true })
      .then((res) => {
        if (res.data.code === CODE_SUCCESS) {
          useUserStore().setToken(res.data.data)
          return true
        }
        return false
      })
      .catch(() => false)
      .finally(() => {
        refreshing = null
      })
  }
  return refreshing
}

/** 命中失效码：先续约再重放一次，仍失败则登出并要求重新登录 */
async function retryAfterRefresh(config: AxiosRequestConfig & { retried?: boolean }) {
  const store = useUserStore()
  // 本来就没登录：不续约、不弹登录框，静默失败交给调用方决定是否提示
  if (!store.token) return Promise.reject(new Error('unauthorized'))

  if (!config.retried && (await refreshOnce())) {
    config.retried = true
    return http.request(config)
  }
  store.reset()
  ElMessage.error('登录已过期，请重新登录')
  unauthorizedHandler?.()
  return Promise.reject(new Error('unauthorized'))
}

/** 响应拦截：成功直接解包 data；业务失败统一提示；失效码走续约 */
http.interceptors.response.use(
  (response) => {
    const refreshToken = response.headers['refresh-token']
    if (refreshToken) useUserStore().setRefreshToken(String(refreshToken))

    const body = response.data as ResultData<unknown>
    if (body && body.code === CODE_SUCCESS) return body.data as never
    if (body && CODE_EXPIRE.includes(body.code)) {
      return retryAfterRefresh(response.config) as never
    }
    ElMessage.error(body?.message || '请求失败')
    return Promise.reject(body)
  },
  (error: AxiosError<ResultData>) => {
    const status = error.response?.status ?? 0
    if (CODE_EXPIRE.includes(status)) {
      return retryAfterRefresh(error.config ?? {}) as never
    }
    const message = error.response?.data?.message || error.message || '网络异常'
    ElMessage.error(message)
    return Promise.reject(error)
  },
)

export function get<T>(url: string, params?: object, config?: AxiosRequestConfig): Promise<T> {
  return http.get(url, { params, ...config }) as unknown as Promise<T>
}

export function post<T = void>(url: string, data?: object, config?: AxiosRequestConfig): Promise<T> {
  return http.post(url, data, config) as unknown as Promise<T>
}

export function put<T = void>(url: string, data?: object, config?: AxiosRequestConfig): Promise<T> {
  return http.put(url, data, config) as unknown as Promise<T>
}

export function del<T = void>(url: string, data?: object, config?: AxiosRequestConfig): Promise<T> {
  return http.delete(url, { data, ...config }) as unknown as Promise<T>
}
