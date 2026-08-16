import request from '@/utils/request'

export function login(data: { username: string; password: string }) {
  return request.post('/auth/login', data)
}

export function logout() {
  return request.post('/auth/logout')
}
