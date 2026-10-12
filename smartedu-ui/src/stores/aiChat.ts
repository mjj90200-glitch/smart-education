import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { ChatMessage, MessageType, ConversationHistory } from '@/api/ai'
import { AgentStatus } from '@/api/ai'

// 简单的 UUID 生成函数
function generateId(): string {
  return `id_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
}

/**
 * AI 聊天 Store
 * 管理聊天会话状态、消息历史、Agent 执行状态
 */
export const useAiChatStore = defineStore(
  'aiChat',
  () => {
    // 当前会话 ID
    const sessionId = ref<string>('')

    // 消息列表
    const messages = ref<ChatMessage[]>([])

    // Agent 当前状态
    const agentStatus = ref<AgentStatus>(AgentStatus.IDLE)

    // 是否正在流式接收
    const isStreaming = ref(false)

    // 当前正在生成的消息内容
    const streamingContent = ref('')

    // 会话历史（用于 Pinia 持久化）
    const sessionHistories = ref<Record<string, ChatMessage[]>>({})

    // 初始化会话
    const initSession = (newSessionId?: string) => {
      const id = newSessionId || `session_${Date.now()}`
      sessionId.value = id

      // 从持久化存储恢复消息
      if (sessionHistories.value[id]) {
        messages.value = sessionHistories.value[id]
      } else {
        messages.value = []
      }

      agentStatus.value = AgentStatus.IDLE
      isStreaming.value = false
      streamingContent.value = ''

      return id
    }

    // 添加用户消息
    const addUserMessage = (content: string): ChatMessage => {
      const message: ChatMessage = {
        id: generateId(),
        role: 'user' as MessageType,
        content,
        timestamp: Date.now()
      }
      messages.value.push(message)
      saveToHistory()
      return message
    }

    // 添加 AI 消息
    const addAiMessage = (content: string): ChatMessage => {
      const message: ChatMessage = {
        id: generateId(),
        role: 'ai' as MessageType,
        content,
        timestamp: Date.now(),
        status: agentStatus.value
      }
      messages.value.push(message)
      saveToHistory()
      return message
    }

    // 更新最后一条 AI 消息内容（用于流式更新）
    const updateLastAiMessage = (content: string) => {
      const lastAiMsg = messages.value.slice().reverse().find(m => m.role === 'ai')
      if (lastAiMsg) {
        lastAiMsg.content = content
        lastAiMsg.status = agentStatus.value
      }
    }

    // 设置 Agent 状态
    const setAgentStatus = (status: AgentStatus) => {
      agentStatus.value = status
    }

    // 设置流式状态
    const setStreaming = (streaming: boolean) => {
      isStreaming.value = streaming
      if (!streaming) {
        streamingContent.value = ''
      }
    }

    // 更新流式内容
    const updateStreamingContent = (content: string) => {
      streamingContent.value += content
      updateLastAiMessage(streamingContent.value)
    }

    // 添加系统消息
    const addSystemMessage = (content: string, status?: AgentStatus, toolName?: string, toolResult?: any) => {
      const message: ChatMessage = {
        id: generateId(),
        role: 'system' as MessageType,
        content,
        timestamp: Date.now(),
        status,
        toolName,
        toolResult
      }
      messages.value.push(message)
      saveToHistory()
      return message
    }

    // 清除当前会话
    const clearCurrentSession = () => {
      if (sessionId.value) {
        sessionHistories.value[sessionId.value] = []
      }
      messages.value = []
      agentStatus.value = AgentStatus.IDLE
      isStreaming.value = false
      streamingContent.value = ''
    }

    // 删除指定消息
    const deleteMessage = (messageId: string) => {
      const index = messages.value.findIndex(m => m.id === messageId)
      if (index !== -1) {
        messages.value.splice(index, 1)
        saveToHistory()
      }
    }

    // 保存到历史存储
    const saveToHistory = () => {
      if (sessionId.value) {
        sessionHistories.value[sessionId.value] = messages.value
      }
    }

    // 转换为对话历史格式（用于 API 调用）
    const getConversationHistory = (): ConversationHistory[] => {
      return messages.value
        .filter(m => m.role === 'user' || m.role === 'ai')
        .map(m => ({
          role: m.role === 'user' ? 'user' : 'assistant',
          content: m.content
        }))
    }

    // 计算属性
    const hasMessages = computed(() => messages.value.length > 0)
    const lastMessage = computed(() => messages.value[messages.value.length - 1])
    const isLoading = computed(() => isStreaming.value || agentStatus.value === AgentStatus.THINKING)

    // 根据用户角色生成欢迎语
    const getWelcomeMessage = (userRole?: string) => {
      if (!userRole) {
        return '你好！我是智学助手，请问有什么可以帮助你的？'
      }

      if (userRole === 'STUDENT') {
        return '你好！我是智学助手 📚\n\n我可以帮助你：\n• 提交作业\n• 解答学习问题\n\n你可以上传作业文档，告诉我"帮我提交"，我会协助你完成作业提交！'
      } else if (userRole === 'TEACHER') {
        return '你好！我是智学助手 👨‍🏫\n\n我可以帮助你：\n• 发布新作业\n• 解析教案文档\n\n你可以上传教案或作业文档，告诉我"帮我发布作业"，我会协助你完成！'
      }

      return '你好！我是智学助手，请问有什么可以帮助你的？'
    }

    return {
      // State
      sessionId,
      messages,
      agentStatus,
      isStreaming,
      streamingContent,
      sessionHistories,

      // Actions
      initSession,
      addUserMessage,
      addAiMessage,
      updateLastAiMessage,
      setAgentStatus,
      setStreaming,
      updateStreamingContent,
      addSystemMessage,
      clearCurrentSession,
      deleteMessage,
      getConversationHistory,

      // Computed
      hasMessages,
      lastMessage,
      isLoading,
      getWelcomeMessage
    }
  },
  {
    persist: {
      key: 'smartedu_ai_chat',
      storage: localStorage,
      paths: ['sessionHistories']
    }
  }
)
