<template>
  <div class="template-page">
    <el-card shadow="never">
      <template #header>
        <div class="section-header">
          <span>代办模板管理</span>
          <div class="header-actions">
            <el-upload
              v-permission="'ADMIN'"
              :show-file-list="false"
              :before-upload="handleUpload"
              accept=".docx"
            >
              <el-button type="primary" size="small">上传模板</el-button>
            </el-upload>
          </div>
        </div>
      </template>

      <el-table :data="templates" v-loading="loading" stripe border size="small">
        <el-table-column prop="templateName" label="模板名称" min-width="160" />
        <el-table-column prop="templateType" label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="row.templateType === 'APPOINTMENT' ? 'warning' : 'primary'" size="small">
              {{ row.templateType === 'APPOINTMENT' ? '任命书' : '代办资料' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="originalName" label="原始文件名" width="200" />
        <el-table-column label="文件大小" width="100">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column prop="uploadedTime" label="上传时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-popconfirm title="确认删除该模板？" @confirm="handleDelete(row.id)">
              <template #reference>
                <el-button v-permission="'ADMIN'" size="small" type="danger" link>删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && templates.length === 0" description="暂无模板" />
    </el-card>

    <ExportPanel />

    <el-dialog v-model="uploadDialogVisible" title="上传模板" width="460px">
      <el-form ref="uploadFormRef" :model="uploadForm" :rules="uploadRules" label-width="90px">
        <el-form-item label="模板名称" prop="templateName">
          <el-input v-model="uploadForm.templateName" placeholder="输入展示名称" />
        </el-form-item>
        <el-form-item label="模板类型" prop="templateType">
          <el-select v-model="uploadForm.templateType" style="width:100%">
            <el-option label="代办资料" value="DOCUMENT" />
            <el-option label="任命书" value="APPOINTMENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="已选文件">
          <span v-if="selectedFile">{{ selectedFile.name }}</span>
          <span v-else class="no-file">未选择文件</span>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="uploadDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="uploading" @click="confirmUpload">上传</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { getTemplates, uploadTemplate, deleteTemplate, type DocTemplate } from '@/api/template'
import ExportPanel from './ExportPanel.vue'

const loading = ref(false)
const uploading = ref(false)
const templates = ref<DocTemplate[]>([])
const selectedFile = ref<File | null>(null)
const uploadDialogVisible = ref(false)
const uploadFormRef = ref<FormInstance>()

const uploadForm = reactive({ templateName: '', templateType: 'DOCUMENT' })
const uploadRules = {
  templateName: [{ required: true, message: '请输入模板名称', trigger: 'blur' }],
  templateType: [{ required: true, message: '请选择模板类型', trigger: 'change' }],
}

onMounted(() => fetchTemplates())

async function fetchTemplates() {
  loading.value = true
  try {
    const res = await getTemplates()
    templates.value = res.data
  } finally {
    loading.value = false
  }
}

function handleUpload(file: File): boolean {
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (ext !== 'docx') {
    ElMessage.error('仅支持 .docx 格式的模板文件')
    return false
  }
  if (file.size > 50 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 50MB')
    return false
  }
  selectedFile.value = file
  uploadForm.templateName = file.name.replace(/\.docx$/, '')
  uploadForm.templateType = 'DOCUMENT'
  uploadDialogVisible.value = true
  return false
}

async function confirmUpload() {
  const valid = await uploadFormRef.value?.validate().catch(() => false)
  if (!valid || !selectedFile.value) return
  uploading.value = true
  try {
    await uploadTemplate(selectedFile.value, uploadForm.templateName, uploadForm.templateType)
    ElMessage.success('上传成功')
    uploadDialogVisible.value = false
    selectedFile.value = null
    await fetchTemplates()
  } finally {
    uploading.value = false
  }
}

async function handleDelete(id: number) {
  try {
    await deleteTemplate(id)
    ElMessage.success('删除成功')
    await fetchTemplates()
  } catch { /* handled */ }
}

function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / 1024 / 1024).toFixed(1) + 'MB'
}
</script>

<style scoped>
.template-page { display: flex; flex-direction: column; gap: 16px; }
.section-header { display: flex; justify-content: space-between; align-items: center; }
.header-actions { display: flex; gap: 8px; }
.no-file { color: var(--el-text-color-placeholder); }
</style>