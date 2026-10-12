import { get, post, put, del } from '@/utils/request'

// 课程相关
export const courseApi = {
  // 获取所有课程列表（教师可见所有课程）
  getList: () => get('/course/list'),
  // 获取教师的课程
  getTeacherCourses: () => get('/course/teacher'),
  // 绑定当前教师教授的课程
  assignTeacherCourses: (courseIds: number[]) => post('/course/teacher/assign', { courseIds }),
  // 获取课程详情
  getDetail: (id: number) => get(`/course/${id}`),
  // 创建课程
  create: (data: { courseName: string; courseCode: string; description?: string; credit?: number; semester?: string; grade?: string; major?: string }) =>
    post('/course/create', data)
}

// 题目相关
export const questionApi = {
  // 获取题目列表
  getList: (params?: { courseId?: number; questionType?: string; difficultyLevel?: number; pageNum?: number; pageSize?: number }) =>
    get('/question/list', params),
  // 获取题目详情
  getDetail: (id: number) => get(`/question/${id}`),
  // 批量获取题目
  getByIds: (ids: string) => get('/question/by-ids', { ids }),
  // 导入题目文件
  import: async (file: File, courseId: number) => {
    // 从 localStorage 获取 token
    const userStoreJson = localStorage.getItem('smartedu_user')
    let token = ''
    if (userStoreJson) {
      try {
        const userStore = JSON.parse(userStoreJson)
        token = userStore.token || ''
      } catch (e) {
        console.error('解析用户 store 失败:', e)
      }
    }

    const formData = new FormData()
    formData.append('file', file)
    formData.append('courseId', String(courseId))

    const response = await fetch('/api/teacher/question/import', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`
      },
      body: formData
    })
    return response.json()
  }
}

// 问答相关
export const qaApi = {
  // 提问
  askQuestion: (data: any) => post('/qa/ask', data),
  // 获取问答列表
  getQAList: (params?: any) => get('/qa/list', params),
  // 获取问答详情
  getQADetail: (id: number) => get(`/qa/${id}`),
  // 回答问题
  answerQuestion: (id: number, content: string) => post(`/qa/${id}/answer`, { content }),
  // 采纳答案
  adoptAnswer: (id: number, answerUserId: number) => post(`/qa/${id}/adopt`, null, { params: { answerUserId } }),
  // 获取我的提问
  getMyQuestions: (params?: any) => get('/qa/my-questions', params),
  // 获取热门问题
  getHotQuestions: (params?: any) => get('/qa/hot-questions', params),
  // 点赞
  likeQA: (id: number) => post(`/qa/${id}/like`)
}

// 教师作业相关
export const homeworkApi = {
  // 创建作业
  create: (data: any) => post('/teacher/homework', data),
  // 更新作业
  update: (id: number, data: any) => put(`/teacher/homework/${id}`, data),
  // 删除作业
  delete: (id: number) => del(`/teacher/homework/${id}`),
  // 获取作业列表
  getList: (params?: any) => get('/teacher/homework/list', params),
  // 获取作业详情
  getDetail: (id: number) => get(`/teacher/homework/${id}`),
  // 发布作业
  publish: (id: number) => post(`/teacher/homework/${id}/publish`),
  // 快速发布作业（上传文档直接发布）
  quickPublish: async (formData: FormData) => {
    const userStoreJson = localStorage.getItem('smartedu_user')
    let token = ''
    if (userStoreJson) {
      try {
        const userStore = JSON.parse(userStoreJson)
        token = userStore.token || ''
      } catch (e) {
        console.error('解析用户 store 失败:', e)
      }
    }

    const response = await fetch('/api/homework/teacher/quick-publish', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${token}`
      },
      body: formData
    })
    return response.json()
  },
  // 批改单个提交
  gradeSubmission: (submissionId: number, score: number, comment?: string) => {
    const params: any = { score }
    if (comment !== undefined && comment !== null) {
      params.comment = comment
    }
    return post(`/teacher/homework/${submissionId}/grade`, null, { params })
  },
  // 获取提交列表
  getSubmissions: (id: number, params?: any) => get(`/teacher/homework/${id}/submissions`, params),
  // 获取作业统计
  getStatistics: (id: number) => get(`/teacher/homework/${id}/statistics`),
  // 自动批改（AI 批改）
  autoGrade: (id: number) => post(`/teacher/homework/${id}/auto-grade`),
  // 获取学生学情分析
  getStudentAnalysis: (params: { studentId: number; courseId?: number }) =>
    get('/teacher/homework/student-analysis', params),
  // 获取班级学情分析
  getClassAnalysis: (courseId?: number) => get('/teacher/homework/class-analysis', { courseId }),

  // ========== AI 解析相关接口 ==========
  // 获取 AI 解析内容
  getAiAnalysis: (homeworkId: number) => get(`/teacher/homework/${homeworkId}/ai-analysis`),
  // 审核并发布 AI 解析（使用 data 传递参数，避免 URL 过长）
  approveAiAnalysis: (homeworkId: number, editedContent?: string) => {
    const data: any = {}
    if (editedContent !== undefined && editedContent !== null && editedContent.trim() !== '') {
      data.editedContent = editedContent
    }
    return post(`/teacher/homework/${homeworkId}/ai-analysis/approve`, data)
  },
  // 重新生成 AI 解析
  retryAiAnalysis: (homeworkId: number) => post(`/teacher/homework/${homeworkId}/ai-analysis/retry`)
}

export const homeworkAnalysisApi = {
  getOverview: (courseId: number) => get('/teacher/homework/analysis/overview', { courseId }),
  getStudentDetail: (studentId: number, courseId: number) =>
    get(`/teacher/homework/analysis/student/${studentId}`, { courseId }),
  remindStudents: (data: { courseId: number; studentIds: number[]; message?: string }) =>
    post('/teacher/homework/analysis/remind', data)
}

export const classEvaluationApi = {
  getOverview: (params: { courseId: number; sessionDate?: string; weekNumber?: number }) =>
    get('/teacher/class-evaluation/overview', params),
  save: (data: any) => post('/teacher/class-evaluation/save', data),
  importExcel: async (file: File, data: { courseId: number; termStartDate?: string }) => {
    const userStoreJson = localStorage.getItem('smartedu_user')
    let token = ''
    if (userStoreJson) {
      try {
        const userStore = JSON.parse(userStoreJson)
        token = userStore.token || ''
      } catch (e) {
        console.error('解析用户 store 失败:', e)
      }
    }

    const formData = new FormData()
    formData.append('file', file)
    formData.append('courseId', String(data.courseId))
    if (data.termStartDate) {
      formData.append('termStartDate', data.termStartDate)
    }

    const response = await fetch('/api/teacher/class-evaluation/import', {
      method: 'POST',
      headers: token ? { Authorization: `Bearer ${token}` } : undefined,
      body: formData
    })
    return response.json()
  },
  exportExcel: async (params: { courseId: number; sessionDate?: string; weekNumber?: number }) => {
    const query = new URLSearchParams()
    query.set('courseId', String(params.courseId))
    if (params.sessionDate) {
      query.set('sessionDate', params.sessionDate)
    }
    if (params.weekNumber != null) {
      query.set('weekNumber', String(params.weekNumber))
    }

    const userStoreJson = localStorage.getItem('smartedu_user')
    let token = ''
    if (userStoreJson) {
      try {
        const userStore = JSON.parse(userStoreJson)
        token = userStore.token || ''
      } catch (e) {
        console.error('解析用户 store 失败:', e)
      }
    }

    const response = await fetch(`/api/teacher/class-evaluation/export?${query.toString()}`, {
      method: 'GET',
      headers: token ? { Authorization: `Bearer ${token}` } : undefined
    })

    if (!response.ok) {
      const text = await response.text()
      throw new Error(text || '导出失败')
    }

    return response.blob()
  }
}

// 教师备课相关
export const lessonPrepApi = {
  // 推荐备课资源
  recommendResources: (params?: any) => get('/teacher/lesson/resources', params),
  // 生成教案
  generateLessonPlan: (params: any) => get('/teacher/lesson/plan/generate', params),
  // 生成 PPT 大纲
  generatePPTOutline: (lessonPlanId: number) => get('/teacher/lesson/ppt/generate', { lessonPlanId }),
  // 上传资源
  uploadResource: (data: any) => post('/teacher/lesson/resource/upload', null, { params: data }),
  // 获取我的资源
  getMyResources: (params?: any) => get('/teacher/lesson/my-resources', params),
  // 获取课程教学统计
  getCourseStats: (courseId: number) => get(`/teacher/lesson/course-stats`, { courseId }),
  // 分析知识点教学情况
  analyzeKnowledgePoints: (courseId: number) => get('/teacher/lesson/knowledge-analysis', { courseId })
}

// AI 学情分析相关
export const analysisApi = {
  // 获取班级学情报告
  getClassReport: (courseId: number, params?: { homeworkId?: number; startDate?: string; endDate?: string }) =>
    get(`/ai/analysis/class/${courseId}`, params),
  // 获取学生个人报告
  getStudentReport: (studentId: number, courseId: number, reportType?: string) =>
    get(`/ai/analysis/student/${studentId}`, { courseId, reportType }),
  // 获取学习预警列表
  getWarnings: (courseId: number, warningType?: string) =>
    get('/ai/analysis/warnings', { courseId, warningType }),
  // 获取教学建议
  getTeachingSuggestion: (params: { homeworkId: number; courseId?: number }) =>
    post('/ai/analysis/teaching-suggestion', params),
  // 获取知识点掌握图谱
  getKnowledgeMap: (studentId: number, courseId: number) =>
    get(`/ai/analysis/knowledge-map/${studentId}`, { courseId })
}
