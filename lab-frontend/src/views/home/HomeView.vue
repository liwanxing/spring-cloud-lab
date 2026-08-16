<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { logout } from '@/api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

async function handleLogout() {
  try { await logout() } catch {}
  userStore.logout()
  ElMessage.success('已退出')
  router.push('/login')
}
</script>

<template>
  <div style="padding: 40px">
    <el-card>
      <h2>欢迎使用 Cloud Mall Lab</h2>
      <p>登录状态正常 ✅</p>
      <el-button type="danger" @click="handleLogout">退出登录</el-button>
    </el-card>
  </div>
</template>
