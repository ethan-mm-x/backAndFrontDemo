import http from './http'

export interface ApiResult<T> {
  code: number
  message: string
  data: T
}

export function fetchCaptcha() {
  return http.get<ApiResult<{ captchaId: string; imageBase64: string }>>('/auth/captcha')
}

export function register(data: {
  username: string
  password: string
  captchaId: string
  captchaCode: string
}) {
  return http.post<ApiResult<null>>('/auth/register', data)
}

export function login(data: {
  username: string
  password: string
  captchaId: string
  captchaCode: string
}) {
  return http.post<ApiResult<{ token: string; userId: number; username: string }>>('/auth/login', data)
}

export function pageUsers(params: { page: number; size: number; username?: string }) {
  return http.get<
    ApiResult<{
      records: { id: number; username: string; createdAt: string }[]
      total: number
      current: number
      size: number
    }>
  >('/users', { params })
}

export function batchDeleteUsers(ids: number[]) {
  return http.delete<ApiResult<null>>('/users/batch', { data: { ids } })
}
