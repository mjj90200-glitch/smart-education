import { get, post } from '@/utils/request'

// 登录
export function login(data: { username: string; password: string; rememberMe?: boolean }) {
  return post('/auth/login', data)
}

// 注册
export function register(data: any) {
  return post('/auth/register', data)
}

// 登出
export function logout() {
  return post('/auth/logout')
}

// 获取当前用户信息
export function getCurrentUserInfo() {
  return get('/auth/info')
}
