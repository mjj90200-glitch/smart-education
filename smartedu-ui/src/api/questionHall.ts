/**
 * 答疑大厅 API
 * 纯人工问答讨论区，不包含 AI 功能
 */
import { get, post, del } from '@/utils/request'

// ==================== 类型定义 ====================

/** 帖子分类 */
export type TopicCategory = 'QUESTION' | 'DISCUSSION' | 'SHARE'

/** 用户角色 */
export type UserRole = 'STUDENT' | 'TEACHER' | 'ADMIN'

/** 帖子列表项 */
export interface TopicListItem {
  id: number
  title: string
  contentPreview: string
  category: TopicCategory
  userId: number
  userName: string
  userAvatar: string | null
  userRole: UserRole
  viewCount: number
  replyCount: number
  likeCount: number
  isSolved: number
  status: number
  createdAt: string
  isLiked: boolean
}

/** 帖子详情 */
export interface TopicDetail extends TopicListItem {
  content: string
  attachmentUrl: string | null
  acceptedReplyId: number | null
  updatedAt: string
  replies: ReplyItem[]
}

/** 回复项 */
export interface ReplyItem {
  id: number
  topicId: number
  userId: number
  userName: string
  userAvatar: string | null
  userRole: UserRole
  isTeacher: boolean
  content: string
  likeCount: number
  isAccepted: number
  parentReplyId: number | null
  attachmentUrl: string | null
  createdAt: string
  isLiked: boolean
  children: ReplyItem[]
}

/** 分页参数 */
export interface PageParams {
  pageNum?: number
  pageSize?: number
}

/** 列表查询参数 */
export interface TopicListParams extends PageParams {
  category?: TopicCategory
  keyword?: string
  status?: number
}

// ==================== API 接口 ====================

/** 获取帖子列表 */
export function getTopicList(params: TopicListParams) {
  return get<{ list: TopicListItem[]; total: number; pageNum: number; pageSize: number }>(
    '/question-hall/topics',
    params
  )
}

/** 获取帖子详情 */
export function getTopicDetail(id: number) {
  return get<TopicDetail>(`/question-hall/topics/${id}`)
}

/** 创建帖子 */
export function createTopic(data: {
  title: string
  content: string
  category?: TopicCategory
  file?: File
}) {
  const formData = new FormData()
  formData.append('title', data.title)
  formData.append('content', data.content)
  formData.append('category', data.category || 'QUESTION')
  if (data.file) {
    formData.append('file', data.file)
  }

  return post<number>('/question-hall/topics', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 删除帖子 */
export function deleteTopic(id: number) {
  return del<void>(`/question-hall/topics/${id}`)
}

/** 点赞帖子 */
export function likeTopic(id: number) {
  return post<{ isLiked: boolean; message: string }>(`/question-hall/topics/${id}/like`)
}

/** 创建回复 */
export function createReply(topicId: number, data: {
  content: string
  parentReplyId?: number
  file?: File
}) {
  const formData = new FormData()
  formData.append('content', data.content)
  if (data.parentReplyId) {
    formData.append('parentReplyId', String(data.parentReplyId))
  }
  if (data.file) {
    formData.append('file', data.file)
  }

  return post<number>(`/question-hall/topics/${topicId}/replies`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 删除回复 */
export function deleteReply(id: number) {
  return del<void>(`/question-hall/replies/${id}`)
}

/** 采纳答案 */
export function acceptReply(topicId: number, replyId: number) {
  return post<void>(`/question-hall/topics/${topicId}/accept/${replyId}`)
}

/** 点赞回复 */
export function likeReply(id: number) {
  return post<{ isLiked: boolean; message: string }>(`/question-hall/replies/${id}/like`)
}

// ==================== 管理员接口 ====================

/** 管理员删除帖子（逻辑删除） */
export function adminDeleteTopic(id: number) {
  return del<void>(`/question-hall/admin/topics/${id}`)
}

/** 管理员删除回复（逻辑删除） */
export function adminDeleteReply(id: number) {
  return del<void>(`/question-hall/admin/replies/${id}`)
}