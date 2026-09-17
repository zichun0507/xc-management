<template>
  <div class="room-page">
    <el-card shadow="never" class="section-card">
      <template #header>
        <div class="section-header">
          <span>楼栋管理</span>
          <el-button v-permission="'ADMIN'" type="primary" size="small" @click="showBuildingDialog = true">
            新增楼栋
          </el-button>
        </div>
      </template>
      <div class="building-list">
        <el-tag
          v-for="b in buildings"
          :key="b.id"
          :type="selectedBuildingId === b.id ? 'primary' : 'info'"
          :effect="selectedBuildingId === b.id ? 'dark' : 'plain'"
          class="building-tag"
          closable
          @click="selectBuilding(b)"
          @close.prevent="handleDeleteBuilding(b)"
        >
          {{ b.name }}<span v-if="b.landlord" class="landlord-hint">（{{ b.landlord }}）</span>
        </el-tag>
        <el-empty v-if="buildings.length === 0" :image-size="60" description="暂无楼栋" />
      </div>
    </el-card>

    <el-card v-if="selectedBuildingId" shadow="never" class="section-card">
      <template #header>
        <div class="section-header">
          <span>楼层管理 — {{ selectedBuildingName }}</span>
          <el-button v-permission="'ADMIN'" type="primary" size="small" @click="showFloorDialog = true">
            新增楼层
          </el-button>
        </div>
      </template>
      <div class="floor-list">
        <el-tag
          v-for="f in floors"
          :key="f.id"
          :type="selectedFloorId === f.id ? 'primary' : 'success'"
          :effect="selectedFloorId === f.id ? 'dark' : 'plain'"
          class="floor-tag"
          closable
          @click="selectFloor(f)"
          @close.prevent="handleDeleteFloor(f)"
        >
          {{ f.name }}
        </el-tag>
        <el-empty v-if="floors.length === 0" :image-size="60" description="暂无楼层" />
      </div>
    </el-card>

    <el-card shadow="never">
      <template #header>
        <div class="section-header">
          <span>房间列表</span>
          <div class="header-actions">
            <el-button v-permission="'ADMIN'" type="primary" size="small" @click="openRoomDialog()">
              新增房间
            </el-button>
          </div>
        </div>
      </template>

      <el-form :inline="true" size="small" class="filter-bar">
        <el-form-item label="楼栋">
          <el-select v-model="filterBuildingId" placeholder="全部楼栋" clearable @change="onFilterChange">
            <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="楼层">
          <el-select v-model="filterFloorId" placeholder="全部楼层" clearable @change="onFilterChange">
            <el-option v-for="f in filteredFloors" :key="f.id" :label="f.name" :value="f.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="房间号">
          <el-input v-model="filterRoomNumber" placeholder="模糊搜索" clearable @change="onFilterChange" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filterStatus" placeholder="全部状态" clearable @change="onFilterChange">
            <el-option label="空闲" value="FREE" />
            <el-option label="已分配" value="OCCUPIED" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
          <el-button @click="resetFilter">重置</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="roomPage.records" v-loading="loading" stripe border size="small">
        <el-table-column prop="roomNumber" label="房间号" width="120" />
        <el-table-column label="楼栋" width="120">
          <template #default="{ row }">{{ getBuildingName(row.buildingId) }}</template>
        </el-table-column>
        <el-table-column label="楼层" width="120">
          <template #default="{ row }">{{ getFloorName(row.floorId) }}</template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'ADMIN'" size="small" type="primary" link @click="openRoomDialog(row)">编辑</el-button>
            <el-popconfirm title="确认删除该房间？" @confirm="handleDeleteRoom(row.id)">
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
          :total="roomPage.total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @change="fetchRooms"
        />
      </div>
    </el-card>

    <el-dialog v-model="showBuildingDialog" :title="editingBuilding ? '编辑楼栋' : '新增楼栋'" width="420px">
      <el-form ref="buildingFormRef" :model="buildingForm" :rules="buildingRules" label-width="90px">
        <el-form-item label="楼栋名称" prop="name">
          <el-input v-model="buildingForm.name" />
        </el-form-item>
        <el-form-item label="房东名" prop="landlord">
          <el-input v-model="buildingForm.landlord" placeholder="输入房东姓名" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="buildingForm.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showBuildingDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveBuilding">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showFloorDialog" :title="editingFloor ? '编辑楼层' : '新增楼层'" width="420px">
      <el-form ref="floorFormRef" :model="floorForm" :rules="floorRules" label-width="80px">
        <el-form-item label="楼层名称" prop="name">
          <el-input v-model="floorForm.name" />
        </el-form-item>
        <el-form-item label="排序" prop="sortOrder">
          <el-input-number v-model="floorForm.sortOrder" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showFloorDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveFloor">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showRoomDialog" :title="editingRoom ? '编辑房间' : '新增房间'" width="480px">
      <el-form ref="roomFormRef" :model="roomForm" :rules="roomRules" label-width="100px">
        <el-form-item label="所属楼栋" prop="buildingId">
          <el-select v-model="roomForm.buildingId" placeholder="选择楼栋" @change="onRoomBuildingChange">
            <el-option v-for="b in buildings" :key="b.id" :label="b.name" :value="b.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="所属楼层" prop="floorId">
          <el-select v-model="roomForm.floorId" placeholder="选择楼层">
            <el-option v-for="f in floorsForRoom" :key="f.id" :label="f.name" :value="f.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="房间号" prop="roomNumber">
          <el-input v-model="roomForm.roomNumber" />
        </el-form-item>
        <el-form-item v-if="editingRoom" label="状态" prop="status">
          <el-select v-model="roomForm.status" placeholder="选择状态">
            <el-option label="空闲" value="FREE" />
            <el-option label="禁用" value="DISABLED" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showRoomDialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="saveRoom">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import {
  getBuildings, createBuilding, updateBuilding, deleteBuilding,
  getFloors, createFloor, updateFloor, deleteFloor,
  getRooms, createRoom, updateRoom, deleteRoom,
  Building, Floor, Room, RoomPage,
} from '@/api/room'

const loading = ref(false)
const saving = ref(false)
const buildings = ref<Building[]>([])
const floors = ref<Floor[]>([])
const selectedBuildingId = ref<number | null>(null)
const selectedFloorId = ref<number | null>(null)
const selectedBuildingName = ref('')

const roomPage = reactive<RoomPage>({ records: [], total: 0, current: 1, size: 20, pages: 0 })
const page = ref(1)
const size = ref(20)

const filterBuildingId = ref<number | undefined>()
const filterFloorId = ref<number | undefined>()
const filterRoomNumber = ref('')
const filterStatus = ref('')

const showBuildingDialog = ref(false)
const showFloorDialog = ref(false)
const showRoomDialog = ref(false)
const editingBuilding = ref<Building | null>(null)
const editingFloor = ref<Floor | null>(null)
const editingRoom = ref<Room | null>(null)

const buildingFormRef = ref<FormInstance>()
const floorFormRef = ref<FormInstance>()
const roomFormRef = ref<FormInstance>()

const buildingForm = reactive({ name: '', landlord: '', sortOrder: 0 })
const floorForm = reactive({ name: '', sortOrder: 0 })
const roomForm = reactive({ buildingId: undefined as number | undefined, floorId: undefined as number | undefined, roomNumber: '', status: 'FREE' })

const buildingRules = { name: [{ required: true, message: '请输入楼栋名称', trigger: 'blur' }] }
const floorRules = { name: [{ required: true, message: '请输入楼层名称', trigger: 'blur' }] }
const roomRules = {
  buildingId: [{ required: true, message: '请选择楼栋', trigger: 'change' }],
  floorId: [{ required: true, message: '请选择楼层', trigger: 'change' }],
  roomNumber: [{ required: true, message: '请输入房间号', trigger: 'blur' }],
}

const filteredFloors = computed(() => {
  if (!filterBuildingId.value) return floors.value
  return floors.value.filter(f => f.buildingId === filterBuildingId.value)
})

const floorsForRoom = computed(() => {
  if (!roomForm.buildingId) return []
  return floors.value.filter(f => f.buildingId === roomForm.buildingId)
})

function getBuildingName(id: number) {
  return buildings.value.find(b => b.id === id)?.name || '-'
}

function getFloorName(id: number) {
  return floors.value.find(f => f.id === id)?.name || '-'
}

function statusType(status: string) {
  return { FREE: 'success', OCCUPIED: 'warning', DISABLED: 'info' }[status] || 'info'
}

function statusLabel(status: string) {
  return { FREE: '空闲', OCCUPIED: '已分配', DISABLED: '禁用' }[status] || status
}

async function init() {
  try {
    const res = await getBuildings()
    buildings.value = res.data
    if (buildings.value.length > 0 && !selectedBuildingId.value) {
      selectBuilding(buildings.value[0])
    }
  } catch { /* ignore */ }
}

async function selectBuilding(b: Building) {
  selectedBuildingId.value = b.id
  selectedBuildingName.value = b.name
  selectedFloorId.value = null
  const res = await getFloors(b.id)
  floors.value = res.data
  fetchRooms()
}

async function selectFloor(f: Floor) {
  selectedFloorId.value = f.id
  fetchRooms()
}

// Buildings
function openBuildingDialog(building?: Building) {
  editingBuilding.value = building || null
  buildingForm.name = building?.name || ''
  buildingForm.landlord = building?.landlord || ''
  buildingForm.sortOrder = building?.sortOrder || 0
  showBuildingDialog.value = true
}

async function saveBuilding() {
  const valid = await buildingFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const payload = { name: buildingForm.name, landlord: buildingForm.landlord, sortOrder: buildingForm.sortOrder }
    if (editingBuilding.value) {
      await updateBuilding(editingBuilding.value.id, payload)
      ElMessage.success('修改成功')
    } else {
      await createBuilding(payload)
      ElMessage.success('新增成功')
    }
    showBuildingDialog.value = false
    await init()
  } finally {
    saving.value = false
  }
}

async function handleDeleteBuilding(b: Building) {
  try {
    await ElMessageBox.confirm(`确认删除楼栋「${b.name}」？`, '提示', { type: 'warning' })
    await deleteBuilding(b.id)
    ElMessage.success('删除成功')
    if (selectedBuildingId.value === b.id) {
      selectedBuildingId.value = null
      selectedFloorId.value = null
      floors.value = []
    }
    await init()
  } catch (e: any) {
    if (e?.response?.data?.message) ElMessage.error(e.response.data.message)
  }
}

function openFloorDialog(floor?: Floor) {
  editingFloor.value = floor || null
  floorForm.name = floor?.name || ''
  floorForm.sortOrder = floor?.sortOrder || 0
  showFloorDialog.value = true
}

async function saveFloor() {
  const valid = await floorFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editingFloor.value) {
      await updateFloor(editingFloor.value.id, { buildingId: selectedBuildingId.value!, name: floorForm.name, sortOrder: floorForm.sortOrder })
      ElMessage.success('修改成功')
    } else {
      await createFloor({ buildingId: selectedBuildingId.value!, name: floorForm.name, sortOrder: floorForm.sortOrder })
      ElMessage.success('新增成功')
    }
    showFloorDialog.value = false
    if (selectedBuildingId.value) {
      const res = await getFloors(selectedBuildingId.value)
      floors.value = res.data
    }
  } finally {
    saving.value = false
  }
}

async function handleDeleteFloor(f: Floor) {
  try {
    await ElMessageBox.confirm(`确认删除楼层「${f.name}」？`, '提示', { type: 'warning' })
    await deleteFloor(f.id)
    ElMessage.success('删除成功')
    if (selectedBuildingId.value) {
      const res = await getFloors(selectedBuildingId.value)
      floors.value = res.data
    }
  } catch (e: any) {
    if (e?.response?.data?.message) ElMessage.error(e.response.data.message)
  }
}

// Rooms
function onFilterChange() {
  page.value = 1
}

function search() {
  page.value = 1
  fetchRooms()
}

function resetFilter() {
  filterBuildingId.value = undefined
  filterFloorId.value = undefined
  filterRoomNumber.value = ''
  filterStatus.value = ''
  page.value = 1
  fetchRooms()
}

async function fetchRooms() {
  loading.value = true
  try {
    const res = await getRooms({
      buildingId: filterBuildingId.value,
      floorId: filterFloorId.value,
      roomNumber: filterRoomNumber.value || undefined,
      status: filterStatus.value || undefined,
      page: page.value,
      size: size.value,
    })
    Object.assign(roomPage, res.data)
  } finally {
    loading.value = false
  }
}

function openRoomDialog(room?: Room) {
  editingRoom.value = room || null
  if (room) {
    roomForm.buildingId = room.buildingId
    roomForm.floorId = room.floorId
    roomForm.roomNumber = room.roomNumber
    roomForm.status = room.status
  } else {
    roomForm.buildingId = selectedBuildingId.value || undefined
    roomForm.floorId = selectedFloorId.value || undefined
    roomForm.roomNumber = ''
    roomForm.status = 'FREE'
  }
  showRoomDialog.value = true
}

function onRoomBuildingChange(val: number) {
  roomForm.floorId = undefined
}

async function saveRoom() {
  const valid = await roomFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (editingRoom.value) {
      await updateRoom(editingRoom.value.id, {
        buildingId: roomForm.buildingId,
        floorId: roomForm.floorId,
        roomNumber: roomForm.roomNumber,
        status: roomForm.status,
      })
      ElMessage.success('修改成功')
    } else {
      await createRoom({ buildingId: roomForm.buildingId!, floorId: roomForm.floorId!, roomNumber: roomForm.roomNumber })
      ElMessage.success('新增成功')
    }
    showRoomDialog.value = false
    await fetchRooms()
  } finally {
    saving.value = false
  }
}

async function handleDeleteRoom(id: number) {
  try {
    await deleteRoom(id)
    ElMessage.success('删除成功')
    await fetchRooms()
  } catch (e: any) {
    if (e?.response?.data?.message) ElMessage.error(e.response.data.message)
  }
}

onMounted(init)
</script>

<style scoped>
.room-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.section-card {
  border-radius: 8px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.building-list, .floor-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.building-tag, .floor-tag {
  cursor: pointer;
  font-size: 14px;
  padding: 4px 12px;
}
.landlord-hint {
  font-size: 11px; opacity: 0.7; margin-left: 4px;
}

.filter-bar {
  margin-bottom: 0;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>