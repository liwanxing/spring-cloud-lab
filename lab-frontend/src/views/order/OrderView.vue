<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getOrderList, cancelOrder, payOrder } from '@/api/order'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref<any[]>([])
const loading = ref(false)
const total = ref(0)
const query = reactive({ page: 1, size: 10, status: undefined as string | undefined })

const statusMap: Record<string, { label: string; type: string }> = {
  PENDING: { label: '待支付', type: 'warning' },
  PAID: { label: '已支付', type: 'primary' },
  SHIPPED: { label: '已发货', type: '' },
  COMPLETED: { label: '已完成', type: 'success' },
  CANCELLED: { label: '已取消', type: 'info' },
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await getOrderList(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (err: any) { ElMessage.error(err.message || '查询失败') }
  finally { loading.value = false }
}

function handleSearch() { query.page = 1; loadData() }
function handleReset() { query.status = undefined; query.page = 1; loadData() }
function handleSizeChange() { query.page = 1; loadData() }

async function handlePay(row: any) {
  try {
    await ElMessageBox.confirm(`确认支付 ¥${row.totalAmount}？`, '模拟支付', { type: 'warning' })
    await payOrder(row.id)
    ElMessage.success('支付成功')
    loadData()
  } catch (err: any) { if (err?.message) ElMessage.error(err.message || '支付失败') }
}

async function handleCancel(row: any) {
  try {
    await ElMessageBox.confirm('确认取消该订单？', '提示', { type: 'warning' })
    await cancelOrder(row.id)
    ElMessage.success('订单已取消')
    loadData()
  } catch {}
}

onMounted(() => loadData())
</script>

<template>
  <el-card>
    <el-form :inline="true" :model="query" style="margin-bottom: 16px">
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
          <el-option label="待支付" value="PENDING" />
          <el-option label="已支付" value="PAID" />
          <el-option label="已发货" value="SHIPPED" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>
    <el-divider />
    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column type="expand">
        <template #default="{ row }">
          <el-table :data="row.items" style="margin: 8px 40px" size="small" border>
            <el-table-column prop="productName" label="商品名称" min-width="150" />
            <el-table-column prop="productPrice" label="单价" width="100">
              <template #default="{ row: item }">¥{{ item.productPrice }}</template>
            </el-table-column>
            <el-table-column prop="quantity" label="数量" width="80" />
            <el-table-column prop="itemAmount" label="小计" width="100">
              <template #default="{ row: item }"><span style="color: #f56c6c">¥{{ item.itemAmount }}</span></template>
            </el-table-column>
          </el-table>
        </template>
      </el-table-column>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="orderNo" label="订单号" width="280" show-overflow-tooltip />
      <el-table-column prop="itemCount" label="商品种类" width="90" />
      <el-table-column prop="totalAmount" label="总金额" width="100">
        <template #default="{ row }">
          <span style="color: #f56c6c; font-weight: bold">¥{{ row.totalAmount }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="(statusMap[row.status]?.type as any) || ''">
            {{ statusMap[row.status]?.label || row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="下单时间" width="170" />
      <el-table-column prop="paidAt" label="支付时间" width="170">
        <template #default="{ row }">{{ row.paidAt || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 'PENDING'" size="small" type="success" @click="handlePay(row)">支付</el-button>
          <el-button v-if="row.status === 'PENDING'" size="small" type="danger" @click="handleCancel(row)">取消</el-button>
          <span v-if="row.status !== 'PENDING'" style="color: #999; font-size: 12px">-</span>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :page-sizes="[10, 20, 50]" :total="total" layout="total, sizes, prev, pager, next" @size-change="handleSizeChange" @current-change="loadData" style="margin-top: 16px; justify-content: flex-end" />
    <el-empty v-if="!loading && tableData.length === 0" description="暂无订单" />
  </el-card>
</template>
