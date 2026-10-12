<template>
  <div class="video-audit-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <el-icon class="header-icon"><DocumentChecked /></el-icon>
        <h2>视频管理</h2>
        <el-tag type="warning" v-if="activeTab === 'pending' && pendingCount > 0">{{ pendingCount }} 待审核</el-tag>
      </div>
      <el-button @click="handleRefresh" :loading="loading">
        <el-icon><Refresh /></el-icon>
        刷新
      </el-button>
    </div>

    <!-- 标签页切换 -->
    <el-tabs v-model="activeTab" @tab-change="handleTabChange" class="video-tabs">
      <el-tab-pane label="待审核" name="pending">
        <template #label>
          <span>待审核 <el-tag type="warning" size="small" v-if="pendingCount > 0">{{ pendingCount }}</el-tag></span>
        </template>
      </el-tab-pane>
      <el-tab-pane label="已通过" name="approved"></el-tab-pane>
    </el-tabs>

    <el-table :data="videoList" v-loading="loading" stripe style="width: 100%">
      <el-table-column label="封面" width="140">
        <template #default="{ row }">
          <el-image
            :src="getCoverUrl(row.coverUrl)"
            fit="cover"
            class="table-cover"
          >
            <template #error>
              <div class="cover-error">
                <el-icon><VideoPlay /></el-icon>
              </div>
            </template>
          </el-image>
        </template>
      </el-table-column>
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column label="链接" width="200">
        <template #default="{ row }">
          <el-link :href="row.videoUrl" target="_blank" type="primary" :underline="false">
            查看视频 <el-icon><Link /></el-icon>
          </el-link>
        </template>
      </el-table-column>
      <el-table-column prop="description" label="简介" min-width="150" show-overflow-tooltip />
      <el-table-column label="投稿时间" width="160">
        <template #default="{ row }">
          {{ row.createdAt }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <template v-if="activeTab === 'pending'">
            <el-button type="success" size="small" @click="handleApprove(row)">
              <el-icon><Select /></el-icon>
              通过
            </el-button>
            <el-button type="danger" size="small" @click="showRejectDialog(row)">
              <el-icon><CloseBold /></el-icon>
              拒绝
            </el-button>
            <el-button type="warning" size="small" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
          <template v-else>
            <el-button type="danger" size="small" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon>
              删除
            </el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadVideos"
      />
    </div>

    <!-- 空状态 -->
    <el-empty v-if="!loading && videoList.length === 0" :description="activeTab === 'pending' ? '暂无待审核视频' : '暂无已通过视频'" />

    <!-- 拒绝理由弹窗 -->
    <el-dialog
      v-model="rejectDialogVisible"
      title="拒绝理由"
      width="400px"
    >
      <el-input
        v-model="rejectReason"
        type="textarea"
        :rows="4"
        placeholder="请输入拒绝理由..."
        maxlength="200"
        show-word-limit
      />
      <template #footer>
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="danger" @click="handleReject" :loading="rejecting">确认拒绝</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPendingVideos, getApprovedVideos, auditVideo, deleteVideo } from '@/api/video'
import { DocumentChecked, Refresh, VideoPlay, Link, Select, CloseBold, Delete } from '@element-plus/icons-vue'

// 当前标签页：pending 待审核, approved 已通过
const activeTab = ref('pending')

interface VideoItem {
  id: number
  title: string
  coverUrl: string
  videoUrl: string
  description: string
  createdAt: string
}

const loading = ref(false)
const videoList = ref<VideoItem[]>([])
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const pendingCount = ref(0)

// 拒绝弹窗
const rejectDialogVisible = ref(false)
const rejecting = ref(false)
const rejectReason = ref('')
const currentVideo = ref<VideoItem | null>(null)

// 加载待审核视频
const loadPendingVideos = async () => {
  loading.value = true
  try {
    const res = await getPendingVideos({
      page: currentPage.value,
      size: pageSize.value
    })
    if (res.code === 200 && res.data) {
      videoList.value = res.data.records || []
      total.value = res.data.total || 0
      pendingCount.value = total.value
    } else {
      videoList.value = []
      total.value = 0
      pendingCount.value = 0
    }
  } catch (error) {
    console.error('加载失败:', error)
    videoList.value = []
    total.value = 0
    pendingCount.value = 0
  } finally {
    loading.value = false
  }
}

// 加载已审核通过的视频
const loadApprovedVideos = async () => {
  loading.value = true
  try {
    const res = await getApprovedVideos({
      page: currentPage.value,
      size: pageSize.value
    })
    if (res.code === 200 && res.data) {
      videoList.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      videoList.value = []
      total.value = 0
    }
  } catch (error) {
    console.error('加载失败:', error)
    videoList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 根据当前标签页加载对应数据
const loadVideos = () => {
  if (activeTab.value === 'pending') {
    loadPendingVideos()
  } else {
    loadApprovedVideos()
  }
}

// 刷新按钮
const handleRefresh = () => {
  currentPage.value = 1
  loadVideos()
}

// 标签页切换
const handleTabChange = () => {
  currentPage.value = 1
  loadVideos()
}

// 获取封面完整 URL
const getCoverUrl = (url: string): string => {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }
  // 相对路径添加 API 前缀
  return '/api' + url
}

// 通过审核
const handleApprove = async (video: VideoItem) => {
  try {
    await ElMessageBox.confirm(`确定通过视频"${video.title}"的审核吗？`, '确认通过', {
      type: 'success'
    })
    const res = await auditVideo({
      videoId: video.id,
      status: 1
    })
    if (res.code === 200) {
      ElMessage.success('审核通过')
      loadVideos()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    // 用户取消
  }
}

// 显示拒绝弹窗
const showRejectDialog = (video: VideoItem) => {
  currentVideo.value = video
  rejectReason.value = ''
  rejectDialogVisible.value = true
}

// 确认拒绝
const handleReject = async () => {
  if (!rejectReason.value.trim()) {
    ElMessage.warning('请输入拒绝理由')
    return
  }
  if (!currentVideo.value) return

  rejecting.value = true
  try {
    const res = await auditVideo({
      videoId: currentVideo.value.id,
      status: 2,
      rejectReason: rejectReason.value
    })
    if (res.code === 200) {
      ElMessage.success('已拒绝')
      rejectDialogVisible.value = false
      loadVideos()
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (error) {
    ElMessage.error('操作失败')
  } finally {
    rejecting.value = false
  }
}

// 删除视频
const handleDelete = async (video: VideoItem) => {
  try {
    await ElMessageBox.confirm(`确定删除视频"${video.title}"吗？此操作不可恢复。`, '确认删除', {
      type: 'warning'
    })

    const res = await deleteVideo(video.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      // 删除成功后重新加载当前列表
      loadVideos()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    // 用户取消
  }
}

onMounted(() => {
  loadVideos()
})
</script>

<style scoped lang="scss">
.video-audit-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;

  .header-left {
    display: flex;
    align-items: center;
    gap: 12px;

    h2 {
      font-size: 20px;
      font-weight: 600;
      color: #303133;
      margin: 0;
    }

    .header-icon {
      font-size: 24px;
      color: #e6a23c;
    }
  }
}

.table-cover {
  width: 120px;
  height: 68px;
  border-radius: 4px;
  overflow: hidden;

  .cover-error {
    width: 100%;
    height: 100%;
    background: #f5f7fa;
    display: flex;
    align-items: center;
    justify-content: center;
    color: #c0c4cc;
  }
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.video-tabs {
  margin-bottom: 20px;
}
</style>