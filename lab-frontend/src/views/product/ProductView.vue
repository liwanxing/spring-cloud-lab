<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getProductList, createProduct, updateProduct, deleteProduct } from '@/api/product'
import { createOrder } from '@/api/order'
import { addToCart } from '@/api/cart'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref<any[]>([])
const loading = ref(false)
const total = ref(0)
const query = reactive({ page: 1, size: 10, name: '', status: undefined as number | undefined })

const dialogVisible = ref(false)
const dialogTitle = ref('新增商品')
const isEdit = ref(false)
const formRef = ref()
const form = reactive({ id: 0, name: '', description: '', price: 0, stock: 0, category: '', status: 1 })
const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }],
}

const orderDialogVisible = ref(false)
const orderProduct = ref<any>({})
const orderQuantity = ref(1)

async function loadData() {
  loading.value = true
  try {
    const res: any = await getProductList(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (err: any) { ElMessage.error(err.message || '查询失败') }
  finally { loading.value = false }
}

function handleSearch() { query.page = 1; loadData() }
function handleReset() { query.name = ''; query.status = undefined; query.page = 1; loadData() }
function handleSizeChange() { query.page = 1; loadData() }

function handleCreate() {
  orderDialogVisible.value = false
  isEdit.value = false; dialogTitle.value = '新增商品'
  Object.assign(form, { id: 0, name: '', description: '', price: 0, stock: 0, category: '', status: 1 })
  dialogVisible.value = true
}

function handleEdit(row: any) {
  orderDialogVisible.value = false
  isEdit.value = true; dialogTitle.value = '编辑商品'
  Object.assign(form, { id: row.id, name: row.name, description: row.description || '', price: row.price, stock: row.stock, category: row.category || '', status: row.status })
  dialogVisible.value = true
}

async function handleSubmit() {
  try { await formRef.value.validate() } catch { return }
  try {
    if (isEdit.value) {
      const data: any = {}
      if (form.name) data.name = form.name
      if (form.description) data.description = form.description
      if (form.price) data.price = form.price
      if (form.stock !== undefined) data.stock = form.stock
      if (form.category) data.category = form.category
      await updateProduct(form.id, data); ElMessage.success('更新成功')
    } else {
      await createProduct(form); ElMessage.success('创建成功')
    }
    dialogVisible.value = false; loadData()
  } catch (err: any) { ElMessage.error(err.message || '操作失败') }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm('确认删除商品？', '提示', { type: 'warning' })
    await deleteProduct(row.id); ElMessage.success('删除成功'); loadData()
  } catch {}
}

async function handleAddCart(row: any) {
  try {
    await addToCart({ productId: row.id, quantity: 1 })
    ElMessage.success('已加入购物车')
  } catch (err: any) { ElMessage.error(err.message || '加入失败') }
}

function handleOrder(row: any) {
  dialogVisible.value = false
  orderProduct.value = row
  orderQuantity.value = 1
  orderDialogVisible.value = true
}

async function submitOrder() {
  try {
    await createOrder({ productId: orderProduct.value.id, quantity: orderQuantity.value })
    ElMessage.success('下单成功')
    orderDialogVisible.value = false
    loadData()
  } catch (err: any) { ElMessage.error(err.message || '下单失败') }
}

async function handleStatusChange(row: any) {
  try { await updateProduct(row.id, { status: row.status }); ElMessage.success('状态已更新') }
  catch (err: any) { row.status = row.status === 1 ? 0 : 1; ElMessage.error(err.message || '更新失败') }
}

onMounted(() => loadData())
</script>

<template>
  <el-card>
    <el-form :inline="true" :model="query" style="margin-bottom: 16px">
      <el-form-item label="商品名称">
        <el-input v-model="query.name" placeholder="搜索商品" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="上架" :value="1" /><el-option label="下架" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>
    <el-divider />
    <div style="display: flex; justify-content: flex-end; margin-bottom: 12px">
      <el-button type="primary" @click="handleCreate">新增商品</el-button>
    </div>
    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="name" label="商品名称" min-width="180" show-overflow-tooltip />
      <el-table-column prop="category" label="分类" width="100" />
      <el-table-column prop="price" label="价格" width="100">
        <template #default="{ row }">¥{{ row.price }}</template>
      </el-table-column>
      <el-table-column prop="stock" label="库存" width="80" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="handleStatusChange(row)" />
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column prop="updatedAt" label="更新时间" width="170" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{ row }">
          <el-button v-if="row.status === 1 && row.stock > 0" link type="warning" @click="handleAddCart(row)">加购</el-button>
          <el-button v-if="row.status === 1 && row.stock > 0" link type="success" @click="handleOrder(row)">下单</el-button>
          <el-button link @click="handleEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="query.page" v-model:page-size="query.size" :page-sizes="[10, 20, 50]" :total="total" layout="total, sizes, prev, pager, next" @size-change="handleSizeChange" @current-change="loadData" style="margin-top: 16px; justify-content: flex-end" />
    <el-empty v-if="!loading && tableData.length === 0" description="暂无数据" />
  </el-card>

  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="500px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="商品名称" prop="name"><el-input v-model="form.name" placeholder="请输入商品名称" /></el-form-item>
      <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="3" placeholder="商品描述（选填）" /></el-form-item>
      <el-form-item label="价格" prop="price"><el-input-number v-model="form.price" :min="0.01" :precision="2" :step="1" /></el-form-item>
      <el-form-item label="库存" prop="stock"><el-input-number v-model="form.stock" :min="0" :step="10" /></el-form-item>
      <el-form-item label="分类"><el-input v-model="form.category" placeholder="如：手机、电脑、配件" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>

  <el-dialog v-model="orderDialogVisible" title="确认下单" width="420px">
    <div style="color: #606266; line-height: 2">
      <div><b>商品：</b>{{ orderProduct.name }}</div>
      <div><b>单价：</b>¥{{ orderProduct.price }}</div>
      <div><b>库存：</b>{{ orderProduct.stock }}</div>
    </div>
    <div style="margin: 16px 0 0; display: flex; align-items: center">
      <span style="margin-right: 12px"><b>数量：</b></span>
      <el-input-number v-model="orderQuantity" :min="1" :max="orderProduct.stock" size="small" />
    </div>
    <div style="text-align: right; border-top: 1px solid #eee; padding-top: 16px; margin-top: 16px">
      <span style="color: #909399">合计：</span>
      <span style="color: #f56c6c; font-size: 22px; font-weight: bold">¥{{ (orderProduct.price * orderQuantity).toFixed(2) }}</span>
    </div>
    <template #footer>
      <el-button @click="orderDialogVisible = false">取消</el-button>
      <el-button type="primary" @click="submitOrder">确认下单</el-button>
    </template>
  </el-dialog>
</template>
