import axios from 'axios'
import type { Result } from './types'

export const TOKEN_KEY = 'anilink-admin-token'

const http = axios.create({
  baseURL: '/api',
  timeout: 20000,
  withCredentials: true,
})

http.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers['access-token'] = token
  }
  return config
})

http.interceptors.response.use((response) => {
  const body = response.data as Result<unknown> | undefined
  if (body && [305, 601, 602].includes(body.code)) {
    localStorage.removeItem(TOKEN_KEY)
    if (!window.location.hash.startsWith('#/login')) {
      window.location.hash = '#/login'
    }
  }
  return response
})

export default http
