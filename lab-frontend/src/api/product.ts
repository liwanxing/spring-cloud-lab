import request from '@/utils/request'
export function getProductList(params: { page: number; size: number; name?: string; status?: number }) {
  return request.get('/products', { params })
}
export function createProduct(data: any) { return request.post('/products', data) }
export function updateProduct(id: number, data: any) { return request.put(`/products/${id}`, data) }
export function deleteProduct(id: number) { return request.delete(`/products/${id}`) }
