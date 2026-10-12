<template>
  <div class="learning-dashboard page-container">
    <div v-if="loading" class="loading-state">
      <el-skeleton animated :rows="10" />
    </div>

    <template v-else>
      <section class="dashboard-header">
        <div>
          <h1>学习看板</h1>
          <p>用热力图回看学习节奏，用时间轴展开当天的任务完成、成绩反馈和 AI 辅助痕迹。</p>
        </div>
        <el-button :loading="loading" @click="loadDashboard">
          <el-icon><RefreshRight /></el-icon>
          刷新
        </el-button>
      </section>

      <section class="summary-strip">
        <div class="summary-item">
          <span class="summary-label">活跃天数</span>
          <strong>{{ dashboard?.summary.activeDays || 0 }}</strong>
        </div>
        <div class="summary-item">
          <span class="summary-label">完成任务</span>
          <strong>{{ dashboard?.summary.completedTaskCount || 0 }}</strong>
        </div>
        <div class="summary-item">
          <span class="summary-label">成绩发布</span>
          <strong>{{ dashboard?.summary.gradedEventCount || 0 }}</strong>
        </div>
        <div class="summary-item">
          <span class="summary-label">AI 使用</span>
          <strong>{{ dashboard?.summary.aiUsageCount || 0 }}</strong>
        </div>
        <div class="summary-item">
          <span class="summary-label">未读提醒</span>
          <strong>{{ dashboard?.summary.unreadReminderCount || 0 }}</strong>
        </div>
      </section>

      <section class="heatmap-section">
        <div class="dashboard-shell">
          <div class="heatmap-layout">
            <section class="heatmap-card">
              <div class="heatmap-card-head">
                <div class="heatmap-title-group">
                  <h2>学习热力图</h2>
                  <p>{{ dashboard?.startDate }} - {{ dashboard?.endDate }}</p>
                </div>
                <div class="legend">
                  <span>低</span>
                  <div class="legend-scale">
                    <span v-for="level in 5" :key="level" :class="['legend-cell', `level-${level - 1}`]" />
                  </div>
                  <span>高</span>
                </div>
              </div>

              <div class="heatmap-board">
                <div class="heatmap-scroll">
                  <div class="month-row" :style="{ marginLeft: `${weekdayLabelWidth}px` }">
                    <span
                      v-for="month in monthLabels"
                      :key="`${month.label}-${month.column}`"
                      class="month-label"
                      :style="{ gridColumn: `${month.column}` }"
                    >
                      {{ month.label }}
                    </span>
                  </div>

                  <div class="heatmap-main">
                    <div class="weekday-column">
                      <span v-for="item in weekdayLabels" :key="item.key" :class="{ muted: !item.label }">{{ item.label }}</span>
                    </div>

                    <div
                      class="heatmap-grid"
                      :style="{
                        gridTemplateRows: 'repeat(7, 20px)',
                        gridAutoColumns: '20px'
                      }"
                    >
                      <span v-for="blank in leadingEmptyCount" :key="`blank-${blank}`" class="heatmap-cell blank" />

                      <el-tooltip
                        v-for="day in dashboard?.heatmapDays || []"
                        :key="day.date"
                        placement="top"
                        effect="light"
                      >
                        <template #content>
                          <div class="tooltip-content">
                            <div>{{ formatFullDate(day.date) }}</div>
                            <div>完成任务 {{ day.completedTaskCount }} 项</div>
                            <div>使用 AI {{ day.aiUsageCount }} 次</div>
                            <div>老师提醒 {{ day.reminderCount }} 条</div>
                          </div>
                        </template>
                        <button
                          class="heatmap-cell"
                          type="button"
                          :class="[`level-${day.activityLevel}`, { active: day.date === selectedDate }]"
                          @click="selectDay(day.date)"
                        />
                      </el-tooltip>
                    </div>
                  </div>
                </div>
              </div>
            </section>

            <aside class="note-panel">
              <div class="note-panel-head">
                <div>
                  <h3>学习笔记</h3>
                  <p>{{ formatDayTitle(selectedDate) }}</p>
                </div>
                <el-button type="primary" :loading="savingNote" @click="saveNote">保存</el-button>
              </div>

              <el-form label-position="top" class="note-form">
                <el-form-item label="今天干了什么">
                  <el-input
                    v-model="noteForm.completedSummary"
                    type="textarea"
                    :rows="4"
                    placeholder="记录今天推进了哪些课程、作业或复习任务"
                  />
                </el-form-item>
                <el-form-item label="未完成什么">
                  <el-input
                    v-model="noteForm.pendingSummary"
                    type="textarea"
                    :rows="4"
                    placeholder="写下还没做完、准备明天继续跟进的事项"
                  />
                </el-form-item>
                <el-form-item label="今天使用了哪个 AI 工具">
                  <el-input
                    v-model="noteForm.aiToolSummary"
                    placeholder="例如：SmartEdu Agent、Volcengine Ark、豆包"
                  />
                </el-form-item>
                <el-form-item label="补充反思">
                  <el-input
                    v-model="noteForm.reflection"
                    type="textarea"
                    :rows="5"
                    placeholder="今天最大的卡点、收获或者明天的调整计划"
                  />
                </el-form-item>
              </el-form>
            </aside>
          </div>
        </div>

        <div class="note-display">
          <div class="note-display-head">
            <h3>当日笔记盒子</h3>
            <span>{{ formatDayTitle(selectedDate) }}</span>
          </div>
          <div v-if="hasNoteContent" class="note-grid">
            <div class="note-block">
              <label>今天干了什么</label>
              <p>{{ displayValue(noteForm.completedSummary) }}</p>
            </div>
            <div class="note-block">
              <label>未完成什么</label>
              <p>{{ displayValue(noteForm.pendingSummary) }}</p>
            </div>
            <div class="note-block">
              <label>今天使用了哪个 AI 工具</label>
              <p>{{ displayValue(noteForm.aiToolSummary) }}</p>
            </div>
            <div class="note-block">
              <label>补充反思</label>
              <p>{{ displayValue(noteForm.reflection) }}</p>
            </div>
          </div>
          <el-empty v-else description="这一天还没有写学习笔记" :image-size="56" />
        </div>
      </section>

      <section class="timeline-section">
        <div class="section-head">
          <div>
            <h2>{{ formatDayTitle(selectedDate) }}</h2>
            <p>点击上方任意日期方块，这里的行为时间轴会同步切换。</p>
          </div>
          <div class="day-stats">
            <span>任务 {{ selectedDay.completedTaskCount }}</span>
            <span>成绩 {{ selectedDay.gradedEventCount }}</span>
            <span>AI {{ selectedDay.aiUsageCount }}</span>
            <span>提醒 {{ selectedDay.reminderCount }}</span>
          </div>
        </div>

        <div v-if="selectedDay.timelineEvents.length" class="timeline-list">
          <div
            v-for="event in selectedDay.timelineEvents"
            :key="event.id"
            class="timeline-row"
            :class="[`type-${event.type}`, `status-${event.status}`]"
          >
            <div class="timeline-time">{{ event.time }}</div>
            <div class="timeline-track">
              <span class="timeline-dot">
                <el-icon v-if="event.type === 'ai_usage'"><MagicStick /></el-icon>
                <el-icon v-else-if="event.type === 'reminder'"><Bell /></el-icon>
                <el-icon v-else-if="event.status === 'graded'"><Clock /></el-icon>
                <el-icon v-else><CircleCheck /></el-icon>
              </span>
              <span class="timeline-line" />
            </div>
            <div class="timeline-card">
              <div class="card-topline">
                <div class="card-title-group">
                  <h3>{{ event.title }}</h3>
                  <p v-if="event.subtitle">{{ event.subtitle }}</p>
                </div>
                <div class="badge-group">
                  <span v-if="event.courseName" class="chip chip-muted">{{ event.courseName }}</span>
                  <span v-if="event.scoreLabel" class="chip chip-score">{{ event.scoreLabel }}</span>
                  <span v-if="event.type === 'ai_usage'" class="chip chip-ai">AI</span>
                  <span v-if="event.type === 'reminder'" class="chip chip-reminder">{{ event.status === 'unread' ? '待处理' : '已读' }}</span>
                </div>
              </div>

              <div v-if="event.type === 'ai_usage'" class="ai-meta">
                <div class="ai-head">
                  <span class="ai-model">{{ event.aiModel || 'SmartEdu Agent' }}</span>
                  <span class="ai-action">{{ event.actionSummary }}</span>
                </div>
                <div class="ai-detail">{{ event.detail }}</div>
                <div v-if="event.resultSummary" class="ai-result">{{ event.resultSummary }}</div>
              </div>

              <div v-else class="event-detail">
                {{ event.detail }}
              </div>
            </div>
          </div>
        </div>

        <div v-else class="empty-day">
          <el-empty description="当天还没有学习事件" :image-size="72" />
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Bell, CircleCheck, Clock, MagicStick, RefreshRight } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/modules/user'
import { getLearningDashboard, getLearningDashboardNote, saveLearningDashboardNote } from '@/api/student'
import type { LearningDailyNote, LearningDashboardData, LearningDayDetail } from '@/types/student'

const userStore = useUserStore()

const loading = ref(true)
const savingNote = ref(false)
const dashboard = ref<LearningDashboardData | null>(null)
const selectedDate = ref('')
const weekdayLabelWidth = 42
const noteForm = reactive<LearningDailyNote>({
  completedSummary: '',
  pendingSummary: '',
  aiToolSummary: '',
  reflection: '',
  hasContent: false
})

const weekdayLabels = [
  { key: 'mon', label: '一' },
  { key: 'tue', label: '' },
  { key: 'wed', label: '三' },
  { key: 'thu', label: '' },
  { key: 'fri', label: '五' },
  { key: 'sat', label: '' },
  { key: 'sun', label: '日' }
]

const parseLocalDate = (date: string) => {
  const [year, month, day] = date.split('-').map(Number)
  return new Date(year, month - 1, day)
}

const leadingEmptyCount = computed(() => {
  const firstDate = dashboard.value?.heatmapDays?.[0]?.date
  if (!firstDate) return 0
  const day = parseLocalDate(firstDate).getDay()
  return (day + 6) % 7
})

const selectedDay = computed<LearningDayDetail>(() => {
  const matched = dashboard.value?.heatmapDays.find(item => item.date === selectedDate.value)
  if (matched) {
    return {
      date: matched.date,
      completedTaskCount: matched.completedTaskCount,
      aiUsageCount: matched.aiUsageCount,
      reminderCount: matched.reminderCount,
      gradedEventCount: matched.gradedEventCount,
      totalEventCount: matched.totalEventCount,
      timelineEvents: matched.timelineEvents
    }
  }
  return dashboard.value?.selectedDay || {
    date: selectedDate.value,
    completedTaskCount: 0,
    aiUsageCount: 0,
    reminderCount: 0,
    gradedEventCount: 0,
    totalEventCount: 0,
    timelineEvents: []
  }
})

const monthLabels = computed(() => {
  const labels: Array<{ label: string; column: number }> = []
  const cells = dashboard.value?.heatmapDays || []
  const offset = leadingEmptyCount.value

  cells.forEach((day, index) => {
    const date = parseLocalDate(day.date)
    const isFirstVisible = index === 0
    const isMonthStart = date.getDate() === 1
    if (!isFirstVisible && !isMonthStart) return

    labels.push({
      label: `${date.getMonth() + 1}月`,
      column: Math.floor((offset + index) / 7) + 1
    })
  })

  return labels
})

const hasNoteContent = computed(() =>
  [noteForm.completedSummary, noteForm.pendingSummary, noteForm.aiToolSummary, noteForm.reflection]
    .some(item => item && item.trim())
)

const loadDashboard = async () => {
  loading.value = true
  try {
    const userId = userStore.userInfo?.id
    if (!userId) {
      ElMessage.error('用户信息未加载')
      return
    }

    const res = await getLearningDashboard(userId, 112)
    if (res.code === 200 && res.data) {
      dashboard.value = res.data
      selectedDate.value = res.data.selectedDate || res.data.heatmapDays?.[res.data.heatmapDays.length - 1]?.date || ''
      hydrateNoteForm(res.data.selectedNote)
    } else {
      ElMessage.error(res.message || '加载学习看板失败')
    }
  } catch (error: any) {
    console.error('加载学习看板失败:', error)
    ElMessage.error(error?.message || '加载学习看板失败')
  } finally {
    loading.value = false
  }
}

const selectDay = (date: string) => {
  selectedDate.value = date
}

const hydrateNoteForm = (note?: LearningDailyNote) => {
  noteForm.id = note?.id
  noteForm.date = note?.date || selectedDate.value
  noteForm.completedSummary = note?.completedSummary || ''
  noteForm.pendingSummary = note?.pendingSummary || ''
  noteForm.aiToolSummary = note?.aiToolSummary || ''
  noteForm.reflection = note?.reflection || ''
  noteForm.hasContent = Boolean(note?.hasContent)
}

const saveNote = async () => {
  const userId = userStore.userInfo?.id
  if (!userId || !selectedDate.value) {
    ElMessage.warning('用户或日期信息缺失')
    return
  }

  savingNote.value = true
  try {
    const res = await saveLearningDashboardNote(userId, {
      date: selectedDate.value,
      completedSummary: noteForm.completedSummary,
      pendingSummary: noteForm.pendingSummary,
      aiToolSummary: noteForm.aiToolSummary,
      reflection: noteForm.reflection
    })
    if (res.code === 200 && res.data) {
      hydrateNoteForm(res.data)
      if (dashboard.value) {
        dashboard.value.selectedNote = res.data
      }
      ElMessage.success('学习笔记已保存')
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '保存失败')
  } finally {
    savingNote.value = false
  }
}

const loadSelectedDateNote = async (date: string) => {
  const userId = userStore.userInfo?.id
  if (!userId || !date) return
  try {
    const res = await getLearningDashboardNote(userId, date)
    if (res.code === 200 && res.data) {
      hydrateNoteForm(res.data)
      if (dashboard.value) {
        dashboard.value.selectedNote = res.data
      }
    }
  } catch (error) {
    console.error('加载学习笔记失败:', error)
  }
}

const formatFullDate = (date: string) =>
  parseLocalDate(date).toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' })

const formatDayTitle = (date: string) =>
  parseLocalDate(date).toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })

const displayValue = (value?: string) => (value && value.trim() ? value : '暂无记录')

watch(selectedDate, async value => {
  if (!value) return
  if (dashboard.value?.selectedNote?.date === value) {
    hydrateNoteForm(dashboard.value.selectedNote)
    return
  }
  hydrateNoteForm({
    date: value,
    completedSummary: '',
    pendingSummary: '',
    aiToolSummary: '',
    reflection: '',
    hasContent: false
  })
  await loadSelectedDateNote(value)
})

onMounted(() => {
  loadDashboard()
})
</script>

<style scoped lang="scss">
.learning-dashboard {
  display: flex;
  flex-direction: column;
  gap: 20px;
  color: #111827;
  background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
  min-height: 100vh;
  padding: 20px;

  .loading-state,
  .heatmap-section,
  .timeline-section {
    background: #fff;
    border: 1px solid #e5e7eb;
    border-radius: 12px;
    padding: 24px 28px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
    transition: all 0.3s ease;

    &:hover {
      box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12);
    }
  }

  .dashboard-header {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 16px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    padding: 28px 32px;
    border-radius: 16px;
    color: #fff;
    box-shadow: 0 8px 24px rgba(102, 126, 234, 0.3);

    h1 {
      margin: 0;
      font-size: 32px;
      font-weight: 700;
      text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    }

    p {
      margin: 8px 0 0;
      color: rgba(255, 255, 255, 0.9);
      font-size: 14px;
    }

    .el-button {
      background: rgba(255, 255, 255, 0.2);
      border: 1px solid rgba(255, 255, 255, 0.3);
      color: #fff;
      backdrop-filter: blur(10px);
      transition: all 0.3s ease;

      &:hover {
        background: rgba(255, 255, 255, 0.3);
        transform: translateY(-2px);
      }
    }
  }

  .summary-strip {
    display: grid;
    grid-template-columns: repeat(5, minmax(0, 1fr));
    gap: 16px;
  }

  .summary-item {
    background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
    border: 1px solid #e5e7eb;
    border-radius: 12px;
    padding: 20px 22px;
    min-height: 110px;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
    transition: all 0.3s ease;
    position: relative;
    overflow: hidden;

    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      height: 4px;
      background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
    }

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
    }

    strong {
      font-size: 32px;
      line-height: 1;
      font-weight: 700;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }
  }

  .summary-label {
    color: #6b7280;
    font-size: 13px;
    font-weight: 500;
    text-transform: uppercase;
    letter-spacing: 0.5px;
  }

  .dashboard-shell {
    max-width: 1400px;
    margin: 0 auto;
    width: 100%;
  }

  .heatmap-layout {
    display: flex;
    gap: 24px;
    align-items: stretch;
    flex-wrap: wrap;
  }

  .heatmap-card {
    flex: 1.4;
    min-width: 0;
    display: flex;
    flex-direction: column;
    border: 1px solid #e5e7eb;
    border-radius: 12px;
    background: #ffffff;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
    transition: all 0.3s ease;

    &:hover {
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
    }
  }

  .heatmap-card-head {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 16px;
    padding: 20px 24px 16px;
    border-bottom: 1px solid #f0f2f5;
    background: linear-gradient(135deg, #f8f9fa 0%, #ffffff 100%);
  }

  .heatmap-title-group {
    h2 {
      margin: 0;
      font-size: 24px;
      font-weight: 700;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
    }

    p {
      margin: 8px 0 0;
      color: #6b7280;
      font-size: 14px;
    }
  }

  .legend {
    display: flex;
    align-items: center;
    gap: 10px;
    color: #6b7280;
    font-size: 12px;
    flex-shrink: 0;
    padding-top: 4px;
    background: rgba(102, 126, 234, 0.05);
    padding: 8px 12px;
    border-radius: 8px;
  }

  .legend-scale {
    display: flex;
    gap: 4px;
  }

  .legend-cell,
  .heatmap-cell {
    width: 20px;
    height: 20px;
    border-radius: 5px;
    border: 0;
  }

  .heatmap-board {
    padding: 18px 20px 20px;
    display: flex;
    justify-content: center;
  }

  .heatmap-scroll {
    overflow-x: auto;
    width: 100%;
  }

  .month-row {
    display: grid;
    grid-auto-flow: column;
    grid-auto-columns: 20px;
    column-gap: 5px;
    min-height: 24px;
    margin-bottom: 10px;
  }

  .month-label {
    color: #6b7280;
    font-size: 13px;
    white-space: nowrap;
    font-weight: 500;
  }

  .heatmap-main {
    display: flex;
    gap: 12px;
    align-items: flex-start;
    min-width: max-content;
    margin: 0 auto;
  }

  .weekday-column {
    width: 28px;
    display: grid;
    grid-template-rows: repeat(7, 20px);
    gap: 5px;
    font-size: 12px;
    color: #6b7280;

    span {
      display: flex;
      align-items: center;
      justify-content: flex-start;
      font-weight: 500;
    }
  }

  .heatmap-grid {
    display: grid;
    grid-auto-flow: column;
    gap: 5px;
  }

  .heatmap-cell {
    padding: 0;
    cursor: pointer;
    box-shadow: inset 0 0 0 1px rgba(17, 24, 39, 0.04);
    transition: all 0.2s ease;

    &:hover {
      transform: scale(1.15);
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
    }

    &.blank {
      background: transparent;
      box-shadow: none;
      cursor: default;

      &:hover {
        transform: none;
      }
    }

    &.active {
      box-shadow: inset 0 0 0 2px #667eea, 0 0 0 4px rgba(102, 126, 234, 0.2);
    }
  }

  .level-0 {
    background: #eef2f7;
  }

  .level-1 {
    background: #d7f4df;
  }

  .level-2 {
    background: #9ae6b4;
  }

  .level-3 {
    background: #4cc38a;
  }

  .level-4 {
    background: #13795b;
  }

  .tooltip-content {
    font-size: 12px;
    line-height: 1.7;
  }

  .note-panel {
    flex: 1;
    min-width: 320px;
    border: 1px solid #e5e7eb;
    border-radius: 12px;
    background: linear-gradient(135deg, #fcfcfd 0%, #f8f9fa 100%);
    padding: 20px;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
    transition: all 0.3s ease;

    &:hover {
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
    }
  }

  .note-panel-head,
  .note-display-head {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 12px;
    padding-bottom: 16px;
    border-bottom: 2px solid linear-gradient(90deg, #667eea 0%, #764ba2 100%);
    margin-bottom: 16px;
  }

  .note-panel-head h3,
  .note-display-head h3 {
    margin: 0;
    font-size: 20px;
    font-weight: 700;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  .note-panel-head p,
  .note-display-head span {
    margin: 6px 0 0;
    color: #6b7280;
    font-size: 12px;
    font-weight: 500;
  }

  .note-form {
    margin-top: 14px;
  }

  :deep(.note-form .el-textarea__inner) {
    min-height: 110px;
    border-radius: 8px;
    border: 1px solid #e5e7eb;
    transition: all 0.3s ease;

    &:focus {
      border-color: #667eea;
      box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
    }
  }

  .note-display {
    margin-top: 18px;
    padding: 20px;
    border: 1px solid #e5e7eb;
    border-radius: 12px;
    background: linear-gradient(135deg, #fcfcfd 0%, #f8f9fa 100%);
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.06);
  }

  .note-grid {
    margin-top: 14px;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 14px;
  }

  .note-block {
    border-radius: 10px;
    background: #fff;
    border: 1px solid #e5e7eb;
    padding: 16px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    }

    label {
      display: block;
      font-size: 12px;
      color: #6b7280;
      margin-bottom: 8px;
      font-weight: 500;
      text-transform: uppercase;
      letter-spacing: 0.5px;
    }

    p {
      margin: 0;
      font-size: 14px;
      line-height: 1.7;
      color: #111827;
      white-space: pre-wrap;
      word-break: break-word;
    }
  }

  .section-head {
    display: flex;
    align-items: flex-end;
    justify-content: space-between;
    gap: 16px;
    margin-bottom: 20px;
    padding-bottom: 16px;
    border-bottom: 2px solid linear-gradient(90deg, #667eea 0%, #764ba2 100%);
  }

  .section-head h2 {
    margin: 0;
    font-size: 24px;
    font-weight: 700;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }

  .section-head p {
    margin: 8px 0 0;
    color: #6b7280;
    font-size: 14px;
  }

  .day-stats {
    display: flex;
    align-items: center;
    gap: 10px;
    flex-wrap: wrap;

    span {
      font-size: 12px;
      color: #4b5563;
      background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
      padding: 6px 12px;
      border-radius: 999px;
      font-weight: 500;
      transition: all 0.3s ease;

      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      }
    }
  }

  .timeline-list {
    display: flex;
    flex-direction: column;
  }

  .timeline-row {
    display: grid;
    grid-template-columns: 72px 32px minmax(0, 1fr);
    gap: 14px;
    min-height: 96px;
    transition: all 0.3s ease;

    &:hover {
      background: rgba(102, 126, 234, 0.02);
      border-radius: 8px;
    }

    &:last-child .timeline-line {
      display: none;
    }
  }

  .timeline-time {
    padding-top: 8px;
    font-size: 13px;
    color: #6b7280;
    text-align: right;
    font-weight: 500;
  }

  .timeline-track {
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .timeline-dot {
    width: 28px;
    height: 28px;
    border-radius: 999px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    color: #fff;
    flex-shrink: 0;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
    transition: all 0.3s ease;

    &:hover {
      transform: scale(1.1);
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
    }
  }

  .timeline-line {
    width: 2px;
    flex: 1;
    margin-top: 8px;
    background: linear-gradient(180deg, #d1d5db 0%, transparent 100%);
  }

  .timeline-card {
    border: 1px solid #e5e7eb;
    border-radius: 10px;
    padding: 16px 18px;
    margin-bottom: 16px;
    background: #fff;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
    transition: all 0.3s ease;

    &:hover {
      transform: translateY(-2px);
      box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
    }
  }

  .card-topline {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    gap: 14px;
  }

  .card-title-group h3 {
    margin: 0;
    font-size: 16px;
    font-weight: 600;
  }

  .card-title-group p {
    margin: 6px 0 0;
    color: #6b7280;
    font-size: 13px;
  }

  .badge-group {
    display: flex;
    flex-wrap: wrap;
    justify-content: flex-end;
    gap: 8px;
  }

  .chip {
    display: inline-flex;
    align-items: center;
    border-radius: 999px;
    padding: 5px 10px;
    font-size: 12px;
    line-height: 1;
    font-weight: 500;
  }

  .chip-muted {
    background: linear-gradient(135deg, #f3f4f6 0%, #e5e7eb 100%);
    color: #4b5563;
  }

  .chip-score {
    background: linear-gradient(135deg, #dbeafe 0%, #bfdbfe 100%);
    color: #1d4ed8;
  }

  .chip-ai {
    background: linear-gradient(135deg, #ede9fe 0%, #ddd6fe 100%);
    color: #6d28d9;
  }

  .chip-reminder {
    background: linear-gradient(135deg, #fef3c7 0%, #fde68a 100%);
    color: #92400e;
  }

  .event-detail,
  .ai-detail,
  .ai-result {
    margin-top: 12px;
    font-size: 14px;
    line-height: 1.7;
    color: #374151;
  }

  .ai-meta {
    margin-top: 12px;
    padding: 14px;
    border-radius: 10px;
    background: linear-gradient(135deg, #faf5ff 0%, #f3e8ff 100%);
    border: 1px solid #e9d5ff;
  }

  .ai-head {
    display: flex;
    align-items: center;
    flex-wrap: wrap;
    gap: 10px;
    font-size: 12px;
  }

  .ai-model {
    color: #6d28d9;
    font-weight: 600;
  }

  .ai-action {
    color: #7c3aed;
    background: rgba(124, 58, 237, 0.08);
    padding: 4px 8px;
    border-radius: 999px;
  }

  .ai-result {
    color: #5b21b6;
  }

  .type-task .timeline-dot {
    background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  }

  .type-ai_usage .timeline-dot {
    background: linear-gradient(135deg, #7c3aed 0%, #6d28d9 100%);
  }

  .type-reminder .timeline-dot {
    background: linear-gradient(135deg, #d97706 0%, #b45309 100%);
  }

  .status-graded .timeline-dot {
    background: linear-gradient(135deg, #059669 0%, #047857 100%);
  }

  .empty-day {
    padding: 30px 0;
  }

  @media (max-width: 1200px) {
    .summary-strip {
      grid-template-columns: repeat(3, minmax(0, 1fr));
    }
  }

  @media (max-width: 1024px) {
    .heatmap-layout {
      flex-direction: column;
    }

    .note-panel {
      min-width: 0;
      width: 100%;
    }
  }

  @media (max-width: 768px) {
    .dashboard-header,
    .section-head,
    .heatmap-card-head,
    .note-panel-head,
    .note-display-head {
      flex-direction: column;
      align-items: flex-start;
    }

    .summary-strip {
      grid-template-columns: repeat(2, minmax(0, 1fr));
    }

    .note-grid {
      grid-template-columns: 1fr;
    }

    .timeline-row {
      grid-template-columns: 56px 28px minmax(0, 1fr);
      gap: 10px;
    }

    .timeline-time {
      font-size: 12px;
    }
  }
}
</style>
