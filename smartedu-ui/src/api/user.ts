import { get, put, post } from '@/utils/request'

// 获取当前用户信息
export function getCurrentUserInfo() {
  return get('/user/info')
}

// 更新用户信息（邮箱、手机、头像）
export function updateUserInfo(data: any) {
  return put('/user/info', data)
}

// 修改密码
export function changePassword(data: { oldPassword: string; newPassword: string }) {
  return post('/user/change-password', data)
}

// 上传头像
export function uploadAvatar(formData: FormData) {
  return post('/user/avatar', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}
