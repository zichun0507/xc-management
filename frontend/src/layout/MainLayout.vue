<template>
  <div class="layout" :class="{ dark: isDark }">
    <el-container class="layout-container">
      <el-aside :width="isCollapse ? '64px' : '220px'" class="layout-aside">
        <div class="logo">
          <span v-if="!isCollapse" class="logo-text">新长管理平台</span>
          <span v-else class="logo-text-mini">新长</span>
        </div>
        <el-menu
          :default-active="route.path"
          :collapse="isCollapse"
          :collapse-transition="false"
          router
          class="menu"
        >
          <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
            <el-icon><component :is="item.icon" /></el-icon>
            <template #title>{{ item.title }}</template>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <el-container>
        <el-header class="layout-header">
          <div class="header-left">
            <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
              <Fold v-if="!isCollapse" />
              <Expand v-else />
            </el-icon>
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
              <el-breadcrumb-item v-if="currentTitle">{{ currentTitle }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          <div class="header-right">
            <el-tooltip :content="isDark ? '切换浅色模式' : '切换深色模式'">
              <el-icon class="header-icon" @click="toggleDark">
                <Moon v-if="!isDark" />
                <Sunny v-else />
              </el-icon>
            </el-tooltip>
            <el-dropdown trigger="click" @command="handleCommand">
              <span class="user-info">
                <el-icon><User /></el-icon>
                {{ userStore.userInfo?.realName || userStore.userInfo?.username }}
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人信息</el-dropdown-item>
                  <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </div>
        </el-header>

        <el-main class="layout-main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>

    <el-drawer
      v-model="drawerVisible"
      direction="ltr"
      size="220px"
      :with-header="false"
      class="mobile-drawer"
    >
      <div class="logo">
        <span class="logo-text">新长管理平台</span>
      </div>
      <el-menu :default-active="route.path" router @select="drawerVisible = false">
        <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
          <el-icon><component :is="item.icon" /></el-icon>
          <template #title>{{ item.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()
const drawerVisible = ref(false)
const isCollapse = ref(false)

const isDark = ref(localStorage.getItem('dark-mode') === 'true')
function toggleDark() {
  isDark.value = !isDark.value
  localStorage.setItem('dark-mode', String(isDark.value))
  document.documentElement.classList.toggle('dark', isDark.value)
}

watch(isDark, (val) => {
  document.documentElement.classList.toggle('dark', val)
})

onMounted(() => {
  document.documentElement.classList.toggle('dark', isDark.value)
})

const currentTitle = computed(() => {
  return route.meta?.title as string || ''
})

const menuItems = computed(() => {
  const items = [
    { path: '/home', title: '首页', icon: 'HomeFilled' },
    { path: '/rooms', title: '房间管理', icon: 'OfficeBuilding' },
    { path: '/companies', title: '入驻企业', icon: 'Briefcase' },
    { path: '/employees', title: '企业人员', icon: 'UserFilled' },
    { path: '/templates', title: '代办模板', icon: 'Document' },
  ]
  if (userStore.isAdmin) {
    items.push(
      { path: '/system/users', title: '用户管理', icon: 'User' },
      { path: '/system/logs', title: '操作日志', icon: 'List' },
    )
  }
  return items
})

function handleCommand(command: string) {
  if (command === 'logout') {
    userStore.logout()
  }
}
</script>

<style scoped>
.layout {
  height: 100vh;
  overflow: hidden;
}

.layout-container {
  height: 100%;
}

.layout-aside {
  background-color: var(--el-menu-bg-color);
  border-right: 1px solid var(--el-border-color-light);
  transition: width 0.3s;
  overflow: hidden;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-bottom: 1px solid var(--el-border-color-light);
}

.logo-text {
  font-size: 18px;
  font-weight: 600;
  white-space: nowrap;
  color: var(--el-text-color-primary);
}

.logo-text-mini {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.menu {
  border-right: none;
}

.layout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  background-color: var(--el-bg-color);
  border-bottom: 1px solid var(--el-border-color-light);
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: var(--el-text-color-regular);
}

.header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.header-icon {
  font-size: 20px;
  cursor: pointer;
  color: var(--el-text-color-regular);
}

.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 4px;
  color: var(--el-text-color-regular);
}

.layout-main {
  padding: 20px;
  background-color: var(--el-bg-color-page);
  overflow-y: auto;
}

.mobile-drawer .logo {
  justify-content: center;
}

@media (max-width: 768px) {
  .layout-aside {
    display: none;
  }
}
</style>