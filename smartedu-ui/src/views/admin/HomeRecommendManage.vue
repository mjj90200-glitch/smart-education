<template>
  <div class="recommend-manage-container">
    <!-- 页面标题 -->
    <div class="page-header">
      <div class="header-left">
        <el-icon class="header-icon"><SetUp /></el-icon>
        <h2>首页推荐管理</h2>
        <el-tag type="warning">配置首页视频展示</el-tag>
      </div>
      <el-button type="primary" @click="openAddDialog">
        <el-icon><Plus /></el-icon>
        添加推荐
      </el-button>
    </div>

    <!-- 推荐配置区域 -->
    <el-row :gutter="20" class="recommend-section">
      <!-- 轮播推荐 -->
      <el-col :span="12">
        <el-card class="position-card">
          <template #header>
            <div class="card-header">
              <div class="header-title">
                <el-icon class="carousel-icon"><VideoPlay /></el-icon>
                <span>轮播推荐</span>
                <el-tag :type="carouselCount >= 1 ? 'danger' : 'info'" size="small">
                  {{ carouselCount }}/1
                </el-tag>
              </div>
              <el-button
                v-if="carouselCount < 1"
                type="danger"
                size="small"
                @click="openAddDialog(1)"
              >
                + 添加
              </el-button>
            </div>
          </template>
          <el-table :data="carouselList" v-loading="carouselLoading" stripe empty-text="暂无轮播推荐">
            <el-table-column label="封面" width="100">
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
            <el-table-column prop="title" label="标题" show-overflow-tooltip />
            <el-table-column label="播放量" width="100">
              <template #default="{ row }">
                {{ formatCount(row.viewCount) }}
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" fixed="right">
              <template #default="{ row }">
                <el-button type="danger" size="small" link @click="handleRemove(row)">
                  移除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <!-- 网格推荐 -->
      <el-col :span="12">
        <el-card class="position-card">
          <template #header>
            <div class="card-header">
              <div class="header-title">
                <el-icon class="grid-icon"><Grid /></el-icon>
                <span>网格推荐</span>
                <el-tag :type="gridCount >= 4 ? 'danger' : 'success'" size="small">
                  {{ gridCount }}/4
                </el-tag>
              </div>
              <el-button
                v-if="gridCount < 4"
                type="success"
                size="small"
                @click="openAddDialog(2)"
              >
                + 添加
              </el-button>
            </div>
          </template>
          <el-table :data="gridList" v-loading="gridLoading" stripe empty-text="暂无网格推荐">
            <el-table-column label="封面" width="80">
              <template #default="{ row }">
                <el-image
                  :src="getCoverUrl(row.coverUrl)"
                  fit="cover"
                  class="table-cover small"
                >
                  <template #error>
                    <div class="cover-error">
                      <el-icon><VideoPlay /></el-icon>
                    </div>
                  </template>
                </el-image>
              </template>
            </el-table-column>
            <el-table-column prop="title" label="标题" show-overflow-tooltip />
            <el-table-column label="播放量" width="80">
              <template #default="{ row }">
                {{ formatCount(row.viewCount) }}
              </template>
            </el-table-column>
            <el-table-column label="排序" width="140" fixed="right">
              <template #default="{ row, $index }">
                <el-button
                  type="primary"
                  size="small"
                  link
                  :disabled="$index === 0"
                  @click="handleMoveUp(row)"
                >
                  上移
                </el-button>
                <el-button
                  type="primary"
                  size="small"
                  link
                  :disabled="$index === gridList.length - 1"
                  @click="handleMoveDown(row)"
                >
                  下移
                </el-button>
                <el-button type="danger" size="small" link @click="handleRemove(row)">
                  移除
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 添加推荐弹窗 -->
    <el-dialog
      v-model="addDialogVisible"
      title="添加推荐视频"
      width="800px"
      destroy-on-close
    >
      <!-- 搜索栏 -->
      <div class="dialog-search">
        <el-input
          v-model="dialogSearchKeyword"
          placeholder="搜索视频标题..."
          clearable
          class="search-input"
          @input="handleDialogSearchDebounced"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
        <span class="search-hint">
          仅显示已通过审核且未在推荐位的视频
        </span>
      </div>

      <!-- 视频列表 -->
      <el-table
        :data="availableVideos"
        v-loading="dialogLoading"
        stripe
        max-height="400"
        empty-text="暂无可添加的视频"
      >
        <el-table-column label="封面" width="120">
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
        <el-table-column label="播放量" width="100">
          <template #default="{ row }">
            {{ formatCount(row.viewCount) }}
          </template>
        </el-table-column>
        <el-table-column label="收藏数" width="100">
          <template #default="{ row }">
            {{ formatCount(row.collectionCount) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button
              type="danger"
              size="small"
              :disabled="carouselCount >= 1"
              @click="handleSetAsCarousel(row)"
            >
              设为轮播
            </el-button>
            <el-button
              type="success"
              size="small"
              :disabled="gridCount >= 4"
              @click="handleSetAsGrid(row)"
            >
              设为网格
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="dialog-pagination" v-if="dialogTotal > dialogPageSize">
        <el-pagination
          v-model:current-page="dialogPage"
          :page-size="dialogPageSize"
          :total="dialogTotal"
          layout="prev, pager, next"
          @current-change="loadAvailableVideos"
        />
      </div>

      <template #footer>
        <el-button @click="addDialogVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getRecommendByPosition,
  searchAvailableVideos,
  addRecommend,
  removeRecommend,
  moveUpRecommend,
  moveDownRecommend,
  getUnrecommendedVideos
} from '@/api/homeRecommend'
import { SetUp, Search, VideoPlay, Grid, Plus } from '@element-plus/icons-vue'

interface VideoItem {
  id: number
  videoPostId: number
  title: string
  coverUrl: string
  videoUrl: string
  viewCount: number
  collectionCount: number
  positionType: number
  sortOrder: number
}

// 轮播推荐
const carouselList = ref<VideoItem[]>([])
const carouselLoading = ref(false)
const carouselCount = computed(() => carouselList.value.length)

// 网格推荐
const gridList = ref<VideoItem[]>([])
const gridLoading = ref(false)
const gridCount = computed(() => gridList.value.length)

// 弹窗
const addDialogVisible = ref(false)
const dialogSearchKeyword = ref('')
const availableVideos = ref<VideoItem[]>([])
const dialogLoading = ref(false)
const dialogPage = ref(1)
const dialogPageSize = ref(10)
const dialogTotal = ref(0)
let dialogSearchTimer: ReturnType<typeof setTimeout> | null = null

// 打开添加弹窗
const openAddDialog = (positionType?: number) => {
  dialogSearchKeyword.value = ''
  dialogPage.value = 1
  addDialogVisible.value = true
  loadAvailableVideos()
}

// 防抖搜索
const handleDialogSearchDebounced = () => {
  if (dialogSearchTimer) clearTimeout(dialogSearchTimer)
  dialogSearchTimer = setTimeout(() => {
    dialogPage.value = 1
    loadAvailableVideos()
  }, 300)
}

// 加载可添加的视频（未推荐的已审核视频）
const loadAvailableVideos = async () => {
  dialogLoading.value = true
  try {
    const res = await getUnrecommendedVideos({
      keyword: dialogSearchKeyword.value || undefined,
      page: dialogPage.value,
      size: dialogPageSize.value
    })
    if (res.code === 200) {
      availableVideos.value = res.data?.records || res.data || []
      dialogTotal.value = res.data?.total || availableVideos.value.length
    }
  } catch (error) {
    console.error('加载失败:', error)
    availableVideos.value = []
  } finally {
    dialogLoading.value = false
  }
}

// 设为轮播
const handleSetAsCarousel = async (video: VideoItem) => {
  if (carouselCount.value >= 1) {
    ElMessage.warning('轮播位置已满，请先移除现有推荐')
    return
  }
  try {
    await ElMessageBox.confirm(`确定将"${video.title}"设为轮播推荐吗？`, '确认添加')
    const res = await addRecommend({ videoPostId: video.id, positionType: 1 })
    if (res.code === 200) {
      ElMessage.success('添加成功')
      loadCarouselList()
      loadAvailableVideos()
    } else {
      ElMessage.error(res.message || '添加失败')
    }
  } catch (error) {
    // 用户取消
  }
}

// 设为网格
const handleSetAsGrid = async (video: VideoItem) => {
  if (gridCount.value >= 4) {
    ElMessage.warning('网格位置已满（最多4个），请先移除现有推荐')
    return
  }
  try {
    await ElMessageBox.confirm(`确定将"${video.title}"设为网格推荐吗？`, '确认添加')
    const res = await addRecommend({ videoPostId: video.id, positionType: 2 })
    if (res.code === 200) {
      ElMessage.success('添加成功')
      loadGridList()
      loadAvailableVideos()
    } else {
      ElMessage.error(res.message || '添加失败')
    }
  } catch (error) {
    // 用户取消
  }
}

// 移除推荐
const handleRemove = async (item: VideoItem) => {
  try {
    await ElMessageBox.confirm(`确定移除"${item.title}"的推荐吗？`, '确认移除')
    const res = await removeRecommend(item.id)
    if (res.code === 200) {
      ElMessage.success('移除成功')
      loadCarouselList()
      loadGridList()
    }
  } catch (error) {
    // 用户取消
  }
}

// 上移
const handleMoveUp = async (item: VideoItem) => {
  try {
    const res = await moveUpRecommend(item.id)
    if (res.code === 200) {
      ElMessage.success('移动成功')
      loadGridList()
    } else {
      ElMessage.error(res.message || '移动失败')
    }
  } catch (error) {
    console.error('移动失败:', error)
  }
}

// 下移
const handleMoveDown = async (item: VideoItem) => {
  try {
    const res = await moveDownRecommend(item.id)
    if (res.code === 200) {
      ElMessage.success('移动成功')
      loadGridList()
    } else {
      ElMessage.error(res.message || '移动失败')
    }
  } catch (error) {
    console.error('移动失败:', error)
  }
}

// 加载轮播列表
const loadCarouselList = async () => {
  carouselLoading.value = true
  try {
    const res = await getRecommendByPosition(1)
    if (res.code === 200) {
      carouselList.value = res.data || []
    }
  } catch (error) {
    console.error('加载失败:', error)
  } finally {
    carouselLoading.value = false
  }
}

// 加载网格列表
const loadGridList = async () => {
  gridLoading.value = true
  try {
    const res = await getRecommendByPosition(2)
    if (res.code === 200) {
      gridList.value = res.data || []
    }
  } catch (error) {
    console.error('加载失败:', error)
  } finally {
    gridLoading.value = false
  }
}

// 获取封面 URL
const getCoverUrl = (url: string): string => {
  if (!url) return ''
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }
  return '/api' + url
}

// 格式化数字
const formatCount = (count: number): string => {
  if (!count) return '0'
  if (count >= 10000) return (count / 10000).toFixed(1) + 'w'
  if (count >= 1000) return (count / 1000).toFixed(1) + 'k'
  return count.toString()
}

onMounted(() => {
  loadCarouselList()
  loadGridList()
})
</script>

<style scoped lang="scss">
.recommend-manage-container {
  padding: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;

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

.recommend-section {
  .position-card {
    .card-header {
      display: flex;
      justify-content: space-between;
      align-items: center;

      .header-title {
        display: flex;
        align-items: center;
        gap: 8px;

        .carousel-icon {
          color: #f56c6c;
          font-size: 18px;
        }

        .grid-icon {
          color: #67c23a;
          font-size: 18px;
        }
      }
    }
  }
}

.table-cover {
  width: 80px;
  height: 45px;
  border-radius: 4px;
  overflow: hidden;

  &.small {
    width: 60px;
    height: 34px;
  }

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

// 弹窗样式
.dialog-search {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;

  .search-input {
    width: 300px;
  }

  .search-hint {
    font-size: 12px;
    color: #909399;
  }
}

.dialog-pagination {
  display: flex;
  justify-content: center;
  margin-top: 16px;
}
</style>