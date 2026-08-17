import request from '@/utils/request'
export function getOrderList(params: { page: number; size: number; status?: string }) {
  return request.get('/orders', { params })
}
export function createOrder(data: { productId: number; quantity: number }) {
  return request.post('/orders', data)
}
export function cancelOrder(id: number) {
  return request.put(`/orders/${id}/cancel`)
}
