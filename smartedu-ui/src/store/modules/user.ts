import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo } from './types'

export const useUserStore = defineStore(
  'user',
  () => {
    const token = ref<string>('')
    const userInfo = ref<UserInfo | null>(null)

    // 设置用户信息
    const setUserInfo = (info: UserInfo) => {
      userInfo.value = info
    }

    // 设置 Token
    const setToken = (newToken: string) => {
      token.value = newToken
    }

    // 登出
    const logout = () => {
      token.value = ''
      userInfo.value = null
    }

    // 是否已登录（计算属性）
    const isLoggedIn = computed(() => !!token.value && !!userInfo.value)

    // 判断是否为学生
    const isStudent = () => userInfo.value?.role === 'STUDENT'

    // 判断是否为教师
    const isTeacher = () => userInfo.value?.role === 'TEACHER'

    // 判断是否为管理员
    const isAdmin = () => userInfo.value?.role === 'ADMIN'

    return {
      token,
      userInfo,
      setUserInfo,
      setToken,
      logout,
      isLoggedIn,
      isStudent,
      isTeacher,
      isAdmin
    }
  },
  {
    persist: {
      key: 'smartedu_user',
      storage: localStorage,
      paths: ['token', 'userInfo']
    }
  }
)
