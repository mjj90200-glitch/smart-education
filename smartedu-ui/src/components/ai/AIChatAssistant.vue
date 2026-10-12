<template>
  <div :class="['ai-chat-assistant', { embedded }]">
    <!-- 悬浮按钮 -->
    <transition name="fade">
      <div
        v-if="!embedded && !isStandaloneRoute && !isChatOpen"
        class="chat-float-btn"
        @click="toggleChat"
      >
        <el-badge :is-dot="hasUnreadMessage">
          <el-button type="primary" circle size="large">
            <el-icon><ChatDotRound /></el-icon>
          </el-button>
        </el-badge>
      </div>
    </transition>

    <!-- 聊天窗口 -->
    <transition name="slide">
      <div v-if="embedded || (!isStandaloneRoute && isChatOpen)" class="chat-window">
        <!-- 头部 -->
        <div class="chat-header">
          <div class="header-left">
            <el-icon class="ai-icon"><SmartEduIcon /></el-icon>
            <div class="header-text">
              <span class="title">智学助手</span>
              <span class="subtitle">{{ statusText }}</span>
            </div>
          </div>
          <div class="header-right">
            <el-button text @click="clearHistory" title="清除历史">
              <el-icon><Delete /></el-icon>
            </el-button>
            <el-button v-if="!embedded" text @click="toggleChat" title="最小化">
              <el-icon><Minus /></el-icon>
            </el-button>
            <el-button v-if="!embedded" text @click="isChatOpen = false" title="关闭">
              <el-icon><Close /></el-icon>
            </el-button>
          </div>
        </div>

        <!-- 消息列表 -->
        <div
          ref="messageListRef"
          class="chat-messages"
          @dragover.prevent
          @drop.prevent="handleDrop"
        >
          <!-- 欢迎语 -->
          <div v-if="!aiChatStore.hasMessages" class="welcome-message">
            <el-empty :description="aiChatStore.getWelcomeMessage(userStore.userInfo?.role)" :image-size="80">
              <template #image>
                <el-icon :size="60" class="welcome-icon"><ChatLineRound /></el-icon>
              </template>
            </el-empty>
          </div>

          <!-- 消息列表 -->
          <div v-else class="messages-list">
            <div
              v-for="message in aiChatStore.messages"
              :key="message.id"
              :class="['message-item', message.role]"
            >
              <!-- 头像 -->
              <div :class="['avatar', message.role]">
                <el-avatar v-if="message.role === 'user'" :size="36" :src="userStore.userInfo?.avatar" />
                <el-avatar v-else-if="message.role === 'ai'" :size="36" class="ai-avatar">
                  <el-icon><SmartEduIcon /></el-icon>
                </el-avatar>
                <el-avatar v-else :size="36" class="system-avatar">
                  <el-icon><Bell /></el-icon>
                </el-avatar>
              </div>

              <!-- 消息内容 -->
              <div :class="['message-content', message.role]">
                <!-- 系统消息（Agent 状态卡片） -->
                <div v-if="message.role === 'system'" :class="['system-card', message.status]">
                  <el-icon class="status-icon"><component :is="getStatusIcon(message.status)" /></el-icon>
                  <span class="status-text">{{ message.content }}</span>
                  <span v-if="message.toolName" class="tool-name">{{ message.toolName }}</span>
                </div>

                <!-- 普通消息（支持 Markdown 渲染） -->
                <div v-else class="message-bubble">
                  <div v-html="renderMarkdown(message.content)" class="markdown-content"></div>
                </div>

                <!-- 时间戳 -->
                <div class="message-time">{{ formatTime(message.timestamp) }}</div>
              </div>
            </div>

            <!-- 加载中状态 -->
            <div v-if="aiChatStore.isStreaming" class="message-item ai">
              <div class="avatar ai">
                <el-avatar :size="36" class="ai-avatar">
                  <el-icon><SmartEduIcon /></el-icon>
                </el-avatar>
              </div>
              <div class="message-content ai">
                <div class="typing-indicator">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </div>
            </div>
          </div>

          <!-- 拖拽上传提示 -->
          <transition name="fade">
            <div v-if="isDragOver" class="drag-overlay">
              <el-icon :size="48"><Upload /></el-icon>
              <span>释放文件以上传</span>
            </div>
          </transition>
        </div>

        <!-- 文件预览区 -->
        <transition name="slide">
          <div v-if="selectedFile" class="file-preview">
            <el-tag size="small" closable @close="clearSelectedFile">
              <el-icon><Document /></el-icon>
              {{ selectedFile.name }}
            </el-tag>
          </div>
        </transition>

        <!-- 输入区 -->
        <div class="chat-input">
          <div class="input-tools">
            <el-button text size="small" @click="triggerFileUpload" title="上传文件">
              <el-icon><Paperclip /></el-icon>
            </el-button>
            <input
              ref="fileInputRef"
              type="file"
              class="file-input"
              @change="handleFileSelect"
              accept=".doc,.docx,.pdf,.txt"
            />
          </div>
          <el-input
            v-model="inputMessage"
            placeholder="输入消息... (Shift+Enter 换行)"
            type="textarea"
            :rows="2"
            :maxlength="2000"
            show-word-limit
            resize="none"
            @keydown.enter.exact.prevent="sendMessage"
          />
          <div class="input-actions">
            <el-button
              type="primary"
              :loading="aiChatStore.isStreaming"
              :disabled="!canSend"
              @click="sendMessage"
            >
              <el-icon><Promotion /></el-icon>
              发送
            </el-button>
          </div>
        </div>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, nextTick, onMounted, h } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/modules/user'
import { useAiChatStore } from '@/stores/aiChat'
import {
  sendChatMessage,
  sendChatMessageWithFile,
  sendChatMessageStream,
  sendChatMessageWithFileStream,
  AgentStatus,
  type ChatMessage
} from '@/api/ai'
import {
  ChatDotRound,
  ChatLineRound,
  Delete,
  Minus,
  Close,
  Bell,
  Upload,
  Document,
  Paperclip,
  Promotion,
  Loading,
  CircleCheck,
  CircleClose,
  Timer,
  Star
} from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-light.css'
import { ElMessage } from 'element-plus'

const props = withDefaults(defineProps<{
  embedded?: boolean
}>(), {
  embedded: false
})

// 自定义 AI 图标
const SmartEduIcon = {
  name: 'SmartEduIcon',
  render() {
    return h('svg', { viewBox: '0 0 24 24', fill: 'currentColor' }, [
      h('path', { d: 'M12 3L1 9l11 6 9-4.91V17h2V9M5 13.18v4L12 21l7-3.82v-4L12 17l-7-3.82z' })
    ])
  }
}

// Store
const route = useRoute()
const userStore = useUserStore()
const aiChatStore = useAiChatStore()

// Markdown 渲染器
// ========== P0 修复：添加 breaks: true，将 \n 转换为 <br> ==========
const md = new MarkdownIt({
  html: true,        // 允许 HTML 标签
  linkify: true,     // 自动转换链接
  breaks: true,      // 核心：将单个 \n 转换为 <br>（解决换行显示问题）
  typographer: true, // 智能引号等排版优化
  highlight: (str: string, lang: string) => {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<pre class="hljs"><code>${hljs.highlight(str, { language: lang }).value}</code></pre>`
      } catch (__) {}
    }
    return `<pre class="hljs"><code>${md.utils.escapeHtml(str)}</code></pre>`
  }
})

// State
const isChatOpen = ref(false)
const hasUnreadMessage = ref(false)
const inputMessage = ref('')
const selectedFile = ref<File | null>(null)
const isDragOver = ref(false)
const abortController = ref<AbortController | null>(null)

// Refs
const messageListRef = ref<HTMLElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)

// Computed
const canSend = computed(() => {
  return (inputMessage.value.trim() || selectedFile.value) && !aiChatStore.isStreaming
})

const embedded = computed(() => props.embedded)

const isStandaloneRoute = computed(() => route.path === '/student/assistant' || route.path === '/teacher/assistant')

const statusText = computed(() => {
  switch (aiChatStore.agentStatus) {
    case AgentStatus.THINKING:
      return '思考中...'
    case AgentStatus.CALLING_TOOL:
      return '执行中...'
    case AgentStatus.SUCCESS:
      return '已完成'
    case AgentStatus.ERROR:
      return '出错'
    default:
      return '在线'
  }
})

// 初始化会话
onMounted(() => {
  if (!aiChatStore.sessionId) {
    aiChatStore.initSession()
  }
  // 滚动到底部
  nextTick(() => {
    scrollToBottom()
  })
})

// 监听消息变化，自动滚动
watch(
  () => aiChatStore.messages.length,
  () => {
    nextTick(() => {
      scrollToBottom()
    })
  },
  { deep: true }
)

// 渲染 Markdown
// ========== P0 修复：处理空内容，防止空白气泡 ==========
const renderMarkdown = (content: string) => {
  // 空内容检查
  if (!content || !content.trim()) {
    return '<span class="empty-message">正在思考...</span>'
  }
  return md.render(content)
}

// 格式化时间
const formatTime = (timestamp: number) => {
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now.getTime() - date.getTime()

  // 今天
  if (diff < 24 * 60 * 60 * 1000) {
    return date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
  }

  // 昨天
  if (diff < 48 * 60 * 60 * 1000) {
    return '昨天'
  }

  // 更早
  return date.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
}

// 获取状态图标
const getStatusIcon = (status?: AgentStatus) => {
  switch (status) {
    case AgentStatus.THINKING:
      return Loading
    case AgentStatus.CALLING_TOOL:
      return Timer
    case AgentStatus.SUCCESS:
      return CircleCheck
    case AgentStatus.ERROR:
      return CircleClose
    default:
      return Star
  }
}

// 切换聊天窗口
const toggleChat = () => {
  isChatOpen.value = !isChatOpen.value
  if (isChatOpen.value) {
    hasUnreadMessage.value = false
    nextTick(() => {
      scrollToBottom()
    })
  }
}

// 滚动到底部
const scrollToBottom = () => {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

// 发送消息
const sendMessage = async () => {
  const message = inputMessage.value.trim()
  if (!message && !selectedFile.value) return

  // 如果正在流式传输，点击发送按钮取消请求
  if (aiChatStore.isStreaming) {
    abortController.value?.abort()
    aiChatStore.setStreaming(false)
    aiChatStore.setAgentStatus(AgentStatus.IDLE)
    return
  }

  // 创建新的 AbortController
  abortController.value = new AbortController()

  // 准备历史
  const history = aiChatStore.getConversationHistory()

  // 添加用户消息
  if (message) {
    aiChatStore.addUserMessage(message)
  }

  // 添加 AI 占位消息
  aiChatStore.addAiMessage('')
  aiChatStore.setAgentStatus(AgentStatus.THINKING)
  aiChatStore.setStreaming(true)

  try {
    if (selectedFile.value) {
      // 带文件上传 - 使用流式
      await sendChatMessageWithFileStream(
        message || '请帮我处理这个文件',
        selectedFile.value,
        history,
        (chunk) => {
          aiChatStore.updateStreamingContent(chunk)
        },
        abortController.value.signal
      )
    } else {
      // 普通文本 - 使用流式
      await sendChatMessageStream(
        message,
        history,
        (chunk) => {
          aiChatStore.updateStreamingContent(chunk)
        },
        abortController.value.signal
      )
    }

    // 完成
    aiChatStore.setAgentStatus(AgentStatus.SUCCESS)
  } catch (error: any) {
    // ========== 用户取消 ==========
    if (error.name === 'AbortError') {
      aiChatStore.updateLastAiMessage('已取消')
      aiChatStore.setAgentStatus(AgentStatus.IDLE)
      return
    }

    console.error('发送消息失败:', error)

    // ========== 断线重连提示 ==========
    let errorMessage = '抱歉，处理您的请求时遇到错误，请稍后重试。'

    if (error.message) {
      if (error.message.includes('登录') || error.message.includes('过期')) {
        errorMessage = '登录已过期，请刷新页面重新登录。'
        // 提示用户刷新页面
        ElMessage.warning('登录状态已失效，请刷新页面重新登录')
      } else if (error.message.includes('权限') || error.message.includes('403')) {
        errorMessage = '权限不足，请检查登录状态后重试。'
        ElMessage.warning('权限不足，请刷新页面')
      } else if (error.message.includes('服务器') || error.message.includes('500')) {
        errorMessage = '服务器暂时不可用，请稍后重试。'
      } else {
        errorMessage = `处理失败：${error.message}`
      }
    }

    aiChatStore.setAgentStatus(AgentStatus.ERROR)
    aiChatStore.updateLastAiMessage(errorMessage)
  } finally {
    // ========== P1: 强制关闭 Loading（确保执行）==========
    aiChatStore.setStreaming(false)
    inputMessage.value = ''
    clearSelectedFile()
    abortController.value = null
  }
}

// 触发文件选择
const triggerFileUpload = () => {
  fileInputRef.value?.click()
}

// 处理文件选择
const handleFileSelect = (event: Event) => {
  const target = event.target as HTMLInputElement
  if (target.files?.[0]) {
    selectedFile.value = target.files[0]
  }
}

// 清除选中的文件
const clearSelectedFile = () => {
  selectedFile.value = null
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

// 处理拖拽
const handleDrop = (event: DragEvent) => {
  isDragOver.value = false
  const files = event.dataTransfer?.files
  if (files?.[0]) {
    selectedFile.value = files[0]
  }
}

// 清除历史
const clearHistory = () => {
  aiChatStore.clearCurrentSession()
  aiChatStore.initSession()
  ElMessage.success('聊天记录已清除')
}
</script>

<style scoped lang="scss">
.ai-chat-assistant {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 9999;

  &.embedded {
    position: static;
    inset: auto;
    width: 100%;

    .chat-window {
      width: 100%;
      height: min(78vh, 920px);
      min-height: 720px;
      border-radius: 20px;
      box-shadow: 0 20px 60px rgba(15, 23, 42, 0.12);
      border: 1px solid #e5e7eb;
      background: #fff;
      overflow: hidden;
      animation: fadeIn 0.8s ease-in-out;
    }

    .chat-header {
      padding: 16px 20px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: #fff;

      .header-left {
        gap: 12px;

        .ai-icon {
          font-size: 30px;
          animation: pulse 2s infinite;
        }

        .header-text {
          .title {
            font-size: 18px;
            font-weight: 600;
            text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
          }

          .subtitle {
            font-size: 13px;
            opacity: 0.9;
          }
        }
      }
    }

    .chat-messages {
      padding: 22px;
      background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
    }

    .chat-input {
      padding: 16px 18px 18px;
      background: #fff;
      border-top: 1px solid #e5e7eb;
    }
  }

  .chat-float-btn {
    cursor: pointer;
    transition: transform 0.3s, box-shadow 0.3s;

    &:hover {
      transform: scale(1.1);
      box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
    }
  }

  .chat-window {
    width: 420px;
    height: 650px;
    background: #fff;
    border-radius: 20px;
    box-shadow: 0 15px 50px rgba(0, 0, 0, 0.15);
    display: flex;
    flex-direction: column;
    overflow: hidden;
    transition: all 0.3s ease;

    &:hover {
      box-shadow: 0 20px 60px rgba(0, 0, 0, 0.2);
    }

    .chat-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 14px 18px;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      color: #fff;
      box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);

      .header-left {
        display: flex;
        align-items: center;
        gap: 12px;

        .ai-icon {
          font-size: 30px;
          animation: pulse 2s infinite;
        }

        .header-text {
          display: flex;
          flex-direction: column;

          .title {
            font-size: 17px;
            font-weight: 600;
            text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
          }

          .subtitle {
            font-size: 12px;
            opacity: 0.9;
          }
        }
      }

      .header-right {
        display: flex;
        gap: 6px;

        .el-button {
          color: #fff;
          transition: all 0.3s ease;

          &:hover {
            background: rgba(255, 255, 255, 0.2);
            transform: scale(1.05);
          }
        }
      }
    }

    .chat-messages {
      flex: 1;
      overflow-y: auto;
      padding: 18px;
      background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
      position: relative;

      &::-webkit-scrollbar {
        width: 6px;
      }

      &::-webkit-scrollbar-track {
        background: rgba(0, 0, 0, 0.05);
        border-radius: 10px;
      }

      &::-webkit-scrollbar-thumb {
        background: rgba(102, 126, 234, 0.5);
        border-radius: 10px;
        &:hover {
          background: rgba(102, 126, 234, 0.8);
        }
      }

      .welcome-message {
        display: flex;
        justify-content: center;
        align-items: center;
        height: 100%;

        .welcome-icon {
          color: #667eea;
          animation: bounce 2s infinite;
        }
      }

      .messages-list {
        display: flex;
        flex-direction: column;
        gap: 16px;

        .message-item {
          display: flex;
          gap: 10px;
          animation: slideIn 0.3s ease;

          &.user {
            flex-direction: row-reverse;
          }

          .avatar {
            flex-shrink: 0;
            transition: transform 0.3s ease;

            &:hover {
              transform: scale(1.1);
            }

            &.ai {
              .ai-avatar {
                background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                color: #fff;
                box-shadow: 0 2px 8px rgba(102, 126, 234, 0.4);
              }
            }

            &.system {
              .system-avatar {
                background: #909399;
                color: #fff;
              }
            }
          }

          .message-content {
            max-width: 75%;
            position: relative;

            // ========== P0: CSS 补丁 - 确保换行和长词正确显示 ==========
            white-space: pre-wrap;
            word-break: break-word;

            &.user {
              .message-bubble {
                background: linear-gradient(135deg, #409eff 0%, #667eea 100%);
                color: #fff;
                border-radius: 18px 18px 4px 18px;
                padding: 14px 18px;
                box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
                transition: all 0.3s ease;
              }

              .message-time {
                text-align: right;
              }
            }

            &.ai {
              .message-bubble {
                background: #fff;
                color: #303133;
                border-radius: 18px 18px 18px 4px;
                padding: 14px 18px;
                box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
                transition: all 0.3s ease;

                &:hover {
                  box-shadow: 0 6px 20px rgba(0, 0, 0, 0.15);
                }

                .markdown-content {
                  // ========== P0: 基础样式 ==========
                  line-height: 1.6;
                  font-size: 14px;

                  :deep(p) {
                    margin: 0 0 8px 0;

                    &:last-child {
                      margin: 0;
                    }
                  }

                  :deep(pre) {
                    background: #f6f8fa;
                    border-radius: 8px;
                    padding: 12px;
                    overflow-x: auto;
                    margin: 8px 0;
                    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);

                    code {
                      font-family: 'Consolas', 'Monaco', monospace;
                      font-size: 13px;
                      background: transparent;
                      padding: 0;
                    }
                  }

                  :deep(code) {
                    background: #f0f0f0;
                    padding: 2px 6px;
                    border-radius: 4px;
                    font-family: 'Consolas', 'Monaco', monospace;
                    font-size: 13px;
                  }

                  :deep(ul), :deep(ol) {
                    margin: 8px 0;
                    padding-left: 20px;
                  }

                  :deep(li) {
                    margin: 4px 0;
                  }

                  :deep(blockquote) {
                    border-left: 4px solid #667eea;
                    padding-left: 12px;
                    margin: 8px 0;
                    color: #666;
                  }

                  :deep(h1), :deep(h2), :deep(h3), :deep(h4), :deep(h5), :deep(h6) {
                    margin: 12px 0 8px 0;
                    font-weight: 600;
                    color: #667eea;
                  }

                  :deep(h1) { font-size: 20px; }
                  :deep(h2) { font-size: 18px; }
                  :deep(h3) { font-size: 16px; }

                  :deep(a) {
                    color: #667eea;
                    text-decoration: none;
                    font-weight: 500;
                    transition: all 0.3s ease;
                    &:hover {
                      text-decoration: underline;
                      color: #764ba2;
                    }
                  }

                  :deep(table) {
                    border-collapse: collapse;
                    width: 100%;
                    margin: 8px 0;
                    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);

                    th, td {
                      border: 1px solid #e6e6e6;
                      padding: 8px;
                      text-align: left;
                    }

                    th {
                      background: #f5f7fa;
                      font-weight: 600;
                      color: #667eea;
                    }
                  }

                  // ========== 代码块语法高亮样式 ==========
                  :deep(.hljs) {
                    background: #f6f8fa;
                    padding: 12px;
                    border-radius: 8px;
                    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
                  }

                  // ========== P0: 空消息占位样式 ==========
                  :deep(.empty-message) {
                    color: #909399;
                    font-style: italic;
                  }
                }
              }
            }

            .system-card {
              display: flex;
              align-items: center;
              gap: 8px;
              padding: 10px 14px;
              border-radius: 12px;
              font-size: 13px;
              box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
              transition: all 0.3s ease;

              &:hover {
                transform: translateY(-2px);
                box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
              }

              &.thinking {
                background: #e6f7ff;
                color: #1890ff;
              }

              &.calling_tool {
                background: #fff7e6;
                color: #fa8c16;
              }

              &.success {
                background: #f6ffed;
                color: #52c41a;
              }

              &.error {
                background: #fff1f0;
                color: #ff4d4f;
              }

              .status-icon {
                font-size: 16px;
                animation: pulse 1.5s infinite;
              }

              .tool-name {
                margin-left: auto;
                font-size: 12px;
                opacity: 0.7;
              }
            }

            .message-time {
              font-size: 11px;
              color: #909399;
              margin-top: 6px;
            }
          }
        }

        .typing-indicator {
          display: flex;
          gap: 6px;
          padding: 14px 18px;
          background: #fff;
          border-radius: 18px 18px 18px 4px;
          box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);

          span {
            width: 10px;
            height: 10px;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            border-radius: 50%;
            animation: typing 1.4s infinite;

            &:nth-child(2) {
              animation-delay: 0.2s;
            }

            &:nth-child(3) {
              animation-delay: 0.4s;
            }
          }
        }
      }

      .drag-overlay {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(102, 126, 234, 0.9);
        display: flex;
        flex-direction: column;
        justify-content: center;
        align-items: center;
        color: #fff;
        gap: 12px;
        border-radius: 12px;
        backdrop-filter: blur(10px);
        animation: fadeIn 0.3s ease;

        .el-icon {
          animation: bounce 1.5s infinite;
        }
      }
    }

    .file-preview {
      padding: 10px 18px;
      background: #f5f7fa;
      border-top: 1px solid #e6e6e6;
      animation: slideIn 0.3s ease;

      .el-tag {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: #fff;
        border: none;
        font-size: 13px;
        padding: 4px 12px;

        .el-icon {
          margin-right: 6px;
        }

        .el-tag__close {
          color: #fff;
          opacity: 0.8;

          &:hover {
            opacity: 1;
          }
        }
      }
    }

    .chat-input {
      padding: 14px 16px;
      background: #fff;
      border-top: 1px solid #e5e7eb;
      box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.05);

      .input-tools {
        display: flex;
        gap: 10px;
        margin-bottom: 10px;

        .file-input {
          display: none;
        }

        .el-button {
          transition: all 0.3s ease;

          &:hover {
            color: #667eea;
            transform: scale(1.1);
          }
        }
      }

      .el-input {
        border: 1px solid #e5e7eb;
        border-radius: 12px;
        transition: all 0.3s ease;

        &:hover {
          border-color: #667eea;
          box-shadow: 0 0 0 2px rgba(102, 126, 234, 0.1);
        }

        textarea {
          border-radius: 12px;
          resize: none;
        }
      }

      .input-actions {
        display: flex;
        justify-content: flex-end;
        margin-top: 10px;

        .el-button {
          background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
          border: none;
          border-radius: 12px;
          padding: 8px 20px;
          transition: all 0.3s ease;

          &:hover {
            transform: translateY(-2px);
            box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4);
          }

          &:disabled {
            background: #e5e7eb;
            color: #909399;
            transform: none;
            box-shadow: none;
          }
        }
      }
    }
  }
}

// 动画
@keyframes typing {
  0%, 100% {
    transform: translateY(0);
    opacity: 0.4;
  }
  50% {
    transform: translateY(-6px);
    opacity: 1;
  }
}

@keyframes pulse {
  0%, 100% {
    transform: scale(1);
    opacity: 1;
  }
  50% {
    transform: scale(1.05);
    opacity: 0.8;
  }
}

@keyframes bounce {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(-10px);
  }
}

@keyframes slideIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.slide-enter-active,
.slide-leave-active {
  transition: transform 0.3s ease, opacity 0.3s ease;
}

.slide-enter-from,
.slide-leave-to {
  transform: translateY(20px);
  opacity: 0;
}
</style>
