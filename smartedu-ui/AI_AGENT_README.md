# AI Agent 前端模块实现

## 实现时间
2026-04-08

## 修复记录

### 2026-04-08 - 修复 AgentStatus 导入问题
**问题**: `aiChat.ts` 中使用 `import type` 导入 `AgentStatus`，导致运行时无法访问枚举值

**错误**: `ReferenceError: Cannot access 'AgentStatus' at initialization`

**修复**: 将类型导入改为值导入
```typescript
// 修复前
import type { ChatMessage, MessageType, AgentStatus, ConversationHistory } from '@/api/ai'

// 修复后
import type { ChatMessage, MessageType, ConversationHistory } from '@/api/ai'
import { AgentStatus } from '@/api/ai'
```

## 文件清单

### 1. API 封装 (`src/api/ai.ts`)
- `MessageType` - 消息类型枚举（user, ai, system）
- `AgentStatus` - Agent 执行状态（thinking, calling_tool, success, error, idle）
- `ChatMessage` - 消息接口
- `ConversationHistory` - 对话历史接口
- `sendChatMessage()` - 发送普通消息
- `sendChatMessageWithFile()` - 发送消息并上传文件
- `sendChatMessageStream()` - 流式发送消息（SSE）
- `sendChatMessageWithFileStream()` - 流式发送消息并上传文件
- `getConversationHistory()` / `saveConversationHistory()` - 本地会话管理

### 2. Pinia Store (`src/stores/aiChat.ts`)
- 会话状态管理（sessionId, messages, agentStatus, isStreaming）
- 消息操作（addUserMessage, addAiMessage, addSystemMessage）
- 流式更新（updateStreamingContent, updateLastAiMessage）
- 持久化支持（sessionHistories）
- 角色适配欢迎语（getWelcomeMessage）

### 3. 聊天组件 (`src/components/ai/AIChatAssistant.vue`)
- 悬浮窗口设计（可最小化/关闭）
- 消息列表（支持 Markdown 渲染、代码高亮）
- Agent 状态卡片（思考中、执行中、成功、失败）
- 文件拖拽上传
- 流式响应显示（打字机动画）
- 会话历史管理

### 4. 全局集成
- 在 `src/layout/index.vue` 中引入组件
- 所有登录用户可见（学生端/教师端）

## 依赖安装
```bash
npm install markdown-it highlight.js @types/markdown-it @types/highlight.js
```

## 功能特性

### UI 设计
- ✅ Element Plus 风格
- ✅ 可最小化为悬浮图标
- ✅ 渐变色头部（#667eea → #764ba2）
- ✅ 消息气泡区分用户/AI/系统
- ✅ Agent 状态卡片（不同状态不同颜色）

### 流式通信
- ✅ SSE（Server-Sent Events）解析
- ✅ 打字机效果
- ✅ 自动滚动到底部
- ✅ 支持取消请求（AbortController）

### Agent 状态感知
- ✅ THINKING - 思考中（蓝色）
- ✅ CALLING_TOOL - 调用工具中（橙色）
- ✅ SUCCESS - 执行成功（绿色）
- ✅ ERROR - 执行失败（红色）

### 文件上传
- ✅ 点击上传按钮
- ✅ 拖拽上传
- ✅ 文件预览
- ✅ 支持 .doc, .docx, .pdf, .txt

### Markdown 渲染
- ✅ 使用 markdown-it 解析
- ✅ highlight.js 代码高亮
- ✅ 支持代码块、列表、引用等

### 权限适配
- ✅ 学生端：引导语强调"提交作业"
- ✅ 教师端：引导语强调"发布作业"

### 持久化
- ✅ Pinia persisted state
- ✅ localStorage 存储会话历史
- ✅ 刷新页面后恢复聊天

## 使用方法

### 组件自动显示
组件已集成到 layout，用户登录后自动在右下角显示悬浮按钮。

### 手动使用组件
```vue
import AIChatAssistant from '@/components/ai/AIChatAssistant.vue'

// 在 template 中使用
<AIChatAssistant />
```

### 使用 Store
```typescript
import { useAiChatStore } from '@/stores/aiChat'
import { AgentStatus } from '@/api/ai'

const aiChatStore = useAiChatStore()

// 初始化会话
aiChatStore.initSession()

// 发送消息
aiChatStore.addUserMessage('你好')

// 获取对话历史（用于 API 调用）
const history = aiChatStore.getConversationHistory()

// 设置 Agent 状态
aiChatStore.setAgentStatus(AgentStatus.THINKING)
```

### 使用 API
```typescript
import { sendChatMessage, sendChatMessageStream } from '@/api/ai'

// 普通请求
const response = await sendChatMessage('你好', history)

// 流式请求
const content = await sendChatMessageStream(
  '你好',
  history,
  (chunk) => {
    // 实时处理接收到的内容
    console.log('收到:', chunk)
  }
)
```

## 接口对接

### 后端接口
- `POST /api/ai/chat` - 普通聊天
- `POST /api/ai/chat/stream` - 流式聊天
- `POST /api/ai/chat/upload` - 带文件上传
- `POST /api/ai/chat/upload-stream` - 带文件上传（流式）

### 请求格式
```json
// Request Body (history)
[
  {"role": "user", "content": "你好"},
  {"role": "assistant", "content": "你好！有什么可以帮助你的？"}
]

// Response
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "message": "AI 响应内容",
    "timestamp": "2026-04-08T18:52:45.294630400"
  }
}
```

## 注意事项

1. **Token 认证**：流式请求需要手动处理 Authorization header
2. **SSE 格式**：Spring WebFlux 自动格式化为 `data: <content>\n\n`
3. **CORS**：确保后端配置了正确的 CORS 策略
4. **文件大小**：建议限制上传文件大小（<10MB）
5. **移动端适配**：当前针对桌面端优化，移动端需额外适配
