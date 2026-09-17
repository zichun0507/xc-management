<template>
  <el-dialog v-model="visible" :title="isEdit ? '编辑人员' : '新增人员'" width="560px" @open="onOpen">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="姓名" prop="name">
            <el-input v-model="form.name" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="性别" prop="gender">
            <el-select v-model="form.gender" placeholder="请选择" style="width:100%">
              <el-option label="男" value="男" />
              <el-option label="女" value="女" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="身份证号" prop="idCard">
            <el-input v-model="form.idCard" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="form.phone" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="毕业学校" prop="school">
            <el-input v-model="form.school" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="专业" prop="major">
            <el-input v-model="form.major" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="岗位" prop="position">
        <el-input v-model="form.position" />
      </el-form-item>
      <el-form-item label="照片">
        <el-upload
          :show-file-list="false"
          :before-upload="handlePhotoUpload"
          accept=".jpg,.jpeg,.png"
        >
          <template #trigger>
            <el-button size="small">选择照片</el-button>
          </template>
        </el-upload>
        <span v-if="form.photo" class="photo-hint">已上传</span>
        <el-button v-if="form.photo" size="small" type="danger" link @click="form.photo = ''">清除</el-button>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { createEmployee, updateEmployee, getEmployee, uploadEmployeePhoto } from '@/api/employee'

const props = defineProps<{ modelValue: boolean; companyId: number | null; employeeId?: number }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void; (e: 'saved'): void }>()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})
const isEdit = computed(() => !!props.employeeId)
const formRef = ref<FormInstance>()
const saving = ref(false)

const form = reactive({
  name: '', gender: '', idCard: '', phone: '',
  school: '', major: '', position: '', photo: '',
})

const rules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
}

async function onOpen() {
  if (isEdit.value && props.employeeId) {
    const res = await getEmployee(props.employeeId)
    const e = res.data
    Object.assign(form, {
      name: e.name, gender: e.gender || '', idCard: e.idCard || '',
      phone: e.phone || '', school: e.school || '', major: e.major || '',
      position: e.position || '', photo: e.photo || '',
    })
  } else {
    Object.assign(form, {
      name: '', gender: '', idCard: '', phone: '',
      school: '', major: '', position: '', photo: '',
    })
  }
}

async function handlePhotoUpload(file: File): Promise<boolean> {
  const ext = file.name.split('.').pop()?.toLowerCase()
  if (!ext || !['jpg', 'jpeg', 'png'].includes(ext)) {
    ElMessage.error('照片仅支持 jpg/png 格式')
    return false
  }
  if (file.size > 10 * 1024 * 1024) {
    ElMessage.error('照片大小不能超过 10MB')
    return false
  }
  if (isEdit.value && props.employeeId) {
    try {
      const res = await uploadEmployeePhoto(props.employeeId, file)
      form.photo = res.data
      ElMessage.success('照片上传成功')
    } catch { /* handled */ }
  } else {
    ElMessage.warning('请先保存人员信息，再上传照片')
  }
  return false
}

async function save() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = { ...form }
    if (isEdit.value && props.employeeId) {
      await updateEmployee(props.employeeId, payload)
      ElMessage.success('修改成功')
    } else {
      await createEmployee({ ...payload, companyId: props.companyId })
      ElMessage.success('新增成功')
    }
    visible.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.photo-hint { margin: 0 8px; color: var(--el-color-success); font-size: 13px; }
</style>