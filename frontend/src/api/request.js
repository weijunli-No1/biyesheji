import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const request = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

request.interceptors.request.use(config => {
  const auth = useAuthStore()
  if (auth.token) {
    config.headers.Authorization = `Bearer ${auth.token}`
  }
  return config
})

request.interceptors.response.use(
  res => {
    // 后端续期后会在响应头携带新 token，前端自动更新
    const newToken = res.headers['x-new-token']
    if (newToken) {
      const auth = useAuthStore()
      auth.token = newToken
      localStorage.setItem('token', newToken)
    }

    const data = res.data
    if (data.code === 200) return data
    if (data.code === 401) {
      ElMessage.warning('登录已过期，请重新登录')
      useAuthStore().logout()
      router.push('/login')
      return Promise.reject(new Error('登录已过期'))
    }
    if (data.code === 403) {
      ElMessage.error('无权限执行此操作')
      return Promise.reject(new Error('无权限'))
    }
    ElMessage.error(data.message || '请求失败')
    return Promise.reject(new Error(data.message || '请求失败'))
  },
  err => {
    if (err.code === 'ECONNABORTED' || err.message?.includes('timeout')) {
      ElMessage.error('请求超时，请检查网络后重试')
    } else if (!err.response) {
      ElMessage.error('无法连接到服务器，请检查网络')
    } else {
      const status = err.response?.status
      if (status === 401) {
        ElMessage.warning('登录已过期，请重新登录')
        useAuthStore().logout()
        router.push('/login')
      } else if (status === 403) {
        ElMessage.error('无权限执行此操作')
      } else if (status === 404) {
        ElMessage.error('请求的资源不存在')
      } else if (status >= 500) {
        ElMessage.error('服务器内部错误，请稍后重试')
      } else {
        ElMessage.error(err.response?.data?.message || '请求失败')
      }
    }
    return Promise.reject(err)
  }
)

export default request
