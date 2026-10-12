<template>
  <el-container class="layout-container">
    <!-- AI 聊天助手 -->
    <AIChatAssistant />

    <!-- 侧边栏 -->
    <el-aside :width="asideWidth">
      <div class="logo-container">
        <img v-if="!appStore.sidebarCollapsed" src="/logo.svg" alt="Logo" class="logo" />
        <span v-if="!appStore.sidebarCollapsed" class="logo-text">SmartEdu</span>
      </div>

      <el-menu
        :default-active="activeMenu"
        :collapse="appStore.sidebarCollapsed"
        :unique-opened="true"
        router
      >
        <template v-for="route in menuRoutes" :key="route.path">
          <!-- 直接平铺显示所有菜单项，不使用子菜单折叠 -->
          <el-menu-item
            v-if="route.children && route.children.length === 1"
            :index="resolvePath(route.path, route.children[0].path)"
          >
            <el-icon v-if="route.children[0].meta?.icon">
              <component :is="route.children[0].meta.icon" />
            </el-icon>
            <span>{{ route.children[0].meta?.title }}</span>
          </el-menu-item>

          <!-- 多个子菜单时直接平铺显示 -->
          <template v-else-if="route.children && route.children.length > 1">
            <!-- 先显示父菜单标题 -->
            <div class="menu-group-title" v-if="!appStore.sidebarCollapsed">
              {{ route.meta?.title }}
            </div>
            <!-- 然后平铺显示所有子菜单 -->
            <el-menu-item
              v-for="child in route.children"
              :key="child.path"
              :index="resolvePath(route.path, child.path)"
            >
              <el-icon v-if="child.meta?.icon"><component :is="child.meta.icon" /></el-icon>
              <span>{{ child.meta?.title }}</span>
            </el-menu-item>
          </template>
        </template>
      </el-menu>
    </el-aside>

    <!-- 主内容区 -->
    <el-container>
      <!-- 顶部导航 -->
      <el-header class="layout-header">
        <div class="header-left">
          <el-icon class="collapse-icon" @click="toggleSidebar">
            <Fold v-if="!appStore.sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">
              <el-icon class="breadcrumb-icon"><House /></el-icon>
              首页
            </el-breadcrumb-item>
            <el-breadcrumb-item v-if="currentRoute">{{ currentRoute.meta?.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="header-right">
          <el-dropdown trigger="click">
            <div class="user-info">
              <el-avatar :size="36" :src="userStore.userInfo?.avatar" />
              <div class="user-details">
                <span class="user-name">{{ userStore.userInfo?.realName || '用户' }}</span>
                <span class="user-id">{{ userStore.userInfo?.username || '' }}</span>
              </div>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="$router.push('/profile')">
                  <el-icon><User /></el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item divided @click="handleLogout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主要内容 -->
      <el-main class="layout-main">
        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in">
            <component :is="Component" />
          </transition>
        </router-view>
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import { useAppStore } from '@/store/modules/app'
import { Fold, Expand, User, SwitchButton, House } from '@element-plus/icons-vue'
import { logout } from '@/api/auth'
import AIChatAssistant from '@/components/ai/AIChatAssistant.vue'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const appStore = useAppStore()

// 侧边栏宽度
const asideWidth = computed(() =>
  appStore.sidebarCollapsed ? '64px' : '220px'
)

// 当前激活的菜单
const activeMenu = computed(() => route.path)

// 当前路由
const currentRoute = computed(() =>
  router.options.routes.find(r => r.path === route.matched[0]?.path)
)

// 菜单路由（根据用户角色过滤，学生只看到学生端，教师只看到教师端）
const menuRoutes = computed(() => {
  const role = userStore.userInfo?.role
  const routes = router.options.routes
    .filter(r => {
      if (r.path === '/login' || r.path === '/register') return false
      if (r.path === '/') return true // 馆页所有人可见
      const routeRoles = r.meta?.roles as string[] | undefined
      if (!routeRoles?.length) return true
      return role && routeRoles.includes(role)
    })
    .map(r => {
      // 过滤掉带动态参数的子路由（如 :id），只保留静态路由用于菜单显示
      if (r.children) {
        return {
          ...r,
          children: r.children.filter(child => !child.path.includes(':'))
        }
      }
      return r
    })
    .filter(r => {
      // 过滤掉没有可显示子菜单的路由
      if (r.children && r.children.length === 0) return false
      return true
    })
  return routes
})

// 切换侧边栏
const toggleSidebar = () => {
  appStore.toggleSidebar()
}

// 解析路径
const resolvePath = (parentPath: string, childPath: string) => {
  return `${parentPath}/${childPath}`.replace(/\/+/g, '/')
}

// 退出登录
const handleLogout = async () => {
  try {
    await logout()
  } catch (e) {
    console.error(e)
  }
  userStore.logout()
  router.push('/login')
}
</script>

<style scoped lang="scss">
.layout-container {
  height: 100vh;

  .el-aside {
    background-color: #fff;
    transition: width 0.3s;
    overflow: hidden;
    border-right: 1px solid #e6e6e6;

    .logo-container {
      display: flex;
      align-items: center;
      justify-content: center;
      height: 60px;
      padding: 10px;

      .logo {
        width: 32px;
        height: 32px;
      }

      .logo-text {
        margin-left: 10px;
        font-size: 18px;
        font-weight: 600;
        color: #409eff;
      }
    }

    .el-menu {
      background-color: #fff;
      border-right: none;

      .menu-group-title {
        padding: 12px 20px 8px;
        font-size: 12px;
        color: #909399;
        letter-spacing: 1px;
        text-transform: uppercase;
      }

      .el-sub-menu__title,
      .el-menu-item {
        color: #303133;

        &:hover {
          background-color: #ecf5ff;
          color: #409eff;
        }

        &.is-active {
          background-color: #ecf5ff;
          color: #409eff;
        }
      }
    }
  }

  .layout-header {
    background-color: #fff;
    box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);

    .header-left {
      display: flex;
      align-items: center;
      gap: 20px;

      .collapse-icon {
        font-size: 20px;
        cursor: pointer;
        transition: color 0.3s;

        &:hover {
          color: var(--primary-color);
        }
      }

      .breadcrumb-icon {
        margin-right: 4px;
        vertical-align: middle;
      }
    }

    .header-right {
      .user-info {
        display: flex;
        align-items: center;
        gap: 10px;
        cursor: pointer;

        .user-details {
          display: flex;
          flex-direction: column;
          gap: 2px;

          .user-name {
            font-size: 14px;
            color: #303133;
            font-weight: 500;
          }

          .user-id {
            font-size: 12px;
            color: #909399;
          }
        }
      }
    }
  }

  .layout-main {
    background-color: #f5f7fa;
    padding: 0;
    overflow-y: auto;
  }
}

// 过渡动画
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
