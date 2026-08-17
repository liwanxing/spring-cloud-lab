import request from '@/utils/request'

export function getUserList(params: { page: number; size: number; username?: string; status?: number }) {
  return request.get('/users', { params })
}
export function createUser(data: any) { return request.post('/users', data) }
export function updateUser(id: number, data: any) { return request.put(`/users/${id}`, data) }
export function deleteUser(id: number) { return request.delete(`/users/${id}`) }
