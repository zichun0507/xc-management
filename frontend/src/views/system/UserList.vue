<template>
  <div class="system-page">
    <el-card shadow="never">
      <template #header>
        <div class="section-header">
          <span>用户管理</span>
          <el-button type="primary" size="small" @click="openDialog()">新增用户</el-button>
        </div>
      </template>

      <el-table :data="pageData.records" v-loading="loading" stripe border size="small">
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="role" label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 'ADMIN' ? 'danger' : 'primary'" size="small">
              {{ row.role === 'ADMIN' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ENABLED' ? 'success' : 'info'" size="small">
              {{ row.status === 'ENABLED' ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openRoleDialog(row)">分配角色</el-button>
            <el-button size="small" type="warning" link @click="openPasswordDialog(row)">重置密码</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="pageData.total"
          :page-sizes="[10, 20]"
          layout="total, sizes, prev, pager, next, jumper"
          @change="fetchData"
        />
      </div>
    </el-card>

    <el-dialog v-model="addDialogVisible" title="新增用户" width="480px">
      <el-form ref="addFormRef" :model="addForm" :rules="addRules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model="addForm.username" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="addForm.realName" />
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="addForm.password" type="password" show-password />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="addForm.role" style="width:100%">
            <el-option label="管理员" value="ADMIN" />
            <el-option label="普通用户" value="USER" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleCreate">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="roleDialogVisible" title="分配角色" width="420px">
      <p style="margin-bottom:12px">用户：<strong>{{ roleTargetName }}</strong></p>
      <el-select v-model="newRole" style="width:100%">
        <el-option label="管理员" value="ADMIN" />
        <el-option label="普通用户" value="USER" />
      </el-select>
      <template #footer>
        <el-button @click="roleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleUpdateRole">确认</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="passwordDialogVisible" title="重置密码" width="420px">
      <p style="margin-bottom:12px">用户：<strong>{{ passwordTargetName }}</strong></p>
      <el-form ref="passwordFormRef" :model="passwordForm" :rules="passwordRules">
        <el-form-item prop="password">
          <el-input v-model="passwordForm.password" type="password" show-password placeholder="输入新密码" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="passwordDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleResetPassword">确认重置</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { getUsers, createUser, updateUserRole, resetUserPassword, type SysUser } from '@/api/system'

const loading = ref(false)
const saving = ref(false)
const pageData = reactive<{ records: SysUser[]; total: number }>({ records: [], total: 0 })
const page = ref(1)
const size = ref(20)

const addDialogVisible = ref(false)
const addFormRef = ref<FormInstance>()
const addForm = reactive({ username: '', realName: '', password: '', role: 'USER' })
const addRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

const roleDialogVisible = ref(false)
const roleTargetId = ref(0)
const roleTargetName = ref('')
const newRole = ref('USER')

const passwordDialogVisible = ref(false)
const passwordTargetId = ref(0)
const passwordTargetName = ref('')
const passwordFormRef = ref<FormInstance>()
const passwordForm = reactive({ password: '' })
const passwordRules = { password: [{ required: true, message: '请输入新密码', trigger: 'blur' }] }

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const res = await getUsers({ page: page.value, size: size.value })
    Object.assign(pageData, res.data)
  } finally {
    loading.value = false
  }
}

function openDialog() {
  addForm.username = ''; addForm.realName = ''; addForm.password = ''; addForm.role = 'USER'
  addDialogVisible.value = true
}

async function handleCreate() {
  const valid = await addFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await createUser({ ...addForm })
    ElMessage.success('新增成功')
    addDialogVisible.value = false
    await fetchData()
  } finally {
    saving.value = false
  }
}

function openRoleDialog(row: SysUser) {
  roleTargetId.value = row.id
  roleTargetName.value = row.username
  newRole.value = row.role
  roleDialogVisible.value = true
}

async function handleUpdateRole() {
  saving.value = true
  try {
    await updateUserRole(roleTargetId.value, newRole.value)
    ElMessage.success('角色已更新')
    roleDialogVisible.value = false
    await fetchData()
  } finally {
    saving.value = false
  }
}

function openPasswordDialog(row: SysUser) {
  passwordTargetId.value = row.id
  passwordTargetName.value = row.username
  passwordForm.password = ''
  passwordDialogVisible.value = true
}

async function handleResetPassword() {
  const valid = await passwordFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await resetUserPassword(passwordTargetId.value, passwordForm.password)
    ElMessage.success('密码已重置')
    passwordDialogVisible.value = false
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.system-page { display: flex; flex-direction: column; gap: 16px; }
.section-header { display: flex; justify-content: space-between; align-items: center; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>