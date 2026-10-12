<template>
  <div
    class="smart-assistant-page"
    @dragenter.prevent="isDragOver = true"
    @dragover.prevent
    @dragleave.prevent="isDragOver = false"
    @drop.prevent="handleDrop"
  >
    <div class="assistant-shell">
      <header class="assistant-header">
        <div class="brand-block">
          <div class="brand-icon">
            <el-icon><ChatDotRound /></el-icon>
          </div>
          <div class="brand-text">
            <h1>智学助手</h1>
            <p>{{ pageSubtitle }}</p>
          </div>
        </div>
        <div class="header-actions">
          <span class="status-pill">{{ statusText }}</span>
          <el-button text @click="clearHistory">清空对话</el-button>
        </div>
      </header>

      <main ref="messageListRef" class="assistant-main">
        <div v-if="!aiChatStore.hasMessages" class="empty-state">
          <div class="empty-copy">
            <h2>{{ emptyTitle }}</h2>
            <p>{{ emptyDescription }}</p>
          </div>

          <div class="suggestion-grid">
            <button
              v-for="item in promptSuggestions"
              :key="item.title"
              type="button"
              class="suggestion-card"
              @click="useSuggestion(item.prompt)"
            >
              <span class="suggestion-title">{{ item.title }}</span>
              <span class="suggestion-text">{{ item.text }}</span>
              <span class="suggestion-template">{{ item.prompt }}</span>
            </button>
          </div>
        </div>

        <div v-else class="messages-list">
          <div
            v-for="message in aiChatStore.messages"
            :key="message.id"
            :class="['message-row', message.role]"
          >
            <div v-if="message.role !== 'user'" class="assistant-avatar">
              <el-icon v-if="message.role === 'system'"><Bell /></el-icon>
              <el-icon v-else><ChatDotRound /></el-icon>
            </div>

            <div :class="['message-card', message.role]">
              <template v-if="message.role === 'system'">
                <div class="system-label">执行状态</div>
                <div class="system-content">
                  {{ message.content }}
                </div>
              </template>
              <template v-else>
                <div class="message-markdown" v-html="renderMarkdown(message.content)"></div>
              </template>
              <div class="message-meta">
                <span>{{ message.role === 'user' ? '我' : '智学助手' }}</span>
                <span>{{ formatTime(message.timestamp) }}</span>
              </div>
            </div>
          </div>

          <div v-if="aiChatStore.isStreaming" class="message-row ai">
            <div class="assistant-avatar">
              <el-icon><ChatDotRound /></el-icon>
            </div>
            <div class="message-card ai loading-card">
              <div class="typing-indicator">
                <span></span>
                <span></span>
                <span></span>
              </div>
              <div class="message-meta">
                <span>智学助手</span>
                <span>正在生成</span>
              </div>
            </div>
          </div>
        </div>

        <transition name="fade">
          <div v-if="isDragOver" class="drag-overlay">
            <el-icon><Upload /></el-icon>
            <p>松开即可上传文件</p>
          </div>
        </transition>
      </main>

      <footer class="composer-panel">
        <div v-if="selectedFile" class="selected-file">
          <el-tag closable @close="clearSelectedFile">
            <el-icon><Document /></el-icon>
            {{ selectedFile.name }}
          </el-tag>
        </div>

        <div class="composer-row">
          <button type="button" class="attach-button" @click="triggerFileUpload" aria-label="上传文件">
            <el-icon><Plus /></el-icon>
          </button>

          <div class="composer-input">
            <el-input
              v-model="inputMessage"
              type="textarea"
              resize="none"
              :autosize="{ minRows: 1, maxRows: 6 }"
              :maxlength="2000"
              placeholder="输入消息，Shift + Enter 换行"
              @keydown.enter.exact.prevent="sendMessage"
            />
          </div>

          <button
            type="button"
            class="send-button"
            :disabled="!canSend"
            @click="sendMessage"
            aria-label="发送消息"
          >
            <el-icon v-if="!aiChatStore.isStreaming"><Promotion /></el-icon>
            <el-icon v-else class="rotating"><Loading /></el-icon>
          </button>
        </div>

        <input
          ref="fileInputRef"
          type="file"
          class="file-input"
          accept=".doc,.docx,.pdf,.txt"
          @change="handleFileSelect"
        />
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  Bell,
  ChatDotRound,
  Document,
  Loading,
  Plus,
  Promotion,
  Upload
} from '@element-plus/icons-vue'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import 'highlight.js/styles/atom-one-light.css'
import { useUserStore } from '@/store/modules/user'
import { useAiChatStore } from '@/stores/aiChat'
import {
  AgentStatus,
  sendChatMessageStream,
  sendChatMessageWithFileStream
} from '@/api/ai'

const userStore = useUserStore()
const aiChatStore = useAiChatStore()

const md = new MarkdownIt({
  html: true,
  linkify: true,
  breaks: true,
  typographer: true,
  highlight: (str: string, lang: string) => {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return `<pre class="hljs"><code>${hljs.highlight(str, { language: lang }).value}</code></pre>`
      } catch (_) {
        return `<pre class="hljs"><code>${md.utils.escapeHtml(str)}</code></pre>`
      }
    }
    return `<pre class="hljs"><code>${md.utils.escapeHtml(str)}</code></pre>`
  }
})

const inputMessage = ref('')
const selectedFile = ref<File | null>(null)
const isDragOver = ref(false)
const abortController = ref<AbortController | null>(null)

const messageListRef = ref<HTMLElement | null>(null)
const fileInputRef = ref<HTMLInputElement | null>(null)

const role = computed(() => userStore.userInfo?.role || '')
const isTeacherSide = computed(() => role.value === 'TEACHER' || role.value === 'ADMIN')
const canSend = computed(() => Boolean((inputMessage.value.trim() || selectedFile.value) && !aiChatStore.isStreaming))

const statusText = computed(() => {
  switch (aiChatStore.agentStatus) {
    case AgentStatus.THINKING:
      return '思考中'
    case AgentStatus.CALLING_TOOL:
      return '正在执行'
    case AgentStatus.SUCCESS:
      return '已完成'
    case AgentStatus.ERROR:
      return '需要重试'
    default:
      return '在线'
  }
})

const pageSubtitle = computed(() =>
  isTeacherSide.value
    ? '帮助你发布作业、梳理课堂材料与处理教学任务'
    : '帮助你提交作业、整理学习内容与拆解问题'
)

const emptyTitle = computed(() =>
  isTeacherSide.value ? '今天准备处理哪项教学任务？' : '今天想从哪项学习任务开始？'
)

const emptyDescription = computed(() =>
  isTeacherSide.value
    ? '你可以直接输入需求，或者先上传一份文件，再告诉我希望怎么处理。'
    : '你可以直接提问，或者上传作业文件后让我继续帮你提交与整理。'
)

const promptSuggestions = computed(() => {
  if (isTeacherSide.value) {
    return [
      {
        title: '发布作业',
        text: '先上传文件，再按模板补全课程名称、标题和截止时长。',
        prompt: '帮我发布作业（课程名称：）（作业标题：）（截止时间：7天）'
      },
      {
        title: '整理要求',
        text: '把已有作业要求整理成学生更容易执行的版本。',
        prompt: '帮我整理这份作业要求（目标课程：）（希望的说明风格：简洁/详细）'
      },
      {
        title: '课后提醒',
        text: '快速生成提醒学生的通知文案。',
        prompt: '帮我写一段提醒学生的通知（课程名称：）（提醒重点：）'
      }
    ]
  }

  return [
    {
      title: '提交作业',
      text: '如果不清楚作业 ID，也可以直接发，我会先帮你检查未提交作业。',
      prompt: '帮我提交作业（作业ID：）（答案内容：可选）'
    },
    {
      title: '拆解任务',
      text: '把作业要求拆成可以一步步完成的清单。',
      prompt: '帮我拆解这项任务（课程/作业名称：）（我卡住的点：）'
    },
    {
      title: '理解难点',
      text: '把知识点解释得更清楚一点。',
      prompt: '帮我解释这个问题（问题内容：）'
    }
  ]
})

onMounted(() => {
  if (!aiChatStore.sessionId) {
    aiChatStore.initSession()
  }
  nextTick(scrollToBottom)
})

watch(
  () => aiChatStore.messages.length,
  () => nextTick(scrollToBottom)
)

watch(
  () => aiChatStore.streamingContent,
  () => nextTick(scrollToBottom)
)

function renderMarkdown(content: string) {
  if (!content || !content.trim()) {
    return '<span class="empty-text">正在思考...</span>'
  }
  return md.render(content)
}

function formatTime(timestamp: number) {
  return new Date(timestamp).toLocaleTimeString('zh-CN', {
    hour: '2-digit',
    minute: '2-digit'
  })
}

function scrollToBottom() {
  if (messageListRef.value) {
    messageListRef.value.scrollTop = messageListRef.value.scrollHeight
  }
}

function triggerFileUpload() {
  fileInputRef.value?.click()
}

function handleFileSelect(event: Event) {
  const target = event.target as HTMLInputElement
  if (target.files?.[0]) {
    selectedFile.value = target.files[0]
  }
}

function clearSelectedFile() {
  selectedFile.value = null
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
  }
}

function handleDrop(event: DragEvent) {
  isDragOver.value = false
  const file = event.dataTransfer?.files?.[0]
  if (file) {
    selectedFile.value = file
  }
}

function useSuggestion(prompt: string) {
  inputMessage.value = prompt
  nextTick(() => {
    const textarea = document.querySelector('.composer-input textarea') as HTMLTextAreaElement | null
    textarea?.focus()
    if (textarea) {
      const placeholderStart = prompt.indexOf('（')
      const selection = placeholderStart >= 0 ? placeholderStart + 1 : prompt.length
      textarea.setSelectionRange(selection, selection)
    }
  })
}

async function sendMessage() {
  const message = inputMessage.value.trim()
  if (!message && !selectedFile.value) {
    return
  }

  if (aiChatStore.isStreaming) {
    abortController.value?.abort()
    aiChatStore.setStreaming(false)
    aiChatStore.setAgentStatus(AgentStatus.IDLE)
    return
  }

  abortController.value = new AbortController()
  const history = aiChatStore.getConversationHistory()

  if (message) {
    aiChatStore.addUserMessage(message)
  } else if (selectedFile.value) {
    aiChatStore.addUserMessage(`已上传文件：${selectedFile.value.name}`)
  }

  aiChatStore.addAiMessage('')
  aiChatStore.setAgentStatus(AgentStatus.THINKING)
  aiChatStore.setStreaming(true)

  try {
    if (selectedFile.value) {
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
      await sendChatMessageStream(
        message,
        history,
        (chunk) => {
          aiChatStore.updateStreamingContent(chunk)
        },
        abortController.value.signal
      )
    }

    aiChatStore.setAgentStatus(AgentStatus.SUCCESS)
  } catch (error: any) {
    if (error.name === 'AbortError') {
      aiChatStore.updateLastAiMessage('已取消')
      aiChatStore.setAgentStatus(AgentStatus.IDLE)
      return
    }

    let errorMessage = '抱歉，处理这条请求时出了点问题，请稍后再试。'
    if (error?.message) {
      if (error.message.includes('登录') || error.message.includes('过期')) {
        errorMessage = '登录状态已失效，请刷新页面重新登录。'
        ElMessage.warning('登录状态已失效，请刷新页面重新登录')
      } else if (error.message.includes('权限') || error.message.includes('403')) {
        errorMessage = '权限不足，请检查当前登录状态。'
      } else if (error.message.includes('服务器') || error.message.includes('500')) {
        errorMessage = '服务器暂时不可用，请稍后重试。'
      } else {
        errorMessage = `处理失败：${error.message}`
      }
    }

    aiChatStore.setAgentStatus(AgentStatus.ERROR)
    aiChatStore.updateLastAiMessage(errorMessage)
  } finally {
    aiChatStore.setStreaming(false)
    inputMessage.value = ''
    clearSelectedFile()
    abortController.value = null
  }
}

function clearHistory() {
  aiChatStore.clearCurrentSession()
  aiChatStore.initSession()
  ElMessage.success('已清空当前对话')
}
</script>

<style scoped lang="scss">
.smart-assistant-page {
  min-height: calc(100vh - 108px);
  background: #ffffff;
  padding: 24px 24px 28px;
}

.assistant-shell {
  max-width: 1180px;
  height: calc(100vh - 156px);
  min-height: 720px;
  margin: 0 auto;
  display: grid;
  grid-template-rows: auto 1fr auto;
  background: #ffffff;
}

.assistant-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
  padding: 8px 0 20px;
  border-bottom: 1px solid #eceff3;
}

.brand-block {
  display: flex;
  align-items: center;
  gap: 16px;
}

.brand-icon {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #f4f7fb;
  color: #111827;
  font-size: 24px;
}

.brand-text h1 {
  margin: 0;
  font-size: 30px;
  font-weight: 600;
  color: #111827;
}

.brand-text p {
  margin: 6px 0 0;
  font-size: 14px;
  color: #6b7280;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status-pill {
  display: inline-flex;
  align-items: center;
  height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  background: #f4f7fb;
  color: #4b5563;
  font-size: 13px;
}

.assistant-main {
  position: relative;
  overflow-y: auto;
  padding: 28px 0 20px;
}

.empty-state {
  max-width: 920px;
  margin: 72px auto 0;
}

.empty-copy {
  text-align: center;
  margin-bottom: 36px;
}

.empty-copy h2 {
  margin: 0 0 12px;
  font-size: 38px;
  font-weight: 500;
  color: #111827;
}

.empty-copy p {
  margin: 0;
  font-size: 16px;
  color: #6b7280;
  line-height: 1.7;
}

.suggestion-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.suggestion-card {
  text-align: left;
  border: 1px solid #eceff3;
  border-radius: 18px;
  padding: 18px 18px 20px;
  background: #ffffff;
  transition: border-color 0.2s ease, box-shadow 0.2s ease, transform 0.2s ease;
}

.suggestion-card:hover {
  border-color: #d6deeb;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
  transform: translateY(-1px);
}

.suggestion-title {
  display: block;
  margin-bottom: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #111827;
}

.suggestion-text {
  display: block;
  font-size: 13px;
  line-height: 1.7;
  color: #6b7280;
}

.suggestion-template {
  display: block;
  margin-top: 14px;
  font-size: 12px;
  line-height: 1.7;
  color: #374151;
  background: #f8fafc;
  border-radius: 12px;
  padding: 10px 12px;
  word-break: break-word;
}

.messages-list {
  display: flex;
  flex-direction: column;
  gap: 26px;
  width: min(860px, 100%);
  margin: 0 auto;
}

.message-row {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.message-row.user {
  justify-content: flex-end;
}

.assistant-avatar {
  width: 38px;
  height: 38px;
  flex-shrink: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: #f4f7fb;
  color: #111827;
  font-size: 18px;
}

.message-card {
  max-width: min(720px, calc(100% - 52px));
  padding: 18px 20px 14px;
  border-radius: 22px;
  background: #f7f8fa;
}

.message-card.user {
  background: #111827;
  color: #ffffff;
}

.message-card.system {
  background: #f4f7fb;
  border: 1px solid #e5e7eb;
}

.system-label {
  margin-bottom: 8px;
  font-size: 12px;
  color: #6b7280;
}

.system-content {
  font-size: 14px;
  line-height: 1.7;
  color: #111827;
}

.message-markdown {
  font-size: 15px;
  line-height: 1.8;
  color: inherit;
  word-break: break-word;
}

.message-markdown :deep(p) {
  margin: 0 0 10px;
}

.message-markdown :deep(p:last-child) {
  margin-bottom: 0;
}

.message-markdown :deep(pre) {
  margin: 12px 0;
  padding: 14px;
  border-radius: 14px;
  background: #ffffff;
  overflow-x: auto;
}

.message-card.user .message-markdown :deep(pre) {
  background: rgba(255, 255, 255, 0.14);
}

.message-markdown :deep(code) {
  font-size: 13px;
}

.message-markdown :deep(a) {
  color: #2563eb;
}

.message-card.user .message-markdown :deep(a) {
  color: #dbeafe;
}

.message-markdown :deep(ul),
.message-markdown :deep(ol) {
  padding-left: 20px;
  margin: 10px 0;
}

.message-markdown :deep(blockquote) {
  margin: 12px 0;
  padding-left: 12px;
  border-left: 3px solid #d1d5db;
  color: #6b7280;
}

.message-markdown :deep(.empty-text) {
  color: #6b7280;
  font-style: italic;
}

.message-meta {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  margin-top: 12px;
  font-size: 12px;
  color: #8a94a6;
}

.message-card.user .message-meta {
  color: rgba(255, 255, 255, 0.72);
}

.loading-card {
  min-width: 180px;
}

.typing-indicator {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 28px;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #9ca3af;
  animation: blink 1.4s infinite ease-in-out;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

.drag-overlay {
  position: absolute;
  inset: 12px 0 12px;
  border: 1px dashed #c7d2fe;
  border-radius: 24px;
  background: rgba(248, 250, 252, 0.94);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #4b5563;
}

.drag-overlay .el-icon {
  font-size: 36px;
}

.composer-panel {
  padding-top: 16px;
  border-top: 1px solid #eceff3;
  background: #ffffff;
}

.selected-file {
  width: min(860px, 100%);
  margin: 0 auto 12px;
}

.selected-file :deep(.el-tag) {
  height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  border-color: #dbe4f0;
  background: #f8fafc;
  color: #374151;
}

.composer-row {
  width: min(860px, 100%);
  margin: 0 auto;
  display: grid;
  grid-template-columns: 48px minmax(0, 1fr) 48px;
  align-items: end;
  gap: 12px;
}

.attach-button,
.send-button {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: 1px solid #e5e7eb;
  background: #ffffff;
  color: #111827;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s ease, border-color 0.2s ease, color 0.2s ease;
}

.attach-button:hover,
.send-button:hover:not(:disabled) {
  background: #f8fafc;
  border-color: #dbe4f0;
}

.send-button {
  background: #111827;
  border-color: #111827;
  color: #ffffff;
}

.send-button:hover:not(:disabled) {
  background: #1f2937;
  border-color: #1f2937;
}

.send-button:disabled {
  background: #e5e7eb;
  border-color: #e5e7eb;
  color: #9ca3af;
  cursor: not-allowed;
}

.composer-input :deep(.el-textarea__inner) {
  min-height: 56px !important;
  max-height: 180px;
  padding: 16px 18px;
  border-radius: 28px;
  border-color: #e5e7eb;
  box-shadow: none;
  font-size: 15px;
  line-height: 1.6;
}

.composer-input :deep(.el-textarea__inner:focus) {
  border-color: #cbd5e1;
}

.file-input {
  display: none;
}

.rotating {
  animation: spin 1s linear infinite;
}

@keyframes blink {
  0%,
  80%,
  100% {
    opacity: 0.3;
    transform: translateY(0);
  }
  40% {
    opacity: 1;
    transform: translateY(-2px);
  }
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 1024px) {
  .smart-assistant-page {
    padding: 18px 16px 22px;
  }

  .assistant-shell {
    height: calc(100vh - 138px);
    min-height: 680px;
  }

  .empty-copy h2 {
    font-size: 32px;
  }

  .suggestion-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .assistant-shell {
    min-height: calc(100vh - 128px);
    height: auto;
  }

  .assistant-header {
    flex-direction: column;
    align-items: stretch;
  }

  .header-actions {
    justify-content: space-between;
  }

  .brand-text h1 {
    font-size: 26px;
  }

  .empty-state {
    margin-top: 36px;
  }

  .empty-copy h2 {
    font-size: 28px;
  }

  .composer-row {
    grid-template-columns: 44px minmax(0, 1fr) 44px;
    gap: 10px;
  }

  .attach-button,
  .send-button {
    width: 44px;
    height: 44px;
  }

  .message-card {
    max-width: calc(100% - 52px);
  }
}
</style>
