import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    { path: '/login', name: 'login', component: () => import('@/views/login/LoginView.vue') },
    {
      // LayoutView 是布局视图
      path: '/', component: () => import('@/views/layout/LayoutView.vue'), redirect: '/dashboard',
      children: [
        { path: 'dashboard', name: 'dashboard', component: () => import('@/views/dashboard/DashboardView.vue'), meta: { title: '首页' } },
        { path: 'users', name: 'users', component: () => import('@/views/home/HomeView.vue'), meta: { title: '用户管理' } },
        { path: 'products', name: 'products', component: () => import('@/views/product/ProductView.vue'), meta: { title: '商品管理' } },
        { path: 'orders', name: 'orders', component: () => import('@/views/order/OrderView.vue'), meta: { title: '订单管理' } },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const userStore = useUserStore()
  if (to.name !== 'login' && !userStore.token) return { name: 'login' }
  document.title = (to.meta.title as string) || 'Cloud Mall Lab'
})

export default router
