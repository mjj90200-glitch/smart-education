// 学生端相关类型定义

// Dashboard 统计数据
export interface DashboardStats {
  ongoingHomeworkCount: number
  weeklyStudyHours: number
  pendingReviewCount: number
  averageAccuracy: number
  knowledgeGraphPreview: KnowledgeGraphPreview
  todoItems: TodoItem[]
  suggestions: LearningSuggestion[]
}

export interface LearningDashboardData {
  startDate: string
  endDate: string
  selectedDate: string
  summary: LearningDashboardSummary
  heatmapDays: LearningHeatmapDay[]
  selectedDay: LearningDayDetail
  selectedNote: LearningDailyNote
}

export interface LearningDashboardSummary {
  activeDays: number
  completedTaskCount: number
  gradedEventCount: number
  aiUsageCount: number
  unreadReminderCount: number
}

export interface LearningHeatmapDay {
  date: string
  activityLevel: number
  completedTaskCount: number
  aiUsageCount: number
  reminderCount: number
  gradedEventCount: number
  totalEventCount: number
  timelineEvents: LearningTimelineEvent[]
}

export interface LearningDayDetail {
  date: string
  completedTaskCount: number
  aiUsageCount: number
  reminderCount: number
  gradedEventCount: number
  totalEventCount: number
  timelineEvents: LearningTimelineEvent[]
}

export interface LearningTimelineEvent {
  id: string
  time: string
  type: 'task' | 'ai_usage' | 'reminder'
  status: 'completed' | 'graded' | 'read' | 'unread' | string
  title: string
  courseName?: string
  subtitle?: string
  detail?: string
  aiModel?: string
  actionSummary?: string
  resultSummary?: string
  scoreLabel?: string
}

export interface LearningDailyNote {
  id?: number
  date?: string
  completedSummary: string
  pendingSummary: string
  aiToolSummary: string
  reflection: string
  hasContent: boolean
}

// 薄弱知识点
export interface WeakKnowledgePoint {
  id: number
  name: string
  errorCount: number
  mastery: number
}

// 待办事项
export interface TodoItem {
  id: number
  title: string
  deadline: string
  priority: 'HIGH' | 'MEDIUM' | 'LOW'
  completed: boolean
  type: 'HOMEWORK' | 'REVIEW' | 'EXAM'
}

// 学习建议
export interface LearningSuggestion {
  title: string
  type: 'WEAK_POINT' | 'PLAN' | 'REVIEW'
  content: string
  priority: 'HIGH' | 'MEDIUM' | 'LOW'
}





// 问题详情
export interface QuestionDetail {
  id: number
  title: string
  content: string
  answer: string
  analysis: string
  knowledgePointId: number
  knowledgePointName: string
  difficulty: 'EASY' | 'MEDIUM' | 'HARD'
  type: 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'TRUE_FALSE' | 'FILL_IN' | 'SHORT_ANSWER'
  options?: QuestionOption[]
}

// 问题选项
export interface QuestionOption {
  key: string
  value: string
}

// 答疑大厅问题
export interface QAQuestion {
  id: number
  title: string
  content: string
  category: 'CONCEPT' | 'EXERCISE' | 'OTHER'
  courseId?: number
  courseName?: string
  userId: number
  userName: string
  userAvatar?: string
  isAnonymous: boolean
  status: 0 | 1 | 2 // 0-待回答，1-已回答，2-已采纳
  answerType?: 'AI' | 'TEACHER'
  viewCount: number
  likeCount: number
  answerCount: number
  createdAt: string
}

// 回答
export interface QAAnswer {
  id: number
  questionId: number
  content: string
  answerType: 'AI' | 'TEACHER' | 'STUDENT'
  userId: number
  userName: string
  userAvatar?: string
  isAdopted: boolean
  likeCount: number
  createdAt: string
}

// ==================== 作业相关类型 ====================

// 学生作业列表项
export interface StudentHomework {
  id: number
  title: string
  description: string
  courseId: number
  courseName: string
  teacherName: string
  totalScore: number
  startTime: string
  endTime: string
  questionCount: number
  submitLimit: number
  submittedCount: number
  status: number // 0-未开始 1-进行中 2-已截止
  submitStatus: number // 0-未提交 1-已提交未批改 2-已批改
  score?: number
  isLate?: number
}

// 作业详情
export interface HomeworkDetail {
  id: number
  title: string
  description: string
  courseId: number
  courseName: string
  teacherId: number
  teacherName: string
  questionCount: number
  questions: QuestionBrief[]
  totalScore: number
  passScore: number
  startTime: string
  endTime: string
  submitLimit: number
  timeLimitMinutes: number
  autoGrade: number
  status: number
  submittedCount: number
  gradedCount: number
  averageScore: number
  createdAt: string
}

// 题目简要信息
export interface QuestionBrief {
  id: number
  questionType: string
  title: string
  content: string
  difficultyLevel: number
  score: number
}

// 作业提交请求
export interface HomeworkSubmitRequest {
  homeworkId: number
  answers: AnswerItem[]
}

// 答案项
export interface AnswerItem {
  questionId: number
  answer: string
}

// 作业提交记录
export interface HomeworkSubmission {
  id: number
  homeworkId: number
  userId: number
  answers: string
  score?: number
  comment?: string
  gradeStatus: number
  submitTime: string
  gradeTime?: string
  gradeUserId?: number
  isLate: number
  createdAt: string
  updatedAt: string
}
