import axios, { type AxiosError, type AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'
import type { ResultData } from '@/types'

const API_BASE = import.meta.env.VITE_API_BASE ?? '/api'
const CODE_SUCCESS = 200
/** 601：访问令牌过期，可以静默续约 */
const CODE_REFRESH = 601
/** 602：令牌非法 / 刷新令牌已失效 / 检测到重用，必须重新登录 */
const CODE_INVALID = 602
/** 305：未登录（没有携带令牌） */
const CODE_NO_LOGIN = 305
const TIMEOUT = 25_000

export const http = axios.create({
  baseURL: API_BASE,
  withCredentials: true,
  timeout: TIMEOUT,
})

/** 需要重新登录时的外部回调（避免 client 直接依赖 router 造成循环引用） */
let unauthorizedHandler: (() => void) | null = null
export function onUnauthorized(handler: () => void): void {
  unauthorizedHandler = handler
}

/** 请求拦截：只注入 access-token；refresh-token 由服务端 HttpOnly Cookie 自动携带，前端不碰 */
http.interceptors.request.use((config) => {
  const store = useUserStore()
  if (store.token) config.headers['access-token'] = store.token
  return config
})

/**
 * 单飞的令牌续约：并发请求共享同一个刷新 Promise，避免多标签页/并发请求互相踢下线。
 * 刷新接口会同时轮换 refresh-token（服务端写 Set-Cookie），浏览器自动更新 Cookie。
 */
let refreshing: Promise<boolean> | null = null
function refreshOnce(): Promise<boolean> {
  if (!refreshing) {
    refreshing = axios
      .get<ResultData<string>>(`${API_BASE}/main/public/refresh`, {
        withCredentials: true,
        // 刷新请求本身不需要 access-token，避免把过期令牌带进去
        headers: {},
      })
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

/** 会话失效：清本地状态并唤起登录 */
function forceLogout(message = '登录已过期，请重新登录') {
  useUserStore().reset()
  ElMessage.error(message)
  unauthorizedHandler?.()
}

/** 601：先续约再重放一次；续约失败才登出 */
async function retryAfterRefresh(config: AxiosRequestConfig & { retried?: boolean }) {
  const store = useUserStore()
  // 本来就没有令牌：不续约、不弹错，交给调用方处理
  if (!store.token) return Promise.reject(new Error('unauthorized'))

  if (!config.retried && (await refreshOnce())) {
    config.retried = true
    return http.request(config)
  }
  forceLogout()
  return Promise.reject(new Error('unauthorized'))
}

/** 响应拦截：成功直接解包 data；601 续约、602 登出、305 视为未登录 */
http.interceptors.response.use(
  (response) => {
    const body = response.data as unknown

    // 后端有返回 void 的接口（如播放进度上报 /main/video/interact/record/add），响应体为空
    if (body === undefined || body === null || body === '') {
      return undefined as never
    }
    // 非 Result 结构（如文件流、纯文本）原样返回，避免误判为业务失败
    if (typeof body !== 'object' || !('code' in body)) {
      return body as never
    }

    const result = body as ResultData<unknown>
    if (result.code === CODE_SUCCESS) return result.data as never

    if (result.code === CODE_REFRESH) {
      return retryAfterRefresh(response.config) as never
    }
    if (result.code === CODE_INVALID) {
      forceLogout('登录状态已失效，请重新登录')
      return Promise.reject(result)
    }
    if (result.code === CODE_NO_LOGIN) {
      useUserStore().reset()
      return Promise.reject(result)
    }
    ElMessage.error(result.message || '请求失败')
    return Promise.reject(result)
  },
  (error: AxiosError<ResultData>) => {
    const status = error.response?.status ?? 0
    const code = error.response?.data?.code ?? 0
    // 网关异常处理器也可能用 HTTP 状态码透传 601/602
    if (status === CODE_REFRESH || code === CODE_REFRESH) {
      return retryAfterRefresh(error.config ?? {}) as never
    }
    if (status === CODE_INVALID || code === CODE_INVALID) {
      forceLogout('登录状态已失效，请重新登录')
      return Promise.reject(error)
    }
    // 305 表示这次请求没有登录态（例如组件在登录前/登出后触发），静默失败，不弹错误
    if (status === CODE_NO_LOGIN || code === CODE_NO_LOGIN) {
      return Promise.reject(error)
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
