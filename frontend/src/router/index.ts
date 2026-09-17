import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    meta: { requiresAuth: true },
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/home/Home.vue'),
        meta: { title: '首页', icon: 'HomeFilled' },
      },
      {
        path: 'rooms',
        name: 'Rooms',
        component: () => import('@/views/room/RoomList.vue'),
        meta: { title: '房间管理', icon: 'OfficeBuilding' },
      },
      {
        path: 'companies',
        name: 'Companies',
        component: () => import('@/views/company/CompanyList.vue'),
        meta: { title: '入驻企业管理', icon: 'Briefcase' },
      },
      {
        path: 'employees',
        name: 'Employees',
        component: () => import('@/views/employee/EmployeeList.vue'),
        meta: { title: '企业人员管理', icon: 'UserFilled' },
      },
      {
        path: 'templates',
        name: 'Templates',
        component: () => import('@/views/template/TemplateList.vue'),
        meta: { title: '代办模板与导出', icon: 'Document' },
      },
      {
        path: 'system/users',
        name: 'UserList',
        component: () => import('@/views/system/UserList.vue'),
        meta: { title: '用户管理', icon: 'User', adminOnly: true },
      },
      {
        path: 'system/logs',
        name: 'OperationLog',
        component: () => import('@/views/system/OperationLog.vue'),
        meta: { title: '操作日志', icon: 'List', adminOnly: true },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  if (to.meta.requiresAuth !== false && !userStore.isLoggedIn) {
    next('/login')
  } else if (to.path === '/login' && userStore.isLoggedIn) {
    next('/')
  } else if (to.meta.adminOnly && !userStore.isAdmin) {
    next('/')
  } else {
    next()
  }
})

export default router