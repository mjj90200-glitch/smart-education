<template>
  <div class="learning-analysis page-container">
    <el-card class="toolbar-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <div class="page-title">我的学情分析</div>
          <div class="page-subtitle">查看作业完成情况、平均成绩、绩点和老师提醒</div>
        </div>
        <div class="toolbar-right">
          <el-select
            v-model="selectedCourseId"
            placeholder="选择课程"
            style="width: 260px"
            @change="loadPageData"
          >
            <el-option
              v-for="course in courseList"
              :key="course.id"
              :label="course.courseName"
              :value="course.id"
            />
          </el-select>
          <el-button type="primary" :loading="loading" @click="loadPageData">刷新</el-button>
        </div>
      </div>
    </el-card>

    <div v-loading="loading">
      <el-empty
        v-if="!selectedCourseId"
        description="当前还没有可查看的课程"
      />

      <template v-else>
        <el-row :gutter="16" class="summary-row">
          <el-col :span="4">
            <el-card class="summary-card">
              <div class="summary-label">总作业数</div>
              <div class="summary-value">{{ summary.totalHomework }}</div>
            </el-card>
          </el-col>
          <el-col :span="4">
            <el-card class="summary-card">
              <div class="summary-label">已完成</div>
              <div class="summary-value">{{ summary.completedHomework }}</div>
            </el-card>
          </el-col>
          <el-col :span="4">
            <el-card class="summary-card">
              <div class="summary-label">平均成绩</div>
              <div class="summary-value">{{ formatScore(summary.averageScore) }}</div>
            </el-card>
          </el-col>
          <el-col :span="4">
            <el-card class="summary-card">
              <div class="summary-label">GPA</div>
              <div class="summary-value">{{ formatScore(summary.gpa) }}</div>
            </el-card>
          </el-col>
          <el-col :span="4">
            <el-card class="summary-card">
              <div class="summary-label">完成率</div>
              <div class="summary-value">{{ formatPercent(summary.completionRate) }}%</div>
            </el-card>
          </el-col>
          <el-col :span="4">
            <el-card class="summary-card attention">
              <div class="summary-label">未读提醒</div>
              <div class="summary-value">{{ summary.unreadReminderCount }}</div>
            </el-card>
          </el-col>
        </el-row>

        <el-row :gutter="16" class="content-row">
          <el-col :span="15">
            <el-card>
              <template #header>
                <div class="card-header">
                  <span>成绩趋势</span>
                  <span class="header-note">{{ summary.courseName }}</span>
                </div>
              </template>
              <div ref="trendChartRef" class="chart"></div>
            </el-card>
          </el-col>
          <el-col :span="9">
            <el-card class="insight-card">
              <template #header>
                <div class="card-header">
                  <span>当前状态</span>
                  <el-tag :type="scoreTagType(summary.level)">{{ summary.level || '待评分' }}</el-tag>
                </div>
              </template>
              <div class="insight-block">
                <div class="insight-item">
                  <span class="insight-label">已出分作业</span>
                  <span class="insight-value">{{ summary.gradedHomework }}</span>
                </div>
                <div class="insight-item">
                  <span class="insight-label">及格次数</span>
                  <span class="insight-value">{{ summary.passCount }}</span>
                </div>
                <div class="insight-item">
                  <span class="insight-label">迟交次数</span>
                  <span class="insight-value">{{ summary.lateCount }}</span>
                </div>
              </div>
              <div class="progress-block">
                <div class="progress-label">作业完成进度</div>
                <el-progress :percentage="toPercent(summary.completionRate)" :stroke-width="10" />
              </div>
              <div class="suggestion-list">
                <div v-for="(item, index) in suggestions" :key="index" class="suggestion-item">
                  {{ item }}
                </div>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-card class="content-card">
          <template #header>
            <div class="card-header">
              <span>作业成绩与提交情况</span>
            </div>
          </template>
          <el-table :data="homeworkList" empty-text="当前课程还没有已发布作业">
            <el-table-column prop="title" label="作业" min-width="200" />
            <el-table-column prop="endTime" label="截止时间" width="170" />
            <el-table-column prop="submitStatus" label="提交状态" width="110">
              <template #default="scope">
                <el-tag :type="homeworkTagType(scope.row.submitStatus)">
                  {{ scope.row.submitStatus }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="gradeStatusLabel" label="评分状态" width="110" />
            <el-table-column prop="submitTime" label="提交时间" width="170" />
            <el-table-column label="成绩" width="100">
              <template #default="scope">
                {{ scope.row.score == null ? '-' : scope.row.score }}
              </template>
            </el-table-column>
            <el-table-column label="是否及格" width="100">
              <template #default="scope">
                <el-tag v-if="scope.row.score != null" :type="scope.row.passed ? 'success' : 'danger'">
                  {{ scope.row.passed ? '及格' : '未及格' }}
                </el-tag>
                <span v-else>-</span>
              </template>
            </el-table-column>
            <el-table-column prop="comment" label="老师评语" min-width="220" show-overflow-tooltip>
              <template #default="scope">
                {{ scope.row.comment || '暂无评语' }}
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card class="content-card">
          <template #header>
            <div class="card-header">
              <span>老师提醒</span>
              <el-button
                type="primary"
                size="small"
                :disabled="unreadCount === 0"
                @click="markAllAsRead"
              >
                全部标记为已读
              </el-button>
            </div>
          </template>
          <el-empty v-if="reminders.length === 0" description="当前课程暂无老师提醒" />
          <div v-else class="reminder-list">
            <div
              v-for="reminder in reminders"
              :key="reminder.id"
              :class="['reminder-item', { unread: !reminder.read }]"
            >
              <div class="reminder-main">
                <div class="reminder-header">
                  <div class="reminder-title">{{ reminder.title }}</div>
                  <el-tag size="small" :type="reminder.read ? 'info' : 'warning'">
                    {{ reminder.read ? '已读' : '未读' }}
                  </el-tag>
                </div>
                <div class="reminder-meta">
                  <span>{{ reminder.teacherName }}</span>
                  <span>{{ reminder.courseName }}</span>
                  <span>{{ reminder.createTime }}</span>
                </div>
                <div class="reminder-body">{{ reminder.content }}</div>
              </div>
              <div class="reminder-actions">
                <el-button v-if="!reminder.read" type="primary" link @click="markAsRead(reminder.id)">
                  标记已读
                </el-button>
              </div>
            </div>
          </div>
        </el-card>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import {
  getStudentAnalysis,
  getStudentAnalysisCourses,
  getReminders,
  markAllRemindersAsRead,
  markReminderAsRead
} from '@/api/student'

type Summary = {
  courseId: number | null
  courseName: string
  totalHomework: number
  completedHomework: number
  pendingHomework: number
  gradedHomework: number
  passCount: number
  lateCount: number
  completionRate: number | string
  averageScore: number | string
  gpa: number | string
  level: string
  unreadReminderCount: number
}

const loading = ref(false)
const courseList = ref<any[]>([])
const selectedCourseId = ref<number | null>(null)
const summary = ref<Summary>({
  courseId: null,
  courseName: '',
  totalHomework: 0,
  completedHomework: 0,
  pendingHomework: 0,
  gradedHomework: 0,
  passCount: 0,
  lateCount: 0,
  completionRate: 0,
  averageScore: 0,
  gpa: 0,
  level: '',
  unreadReminderCount: 0
})
const homeworkList = ref<any[]>([])
const reminders = ref<any[]>([])
const suggestions = ref<string[]>([])
const scoreTrend = ref<any[]>([])
const unreadCount = ref(0)
const trendChartRef = ref<HTMLElement | null>(null)
let trendChart: echarts.ECharts | null = null

const resetAnalysis = () => {
  summary.value = {
    courseId: selectedCourseId.value,
    courseName: '',
    totalHomework: 0,
    completedHomework: 0,
    pendingHomework: 0,
    gradedHomework: 0,
    passCount: 0,
    lateCount: 0,
    completionRate: 0,
    averageScore: 0,
    gpa: 0,
    level: '',
    unreadReminderCount: 0
  }
  homeworkList.value = []
  suggestions.value = []
  scoreTrend.value = []
}

const loadCourses = async () => {
  const res = await getStudentAnalysisCourses()
  if (res.code === 200) {
    courseList.value = Array.isArray(res.data) ? res.data : []
    if (!selectedCourseId.value && courseList.value.length > 0) {
      selectedCourseId.value = courseList.value[0].id
    }
  }
}

const loadAnalysis = async () => {
  if (!selectedCourseId.value) {
    resetAnalysis()
    return
  }

  const res = await getStudentAnalysis(selectedCourseId.value)
  if (res.code === 200) {
    summary.value = {
      ...summary.value,
      ...res.data.summary
    }
    homeworkList.value = Array.isArray(res.data.homeworkList) ? res.data.homeworkList : []
    suggestions.value = Array.isArray(res.data.suggestions) ? res.data.suggestions : []
    scoreTrend.value = Array.isArray(res.data.scoreTrend) ? res.data.scoreTrend : []
    nextTick(() => {
      renderTrendChart()
    })
  }
}

const loadRemindersData = async () => {
  if (!selectedCourseId.value) {
    reminders.value = []
    unreadCount.value = 0
    return
  }

  const res = await getReminders(selectedCourseId.value)
  if (res.code === 200) {
    reminders.value = Array.isArray(res.data) ? res.data : []
    unreadCount.value = reminders.value.filter(item => !item.read).length
    summary.value.unreadReminderCount = unreadCount.value
  }
}

const loadPageData = async () => {
  if (!selectedCourseId.value) return

  loading.value = true
  try {
    await Promise.all([loadAnalysis(), loadRemindersData()])
  } catch (error: any) {
    console.error('加载学生学情分析失败:', error)
    ElMessage.error(error?.message || '加载学生学情分析失败')
  } finally {
    loading.value = false
  }
}

const markAsRead = async (reminderId: number) => {
  try {
    const res = await markReminderAsRead(reminderId)
    if (res.code === 200) {
      const target = reminders.value.find(item => item.id === reminderId)
      if (target && !target.read) {
        target.read = true
        unreadCount.value = Math.max(unreadCount.value - 1, 0)
        summary.value.unreadReminderCount = unreadCount.value
      }
      ElMessage.success('已标记为已读')
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '标记失败')
  }
}

const markAllAsRead = async () => {
  if (!selectedCourseId.value) return

  try {
    const res = await markAllRemindersAsRead(selectedCourseId.value)
    if (res.code === 200) {
      reminders.value = reminders.value.map(item => ({ ...item, read: true }))
      unreadCount.value = 0
      summary.value.unreadReminderCount = 0
      ElMessage.success('全部标记为已读')
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '操作失败')
  }
}

const renderTrendChart = () => {
  if (!trendChartRef.value) return

  if (trendChart) {
    trendChart.dispose()
  }

  trendChart = echarts.init(trendChartRef.value)
  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      formatter: (params: any) => {
        const point = params?.[0]
        if (!point) return ''
        return `${point.axisValue}<br/>${point.seriesName}：${point.data} 分`
      }
    },
    grid: {
      left: 40,
      right: 20,
      top: 30,
      bottom: 50
    },
    xAxis: {
      type: 'category',
      data: scoreTrend.value.map(item => item.homeworkTitle),
      axisLabel: {
        interval: 0,
        rotate: 20
      }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100
    },
    series: [
      {
        name: '成绩',
        type: 'line',
        smooth: true,
        data: scoreTrend.value.map(item => Number(item.score ?? 0)),
        symbolSize: 8,
        lineStyle: {
          width: 3,
          color: '#409EFF'
        },
        itemStyle: {
          color: '#409EFF'
        },
        areaStyle: {
          color: 'rgba(64, 158, 255, 0.12)'
        }
      }
    ]
  })
}

const handleResize = () => {
  trendChart?.resize()
}

const formatScore = (value: number | string | null | undefined) => {
  if (value === null || value === undefined || value === '') return '-'
  const numberValue = Number(value)
  return Number.isNaN(numberValue) ? value : numberValue.toFixed(2)
}

const formatPercent = (value: number | string | null | undefined) => {
  if (value === null || value === undefined || value === '') return '0.00'
  const numberValue = Number(value)
  return Number.isNaN(numberValue) ? value : numberValue.toFixed(2)
}

const toPercent = (value: number | string | null | undefined) => {
  const numberValue = Number(value ?? 0)
  if (Number.isNaN(numberValue)) return 0
  return Math.max(0, Math.min(100, Number(numberValue.toFixed(2))))
}

const scoreTagType = (level: string) => {
  if (level === '优秀') return 'success'
  if (level === '良好') return 'primary'
  if (level === '中等') return 'warning'
  if (level === '及格') return 'info'
  return 'danger'
}

const homeworkTagType = (status: string) => {
  if (status === '已提交') return 'success'
  if (status === '迟交') return 'warning'
  return 'danger'
}

onMounted(async () => {
  window.addEventListener('resize', handleResize)
  try {
    await loadCourses()
    if (selectedCourseId.value) {
      await loadPageData()
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '初始化失败')
  }
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendChart?.dispose()
})
</script>

<style scoped>
.learning-analysis {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.toolbar-card,
.content-card {
  border-radius: 8px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.toolbar-left {
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
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

.summary-row,
.content-row {
  margin-top: 0;
}

.summary-card {
  min-height: 120px;
  border-radius: 8px;
}

.summary-card.attention {
  background: #fffaf0;
}

.summary-label {
  font-size: 14px;
  color: #6b7280;
  margin-bottom: 18px;
}

.summary-value {
  font-size: 34px;
  line-height: 1;
  font-weight: 700;
  color: #111827;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.header-note {
  font-size: 13px;
  color: #6b7280;
}

.chart {
  width: 100%;
  height: 320px;
}

.insight-card {
  min-height: 100%;
}

.insight-block {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
}

.insight-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-radius: 8px;
  background: #f8fafc;
}

.insight-label {
  color: #6b7280;
}

.insight-value {
  font-weight: 700;
  color: #111827;
}

.progress-block {
  margin-top: 18px;
}

.progress-label {
  margin-bottom: 10px;
  color: #374151;
  font-size: 14px;
}

.suggestion-list {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.suggestion-item {
  padding: 12px 14px;
  border-radius: 8px;
  background: #f8fafc;
  color: #374151;
  line-height: 1.6;
}

.reminder-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.reminder-item {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #ffffff;
}

.reminder-item.unread {
  border-left: 4px solid #e6a23c;
  background: #fffdf7;
}

.reminder-main {
  flex: 1;
  min-width: 0;
}

.reminder-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.reminder-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}

.reminder-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 8px;
  font-size: 13px;
  color: #6b7280;
}

.reminder-body {
  margin-top: 10px;
  color: #374151;
  line-height: 1.7;
}

.reminder-actions {
  flex-shrink: 0;
}

@media (max-width: 1200px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .toolbar-right {
    justify-content: flex-start;
  }
}
</style>
