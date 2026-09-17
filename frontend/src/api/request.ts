import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 60000,
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('xc-token')
  const tokenName = localStorage.getItem('xc-token-name') || 'xc-token'
  if (token) {
    config.headers[tokenName] = token
  }
  return config
})

function handleTokenExpired() {
  localStorage.removeItem('xc-token')
  localStorage.removeItem('xc-user')
  ElMessage.error('登录已过期，3 秒后跳转登录页...')
  setTimeout(() => router.push('/login'), 3000)
}

request.interceptors.response.use(
  (response) => {
    if (response.config.responseType === 'blob' || response.data instanceof Blob) {
      return response
    }
    const res = response.data
    if (res.code === 401) {
      handleTokenExpired()
      return Promise.reject(new Error(res.message))
    }
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message))
    }
    return res
  },
  (error) => {
    if (error.response?.status === 401) {
      handleTokenExpired()
    } else {
      const msg = error.response?.data?.message || error.message || '网络异常'
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  },
)

export default request