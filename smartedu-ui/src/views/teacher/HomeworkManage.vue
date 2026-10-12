<template>
  <div class="homework-manage-container page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>作业管理</span>
          <div class="header-actions">
            <el-button @click="openCourseSelectorDialog">
              选择课程
            </el-button>
            <el-button type="primary" @click="openPublishDialog">
              <el-icon><Plus /></el-icon>
              发布作业
            </el-button>
          </div>
        </div>
      </template>

      <!-- 筛选条件 -->
      <div class="filter-bar">
        <el-select v-model="filterCourse" placeholder="选择课程" style="width: 200px" clearable @change="loadHomeworkList">
          <el-option v-for="course in teacherCourseList" :key="course.id" :label="course.courseName" :value="course.id" />
        </el-select>
        <el-button @click="loadHomeworkList">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
      </div>

      <!-- 作业列表 -->
      <el-table :data="homeworkList" style="width: 100%" v-loading="loading">
        <el-table-column prop="title" label="作业标题" min-width="200" />
        <el-table-column prop="courseName" label="课程" width="150" />
        <el-table-column prop="attachmentName" label="附件" width="200">
          <template #default="{ row }">
            <span v-if="row.attachmentName">{{ row.attachmentName }}</span>
            <span v-else class="text-muted">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.startTime) }}</template>
        </el-table-column>
        <el-table-column prop="endTime" label="截止时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column prop="submittedCount" label="已提交" width="80">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="viewSubmissions(row)">
              {{ row.submittedCount || 0 }}人
            </el-button>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)">{{ getStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="warning" link size="small" @click="openAiAnalysisDrawer(row)">
              <el-icon><Reading /></el-icon>
              AI 解析
            </el-button>
            <el-button type="success" link size="small" @click="autoGradeHomework(row)" :loading="row.autoGrading">
              <el-icon><MagicStick /></el-icon>
              AI 批改
            </el-button>
            <el-button type="primary" link size="small" @click="viewSubmissions(row)">批改</el-button>
            <el-button type="danger" link size="small" @click="deleteHomework(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadHomeworkList"
        />
      </div>
    </el-card>

    <!-- 发布作业对话框 -->
    <el-dialog v-model="showPublishDialog" title="发布作业" width="600px" destroy-on-close>
      <el-form :model="publishForm" label-width="100px" :rules="formRules" ref="formRef">
        <el-form-item label="作业标题" prop="title">
          <el-input v-model="publishForm.title" placeholder="请输入作业标题" />
        </el-form-item>
        <el-form-item label="所属课程" prop="courseId">
          <div style="display: flex; gap: 10px;">
            <el-select v-model="publishForm.courseId" placeholder="请选择课程" style="flex: 1;">
              <el-option v-for="course in teacherCourseList" :key="course.id" :label="course.courseName" :value="course.id" />
            </el-select>
            <el-button type="primary" @click="showCreateCourseDialog = true">新建课程</el-button>
          </div>
        </el-form-item>
        <el-form-item label="作业附件" prop="file">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".docx,.doc,.pdf"
            :on-change="handleFileChange"
            :on-exceed="handleExceed"
            :file-list="fileList"
          >
            <template #trigger>
              <el-button type="primary">选择文件</el-button>
            </template>
            <template #tip>
              <div class="el-upload__tip">支持 Word、PDF 文件，最大 50MB</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="作业描述">
          <el-input v-model="publishForm.description" type="textarea" :rows="3" placeholder="请输入作业描述（可选）" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker
            v-model="publishForm.startTime"
            type="datetime"
            placeholder="不填则立即开始"
            style="width: 100%"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker
            v-model="publishForm.endTime"
            type="datetime"
            placeholder="不填则7天后截止"
            style="width: 100%"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPublishDialog = false">取消</el-button>
        <el-button type="primary" @click="handlePublish" :loading="publishing">立即发布</el-button>
      </template>
    </el-dialog>

    <!-- 查看提交对话框 -->
    <el-dialog v-model="showSubmissionsDialog" title="学生提交" width="900px">
      <el-table :data="submissionList" v-loading="submissionLoading">
        <el-table-column prop="studentName" label="学生" width="100" />
        <el-table-column prop="attachmentName" label="提交内容" min-width="200">
          <template #default="{ row }">
            <a v-if="row.attachmentUrl" :href="getFileUrl(row.attachmentUrl)" target="_blank" class="file-link">
              {{ row.attachmentName || '查看附件' }}
            </a>
            <span v-else-if="row.submissionContent" class="text-muted">{{ row.submissionContent?.substring(0, 50) }}{{ row.submissionContent?.length > 50 ? '...' : '' }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="submitTime" label="提交时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.submitTime) }}</template>
        </el-table-column>
        <el-table-column prop="score" label="得分" width="80">
          <template #default="{ row }">{{ row.score ?? '-' }}</template>
        </el-table-column>
        <el-table-column prop="gradeStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.gradeStatus === 2 ? 'success' : 'warning'" size="small">
              {{ row.gradeStatus === 2 ? '已批改' : '待批改' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openGradeDialog(row)">批改</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 批改对话框 -->
    <el-dialog v-model="showGradeDialog" title="批改作业" width="500px">
      <el-form :model="gradeForm" label-width="80px">
        <el-form-item label="得分">
          <el-input-number v-model="gradeForm.score" :min="0" :max="100" :precision="1" />
        </el-form-item>
        <el-form-item label="评语">
          <el-input v-model="gradeForm.comment" type="textarea" :rows="3" placeholder="请输入评语（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showGradeDialog = false">取消</el-button>
        <el-button type="primary" @click="submitGrade" :loading="gradeLoading">提交</el-button>
      </template>
    </el-dialog>

    <!-- 创建课程对话框 -->
    <el-dialog v-model="showCreateCourseDialog" title="创建课程" width="500px">
      <el-form :model="courseForm" label-width="100px">
        <el-form-item label="课程名称" required>
          <el-input v-model="courseForm.courseName" placeholder="请输入课程名称" />
        </el-form-item>
        <el-form-item label="课程代码" required>
          <el-input v-model="courseForm.courseCode" placeholder="如 CS101" />
        </el-form-item>
        <el-form-item label="学分">
          <el-input-number v-model="courseForm.credit" :min="0.5" :max="10" :precision="1" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateCourseDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateCourse" :loading="courseCreating">创建</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showCourseSelectorDialog" title="选择自己教授的课程" width="560px">
      <div class="course-selector-tip">
        先勾选当前教师负责的课程，作业发布和智学助手都会基于这里的课程列表工作。
      </div>
      <el-checkbox-group v-model="selectedTeacherCourseIds" class="course-selector-list">
        <el-checkbox
          v-for="course in availableCourseList"
          :key="course.id"
          :label="course.id"
          class="course-selector-item"
        >
          <div class="course-option">
            <span class="course-option-name">{{ course.courseName }}</span>
            <span class="course-option-meta">{{ course.courseCode || '未填写课程代码' }}</span>
          </div>
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="showCourseSelectorDialog = false">取消</el-button>
        <el-button type="primary" @click="saveTeacherCourses" :loading="savingTeacherCourses">保存课程</el-button>
      </template>
    </el-dialog>

    <!-- AI 解析审核抽屉 -->
    <el-drawer
      v-model="showAiAnalysisDrawer"
      title="AI 解析审核"
      direction="rtl"
      size="600px"
      :destroy-on-close="true"
    >
      <div v-if="aiAnalysisData" class="ai-analysis-drawer">
        <!-- 状态提示 -->
        <div class="status-banner">
          <el-tag
            :type="getAnalysisStatusType(aiAnalysisData.aiAnalysisStatus)"
            size="large"
          >
            {{ getAnalysisStatusText(aiAnalysisData.aiAnalysisStatus) }}
          </el-tag>
          <span class="homework-title">{{ aiAnalysisData.title }}</span>
        </div>

        <!-- 解析内容编辑区 -->
        <div class="analysis-content-section">
          <div class="section-header">
            <span class="section-title">解析内容</span>
            <el-tag v-if="aiAnalysisData.aiAnalysisStatus === 1" type="info" size="small">
              生成中...
            </el-tag>
            <el-tag v-else-if="aiAnalysisData.aiAnalysisStatus === 2" type="warning" size="small">
              待审核
            </el-tag>
            <el-tag v-else-if="aiAnalysisData.aiAnalysisStatus === 3" type="danger" size="small">
              生成失败
            </el-tag>
            <el-tag v-else-if="aiAnalysisData.aiAnalysisStatus === 4" type="success" size="small">
              已发布
            </el-tag>
            <el-tag v-else type="info" size="small">
              未生成
            </el-tag>
          </div>

          <el-input
            v-model="editedAnalysisContent"
            type="textarea"
            :rows="18"
            placeholder="AI 解析内容将在此显示..."
            :disabled="aiAnalysisData.aiAnalysisStatus === 0 || aiAnalysisData.aiAnalysisStatus === 1"
            class="analysis-textarea"
          />

          <div class="content-tips">
            <el-icon><InfoFilled /></el-icon>
            <span>可以直接修改解析内容后再发布，修改后的内容将作为最终版本展示给学生</span>
          </div>
        </div>

        <!-- 操作按钮 -->
        <div class="action-buttons">
          <el-button @click="refreshAiAnalysis" :loading="analysisLoading">
            <el-icon><Refresh /></el-icon>
            刷新状态
          </el-button>
          <!-- 生成中提示 -->
          <el-button
            v-if="aiAnalysisData.aiAnalysisStatus === 1"
            type="info"
            disabled
          >
            <el-icon class="is-loading"><Loading /></el-icon>
            AI 正在生成中...
          </el-button>
          <!-- 生成失败时显示重试按钮 -->
          <el-button
            v-if="aiAnalysisData.aiAnalysisStatus === 3"
            type="warning"
            @click="handleRetryAnalysis"
            :loading="approvingAnalysis"
          >
            <el-icon><RefreshRight /></el-icon>
            重新生成
          </el-button>
          <!-- 待审核状态的操作按钮 -->
          <el-button
            v-if="aiAnalysisData.aiAnalysisStatus === 2"
            type="primary"
            @click="handleApproveAnalysis(false)"
            :loading="approvingAnalysis"
          >
            <el-icon><Check /></el-icon>
            采用并发布
          </el-button>
          <el-button
            v-if="aiAnalysisData.aiAnalysisStatus === 2"
            type="success"
            @click="handleApproveAnalysis(true)"
            :loading="approvingAnalysis"
          >
            <el-icon><Edit /></el-icon>
            修改后发布
          </el-button>
          <!-- 已发布状态可以更新 -->
          <el-button
            v-if="aiAnalysisData.aiAnalysisStatus === 4"
            type="warning"
            @click="handleApproveAnalysis(true)"
            :loading="approvingAnalysis"
          >
            <el-icon><Edit /></el-icon>
            更新解析
          </el-button>
        </div>

        <!-- 未生成提示 -->
        <div v-if="aiAnalysisData.aiAnalysisStatus === 0" class="help-section">
          <el-alert
            title="AI 解析尚未生成"
            type="info"
            description="发布作业后，系统会自动调用 AI 生成解析内容。如果长时间未生成，请点击「刷新状态」或检查后端日志。"
            show-icon
            :closable="false"
          />
        </div>

        <!-- 生成中提示 -->
        <div v-if="aiAnalysisData.aiAnalysisStatus === 1" class="help-section">
          <el-alert
            title="AI 解析正在生成中..."
            type="warning"
            description="系统正在调用 AI 生成解析内容，通常需要 10-30 秒。生成完成后状态会变为「待审核」，届时您可以审核并发布。"
            show-icon
            :closable="false"
          />
        </div>

        <!-- 生成失败提示 -->
        <div v-if="aiAnalysisData.aiAnalysisStatus === 3" class="help-section">
          <el-alert
            title="AI 解析生成失败"
            type="error"
            :description="aiAnalysisData.aiAnalysisContent || '请检查 AI 服务配置或稍后重试'"
            show-icon
            :closable="false"
          />
        </div>
      </div>

      <!-- 加载中 -->
      <div v-else class="empty-state">
        <el-empty description="请选择作业查看 AI 解析" />
      </div>
    </el-drawer>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, RefreshRight, MagicStick, Reading, Check, Edit, InfoFilled, Loading } from '@element-plus/icons-vue'
import { courseApi, homeworkApi } from '@/api/teacher'

// 数据
const availableCourseList = ref<any[]>([])
const teacherCourseList = ref<any[]>([])
const homeworkList = ref<any[]>([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const filterCourse = ref('')
const showCourseSelectorDialog = ref(false)
const savingTeacherCourses = ref(false)
const selectedTeacherCourseIds = ref<number[]>([])

// 批改状态
const autoGradingMap = ref<Record<number, boolean>>({})

// 发布表单
const showPublishDialog = ref(false)
const publishing = ref(false)
const formRef = ref()
const uploadRef = ref()
const fileList = ref<any[]>([])
const publishForm = reactive({
  title: '',
  courseId: null as number | null,
  description: '',
  startTime: '',
  endTime: '',
  file: null as File | null
})
const formRules = {
  title: [{ required: true, message: '请输入作业标题', trigger: 'blur' }],
  courseId: [{ required: true, message: '请选择课程', trigger: 'change' }]
}

// 提交列表
const showSubmissionsDialog = ref(false)
const submissionList = ref<any[]>([])
const submissionLoading = ref(false)
const currentHomework = ref<any>(null)

// 批改
const showGradeDialog = ref(false)
const gradeLoading = ref(false)
const currentSubmission = ref<any>(null)
const gradeForm = reactive({ score: 0, comment: '' })

// 创建课程
const showCreateCourseDialog = ref(false)
const courseCreating = ref(false)
const courseForm = reactive({ courseName: '', courseCode: '', credit: 3 })

// AI 解析审核
const showAiAnalysisDrawer = ref(false)
const aiAnalysisData = ref<any>(null)
const editedAnalysisContent = ref('')
const analysisLoading = ref(false)
const approvingAnalysis = ref(false)
const currentAnalysisHomework = ref<any>(null)

// 加载课程
const loadCourseList = async () => {
  try {
    const res = await courseApi.getList()
    if (res.code === 200) availableCourseList.value = res.data || []
  } catch (e) {
    console.error('加载课程失败:', e)
  }
}

const loadTeacherCourseList = async () => {
  try {
    const res = await courseApi.getTeacherCourses()
    if (res.code === 200) {
      teacherCourseList.value = res.data || []
      selectedTeacherCourseIds.value = teacherCourseList.value.map((course: any) => course.id)
    }
  } catch (e) {
    console.error('加载教师课程失败:', e)
  }
}

// 加载作业
const loadHomeworkList = async () => {
  loading.value = true
  try {
    const res = await homeworkApi.getList({
      courseId: filterCourse.value || undefined,
      pageNum: currentPage.value,
      pageSize: pageSize.value
    })
    console.log('作业列表响应:', res)  // 调试日志

    if (res.code === 200) {
      // 增加健壮性检查：确保数据是数组
      const listData = res.data?.records || res.data?.list || res.data || []
      homeworkList.value = Array.isArray(listData) ? listData : []
      total.value = res.data?.total || 0
    } else {
      console.error('接口返回异常:', res)
      homeworkList.value = []
    }
  } catch (e) {
    console.error('加载作业失败:', e)
    ElMessage.error('加载作业失败，请检查后端服务')
    homeworkList.value = []  // 确保出错时也是数组
  } finally {
    loading.value = false
  }
}

// 打开发布对话框
const openPublishDialog = () => {
  if (!teacherCourseList.value.length) {
    ElMessage.warning('请先点击“选择课程”，勾选当前教师负责的课程')
    return
  }
  publishForm.title = ''
  publishForm.courseId = null
  publishForm.description = ''
  publishForm.startTime = ''
  publishForm.endTime = ''
  publishForm.file = null
  fileList.value = []
  showPublishDialog.value = true
}

const openCourseSelectorDialog = async () => {
  await Promise.all([loadCourseList(), loadTeacherCourseList()])
  showCourseSelectorDialog.value = true
}

const saveTeacherCourses = async () => {
  savingTeacherCourses.value = true
  try {
    const res = await courseApi.assignTeacherCourses(selectedTeacherCourseIds.value)
    if (res.code === 200) {
      ElMessage.success('课程已更新')
      showCourseSelectorDialog.value = false
      await loadTeacherCourseList()

      if (publishForm.courseId && !selectedTeacherCourseIds.value.includes(publishForm.courseId)) {
        publishForm.courseId = null
      }
      if (filterCourse.value && !selectedTeacherCourseIds.value.includes(Number(filterCourse.value))) {
        filterCourse.value = ''
      }

      await loadHomeworkList()
    } else {
      ElMessage.error(res.message || '课程更新失败')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '课程更新失败')
  } finally {
    savingTeacherCourses.value = false
  }
}

// 文件选择
const handleFileChange = (file: any) => {
  publishForm.file = file.raw
}

const handleExceed = () => {
  ElMessage.warning('只能上传一个文件')
}

// 发布作业
const handlePublish = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  if (!publishForm.file) {
    ElMessage.warning('请选择作业附件')
    return
  }

  publishing.value = true
  try {
    const formData = new FormData()
    formData.append('file', publishForm.file)
    formData.append('title', publishForm.title)
    formData.append('courseId', String(publishForm.courseId))
    if (publishForm.description) formData.append('description', publishForm.description)
    if (publishForm.startTime) formData.append('startTime', publishForm.startTime)
    if (publishForm.endTime) formData.append('endTime', publishForm.endTime)

    const res = await homeworkApi.quickPublish(formData)
    if (res.code === 200) {
      ElMessage.success('发布成功')
      showPublishDialog.value = false
      loadHomeworkList()
    } else {
      ElMessage.error(res.message || '发布失败')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '发布失败')
  } finally {
    publishing.value = false
  }
}

// 查看提交
const viewSubmissions = async (row: any) => {
  currentHomework.value = row
  submissionLoading.value = true
  showSubmissionsDialog.value = true
  try {
    const res = await homeworkApi.getSubmissions(row.id)
    if (res.code === 200) {
      submissionList.value = res.data?.records || res.data?.list || res.data || []
    }
  } catch (e) {
    console.error('加载提交失败:', e)
  } finally {
    submissionLoading.value = false
  }
}

// 批改
const openGradeDialog = (submission: any) => {
  currentSubmission.value = submission
  gradeForm.score = submission.score || 0
  gradeForm.comment = submission.comment || ''
  showGradeDialog.value = true
}

const submitGrade = async () => {
  if (!currentSubmission.value) return
  gradeLoading.value = true
  try {
    await homeworkApi.gradeSubmission(currentSubmission.value.id, gradeForm.score, gradeForm.comment)
    ElMessage.success('批改成功')
    showGradeDialog.value = false
    viewSubmissions(currentHomework.value)
  } catch (e: any) {
    ElMessage.error(e.message || '批改失败')
  } finally {
    gradeLoading.value = false
  }
}

// 删除作业
const deleteHomework = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定要删除该作业吗？', '提示', { type: 'warning' })
    await homeworkApi.delete(row.id)
    ElMessage.success('删除成功')
    loadHomeworkList()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

// AI 一键批改
const autoGradeHomework = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定要使用 AI 自动批改该作业吗？\n\n客观题（选择、判断、填空）将自动判分\n主观题（简答、论述）将由 AI 智能评分', 'AI 批改', {
      type: 'warning',
      confirmButtonText: '开始批改',
      cancelButtonText: '取消'
    })

    autoGradingMap.value[row.id] = true
    const res = await homeworkApi.autoGrade(row.id)

    if (res.code === 200) {
      const data = res.data
      ElMessage.success(data.message || `成功批改 ${data.gradedCount} 份作业`)
      loadHomeworkList()
    } else {
      ElMessage.error(res.message || '批改失败')
    }
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '批改失败')
  } finally {
    autoGradingMap.value[row.id] = false
  }
}

// 创建课程
const handleCreateCourse = async () => {
  if (!courseForm.courseName || !courseForm.courseCode) {
    ElMessage.warning('请填写课程名称和代码')
    return
  }
  courseCreating.value = true
  try {
    const res = await courseApi.create(courseForm)
    if (res.code === 200) {
      ElMessage.success('创建成功')
      showCreateCourseDialog.value = false
      await loadCourseList()
      await loadTeacherCourseList()
      if (res.data) publishForm.courseId = res.data
    }
  } catch (e: any) {
    ElMessage.error(e.message || '创建失败')
  } finally {
    courseCreating.value = false
  }
}

// ========== AI 解析相关方法 ==========

// 打开 AI 解析审核抽屉
const openAiAnalysisDrawer = async (row: any) => {
  currentAnalysisHomework.value = row
  showAiAnalysisDrawer.value = true
  await loadAiAnalysis(row.id)
}

// 加载 AI 解析内容
const loadAiAnalysis = async (homeworkId: number) => {
  analysisLoading.value = true
  try {
    const res = await homeworkApi.getAiAnalysis(homeworkId)
    if (res.code === 200 && res.data) {
      aiAnalysisData.value = res.data
      // 优先显示老师修改后的版本，否则显示 AI 原始版本
      editedAnalysisContent.value = res.data.teacherEditedAnalysis || res.data.aiAnalysisContent || ''
    } else {
      ElMessage.error(res.message || '获取解析失败')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '获取解析失败')
  } finally {
    analysisLoading.value = false
  }
}

// 刷新 AI 解析状态
const refreshAiAnalysis = async () => {
  if (currentAnalysisHomework.value) {
    await loadAiAnalysis(currentAnalysisHomework.value.id)
    ElMessage.success('状态已刷新')
  }
}

// 审核并发布解析
const handleApproveAnalysis = async (withEdit: boolean) => {
  if (!aiAnalysisData.value) return

  if (withEdit && !editedAnalysisContent.value.trim()) {
    ElMessage.warning('请输入解析内容')
    return
  }

  approvingAnalysis.value = true
  try {
    const contentToSubmit = withEdit ? editedAnalysisContent.value : undefined
    const res = await homeworkApi.approveAiAnalysis(aiAnalysisData.value.homeworkId, contentToSubmit)

    if (res.code === 200) {
      ElMessage.success(withEdit ? '修改后发布成功' : '解析已发布')
      await loadAiAnalysis(aiAnalysisData.value.homeworkId)
      loadHomeworkList() // 刷新列表状态
    } else {
      ElMessage.error(res.message || '发布失败')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '发布失败')
  } finally {
    approvingAnalysis.value = false
  }
}

// 重新生成解析
const handleRetryAnalysis = async () => {
  if (!aiAnalysisData.value) return

  approvingAnalysis.value = true
  try {
    const res = await homeworkApi.retryAiAnalysis(aiAnalysisData.value.homeworkId)

    if (res.code === 200) {
      ElMessage.success('已重新触发 AI 解析生成')
      await loadAiAnalysis(aiAnalysisData.value.homeworkId)
    } else {
      ElMessage.error(res.message || '重试失败')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '重试失败')
  } finally {
    approvingAnalysis.value = false
  }
}

// 解析状态样式
const getAnalysisStatusType = (status: number) => {
  const types: Record<number, any> = { 0: 'info', 1: 'warning', 2: 'success', 3: 'danger' }
  return types[status] || 'info'
}

const getAnalysisStatusText = (status: number) => {
  const texts: Record<number, string> = { 0: '未生成', 1: '待审核', 2: '已发布', 3: '生成失败' }
  return texts[status] || '未知'
}

// 工具函数
const formatDateTime = (datetime: string) => {
  if (!datetime) return '-'
  return datetime.replace('T', ' ').substring(0, 16)
}

const getStatusType = (status: number) => {
  const types: Record<number, any> = { 0: 'info', 1: 'primary', 2: 'warning', 3: 'success' }
  return types[status] || 'info'
}

const getStatusText = (status: number) => {
  const texts: Record<number, string> = { 0: '草稿', 1: '进行中', 2: '已截止', 3: '已批阅' }
  return texts[status] || '未知'
}

const getFileUrl = (url: string) => {
  if (!url) return '#'
  if (url.startsWith('http')) return url
  // 如果已经是完整路径（以 /api 开头），直接返回
  if (url.startsWith('/api')) return url
  // 否则拼接 /api 前缀
  return '/api' + url
}

onMounted(() => {
  loadCourseList()
  loadTeacherCourseList()
  loadHomeworkList()
})
</script>

<style scoped lang="scss">
.homework-manage-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  .header-actions {
    display: flex;
    gap: 12px;
  }
  .filter-bar {
    display: flex;
    gap: 15px;
    margin-bottom: 20px;
  }
  .pagination-bar {
    display: flex;
    justify-content: center;
    margin-top: 20px;
  }
  .text-muted {
    color: #909399;
  }
  .file-link {
    color: #409eff;
    text-decoration: none;
    &:hover {
      text-decoration: underline;
    }
  }
}

.course-selector-tip {
  margin-bottom: 16px;
  padding: 12px 14px;
  border-radius: 10px;
  background: #f5f7fa;
  color: #606266;
  line-height: 1.6;
}

.course-selector-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
  max-height: 360px;
  overflow-y: auto;
}

.course-selector-item {
  margin-right: 0;
  padding: 12px 14px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
}

.course-option {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.course-option-name {
  font-weight: 600;
  color: #303133;
}

.course-option-meta {
  font-size: 12px;
  color: #909399;
}

// AI 解析审核抽屉样式
.ai-analysis-drawer {
  .status-banner {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    border-radius: 12px;
    margin-bottom: 24px;

    .homework-title {
      color: white;
      font-size: 16px;
      font-weight: 500;
    }
  }

  .analysis-content-section {
    background: #f8f9fa;
    border-radius: 12px;
    padding: 20px;
    margin-bottom: 24px;

    .section-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 16px;

      .section-title {
        font-size: 16px;
        font-weight: 600;
        color: #303133;
      }
    }

    .analysis-textarea {
      :deep(.el-textarea__inner) {
        font-family: 'Microsoft YaHei', sans-serif;
        line-height: 1.8;
        background: white;
        border-radius: 8px;
        border: 1px solid #e4e7ed;
        padding: 16px;
      }
    }

    .content-tips {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-top: 12px;
      padding: 12px;
      background: #ecf5ff;
      border-radius: 8px;
      color: #409eff;
      font-size: 13px;
    }
  }

  .action-buttons {
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
    padding: 16px;
    background: white;
    border-radius: 12px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  }

  .help-section {
    margin-top: 24px;
  }
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 300px;
}
</style>
