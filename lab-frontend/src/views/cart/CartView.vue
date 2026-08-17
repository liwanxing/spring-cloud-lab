<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { getCartList, updateCartQuantity, removeFromCart, clearCart } from '@/api/cart'
import { createOrderFromCart } from '@/api/order'
import { ElMessage, ElMessageBox } from 'element-plus'

const cartItems = ref<any[]>([])
const loading = ref(false)

const totalPrice = computed(() => cartItems.value.reduce((s, i) => s + i.productPrice * i.quantity, 0).toFixed(2))
const totalCount = computed(() => cartItems.value.reduce((s, i) => s + i.quantity, 0))

async function loadData() {
  loading.value = true
  try {
    const res: any = await getCartList()
    cartItems.value = res.data
  } catch (err: any) { ElMessage.error(err.message || '查询失败') }
  finally { loading.value = false }
}

async function handleQuantityChange(item: any) {
  try { await updateCartQuantity(item.id, item.quantity) }
  catch (err: any) { ElMessage.error(err.message); loadData() }
}

async function handleRemove(id: number) {
  try { await removeFromCart(id); ElMessage.success('已移除'); loadData() } catch {}
}

async function handleClear() {
  try {
    await ElMessageBox.confirm('确认清空购物车？', '提示', { type: 'warning' })
    await clearCart(); ElMessage.success('已清空'); loadData()
  } catch {}
}

async function handleCheckout() {
  if (cartItems.value.length === 0) { ElMessage.warning('购物车为空'); return }
  try {
    const res: any = await createOrderFromCart()
    ElMessage.success('下单成功，共 ' + res.data.itemCount + ' 件商品')
    loadData()
  } catch (err: any) { ElMessage.error(err.message || '结算失败') }
}

onMounted(() => loadData())
</script>

<template>
  <el-card v-loading="loading">
    <template #header>
      <div style="display: flex; justify-content: space-between; align-items: center">
        <span>购物车</span>
        <el-button link type="danger" @click="handleClear" v-if="cartItems.length > 0">清空购物车</el-button>
      </div>
    </template>
    <el-table :data="cartItems" stripe>
      <el-table-column prop="productName" label="商品名称" min-width="200" />
      <el-table-column prop="productPrice" label="单价" width="120">
        <template #default="{ row }">¥{{ row.productPrice }}</template>
      </el-table-column>
      <el-table-column label="数量" width="180">
        <template #default="{ row }">
          <el-input-number v-model="row.quantity" :min="1" size="small" @change="handleQuantityChange(row)" />
        </template>
      </el-table-column>
      <el-table-column label="小计" width="120">
        <template #default="{ row }">
          <span style="color: #f56c6c; font-weight: bold">¥{{ row.itemAmount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80">
        <template #default="{ row }">
          <el-button link type="danger" @click="handleRemove(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <div v-if="cartItems.length > 0" style="margin-top: 20px; display: flex; justify-content: space-between; align-items: center">
      <span style="color: #909399">共 {{ totalCount }} 件</span>
      <div style="display: flex; align-items: center; gap: 16px">
        <span style="color: #909399">合计：</span>
        <span style="color: #f56c6c; font-size: 24px; font-weight: bold">¥{{ totalPrice }}</span>
        <el-button type="primary" size="large" @click="handleCheckout">结算</el-button>
      </div>
    </div>
    <el-empty v-if="!loading && cartItems.length === 0" description="购物车是空的">
      <el-button type="primary" @click="$router.push('/products')">去逛逛</el-button>
    </el-empty>
  </el-card>
</template>
