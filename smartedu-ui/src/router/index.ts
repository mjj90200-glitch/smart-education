import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'

// 路由配置
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/',
    component: () => import('@/layout/index.vue'),
    redirect: '/home',
    children: [
      {
        path: 'home',
        name: 'Home',
        component: () => import('@/views/home/Index.vue'),
        meta: { title: '首页', icon: 'Home' }
      }
    ]
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/profile/Index.vue'),
    meta: { title: '个人中心', requiresAuth: true },
    children: []
  },
  {
    path: '/student',
    component: () => import('@/layout/index.vue'),
    redirect: '/student/dashboard',
    meta: { title: '学生端', requiresAuth: true, roles: ['STUDENT'] },
    children: [
      {
        path: 'dashboard',
        name: 'StudentDashboard',
        component: () => import('@/views/student/Dashboard.vue'),
        meta: { title: '学习看板', icon: 'DataAnalysis' }
      },
      {
        path: 'learning-analysis',
        name: 'StudentLearningAnalysis',
        component: () => import('@/views/student/LearningAnalysis.vue'),
        meta: { title: '学情分析', icon: 'TrendCharts' }
      },

      {
        path: 'homework',
        name: 'StudentHomework',
        component: () => import('@/views/student/Homework.vue'),
        meta: { title: '我的作业', icon: 'Document' }
      },
      {
        path: 'assistant',
        name: 'StudentAssistant',
        component: () => import('@/views/shared/SmartAssistant.vue'),
        meta: { title: '智学助手', icon: 'ChatDotRound' }
      }
    ]
  },
  {
    path: '/teacher',
    component: () => import('@/layout/index.vue'),
    redirect: '/teacher/class-evaluation',
    meta: { title: '教师端', requiresAuth: true, roles: ['TEACHER', 'ADMIN'] },
    children: [
      {
        path: 'class-evaluation',
        name: 'ClassEvaluation',
        component: () => import('@/views/teacher/ClassEvaluation.vue'),
        meta: { title: '课堂评估', icon: 'VideoPlay' }
      },
      {
        path: 'analysis',
        name: 'AnalysisReport',
        component: () => import('@/views/teacher/AnalysisReport.vue'),
        meta: { title: '学情分析', icon: 'TrendCharts' }
      },
      {
        path: 'homework',
        name: 'HomeworkManage',
        component: () => import('@/views/teacher/HomeworkManage.vue'),
        meta: { title: '作业管理', icon: 'Document' }
      },
      {
        path: 'assistant',
        name: 'TeacherAssistant',
        component: () => import('@/views/shared/SmartAssistant.vue'),
        meta: { title: '智学助手', icon: 'ChatDotRound' }
      }
    ]
  },
    {
    path: '/admin',
    component: () => import('@/layout/index.vue'),
    redirect: '/admin/news',
    meta: { title: '管理后台', requiresAuth: true, roles: ['ADMIN'] },
    children: [
      {
        path: 'news',
        name: 'NewsManage',
        component: () => import('@/views/admin/NewsManage.vue'),
        meta: { title: '新闻管理', icon: 'Document' }
      },
      {
        path: 'video-audit',
        name: 'VideoAudit',
        component: () => import('@/views/admin/VideoAudit.vue'),
        meta: { title: '视频管理', icon: 'VideoPlay' }
      },
      {
        path: 'home-recommend',
        name: 'HomeRecommendManage',
        component: () => import('@/views/admin/HomeRecommendManage.vue'),
        meta: { title: '首页推荐', icon: 'SetUp' }
      }
    ]
  },
  {
    path: '/video-study',
    name: 'VideoStudy',
    component: () => import('@/layout/index.vue'),
    redirect: '/video-study/list',
    meta: { title: '视频学习', requiresAuth: false },
    children: [
      {
        path: 'list',
        name: 'VideoStudyList',
        component: () => import('@/views/video/VideoStudy.vue'),
        meta: { title: '视频学习', icon: 'VideoPlay', requiresAuth: false }
      }
    ]
  },
  {
    path: '/question-hall',
    component: () => import('@/layout/index.vue'),
    redirect: '/question-hall/list',
    meta: { title: '答疑大厅', requiresAuth: true },
    children: [
      {
        path: 'list',
        name: 'QuestionHallList',
        component: () => import('@/views/question-hall/Index.vue'),
        meta: { title: '答疑大厅', icon: 'ChatDotRound' }
      },
      {
        path: ':id',
        name: 'QuestionHallDetail',
        component: () => import('@/views/question-hall/Detail.vue'),
        meta: { title: '帖子详情' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  console.log('[路由守卫] 从:', from.path, '到:', to.path, 'name:', to.name)

  document.title = to.meta.title ? `${to.meta.title} - SmartEdu` : 'SmartEdu 智慧教育平台'

  // 从 localStorage 直接获取用户信息
  let token: string | null = null
  let userInfo: any = null
  const userStoreJson = localStorage.getItem('smartedu_user')
  console.log('[路由守卫] localStorage raw:', userStoreJson)

  if (userStoreJson) {
    try {
      const parsed = JSON.parse(userStoreJson)
      console.log('[路由守卫] parsed:', parsed)
      // pinia-plugin-persistedstate 可能存储格式为 { value: { token, userInfo } } 或直接是 { token, userInfo }
      token = parsed.value?.token || parsed.token || null
      userInfo = parsed.value?.userInfo || parsed.userInfo || null
      console.log('[路由守卫] extracted token:', token, 'userInfo:', userInfo)
    } catch (e) {
      console.error('[路由守卫] 解析用户 store 失败:', e)
    }
  }

  console.log('[路由守卫] token:', token, 'userInfo:', userInfo, 'to:', to.path)

  // 不需要登录的页面
  if (['Login', 'Register'].includes(to.name as string)) {
    if (token) {
      console.log('[路由守卫] 已登录，跳转到首页')
      next('/')
    } else {
      console.log('[路由守卫] 未登录，访问登录页')
      next()
    }
    return
  }

  // 需要登录 - 检查所有匹配路由的 meta.requiresAuth
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth)
  console.log('[路由守卫] requiresAuth:', requiresAuth, 'to.matched:', to.matched.map(r => ({ path: r.path, meta: r.meta })))

  if (requiresAuth && !token) {
    console.log('[路由守卫] 无 token，重定向到登录页')
    next({ name: 'Login', query: { redirect: to.fullPath } })
    return
  }

  // 检查角色权限（遍历所有匹配的路由记录）
  for (const record of to.matched) {
    const roles = record.meta?.roles as string[] | undefined
    if (roles?.length) {
      console.log('[路由守卫] 检查角色:', roles, '用户角色:', userInfo?.role)
      if (!roles.includes(userInfo?.role || '')) {
        console.log('[路由守卫] 角色权限不足，重定向到首页')
        next({ path: '/home' })
        return
      }
    }
  }

  console.log('[路由守卫] 允许访问')
  next()
})

export default router
