<template>
  <div class="analysis-report page-container">
    <el-card class="toolbar-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <div class="page-title">学情分析</div>
          <div class="page-subtitle">查看课程作业进度、学生 GPA 和提醒名单</div>
        </div>
        <div class="toolbar-right">
          <el-select
            v-model="selectedCourseId"
            placeholder="选择课程"
            style="width: 260px"
            @change="loadOverview"
          >
            <el-option
              v-for="course in courseList"
              :key="course.id"
              :label="course.courseName"
              :value="course.id"
            />
          </el-select>
          <el-button type="primary" :loading="loading" @click="loadOverview">刷新</el-button>
        </div>
      </div>
    </el-card>

    <div v-loading="loading">
      <el-row :gutter="16" class="summary-row">
        <el-col :span="6">
          <el-card class="summary-card">
            <div class="summary-label">已发布作业</div>
            <div class="summary-value">{{ overview.summary.totalHomeworkCount }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="summary-card">
            <div class="summary-label">课程学生</div>
            <div class="summary-value">{{ overview.summary.studentCount }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="summary-card">
            <div class="summary-label">平均分</div>
            <div class="summary-value">{{ formatScore(overview.summary.averageScore) }}</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="summary-card">
            <div class="summary-label">整体完成率</div>
            <div class="summary-value">{{ formatPercent(overview.summary.completionRate) }}%</div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="content-row">
        <el-col :span="14">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>作业完成进度</span>
              </div>
            </template>
            <el-table :data="overview.homeworkProgress" empty-text="当前课程还没有已发布作业">
              <el-table-column prop="title" label="作业" min-width="180" />
              <el-table-column prop="submittedCount" label="已提交" width="90" />
              <el-table-column prop="unsubmittedCount" label="未提交" width="90" />
              <el-table-column prop="gradedCount" label="已出分" width="90" />
              <el-table-column prop="lateCount" label="迟交" width="80" />
              <el-table-column label="平均分" width="100">
                <template #default="scope">{{ formatScore(scope.row.averageScore) }}</template>
              </el-table-column>
              <el-table-column label="完成率" width="120">
                <template #default="scope">
                  <el-progress :percentage="toPercent(scope.row.completionRate)" :stroke-width="8" />
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
        <el-col :span="10">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>成绩等级分布</span>
              </div>
            </template>
            <div ref="levelChartRef" class="chart"></div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" class="content-row">
        <el-col :span="12">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>及格学生</span>
                <el-tag type="success">{{ overview.passStudents.length }} 人</el-tag>
              </div>
            </template>
            <el-table :data="overview.passStudents" max-height="360" empty-text="暂无及格学生">
              <el-table-column prop="studentName" label="姓名" width="120" />
              <el-table-column prop="className" label="班级" min-width="120" />
              <el-table-column label="平均分" width="100">
                <template #default="scope">{{ formatScore(scope.row.averageScore) }}</template>
              </el-table-column>
              <el-table-column prop="level" label="等级" width="90">
                <template #default="scope">
                  <el-tag :type="scoreTagType(scope.row.level)">{{ scope.row.level }}</el-tag>
                </template>
              </el-table-column>
              <el-table-column label="GPA" width="80">
                <template #default="scope">{{ formatScore(scope.row.gpa) }}</template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card>
            <template #header>
              <div class="card-header">
                <span>需提醒学生</span>
                <div class="card-actions">
                  <el-button
                    type="warning"
                    size="small"
                    :disabled="selectedReminderIds.length === 0"
                    @click="sendReminder"
                  >
                    提醒学生
                  </el-button>
                </div>
              </div>
            </template>
            <el-table
              :data="overview.attentionStudents"
              max-height="360"
              empty-text="暂无需要提醒的学生"
              @selection-change="handleSelectionChange"
            >
              <el-table-column type="selection" width="48" />
              <el-table-column prop="studentName" label="姓名" width="110" />
              <el-table-column label="平均分" width="100">
                <template #default="scope">{{ formatScore(scope.row.averageScore) }}</template>
              </el-table-column>
              <el-table-column prop="missingCount" label="未交作业" width="100" />
              <el-table-column prop="lateCount" label="迟交次数" width="100" />
              <el-table-column prop="analysis" label="情况说明" min-width="220" show-overflow-tooltip />
            </el-table>
          </el-card>
        </el-col>
      </el-row>

      <el-card class="student-card">
        <template #header>
          <div class="card-header">
            <span>学生成绩分析</span>
            <el-input
              v-model="keyword"
              clearable
              placeholder="搜索学生姓名"
              style="width: 220px"
            />
          </div>
        </template>
        <el-table :data="filteredStudents" empty-text="暂无学生数据">
          <el-table-column type="expand">
            <template #default="scope">
              <div class="expand-panel">
                <div class="expand-title">作业提交明细</div>
                <el-table :data="scope.row.homeworks" size="small">
                  <el-table-column prop="title" label="作业" min-width="180" />
                  <el-table-column prop="submitStatus" label="提交状态" width="110">
                    <template #default="homeworkScope">
                      <el-tag :type="homeworkTagType(homeworkScope.row.submitStatus)">
                        {{ homeworkScope.row.submitStatus }}
                      </el-tag>
                    </template>
                  </el-table-column>
                  <el-table-column prop="submitTime" label="提交时间" width="180" />
                  <el-table-column label="成绩" width="100">
                    <template #default="homeworkScope">
                      {{ homeworkScope.row.score == null ? '-' : homeworkScope.row.score }}
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="studentName" label="姓名" width="120" />
          <el-table-column prop="className" label="班级" min-width="140" />
          <el-table-column label="完成情况" width="140">
            <template #default="scope">
              {{ scope.row.submittedCount }}/{{ overview.summary.totalHomeworkCount }}
            </template>
          </el-table-column>
          <el-table-column label="平均分" width="100">
            <template #default="scope">{{ formatScore(scope.row.averageScore) }}</template>
          </el-table-column>
          <el-table-column label="GPA" width="90">
            <template #default="scope">{{ formatScore(scope.row.gpa) }}</template>
          </el-table-column>
          <el-table-column prop="level" label="成绩分析" width="100">
            <template #default="scope">
              <el-tag :type="scoreTagType(scope.row.level)">{{ scope.row.level }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="missingCount" label="未交" width="80" />
          <el-table-column prop="lateCount" label="迟交" width="80" />
          <el-table-column prop="analysis" label="分析说明" min-width="260" show-overflow-tooltip />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="scope">
              <el-button type="primary" link @click="showStudentDetail(scope.row.studentId)">查看详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <el-dialog v-model="detailVisible" title="学生学情详情" width="860px">
      <div v-if="studentDetail">
        <el-descriptions :column="3" border>
          <el-descriptions-item label="姓名">{{ studentDetail.studentName }}</el-descriptions-item>
          <el-descriptions-item label="班级">{{ studentDetail.className }}</el-descriptions-item>
          <el-descriptions-item label="专业">{{ studentDetail.major }}</el-descriptions-item>
          <el-descriptions-item label="平均分">{{ formatScore(studentDetail.averageScore) }}</el-descriptions-item>
          <el-descriptions-item label="GPA">{{ formatScore(studentDetail.gpa) }}</el-descriptions-item>
          <el-descriptions-item label="成绩分析">{{ studentDetail.level }}</el-descriptions-item>
        </el-descriptions>
        <p class="detail-analysis">{{ studentDetail.analysis }}</p>
        <el-table :data="studentDetail.homeworks" size="small">
          <el-table-column prop="title" label="作业" min-width="180" />
          <el-table-column prop="submitStatus" label="提交状态" width="120" />
          <el-table-column prop="submitTime" label="提交时间" width="180" />
          <el-table-column label="成绩" width="100">
            <template #default="scope">{{ scope.row.score == null ? '-' : scope.row.score }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { courseApi, homeworkAnalysisApi } from '@/api/teacher'

const loading = ref(false)
const courseList = ref<any[]>([])
const selectedCourseId = ref<number | null>(null)
const keyword = ref('')
const selectedReminderIds = ref<number[]>([])
const detailVisible = ref(false)
const studentDetail = ref<any | null>(null)
const levelChartRef = ref<HTMLElement | null>(null)
let levelChart: echarts.ECharts | null = null

const overview = ref({
  summary: {
    totalHomeworkCount: 0,
    studentCount: 0,
    averageScore: 0,
    completionRate: 0,
    levelDistribution: {} as Record<string, number>
  },
  homeworkProgress: [] as any[],
  studentAnalysis: [] as any[],
  passStudents: [] as any[],
  attentionStudents: [] as any[]
})

const filteredStudents = computed(() => {
  const list = overview.value.studentAnalysis || []
  if (!keyword.value.trim()) {
    return list
  }
  return list.filter((item: any) => item.studentName?.includes(keyword.value.trim()))
})

const formatScore = (value: number | string | null | undefined) => {
  if (value == null || value === '') {
    return '-'
  }
  const numericValue = Number(value)
  return Number.isFinite(numericValue) ? numericValue.toFixed(2) : String(value)
}

const formatPercent = (value: number | string | null | undefined) => {
  if (value == null || value === '') {
    return '0.00'
  }
  const numericValue = Number(value)
  return Number.isFinite(numericValue) ? numericValue.toFixed(2) : String(value)
}

const toPercent = (value: number | string | null | undefined) => {
  const numericValue = Number(value ?? 0)
  return Number.isFinite(numericValue) ? numericValue : 0
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

const loadCourses = async () => {
  const res = await courseApi.getTeacherCourses()
  if (res.code === 200) {
    courseList.value = res.data || []
    if (!selectedCourseId.value && courseList.value.length > 0) {
      selectedCourseId.value = courseList.value[0].id
    }
  }
}

const renderLevelChart = async () => {
  await nextTick()
  if (!levelChartRef.value) return
  if (!levelChart) {
    levelChart = echarts.init(levelChartRef.value)
  }

  const distribution = overview.value.summary.levelDistribution || {}
  levelChart.setOption({
    tooltip: { trigger: 'item' },
    series: [
      {
        type: 'pie',
        radius: ['45%', '72%'],
        label: { formatter: '{b}: {c}' },
        data: Object.entries(distribution).map(([name, value]) => ({ name, value }))
      }
    ]
  })
}

const loadOverview = async () => {
  if (!selectedCourseId.value) return
  loading.value = true
  selectedReminderIds.value = []
  try {
    const res = await homeworkAnalysisApi.getOverview(selectedCourseId.value)
    if (res.code === 200) {
      overview.value = res.data
      await renderLevelChart()
    }
  } catch (error) {
    ElMessage.error('加载学情分析失败')
  } finally {
    loading.value = false
  }
}

const handleSelectionChange = (rows: any[]) => {
  selectedReminderIds.value = rows.map(row => row.studentId)
}

const sendReminder = async () => {
  if (!selectedCourseId.value || selectedReminderIds.value.length === 0) return
  try {
    const promptResult = await ElMessageBox.prompt(
      '输入提醒内容，留空则使用默认提醒',
      '提醒学生',
      {
        inputPlaceholder: '请尽快补交作业并关注近期成绩变化',
        confirmButtonText: '发送',
        cancelButtonText: '取消'
      }
    )

    const res = await homeworkAnalysisApi.remindStudents({
      courseId: selectedCourseId.value,
      studentIds: selectedReminderIds.value,
      message: promptResult.value || undefined
    })
    if (res.code === 200) {
      ElMessage.success(`已提醒 ${res.data.remindedCount} 名学生`)
    }
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.info('已取消提醒')
    }
  }
}

const showStudentDetail = async (studentId: number) => {
  if (!selectedCourseId.value) return
  const res = await homeworkAnalysisApi.getStudentDetail(studentId, selectedCourseId.value)
  if (res.code === 200) {
    studentDetail.value = res.data
    detailVisible.value = true
  }
}

onMounted(async () => {
  await loadCourses()
  await loadOverview()
  window.addEventListener('resize', () => {
    levelChart?.resize()
  })
})
</script>

<style scoped lang="scss">
.analysis-report {
  .toolbar-card,
  .student-card {
    margin-bottom: 16px;
  }

  .toolbar,
  .card-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 16px;
  }

  .toolbar-left {
    display: flex;
    flex-direction: column;
    gap: 4px;
  }

  .toolbar-right,
  .card-actions {
    display: flex;
    align-items: center;
    gap: 12px;
  }

  .page-title {
    font-size: 20px;
    font-weight: 600;
    color: #1f2937;
  }

  .page-subtitle {
    font-size: 13px;
    color: #6b7280;
  }

  .summary-row,
  .content-row {
    margin-bottom: 16px;
  }

  .summary-card {
    .summary-label {
      color: #6b7280;
      font-size: 13px;
      margin-bottom: 12px;
    }

    .summary-value {
      font-size: 28px;
      font-weight: 700;
      color: #111827;
    }
  }

  .chart {
    height: 320px;
  }

  .expand-panel {
    padding: 12px 24px;
    background: #f8fafc;
  }

  .expand-title,
  .detail-analysis {
    margin-bottom: 12px;
    color: #374151;
  }
}
</style>
