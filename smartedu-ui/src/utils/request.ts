import axios, { type AxiosInstance, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

// 响应数据接口
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: number
}

// 创建 axios 实例
// 使用环境变量配置 API 基础路径，默认为 /api（适用于 Docker 部署）
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8'
  }
})

// 请求拦截器
service.interceptors.request.use(
  (config) => {
    // 从 localStorage 直接获取 token（避免在拦截器中使用 useUserStore）
    const userStoreJson = localStorage.getItem('smartedu_user')
    let token: string | null = null
    if (userStoreJson) {
      try {
        const userStore = JSON.parse(userStoreJson)
        token = userStore.token
      } catch (e) {
        console.error('解析用户 store 失败:', e)
      }
    }

    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse<ApiResponse>) => {
    const res = response.data

    // 成功
    if (res.code === 200) {
      return res
    }

    // 业务错误
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    console.error('HTTP 错误:', error)

    switch (error.response?.status) {
      case 401:
        ElMessage.error('登录已过期，请重新登录')
        // 清除用户信息
        localStorage.removeItem('smartedu_user')
        // 跳转到登录页
        window.location.href = '/login'
        break
      case 403:
        ElMessage.error('权限不足')
        break
      case 404:
        ElMessage.error('请求的资源不存在')
        break
      case 500:
        ElMessage.error('服务器错误')
        break
      default:
        ElMessage.error(error.message || '网络错误')
    }

    return Promise.reject(error)
  }
)

// 封装请求方法
export function get<T = any>(url: string, params?: any): Promise<ApiResponse<T>> {
  return service.get(url, { params })
}

export function post<T = any>(url: string, data?: any, config?: AxiosRequestConfig): Promise<ApiResponse<T>> {
  return service.post(url, data, config)
}

export function put<T = any>(url: string, data?: any): Promise<ApiResponse<T>> {
  return service.put(url, data)
}

export function del<T = any>(url: string, params?: any): Promise<ApiResponse<T>> {
  return service.delete(url, { params })
}

// 文件上传专用方法（使用 FormData）
export function postFile<T = any>(url: string, formData: FormData): Promise<ApiResponse<T>> {
  return service.post(url, formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

export default service
