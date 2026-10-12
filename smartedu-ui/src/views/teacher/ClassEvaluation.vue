<template>
  <div class="class-evaluation page-container">
    <el-card class="toolbar-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <div class="page-title">课堂评估</div>
          <div class="page-subtitle">上传标准 18 周考勤成绩表，系统自动生成每周课堂评估</div>
        </div>
        <div class="toolbar-right">
          <el-select v-model="selectedCourseId" placeholder="选择课程" style="width: 220px">
            <el-option
              v-for="course in courseList"
              :key="course.id"
              :label="course.courseName"
              :value="course.id"
            />
          </el-select>
          <el-date-picker
            v-model="termStartDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="学期起始日期"
            style="width: 180px"
          />
          <el-upload
            :auto-upload="false"
            :show-file-list="false"
            accept=".xlsx,.xls"
            :on-change="handleTemplateChange"
          >
            <el-button type="primary">上传 Excel</el-button>
          </el-upload>
          <el-select v-model="selectedWeek" placeholder="选择周次" style="width: 140px" @change="loadOverview">
            <el-option
              v-for="week in availableWeeks"
              :key="week"
              :label="`第 ${week} 周`"
              :value="week"
            />
          </el-select>
          <el-button :loading="loading" @click="loadOverview">刷新</el-button>
          <el-button type="success" :loading="saving" @click="saveSession">保存调整</el-button>
          <el-button type="primary" :loading="exporting" @click="exportExcel">导出 Excel</el-button>
        </div>
      </div>
    </el-card>

    <el-alert
      title="导入说明"
      type="info"
      :closable="false"
      description="模板第一列为学号、第二列为姓名，第三列起为第1周到第18周。系统会把“优/良/及/缺/假”转换为课堂出勤与互动表现，老师仍可在导入后继续微调。"
    />

    <div v-loading="loading" class="page-body">
      <el-empty v-if="!selectedCourseId" description="请先选择课程并上传标准 Excel" />

      <template v-else>
        <el-card v-if="importSummary.fileName" class="import-card">
          <div class="import-meta">
            <div><strong>最近导入文件：</strong>{{ importSummary.fileName }}</div>
            <div><strong>已识别周次：</strong>{{ importSummary.importedWeeks.map((week: number) => `第${week}周`).join('、') || '暂无' }}</div>
            <div><strong>导入学生数：</strong>{{ importSummary.studentCount }}</div>
          </div>
        </el-card>

        <el-row :gutter="16" class="summary-row">
          <el-col :span="4">
            <el-card class="summary-card">
              <div class="summary-label">当前周次</div>
              <div class="summary-value">{{ selectedWeek ? `第${selectedWeek}周` : '-' }}</div>
            </el-card>
          </el-col>
          <el-col :span="4">
            <el-card class="summary-card">
              <div class="summary-label">应到人数</div>
              <div class="summary-value">{{ summary.studentCount }}</div>
            </el-card>
          </el-col>
          <el-col :span="4">
            <el-card class="summary-card">
              <div class="summary-label">到课率</div>
              <div class="summary-value">{{ formatNumber(summary.attendanceRate) }}%</div>
            </el-card>
          </el-col>
          <el-col :span="4">
            <el-card class="summary-card">
              <div class="summary-label">答题次数</div>
              <div class="summary-value">{{ summary.totalAnswerCount }}</div>
            </el-card>
          </el-col>
          <el-col :span="4">
            <el-card class="summary-card">
              <div class="summary-label">提问次数</div>
              <div class="summary-value">{{ summary.totalQuestionCount }}</div>
            </el-card>
          </el-col>
          <el-col :span="4">
            <el-card class="summary-card">
              <div class="summary-label">缺勤/请假</div>
              <div class="summary-value">{{ summary.absentCount + summary.leaveCount }}</div>
            </el-card>
          </el-col>
        </el-row>

        <el-card class="session-card">
          <template #header>
            <div class="card-header">
              <span>周次概览</span>
              <div class="header-meta">
                <el-tag v-if="overview.session.sourceFileName" type="info">{{ overview.session.sourceFileName }}</el-tag>
                <el-tag>{{ overview.course.courseName || '-' }}</el-tag>
              </div>
            </div>
          </template>
          <el-row :gutter="16">
            <el-col :span="8">
              <el-form-item label="课堂标题">
                <el-input v-model="sessionForm.sessionTitle" placeholder="课堂标题" />
              </el-form-item>
            </el-col>
            <el-col :span="8">
              <el-form-item label="授课主题">
                <el-input v-model="sessionForm.teachingTopic" placeholder="例如：第 6 讲课堂互动总结" />
              </el-form-item>
            </el-col>
            <el-col :span="4">
              <el-form-item label="开始时间">
                <el-time-picker
                  v-model="sessionForm.startTime"
                  value-format="HH:mm:ss"
                  format="HH:mm"
                  placeholder="开始"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
            <el-col :span="4">
              <el-form-item label="结束时间">
                <el-time-picker
                  v-model="sessionForm.endTime"
                  value-format="HH:mm:ss"
                  format="HH:mm"
                  placeholder="结束"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-card>

        <el-row :gutter="16" class="content-row">
          <el-col :span="16">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>学生周课堂记录</span>
                  <el-tag type="info">{{ studentRows.length }} 人</el-tag>
                </div>
              </template>
              <el-table :data="studentRows" empty-text="请先上传 Excel 模板">
                <el-table-column prop="studentNo" label="学号" width="120" />
                <el-table-column prop="studentName" label="姓名" width="120" />
                <el-table-column prop="className" label="班级" min-width="120" />
                <el-table-column prop="performanceLevel" label="模板标记" width="100">
                  <template #default="scope">
                    <el-tag size="small" :type="performanceTagType(scope.row.performanceLevel)">
                      {{ scope.row.performanceLevel || '-' }}
                    </el-tag>
                  </template>
                </el-table-column>
                <el-table-column label="出勤状态" width="140">
                  <template #default="scope">
                    <el-select v-model="scope.row.attendanceStatus" size="small" @change="recalculateSummary">
                      <el-option v-for="item in attendanceOptions" :key="item.value" :label="item.label" :value="item.value" />
                    </el-select>
                  </template>
                </el-table-column>
                <el-table-column label="答题次数" width="120">
                  <template #default="scope">
                    <el-input-number v-model="scope.row.answerCount" :min="0" :max="99" size="small" @change="recalculateRow(scope.row)" />
                  </template>
                </el-table-column>
                <el-table-column label="提问次数" width="120">
                  <template #default="scope">
                    <el-input-number v-model="scope.row.questionCount" :min="0" :max="99" size="small" @change="recalculateRow(scope.row)" />
                  </template>
                </el-table-column>
                <el-table-column label="参与分" width="120">
                  <template #default="scope">
                    <el-input-number v-model="scope.row.participationScore" :min="0" :max="100" size="small" @change="recalculateSummary" />
                  </template>
                </el-table-column>
                <el-table-column label="缺勤原因" min-width="160">
                  <template #default="scope">
                    <el-input v-model="scope.row.absentReason" size="small" placeholder="缺勤/请假原因" />
                  </template>
                </el-table-column>
                <el-table-column label="教师备注" min-width="220">
                  <template #default="scope">
                    <el-input v-model="scope.row.teacherComment" size="small" placeholder="课堂观察备注" />
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>

          <el-col :span="8">
            <el-card class="insight-card">
              <template #header>
                <div class="card-header">
                  <span>课堂分析</span>
                </div>
              </template>

              <div class="insight-group">
                <div class="insight-title">高互动学生</div>
                <el-empty v-if="!insights.topParticipants.length" description="暂无数据" :image-size="60" />
                <div v-else class="chip-list">
                  <el-tag
                    v-for="item in insights.topParticipants.slice(0, 5)"
                    :key="`${item.studentNo}-${item.studentName}`"
                    type="success"
                  >
                    {{ item.studentName }} {{ item.answerCount }}/{{ item.questionCount }}
                  </el-tag>
                </div>
              </div>

              <div class="insight-group">
                <div class="insight-title">缺勤/请假学生</div>
                <el-empty v-if="!insights.absentStudents.length" description="暂无" :image-size="60" />
                <div v-else class="name-list">
                  <div v-for="item in insights.absentStudents" :key="`${item.studentNo}-${item.studentName}`" class="name-item">
                    <span>{{ item.studentName }}</span>
                    <span class="muted">{{ attendanceLabel(item.attendanceStatus) }}</span>
                  </div>
                </div>
              </div>

              <div class="insight-group">
                <div class="insight-title">需关注学生</div>
                <el-empty v-if="!insights.lowParticipationStudents.length" description="暂无" :image-size="60" />
                <div v-else class="name-list">
                  <div v-for="item in insights.lowParticipationStudents" :key="`${item.studentNo}-${item.studentName}`" class="name-item">
                    <span>{{ item.studentName }}</span>
                    <span class="muted">参与分 {{ item.participationScore }}</span>
                  </div>
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-card>
          <template #header>
            <div class="card-header">
              <span>整场课堂结论</span>
            </div>
          </template>
          <el-input
            v-model="sessionForm.analysisSummary"
            type="textarea"
            :rows="4"
            placeholder="系统会按导入结果生成分析，也可以手动调整。"
          />
        </el-card>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadFile } from 'element-plus'
import { classEvaluationApi, courseApi } from '@/api/teacher'

const loading = ref(false)
const saving = ref(false)
const exporting = ref(false)
const selectedCourseId = ref<number | null>(null)
const selectedWeek = ref<number | null>(null)
const termStartDate = ref('')
const availableWeeks = ref<number[]>([])
const courseList = ref<any[]>([])
const studentRows = ref<any[]>([])

const importSummary = reactive({
  fileName: '',
  importedWeeks: [] as number[],
  studentCount: 0
})

const overview = reactive({
  course: {
    id: null,
    courseName: '',
    courseCode: '',
    semester: ''
  },
  session: {
    id: null as number | null,
    sourceFileName: ''
  }
})

const sessionForm = reactive({
  sessionTitle: '',
  teachingTopic: '',
  startTime: '',
  endTime: '',
  analysisSummary: ''
})

const summary = reactive({
  studentCount: 0,
  presentCount: 0,
  lateCount: 0,
  absentCount: 0,
  leaveCount: 0,
  attendanceRate: 0,
  totalAnswerCount: 0,
  totalQuestionCount: 0,
  totalInteractionCount: 0,
  highInteractionCount: 0,
  averageParticipationScore: 0
})

const insights = reactive({
  topParticipants: [] as any[],
  absentStudents: [] as any[],
  lowParticipationStudents: [] as any[]
})

const attendanceOptions = [
  { label: '未标记', value: 'UNMARKED' },
  { label: '到课', value: 'PRESENT' },
  { label: '迟到', value: 'LATE' },
  { label: '缺勤', value: 'ABSENT' },
  { label: '请假', value: 'LEAVE' }
]

const loadCourses = async () => {
  const res = await courseApi.getTeacherCourses()
  if (res.code === 200) {
    courseList.value = Array.isArray(res.data) ? res.data : []
    if (!selectedCourseId.value && courseList.value.length > 0) {
      selectedCourseId.value = courseList.value[0].id
    }
  }
}

const applyOverview = (data: any) => {
  overview.course = data.course || overview.course
  overview.session.id = data.session?.id ?? null
  overview.session.sourceFileName = data.session?.sourceFileName || ''
  sessionForm.sessionTitle = data.session?.sessionTitle || `${overview.course.courseName || ''} 课堂评估`
  sessionForm.teachingTopic = data.session?.teachingTopic || ''
  sessionForm.startTime = data.session?.startTime || ''
  sessionForm.endTime = data.session?.endTime || ''
  sessionForm.analysisSummary = data.session?.analysisSummary || ''
  studentRows.value = Array.isArray(data.students) ? data.students.map((item: any) => ({ ...item })) : []
  availableWeeks.value = Array.isArray(data.availableWeeks) ? data.availableWeeks : []
  if (data.session?.weekNumber != null) {
    selectedWeek.value = data.session.weekNumber
  } else if (!selectedWeek.value && availableWeeks.value.length > 0) {
    selectedWeek.value = availableWeeks.value[0]
  }
  Object.assign(summary, data.summary || {})
  Object.assign(insights, data.insights || {
    topParticipants: [],
    absentStudents: [],
    lowParticipationStudents: []
  })
}

const loadOverview = async () => {
  if (!selectedCourseId.value) return

  loading.value = true
  try {
    const res = await classEvaluationApi.getOverview({
      courseId: selectedCourseId.value,
      weekNumber: selectedWeek.value || undefined
    })
    if (res.code === 200) {
      applyOverview(res.data)
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '加载课堂评估失败')
  } finally {
    loading.value = false
  }
}

const handleTemplateChange = async (uploadFile: UploadFile) => {
  if (!selectedCourseId.value) {
    ElMessage.warning('请先选择课程')
    return
  }
  if (!uploadFile.raw) {
    ElMessage.warning('未读取到上传文件')
    return
  }

  loading.value = true
  try {
    const res = await classEvaluationApi.importExcel(uploadFile.raw, {
      courseId: selectedCourseId.value,
      termStartDate: termStartDate.value || undefined
    })
    if (res.code === 200) {
      applyOverview(res.data)
      Object.assign(importSummary, res.data.importSummary || {
        fileName: uploadFile.name,
        importedWeeks: [],
        studentCount: 0
      })
      ElMessage.success('Excel 导入成功')
    } else {
      ElMessage.error(res.message || '导入失败')
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '导入失败')
  } finally {
    loading.value = false
  }
}

const recalculateRow = (row: any) => {
  row.answerCount = Number(row.answerCount || 0)
  row.questionCount = Number(row.questionCount || 0)
  recalculateSummary()
}

const rebuildAnalysisSummary = () => {
  const texts: string[] = []
  texts.push(`第${selectedWeek.value || '-'}周课堂到课率为 ${formatNumber(summary.attendanceRate)}%，答题 ${summary.totalAnswerCount} 次，提问 ${summary.totalQuestionCount} 次。`)
  if (summary.averageParticipationScore >= 80) {
    texts.push('整体课堂参与度较高。')
  } else if (summary.averageParticipationScore >= 60) {
    texts.push('整体课堂参与度中等，可以进一步增加互动。')
  } else {
    texts.push('整体课堂参与度偏低，建议重点带动沉默学生。')
  }
  if (insights.absentStudents.length > 0) {
    texts.push(`需关注缺勤/请假学生：${insights.absentStudents.map((item: any) => item.studentName).join('、')}。`)
  }
  sessionForm.analysisSummary = texts.join(' ')
}

const recalculateSummary = () => {
  const rows = studentRows.value
  summary.studentCount = rows.length
  summary.presentCount = rows.filter(item => item.attendanceStatus === 'PRESENT').length
  summary.lateCount = rows.filter(item => item.attendanceStatus === 'LATE').length
  summary.absentCount = rows.filter(item => item.attendanceStatus === 'ABSENT').length
  summary.leaveCount = rows.filter(item => item.attendanceStatus === 'LEAVE').length
  summary.totalAnswerCount = rows.reduce((sum, item) => sum + Number(item.answerCount || 0), 0)
  summary.totalQuestionCount = rows.reduce((sum, item) => sum + Number(item.questionCount || 0), 0)
  summary.totalInteractionCount = summary.totalAnswerCount + summary.totalQuestionCount
  summary.highInteractionCount = rows.filter(item => Number(item.answerCount || 0) + Number(item.questionCount || 0) >= 3 || Number(item.participationScore || 0) >= 80).length
  summary.attendanceRate = rows.length ? Number((((summary.presentCount + summary.lateCount) * 100) / rows.length).toFixed(2)) : 0
  summary.averageParticipationScore = rows.length
    ? Number((rows.reduce((sum, item) => sum + Number(item.participationScore || 0), 0) / rows.length).toFixed(2))
    : 0

  insights.topParticipants = [...rows]
    .sort((a, b) => Number(b.participationScore || 0) - Number(a.participationScore || 0) || (Number(b.answerCount || 0) + Number(b.questionCount || 0)) - (Number(a.answerCount || 0) + Number(a.questionCount || 0)))
    .slice(0, 5)
  insights.absentStudents = rows.filter(item => item.attendanceStatus === 'ABSENT' || item.attendanceStatus === 'LEAVE')
  insights.lowParticipationStudents = rows.filter(item =>
    item.attendanceStatus !== 'ABSENT' &&
    item.attendanceStatus !== 'LEAVE' &&
    Number(item.answerCount || 0) + Number(item.questionCount || 0) === 0 &&
    Number(item.participationScore || 0) < 60
  )

  rebuildAnalysisSummary()
}

const saveSession = async () => {
  if (!selectedCourseId.value) {
    ElMessage.warning('请先选择课程')
    return
  }

  saving.value = true
  try {
    const baseDate = termStartDate.value || new Date().toISOString().slice(0, 10)
    const res = await classEvaluationApi.save({
      courseId: selectedCourseId.value,
      weekNumber: selectedWeek.value,
      sessionDate: baseDate,
      sessionTitle: sessionForm.sessionTitle,
      teachingTopic: sessionForm.teachingTopic,
      startTime: sessionForm.startTime || null,
      endTime: sessionForm.endTime || null,
      analysisSummary: sessionForm.analysisSummary,
      students: studentRows.value.map(item => ({
        studentId: item.studentId,
        studentNo: item.studentNo,
        studentName: item.studentName,
        className: item.className,
        performanceLevel: item.performanceLevel,
        attendanceStatus: item.attendanceStatus,
        answerCount: Number(item.answerCount || 0),
        questionCount: Number(item.questionCount || 0),
        participationScore: Number(item.participationScore || 0),
        teacherComment: item.teacherComment,
        absentReason: item.absentReason
      }))
    })
    if (res.code === 200) {
      applyOverview(res.data)
      ElMessage.success('课堂评估已保存')
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

const exportExcel = async () => {
  if (!selectedCourseId.value) {
    ElMessage.warning('请先选择课程')
    return
  }
  if (!selectedWeek.value) {
    ElMessage.warning('请先选择周次')
    return
  }

  exporting.value = true
  try {
    const blob = await classEvaluationApi.exportExcel({
      courseId: selectedCourseId.value,
      weekNumber: selectedWeek.value
    })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `class-evaluation-week-${selectedWeek.value}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (error: any) {
    ElMessage.error(error?.message || '导出失败')
  } finally {
    exporting.value = false
  }
}

const attendanceLabel = (status: string) => attendanceOptions.find(item => item.value === status)?.label || '未标记'

const performanceTagType = (level: string) => {
  if (level === '优') return 'success'
  if (level === '良') return 'primary'
  if (level === '及') return 'warning'
  if (level === '缺') return 'danger'
  if (level === '假') return 'info'
  return ''
}

const formatNumber = (value: number | string) => Number(value || 0).toFixed(2)

onMounted(async () => {
  await loadCourses()
  if (selectedCourseId.value) {
    await loadOverview()
  }
})

watch(selectedCourseId, async (courseId, previousCourseId) => {
  if (!courseId) return
  if (courseId !== previousCourseId) {
    selectedWeek.value = null
    Object.assign(importSummary, {
      fileName: '',
      importedWeeks: [],
      studentCount: 0
    })
  }
  await loadOverview()
})
</script>

<style scoped lang="scss">
.class-evaluation {
  display: flex;
  flex-direction: column;
  gap: 16px;

  .toolbar-card,
  .session-card,
  .import-card {
    border-radius: 8px;
  }

  .toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;

    .toolbar-left {
      display: flex;
      flex-direction: column;
      align-items: flex-start;
      gap: 6px;
    }

    .toolbar-right {
      display: flex;
      align-items: center;
      gap: 12px;
      flex-wrap: wrap;
    }
  }

  .page-title {
    font-size: 20px;
    font-weight: 700;
    color: #1f2937;
  }

  .page-subtitle {
    font-size: 14px;
    color: #6b7280;
  }

  .page-body {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .import-meta {
    display: flex;
    flex-wrap: wrap;
    gap: 20px;
    font-size: 14px;
    color: #374151;
  }

  .summary-card {
    min-height: 116px;
  }

  .summary-label {
    font-size: 14px;
    color: #6b7280;
    margin-bottom: 18px;
  }

  .summary-value {
    font-size: 30px;
    line-height: 1.1;
    font-weight: 700;
    color: #111827;
  }

  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 12px;
  }

  .header-meta {
    display: flex;
    gap: 8px;
    flex-wrap: wrap;
  }

  .insight-card {
    height: 100%;
  }

  .insight-group + .insight-group {
    margin-top: 20px;
  }

  .insight-title {
    font-size: 14px;
    font-weight: 600;
    color: #374151;
    margin-bottom: 10px;
  }

  .chip-list {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }

  .name-list {
    display: flex;
    flex-direction: column;
    gap: 10px;
  }

  .name-item {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    padding: 10px 12px;
    border-radius: 8px;
    background: #f8fafc;
    color: #111827;
  }

  .muted {
    color: #6b7280;
    flex-shrink: 0;
  }
}
</style>
