<template>
  <div class="employee-page">
    <el-card shadow="never">
      <template #header>
        <div class="section-header">
          <span>企业人员管理</span>
          <div class="header-actions">
            <el-select v-if="selectedCompanyId" v-model="rosterFormat" size="small" style="width:120px;margin-right:8px">
              <el-option label="Excel 导出" value="excel" />
              <el-option label="Word 导出" value="word" />
            </el-select>
            <el-button size="small" :disabled="!selectedCompanyId" @click="exportRoster">导出花名册</el-button>
            <el-button v-permission="'ADMIN'" type="primary" size="small" :disabled="!selectedCompanyId" @click="openForm()">新增人员</el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" size="small" class="filter-bar">
        <el-form-item label="所属企业">
          <el-select v-model="selectedCompanyId" placeholder="选择企业" filterable clearable style="width:240px" @change="onCompanyChange">
            <el-option v-for="c in companies" :key="c.id" :label="c.companyName" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterStatus" placeholder="全部" clearable @change="search" style="width:120px">
            <el-option label="在职" value="ACTIVE" />
            <el-option label="离职" value="INACTIVE" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="pageData.records" v-loading="loading" stripe border size="small">
        <el-table-column prop="name" label="姓名" width="100" />
        <el-table-column prop="gender" label="性别" width="60" />
        <el-table-column prop="idCard" label="身份证号" width="170" />
        <el-table-column prop="school" label="毕业学校" min-width="140" />
        <el-table-column prop="major" label="专业" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="position" label="岗位" width="100" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 'ACTIVE' ? 'success' : 'info'" size="small">
              {{ row.status === 'ACTIVE' ? '在职' : '离职' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openForm(row.id)">编辑</el-button>
            <el-button size="small" type="warning" link @click="handleStatusToggle(row)">
              {{ row.status === 'ACTIVE' ? '离职' : '复职' }}
            </el-button>
            <el-button size="small" type="primary" link @click="showAttachments(row)">附件</el-button>
            <el-popconfirm title="确认删除该人员？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button v-permission="'ADMIN'" size="small" type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="pageData.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @change="fetchData"
        />
      </div>
    </el-card>

    <EmployeeForm v-model="formVisible" :company-id="selectedCompanyId" :employee-id="editingEmployeeId" @saved="fetchData" />

    <el-dialog v-model="attachmentVisible" title="附件管理" width="600px" :destroy-on-close="true">
      <AttachmentPanel :employee-id="currentEmployeeId" />
    </el-dialog>

    <el-dialog v-model="statusDialogVisible" title="状态变更" width="420px">
      <p style="margin-bottom:16px">
        确认将「{{ statusTargetName }}」{{ statusTargetNew === 'ACTIVE' ? '复职' : '离职' }}？
      </p>
      <template #footer>
        <el-button @click="statusDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingStatus" @click="confirmStatusChange">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getEmployees, changeEmployeeStatus, deleteEmployee,
  exportRosterExcel, exportRosterWord,
  type Employee, type EmployeePage,
} from '@/api/employee'
import { getCompanies } from '@/api/company'
import EmployeeForm from './EmployeeForm.vue'
import AttachmentPanel from './AttachmentPanel.vue'

const loading = ref(false)
const savingStatus = ref(false)
const companies = ref<any[]>([])
const selectedCompanyId = ref<number | null>(null)
const filterStatus = ref('')
const rosterFormat = ref('excel')

const pageData = reactive<EmployeePage>({ records: [], total: 0, current: 1, size: 20, pages: 0 })
const page = ref(1)
const size = ref(20)

const formVisible = ref(false)
const editingEmployeeId = ref<number | undefined>()

const attachmentVisible = ref(false)
const currentEmployeeId = ref<number | null>(null)

const statusDialogVisible = ref(false)
const statusTargetName = ref('')
const statusTargetNew = ref('')
const statusTargetId = ref<number>(0)

onMounted(async () => {
  try {
    const res = await getCompanies({ page: 1, size: 9999 })
    companies.value = res.data.records || []
  } catch { /* ignore */ }
})

function search() {
  page.value = 1
  fetchData()
}

function onCompanyChange() {
  filterStatus.value = ''
  page.value = 1
  fetchData()
}

async function fetchData() {
  if (!selectedCompanyId.value) {
    Object.assign(pageData, { records: [], total: 0 })
    return
  }
  loading.value = true
  try {
    const res = await getEmployees({
      companyId: selectedCompanyId.value,
      status: filterStatus.value || undefined,
      page: page.value,
      size: size.value,
    })
    Object.assign(pageData, res.data)
  } finally {
    loading.value = false
  }
}

function openForm(id?: number) {
  editingEmployeeId.value = id
  formVisible.value = true
}

function showAttachments(row: Employee) {
  currentEmployeeId.value = row.id
  attachmentVisible.value = true
}

function handleStatusToggle(row: Employee) {
  statusTargetId.value = row.id
  statusTargetName.value = row.name
  statusTargetNew.value = row.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'
  statusDialogVisible.value = true
}

async function confirmStatusChange() {
  savingStatus.value = true
  try {
    await changeEmployeeStatus(statusTargetId.value, { status: statusTargetNew.value })
    ElMessage.success('状态变更成功')
    statusDialogVisible.value = false
    await fetchData()
  } finally {
    savingStatus.value = false
  }
}

async function handleDelete(id: number) {
  try {
    await deleteEmployee(id)
    ElMessage.success('删除成功')
    await fetchData()
  } catch { /* handled */ }
}

async function exportRoster() {
  if (!selectedCompanyId.value) return
  try {
    const fn = rosterFormat.value === 'word' ? exportRosterWord : exportRosterExcel
    const ext = rosterFormat.value === 'word' ? 'docx' : 'xlsx'
    const res = await fn(selectedCompanyId.value)
    const blob = new Blob([res.data], {
      type: rosterFormat.value === 'word'
        ? 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
        : 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    const companyName = companies.value.find((c: any) => c.id === selectedCompanyId.value)?.companyName || 'roster'
    a.download = `${companyName}_花名册.${ext}`
    a.click()
    URL.revokeObjectURL(url)
  } catch { /* handled */ }
}
</script>

<style scoped>
.employee-page { display: flex; flex-direction: column; gap: 16px; }
.section-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; }
.header-actions { display: flex; align-items: center; }
.filter-bar { margin-bottom: 0; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>