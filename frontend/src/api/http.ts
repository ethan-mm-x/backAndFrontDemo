import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const TOKEN_KEY = 'demo_token'

export function getToken(): string {
  return localStorage.getItem(TOKEN_KEY) || ''
}

export function setToken(token: string) {
  localStorage.setItem(TOKEN_KEY, token)
}

export function clearToken() {
  localStorage.removeItem(TOKEN_KEY)
}

const http = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

http.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  (resp) => {
    const newToken = resp.headers['x-new-token']
    if (newToken) {
      setToken(String(newToken))
    }
    const body = resp.data
    if (body && typeof body.code === 'number' && body.code !== 0) {
      ElMessage.error(body.message || '请求失败')
      if (body.code === 401) {
        clearToken()
        router.push('/login')
      }
      return Promise.reject(body)
    }
    return resp
  },
  (err) => {
    ElMessage.error(err?.message || '网络异常')
    return Promise.reject(err)
  },
)

export default http
