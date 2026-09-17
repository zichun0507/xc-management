<template>
  <div class="company-page">
    <el-card shadow="never">
      <template #header>
        <div class="section-header">
          <span>入驻企业管理</span>
          <div class="header-actions">
            <el-button v-permission="'ADMIN'" type="primary" size="small" @click="openForm()">新增企业</el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" size="small" class="filter-bar">
        <el-form-item label="企业名称">
          <el-input v-model="filterName" placeholder="模糊搜索" clearable @change="onFilterChange" />
        </el-form-item>
        <el-form-item label="房间号">
          <el-input v-model="filterRoom" placeholder="搜索" clearable @change="onFilterChange" />
        </el-form-item>
        <el-form-item label="楼栋">
          <el-select v-model="filterBuilding" placeholder="全部" clearable @change="onFilterChange">
            <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="楼层">
          <el-select v-model="filterFloor" placeholder="全部" clearable @change="onFilterChange">
            <el-option v-for="f in floors" :key="f.id" :label="f.name" :value="f.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="pageData.records" v-loading="loading" stripe border size="small">
        <el-table-column prop="companyName" label="企业全称" min-width="140" />
        <el-table-column prop="shortName" label="简称" width="100" />
        <el-table-column prop="englishName" label="英文缩写" width="100" />
        <el-table-column prop="unifiedCode" label="信用代码" width="140" />
        <el-table-column prop="legalPerson" label="法定代表人" width="100" />
        <el-table-column prop="contactPerson" label="联系人" width="100" />
        <el-table-column prop="contactPhone" label="联系电话" width="130" />
        <el-table-column label="入驻期限" width="200">
          <template #default="{ row }">
            {{ row.leaseStartDate || '-' }} ~ {{ row.leaseEndDate || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="businessStatus" label="业务状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.businessStatus)" size="small">
              {{ statusLabel(row.businessStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" link @click="openForm(row.id)">编辑</el-button>
            <el-dropdown v-permission="'ADMIN'" trigger="click" @command="(cmd: string) => handleStatusChange(row, cmd)">
              <el-button size="small" type="warning" link>
                状态变更<el-icon><ArrowDown /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="NORMAL" :disabled="row.businessStatus === 'NORMAL'">恢复运营</el-dropdown-item>
                  <el-dropdown-item command="MOVED_OUT" :disabled="row.businessStatus === 'MOVED_OUT'">迁出</el-dropdown-item>
                  <el-dropdown-item command="SUSPENDED" :disabled="row.businessStatus === 'SUSPENDED'">停办</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
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

    <CompanyForm v-model="formVisible" :company-id="editingCompanyId" @saved="fetchData" />

    <el-dialog v-model="statusDialogVisible" title="状态变更" width="420px">
      <el-form ref="statusFormRef" :model="statusForm" :rules="statusRules" label-width="80px">
        <el-form-item label="目标状态">
          <el-tag :type="statusType(nextStatus)" size="large">{{ statusLabel(nextStatus) }}</el-tag>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="statusForm.remark" type="textarea" :rows="3" placeholder="请填写变更原因（必填）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="savingStatus" @click="confirmStatusChange">确认变更</el-button>
      </template>
    </el-dialog>

    </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { ArrowDown } from '@element-plus/icons-vue'
import CompanyForm from './CompanyForm.vue'
import {
  getCompanies, changeCompanyStatus, type Company, type CompanyPage,
} from '@/api/company'
import { getBuildings, getFloors } from '@/api/room'

const loading = ref(false)
const savingStatus = ref(false)
const buildings = ref<any[]>([])
const floors = ref<any[]>([])

const pageData = reactive<CompanyPage>({ records: [], total: 0, current: 1, size: 20, pages: 0 })
const page = ref(1)
const size = ref(20)

const filterName = ref('')
const filterRoom = ref('')
const filterBuilding = ref<number | undefined>()
const filterFloor = ref<number | undefined>()

const formVisible = ref(false)
const editingCompanyId = ref<number | undefined>()
const statusDialogVisible = ref(false)
const statusFormRef = ref<FormInstance>()
const selectedCompany = ref<Company | null>(null)
const nextStatus = ref('')
const statusForm = reactive({ remark: '' })
const statusRules = { remark: [{ required: true, message: '请填写变更原因', trigger: 'blur' }] }

// initial
onMounted(async () => {
  const [bRes] = await Promise.all([getBuildings()])
  buildings.value = bRes.data
  fetchData()
})

watch(filterBuilding, async (val) => {
  filterFloor.value = undefined
  if (val) {
    const res = await getFloors(val)
    floors.value = res.data
  } else {
    floors.value = []
  }
})

function statusType(s: string) {
  return { NORMAL: 'success', MOVED_OUT: 'info', SUSPENDED: 'warning' }[s] || 'info'
}
function statusLabel(s: string) {
  return { NORMAL: '正常运营', MOVED_OUT: '迁出', SUSPENDED: '停办' }[s] || s
}
function onFilterChange() { page.value = 1 }
function search() { page.value = 1; fetchData() }
function resetFilter() {
  filterName.value = ''; filterRoom.value = ''; filterBuilding.value = undefined; filterFloor.value = undefined
  page.value = 1; fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getCompanies({
      companyName: filterName.value || undefined,
      roomNumber: filterRoom.value || undefined,
      buildingId: filterBuilding.value,
      floorId: filterFloor.value,
      page: page.value,
      size: size.value,
    })
    Object.assign(pageData, res.data)
  } finally {
    loading.value = false
  }
}

function openForm(id?: number) {
  editingCompanyId.value = id
  formVisible.value = true
}

function handleStatusChange(row: Company, cmd: string) {
  if (cmd === 'MOVED_OUT' || cmd === 'SUSPENDED') {
    selectedCompany.value = row
    nextStatus.value = cmd
    statusForm.remark = ''
    statusDialogVisible.value = true
  } else {
    ElMessageBox.confirm(`确认将「${row.companyName}」恢复为正常运营？`, '提示', { type: 'warning' })
      .then(async () => {
        await changeCompanyStatus(row.id, { businessStatus: 'NORMAL', remark: '恢复运营' })
        ElMessage.success('状态已变更')
        await fetchData()
      })
      .catch(() => {})
  }
}

async function confirmStatusChange() {
  const valid = await statusFormRef.value?.validate().catch(() => false)
  if (!valid || !selectedCompany.value) return
  savingStatus.value = true
  try {
    await changeCompanyStatus(selectedCompany.value.id, {
      businessStatus: nextStatus.value,
      remark: statusForm.remark,
    })
    ElMessage.success('状态变更成功')
    statusDialogVisible.value = false
    await fetchData()
  } finally {
    savingStatus.value = false
  }
}

async function downloadTemplate() {
  try {
    const { default: dt } = await import('@/api/company')
    const res = await dt.downloadTemplate()
    const blob = new Blob([res.data])
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'import-template.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  } catch { /* handled */ }
}
</script>

<style scoped>
.company-page { display: flex; flex-direction: column; gap: 16px; }
.section-header { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; }
.header-actions { display: flex; gap: 8px; align-items: center; }
.filter-bar { margin-bottom: 0; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>