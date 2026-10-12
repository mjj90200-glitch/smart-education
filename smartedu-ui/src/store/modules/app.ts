import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore(
  'app',
  () => {
    // 侧边栏折叠状态
    const sidebarCollapsed = ref(false)

    // 切换侧边栏
    const toggleSidebar = () => {
      sidebarCollapsed.value = !sidebarCollapsed.value
    }

    // 设置侧边栏状态
    const setSidebarCollapsed = (collapsed: boolean) => {
      sidebarCollapsed.value = collapsed
    }

    // 主题
    const theme = ref<'light' | 'dark'>('light')

    // 切换主题
    const toggleTheme = () => {
      theme.value = theme.value === 'light' ? 'dark' : 'light'
      document.documentElement.setAttribute('data-theme', theme.value)
    }

    return {
      sidebarCollapsed,
      toggleSidebar,
      setSidebarCollapsed,
      theme,
      toggleTheme
    }
  },
  {
    persist: {
      key: 'smartedu_app',
      storage: localStorage,
      paths: ['sidebarCollapsed', 'theme']
    }
  }
)
