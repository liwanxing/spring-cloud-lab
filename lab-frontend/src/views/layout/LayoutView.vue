<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getCurrentUser, logout } from '@/api/auth'
import { ElMessage } from 'element-plus'
import { House, User, Goods, Fold, Expand, ArrowDown } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const currentUser = ref<any>(null)
const isCollapse = ref(false)

onMounted(async () => {
  try {
    const res: any = await getCurrentUser()
    currentUser.value = res.data
  } catch (err: any) {
    ElMessage.error('获取用户信息失败')
  }
})

async function handleLogout() {
  try { await logout() } catch {}
  userStore.logout()
  ElMessage.success('已退出')
  router.push('/login')
}
</script>

<template>
  <el-container class="layout-container">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="layout-aside">
      <div class="logo">{{ isCollapse ? 'CM' : 'Cloud Mall Lab' }}</div>
      <el-menu :default-active="route.path" router :collapse="isCollapse"
        background-color="#304156" text-color="#bfcbd9" active-text-color="#409eff">
        <el-menu-item index="/dashboard">
          <el-icon><House /></el-icon>
          <template #title>首页</template>
        </el-menu-item>
        <el-menu-item index="/users">
          <el-icon><User /></el-icon>
          <template #title>用户管理</template>
        </el-menu-item>
        <el-menu-item index="/products">
          <el-icon><Goods /></el-icon>
          <template #title>商品管理</template>
        </el-menu-item>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
            <Fold v-if="!isCollapse" /><Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/" style="margin-left: 16px">
            <el-breadcrumb-item :to="{ path: '/dashboard' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.name !== 'dashboard'">{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="(cmd: string) => cmd === 'logout' && handleLogout()">
            <span class="user-info">{{ currentUser?.username || '未登录' }}<el-icon><ArrowDown /></el-icon></span>
            <template #dropdown>
              <el-dropdown-menu><el-dropdown-item command="logout">退出登录</el-dropdown-item></el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="layout-main"><router-view /></el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.layout-container { height: 100vh; }
.layout-aside { background-color: #304156; transition: width 0.3s; overflow: hidden; }
.logo { height: 60px; line-height: 60px; text-align: center; color: #fff; font-size: 18px; font-weight: bold; background-color: #263445; }
.layout-header { display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #eee; background: #fff; }
.header-left { display: flex; align-items: center; }
.collapse-btn { font-size: 20px; cursor: pointer; }
.user-info { display: flex; align-items: center; gap: 4px; cursor: pointer; }
.layout-main { background-color: #f0f2f5; }
</style>
