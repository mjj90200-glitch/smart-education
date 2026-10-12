<template>
  <div class="topic-detail" v-loading="loading">
    <!-- 返回按钮 -->
    <div class="back-bar">
      <el-button text @click="router.back()" class="back-btn">
        <el-icon><ArrowLeft /></el-icon>
        返回列表
      </el-button>
    </div>

    <!-- 主帖内容 -->
    <div class="topic-main-card" v-if="topic">
      <!-- 楼主信息区 -->
      <div class="author-section">
        <div class="author-avatar-wrapper">
          <el-avatar :size="56" :src="topic.userAvatar || undefined" class="main-avatar">
            {{ topic.userName?.charAt(0) }}
          </el-avatar>
          <span v-if="topic.userRole === 'TEACHER'" class="teacher-crown">
            <el-icon><Medal /></el-icon>
          </span>
        </div>
        <div class="author-meta">
          <div class="author-name-row">
            <span class="author-name">{{ topic.userName }}</span>
            <el-tag
              v-if="topic.userRole === 'TEACHER'"
              type="warning"
              size="small"
              effect="dark"
              class="role-tag"
            >
              <el-icon><Medal /></el-icon>
              老师
            </el-tag>
          </div>
          <div class="post-time">
            <el-icon><Clock /></el-icon>
            {{ formatTime(topic.createdAt) }} 发布
          </div>
        </div>
      </div>

      <!-- 帖子标题 -->
      <div class="topic-tags">
        <span :class="['tag', `tag-${topic.category.toLowerCase()}`]">
          {{ getCategoryLabel(topic.category) }}
        </span>
        <span class="tag tag-solved" v-if="topic.isSolved === 1">
          <el-icon><CircleCheck /></el-icon> 已解决
        </span>
        <span class="tag tag-pinned" v-if="topic.status === 2">
          <el-icon><Top /></el-icon> 置顶
        </span>
      </div>
      <h1 class="topic-title">{{ topic.title }}</h1>

      <!-- 帖子内容 -->
      <div class="topic-content">
        <div class="content-text" v-html="formatContent(topic.content)"></div>
        <div v-if="topic.attachmentUrl" class="attachment">
          <el-link :href="topic.attachmentUrl" target="_blank" type="primary">
            <el-icon><Document /></el-icon>
            查看附件
          </el-link>
        </div>
      </div>

      <!-- 操作区 -->
      <div class="topic-actions">
        <div class="stats">
          <span class="stat-item">
            <el-icon><View /></el-icon>
            {{ topic.viewCount }} 浏览
          </span>
          <span class="stat-item">
            <el-icon><ChatDotRound /></el-icon>
            {{ topic.replyCount }} 回复
          </span>
        </div>
        <div class="actions">
          <el-button
            :type="topic.isLiked ? 'danger' : 'primary'"
            :plain="!topic.isLiked"
            @click="handleLikeTopic"
            class="like-btn"
          >
            <el-icon><Star /></el-icon>
            {{ topic.isLiked ? '已点赞' : '点赞' }} ({{ topic.likeCount }})
          </el-button>
          <!-- 帖子作者删除 -->
          <el-button
            v-if="topic.userId === currentUserId"
            type="danger"
            plain
            @click="handleDeleteTopic"
          >
            <el-icon><Delete /></el-icon>
            删除帖子
          </el-button>
          <!-- 管理员删除帖子 -->
          <el-button
            v-if="isAdmin && topic.userId !== currentUserId"
            type="danger"
            link
            @click="handleAdminDeleteTopic"
          >
            <el-icon><Delete /></el-icon>
            管理员删除
          </el-button>
        </div>
      </div>
    </div>

    <!-- 回复列表 -->
    <div class="reply-section">
      <div class="section-header">
        <h3 class="section-title">
          <el-icon><ChatDotRound /></el-icon>
          全部回复
          <span class="reply-count">{{ topic?.replyCount || 0 }}</span>
        </h3>
      </div>

      <div v-if="replies.length === 0" class="empty-replies">
        <el-empty description="暂无回复，快来抢沙发吧！">
          <el-button type="primary" @click="() => document.querySelector('.post-reply')?.scrollIntoView({ behavior: 'smooth' })">
            写下第一个回复
          </el-button>
        </el-empty>
      </div>

      <div v-for="reply in replies" :key="reply.id" :class="['reply-card', { 'teacher-reply': reply.isTeacher, 'accepted-reply': reply.isAccepted === 1 }]">
        <!-- 老师标识角标 -->
        <div v-if="reply.isTeacher" class="teacher-badge-corner">
          <el-icon><Medal /></el-icon>
          名师解答
        </div>

        <div class="reply-header">
          <div class="reply-author">
            <el-avatar :size="40" :src="reply.userAvatar || undefined" class="reply-avatar">
              {{ reply.userName?.charAt(0) }}
            </el-avatar>
            <div class="author-info">
              <span class="user-name">{{ reply.userName }}</span>
              <div class="badges">
                <el-tag
                  v-if="reply.isTeacher"
                  type="warning"
                  effect="dark"
                  size="small"
                  class="teacher-tag"
                >
                  <el-icon><Medal /></el-icon>
                  老师
                </el-tag>
                <el-tag
                  v-if="reply.isAccepted === 1"
                  type="success"
                  size="small"
                  class="accepted-tag"
                >
                  <el-icon><Check /></el-icon>
                  最佳答案
                </el-tag>
              </div>
            </div>
          </div>
          <span class="reply-time">
            <el-icon><Clock /></el-icon>
            {{ formatTime(reply.createdAt) }}
          </span>
        </div>

        <div class="reply-text" v-html="formatContent(reply.content)"></div>

        <div v-if="reply.attachmentUrl" class="attachment">
          <el-link :href="reply.attachmentUrl" target="_blank" type="primary">
            <el-icon><Document /></el-icon>
            查看附件
          </el-link>
        </div>

        <div class="reply-actions">
          <span
            :class="['action-btn', { liked: reply.isLiked }]"
            @click="handleLikeReply(reply)"
          >
            <el-icon><Star /></el-icon>
            {{ reply.likeCount }}
          </span>
          <span class="action-btn" @click="showReplyInput(reply)">
            <el-icon><ChatDotRound /></el-icon>
            回复
          </span>
          <span
            v-if="canAcceptReply && reply.isAccepted !== 1"
            class="action-btn accept-btn"
            @click="handleAcceptReply(reply.id)"
          >
            <el-icon><Check /></el-icon>
            采纳
          </span>
          <span
            v-if="canDeleteReply(reply)"
            class="action-btn delete-btn"
            @click="handleDeleteReply(reply.id)"
          >
            <el-icon><Delete /></el-icon>
            删除
          </span>
          <!-- 管理员删除回复 -->
          <span
            v-if="isAdmin && !canDeleteReply(reply)"
            class="action-btn admin-delete-btn"
            @click="handleAdminDeleteReply(reply)"
          >
            <el-icon><Delete /></el-icon>
            管理员删除
          </span>
        </div>

        <!-- 楼中楼回复 -->
        <div v-if="reply.children && reply.children.length > 0" class="nested-replies">
          <div v-for="child in reply.children" :key="child.id" class="nested-reply">
            <div class="nested-header">
              <el-avatar :size="28" :src="child.userAvatar || undefined">
                {{ child.userName?.charAt(0) }}
              </el-avatar>
              <span class="nested-user">{{ child.userName }}</span>
              <el-tag
                v-if="child.isTeacher"
                type="warning"
                size="small"
                effect="plain"
              >老师</el-tag>
              <span class="nested-time">{{ formatTime(child.createdAt) }}</span>
            </div>
            <div class="nested-content" v-html="formatContent(child.content)"></div>
            <div class="nested-actions">
              <span
                :class="{ liked: child.isLiked }"
                @click="handleLikeReply(child)"
              >
                <el-icon><Star /></el-icon>
                {{ child.likeCount }}
              </span>
              <span @click="showReplyInput(reply, child)">
                <el-icon><ChatDotRound /></el-icon>
                回复
              </span>
            </div>
          </div>
        </div>

        <!-- 回复输入框 -->
        <div v-if="replyInputVisible === reply.id" class="reply-input-box">
          <el-input
            v-model="replyContent"
            type="textarea"
            :rows="3"
            placeholder="输入回复内容..."
          />
          <div class="reply-input-actions">
            <el-button size="default" @click="replyInputVisible = null">取消</el-button>
            <el-button
              type="primary"
              size="default"
              @click="submitReply(reply.id)"
              :loading="submitting"
            >
              发送
            </el-button>
          </div>
        </div>
      </div>
    </div>

    <!-- 发表回复 -->
    <div class="post-reply">
      <h3 class="section-title">
        <el-icon><Edit /></el-icon>
        发表回复
      </h3>
      <el-input
        v-model="newReplyContent"
        type="textarea"
        :rows="4"
        placeholder="分享你的想法或解答..."
      />
      <div class="post-actions">
        <el-upload
          v-model:file-list="replyFileList"
          :limit="1"
          :auto-upload="false"
        >
          <el-button type="primary" plain>
            <el-icon><Upload /></el-icon>
            上传附件
          </el-button>
        </el-upload>
        <el-button
          type="primary"
          size="large"
          @click="submitNewReply"
          :loading="submitting"
          class="submit-btn"
        >
          <el-icon v-if="!submitting"><Promotion /></el-icon>
          {{ submitting ? '发送中...' : '发表回复' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  ArrowLeft,
  View,
  ChatDotRound,
  Star,
  Document,
  Check,
  Delete,
  Upload,
  Medal,
  Clock,
  CircleCheck,
  Top,
  Edit,
  Promotion
} from '@element-plus/icons-vue'
import {
  getTopicDetail,
  deleteTopic as deleteTopicApi,
  likeTopic,
  createReply,
  deleteReply as deleteReplyApi,
  acceptReply,
  likeReply,
  adminDeleteTopic,
  adminDeleteReply,
  type TopicDetail,
  type ReplyItem
} from '@/api/questionHall'
import { useUserStore } from '@/store/modules/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

// 状态
const loading = ref(false)
const submitting = ref(false)
const topic = ref<TopicDetail | null>(null)
const replies = ref<ReplyItem[]>([])

// 回复相关
const newReplyContent = ref('')
const replyFileList = ref<any[]>([])
const replyInputVisible = ref<number | null>(null)
const replyContent = ref('')

// 计算属性
const currentUserId = computed(() => userStore.userInfo?.id)
const isAdmin = computed(() => userStore.isAdmin())

const canDeleteTopic = computed(() => {
  return topic.value?.userId === currentUserId.value || isAdmin.value
})

const canAcceptReply = computed(() => {
  return topic.value?.userId === currentUserId.value && topic.value?.isSolved !== 1
})

// 加载帖子详情
const loadTopicDetail = async () => {
  // 获取路由参数
  const topicIdParam = route.params.id as string
  console.log('[Detail.vue] Current Topic ID param:', topicIdParam, 'type:', typeof topicIdParam)

  // 防御性检查：验证 id 是否有效
  if (!topicIdParam) {
    console.error('[Detail.vue] Missing topic ID in route params')
    ElMessage.error('帖子ID缺失')
    router.push('/question-hall/list')
    return
  }

  // 防止路由占位符未被替换的情况（如直接访问 /question-hall/:id）
  if (topicIdParam === ':id' || topicIdParam.startsWith(':')) {
    console.error('[Detail.vue] Route placeholder not replaced:', topicIdParam)
    ElMessage.error('帖子ID无效：路由参数未替换')
    router.push('/question-hall/list')
    return
  }

  // 转换为数字并验证
  const topicId = Number(topicIdParam)
  if (isNaN(topicId) || topicId <= 0) {
    console.error('[Detail.vue] Invalid topic ID:', topicIdParam, 'converted to:', topicId)
    ElMessage.error('帖子ID无效')
    router.push('/question-hall/list')
    return
  }

  console.log('[Detail.vue] Loading topic detail for ID:', topicId)
  loading.value = true

  try {
    const res = await getTopicDetail(topicId)
    if (res.code === 200 && res.data) {
      topic.value = res.data
      replies.value = res.data.replies || []
    } else {
      ElMessage.error('帖子不存在')
      router.push('/question-hall/list')
    }
  } catch (error) {
    console.error('加载帖子详情失败:', error)
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

// 点赞帖子
const handleLikeTopic = async () => {
  if (!topic.value) return
  try {
    const res = await likeTopic(topic.value.id)
    if (res.code === 200 && res.data) {
      topic.value.isLiked = res.data.isLiked
      topic.value.likeCount += res.data.isLiked ? 1 : -1
    }
  } catch (error) {
    console.error('点赞失败:', error)
  }
}

// 删除帖子
const handleDeleteTopic = async () => {
  if (!topic.value) return

  try {
    await ElMessageBox.confirm('确定要删除这个帖子吗？', '提示', {
      type: 'warning'
    })

    const res = await deleteTopicApi(topic.value.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      router.push('/question-hall')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

// 管理员删除帖子
const handleAdminDeleteTopic = async () => {
  if (!topic.value) return

  try {
    await ElMessageBox.confirm(
      `确定要删除帖子「${topic.value.title}」吗？此操作不可恢复。`,
      '管理员删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )

    const res = await adminDeleteTopic(topic.value.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      router.push('/question-hall/list')
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

// 管理员删除回复
const handleAdminDeleteReply = async (reply: ReplyItem) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除 ${reply.userName} 的回复吗？此操作不可恢复。`,
      '管理员删除确认',
      {
        confirmButtonText: '确定删除',
        cancelButtonText: '取消',
        type: 'warning',
        confirmButtonClass: 'el-button--danger'
      }
    )

    const res = await adminDeleteReply(reply.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadTopicDetail()
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

// 显示回复输入框
const showReplyInput = (reply: ReplyItem, child?: ReplyItem) => {
  replyInputVisible.value = reply.id
  replyContent.value = child ? `@${child.userName} ` : ''
}

// 提交回复
const submitReply = async (replyId: number) => {
  if (!topic.value || !replyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }

  submitting.value = true
  try {
    const res = await createReply(topic.value.id, {
      content: replyContent.value,
      parentReplyId: replyId
    })

    if (res.code === 200) {
      ElMessage.success('回复成功')
      replyInputVisible.value = null
      replyContent.value = ''
      loadTopicDetail()
    }
  } catch (error) {
    console.error('回复失败:', error)
  } finally {
    submitting.value = false
  }
}

// 提交新回复
const submitNewReply = async () => {
  if (!topic.value || !newReplyContent.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }

  submitting.value = true
  try {
    const res = await createReply(topic.value.id, {
      content: newReplyContent.value,
      file: replyFileList.value[0]?.raw
    })

    if (res.code === 200) {
      ElMessage.success('回复成功')
      newReplyContent.value = ''
      replyFileList.value = []
      loadTopicDetail()
    }
  } catch (error) {
    console.error('回复失败:', error)
  } finally {
    submitting.value = false
  }
}

// 点赞回复
const handleLikeReply = async (reply: ReplyItem) => {
  try {
    const res = await likeReply(reply.id)
    if (res.code === 200 && res.data) {
      reply.isLiked = res.data.isLiked
      reply.likeCount += res.data.isLiked ? 1 : -1
    }
  } catch (error) {
    console.error('点赞失败:', error)
  }
}

// 采纳答案
const handleAcceptReply = async (replyId: number) => {
  if (!topic.value) return

  try {
    await ElMessageBox.confirm('确定采纳这个回答为最佳答案吗？', '提示')

    const res = await acceptReply(topic.value.id, replyId)
    if (res.code === 200) {
      ElMessage.success('采纳成功')
      loadTopicDetail()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('采纳失败:', error)
    }
  }
}

// 判断是否可删除回复
const canDeleteReply = (reply: ReplyItem) => {
  return reply.userId === currentUserId.value
}

// 删除回复
const handleDeleteReply = async (replyId: number) => {
  try {
    await ElMessageBox.confirm('确定要删除这条回复吗？', '提示', {
      type: 'warning'
    })

    const res = await deleteReplyApi(replyId)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadTopicDetail()
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
    }
  }
}

// 获取分类标签类型
const getCategoryType = (category: string): string => {
  const types: Record<string, string> = {
    QUESTION: 'primary',
    DISCUSSION: 'warning',
    SHARE: 'success'
  }
  return types[category] || 'info'
}

// 获取分类标签文字
const getCategoryLabel = (category: string): string => {
  const labels: Record<string, string> = {
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

// 格式化内容（简单的换行处理）
const formatContent = (content: string): string => {
  return content.replace(/\n/g, '<br>')
}

onMounted(() => {
  loadTopicDetail()
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
$teacher-bg: #e3f2fd;
$teacher-border: #2196f3;
$question-color: #5c6bc0;
$discussion-color: #ff9800;
$share-color: #26a69a;

// ==================== 主容器 ====================
.topic-detail {
  max-width: 920px;
  margin: 0 auto;
  padding: 24px;
  background: $bg-color;
  min-height: calc(100vh - 60px);
}

// ==================== 返回按钮 ====================
.back-bar {
  margin-bottom: 16px;

  .back-btn {
    color: $text-secondary;
    font-size: 14px;

    &:hover {
      color: $primary-color;
    }
  }
}

// ==================== 主帖卡片 ====================
.topic-main-card {
  background: $card-bg;
  border-radius: 16px;
  padding: 28px 32px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba($border-color, 0.4);
}

// ==================== 楼主信息区 ====================
.author-section {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 20px;
  padding-bottom: 20px;
  border-bottom: 1px solid rgba($border-color, 0.4);

  .author-avatar-wrapper {
    position: relative;

    .main-avatar {
      border: 3px solid #fff;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
    }

    .teacher-crown {
      position: absolute;
      top: -6px;
      right: -6px;
      width: 24px;
      height: 24px;
      background: linear-gradient(135deg, #ffd54f 0%, #ffb300 100%);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      font-size: 12px;
      box-shadow: 0 2px 6px rgba(255, 179, 0, 0.4);
    }
  }

  .author-meta {
    .author-name-row {
      display: flex;
      align-items: center;
      gap: 10px;

      .author-name {
        font-size: 18px;
        font-weight: 600;
        color: $text-primary;
      }

      .role-tag {
        display: flex;
        align-items: center;
        gap: 4px;
      }
    }

    .post-time {
      display: flex;
      align-items: center;
      gap: 6px;
      margin-top: 6px;
      font-size: 13px;
      color: $text-muted;
    }
  }
}

// ==================== 标签区 ====================
.topic-tags {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;

  .tag {
    display: inline-flex;
    align-items: center;
    gap: 4px;
    padding: 3px 12px;
    border-radius: 16px;
    font-size: 12px;
    font-weight: 500;

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

    &.tag-pinned {
      background: linear-gradient(135deg, #ff6b6b 0%, #ee5a5a 100%);
      color: #fff;
    }
  }
}

// ==================== 标题 ====================
.topic-title {
  font-size: 26px;
  font-weight: 700;
  color: $text-primary;
  margin: 0 0 20px 0;
  line-height: 1.4;
}

// ==================== 内容区 ====================
.topic-content {
  .content-text {
    font-size: 16px;
    line-height: 1.8;
    color: $text-primary;
    margin-bottom: 20px;
    white-space: pre-wrap;
    word-break: break-word;
  }

  .attachment {
    padding: 14px 18px;
    background: rgba($primary-color, 0.05);
    border-radius: 8px;
    border: 1px dashed rgba($primary-color, 0.3);
  }
}

// ==================== 操作区 ====================
.topic-actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 20px;
  border-top: 1px solid rgba($border-color, 0.4);

  .stats {
    display: flex;
    gap: 20px;

    .stat-item {
      display: flex;
      align-items: center;
      gap: 6px;
      color: $text-muted;
      font-size: 14px;
    }
  }

  .actions {
    display: flex;
    gap: 12px;

    .like-btn {
      min-width: 120px;
    }
  }
}

// ==================== 回复区 ====================
.reply-section {
  background: $card-bg;
  border-radius: 16px;
  padding: 24px 28px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba($border-color, 0.4);

  .section-header {
    margin-bottom: 20px;
    padding-bottom: 16px;
    border-bottom: 1px solid rgba($border-color, 0.4);
  }

  .section-title {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 18px;
    font-weight: 600;
    color: $text-primary;
    margin: 0;

    .reply-count {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      min-width: 28px;
      height: 22px;
      padding: 0 8px;
      background: rgba($primary-color, 0.1);
      color: $primary-color;
      border-radius: 12px;
      font-size: 13px;
      font-weight: 500;
    }
  }
}

// ==================== 回复卡片 ====================
.reply-card {
  position: relative;
  padding: 20px;
  margin-bottom: 16px;
  background: #fafbfc;
  border-radius: 12px;
  border: 1px solid rgba($border-color, 0.3);
  transition: all 0.25s ease;

  &:hover {
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  }

  &:last-child {
    margin-bottom: 0;
  }

  // 老师回复高亮
  &.teacher-reply {
    background: linear-gradient(135deg, rgba($teacher-bg, 0.5) 0%, rgba(#bbdefb, 0.3) 100%);
    border: 2px solid $teacher-border;
    box-shadow: 0 4px 16px rgba($teacher-border, 0.15);

    .teacher-badge-corner {
      position: absolute;
      top: -1px;
      right: 20px;
      display: flex;
      align-items: center;
      gap: 4px;
      padding: 4px 12px;
      background: linear-gradient(135deg, $teacher-border 0%, #1976d2 100%);
      color: #fff;
      font-size: 12px;
      font-weight: 500;
      border-radius: 0 0 8px 8px;
    }
  }

  // 已采纳回复
  &.accepted-reply {
    border-color: $success-color;
    background: linear-gradient(135deg, rgba($success-color, 0.05) 0%, rgba(#c8e6c9, 0.2) 100%);
  }

  .teacher-badge-corner {
    display: none;
  }
}

// ==================== 回复头部 ====================
.reply-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 14px;

  .reply-author {
    display: flex;
    align-items: center;
    gap: 12px;

    .reply-avatar {
      border: 2px solid #fff;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    }

    .author-info {
      .user-name {
        font-weight: 600;
        font-size: 15px;
        color: $text-primary;
      }

      .badges {
        display: flex;
        gap: 6px;
        margin-top: 4px;

        .teacher-tag,
        .accepted-tag {
          display: flex;
          align-items: center;
          gap: 4px;
        }
      }
    }
  }

  .reply-time {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 12px;
    color: $text-muted;
  }
}

// ==================== 回复内容 ====================
.reply-text {
  font-size: 15px;
  line-height: 1.7;
  color: $text-primary;
  margin-bottom: 14px;
  white-space: pre-wrap;
  word-break: break-word;
}

// ==================== 回复操作 ====================
.reply-actions {
  display: flex;
  gap: 16px;

  .action-btn {
    display: flex;
    align-items: center;
    gap: 4px;
    font-size: 13px;
    color: $text-muted;
    cursor: pointer;
    padding: 4px 8px;
    border-radius: 4px;
    transition: all 0.2s;

    &:hover {
      color: $primary-color;
      background: rgba($primary-color, 0.08);
    }

    &.liked {
      color: $danger-color;
    }

    &.accept-btn:hover {
      color: $success-color;
      background: rgba($success-color, 0.08);
    }

    &.delete-btn:hover {
      color: $danger-color;
      background: rgba($danger-color, 0.08);
    }

    // 管理员删除按钮样式
    &.admin-delete-btn {
      color: rgba($danger-color, 0.7);
      background: rgba($danger-color, 0.05);
      border: 1px dashed rgba($danger-color, 0.3);

      &:hover {
        color: #fff;
        background: $danger-color;
        border-color: $danger-color;
      }
    }
  }
}

// ==================== 楼中楼回复 ====================
.nested-replies {
  margin-top: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid rgba($border-color, 0.3);
}

.nested-reply {
  padding: 12px 0;
  border-bottom: 1px solid rgba($border-color, 0.2);

  &:last-child {
    border-bottom: none;
  }

  .nested-header {
    display: flex;
    align-items: center;
    gap: 10px;
    margin-bottom: 8px;

    .nested-user {
      font-size: 14px;
      font-weight: 500;
      color: $text-primary;
    }

    .nested-time {
      font-size: 12px;
      color: $text-muted;
      margin-left: auto;
    }
  }

  .nested-content {
    font-size: 14px;
    color: $text-secondary;
    line-height: 1.6;
    margin-bottom: 8px;
  }

  .nested-actions {
    display: flex;
    gap: 12px;
    font-size: 12px;
    color: $text-muted;

    span {
      display: flex;
      align-items: center;
      gap: 4px;
      cursor: pointer;

      &:hover {
        color: $primary-color;
      }

      &.liked {
        color: $danger-color;
      }
    }
  }
}

// ==================== 回复输入框 ====================
.reply-input-box {
  margin-top: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid $primary-color;

  .reply-input-actions {
    display: flex;
    justify-content: flex-end;
    gap: 10px;
    margin-top: 12px;
  }
}

// ==================== 发表回复区 ====================
.post-reply {
  background: $card-bg;
  border-radius: 16px;
  padding: 24px 28px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  border: 1px solid rgba($border-color, 0.4);

  .section-title {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 18px;
    font-weight: 600;
    color: $text-primary;
    margin: 0 0 16px 0;
  }

  .post-actions {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-top: 16px;

    .submit-btn {
      min-width: 140px;
    }
  }
}

// ==================== 空状态 ====================
.empty-replies {
  padding: 60px 0;
}
</style>