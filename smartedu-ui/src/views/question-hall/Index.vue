<template>
  <div class="question-hall">
    <!-- 顶部操作栏 -->
    <div class="header-actions">
      <div class="search-box">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索帖子..."
          clearable
          size="large"
          @keyup.enter="handleSearch"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <el-button type="primary" size="large" @click="handleSearch">
          <el-icon><Search /></el-icon>
          搜索
        </el-button>
      </div>

      <div class="filter-actions">
        <el-select v-model="filterCategory" placeholder="全部分类" clearable size="large" @change="handleFilter">
          <el-option label="全部" value="" />
          <el-option label="提问" value="QUESTION" />
          <el-option label="讨论" value="DISCUSSION" />
          <el-option label="分享" value="SHARE" />
        </el-select>

        <el-button type="primary" size="large" @click="showCreateDialog = true">
          <el-icon><Plus /></el-icon>
          发布帖子
        </el-button>
      </div>
    </div>

    <!-- 帖子列表 -->
    <div class="topic-list-container" v-loading="loading">
      <div v-if="topicList.length === 0 && !loading" class="empty-state">
        <el-empty description="暂无帖子，快来发第一个吧！">
          <el-button type="primary" @click="showCreateDialog = true">发布第一个帖子</el-button>
        </el-empty>
      </div>

      <div class="topic-list">
        <div
          v-for="topic in topicList"
          :key="topic.id"
          class="topic-card"
          @click="goToDetail(topic.id)"
        >
          <!-- 分类标签区 -->
          <div class="card-tags">
            <span class="tag tag-pinned" v-if="topic.status === 2">
              <el-icon><Top /></el-icon> 置顶
            </span>
            <span :class="['tag', `tag-${topic.category.toLowerCase()}`]">
              {{ getCategoryLabel(topic.category) }}
            </span>
            <span class="tag tag-solved" v-if="topic.isSolved === 1">
              <el-icon><CircleCheck /></el-icon> 已解决
            </span>
          </div>

          <!-- 标题 -->
          <h3 class="card-title">{{ topic.title }}</h3>

          <!-- 内容预览 -->
          <p class="card-preview">{{ topic.contentPreview }}</p>

          <!-- 底部信息 -->
          <div class="card-footer">
            <div class="author-info">
              <el-avatar :size="28" :src="topic.userAvatar || undefined" class="author-avatar">
                {{ topic.userName?.charAt(0) }}
              </el-avatar>
              <span class="author-name">{{ topic.userName }}</span>
              <span class="author-badge teacher" v-if="topic.userRole === 'TEACHER'">
                <el-icon><Medal /></el-icon> 老师
              </span>
              <span class="post-time">
                <el-icon><Clock /></el-icon>
                {{ formatTime(topic.createdAt) }}
              </span>
            </div>

            <div class="card-stats">
              <span class="stat-item">
                <el-icon><View /></el-icon>
                <span>{{ topic.viewCount }}</span>
              </span>
              <span class="stat-item">
                <el-icon><ChatDotRound /></el-icon>
                <span>{{ topic.replyCount }}</span>
              </span>
              <span
                class="stat-item like-btn"
                :class="{ liked: topic.isLiked }"
                @click.stop="handleLikeTopic(topic)"
              >
                <el-icon><Star /></el-icon>
                <span>{{ topic.likeCount }}</span>
              </span>
              <!-- 管理员删除按钮 -->
              <span
                v-if="isAdmin"
                class="stat-item admin-delete-btn"
                @click="handleAdminDelete(topic, $event)"
              >
                <el-icon><Delete /></el-icon>
                <span>删除</span>
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > 0">
      <el-pagination
        v-model:current-page="pageNum"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        background
        @size-change="loadTopics"
        @current-change="loadTopics"
      />
    </div>

    <!-- 发帖对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      title="发布新帖"
      width="640px"
      destroy-on-close
      class="create-dialog"
    >
      <el-form ref="createFormRef" :model="createForm" :rules="createRules" label-width="80px" label-position="top">
        <el-form-item label="分类" prop="category">
          <el-radio-group v-model="createForm.category" class="category-radio">
            <el-radio value="QUESTION" border>
              <el-icon><QuestionFilled /></el-icon>
              提问
            </el-radio>
            <el-radio value="DISCUSSION" border>
              <el-icon><ChatLineRound /></el-icon>
              讨论
            </el-radio>
            <el-radio value="SHARE" border>
              <el-icon><Share /></el-icon>
              分享
            </el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="标题" prop="title">
          <el-input
            v-model="createForm.title"
            placeholder="请输入标题（5-100字）"
            maxlength="100"
            show-word-limit
            size="large"
          />
        </el-form-item>

        <el-form-item label="内容" prop="content">
          <el-input
            v-model="createForm.content"
            type="textarea"
            placeholder="请详细描述你的问题或想法..."
            :rows="8"
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>

        <el-form-item label="附件">
          <el-upload
            v-model:file-list="fileList"
            :limit="1"
            :auto-upload="false"
            drag
          >
            <el-icon class="el-icon--upload"><Upload /></el-icon>
            <div class="el-upload__text">
              拖拽文件到此处或 <em>点击上传</em>
            </div>
            <template #tip>
              <div class="el-upload__tip">支持上传图片、文档等，最大 10MB</div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>

      <template #footer>
        <div class="dialog-footer">
          <el-button size="large" @click="showCreateDialog = false">取消</el-button>
          <el-button type="primary" size="large" @click="handleCreateTopic" :loading="submitting">
            <el-icon v-if="!submitting"><Promotion /></el-icon>
            {{ submitting ? '发布中...' : '发布帖子' }}
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, View, ChatDotRound, Star, Upload, Top, CircleCheck, Medal, Clock, QuestionFilled, ChatLineRound, Share, Promotion, Delete } from '@element-plus/icons-vue'
import {
  getTopicList,
  createTopic,
  likeTopic,
  adminDeleteTopic,
  type TopicListItem,
  type TopicCategory
} from '@/api/questionHall'
import { useUserStore } from '@/store/modules/user'

const router = useRouter()
const userStore = useUserStore()

// 管理员权限判断
const isAdmin = computed(() => userStore.isAdmin())

// 状态
const loading = ref(false)
const submitting = ref(false)
const topicList = ref<TopicListItem[]>([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

// 搜索和筛选
const searchKeyword = ref('')
const filterCategory = ref<TopicCategory | ''>('')

// 发帖表单
const showCreateDialog = ref(false)
const createFormRef = ref()
const fileList = ref<any[]>([])
const createForm = reactive({
  title: '',
  content: '',
  category: 'QUESTION' as TopicCategory
})

const createRules = {
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 5, max: 100, message: '标题长度 5-100 字', trigger: 'blur' }
  ],
  content: [
    { required: true, message: '请输入内容', trigger: 'blur' },
    { min: 10, max: 5000, message: '内容长度 10-5000 字', trigger: 'blur' }
  ]
}

// 加载帖子列表
const loadTopics = async () => {
  loading.value = true
  try {
    const res = await getTopicList({
      pageNum: pageNum.value,
      pageSize: pageSize.value,
      keyword: searchKeyword.value || undefined,
      category: filterCategory.value || undefined
    })
    console.log('[Index.vue] API response:', res)
    if (res.code === 200 && res.data) {
      topicList.value = res.data.list
      total.value = res.data.total
      // 调试：检查第一个帖子的 ID
      if (res.data.list.length > 0) {
        console.log('[Index.vue] First topic:', res.data.list[0], 'ID type:', typeof res.data.list[0].id)
      }
    }
  } catch (error) {
    console.error('加载帖子列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pageNum.value = 1
  loadTopics()
}

// 筛选
const handleFilter = () => {
  pageNum.value = 1
  loadTopics()
}

// 跳转详情
const goToDetail = (id: number | undefined) => {
  // 防御性检查：验证 id 是否有效
  if (!id || isNaN(id) || id <= 0) {
    console.error('[Index.vue] Invalid topic ID:', id)
    ElMessage.warning('帖子ID无效')
    return
  }
  console.log('[Index.vue] Navigating to topic detail, ID:', id)
  router.push(`/question-hall/${id}`)
}

// 点赞帖子
const handleLikeTopic = async (topic: TopicListItem) => {
  try {
    const res = await likeTopic(topic.id)
    if (res.code === 200 && res.data) {
      topic.isLiked = res.data.isLiked
      topic.likeCount += res.data.isLiked ? 1 : -1
    }
  } catch (error) {
    console.error('点赞失败:', error)
  }
}

// 管理员删除帖子
const handleAdminDelete = async (topic: TopicListItem, event: Event) => {
  // 阻止事件冒泡，避免触发卡片点击
  event.stopPropagation()

  try {
    await ElMessageBox.confirm(
      `确定要删除帖子「${topic.title}」吗？此操作不可恢复。`,
      '管理员删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )

    const res = await adminDeleteTopic(topic.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      // 刷新列表
      loadTopics()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

// 创建帖子
const handleCreateTopic = async () => {
  const valid = await createFormRef.value?.validate()
  if (!valid) return

  submitting.value = true
  try {
    const res = await createTopic({
      title: createForm.title,
      content: createForm.content,
      category: createForm.category,
      file: fileList.value[0]?.raw
    })

    if (res.code === 200) {
      ElMessage.success('发布成功')
      showCreateDialog.value = false
      // 重置表单
      createForm.title = ''
      createForm.content = ''
      createForm.category = 'QUESTION'
      fileList.value = []
      // 刷新列表
      loadTopics()
    } else {
      ElMessage.error(res.message || '发布失败')
    }
  } catch (error) {
    console.error('发布失败:', error)
    ElMessage.error('发布失败')
  } finally {
    submitting.value = false
  }
}

// 获取分类标签类型
const getCategoryType = (category: TopicCategory): string => {
  const types: Record<TopicCategory, string> = {
    QUESTION: 'primary',
    DISCUSSION: 'warning',
    SHARE: 'success'
  }
  return types[category] || 'info'
}

// 获取分类标签文字
const getCategoryLabel = (category: TopicCategory): string => {
  const labels: Record<TopicCategory, string> = {
    QUESTION: '提问',
    DISCUSSION: '讨论',
    SHARE: '分享'
  }
  return labels[category] || category
}

// 格式化时间
const formatTime = (time: string): string => {
  const date = new Date(time)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)} 天前`

  return date.toLocaleDateString()
}

onMounted(() => {
  loadTopics()
})
</script>

<style scoped lang="scss">
// ==================== 变量定义 ====================
$bg-color: #f6f8fa;
$card-bg: #ffffff;
$text-primary: #24292f;
$text-secondary: #57606a;
$text-muted: #8c959f;
$border-color: #d0d7de;
$primary-color: #409eff;
$success-color: #52c41a;
$warning-color: #faad14;
$danger-color: #ff4d4f;
$question-color: #5c6bc0;
$discussion-color: #ff9800;
$share-color: #26a69a;

// ==================== 主容器 ====================
.question-hall {
  padding: 24px;
  background: $bg-color;
  min-height: calc(100vh - 60px);
}

// ==================== 顶部操作栏 ====================
.header-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 16px 24px;
  background: $card-bg;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba($border-color, 0.5);

  .search-box {
    display: flex;
    gap: 12px;

    .el-input {
      width: 360px;
    }
  }

  .filter-actions {
    display: flex;
    gap: 12px;
  }
}

// ==================== 帖子列表容器 ====================
.topic-list-container {
  min-height: 400px;
}

.topic-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

// ==================== 帖子卡片 ====================
.topic-card {
  background: $card-bg;
  border-radius: 12px;
  padding: 20px 24px;
  border: 1px solid rgba($border-color, 0.5);
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
    border-color: $primary-color;
  }
}

// ==================== 分类标签 ====================
.card-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;

  .tag {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 2px 10px;
    border-radius: 16px;
    font-size: 12px;
    font-weight: 500;
    line-height: 20px;

    &.tag-pinned {
      background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
      color: #fff;
    }

    &.tag-question {
      background: rgba($question-color, 0.12);
      color: $question-color;
    }

    &.tag-discussion {
      background: rgba($discussion-color, 0.12);
      color: $discussion-color;
    }

    &.tag-share {
      background: rgba($share-color, 0.12);
      color: $share-color;
    }

    &.tag-solved {
      background: rgba($success-color, 0.12);
      color: $success-color;
    }
  }
}

// ==================== 标题 ====================
.card-title {
  font-size: 17px;
  font-weight: 600;
  color: $text-primary;
  margin: 0 0 8px 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

// ==================== 内容预览 ====================
.card-preview {
  color: $text-secondary;
  font-size: 14px;
  line-height: 1.6;
  margin: 0 0 16px 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

// ==================== 底部信息 ====================
.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 12px;
  border-top: 1px solid rgba($border-color, 0.4);
}

.author-info {
  display: flex;
  align-items: center;
  gap: 10px;

  .author-avatar {
    border: 2px solid #fff;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  }

  .author-name {
    font-size: 14px;
    font-weight: 500;
    color: $text-primary;
  }

  .author-badge {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 2px 8px;
    border-radius: 12px;
    font-size: 12px;

    &.teacher {
      background: linear-gradient(135deg, #ffd54f 0%, #ffb300 100%);
      color: #5d4037;
    }
  }

  .post-time {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 13px;
    color: $text-muted;
  }
}

.card-stats {
  display: flex;
  align-items: center;
  gap: 16px;

  .stat-item {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 13px;
    color: $text-muted;
    transition: color 0.2s;

    &:hover {
      color: $primary-color;
    }

    &.like-btn {
      cursor: pointer;

      &.liked {
        color: $danger-color;

        .el-icon {
          animation: heartBeat 0.3s ease-in-out;
        }
      }
    }

    // 管理员删除按钮
    &.admin-delete-btn {
      cursor: pointer;
      margin-left: 8px;
      padding: 4px 8px;
      border-radius: 4px;
      color: $text-muted;
      background: rgba($danger-color, 0.08);

      &:hover {
        color: #fff;
        background: $danger-color;
      }
    }
  }
}

@keyframes heartBeat {
  0% { transform: scale(1); }
  50% { transform: scale(1.3); }
  100% { transform: scale(1); }
}

// ==================== 分页 ====================
.pagination-wrapper {
  display: flex;
  justify-content: center;
  padding: 24px;
  background: $card-bg;
  border-radius: 12px;
  margin-top: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba($border-color, 0.5);
}

// ==================== 空状态 ====================
.empty-state {
  padding: 80px 0;
  background: $card-bg;
  border-radius: 12px;
  border: 1px solid rgba($border-color, 0.5);
}

// ==================== 发帖对话框 ====================
.create-dialog {
  .category-radio {
    display: flex;
    gap: 16px;

    .el-radio {
      margin-right: 0;
      padding: 12px 20px;
      border-radius: 8px;

      &.is-checked {
        border-color: $primary-color;
        background: rgba($primary-color, 0.05);
      }
    }
  }

  .dialog-footer {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
  }
}
</style>