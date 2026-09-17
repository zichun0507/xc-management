import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/api/request'
import router from '@/router'

interface UserInfo {
  id: number
  username: string
  realName: string
  role: string
  status: string
}

export const useUserStore = defineStore('user', () => {
  const token = ref(localStorage.getItem('xc-token') || '')
  const userInfo = ref<UserInfo | null>(loadUserInfo())

  function loadUserInfo(): UserInfo | null {
    const raw = localStorage.getItem('xc-user')
    if (!raw) return null
    try {
      return JSON.parse(raw)
    } catch {
      return null
    }
  }

  const isLoggedIn = computed(() => !!token.value)
  const role = computed(() => userInfo.value?.role || '')
  const isAdmin = computed(() => role.value === 'ADMIN')

  async function login(username: string, password: string) {
    const res = await request.post('/auth/login', { username, password })
    const data = res.data as { token: string; tokenName: string; user: UserInfo }
    token.value = data.token
    userInfo.value = data.user
    localStorage.setItem('xc-token', data.token)
    localStorage.setItem('xc-token-name', data.tokenName)
    localStorage.setItem('xc-user', JSON.stringify(data.user))
    await router.push('/')
  }

  async function logout() {
    try {
      await request.post('/auth/logout')
    } finally {
      token.value = ''
      userInfo.value = null
      localStorage.removeItem('xc-token')
      localStorage.removeItem('xc-token-name')
      localStorage.removeItem('xc-user')
      router.push('/login')
    }
  }

  async function fetchUserInfo() {
    try {
      const res = await request.get('/auth/me')
      const user = res.data as UserInfo
      userInfo.value = user
      localStorage.setItem('xc-user', JSON.stringify(user))
    } catch {
      await logout()
    }
  }

  return { token, userInfo, isLoggedIn, role, isAdmin, login, logout, fetchUserInfo }
})