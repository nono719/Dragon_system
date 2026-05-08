import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../store/user'

const http = axios.create({
  baseURL: '/api',
  timeout: 60000
})

http.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers.Authorization = `Bearer ${userStore.token}`
  }
  return config
})

http.interceptors.response.use(
  (resp) => {
    const body = resp.data
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code !== 0) {
        ElMessage.error(body.message || '请求失败')
        return Promise.reject(new Error(body.message || 'biz_error'))
      }
      return body.data
    }
    return body
  },
  (err) => {
    const status = err.response?.status
    const data = err.response?.data
    const msg = data?.message || err.message || '网络异常'
    if (status === 401) {
      const userStore = useUserStore()
      userStore.logout()
      ElMessage.warning('请先登录')
    } else {
      ElMessage.error(msg)
    }
    return Promise.reject(err)
  }
)

export default http
