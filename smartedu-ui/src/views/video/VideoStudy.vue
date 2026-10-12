<template>
  <div class="video-study-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <el-icon class="header-icon"><VideoPlay /></el-icon>
        <h2>视频学习</h2>
        <span class="subtitle">精选优质学习资源</span>
      </div>
      <div class="header-right">
        <el-button type="primary" @click="showSubmitDialog" v-if="userStore.isLoggedIn">
          <el-icon><Upload /></el-icon>
          投稿视频
        </el-button>
      </div>
    </div>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-input
        v-model="searchKeyword"
        placeholder="搜索视频标题或简介..."
        clearable
        @input="handleSearch"
        class="search-input"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
    </div>

    <!-- 视频列表 -->
    <div class="video-grid" v-loading="loading">
      <el-row :gutter="20">
        <el-col :xs="12" :sm="8" :md="6" v-for="video in videoList" :key="video.id">
          <el-card class="video-card" shadow="hover" @click="openVideo(video)">
            <div class="video-cover">
              <el-image
                :src="getCoverUrl(video.coverUrl)"
                fit="cover"
                class="cover-image"
              >
                <template #error>
                  <div class="cover-placeholder">
                    <el-icon><VideoPlay /></el-icon>
                  </div>
                </template>
              </el-image>
              <div class="play-overlay">
                <el-icon class="play-icon"><VideoPlay /></el-icon>
              </div>
              <div class="video-stats">
                <span><el-icon><View /></el-icon> {{ formatCount(video.viewCount) }}</span>
              </div>
            </div>
            <div class="video-info">
              <div class="video-title">{{ video.title }}</div>
              <div class="video-meta">
                <span class="collection-btn" @click.stop="handleCollection(video)">
                  <el-icon :class="{ collected: video.collected }">
                    <Star v-if="video.collected" />
                    <StarFilled v-else />
                  </el-icon>
                  {{ video.collectionCount || 0 }}
                </span>
                <span class="delete-btn" v-if="isAdmin" @click.stop="handleDelete(video)">
                  <el-icon><Delete /></el-icon>
                  删除
                </span>
              </div>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-empty v-if="!loading && videoList.length === 0" description="暂无视频" />
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > pageSize">
      <el-pagination
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="total"
        layout="prev, pager, next"
        @current-change="loadVideos"
      />
    </div>

    <!-- 投稿弹窗 -->
    <el-dialog
      v-model="submitDialogVisible"
      title="投稿视频"
      width="550px"
      destroy-on-close
    >
      <el-form :model="submitForm" :rules="submitRules" ref="submitFormRef" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="submitForm.title" placeholder="请输入视频标题" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="封面" prop="coverUrl">
          <div class="cover-upload-wrapper">
            <el-upload
              class="cover-uploader"
              :action="uploadUrl"
              :headers="uploadHeaders"
              :show-file-list="false"
              :on-success="handleUploadSuccess"
              :on-error="handleUploadError"
              :before-upload="beforeUpload"
              accept=".jpg,.jpeg,.png"
            >
              <div v-if="submitForm.coverUrl" class="cover-preview">
                <el-image :src="getPreviewUrl(submitForm.coverUrl)" fit="cover" class="cover-image" />
                <div class="cover-actions">
                  <el-icon @click.stop="removeCover"><Delete /></el-icon>
                </div>
              </div>
              <div v-else class="cover-placeholder">
                <el-icon class="upload-icon"><Plus /></el-icon>
                <div class="upload-text">上传封面</div>
                <div class="upload-hint">JPG/PNG，最大5MB</div>
              </div>
            </el-upload>
          </div>
        </el-form-item>
        <el-form-item label="B站链接" prop="videoUrl">
          <el-input v-model="submitForm.videoUrl" placeholder="请输入B站视频完整链接">
            <template #prepend>https://</template>
          </el-input>
        </el-form-item>
        <el-form-item label="简介">
          <el-input
            v-model="submitForm.description"
            type="textarea"
            :rows="3"
            placeholder="请输入视频简介（选填）"
            maxlength="200"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="submitDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue'
import { useUserStore } from '@/store/modules/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getVideoList, submitVideo, toggleCollection, deleteVideo } from '@/api/video'
import { VideoPlay, Upload, Search, View, Star, StarFilled, Plus, Delete } from '@element-plus/icons-vue'
import type { FormInstance, FormRules, UploadProps } from 'element-plus'

const userStore = useUserStore()

// 检查用户是否是管理员
const isAdmin = computed(() => {
  return userStore.isLoggedIn && userStore.userInfo?.role === 'ADMIN'
})

// 上传配置
const uploadUrl = '/api/common/upload/cover'
const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${userStore.token}`
}))

interface VideoItem {
  id: number
  title: string
  coverUrl: string
  videoUrl: string
  description: string
  viewCount: number
  collectionCount: number
  collected: boolean
}

const loading = ref(false)
const videoList = ref<VideoItem[]>([])
const currentPage = ref(1)
const pageSize = ref(12)
const total = ref(0)
const searchKeyword = ref('')
let searchTimer: ReturnType<typeof setTimeout> | null = null

// 投稿表单
const submitDialogVisible = ref(false)
const submitting = ref(false)
const submitFormRef = ref<FormInstance>()
const submitForm = reactive({
  title: '',
  coverUrl: '',
  videoUrl: '',
  description: ''
})
const submitRules: FormRules = {
  title: [{ required: true, message: '请输入视频标题', trigger: 'blur' }],
  videoUrl: [{ required: true, message: '请输入B站视频链接', trigger: 'blur' }]
}

// 上传前验证
const beforeUpload: UploadProps['beforeUpload'] = (file) => {
  const isImage = ['image/jpeg', 'image/png', 'image/jpg'].includes(file.type)
  if (!isImage) {
    ElMessage.error('封面仅支持 JPG 和 PNG 格式')
    return false
  }
  const isLt5M = file.size / 1024 / 1024 < 5
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
    return false
  }
  return true
}

// 上传成功
const handleUploadSuccess = (response: any) => {
  if (response.code === 200 && response.data) {
    submitForm.coverUrl = response.data.url
    ElMessage.success('封面上传成功')
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

// 上传失败
const handleUploadError = () => {
  ElMessage.error('上传失败，请重试')
}

// 移除封面
const removeCover = () => {
  submitForm.coverUrl = ''
}

// 加载视频列表
const loadVideos = async () => {
  loading.value = true
  try {
    const res = await getVideoList({
      page: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined
    })
    if (res.code === 200 && res.data) {
      videoList.value = res.data.records || []
      total.value = res.data.total || 0
    } else {
      videoList.value = []
      total.value = 0
    }
  } catch (error) {
    console.error('加载视频失败:', error)
    videoList.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

// 防抖搜索
const handleSearch = () => {
  if (searchTimer) clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    currentPage.value = 1
    loadVideos()
  }, 300)
}

// 打开视频
const openVideo = (video: VideoItem) => {
  if (video.videoUrl) {
    window.open(video.videoUrl, '_blank', 'noopener,noreferrer')
  }
}

// 获取封面完整 URL
const getCoverUrl = (url: string): string => {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }
  return '/api' + url
}

// 获取预览 URL（用于投稿弹窗预览）
const getPreviewUrl = (url: string): string => {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }
  // 本地路径需要加 /api 前缀
  return '/api' + url
}

// 格式化数字
const formatCount = (count: number): string => {
  if (!count) return '0'
  if (count >= 10000) return (count / 10000).toFixed(1) + 'w'
  if (count >= 1000) return (count / 1000).toFixed(1) + 'k'
  return count.toString()
}

// 收藏
const handleCollection = async (video: VideoItem) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const res = await toggleCollection(video.id)
    if (res.code === 200) {
      video.collected = res.data
      video.collectionCount = res.data ? video.collectionCount + 1 : Math.max(0, video.collectionCount - 1)
      ElMessage.success(res.message)
    }
  } catch (error) {
    console.error('收藏失败:', error)
  }
}

// 显示投稿弹窗
const showSubmitDialog = () => {
  submitForm.title = ''
  submitForm.coverUrl = ''
  submitForm.videoUrl = ''
  submitForm.description = ''
  submitDialogVisible.value = true
}

// 提交投稿
const handleSubmit = async () => {
  if (!submitFormRef.value) return
  await submitFormRef.value.validate(async (valid) => {
    if (!valid) return
    submitting.value = true
    try {
      let videoUrl = submitForm.videoUrl
      if (!videoUrl.startsWith('http')) {
        videoUrl = 'https://' + videoUrl
      }
      const res = await submitVideo({
        title: submitForm.title,
        coverUrl: submitForm.coverUrl,
        videoUrl: videoUrl,
        description: submitForm.description
      })
      if (res.code === 200) {
        ElMessage.success('投稿成功，等待审核')
        submitDialogVisible.value = false
      } else {
        ElMessage.error(res.message || '投稿失败')
      }
    } catch (error) {
      ElMessage.error('投稿失败')
    } finally {
      submitting.value = false
    }
  })
}

// 删除视频
const handleDelete = async (video: VideoItem) => {
  try {
    await ElMessageBox.confirm(`确定删除视频"${video.title}"吗？此操作不可恢复。`, '确认删除', {
      type: 'warning'
    })
    
    console.log('删除视频ID:', video.id)
    console.log('删除前视频列表长度:', videoList.value.length)
    
    const res = await deleteVideo(video.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      
      // 手动从视频列表中移除被删除的视频
      videoList.value = videoList.value.filter(item => item.id !== video.id)
      console.log('删除后视频列表长度:', videoList.value.length)
      
      // 刷新视频列表以确保数据一致性
      setTimeout(() => {
        loadVideos()
      }, 100)
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (error) {
    console.error('删除视频失败:', error)
    // 用户取消
  }
}

onMounted(() => {
  loadVideos()
})
</script>

<style scoped lang="scss">
.video-study-container {
  padding: 20px;
  max-width: 1400px;
  margin: 0 auto;
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
      font-size: 24px;
      font-weight: 700;
      color: #303133;
      margin: 0;
    }

    .subtitle {
      font-size: 14px;
      color: #909399;
    }

    .header-icon {
      font-size: 28px;
      color: #409eff;
    }
  }
}

.search-bar {
  margin-bottom: 24px;

  .search-input {
    max-width: 400px;
  }
}

.video-grid {
  min-height: 400px;

  .video-card {
    cursor: pointer;
    transition: all 0.3s ease;
    border-radius: 12px;
    overflow: hidden;
    margin-bottom: 20px;

    &:hover {
      transform: translateY(-4px);
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);

      .play-overlay {
        opacity: 1;
      }
    }

    :deep(.el-card__body) {
      padding: 0;
    }

    .video-cover {
      position: relative;
      aspect-ratio: 16 / 9;
      overflow: hidden;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

      .cover-image {
        width: 100%;
        height: 100%;
      }

      .cover-placeholder {
        width: 100%;
        height: 100%;
        display: flex;
        align-items: center;
        justify-content: center;
        color: #fff;

        .el-icon {
          font-size: 48px;
          opacity: 0.5;
        }
      }

      .play-overlay {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.4);
        display: flex;
        align-items: center;
        justify-content: center;
        opacity: 0;
        transition: opacity 0.3s ease;

        .play-icon {
          font-size: 48px;
          color: #fff;
        }
      }

      .video-stats {
        position: absolute;
        bottom: 8px;
        right: 8px;
        background: rgba(0, 0, 0, 0.6);
        padding: 2px 8px;
        border-radius: 4px;
        font-size: 12px;
        color: #fff;

        span {
          display: flex;
          align-items: center;
          gap: 4px;
        }
      }
    }

    .video-info {
      padding: 12px;

      .video-title {
        font-size: 14px;
        font-weight: 500;
        color: #303133;
        line-height: 1.5;
        display: -webkit-box;
        -webkit-line-clamp: 2;
        -webkit-box-orient: vertical;
        overflow: hidden;
        margin-bottom: 8px;
      }

      .video-meta {
        display: flex;
        justify-content: space-between;
        align-items: center;

        .collection-btn {
          display: flex;
          align-items: center;
          gap: 4px;
          font-size: 12px;
          color: #909399;
          cursor: pointer;
          transition: color 0.2s;

          &:hover {
            color: #f56c6c;
          }

          .collected {
            color: #f56c6c;
          }
        }

        .delete-btn {
          display: flex;
          align-items: center;
          gap: 4px;
          font-size: 12px;
          color: #909399;
          cursor: pointer;
          transition: color 0.2s;

          &:hover {
            color: #f56c6c;
          }
        }
      }
    }
  }
}

.pagination-wrapper {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

// 封面上传样式
.cover-upload-wrapper {
  width: 100%;

  .cover-uploader {
    width: 100%;

    :deep(.el-upload) {
      width: 100%;
      border: 1px dashed #d9d9d9;
      border-radius: 8px;
      cursor: pointer;
      position: relative;
      overflow: hidden;
      transition: border-color 0.3s;

      &:hover {
        border-color: #409eff;
      }
    }
  }

  .cover-preview {
    width: 100%;
    height: 180px;
    position: relative;

    .cover-image {
      width: 100%;
      height: 100%;
      border-radius: 8px;
    }

    .cover-actions {
      position: absolute;
      top: 8px;
      right: 8px;
      width: 28px;
      height: 28px;
      background: rgba(0, 0, 0, 0.5);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #fff;
      cursor: pointer;
      transition: background 0.3s;

      &:hover {
        background: rgba(0, 0, 0, 0.7);
      }
    }
  }

  .cover-placeholder {
    width: 100%;
    height: 180px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    background: #f5f7fa;
    border-radius: 8px;

    .upload-icon {
      font-size: 36px;
      color: #909399;
      margin-bottom: 8px;
    }

    .upload-text {
      font-size: 14px;
      color: #606266;
      margin-bottom: 4px;
    }

    .upload-hint {
      font-size: 12px;
      color: #909399;
    }
  }
}
</style>