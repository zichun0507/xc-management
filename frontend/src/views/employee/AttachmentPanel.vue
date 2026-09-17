<template>
  <div class="attachment-panel">
    <el-descriptions :column="1" border size="small">
      <el-descriptions-item v-for="item in attachmentTypes" :key="item.type" :label="item.label">
        <div v-if="findAttachment(item.type)" class="file-info">
          <el-link type="primary" :underline="false" @click="preview(findAttachment(item.type)!.id)">
            <el-icon><Document /></el-icon>
            {{ findAttachment(item.type)!.originalName }}
          </el-link>
          <span class="file-meta">({{ formatSize(findAttachment(item.type)!.fileSize) }})</span>
          <el-button size="small" type="danger" link @click="handleDelete(findAttachment(item.type)!.id)">删除</el-button>
        </div>
        <div v-else class="file-empty">未上传</div>
        <el-upload
          v-permission="'ADMIN'"
          :show-file-list="false"
          :before-upload="(file) => handleUpload(file, item.type)"
          accept=".jpg,.jpeg,.png,.pdf"
        >
          <el-button size="small" type="primary">{{ findAttachment(item.type) ? '替换' : '上传' }}</el-button>
        </el-upload>
      </el-descriptions-item>
    </el-descriptions>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Document } from '@element-plus/icons-vue'
import {
  getAttachments, uploadAttachment, deleteAttachment,
  type Attachment,
} from '@/api/employee'

const props = defineProps<{ employeeId: number | null }>()
const attachments = ref<Attachment[]>([])

const attachmentTypes = [
  { type: 'ID_CARD_FRONT', label: '身份证正面' },
  { type: 'ID_CARD_BACK', label: '身份证反面' },
  { type: 'GRADUATION_CERT', label: '毕业证照片' },
  { type: 'EDUCATION_REPORT', label: '学历认证报告' },
]

watch(() => props.employeeId, async (id) => {
  if (id) {
    const res = await getAttachments(id)
    attachments.value = res.data
  } else {
    attachments.value = []
  }
}, { immediate: true })

function findAttachment(type: string) {
  return attachments.value.find(a => a.attachmentType === type)
}

function formatSize(bytes: number) {
  if (bytes < 1024) return bytes + 'B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + 'KB'
  return (bytes / 1024 / 1024).toFixed(1) + 'MB'
}

async function handleUpload(file: File, type: string) {
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (!ext || !['jpg', 'jpeg', 'png', 'pdf'].includes(ext)) {
    ElMessage.error('仅支持 jpg/png/pdf 格式')
    return false
  }
  if (type === 'EDUCATION_REPORT' && ext !== 'pdf') {
    ElMessage.error('学历认证报告仅支持 PDF 格式')
    return false
  }
  if (type !== 'EDUCATION_REPORT' && !['jpg', 'jpeg', 'png'].includes(ext)) {
    ElMessage.error('证件照片仅支持 jpg/png 格式')
    return false
  }
  if (file.size > 50 * 1024 * 1024) {
    ElMessage.error('文件大小不能超过 50MB')
    return false
  }
  try {
    await uploadAttachment(props.employeeId!, type, file)
    ElMessage.success('上传成功')
    const res = await getAttachments(props.employeeId!)
    attachments.value = res.data
  } catch { /* handled */ }
  return false
}

function preview(id: number) {
  ElMessage.info('附件预览需在后端配置静态文件访问或者单独开发下载接口')
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确认删除该附件？仅删除元数据，磁盘文件将保留', '提示', { type: 'warning' })
    await deleteAttachment(id)
    ElMessage.success('删除成功')
    attachments.value = attachments.value.filter(a => a.id !== id)
  } catch { /* cancelled or handled */ }
}
</script>

<style scoped>
.attachment-panel { padding: 0; }
.file-info { display: flex; align-items: center; gap: 8px; }
.file-meta { color: var(--el-text-color-secondary); font-size: 12px; }
.file-empty { color: var(--el-text-color-placeholder); font-size: 13px; }
.el-descriptions__cell { padding: 8px 12px; }
</style>