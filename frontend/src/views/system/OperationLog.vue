<template>
  <div class="system-page">
    <el-card shadow="never">
      <template #header>
        <div class="section-header">
          <span>操作日志</span>
          <el-tag type="info" size="small">日志保留 6 个月</el-tag>
        </div>
      </template>

      <el-form :inline="true" size="small" class="filter-bar">
        <el-form-item label="操作人">
          <el-input v-model="filterOperator" placeholder="模糊搜索" clearable @change="onFilterChange" />
        </el-form-item>
        <el-form-item label="模块">
          <el-select v-model="filterModule" placeholder="全部" clearable @change="onFilterChange" style="width:130px">
            <el-option label="房间管理" value="ROOM" />
            <el-option label="企业管理" value="COMPANY" />
            <el-option label="人员管理" value="EMPLOYEE" />
            <el-option label="模板管理" value="TEMPLATE" />
            <el-option label="用户管理" value="USER" />
            <el-option label="导出" value="EXPORT" />
            <el-option label="权限" value="AUTH" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DDTHH:mm:ss"
            @change="onFilterChange"
            style="width:340px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="pageData.records" v-loading="loading" stripe border size="small">
        <el-table-column prop="operatorName" label="操作人" width="100" />
        <el-table-column prop="module" label="模块" width="100">
          <template #default="{ row }">{{ moduleLabel(row.module) }}</template>
        </el-table-column>
        <el-table-column prop="action" label="动作" width="120">
          <template #default="{ row }">{{ actionLabel(row.action) }}</template>
        </el-table-column>
        <el-table-column prop="content" label="操作内容" min-width="300" show-overflow-tooltip />
        <el-table-column prop="operationTime" label="操作时间" width="170" />
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
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { getLogs, type OperationLog } from '@/api/system'

const loading = ref(false)
const pageData = reactive<{ records: OperationLog[]; total: number }>({ records: [], total: 0 })
const page = ref(1)
const size = ref(20)

const filterOperator = ref('')
const filterModule = ref('')
const dateRange = ref<string[] | null>(null)

function moduleLabel(m: string) {
  const map: Record<string, string> = {
    ROOM: '房间', COMPANY: '企业', EMPLOYEE: '人员',
    TEMPLATE: '模板', USER: '用户', EXPORT: '导出', AUTH: '权限',
  }
  return map[m] || m
}

function actionLabel(a: string) {
  const map: Record<string, string> = {
    CREATE: '新增', UPDATE: '修改', DELETE: '删除',
    STATUS_CHANGE: '状态变更', LOGIN: '登录', LOGOUT: '登出',
    IMPORT: '导入', UPLOAD: '上传',
  }
  return map[a] || a
}

function onFilterChange() { page.value = 1 }

function search() { page.value = 1; fetchData() }

function resetFilter() {
  filterOperator.value = ''; filterModule.value = ''; dateRange.value = null
  page.value = 1; fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getLogs({
      operatorName: filterOperator.value || undefined,
      module: filterModule.value || undefined,
      startTime: dateRange.value?.[0] || undefined,
      endTime: dateRange.value?.[1] || undefined,
      page: page.value,
      size: size.value,
    })
    Object.assign(pageData, res.data)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.system-page { display: flex; flex-direction: column; gap: 16px; }
.section-header { display: flex; justify-content: space-between; align-items: center; }
.filter-bar { margin-bottom: 0; }
.pagination-wrap { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>