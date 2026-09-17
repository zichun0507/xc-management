<template>
  <el-dialog v-model="visible" :title="isEdit ? '编辑企业' : '新增企业'" width="640px" @open="onOpen">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="企业全称" prop="companyName">
            <el-input v-model="form.companyName" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="简称" prop="shortName">
            <el-input v-model="form.shortName" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="英文缩写" prop="englishName">
            <el-input v-model="form.englishName" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="信用代码" prop="unifiedCode">
            <el-input v-model="form.unifiedCode" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="法定代表人" prop="legalPerson">
            <el-input v-model="form.legalPerson" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="联系人" prop="contactPerson">
            <el-input v-model="form.contactPerson" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="联系电话" prop="contactPhone">
            <el-input v-model="form.contactPhone" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="经营地址" prop="address">
            <el-input v-model="form.address" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="20">
        <el-col :span="12">
          <el-form-item label="入驻开始" prop="leaseStartDate">
            <el-date-picker v-model="form.leaseStartDate" type="date" placeholder="选择日期" style="width:100%" value-format="YYYY-MM-DD" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="入驻到期" prop="leaseEndDate">
            <el-date-picker v-model="form.leaseEndDate" type="date" placeholder="选择日期" style="width:100%" value-format="YYYY-MM-DD" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="备注" prop="remark">
        <el-input v-model="form.remark" type="textarea" :rows="2" />
      </el-form-item>
      <el-form-item label="分配房间">
        <el-select v-model="form.roomIds" multiple filterable placeholder="选择房间（可多选）" style="width:100%">
          <el-option-group v-for="g in roomGroups" :key="g.label" :label="g.label">
            <el-option v-for="r in g.options" :key="r.value" :label="r.label" :value="r.value" :disabled="r.disabled" />
          </el-option-group>
        </el-select>
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
import { getRooms, type Room } from '@/api/room'
import { createCompany, updateCompany, getCompanyDetail } from '@/api/company'

const props = defineProps<{ modelValue: boolean; companyId?: number }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void; (e: 'saved'): void }>()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})
const isEdit = computed(() => !!props.companyId)

const formRef = ref<FormInstance>()
const saving = ref(false)
const allRooms = ref<Room[]>([])
const form = reactive({
  companyName: '', shortName: '', englishName: '', unifiedCode: '',
  legalPerson: '', contactPerson: '', contactPhone: '', address: '',
  leaseStartDate: '', leaseEndDate: '', remark: '',
  roomIds: [] as number[],
})

const rules = {
  companyName: [{ required: true, message: '请输入企业名称', trigger: 'blur' }],
}

const roomGroups = computed(() => {
  const map = new Map<string, { value: number; label: string; disabled: boolean }[]>()
  for (const r of allRooms.value) {
    const key = `楼栋 ${r.buildingId}`
    if (!map.has(key)) map.set(key, [])
    map.get(key)!.push({
      value: r.id,
      label: `${r.roomNumber} (${statusLabel(r.status)})`,
      disabled: r.status === 'OCCUPIED' && !form.roomIds.includes(r.id),
    })
  }
  return Array.from(map.entries()).map(([label, options]) => ({ label, options }))
})

function statusLabel(s: string) {
  return { FREE: '空闲', OCCUPIED: '已分配', DISABLED: '禁用' }[s] || s
}

async function onOpen() {
  const res = await getRooms({ page: 1, size: 9999 })
  allRooms.value = res.data.records || []
  if (isEdit.value && props.companyId) {
    const detail = await getCompanyDetail(props.companyId!)
    const c = detail.data.company
    Object.assign(form, {
      companyName: c.companyName, shortName: c.shortName || '', englishName: c.englishName || '',
      unifiedCode: c.unifiedCode || '',
      legalPerson: c.legalPerson || '', contactPerson: c.contactPerson || '',
      contactPhone: c.contactPhone || '', address: c.address || '',
      leaseStartDate: c.leaseStartDate || '', leaseEndDate: c.leaseEndDate || '',
      remark: c.remark || '', roomIds: detail.data.roomIds || [],
    })
  } else {
    Object.assign(form, {
      companyName: '', shortName: '', englishName: '', unifiedCode: '',
      legalPerson: '', contactPerson: '', contactPhone: '', address: '',
      leaseStartDate: '', leaseEndDate: '', remark: '', roomIds: [],
    })
  }
}

async function save() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = { ...form }
    if (isEdit.value && props.companyId) {
      await updateCompany(props.companyId, payload)
      ElMessage.success('修改成功')
    } else {
      await createCompany(payload)
      ElMessage.success('新增成功')
    }
    visible.value = false
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>