import request from '@/utils/request'
export function getOrderList(params: { page: number; size: number; status?: string }) {
  return request.get('/orders', { params })
}
export function createOrder(data: { items: { productId: number; quantity: number }[] }) {
  return request.post('/orders', data)
}
export function createOrderFromCart() {
  return request.post('/orders/from-cart')
}
export function cancelOrder(id: number) {
  return request.put(`/orders/${id}/cancel`)
}
export function payOrder(id: number) {
  return request.put(`/orders/${id}/pay`)
}
