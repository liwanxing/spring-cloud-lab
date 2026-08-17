import request from '@/utils/request'
export function getCartList() { return request.get('/cart') }
export function addToCart(data: { productId: number; quantity: number }) { return request.post('/cart', data) }
export function updateCartQuantity(id: number, quantity: number) { return request.put(`/cart/${id}`, { quantity }) }
export function removeFromCart(id: number) { return request.delete(`/cart/${id}`) }
export function clearCart() { return request.delete('/cart') }
