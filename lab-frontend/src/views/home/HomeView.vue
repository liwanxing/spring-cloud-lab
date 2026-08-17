<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { getUserList, createUser, updateUser, deleteUser } from '@/api/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const tableData = ref<any[]>([])
const loading = ref(false)
const total = ref(0)
const query = reactive({ page: 1, size: 10, username: '', status: undefined as number | undefined })

const dialogVisible = ref(false)
const dialogTitle = ref('新增用户')
const isEdit = ref(false)
const formRef = ref()
const form = reactive({ id: 0, username: '', password: '', email: '', phone: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  email: [{ type: 'email' as const, message: '邮箱格式不正确', trigger: 'blur' }],
}

async function loadData() {
  loading.value = true
  try {
    const res: any = await getUserList(query)
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (err: any) {
    ElMessage.error(err.message || '查询失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() { query.page = 1; loadData() }
function handleReset() { query.username = ''; query.status = undefined; query.page = 1; loadData() }
function handleSizeChange() { query.page = 1; loadData() }

function handleCreate() {
  isEdit.value = false
  dialogTitle.value = '新增用户'
  Object.assign(form, { id: 0, username: '', password: '', email: '', phone: '' })
  dialogVisible.value = true
}

function handleEdit(row: any) {
  isEdit.value = true
  dialogTitle.value = '编辑用户'
  Object.assign(form, { id: row.id, username: row.username, password: '', email: row.email || '', phone: row.phone || '' })
  dialogVisible.value = true
}

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  try {
    if (isEdit.value) {
      const data: any = {}
      if (form.password) data.password = form.password
      if (form.email) data.email = form.email
      if (form.phone) data.phone = form.phone
      await updateUser(form.id, data)
      ElMessage.success('更新成功')
    } else {
      await createUser(form)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (err: any) {
    ElMessage.error(err.message || '操作失败')
  }
}

async function handleDelete(row: any) {
  try {
    await ElMessageBox.confirm(`确认删除用户 ${row.username}？`, '提示', { type: 'warning' })
    await deleteUser(row.id)
    ElMessage.success('删除成功')
    loadData()
  } catch {}
}

async function handleStatusChange(row: any) {
  try {
    await updateUser(row.id, { status: row.status })
    ElMessage.success('状态已更新')
  } catch (err: any) {
    row.status = row.status === 1 ? 0 : 1
    ElMessage.error(err.message || '更新失败')
  }
}

onMounted(() => loadData())
</script>

<template>
  <el-card>
    <el-form :inline="true" :model="query" style="margin-bottom: 16px">
      <el-form-item label="用户名">
        <el-input v-model="query.username" placeholder="搜索用户名" clearable @keyup.enter="handleSearch" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
          <el-option label="正常" :value="1" />
          <el-option label="禁用" :value="0" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="handleReset">重置</el-button>
      </el-form-item>
    </el-form>

    <el-divider />

    <div style="display: flex; justify-content: flex-end; margin-bottom: 12px">
      <el-button type="primary" @click="handleCreate">新增用户</el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="用户名" />
      <el-table-column prop="email" label="邮箱" />
      <el-table-column prop="phone" label="手机号" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-switch v-model="row.status" :active-value="1" :inactive-value="0" @change="handleStatusChange(row)" />
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="创建时间" width="170" />
      <el-table-column prop="updatedAt" label="更新时间" width="170" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button size="small" @click="handleEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination
      v-model:current-page="query.page"
      v-model:page-size="query.size"
      :page-sizes="[10, 20, 50]"
      :total="total"
      layout="total, sizes, prev, pager, next"
      @size-change="handleSizeChange"
      @current-change="loadData"
      style="margin-top: 16px; justify-content: flex-end"
    />

    <el-empty v-if="!loading && tableData.length === 0" description="暂无数据" />
  </el-card>

  <el-dialog v-model="dialogVisible" :title="dialogTitle" width="420px">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
      <el-form-item label="用户名" prop="username">
        <el-input v-model="form.username" :disabled="isEdit" placeholder="请输入用户名" />
      </el-form-item>
      <el-form-item :label="isEdit ? '新密码' : '密码'" prop="password">
        <el-input v-model="form.password" type="password" :placeholder="isEdit ? '留空则不修改' : '请输入密码'" show-password />
      </el-form-item>
      <el-form-item label="邮箱" prop="email">
        <el-input v-model="form.email" placeholder="请输入邮箱" />
      </el-form-item>
      <el-form-item label="手机号" prop="phone">
        <el-input v-model="form.phone" placeholder="请输入手机号" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="dialogVisible = false">取消</el-button>
      <el-button type="primary" @click="handleSubmit">确定</el-button>
    </template>
  </el-dialog>
</template>
