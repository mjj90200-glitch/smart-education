<template>
  <div class="homework-container page-container">
    <!-- 筛选 -->
    <el-card class="filter-card">
      <el-form :inline="true">
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 150px">
            <el-option label="未开始" :value="0" />
            <el-option label="进行中" :value="1" />
            <el-option label="已截止" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadHomeworkList">查询</el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 作业列表 -->
    <el-card class="list-card">
      <template #header>
        <div class="card-header">
          <span>我的作业</span>
          <span class="total-count">共 {{ pagination.total }} 项作业</span>
        </div>
      </template>

      <el-table :data="homeworkList" v-loading="loading" stripe>
        <el-table-column prop="title" label="作业标题" min-width="200" />
        <el-table-column prop="courseName" label="课程" width="150" />
        <el-table-column prop="teacherName" label="教师" width="100" />
        <el-table-column prop="endTime" label="截止时间" width="160">
          <template #default="{ row }">
            <span :class="{ 'text-danger': isOverdue(row.endTime) }">
              {{ formatTime(row.endTime) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.status)" size="small">
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="submitStatus" label="提交状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="getSubmitStatusType(row.submitStatus)" size="small">
              {{ getSubmitStatusText(row.submitStatus) }}
            </el-tag>
            <div v-if="row.score != null" class="score-text">得分: {{ row.score }}</div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewHomework(row)">查看作业</el-button>
            <el-button v-if="row.submitStatus === 0 && row.status === 1" type="success" link @click="openSubmitDialog(row)">
              提交
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="queryParams.pageNum"
          v-model:page-size="queryParams.pageSize"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next"
          @size-change="loadHomeworkList"
          @current-change="loadHomeworkList"
        />
      </div>
    </el-card>

    <!-- 作业详情对话框 -->
    <el-dialog v-model="detailVisible" title="作业详情" width="800px" destroy-on-close>
      <div v-if="homeworkDetail" class="homework-detail">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="作业标题" :span="2">{{ homeworkDetail.title }}</el-descriptions-item>
          <el-descriptions-item label="所属课程">{{ homeworkDetail.courseName }}</el-descriptions-item>
          <el-descriptions-item label="发布教师">{{ homeworkDetail.teacherName }}</el-descriptions-item>
          <el-descriptions-item label="总分">{{ homeworkDetail.totalScore }}分</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ formatTime(homeworkDetail.startTime) }}</el-descriptions-item>
          <el-descriptions-item label="截止时间">
            <span :class="{ 'text-danger': isOverdue(homeworkDetail.endTime) }">
              {{ formatTime(homeworkDetail.endTime) }}
            </span>
          </el-descriptions-item>
          <el-descriptions-item label="作业描述" :span="2">
            {{ homeworkDetail.description || '无' }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 作业附件 -->
        <div class="attachment-section">
          <h4>作业附件</h4>
          <div v-if="homeworkDetail.attachmentUrl" class="attachment-card">
            <el-icon :size="40" color="#409eff"><Document /></el-icon>
            <div class="attachment-info">
              <div class="attachment-name">{{ homeworkDetail.attachmentName || '作业文档' }}</div>
              <div class="attachment-actions">
                <el-button type="primary" @click="downloadFile(homeworkDetail.attachmentUrl, homeworkDetail.attachmentName)">
                  <el-icon><Download /></el-icon>
                  下载查看
                </el-button>
                <el-button v-if="isPreviewable(homeworkDetail.attachmentName)" @click="previewFile(homeworkDetail.attachmentUrl)">
                  <el-icon><View /></el-icon>
                  预览
                </el-button>
              </div>
            </div>
          </div>
          <el-empty v-else description="本作业未提供附件资料" :image-size="80" />
        </div>

        <!-- AI 思路点拨区块（锁定状态 - 提交前显示） -->
        <div v-if="!homeworkDetail.mySubmission" class="ai-analysis-section locked">
          <div class="ai-header">
            <div class="ai-title">
              <el-icon class="ai-icon"><MagicStick /></el-icon>
              <span>AI 思路点拨</span>
            </div>
            <el-tag type="info" size="small" effect="plain">
              <el-icon><Lock /></el-icon>
              提交后解锁
            </el-tag>
          </div>
          <div class="locked-content">
            <el-icon class="lock-icon" :size="48"><Lock /></el-icon>
            <p class="lock-text">提交作业后即可查看 AI 解析</p>
            <p class="lock-hint">AI 思路点拨由教师审核后发布，帮助你更好地理解作业要点</p>
          </div>
        </div>

        <!-- 我的提交 -->
        <div v-if="homeworkDetail.mySubmission" class="submission-section">
          <h4>我的提交</h4>
          <el-descriptions :column="2" border>
            <el-descriptions-item label="提交时间">{{ formatTime(homeworkDetail.mySubmission.submitTime) }}</el-descriptions-item>
            <el-descriptions-item label="状态">
              <el-tag :type="homeworkDetail.mySubmission.gradeStatus === 2 ? 'success' : 'warning'" size="small">
                {{ homeworkDetail.mySubmission.gradeStatus === 2 ? '已批改' : '待批改' }}
              </el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="得分" v-if="homeworkDetail.mySubmission.score != null">
              <span class="score-value">{{ homeworkDetail.mySubmission.score }}</span> 分
            </el-descriptions-item>
            <el-descriptions-item label="教师评语" v-if="homeworkDetail.mySubmission.comment">
              {{ homeworkDetail.mySubmission.comment }}
            </el-descriptions-item>
            <el-descriptions-item label="提交附件" v-if="homeworkDetail.mySubmission.attachmentUrl" :span="2">
              <a :href="getFileUrl(homeworkDetail.mySubmission.attachmentUrl)" target="_blank" class="file-link">
                {{ homeworkDetail.mySubmission.attachmentName || '查看附件' }}
              </a>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- AI 思路点拨区块 -->
        <transition name="expand">
          <div v-if="showAiAnalysis" class="ai-analysis-section">
            <div class="ai-header">
              <div class="ai-title">
                <el-icon class="ai-icon"><MagicStick /></el-icon>
                <span>AI 思路点拨</span>
              </div>
              <el-tag type="success" size="small" effect="dark">已解锁</el-tag>
            </div>
            <div class="ai-content">
              <div class="analysis-text" v-html="formatAnalysisText(homeworkDetail.analysis)"></div>
            </div>
            <div class="ai-footer">
              <el-icon><InfoFilled /></el-icon>
              <span>以上内容由 AI 生成，经教师审核后发布</span>
            </div>
          </div>
        </transition>
      </div>

      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button
          v-if="homeworkDetail && !homeworkDetail.mySubmission && homeworkDetail.status === 1"
          type="primary"
          @click="openSubmitDialog(homeworkDetail)"
        >
          提交作业
        </el-button>
      </template>
    </el-dialog>

    <!-- 提交作业对话框 -->
    <el-dialog v-model="submitVisible" title="提交作业" width="600px" destroy-on-close>
      <el-form :model="submitForm" label-width="100px">
        <el-form-item label="答案附件">
          <el-upload
            ref="uploadRef"
            :auto-upload="false"
            :limit="1"
            accept=".docx,.doc,.pdf,.txt,.zip,.rar"
            :on-change="handleFileChange"
            :on-exceed="handleExceed"
            :file-list="submitFileList"
          >
            <template #trigger>
              <el-button type="primary">选择文件</el-button>
            </template>
            <template #tip>
              <div class="el-upload__tip">支持 Word、PDF、TXT、ZIP 文件，最大 50MB</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="文字答案">
          <el-input
            v-model="submitForm.content"
            type="textarea"
            :rows="5"
            placeholder="如果不需要上传文件，可以在这里填写答案"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确认提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Document, Download, View, MagicStick, Lock, InfoFilled } from '@element-plus/icons-vue'
import { get, postFile } from '@/utils/request'

// 数据
const loading = ref(false)
const homeworkList = ref<any[]>([])
const queryParams = reactive({ status: undefined as number | undefined, pageNum: 1, pageSize: 10 })
const pagination = reactive({ total: 0 })

const detailVisible = ref(false)
const submitVisible = ref(false)
const submitting = ref(false)
const homeworkDetail = ref<any>(null)
const uploadRef = ref()
const submitFileList = ref<any[]>([])

// AI 解析展示控制
const showAiAnalysis = ref(false)
const justSubmitted = ref(false)

const submitForm = reactive({
  homeworkId: 0,
  file: null as File | null,
  content: ''
})

// 加载作业列表
const loadHomeworkList = async () => {
  loading.value = true
  try {
    const res = await get('/student/homework/list', queryParams)
    console.log('作业列表响应:', res)  // 调试日志

    if (res.code === 200 && res.data) {
      // 增加健壮性检查：确保 list 是数组
      const listData = res.data.list || res.data.records || res.data || []
      homeworkList.value = Array.isArray(listData) ? listData : []
      pagination.total = res.data.total || 0
    } else {
      console.error('接口返回异常:', res)
      homeworkList.value = []
    }
  } catch (e) {
    console.error('加载失败:', e)
    ElMessage.error('加载失败，请检查后端服务')
    homeworkList.value = []  // 确保出错时也是数组
  } finally {
    loading.value = false
  }
}

const resetQuery = () => {
  queryParams.status = undefined
  queryParams.pageNum = 1
  loadHomeworkList()
}

// 查看作业详情
const viewHomework = async (row: any) => {
  try {
    const res = await get(`/student/homework/${row.id}`)
    console.log('作业详情响应数据:', res.data)  // 调试日志
    if (res.code === 200 && res.data) {
      homeworkDetail.value = res.data
      console.log('作业附件 URL:', res.data.attachmentUrl)  // 调试日志
      console.log('作业附件名称:', res.data.attachmentName)  // 调试日志

      // 如果有解析内容且已提交，显示 AI 解析
      if (res.data.analysis && res.data.mySubmission) {
        showAiAnalysis.value = true
      } else {
        showAiAnalysis.value = false
      }

      justSubmitted.value = false
      detailVisible.value = true
    } else {
      ElMessage.error(res.message || '获取详情失败')
    }
  } catch (e) {
    console.error('获取详情失败:', e)
    ElMessage.error('获取详情失败')
  }
}

// 打开提交对话框
const openSubmitDialog = (row: any) => {
  submitForm.homeworkId = row.id
  submitForm.file = null
  submitForm.content = ''
  submitFileList.value = []
  submitVisible.value = true
}

const handleFileChange = (file: any) => {
  submitForm.file = file.raw
}

const handleExceed = () => {
  ElMessage.warning('只能上传一个文件')
}

// 提交作业
const handleSubmit = async () => {
  if (!submitForm.file && !submitForm.content?.trim()) {
    ElMessage.warning('请上传附件或填写文字答案')
    return
  }

  submitting.value = true
  try {
    const userStoreJson = localStorage.getItem('smartedu_user')
    let token = ''
    if (userStoreJson) {
      try {
        token = JSON.parse(userStoreJson).token || ''
      } catch (e) {}
    }

    const formData = new FormData()
    formData.append('homeworkId', String(submitForm.homeworkId))
    if (submitForm.file) {
      formData.append('file', submitForm.file)
    }
    if (submitForm.content) {
      formData.append('content', submitForm.content)
    }

    const res = await postFile('/student/homework/submit', formData)

    if (res.code === 200) {
      ElMessage.success('🎉 提交成功！正在加载 AI 思路点拨...')
      submitVisible.value = false

      // 重新获取作业详情（包含解析内容）
      const detailRes = await get(`/student/homework/${submitForm.homeworkId}`)
      if (detailRes.code === 200 && detailRes.data) {
        homeworkDetail.value = detailRes.data
        justSubmitted.value = true

        // 如果有解析内容，平滑展开 AI 解析区块
        if (detailRes.data.analysis) {
          setTimeout(() => {
            showAiAnalysis.value = true
          }, 300)
        }
      }

      loadHomeworkList()
    } else {
      ElMessage.error(res.message || '提交失败')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

// 工具函数
const formatTime = (time: string) => {
  if (!time) return '-'
  return new Date(time).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

const isOverdue = (endTime: string) => {
  if (!endTime) return false
  return new Date(endTime) < new Date()
}

const getStatusType = (status: number) => ({ 0: 'info', 1: 'primary', 2: 'danger' }[status] || 'info')
const getStatusText = (status: number) => ({ 0: '未开始', 1: '进行中', 2: '已截止' }[status] || '未知')
const getSubmitStatusType = (status: number) => ({ 0: 'info', 1: 'warning', 2: 'success' }[status] || 'info')
const getSubmitStatusText = (status: number) => ({ 0: '未提交', 1: '待批改', 2: '已批改' }[status] || '未知')

const getFileUrl = (url: string) => {
  if (!url) return '#'
  if (url.startsWith('http')) return url
  // 如果已经是完整路径（以 /api 开头），直接返回
  if (url.startsWith('/api')) return url
  // 否则拼接 /api 前缀
  return '/api' + url
}

const downloadFile = (url: string, name: string) => {
  const link = document.createElement('a')
  link.href = getFileUrl(url)
  link.download = name || '作业文档'
  link.target = '_blank'
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
}

const isPreviewable = (name: string) => {
  if (!name) return false
  const ext = name.split('.').pop()?.toLowerCase()
  return ['pdf', 'txt', 'doc', 'docx'].includes(ext || '')
}

const previewFile = (url: string) => {
  window.open(getFileUrl(url), '_blank')
}

// 格式化解析文本（支持简单的 Markdown）
const formatAnalysisText = (text: string) => {
  if (!text) return ''
  // 转义 HTML
  let formatted = text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
  // 换行转 br
  formatted = formatted.replace(/\n/g, '<br>')
  // 粗体 **text**
  formatted = formatted.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
  // 标题 ### text
  formatted = formatted.replace(/### (.+?)(<br>|$)/g, '<h4>$1</h4>')
  formatted = formatted.replace(/## (.+?)(<br>|$)/g, '<h3>$1</h3>')
  formatted = formatted.replace(/# (.+?)(<br>|$)/g, '<h2>$1</h2>')
  // 代码块
  formatted = formatted.replace(/`(.+?)`/g, '<code>$1</code>')
  return formatted
}

onMounted(() => {
  loadHomeworkList()
})
</script>

<style scoped lang="scss">
.homework-container {
  .filter-card { margin-bottom: 16px; }
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    .total-count { font-size: 14px; color: #909399; }
  }
  .text-danger { color: #f56c6c; }
  .score-text { font-size: 12px; color: #67c23a; margin-top: 4px; }
  .pagination-wrapper { margin-top: 16px; display: flex; justify-content: flex-end; }
  .file-link { color: #409eff; text-decoration: none; &:hover { text-decoration: underline; } }

  .homework-detail {
    .attachment-section, .submission-section {
      margin-top: 24px;
      h4 { margin-bottom: 12px; padding-bottom: 8px; border-bottom: 1px solid #ebeef5; }
    }
    .attachment-card {
      display: flex;
      align-items: center;
      gap: 20px;
      padding: 20px;
      background: #f5f7fa;
      border-radius: 8px;
      .attachment-info {
        flex: 1;
        .attachment-name { font-size: 16px; font-weight: 500; margin-bottom: 10px; }
        .attachment-actions { display: flex; gap: 10px; }
      }
    }
    .score-value { font-size: 20px; font-weight: bold; color: #67c23a; }
  }
}

// AI 思路点拨区块样式
.ai-analysis-section {
  margin-top: 24px;
  border-radius: 16px;
  overflow: hidden;
  border: 2px solid transparent;
  background: linear-gradient(white, white) padding-box,
              linear-gradient(135deg, #667eea 0%, #764ba2 100%) border-box;
  box-shadow: 0 4px 20px rgba(102, 126, 234, 0.15);

  .ai-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 16px 20px;
    background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
    color: white;

    .ai-title {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 16px;
      font-weight: 600;

      .ai-icon {
        font-size: 20px;
        animation: pulse 2s infinite;
      }
    }
  }

  .ai-content {
    padding: 24px;
    background: white;

    .analysis-text {
      line-height: 1.8;
      color: #303133;
      font-size: 15px;

      :deep(h2) {
        font-size: 18px;
        font-weight: 600;
        margin: 16px 0 8px;
        color: #303133;
      }

      :deep(h3) {
        font-size: 16px;
        font-weight: 600;
        margin: 14px 0 6px;
        color: #303133;
      }

      :deep(h4) {
        font-size: 15px;
        font-weight: 500;
        margin: 12px 0 4px;
        color: #606266;
      }

      :deep(code) {
        background: #f5f7fa;
        padding: 2px 8px;
        border-radius: 4px;
        font-family: 'Consolas', monospace;
        color: #e6a23c;
        font-size: 14px;
      }

      :deep(strong) {
        color: #409eff;
        font-weight: 600;
      }
    }
  }

  .ai-footer {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 12px 20px;
    background: #f5f7fa;
    color: #909399;
    font-size: 13px;
  }

  // 锁定状态样式
  &.locked {
    border: 1px dashed #dcdfe6;
    background: #fafafa;
    box-shadow: none;

    .ai-header {
      background: #e4e7ed;
      color: #909399;

      .ai-title {
        .ai-icon {
          animation: none;
          opacity: 0.5;
        }
      }
    }

    .locked-content {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 40px 20px;
      text-align: center;

      .lock-icon {
        color: #c0c4cc;
        margin-bottom: 16px;
      }

      .lock-text {
        font-size: 16px;
        color: #606266;
        margin: 0 0 8px;
        font-weight: 500;
      }

      .lock-hint {
        font-size: 13px;
        color: #909399;
        margin: 0;
      }
    }
  }
}

// 展开/收起动画
.expand-enter-active {
  animation: expand 0.5s ease-out;
}

.expand-leave-active {
  animation: expand 0.3s ease-in reverse;
}

@keyframes expand {
  0% {
    opacity: 0;
    max-height: 0;
    transform: translateY(-10px);
  }
  100% {
    opacity: 1;
    max-height: 1000px;
    transform: translateY(0);
  }
}

@keyframes pulse {
  0%, 100% {
    opacity: 1;
    transform: scale(1);
  }
  50% {
    opacity: 0.8;
    transform: scale(1.1);
  }
}
</style>