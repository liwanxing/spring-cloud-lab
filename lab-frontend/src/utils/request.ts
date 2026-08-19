import axios from 'axios'
import { useUserStore } from '@/stores/user'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

request.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = userStore.token
  }
  return config
})

// 踢回登录页：携带当前页 fullPath 作为 redirect，登录后跳回原页面（已在登录页则不携带）
function kickToLogin() {
  const userStore = useUserStore()
  const current = router.currentRoute.value
  userStore.logout()
  if (current.path === '/login') return
  router.push({ path: '/login', query: { redirect: current.fullPath } })
}

request.interceptors.response.use(
  (response) => {
    const res = response.data
    if (res.code !== 200) {
      if (res.code === 401) {
        kickToLogin()
      }
      // code 挂到 Error 上：调用方（如 LayoutView）据此区分 401 被踢与其他失败，避免误导性弹窗
      const err = new Error(res.message || '请求失败')
      ;(err as any).code = res.code
      return Promise.reject(err)
    }
    return res
  },
  (error) => {
    if (error.response?.status === 401) {
      kickToLogin()
    }
    // 提取后端统一 Result 的 message（限流 429 / 业务错误 400 / 服务不可用 503），没有才退回 axios 默认文案
    const msg = error.response?.data?.message
    const err = new Error(msg || error.message || '请求失败')
    ;(err as any).code = error.response?.status
    return Promise.reject(err)
  }
)

export default request
