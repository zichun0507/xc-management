<template>
  <div class="home-page">
    <h3>欢迎使用新长入驻公司管理平台</h3>

    <el-row :gutter="20" class="stats-row">
      <el-col :xs="12" :sm="6" v-for="card in primaryCards" :key="card.title">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-title">{{ card.title }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="stats-row" style="margin-top:20px">
      <el-col :xs="24" :sm="8" v-for="group in detailGroups" :key="group.title">
        <el-card shadow="hover">
          <template #header>{{ group.title }}</template>
          <div v-for="item in group.items" :key="item.label" class="detail-item">
            <span class="detail-label">{{ item.label }}</span>
            <span class="detail-value">{{ item.value }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card shadow="never" class="import-export-card">
      <template #header>
        <span>企业数据导入 / 导出</span>
      </template>
      <el-space wrap>
        <el-button size="small" @click="handleDownloadTemplate">下载导入模板</el-button>
        <el-upload
          v-permission="'ADMIN'"
          :show-file-list="false"
          :before-upload="handleImport"
          accept=".xlsx,.xls"
        >
          <el-button size="small" type="success">批量导入</el-button>
        </el-upload>
        <el-button size="small" @click="handleExport">批量导出全部企业</el-button>
      </el-space>
    </el-card>

    <el-dialog v-model="importResultVisible" title="导入结果" width="500px">
      <el-alert
        :title="`导入完成：成功 ${importResult.successCount} / 共 ${importResult.totalCount} 条`"
        :type="importResult.errors.length === 0 ? 'success' : 'warning'"
        show-icon
        style="margin-bottom:16px"
      />
      <el-table v-if="importResult.errors.length > 0" :data="importResult.errors" stripe border size="small" max-height="300">
        <el-table-column prop="row" label="行号" width="80" />
        <el-table-column prop="reason" label="错误原因" />
      </el-table>
      <template #footer>
        <el-button type="primary" @click="importResultVisible = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getDashboardStats, type DashboardStats } from '@/api/dashboard'
import { exportCompanies, importCompanies, downloadTemplate, type ImportResult } from '@/api/company'

const stats = ref<DashboardStats>({
  buildingCount: 0, roomTotal: 0, roomFree: 0, roomOccupied: 0, roomDisabled: 0,
  companyTotal: 0, companyNormal: 0,
  employeeTotal: 0, employeeActive: 0,
})

const importResultVisible = ref(false)
const importResult = reactive<ImportResult>({ successCount: 0, totalCount: 0, errors: [] })

const primaryCards = computed(() => [
  { title: '楼栋总数', value: stats.value.buildingCount },
  { title: '房间总数', value: stats.value.roomTotal },
  { title: '入驻企业', value: stats.value.companyTotal },
  { title: '企业人员', value: stats.value.employeeTotal },
])

const detailGroups = computed(() => [
  {
    title: '房间状态',
    items: [
      { label: '空闲', value: stats.value.roomFree },
      { label: '已分配', value: stats.value.roomOccupied },
      { label: '禁用', value: stats.value.roomDisabled },
    ],
  },
  {
    title: '企业状态',
    items: [
      { label: '正常运营', value: stats.value.companyNormal },
      { label: '迁出/停办', value: stats.value.companyTotal - stats.value.companyNormal },
    ],
  },
  {
    title: '人员状态',
    items: [
      { label: '在职', value: stats.value.employeeActive },
      { label: '离职', value: stats.value.employeeTotal - stats.value.employeeActive },
    ],
  },
])

onMounted(async () => {
  try {
    const res = await getDashboardStats()
    stats.value = res.data
  } catch { /* ignore */ }
})

async function handleImport(file: File): Promise<boolean> {
  try {
    const res = await importCompanies(file)
    Object.assign(importResult, res.data)
    importResultVisible.value = true
  } catch { /* handled */ }
  return false
}

async function handleExport() {
  try {
    const res = await exportCompanies({})
    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = 'companies.xlsx'
    a.click()
    URL.revokeObjectURL(url)
  } catch { /* handled */ }
}

async function handleDownloadTemplate() {
  try {
    const res = await downloadTemplate()
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
.home-page { padding: 20px; }
.home-page h3 { margin: 0 0 24px; font-size: 20px; color: var(--el-text-color-primary); }
.stats-row { margin-top: 16px; }
.stat-card { text-align: center; }
.stat-value { font-size: 32px; font-weight: 600; color: var(--el-color-primary); }
.stat-title { margin-top: 8px; font-size: 14px; color: var(--el-text-color-secondary); }
.detail-item { display: flex; justify-content: space-between; padding: 8px 0; border-bottom: 1px solid var(--el-border-color-light); }
.detail-item:last-child { border-bottom: none; }
.detail-label { font-size: 14px; color: var(--el-text-color-regular); }
.detail-value { font-size: 16px; font-weight: 600; color: var(--el-color-primary); }
.import-export-card { margin-top: 20px; }
</style>