import { post, postFile } from '@/utils/request'

/**
 * 消息类型枚举
 */
export enum MessageType {
  USER = 'user',
  AI = 'ai',
  SYSTEM = 'system'
}

/**
 * Agent 执行状态
 */
export enum AgentStatus {
  THINKING = 'thinking',
  CALLING_TOOL = 'calling_tool',
  SUCCESS = 'success',
  ERROR = 'error',
  IDLE = 'idle'
}

/**
 * 消息接口
 */
export interface ChatMessage {
  id: string
  role: MessageType
  content: string
  timestamp: number
  status?: AgentStatus
  toolName?: string
  toolResult?: any
}

/**
 * 对话历史接口
 */
export interface ConversationHistory {
  role: 'user' | 'assistant' | 'system'
  content: string
}

/**
 * AI 聊天响应接口
 */
export interface ChatResponse {
  message: string
  timestamp: string
}

function extractSseDataLine(line: string): string | null {
  if (!line.startsWith('data:')) {
    return null
  }
  const payload = line.slice(5)
  return payload.startsWith(' ') ? payload.slice(1) : payload
}

/**
 * 发送消息（普通文本）
 */
export function sendChatMessage(message: string, history: ConversationHistory[] = []) {
  return post<ChatResponse>('/ai/chat', { message, history }, {
    headers: {
      'Content-Type': 'application/json'
    }
  })
}

/**
 * 发送消息并上传文件
 */
export function sendChatMessageWithFile(message: string, file: File, history: ConversationHistory[] = []) {
  const formData = new FormData()
  formData.append('message', message)
  formData.append('file', file)

  // history 作为 JSON 字符串附加
  if (history.length > 0) {
    formData.append('history', JSON.stringify(history))
  }

  return postFile<ChatResponse>('/ai/chat/upload', formData)
}

/**
 * 流式发送消息
 * 使用 fetch API 直接处理 SSE 流
 *
 * 修复说明：
 * 1. 使用 POST + JSON body 格式发送请求
 * 2. 完善错误处理，对 401/403 提供友好提示
 * 3. 确保 finally 块强制关闭 Loading
 */
export async function sendChatMessageStream(
  message: string,
  history: ConversationHistory[] = [],
  onChunk: (text: string) => void,
  signal?: AbortSignal
): Promise<string> {
  // 获取 token
  const userStoreJson = localStorage.getItem('smartedu_user')
  let token: string | null = null
  if (userStoreJson) {
    try {
      const userStore = JSON.parse(userStoreJson)
      token = userStore.token
    } catch (e) {
      console.error('解析用户 store 失败:', e)
    }
  }

  // 检查 token 是否存在
  if (!token) {
    throw new Error('请先登录')
  }

  const headers: Record<string, string> = {
    'Accept': 'text/event-stream',
    'Authorization': `Bearer ${token}`,
    'Content-Type': 'application/json'
  }

  // ========== P1: 请求体修正 ==========
  // 使用 POST + JSON body 格式
  const response = await fetch(`${import.meta.env.VITE_API_BASE_URL || '/api'}/ai/chat/stream`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ message, history }),  // JSON body
    signal,
    credentials: 'include'
  })

  // ========== P1: 完善错误处理 ==========
  if (!response.ok) {
    let errorMessage = '请求失败'

    // 根据状态码提供友好提示
    if (response.status === 401) {
      throw new Error('登录已过期，请刷新页面重新登录')
    } else if (response.status === 403) {
      throw new Error('权限不足，请检查登录状态')
    } else if (response.status === 500) {
      throw new Error('服务器内部错误，请稍后重试')
    }

    // 尝试解析错误响应体
    try {
      const errorBody = await response.text()
      if (errorBody) {
        try {
          const errorJson = JSON.parse(errorBody)
          errorMessage = errorJson.message || errorJson.error || errorMessage
        } catch {
          errorMessage = errorBody
        }
      }
    } catch {
      // 无法读取错误响应体
    }

    throw new Error(`请求失败 (${response.status}): ${errorMessage}`)
  }

  const reader = response.body?.getReader()
  if (!reader) {
    throw new Error('无法获取响应流')
  }

  const decoder = new TextDecoder('utf-8')
  let fullContent = ''
  let buffer = ''  // 用于存储不完整的消息

  // ========== P0: 增强错误处理 + 详细日志 ==========
  let lastChunkTime = Date.now()
  let chunkCount = 0
  const CHUNK_TIMEOUT = 30000 // 30秒无新数据视为超时

  try {
    while (true) {
      const { done, value } = await reader.read()

      // 检查超时（如果流没有关闭但长时间无数据）
      const elapsed = Date.now() - lastChunkTime
      if (!done && elapsed > CHUNK_TIMEOUT) {
        console.warn('[SSE] 超过30秒无新数据，可能连接已中断')
        throw new Error('响应超时：超过30秒未收到新数据')
      }

      if (done) {
        console.log('[SSE] Stream done, chunks:', chunkCount, 'total length:', fullContent.length)
        break
      }

      lastChunkTime = Date.now()
      const decodedChunk = decoder.decode(value, { stream: true })
      chunkCount++
      console.debug('[SSE] Chunk #', chunkCount, ':', decodedChunk.length, 'bytes')

      buffer += decodedChunk

      // ========== P0: 正确解析 SSE 格式 ==========
      // SSE 消息以双换行符 (\n\n) 分隔
      // 每条消息可能包含多个 data: 行，需要拼接成一个内容

      const messages = buffer.split('\n\n')

      // 最后一个可能是不完整的消息，保留在 buffer 中
      buffer = messages.pop() || ''

      for (const message of messages) {
        if (!message.trim()) continue

        // 解析消息中的所有 data: 行
        const lines = message.split('\n')
        let dataContent = ''
        let isError = false
        let errorMessage = ''

        for (const line of lines) {
          const data = extractSseDataLine(line)
          if (data !== null) {

            // 检测错误 JSON
            if (data.startsWith('{') && data.includes('"error"')) {
              try {
                const parsed = JSON.parse(data)
                if (parsed.error) {
                  isError = true
                  errorMessage = parsed.error
                  break
                }
              } catch {
                // 解析失败，作为普通内容
              }
            }

            // 如果是多行 data，需要重新拼接换行符
            if (dataContent) {
              dataContent += '\n'
            }
            dataContent += data
          }
        }

        // 处理结果
        if (isError) {
          console.error('[SSE] Error received:', errorMessage)
          throw new Error(errorMessage)
        }

        if (dataContent && dataContent !== '[DONE]') {
          try {
            fullContent += dataContent
            onChunk(dataContent)
            console.debug('[SSE] Content appended, total:', fullContent.length, 'chars')
          } catch (e) {
            console.warn('[SSE] onChunk callback failed:', e)
            // 继续处理，不中断流
          }
        }
      }
    }

    // 处理 buffer 中剩余的内容
    if (buffer.trim()) {
      const lines = buffer.split('\n')
      let dataContent = ''

      for (const line of lines) {
        const data = extractSseDataLine(line)
        if (data !== null) {
          if (dataContent) {
            dataContent += '\n'
          }
          dataContent += data
        }
      }

      if (dataContent && dataContent !== '[DONE]') {
        // 检测错误
        if (dataContent.startsWith('{') && dataContent.includes('"error"')) {
          try {
            const parsed = JSON.parse(dataContent)
            if (parsed.error) {
              throw new Error(parsed.error)
            }
          } catch (e) {
            if (e instanceof Error) throw e
          }
        }
        fullContent += dataContent
        onChunk(dataContent)
      }
    }
  } finally {
    // 确保 reader 释放
    console.log('[SSE] Stream finished, final content length:', fullContent.length)
    try {
      reader.releaseLock()
      console.debug('[SSE] Reader released successfully')
    } catch (e) {
      console.warn('[SSE] Reader release failed (may already be released):', e)
    }
  }

  return fullContent
}

/**
 * 流式发送消息并上传文件
 */
export async function sendChatMessageWithFileStream(
  message: string,
  file: File,
  history: ConversationHistory[] = [],
  onChunk: (text: string) => void,
  signal?: AbortSignal
): Promise<string> {
  const userStoreJson = localStorage.getItem('smartedu_user')
  let token: string | null = null
  if (userStoreJson) {
    try {
      const userStore = JSON.parse(userStoreJson)
      token = userStore.token
    } catch (e) {
      console.error('解析用户 store 失败:', e)
    }
  }

  // 检查 token 是否存在
  if (!token) {
    throw new Error('请先登录')
  }

  const headers: Record<string, string> = {
    'Accept': 'text/event-stream',
    'Authorization': `Bearer ${token}`
  }

  const formData = new FormData()
  formData.append('message', message)
  formData.append('file', file)
  if (history.length > 0) {
    formData.append('history', JSON.stringify(history))
  }

  const response = await fetch(`${import.meta.env.VITE_API_BASE_URL || '/api'}/ai/chat/upload-stream`, {
    method: 'POST',
    headers,
    body: formData,
    signal,
    credentials: 'include'
  })

  // 完善错误处理
  if (!response.ok) {
    if (response.status === 401) {
      throw new Error('登录已过期，请刷新页面重新登录')
    } else if (response.status === 403) {
      throw new Error('权限不足，请检查登录状态')
    } else if (response.status === 500) {
      throw new Error('服务器内部错误，请稍后重试')
    }

    let errorMessage = '请求失败'
    try {
      const errorBody = await response.text()
      if (errorBody) {
        try {
          const errorJson = JSON.parse(errorBody)
          errorMessage = errorJson.message || errorJson.error || errorMessage
        } catch {
          errorMessage = errorBody
        }
      }
    } catch {
      // 无法读取错误响应体
    }

    throw new Error(`请求失败 (${response.status}): ${errorMessage}`)
  }

  const reader = response.body?.getReader()
  if (!reader) {
    throw new Error('无法获取响应流')
  }

  const decoder = new TextDecoder('utf-8')
  let fullContent = ''
  let buffer = ''

  try {
    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })

      // 正确解析 SSE 格式
      const messages = buffer.split('\n\n')
      buffer = messages.pop() || ''

      for (const message of messages) {
        if (!message.trim()) continue

        const lines = message.split('\n')
        let dataContent = ''
        let isError = false
        let errorMessage = ''

        for (const line of lines) {
          const data = extractSseDataLine(line)
          if (data !== null) {

            if (data.startsWith('{') && data.includes('"error"')) {
              try {
                const parsed = JSON.parse(data)
                if (parsed.error) {
                  isError = true
                  errorMessage = parsed.error
                  break
                }
              } catch {
                // 解析失败，作为普通内容
              }
            }

            if (dataContent) {
              dataContent += '\n'
            }
            dataContent += data
          }
        }

        if (isError) {
          throw new Error(errorMessage)
        }

        if (dataContent && dataContent !== '[DONE]') {
          fullContent += dataContent
          onChunk(dataContent)
        }
      }
    }

    // 处理剩余 buffer
    if (buffer.trim()) {
      const lines = buffer.split('\n')
      let dataContent = ''

      for (const line of lines) {
        const data = extractSseDataLine(line)
        if (data !== null) {
          if (dataContent) {
            dataContent += '\n'
          }
          dataContent += data
        }
      }

      if (dataContent && dataContent !== '[DONE]') {
        if (dataContent.startsWith('{') && dataContent.includes('"error"')) {
          try {
            const parsed = JSON.parse(dataContent)
            if (parsed.error) {
              throw new Error(parsed.error)
            }
          } catch (e) {
            if (e instanceof Error) throw e
          }
        }
        fullContent += dataContent
        onChunk(dataContent)
      }
    }
  } finally {
    try {
      reader.releaseLock()
    } catch {
      // reader 可能已释放
    }
  }

  return fullContent
}

/**
 * 获取会话历史（从本地存储）
 */
export function getConversationHistory(sessionId: string): ConversationHistory[] {
  const key = `ai_chat_history_${sessionId}`
  const stored = localStorage.getItem(key)
  return stored ? JSON.parse(stored) : []
}

/**
 * 保存会话历史到本地存储
 */
export function saveConversationHistory(sessionId: string, history: ConversationHistory[]) {
  const key = `ai_chat_history_${sessionId}`
  localStorage.setItem(key, JSON.stringify(history))
}

/**
 * 清除会话历史
 */
export function clearConversationHistory(sessionId: string) {
  const key = `ai_chat_history_${sessionId}`
  localStorage.removeItem(key)
}
