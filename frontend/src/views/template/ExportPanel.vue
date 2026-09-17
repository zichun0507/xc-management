<template>
  <el-card shadow="never">
    <template #header>
      <div class="section-header">
        <span>资料导出</span>
      </div>
    </template>

    <el-form :inline="true" size="small" class="export-form">
      <el-form-item label="选择企业">
        <el-select v-model="selectedCompanyId" placeholder="选择入驻企业" filterable style="width:280px" @change="onCompanyChange">
          <el-option v-for="c in companies" :key="c.id" :label="c.companyName" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="exporting" :disabled="!selectedCompanyId" @click="exportAll">
          导出全套代办资料
        </el-button>
      </el-form-item>
      <el-form-item>
        <el-button :loading="appointmentExporting" :disabled="!selectedCompanyId" @click="exportAppointment">
          导出任命书
        </el-button>
      </el-form-item>
    </el-form>

    <el-alert v-if="exporting || appointmentExporting" type="info" show-icon :closable="false" style="margin-top:8px">
      <template #title>
        <span>正在生成文档，请耐心等待...</span>
      </template>
    </el-alert>
  </el-card>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getCompanies } from '@/api/company'
import { getEmployees } from '@/api/employee'
import { exportCompanyDocs, exportAppointment } from '@/api/template'

const companies = ref<any[]>([])
const selectedCompanyId = ref<number | null>(null)
const exporting = ref(false)
const appointmentExporting = ref(false)

onMounted(async () => {
  try {
    const res = await getCompanies({ page: 1, size: 9999 })
    companies.value = res.data.records || []
  } catch { /* ignore */ }
})

function onCompanyChange() { /* noop */ }

function downloadBlob(data: any, filename: string) {
  const blob = new Blob([data], { type: 'application/zip' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = filename
  a.click()
  URL.revokeObjectURL(url)
}

async function exportAll() {
  if (!selectedCompanyId.value) return
  exporting.value = true
  try {
    const companyName = companies.value.find((c: any) => c.id === selectedCompanyId.value)?.companyName || 'export'
    const res = await exportCompanyDocs(selectedCompanyId.value)
    downloadBlob(res.data, `${companyName}_代办资料.zip`)
    ElMessage.success('导出成功')
  } catch (e: any) {
    if (e?.response?.data?.message?.includes('超时')) {
      ElMessage.error('导出超时，请减少导出数量后重试')
    } else {
      ElMessage.error('导出失败')
    }
  } finally {
    exporting.value = false
  }
}

async function exportAppointment() {
  if (!selectedCompanyId.value) return
  appointmentExporting.value = true
  try {
    const empRes = await getEmployees({ companyId: selectedCompanyId.value, status: 'ACTIVE', page: 1, size: 9999 })
    const employees = empRes.data.records || []
    if (employees.length === 0) {
      ElMessage.warning('该企业下无在职人员')
      return
    }
    const companyName = companies.value.find((c: any) => c.id === selectedCompanyId.value)?.companyName || 'export'
    const empIds = employees.map((e: any) => e.id)
    const res = await exportAppointment(selectedCompanyId.value, empIds)
    downloadBlob(res.data, `${companyName}_任命书.zip`)
    ElMessage.success('导出成功')
  } catch (e: any) {
    if (e?.response?.data?.message?.includes('超时')) {
      ElMessage.error('导出超时，请减少导出数量后重试')
    } else {
      ElMessage.error('导出失败')
    }
  } finally {
    appointmentExporting.value = false
  }
}
</script>

<style scoped>
.section-header { display: flex; justify-content: space-between; align-items: center; }
.export-form { margin-bottom: 0; }
</style>