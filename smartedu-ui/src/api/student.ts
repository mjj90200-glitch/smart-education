import { get, post, put } from '@/utils/request'

// ==================== Dashboard 相关接口 ====================

// 获取学生 Dashboard 统计数据
export function getDashboardStats(userId: number) {
  return get('/student/learning/dashboard/stats', { userId })
}

// 获取学生学习看板
export function getLearningDashboard(userId: number, days?: number) {
  return get('/student/learning/dashboard', { userId, days })
}

export function saveLearningDashboardNote(
  userId: number,
  data: {
    date: string
    completedSummary: string
    pendingSummary: string
    aiToolSummary: string
    reflection: string
  }
) {
  return post('/student/learning/dashboard/note', data, { params: { userId } })
}

export function getLearningDashboardNote(userId: number, date: string) {
  return get('/student/learning/dashboard/note', { userId, date })
}







// 获取推荐练习题
export function getRecommendQuestions(count?: number) {
  return get('/student/learning/recommend-questions', { count })
}

// ==================== 作业相关接口 ====================

// 获取作业列表
export function getHomeworkList(params?: { courseId?: number; status?: number; pageNum?: number; pageSize?: number }) {
  return get('/student/homework/list', params)
}

// 获取作业详情
export function getHomeworkDetail(id: number) {
  return get(`/student/homework/${id}`)
}

// 提交作业
export function submitHomework(data: { homeworkId: number; answers: { questionId: number; answer: string }[] }) {
  return post('/student/homework/submit', data)
}

// 获取我的提交记录
export function getMySubmission(homeworkId: number) {
  return get(`/student/homework/submission/${homeworkId}`)
}

// ==================== 问答相关接口 ====================

export const qaApi = {
  // 提问
  askQuestion: (data: any) => post('/qa/ask', data),
  // 获取问答列表
  getQAList: (params?: any) => get('/qa/list', params),
  // 获取问答详情
  getQADetail: (id: number) => get(`/qa/${id}`),
  // 回答问题 - 使用 URL 参数传递 content
  answerQuestion: (id: number, content: string) => post(`/qa/${id}/answer?content=${encodeURIComponent(content)}`),
  // 采纳答案
  adoptAnswer: (id: number) => post(`/qa/${id}/adopt`),
  // 获取我的提问
  getMyQuestions: (params?: any) => get('/qa/my-questions', params),
  // 获取热门问题
  getHotQuestions: (params?: any) => get('/qa/hot-questions', params),
  // 点赞
  likeQA: (id: number) => post(`/qa/${id}/like`)
}

// ==================== 学情分析相关接口 ====================

// 获取学生学情分析数据
export function getStudentAnalysis(courseId: number) {
  return get('/student/learning/analysis', { courseId })
}

// 获取当前学生可查看课程
export function getStudentAnalysisCourses() {
  return get('/student/learning/courses')
}

// 获取老师提醒列表
export function getReminders(courseId?: number) {
  return get('/student/learning/reminders', courseId ? { courseId } : undefined)
}

// 标记提醒为已读
export function markReminderAsRead(reminderId: number) {
  return post(`/student/learning/reminders/${reminderId}/read`)
}

// 标记所有提醒为已读
export function markAllRemindersAsRead(courseId?: number) {
  return post('/student/learning/reminders/read-all', null, { params: courseId ? { courseId } : undefined })
}
