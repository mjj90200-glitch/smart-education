<template>
  <div class="learning-analysis page-container">
    <el-card class="filter-card">
      <template #header>
        <div class="card-header">
          <span>学情分析</span>
          <el-select v-model="selectedCourse" placeholder="选择课程" style="width: 200px" @change="loadCourseData">
            <el-option v-for="course in courseList" :key="course.id" :label="course.courseName" :value="course.id"/>
          </el-select>
        </div>
      </template>

      <!-- 分析类型切换 -->
      <el-tabs v-model="activeTab" @tab-change="handleTabChange">
        <el-tab-pane label="班级概览" name="class"></el-tab-pane>
        <el-tab-pane label="学生详情" name="student"></el-tab-pane>
        <el-tab-pane label="学习预警" name="warning"></el-tab-pane>
        <el-tab-pane label="教学建议" name="suggestion"></el-tab-pane>
      </el-tabs>
    </el-card>

    <!-- 班级概览 -->
    <div v-if="activeTab === 'class'" class="analysis-content">
      <!-- 统计卡片 -->
      <el-row :gutter="20" class="stats-row">
        <el-col :span="6">
          <el-card class="stat-card">
            <el-statistic title="参考人数" :value="classStats.studentCount">
              <template #suffix>人</template>
            </el-statistic>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <el-statistic title="平均分" :value="classStats.avgScore" :precision="1">
              <template #suffix>分</template>
            </el-statistic>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <el-statistic title="及格率" :value="classStats.passRate" :precision="1">
              <template #suffix>%</template>
            </el-statistic>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card">
            <el-statistic title="迟交率" :value="classStats.lateRate" :precision="1">
              <template #suffix>%</template>
            </el-statistic>
          </el-card>
        </el-col>
      </el-row>

      <!-- 分数分布图 -->
      <el-row :gutter="20" class="chart-row">
        <el-col :span="12">
          <el-card>
            <template #header>分数分布</template>
            <div ref="scoreDistChartRef" style="height: 300px"></div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card>
            <template #header>成绩排名 TOP 10</template>
            <el-table :data="topStudents" size="small" :show-header="true">
              <el-table-column type="index" label="排名" width="60"/>
              <el-table-column prop="studentName" label="姓名" min-width="100"/>
              <el-table-column prop="avgScore" label="平均分" width="80">
                <template #default="{row}">{{ row.avgScore.toFixed(1) }}</template>
              </el-table-column>
              <el-table-column prop="homeworkCount" label="作业数" width="70"/>
            </el-table>
          </el-card>
        </el-col>
      </el-row>

      <!-- AI 分析报告 -->
      <el-card class="ai-analysis-card">
        <template #header>
          <div class="ai-header">
            <el-icon><MagicStick/></el-icon>
            <span>AI 学情分析报告</span>
          </div>
        </template>
        <div v-if="classStats.aiAnalysis" class="ai-content">
          {{ classStats.aiAnalysis }}
        </div>
        <el-empty v-else description="暂无 AI 分析报告"/>
      </el-card>
    </div>

    <!-- 学生详情 -->
    <div v-if="activeTab === 'student'" class="analysis-content">
      <!-- 学生选择 -->
      <div class="student-filter">
        <el-select v-model="selectedStudent" placeholder="选择学生" style="width: 200px" @change="loadStudentData">
          <el-option v-for="student in studentList" :key="student.id" :label="student.realName" :value="student.id"/>
        </el-select>
      </div>

      <div v-if="studentReport.studentName" class="student-detail">
        <!-- 基本信息 -->
        <el-card class="student-info">
          <el-descriptions :column="3" title="学生信息">
            <el-descriptions-item label="姓名">{{ studentReport.studentName }}</el-descriptions-item>
            <el-descriptions-item label="班级">{{ studentReport.className }}</el-descriptions-item>
            <el-descriptions-item label="专业">{{ studentReport.major }}</el-descriptions-item>
            <el-descriptions-item label="提交次数">{{ studentReport.submittedCount }}</el-descriptions-item>
            <el-descriptions-item label="平均分">
              <el-tag :type="getScoreType(studentReport.avgScore)">{{ studentReport.avgScore }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="学习状态">
              <el-tag :type="getRiskType(studentReport.riskLevel)">
                {{ getRiskText(studentReport.riskLevel) }}
              </el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 学习趋势图 -->
        <el-card class="trend-card">
          <template #header>学习趋势</template>
          <div ref="trendChartRef" style="height: 300px"></div>
        </el-card>

        <!-- 薄弱知识点 -->
        <el-card>
          <template #header>薄弱知识点</template>
          <el-table :data="studentReport.weakPoints" size="small" v-if="studentReport.weakPoints?.length > 0">
            <el-table-column prop="knowledgePointId" label="知识点 ID" width="100"/>
            <el-table-column prop="avgScore" label="掌握度" width="150">
              <template #default="{row}">
                <el-progress :percentage="row.avgScore" :color="getMasteryColor(row.avgScore)"/>
              </template>
            </el-table-column>
            <el-table-column prop="weakReason" label="薄弱程度" min-width="200"/>
          </el-table>
          <el-empty v-else description="暂无薄弱知识点"/>
        </el-card>

        <!-- AI 学习建议 -->
        <el-card class="ai-suggestion-card">
          <template #header>
            <div class="ai-header">
              <el-icon><MagicStick/></el-icon>
              <span>AI 学习建议</span>
            </div>
          </template>
          <div v-if="studentReport.aiSuggestion" class="ai-content">
            {{ studentReport.aiSuggestion }}
          </div>
          <el-empty v-else description="暂无 AI 建议"/>
        </el-card>
      </div>

      <el-empty v-else description="请选择学生查看详情"/>
    </div>

    <!-- 学习预警 -->
    <div v-if="activeTab === 'warning'" class="analysis-content">
      <el-card>
        <template #header>
          <div class="warning-header">
            <el-icon><Warning/></el-icon>
            <span>学习预警学生名单</span>
            <el-tag type="danger" style="margin-left: 10px">{{ warningList.length }}人</el-tag>
          </div>
        </template>

        <el-table :data="warningList" style="width: 100%">
          <el-table-column prop="studentName" label="学生姓名" width="100"/>
          <el-table-column prop="warningType" label="预警类型" width="120">
            <template #default="{row}">
              <el-tag :type="getWarningTypeColor(row.warningType)">
                {{ getWarningTypeText(row.warningType) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="warningLevel" label="预警级别" width="100">
            <template #default="{row}">
              <el-tag :type="row.warningLevel === 'high' ? 'danger' : 'warning'">
                {{ row.warningLevel === 'high' ? '严重' : '中等' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="description" label="预警详情" min-width="250"/>
          <el-table-column prop="value" label="预警值" width="100"/>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{row}">
              <el-button size="small" @click="viewStudentDetail(row.studentId)">查看详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 教学建议 -->
    <div v-if="activeTab === 'suggestion'" class="analysis-content">
      <el-card>
        <template #header>
          <div class="suggestion-header">
            <el-icon><Document/></el-icon>
            <span>教学建议</span>
          </div>
        </template>

        <el-form :model="suggestionForm" label-width="100px">
          <el-form-item label="选择作业">
            <el-select v-model="suggestionForm.homeworkId" placeholder="选择作业" style="width: 300px">
              <el-option v-for="hw in homeworkList" :key="hw.id" :label="hw.title" :value="hw.id"/>
            </el-select>
            <el-button type="primary" @click="loadTeachingSuggestion" :loading="suggestionLoading">
              生成建议
            </el-button>
          </el-form-item>
        </el-form>

        <div v-if="teachingSuggestion.suggestions" class="suggestion-content">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="提交人数">{{ teachingSuggestion.submittedCount }}</el-descriptions-item>
            <el-descriptions-item label="平均分">{{ teachingSuggestion.avgScore }}</el-descriptions-item>
            <el-descriptions-item label="及格率">{{ teachingSuggestion.passRate }}%</el-descriptions-item>
          </el-descriptions>

          <el-divider>教学建议</el-divider>
          <ul class="suggestion-list">
            <li v-for="(item, index) in teachingSuggestion.suggestions" :key="index">{{ item }}</li>
          </ul>

          <div v-if="teachingSuggestion.aiSuggestion" class="ai-suggestion">
            <div class="ai-header">
              <el-icon><MagicStick/></el-icon>
              <span>AI 补充建议</span>
            </div>
            <p>{{ teachingSuggestion.aiSuggestion }}</p>
          </div>
        </div>

        <el-empty v-else description="请选择作业并生成教学建议"/>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import {ref, reactive, onMounted, nextTick} from 'vue'
import {ElMessage} from 'element-plus'
import {MagicStick, Warning, Document} from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import type {EChartsOption} from 'echarts'
import {courseApi, homeworkApi, analysisApi} from '@/api/teacher'

// 数据
const courseList = ref<any[]>([])
const studentList = ref<any[]>([])
const homeworkList = ref<any[]>([])
const selectedCourse = ref<number | null>(null)
const selectedStudent = ref<number | null>(null)
const activeTab = ref('class')

// 班级统计
const classStats = ref<any>({
  studentCount: 0,
  avgScore: 0,
  passRate: 0,
  lateRate: 0,
  scoreDistribution: {},
  aiAnalysis: ''
})
const topStudents = ref<any[]>([])

// 学生报告
const studentReport = ref<any>({
  studentName: '',
  className: '',
  major: '',
  submittedCount: 0,
  avgScore: 0,
  riskLevel: 'low',
  weakPoints: [],
  aiSuggestion: '',
  learningTrend: []
})

// 预警列表
const warningList = ref<any[]>([])

// 教学建议
const suggestionForm = reactive({homeworkId: null as number | null})
const teachingSuggestion = ref<any>({})
const suggestionLoading = ref(false)

// 图表引用
const scoreDistChartRef = ref<HTMLElement | null>(null)
const trendChartRef = ref<HTMLElement | null>(null)
let scoreDistChart: echarts.ECharts | null = null
let trendChart: echarts.ECharts | null = null

// 加载课程
const loadCourseList = async () => {
  try {
    const res = await courseApi.getList()
    if (res.code === 200) {
      courseList.value = res.data || []
      if (courseList.value.length > 0) {
        selectedCourse.value = courseList.value[0].id
        loadCourseData()
      }
    }
  } catch (e) {
    console.error('加载课程失败:', e)
  }
}

// 加载课程数据
const loadCourseData = async () => {
  if (!selectedCourse.value) return
  await loadClassReport()
  await loadWarningList()
  await loadHomeworkList()
}

// 加载班级报告
const loadClassReport = async () => {
  try {
    const res = await analysisApi.getClassReport(selectedCourse.value)
    if (res.code === 200 && res.data) {
      classStats.value = res.data
      topStudents.value = res.data.ranking?.top10 || []
      nextTick(() => {
        renderScoreDistChart(res.data.scoreDistribution)
      })
    }
  } catch (e: any) {
    console.error('加载班级报告失败:', e)
  }
}

// 加载学生列表
const loadStudentList = async () => {
  // 这里需要从班级提交中获取学生列表
  // 暂时用空数据
  studentList.value = []
}

// 加载学生数据
const loadStudentData = async () => {
  if (!selectedStudent.value || !selectedCourse.value) return
  try {
    const res = await analysisApi.getStudentReport(selectedStudent.value, selectedCourse.value)
    if (res.code === 200 && res.data) {
      studentReport.value = res.data
      nextTick(() => {
        renderTrendChart(res.data.learningTrend)
      })
    }
  } catch (e: any) {
    console.error('加载学生数据失败:', e)
  }
}

// 加载预警列表
const loadWarningList = async () => {
  if (!selectedCourse.value) return
  try {
    const res = await analysisApi.getWarnings(selectedCourse.value)
    if (res.code === 200) {
      warningList.value = res.data || []
    }
  } catch (e: any) {
    console.error('加载预警列表失败:', e)
  }
}

// 加载作业列表
const loadHomeworkList = async () => {
  if (!selectedCourse.value) return
  try {
    const res = await homeworkApi.getList({courseId: selectedCourse.value})
    if (res.code === 200) {
      homeworkList.value = res.data?.list || res.data?.records || []
    }
  } catch (e: any) {
    console.error('加载作业列表失败:', e)
  }
}

// 加载教学建议
const loadTeachingSuggestion = async () => {
  if (!suggestionForm.homeworkId) return
  suggestionLoading.value = true
  try {
    const res = await analysisApi.getTeachingSuggestion({
      homeworkId: suggestionForm.homeworkId,
      courseId: selectedCourse.value
    })
    if (res.code === 200 && res.data) {
      teachingSuggestion.value = res.data
    }
  } catch (e: any) {
    ElMessage.error('加载教学建议失败')
  } finally {
    suggestionLoading.value = false
  }
}

// 切换标签
const handleTabChange = (tab: string) => {
  if (tab === 'student') {
    loadStudentList()
  } else if (tab === 'warning') {
    loadWarningList()
  } else if (tab === 'suggestion') {
    loadHomeworkList()
  }
}

// 查看学生详情
const viewStudentDetail = (studentId: number) => {
  selectedStudent.value = studentId
  activeTab.value = 'student'
  loadStudentData()
}

// 渲染分数分布图
const renderScoreDistChart = (data: Record<string, number>) => {
  if (!scoreDistChartRef.value) return
  if (!scoreDistChart) {
    scoreDistChart = echarts.init(scoreDistChartRef.value)
  }

  const option: EChartsOption = {
    tooltip: {trigger: 'axis'},
    xAxis: {
      type: 'category',
      data: Object.keys(data)
    },
    yAxis: {
      type: 'value',
      name: '人数'
    },
    series: [{
      data: Object.values(data),
      type: 'bar',
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          {offset: 0, color: '#83bff6'},
          {offset: 1, color: '#188df0'}
        ])
      }
    }]
  }
  scoreDistChart.setOption(option)
}

// 渲染学习趋势图
const renderTrendChart = (data: any[]) => {
  if (!trendChartRef.value) return
  if (!trendChart) {
    trendChart = echarts.init(trendChartRef.value)
  }

  const option: EChartsOption = {
    tooltip: {trigger: 'axis'},
    xAxis: {
      type: 'category',
      data: data.map(item => item.time)
    },
    yAxis: {
      type: 'value',
      name: '分数',
      min: 0,
      max: 100
    },
    series: [{
      data: data.map(item => item.score),
      type: 'line',
      smooth: true,
      itemStyle: {color: '#67C23A'}
    }]
  }
  trendChart.setOption(option)
}

// 工具函数
const getScoreType = (score: number) => {
  if (score >= 90) return 'success'
  if (score >= 75) return 'primary'
  if (score >= 60) return 'warning'
  return 'danger'
}

const getRiskType = (level: string) => {
  if (level === 'high') return 'danger'
  if (level === 'medium') return 'warning'
  return 'success'
}

const getRiskText = (level: string) => {
  if (level === 'high') return '高风险'
  if (level === 'medium') return '中风险'
  return '正常'
}

const getWarningTypeText = (type: string) => {
  const map: Record<string, string> = {
    'LOW_SCORE': '低分预警',
    'LATE_SUBMISSION': '迟交预警',
    'ABSENCE': '缺交预警',
    'DECLINING-TREND': '成绩下滑'
  }
  return map[type] || type
}

const getWarningTypeColor = (type: string) => {
  if (type === 'LOW_SCORE') return 'danger'
  if (type === 'LATE_SUBMISSION') return 'warning'
  return 'info'
}

const getMasteryColor = (score: number) => {
  if (score >= 80) return '#67C23A'
  if (score >= 60) return '#E6A23C'
  return '#F56C6C'
}

onMounted(() => {
  loadCourseList()
})
</script>

<style scoped lang="scss">
.learning-analysis {
  .filter-card {
    margin-bottom: 20px;

    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
  }

  .analysis-content {
    .stats-row {
      margin-bottom: 20px;

      .stat-card {
        text-align: center;

        :deep(.el-statistic__content) {
          font-size: 28px;
          font-weight: bold;
        }
      }
    }

    .chart-row {
      margin-bottom: 20px;
    }

    .ai-analysis-card, .ai-suggestion-card {
      margin-top: 20px;

      .ai-header {
        display: flex;
        align-items: center;
        gap: 8px;
        font-weight: bold;
        color: #67C23A;
      }

      .ai-content {
        line-height: 1.8;
        color: #333;
        padding: 10px 0;
      }
    }

    .student-filter {
      margin-bottom: 20px;
    }

    .student-detail {
      .student-info {
        margin-bottom: 20px;
      }

      .trend-card {
        margin-bottom: 20px;
      }
    }

    .warning-header {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: bold;
    }

    .suggestion-header {
      display: flex;
      align-items: center;
      gap: 8px;
      font-weight: bold;
    }

    .suggestion-content {
      margin-top: 20px;

      .suggestion-list {
        padding-left: 20px;
        line-height: 2;

        li {
          margin-bottom: 10px;
        }
      }

      .ai-suggestion {
        margin-top: 20px;
        padding: 15px;
        background: #f0f9eb;
        border-radius: 8px;
        border: 1px solid #e1f3d8;

        .ai-header {
          display: flex;
          align-items: center;
          gap: 8px;
          font-weight: bold;
          color: #67C23A;
          margin-bottom: 10px;
        }

        p {
          line-height: 1.8;
          color: #333;
        }
      }
    }
  }
}
</style>
