<template>
  <div class="home-container">
    <!-- 科技资讯大屏 -->
    <div class="tech-news-section">
      <div class="section-header">
        <div class="header-left">
          <el-icon class="header-icon"><Monitor /></el-icon>
          <h2>科技前沿资讯</h2>
          <span class="subtitle">实时关注科技动态，把握时代脉搏</span>
        </div>
        <div class="header-right">
          <el-tag type="primary" effect="plain">AI 人工智能</el-tag>
          <el-tag type="success" effect="plain">前沿科技</el-tag>
          <el-tag type="warning" effect="plain">行业动态</el-tag>
        </div>
      </div>

      <div class="news-content">
        <!-- 左侧轮播图 (70%) -->
        <div class="carousel-wrapper">
          <el-carousel height="420px" v-if="carouselNews.length" indicator-position="outside">
            <el-carousel-item v-for="news in carouselNews" :key="news.id">
              <div class="carousel-item" @click="handleNewsClick(news)">
                <div class="carousel-image-wrapper" :style="news.imageUrl ? {} : { background: fallbackGradients[(news.id - 1) % fallbackGradients.length] }">
                  <img
                    v-if="news.imageUrl"
                    :src="getImageUrl(news.imageUrl)"
                    :alt="news.title"
                    class="carousel-image"
                    @error="(event) => handleImageError(event, news.id)"
                  />
                  <div class="image-overlay">
                    <el-icon class="overlay-icon"><ZoomIn /></el-icon>
                  </div>
                </div>
                <div class="carousel-info">
                  <div class="carousel-source">{{ news.sourceName }}</div>
                  <h3 class="carousel-title">{{ news.title }}</h3>
                  <p class="carousel-summary">{{ news.summary }}</p>
                  <div class="carousel-meta">
                    <span class="publish-time">{{ formatTime(news.publishTime) }}</span>
                    <el-tag v-if="news.isTop" size="small" type="danger">置顶</el-tag>
                    <el-tag v-if="news.isManual" size="small" type="success">精选</el-tag>
                  </div>
                </div>
              </div>
            </el-carousel-item>
          </el-carousel>
          <el-card v-else class="empty-carousel">
            <el-empty description="暂无轮播新闻" :image-size="80">
              <el-button type="primary" size="small" @click="refreshNews">刷新</el-button>
            </el-empty>
          </el-card>
        </div>

        <!-- 右侧新闻列表 (30%) -->
        <div class="news-list-wrapper">
          <div class="list-header">
            <h3>📰 快讯</h3>
            <el-button link type="primary" @click="refreshNews">
              <el-icon><Refresh /></el-icon>
              刷新
            </el-button>
          </div>
          <div class="news-list">
            <div
              v-for="(news, index) in listNews"
              :key="news.id"
              class="news-list-item"
              @click="handleNewsClick(news)"
            >
              <span class="item-index" :class="{ 'top-3': index < 3 }">{{ index + 1 }}</span>
              <div class="item-content">
                <div class="item-title">{{ news.title }}</div>
                <div class="item-meta">
                  <span class="item-source">{{ news.sourceName }}</span>
                  <span class="item-time">{{ formatTime(news.publishTime) }}</span>
                </div>
              </div>
            </div>
            <div v-if="!listNews.length" class="empty-list">
              <el-empty description="暂无快讯" :image-size="60" />
            </div>
          </div>
        </div>
      </div>
    </div>

    <el-divider class="section-divider" />

    <!-- 视频学习 - B站像素级复刻 -->
    <div class="video-section">
      <!-- 紧凑标题栏 -->
      <div class="video-header">
        <div class="header-title">
          <el-icon class="title-icon"><VideoPlay /></el-icon>
          <h2>视频学习</h2>
          <span class="subtitle">精选优质学习资源</span>
        </div>
        <el-button type="primary" link class="more-btn" @click="router.push('/video-study')">
          查看更多 <el-icon><ArrowRight /></el-icon>
        </el-button>
      </div>

      <!-- B站风格栅格布局 - gutter=16 -->
      <el-row :gutter="16" class="video-row" v-if="featuredVideo || gridVideos.length > 0">
        <!-- 左侧大轮播 (Span 11) -->
        <el-col :span="11" class="video-left">
          <div class="featured-video" v-if="featuredVideo" @click="openVideo(featuredVideo)">
            <div class="featured-cover">
              <img
                :src="getVideoCoverUrl(featuredVideo.coverUrl)"
                :alt="featuredVideo.title"
                class="cover-image"
                referrerpolicy="no-referrer"
                @error="handleCoverError"
              />
              <!-- B站风格播放量遮罩 -->
              <div class="cover-overlay">
                <div class="play-btn">
                  <el-icon><VideoPlay /></el-icon>
                </div>
              </div>
              <!-- 标题浮动在图片底部 - 磨砂玻璃背景 -->
              <div class="featured-title-bar">
                <h3 class="featured-title">{{ featuredVideo.title }}</h3>
                <div class="featured-stats">
                  <el-icon><View /></el-icon>
                  <span>{{ formatCount(featuredVideo.viewCount) }}</span>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="featured-placeholder">
            <el-empty description="暂无推荐视频" :image-size="60" />
          </div>
        </el-col>

        <!-- 右侧 2x2 网格 (Span 13) -->
        <el-col :span="13" class="video-right">
          <div class="video-grid">
            <!-- 实际视频卡片 -->
            <div class="video-card" v-for="video in gridVideos" :key="video.id" @click="openVideo(video)">
              <div class="card-cover">
                <img
                  :src="getVideoCoverUrl(video.coverUrl)"
                  :alt="video.title"
                  class="cover-image"
                  referrerpolicy="no-referrer"
                  @error="handleCoverError"
                />
                <!-- B站风格播放量遮罩 - 左下角 -->
                <div class="card-stats-overlay">
                  <el-icon class="stats-icon"><VideoPlay /></el-icon>
                  <span class="stats-text">{{ formatCount(video.viewCount) }}</span>
                </div>
              </div>
              <div class="card-info">
                <h4 class="card-title">{{ video.title }}</h4>
              </div>
            </div>
            <!-- 占位卡片 - 固定4格布局 -->
            <div class="video-card placeholder" v-for="i in (4 - gridVideos.length)" :key="'placeholder-' + i">
              <div class="card-cover">
                <div class="placeholder-content">
                  <el-icon><VideoPlay /></el-icon>
                </div>
              </div>
              <div class="card-info">
                <h4 class="card-title placeholder-text">暂无视频</h4>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>

      <!-- 无数据时显示 -->
      <div v-else class="no-videos">
        <el-empty description="暂无视频内容" :image-size="80" />
      </div>
    </div>

    <el-divider class="section-divider" />

    <!-- 快捷入口 -->
    <el-card class="quick-access-card">
      <template #header>
        <div class="card-header">
          <div class="header-title">
            <el-icon class="header-icon"><Grid /></el-icon>
            <span>快捷入口</span>
          </div>
        </div>
      </template>
      <div class="quick-actions">
        <template v-if="userStore.isStudent()">
          <el-button type="primary" @click="router.push('/question-hall')" size="large">
            <el-icon><ChatDotRound /></el-icon>
            答疑大厅
          </el-button>
          <el-button type="primary" @click="router.push('/student/learning-analysis')" size="large">
            <el-icon><DataAnalysis /></el-icon>
            学情分析
          </el-button>
          <el-button type="primary" @click="router.push('/student/dashboard')" size="large">
            <el-icon><DataBoard /></el-icon>
            学习看板
          </el-button>
          <el-button type="primary" @click="router.push('/video-study')" size="large">
            <el-icon><VideoPlay /></el-icon>
            视频学习
          </el-button>
        </template>
        <template v-else-if="userStore.isTeacher()">
          <el-button type="primary" @click="router.push('/teacher/homework')" size="large">
            <el-icon><Document /></el-icon>
            作业管理
          </el-button>
          <el-button type="primary" @click="router.push('/teacher/analysis')" size="large">
            <el-icon><DataAnalysis /></el-icon>
            学情分析
          </el-button>
          <el-button type="primary" @click="router.push('/teacher/class-evaluation')" size="large">
            <el-icon><VideoPlay /></el-icon>
            课堂评估
          </el-button>
          <el-button type="primary" @click="router.push('/video-study')" size="large">
            <el-icon><VideoPlay /></el-icon>
            视频学习
          </el-button>
        </template>
        <template v-else-if="userStore.isAdmin()">
          <el-button type="success" @click="router.push('/admin/news')" size="large">
            <el-icon><Document /></el-icon>
            新闻管理
          </el-button>
          <el-button type="primary" @click="refreshNews" size="large">
            <el-icon><Refresh /></el-icon>
            刷新新闻
          </el-button>
        </template>
        <template v-else>
          <el-button type="primary" @click="router.push('/home')" size="large">
            <el-icon><HomeFilled /></el-icon>
            首页
          </el-button>
        </template>
      </div>
    </el-card>

    <el-divider class="section-divider" />

    <!-- 智能学习看板入口 -->
    <el-row :gutter="20" class="mt-20">
      <el-col :span="12">
        <el-card class="dashboard-preview-card" @click="goToDashboard">
          <template #header>
            <div class="card-header">
              <div class="header-title">
                <el-icon class="header-icon"><DataBoard /></el-icon>
                <span>{{ userStore.isStudent() ? '智能学习看板' : '智能教学看板' }}</span>
              </div>
              <el-button link type="primary">
                查看详情
                <el-icon><ArrowRight /></el-icon>
              </el-button>
            </div>
          </template>
          <div class="dashboard-preview">
            <template v-if="userStore.isStudent()">
              <div class="preview-note-item">
                <div class="preview-note-head">
                  <div class="stat-icon blue"><el-icon><Document /></el-icon></div>
                  <div>
                    <div class="preview-note-label">今日任务</div>
                    <div class="preview-note-date">{{ todayLabel }}</div>
                  </div>
                </div>
                <div class="preview-note-content">{{ todayCompletedText }}</div>
              </div>
              <div class="preview-note-item">
                <div class="preview-note-head">
                  <div class="stat-icon green"><el-icon><Clock /></el-icon></div>
                  <div>
                    <div class="preview-note-label">昨日任务</div>
                    <div class="preview-note-date">{{ yesterdayLabel }}</div>
                  </div>
                </div>
                <div class="preview-note-content">{{ yesterdayCompletedText }}</div>
              </div>
              <div class="preview-note-item">
                <div class="preview-note-head">
                  <div class="stat-icon purple"><el-icon><Trophy /></el-icon></div>
                  <div>
                    <div class="preview-note-label">今日 AI</div>
                    <div class="preview-note-date">已使用工具</div>
                  </div>
                </div>
                <div class="preview-note-content">{{ todayAiText }}</div>
              </div>
            </template>
            <template v-else>
              <div class="preview-stat">
                <div class="stat-icon blue"><el-icon><Document /></el-icon></div>
                <div class="stat-info">
                  <div class="stat-value">12</div>
                  <div class="stat-label">待批改作业</div>
                </div>
              </div>
              <div class="preview-stat">
                <div class="stat-icon green"><el-icon><Clock /></el-icon></div>
                <div class="stat-info">
                  <div class="stat-value">24h</div>
                  <div class="stat-label">本周授课</div>
                </div>
              </div>
              <div class="preview-stat">
                <div class="stat-icon purple"><el-icon><Trophy /></el-icon></div>
                <div class="stat-info">
                  <div class="stat-value">92%</div>
                  <div class="stat-label">班级平均分</div>
                </div>
              </div>
            </template>
          </div>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card class="notice-card">
          <template #header>
            <div class="card-header">
              <div class="header-title">
                <el-icon class="header-icon"><Bell /></el-icon>
                <span>通知公告</span>
              </div>
              <el-button link type="primary">查看更多</el-button>
            </div>
          </template>
          <el-timeline>
            <el-timeline-item timestamp="03-15" placement="top" type="primary">
              <div class="notice-item">
                <span class="notice-title">AI 助力教育创新研讨会</span>
                <el-tag size="small" type="warning">热门</el-tag>
              </div>
            </el-timeline-item>
            <el-timeline-item timestamp="03-12" placement="top" type="success">
              <div class="notice-item">
                <span class="notice-title">平台知识库更新完成</span>
              </div>
            </el-timeline-item>
            <el-timeline-item timestamp="03-10" placement="top" type="info">
              <div class="notice-item">
                <span class="notice-title">新学期课程资源已上线</span>
              </div>
            </el-timeline-item>
          </el-timeline>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import { ElMessage } from 'element-plus'
import { getCarouselNews, getListNews } from '@/api/news'
import { getHomeVideos } from '@/api/video'
import { getHomeRecommendList } from '@/api/homeRecommend'
import { getLearningDashboardNote } from '@/api/student'
import {
  Monitor,
  Refresh,
  Grid,
  ChatDotRound,
  Document,
  DataAnalysis,
  VideoPlay,
  HomeFilled,
  DataBoard,
  ArrowRight,
  Trophy,
  Clock,
  Bell,
  ZoomIn,
  View,
  ArrowRight as ArrowRightIcon
} from '@element-plus/icons-vue'

const router = useRouter()
const userStore = useUserStore()

// 新闻接口定义
interface NewsItem {
  id: number
  title: string
  summary: string
  imageUrl: string
  sourceUrl: string
  sourceName: string
  publishTime: string
  isTop: number
  isManual: number
}

// 新闻数据
const carouselNews = ref<NewsItem[]>([])
const listNews = ref<NewsItem[]>([])
const loading = ref(false)

interface LearningNotePreview {
  date?: string
  completedSummary?: string
  pendingSummary?: string
  aiToolSummary?: string
  hasContent?: boolean
}

const todayNote = ref<LearningNotePreview | null>(null)
const yesterdayNote = ref<LearningNotePreview | null>(null)

// 推荐视频接口定义
interface VideoItem {
  id: number
  title: string
  coverUrl: string
  videoUrl: string
  description: string
  viewCount: number
  collectionCount: number
  collected: boolean
}

// 视频数据
const homeVideos = ref<VideoItem[]>([])
const featuredVideo = ref<VideoItem | null>(null)
const gridVideos = ref<VideoItem[]>([])
const videoLoading = ref(false)

const formatShortDate = (date: Date) =>
  `${date.getMonth() + 1}月${date.getDate()}日`

const formatDateParam = (date: Date) => {
  const year = date.getFullYear()
  const month = `${date.getMonth() + 1}`.padStart(2, '0')
  const day = `${date.getDate()}`.padStart(2, '0')
  return `${year}-${month}-${day}`
}

const today = computed(() => new Date())
const yesterday = computed(() => {
  const date = new Date()
  date.setDate(date.getDate() - 1)
  return date
})

const todayLabel = computed(() => formatShortDate(today.value))
const yesterdayLabel = computed(() => formatShortDate(yesterday.value))

const normalizePreviewText = (value?: string) => {
  if (!value || !value.trim()) return '无'
  return value.trim()
}

const todayCompletedText = computed(() => normalizePreviewText(todayNote.value?.completedSummary))
const yesterdayCompletedText = computed(() => normalizePreviewText(yesterdayNote.value?.completedSummary))
const todayAiText = computed(() => normalizePreviewText(todayNote.value?.aiToolSummary))

const loadDashboardPreview = async () => {
  if (!userStore.isStudent() || !userStore.userInfo?.id) return

  try {
    const userId = userStore.userInfo.id
    const [todayRes, yesterdayRes] = await Promise.all([
      getLearningDashboardNote(userId, formatDateParam(today.value)),
      getLearningDashboardNote(userId, formatDateParam(yesterday.value))
    ])

    todayNote.value = todayRes.code === 200 ? todayRes.data : null
    yesterdayNote.value = yesterdayRes.code === 200 ? yesterdayRes.data : null
  } catch (error) {
    console.error('加载学习看板预览失败:', error)
    todayNote.value = null
    yesterdayNote.value = null
  }
}

// 获取默认图片（用于备用）
const getDefaultImage = (id: number): string => {
  const images = [
    'https://images.unsplash.com/photo-1677442136019-21780ecad995?w=800&h=400&fit=crop',
    'https://images.unsplash.com/photo-1555617778-02518510b9fa?w=800&h=400&fit=crop',
    'https://images.unsplash.com/photo-1552664730-d307ca884978?w=800&h=400&fit=crop',
    'https://images.unsplash.com/photo-1512941937669-90a1b58e7e9c?w=800&h=400&fit=crop'
  ]
  return images[(id - 1) % images.length]
}

// 图片加载失败时的备用渐变色
const fallbackGradients = [
  'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
  'linear-gradient(135deg, #f093fb 0%, #f5576c 100%)',
  'linear-gradient(135deg, #4facfe 0%, #00f2fe 100%)',
  'linear-gradient(135deg, #43e97b 0%, #38f9d7 100%)',
  'linear-gradient(135deg, #fa709a 0%, #fee140 100%)',
  'linear-gradient(135deg, #a8edea 0%, #fed6e3 100%)'
]

// 格式化时间
const formatTime = (time: string): string => {
  if (!time) return ''
  const now = new Date()
  const newsTime = new Date(time)
  const diff = now.getTime() - newsTime.getTime()
  const hours = Math.floor(diff / (1000 * 60 * 60))
  const days = Math.floor(hours / 24)

  if (hours < 1) return '刚刚'
  if (hours < 24) return `${hours}小时前`
  if (days < 3) return `${days}天前`
  return `${newsTime.getMonth() + 1}月${newsTime.getDate()}日`
}

// 处理新闻点击
const handleNewsClick = (news: NewsItem) => {
  if (news.sourceUrl && news.sourceUrl !== '#') {
    window.open(news.sourceUrl, '_blank')
  } else {
    console.log('查看新闻详情:', news.title)
    // TODO: 跳转到新闻详情页
  }
}

// 刷新新闻（从 API 加载）
const refreshNews = async () => {
  loading.value = true
  try {
    // 清空数据
    carouselNews.value = []
    listNews.value = []

    // 并行请求
    const [carouselRes, listRes] = await Promise.all([
      getCarouselNews(),
      getListNews(10)
    ])

    // 检查响应
    if (carouselRes.code === 200) {
      carouselNews.value = carouselRes.data || []
    }
    if (listRes.code === 200) {
      listNews.value = listRes.data || []
    }

    // 不再使用 Mock 数据（图片可能被墙）
    // 如果 API 返回空数据，显示空状态
  } catch (error) {
    console.error('加载新闻失败:', error)
    ElMessage.warning('新闻加载失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

// 图片加载失败处理
const handleImageError = (event: Event, newsId: number) => {
  const img = event.target as HTMLImageElement
  const gradient = fallbackGradients[(newsId - 1) % fallbackGradients.length]

  // 隐藏图片
  img.style.display = 'none'

  // 设置父容器背景为渐变色
  const wrapper = img.closest('.carousel-image-wrapper')
  if (wrapper) {
    wrapper.style.background = gradient
  }

  console.log(`图片加载失败，使用渐变色背景：${newsId}`)
}

// 获取图片完整 URL（添加 /api 前缀）
const getImageUrl = (url: string): string => {
  if (!url) return ''
  // 如果是完整 URL（http 开头），直接返回
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }
  // 否则添加 /api 前缀
  return '/api' + url
}

// 跳转看板
const goToDashboard = () => {
  if (userStore.isStudent()) {
    router.push('/student/dashboard')
  } else if (userStore.isTeacher() || userStore.isAdmin()) {
    router.push('/teacher/homework')
  }
}

// 加载首页视频（优先使用推荐配置，1个精选 + 4个网格）
const loadHomeVideos = async () => {
  videoLoading.value = true
  try {
    // 优先使用推荐接口
    const recommendRes = await getHomeRecommendList()
    if (recommendRes.code === 200 && recommendRes.data && recommendRes.data.length > 0) {
      // 分离轮播和网格
      const carouselVideos = recommendRes.data.filter((v: any) => v.positionType === 1)
      const gridVideosList = recommendRes.data.filter((v: any) => v.positionType === 2)

      // 设置轮播（取第一个）
      featuredVideo.value = carouselVideos[0] || null
      // 设置网格（最多4个）
      gridVideos.value = gridVideosList.slice(0, 4)
    } else {
      // 推荐表无数据，fallback 到原有接口
      const res = await getHomeVideos()
      if (res.code === 200 && res.data) {
        const videos = res.data.slice(0, 5)
        featuredVideo.value = videos[0] || null
        gridVideos.value = videos.slice(1, 5)
      } else {
        featuredVideo.value = null
        gridVideos.value = []
      }
    }
  } catch (error) {
    console.error('加载视频失败:', error)
    // 出错时尝试 fallback
    try {
      const res = await getHomeVideos()
      if (res.code === 200 && res.data) {
        const videos = res.data.slice(0, 5)
        featuredVideo.value = videos[0] || null
        gridVideos.value = videos.slice(1, 5)
      }
    } catch {
      featuredVideo.value = null
      gridVideos.value = []
    }
  } finally {
    videoLoading.value = false
  }
}

// 打开视频
const openVideo = (video: VideoItem) => {
  if (video.videoUrl) {
    window.open(video.videoUrl, '_blank', 'noopener,noreferrer')
  }
}

// 获取视频封面完整 URL
const getVideoCoverUrl = (url: string): string => {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }
  return '/api' + url
}

// 封面加载失败处理
const handleCoverError = (event: Event) => {
  const img = event.target as HTMLImageElement
  img.style.display = 'none'
  const parent = img.parentElement
  if (parent) {
    parent.style.background = 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
    parent.innerHTML = '<div class="cover-error-icon"><svg viewBox="0 0 1024 1024" width="48" height="48" fill="rgba(255,255,255,0.5)"><path d="M512 64C264.6 64 64 264.6 64 512s200.6 448 448 448 448-200.6 448-448S759.4 64 512 64zm0 820c-205.4 0-372-166.6-372-372s166.6-372 372-372 372 166.6 372 372-166.6 372-372 372z"/><path d="M464 336a48 48 0 1 0 96 0 48 48 0 1 0-96 0zm72 112h-48c-4.4 0-8 3.6-8 8v272c0 4.4 3.6 8 8 8h48c4.4 0 8-3.6 8-8V456c0-4.4-3.6-8-8-8z"/></svg></div>'
  }
}

// 格式化数字
const formatCount = (count: number): string => {
  if (!count) return '0'
  if (count >= 10000) return (count / 10000).toFixed(1) + 'w'
  if (count >= 1000) return (count / 1000).toFixed(1) + 'k'
  return count.toString()
}

// Mock 数据作为备用
const mockCarouselNews: NewsItem[] = [
  {
    id: 1,
    title: 'OpenAI 发布 GPT-5：多模态能力全面升级，支持实时视频理解',
    summary: 'OpenAI 今日正式发布 GPT-5，新增实时视频理解、代码自动调试等功能，推理速度提升 3 倍。',
    imageUrl: 'https://images.unsplash.com/photo-1677442136019-21780ecad995?w=800&h=400&fit=crop',
    sourceUrl: '#',
    sourceName: 'OpenAI 官方博客',
    publishTime: '2026-03-15 10:30:00',
    isTop: 1,
    isManual: 1
  },
  {
    id: 2,
    title: '英伟达发布新一代 AI 芯片 Blackwell B300：性能提升 10 倍',
    summary: 'NVIDIA Blackwell B300 采用 3nm 工艺，支持 10TB/s 内存带宽，专为大模型训练设计。',
    imageUrl: 'https://images.unsplash.com/photo-1555617778-02518510b9fa?w=800&h=400&fit=crop',
    sourceUrl: '#',
    sourceName: 'NVIDIA',
    publishTime: '2026-03-14 16:20:00',
    isTop: 0,
    isManual: 1
  },
  {
    id: 3,
    title: '智谱 AI 完成 5 亿美元融资，估值突破 30 亿美元',
    summary: '智谱 AI 宣布完成由红杉资本领投的 5 亿美元 D 轮融资，将加速 GLM 大模型研发及商业化落地。',
    imageUrl: 'https://images.unsplash.com/photo-1552664730-d307ca884978?w=800&h=400&fit=crop',
    sourceUrl: '#',
    sourceName: '智谱 AI',
    publishTime: '2026-03-13 09:15:00',
    isTop: 0,
    isManual: 1
  },
  {
    id: 4,
    title: '苹果 WWDC 2026：iOS 18 深度集成 AI，Siri 全面进化',
    summary: '苹果发布 iOS 18，Siri 支持自然语言多轮对话，可自动完成复杂任务，并开放更多系统 API。',
    imageUrl: 'https://images.unsplash.com/photo-1512941937669-90a1b58e7e9c?w=800&h=400&fit=crop',
    sourceUrl: '#',
    sourceName: 'Apple',
    publishTime: '2026-03-12 02:00:00',
    isTop: 0,
    isManual: 0
  }
]

// 列表新闻 Mock 数据
const mockListNews: NewsItem[] = [
  { id: 11, title: '清华发布开源大模型 GLM-Edge，推理速度超越 GPT-4', summary: '', imageUrl: '', sourceUrl: '#', sourceName: '清华大学', publishTime: '2026-03-15 14:30:00', isTop: 1, isManual: 1 },
  { id: 12, title: '谷歌 DeepMind 突破：AI 首次独立发现数学定理', summary: '', imageUrl: '', sourceUrl: '#', sourceName: 'DeepMind', publishTime: '2026-03-15 11:20:00', isTop: 0, isManual: 0 },
  { id: 13, title: '字节豆包大模型日活突破 5000 万，成国内第一', summary: '', imageUrl: '', sourceUrl: '#', sourceName: '字节跳动', publishTime: '2026-03-15 09:45:00', isTop: 0, isManual: 0 },
  { id: 14, title: '华为盘古大模型 5.0 发布：支持 100+ 语言', summary: '', imageUrl: '', sourceUrl: '#', sourceName: '华为', publishTime: '2026-03-14 20:00:00', isTop: 0, isManual: 0 },
  { id: 15, title: '特斯拉 Optimus 机器人量产，售价 2 万美元', summary: '', imageUrl: '', sourceUrl: '#', sourceName: 'Tesla', publishTime: '2026-03-14 18:30:00', isTop: 0, isManual: 0 },
  { id: 16, title: '阿里云通义千问开源 Qwen-72B，性能领跑', summary: '', imageUrl: '', sourceUrl: '#', sourceName: '阿里云', publishTime: '2026-03-14 15:00:00', isTop: 0, isManual: 0 },
  { id: 17, title: '百度文心一言 5.0 上线，支持 1M 超长上下文', summary: '', imageUrl: '', sourceUrl: '#', sourceName: '百度', publishTime: '2026-03-14 10:00:00', isTop: 0, isManual: 0 },
  { id: 18, title: 'Meta 开源 Llama-4，商业化限制全面解除', summary: '', imageUrl: '', sourceUrl: '#', sourceName: 'Meta', publishTime: '2026-03-13 22:00:00', isTop: 0, isManual: 0 },
  { id: 19, title: '小米发布自研 Vela 操作系统，打通 AIoT 生态', summary: '', imageUrl: '', sourceUrl: '#', sourceName: '小米', publishTime: '2026-03-13 16:00:00', isTop: 0, isManual: 0 },
  { id: 20, title: '商汤日日新 5.0 发布，文生图质量大幅提升', summary: '', imageUrl: '', sourceUrl: '#', sourceName: '商汤科技', publishTime: '2026-03-13 11:00:00', isTop: 0, isManual: 0 }
]

// 初始化加载
onMounted(() => {
  refreshNews()
  loadHomeVideos()
  loadDashboardPreview()
})
</script>

<style scoped lang="scss">
.home-container {
  padding: 0;
  max-width: 1400px;
  margin: 0 auto;
}

/* ============ 视频学习 - B站像素级复刻 ============ */
.video-section {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;

  // 紧凑标题栏 - B站风格
  .video-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;

    .header-title {
      display: flex;
      align-items: center;
      gap: 6px;

      .title-icon {
        font-size: 18px;
        color: #00a1d6;
      }

      h2 {
        font-size: 18px;
        font-weight: 600;
        color: #18191c;
        margin: 0;
      }

      .subtitle {
        font-size: 12px;
        color: #9499a0;
        margin-left: 8px;
      }
    }

    .more-btn {
      font-size: 12px;
      color: #9499a0;

      &:hover {
        color: #00a1d6;
      }
    }
  }

  // B站风格栅格行 - 强制等高
  .video-row {
    display: flex;
    align-items: stretch;
  }

  // 左侧精选大图 (span=11)
  .video-left {
    display: flex;
    height: 100%;
  }

  .featured-video {
    width: 100%;
    height: 100%;
    cursor: pointer;
    border-radius: 6px;
    overflow: hidden;
    position: relative;

    &:hover {
      .cover-image {
        transform: scale(1.03);
      }

      .cover-overlay {
        opacity: 1;
      }
    }

    .featured-cover {
      position: relative;
      width: 100%;
      height: 100%;
      min-height: 450px;

      .cover-image {
        width: 100%;
        height: 100%;
        object-fit: cover;
        transition: transform 0.3s ease;
      }

      .cover-overlay {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.3);
        display: flex;
        align-items: center;
        justify-content: center;
        opacity: 0;
        transition: opacity 0.2s ease;

        .play-btn {
          width: 50px;
          height: 50px;
          border-radius: 50%;
          background: rgba(255, 255, 255, 0.9);
          display: flex;
          align-items: center;
          justify-content: center;

          .el-icon {
            font-size: 24px;
            color: #00a1d6;
            margin-left: 3px;
          }
        }
      }

      // B站风格底部标题栏
      .featured-title-bar {
        position: absolute;
        bottom: 0;
        left: 0;
        right: 0;
        padding: 32px 10px 8px;
        background: linear-gradient(transparent, rgba(0, 0, 0, 0.7));
        display: flex;
        justify-content: space-between;
        align-items: flex-end;

        .featured-title {
          flex: 1;
          font-size: 14px;
          font-weight: 500;
          color: #fff;
          margin: 0;
          line-height: 1.4;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }

        .featured-stats {
          display: flex;
          align-items: center;
          gap: 3px;
          font-size: 12px;
          color: rgba(255, 255, 255, 0.8);
          margin-left: 8px;
          flex-shrink: 0;
        }
      }
    }
  }

  .featured-placeholder {
    width: 100%;
    min-height: 450px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f4f5f7;
    border-radius: 6px;
  }

  // 右侧 2x2 网格 (span=13)
  .video-right {
    display: flex;
    height: 100%;
  }

  .video-grid {
    display: grid;
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    width: 100%;
    height: 100%;

    .video-card {
      cursor: pointer;
      border-radius: 6px;
      overflow: hidden;
      background: #fff;
      display: flex;
      flex-direction: column;
      // 扁平化 - 无边框无阴影

      &:hover {
        .cover-image {
          transform: scale(1.03);
        }

        .card-title {
          color: #00a1d6;
        }
      }

      // 占位卡片样式
      &.placeholder {
        cursor: default;
        background: #f4f5f7;

        &:hover {
          .cover-image {
            transform: none;
          }
        }

        .placeholder-content {
          width: 100%;
          height: 100%;
          display: flex;
          align-items: center;
          justify-content: center;
          color: #c9ccd0;

          .el-icon {
            font-size: 28px;
          }
        }

        .placeholder-text {
          color: #9499a0;
        }
      }

      .card-cover {
        position: relative;
        width: 100%;
        aspect-ratio: 16 / 9;
        overflow: hidden;
        background: #e3e5e7;

        .cover-image {
          width: 100%;
          height: 100%;
          object-fit: cover;
          transition: transform 0.3s ease;
        }

        // B站风格播放量遮罩 - 左下角
        .card-stats-overlay {
          position: absolute;
          bottom: 6px;
          left: 6px;
          display: flex;
          align-items: center;
          gap: 3px;
          padding: 2px 6px;
          background: rgba(0, 0, 0, 0.6);
          border-radius: 3px;
          font-size: 11px;
          color: #fff;

          .stats-icon {
            font-size: 12px;
          }

          .stats-text {
            line-height: 1;
          }
        }
      }

      .card-info {
        padding: 8px;
        background: #fff;

        .card-title {
          font-size: 13px;
          font-weight: 400;
          color: #18191c;
          line-height: 1.4;
          margin: 0;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
          transition: color 0.2s ease;
        }
      }
    }
  }

  .no-videos {
    min-height: 450px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: #f4f5f7;
    border-radius: 6px;
  }
}

/* ============ 科技资讯大屏 ============ */
.tech-news-section {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  padding: 24px;
  margin: 20px;
  box-shadow: 0 8px 32px rgba(102, 126, 234, 0.3);

  .section-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;

    .header-left {
      display: flex;
      align-items: baseline;
      gap: 12px;

      h2 {
        font-size: 24px;
        font-weight: 700;
        color: #fff;
        margin: 0;
      }

      .subtitle {
        font-size: 14px;
        color: rgba(255, 255, 255, 0.8);
        margin-left: 8px;
      }

      .header-icon {
        font-size: 28px;
        color: #fff;
      }
    }

    .header-right {
      display: flex;
      gap: 8px;

      .el-tag {
        background: rgba(255, 255, 255, 0.15);
        border-color: rgba(255, 255, 255, 0.3);
        color: #fff;
      }
    }
  }

  .news-content {
    display: grid;
    grid-template-columns: 70% 30%;
    gap: 20px;
    min-height: 480px;

    @media (max-width: 992px) {
      grid-template-columns: 1fr;
      min-height: auto;
    }
  }

  /* 左侧轮播图 */
  .carousel-wrapper {
    background: #fff;
    border-radius: 12px;
    overflow: hidden;

    .carousel-item {
      height: 100%;
      display: flex;
      flex-direction: column;
      cursor: pointer;

      .carousel-image-wrapper {
        position: relative;
        height: 260px;
        overflow: hidden;
        background: #f5f5f5;

        .carousel-image {
          width: 100%;
          height: 100%;
          object-fit: cover;
          transition: transform 0.5s ease;
        }

        &:hover .carousel-image {
          transform: scale(1.05);
        }

        .image-overlay {
          position: absolute;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          background: rgba(0, 0, 0, 0.3);
          display: flex;
          align-items: center;
          justify-content: center;
          opacity: 0;
          transition: opacity 0.3s ease;

          .overlay-icon {
            font-size: 48px;
            color: #fff;
          }
        }

        &:hover .image-overlay {
          opacity: 1;
        }
      }

      .carousel-info {
        flex: 1;
        padding: 16px 20px;
        display: flex;
        flex-direction: column;

        .carousel-source {
          font-size: 12px;
          color: #667eea;
          font-weight: 600;
          margin-bottom: 8px;
        }

        .carousel-title {
          font-size: 18px;
          font-weight: 600;
          color: #1a1a1a;
          margin-bottom: 8px;
          line-height: 1.4;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
        }

        .carousel-summary {
          font-size: 14px;
          color: #666;
          line-height: 1.6;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
          overflow: hidden;
          margin-bottom: 12px;
        }

        .carousel-meta {
          margin-top: auto;
          display: flex;
          align-items: center;
          gap: 8px;

          .publish-time {
            font-size: 12px;
            color: #999;
          }
        }
      }
    }

    .empty-carousel {
      height: 420px;
      display: flex;
      align-items: center;
      justify-content: center;
    }
  }

  /* 右侧新闻列表 */
  .news-list-wrapper {
    background: #fff;
    border-radius: 12px;
    padding: 16px;
    display: flex;
    flex-direction: column;

    .list-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;
      padding-bottom: 12px;
      border-bottom: 1px solid #eee;

      h3 {
        font-size: 16px;
        font-weight: 600;
        color: #1a1a1a;
        margin: 0;
      }
    }

    .news-list {
      flex: 1;
      overflow-y: auto;
      max-height: 420px;

      .news-list-item {
        display: flex;
        gap: 12px;
        padding: 12px 8px;
        border-radius: 8px;
        cursor: pointer;
        transition: background 0.2s ease;

        &:hover {
          background: #f5f7fa;
        }

        .item-index {
          width: 24px;
          height: 24px;
          display: flex;
          align-items: center;
          justify-content: center;
          font-size: 14px;
          font-weight: 600;
          color: #999;
          flex-shrink: 0;

          &.top-3 {
            color: #f56c6c;
            font-size: 16px;
            font-weight: 700;
          }
        }

        .item-content {
          flex: 1;
          min-width: 0;

          .item-title {
            font-size: 14px;
            color: #303133;
            line-height: 1.5;
            display: -webkit-box;
            -webkit-line-clamp: 2;
            -webkit-box-orient: vertical;
            overflow: hidden;
            margin-bottom: 6px;
          }

          .item-meta {
            display: flex;
            gap: 12px;
            font-size: 12px;
            color: #909399;

            .item-source {
              color: #667eea;
            }
          }
        }
      }

      .empty-list {
        display: flex;
        align-items: center;
        justify-content: center;
        height: 200px;
      }
    }
  }
}

.section-divider {
  margin: 20px 0;
}

/* ============ 快捷入口 ============ */
.quick-access-card {
  margin: 0 20px;
  border-radius: 12px;

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 600;
      font-size: 16px;
      color: #1a1a1a;

      .header-icon {
        font-size: 20px;
        color: #667eea;
      }
    }
  }

  .quick-actions {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 16px;
    padding: 8px 0;

    @media (max-width: 768px) {
      grid-template-columns: repeat(2, 1fr);
    }

    .el-button {
      height: 64px;
      font-size: 15px;
      display: flex;
      flex-direction: column;
      gap: 8px;
      border-radius: 12px;
      transition: all 0.3s ease;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
      }

      .el-icon {
        font-size: 24px;
      }
    }
  }
}

/* ============ 看板预览 ============ */
.mt-20 {
  margin-top: 20px;
  padding: 0 20px;
}

.dashboard-preview-card,
.notice-card {
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s ease;
  background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
  border: 1px solid rgba(102, 126, 234, 0.1);
  overflow: hidden;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(102, 126, 234, 0.2);
  }

  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 20px;
    background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
    border-bottom: 1px solid rgba(102, 126, 234, 0.1);

    .header-title {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: 600;
      font-size: 16px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;

      .header-icon {
        font-size: 20px;
        color: #667eea;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
      }
    }

    .el-button {
      transition: all 0.3s ease;
      color: #667eea;
      font-weight: 500;
      font-size: 14px;

      &:hover {
        color: #764ba2;
        transform: translateX(4px);
      }
    }
  }
}

.dashboard-preview {
  padding: 16px 20px;

  .preview-note-item {
    border: 1px solid rgba(102, 126, 234, 0.1);
    border-radius: 10px;
    background: linear-gradient(135deg, #fcfcfd 0%, #f8f9fa 100%);
    padding: 14px 16px 12px;
    transition: all 0.3s ease;
    position: relative;
    overflow: hidden;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      width: 4px;
      height: 100%;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 6px 16px rgba(102, 126, 234, 0.1);
    }
  }

  .preview-note-item + .preview-note-item {
    margin-top: 12px;
  }

  .preview-note-head {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 10px;
  }

  .stat-icon {
    width: 36px;
    height: 36px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 18px;
    color: #fff;
    box-shadow: 0 3px 10px rgba(0, 0, 0, 0.15);
    transition: all 0.3s ease;

    &:hover {
      transform: scale(1.05);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
    }

    &.blue {
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    }

    &.green {
      background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
    }

    &.purple {
      background: linear-gradient(135deg, #8e44ad 0%, #c0392b 100%);
    }
  }

  .preview-note-label {
    font-size: 14px;
    font-weight: 600;
    color: #1a1a1a;
  }

  .preview-note-date {
    margin-top: 2px;
    font-size: 12px;
    color: #7b8190;
    font-weight: 500;
  }

  .preview-note-content {
    font-size: 13px;
    line-height: 1.6;
    color: #374151;
    min-height: 44px;
    white-space: pre-wrap;
    word-break: break-word;
    padding-left: 48px;
  }

  .preview-stat {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 12px 0;
    border-bottom: 1px solid rgba(102, 126, 234, 0.05);

    &:last-child {
      border-bottom: none;
    }

    .stat-icon {
      width: 40px;
      height: 40px;
      font-size: 20px;
    }

    .stat-info {
      flex: 1;

      .stat-value {
        font-size: 22px;
        font-weight: 700;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        background-clip: text;
        margin-bottom: 4px;
      }

      .stat-label {
        font-size: 13px;
        color: #7b8190;
        font-weight: 500;
      }
    }
  }
}

.notice-card {
  .el-timeline {
    padding: 0 20px 16px;
  }
}

.notice-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: linear-gradient(135deg, #fcfcfd 0%, #f8f9fa 100%);
  border-radius: 8px;
  transition: all 0.3s ease;
  margin-bottom: 10px;

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(102, 126, 234, 0.1);
  }

  .notice-title {
    font-size: 13px;
    font-weight: 500;
    color: #1a1a1a;
    flex: 1;
  }
}
</style>
