<template>
  <div class="news-manage-container page-container">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>新闻管理</span>
          <div class="header-actions">
            <el-button type="success" @click="handleRefreshNews" :loading="refreshLoading">
              <el-icon><Refresh /></el-icon>
              抓取新闻
            </el-button>
            <el-button type="primary" @click="openAddDialog">
              <el-icon><Plus /></el-icon>
              新增新闻
            </el-button>
            <el-button type="warning" @click="showCarouselDialog = true">
              <el-icon><Picture /></el-icon>
              轮播图管理
            </el-button>
          </div>
        </div>
      </template>

      <!-- 筛选条件 -->
      <div class="filter-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索新闻标题"
          style="width: 200px"
          clearable
          @clear="loadNewsList"
        >
          <template #append>
            <el-button @click="loadNewsList">
              <el-icon><Search /></el-icon>
            </el-button>
          </template>
        </el-input>

        <el-select v-model="filterNewsType" placeholder="新闻类型" style="width: 150px" clearable @change="loadNewsList">
          <el-option label="轮播图" :value="1" />
          <el-option label="列表新闻" :value="2" />
        </el-select>

        <el-button @click="loadNewsList">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>

        <!-- 批量操作 -->
        <el-dropdown @command="handleBatchOperation" v-if="selectionList.length > 0">
          <el-button type="primary">
            批量操作 <el-icon><ArrowDown /></el-icon>
          </el-button>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="setCarousel">设为轮播图</el-dropdown-item>
              <el-dropdown-item command="setList">设为列表新闻</el-dropdown-item>
              <el-dropdown-item command="delete" divided>批量删除</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <!-- 新闻列表 -->
      <el-table :data="newsList" style="width: 100%" v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="title" label="标题" min-width="250" show-overflow-tooltip />
        <el-table-column prop="sourceName" label="来源" width="120" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag :type="row.newsType === 1 ? 'success' : 'primary'" size="small">
              {{ row.newsType === 1 ? '轮播图' : '列表' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="置顶" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isTop === 1 ? 'danger' : 'info'" size="small">
              {{ row.isTop === 1 ? '置顶' : '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="来源" width="80">
          <template #default="{ row }">
            <el-tag :type="row.isManual === 1 ? 'warning' : 'success'" size="small">
              {{ row.isManual === 1 ? '手动' : '自动' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="publishTime" label="发布时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.publishTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="openEditDialog(row)">编辑</el-button>
            <el-button
              type="warning"
              link
              size="small"
              @click="toggleTop(row)"
            >
              {{ row.isTop === 1 ? '取消置顶' : '置顶' }}
            </el-button>
            <el-button type="danger" link size="small" @click="deleteNews(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @current-change="loadNewsList"
          @size-change="loadNewsList"
        />
      </div>
    </el-card>

    <!-- 新增/编辑新闻对话框 -->
    <el-dialog
      v-model="showDialog"
      :title="isEdit ? '编辑新闻' : '新增新闻'"
      width="700px"
      destroy-on-close
    >
      <el-form :model="form" label-width="100px" :rules="formRules" ref="formRef">
        <el-form-item label="新闻标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入新闻标题" />
        </el-form-item>

        <el-form-item label="新闻摘要" prop="summary">
          <el-input v-model="form.summary" type="textarea" :rows="3" placeholder="请输入新闻摘要（可选）" />
        </el-form-item>

        <el-form-item label="详细内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" placeholder="请输入新闻详细内容（可选）" />
        </el-form-item>

        <el-form-item label="封面图片" prop="imageUrl">
          <el-input v-model="form.imageUrl" placeholder="请输入图片 URL 或从下方选择" clearable>
            <template #append>
              <el-button @click="triggerUpload" :loading="uploading">
                <el-icon><Upload /></el-icon>
                {{ uploading ? '上传中...' : '上传' }}
              </el-button>
            </template>
          </el-input>
          <div class="form-tip">支持 JPG、PNG、GIF 格式，最大 10MB，或从下方预设图片选择</div>

          <!-- 隐藏的文件输入 -->
          <input
            ref="fileInputRef"
            type="file"
            accept="image/*"
            style="display: none"
            @change="handleFileChange"
          />

          <!-- 图片预览 -->
          <div v-if="form.imageUrl" class="image-preview">
            <img :src="getImageUrl(form.imageUrl)" alt="封面预览" @error="handlePreviewError" />
          </div>

          <!-- 快速选择图片 -->
          <div class="quick-image-selector">
            <div class="selector-label">快速选择：</div>
            <div class="image-options">
              <div
                v-for="(img, index) in defaultImageOptions"
                :key="index"
                class="image-option"
                :class="{ selected: form.imageUrl === img }"
                @click="form.imageUrl = img"
              >
                <img :src="img" :alt="'选项' + (index + 1)" />
                <el-icon v-if="form.imageUrl === img" class="selected-icon"><Check /></el-icon>
              </div>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="原文链接" prop="sourceUrl">
          <el-input v-model="form.sourceUrl" placeholder="请输入原文链接（可选）" />
        </el-form-item>

        <el-form-item label="来源名称" prop="sourceName">
          <el-input v-model="form.sourceName" placeholder="请输入来源名称，如：OpenAI 官方博客" />
        </el-form-item>

        <el-form-item label="新闻类型" prop="newsType">
          <el-radio-group v-model="form.newsType">
            <el-radio :value="1">轮播图</el-radio>
            <el-radio :value="2">列表新闻</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="是否置顶">
          <el-radio-group v-model="form.isTop">
            <el-radio :value="1">是</el-radio>
            <el-radio :value="0">否</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="发布时间" prop="publishTime">
          <el-date-picker
            v-model="form.publishTime"
            type="datetime"
            placeholder="选择发布时间"
            style="width: 100%"
            format="YYYY-MM-DD HH:mm"
            value-format="YYYY-MM-DD HH:mm:ss"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>

    <!-- 轮播图管理对话框 -->
    <el-dialog
      v-model="showCarouselDialog"
      title="轮播图管理"
      width="900px"
      destroy-on-close
      @open="handleCarouselDialogOpen"
    >
      <div class="carousel-manage">
        <el-alert
          title="操作说明"
          type="info"
          :closable="false"
          show-icon
          class="mb-10"
        >
          <p>1. 拖拽排序：拖动左侧的拖拽手柄调整轮播图顺序</p>
          <p>2. 添加新闻：从右侧候选列表中选择新闻，点击"添加"按钮</p>
          <p>3. 移除新闻：点击轮播图列表中的"移除"按钮（仅从轮播图移除，不删除新闻）</p>
          <p>4. 最多显示 6 张轮播图</p>
        </el-alert>

        <el-row :gutter="20">
          <!-- 左侧：当前轮播图列表 -->
          <el-col :span="12">
            <div class="panel">
              <div class="panel-header">
                <span>📌 当前轮播图（{{ carouselList.length }}/6）</span>
              </div>
              <div class="panel-body">
                <el-empty v-if="carouselList.length === 0" description="暂无轮播图" :image-size="80" />
                <vue-draggable
                  v-else
                  v-model="carouselList"
                  :animation="200"
                  handle=".drag-handle"
                  class="draggable-list"
                >
                  <div
                    v-for="item in carouselList"
                    :key="item.id"
                    class="draggable-item"
                  >
                    <el-icon class="drag-handle"><Rank /></el-icon>
                    <div class="item-content">
                      <el-tag type="success" size="small" class="order-tag">{{ item.orderIndex }}</el-tag>
                      <span class="item-title">{{ item.title }}</span>
                    </div>
                    <div class="item-actions">
                      <el-button
                        type="primary"
                        link
                        size="small"
                        @click="moveUp(item)"
                        :disabled="carouselList.indexOf(item) === 0"
                      >
                        <el-icon><Top /></el-icon>
                      </el-button>
                      <el-button
                        type="primary"
                        link
                        size="small"
                        @click="moveDown(item)"
                        :disabled="carouselList.indexOf(item) === carouselList.length - 1"
                      >
                        <el-icon><Bottom /></el-icon>
                      </el-button>
                      <el-button
                        type="danger"
                        link
                        size="small"
                        @click="removeFromCarousel(item)"
                      >
                        <el-icon><Delete /></el-icon>
                      </el-button>
                    </div>
                  </div>
                </vue-draggable>
              </div>
            </div>
          </el-col>

          <!-- 右侧：候选新闻列表 -->
          <el-col :span="12">
            <div class="panel">
              <div class="panel-header">
                <span>📋 候选新闻列表</span>
                <el-input
                  v-model="carouselKeyword"
                  placeholder="搜索候选新闻"
                  size="small"
                  style="width: 150px"
                  clearable
                  @input="loadCarouselCandidates"
                />
              </div>
              <div class="panel-body">
                <el-empty v-if="carouselCandidates.length === 0" description="暂无候选新闻" :image-size="80" />
                <div v-else class="candidate-list">
                  <div
                    v-for="item in carouselCandidates"
                    :key="item.id"
                    class="candidate-item"
                    :class="{ disabled: isAlreadyInCarousel(item.id) }"
                  >
                    <div class="item-content">
                      <span class="item-title">{{ item.title }}</span>
                      <el-tag :type="item.newsType === 1 ? 'success' : 'primary'" size="small">
                        {{ item.newsType === 1 ? '轮播图' : '列表' }}
                      </el-tag>
                    </div>
                    <el-button
                      type="primary"
                      size="small"
                      @click="addToCarousel(item)"
                      :disabled="isAlreadyInCarousel(item.id) || carouselList.length >= 6"
                    >
                      添加
                    </el-button>
                  </div>
                </div>
              </div>
            </div>
          </el-col>
        </el-row>
      </div>

      <template #footer>
        <el-button @click="showCarouselDialog = false">取消</el-button>
        <el-button type="primary" @click="saveCarouselSettings" :loading="savingCarousel">
          保存轮播图设置
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Refresh, Search, Picture, Rank, Top, Bottom, Delete, ArrowDown, Check, Upload } from '@element-plus/icons-vue'
import { VueDraggable } from 'vue-draggable-plus'
import {
  getAdminNewsList,
  saveNews,
  deleteNews as apiDeleteNews,
  updateNewsTopStatus,
  manualUpdateNewsDev,
  saveCarouselOrder,
  batchUpdateNewsType,
  uploadNewsImage
} from '@/api/news'

// 默认图片选项（供管理员快速选择）
const defaultImageOptions = [
  'https://images.unsplash.com/photo-1518770660439-4636190af475?w=800&h=400&fit=crop',  // 芯片
  'https://images.unsplash.com/photo-1519389950473-47ba0277781c?w=800&h=400&fit=crop',  // 编程
  'https://images.unsplash.com/photo-1504639725590-34d0984388bd?w=800&h=400&fit=crop',  // AI
  'https://images.unsplash.com/photo-1550751827-4bd374c3f58b?w=800&h=400&fit=crop',  // 网络安全
  'https://images.unsplash.com/photo-1526374965328-7f61d4dc18c5?w=800&h=400&fit=crop',  // 黑客
  'https://images.unsplash.com/photo-1555255714-732bb582f834?w=800&h=400&fit=crop',  // 数据
  'https://images.unsplash.com/photo-1558494949-ef010cbdcc31?w=800&h=400&fit=crop',  // 云服务
  'https://images.unsplash.com/photo-1485827404703-89b55fcc595e?w=800&h=400&fit=crop'   // 机器人
]

// 数据
const newsList = ref<any[]>([])
const loading = ref(false)
const refreshLoading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const keyword = ref('')
const filterNewsType = ref<number | null>(null)

// 批量选择
const selectionList = ref<any[]>([])

// 对话框
const showDialog = ref(false)
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref()
const fileInputRef = ref<HTMLInputElement>()
const uploading = ref(false)

// 轮播图管理
const showCarouselDialog = ref(false)
const carouselList = ref<any[]>([])
const carouselCandidates = ref<any[]>([])
const carouselKeyword = ref('')
const savingCarousel = ref(false)

// 表单
const form = reactive({
  id: null as number | null,
  title: '',
  summary: '',
  content: '',
  imageUrl: '',
  sourceUrl: '',
  sourceName: '',
  newsType: 2,
  isTop: 0,
  isManual: 1,
  publishTime: ''
})

// 表单验证规则
const formRules = {
  title: [{ required: true, message: '请输入新闻标题', trigger: 'blur' }],
  sourceName: [{ required: true, message: '请输入来源名称', trigger: 'blur' }]
}

// 加载新闻列表
const loadNewsList = async () => {
  loading.value = true
  try {
    const res = await getAdminNewsList({
      pageNum: currentPage.value,
      pageSize: pageSize.value,
      keyword: keyword.value || undefined,
      newsType: filterNewsType.value || undefined
    })

    if (res.code === 200) {
      newsList.value = res.data?.records || []
      total.value = res.data?.total || 0
    } else {
      ElMessage.error(res.message || '加载失败')
    }
  } catch (e: any) {
    console.error('加载新闻失败:', e)
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

// 打开新增对话框
const openAddDialog = () => {
  isEdit.value = false
  form.id = null
  form.title = ''
  form.summary = ''
  form.content = ''
  form.imageUrl = ''
  form.sourceUrl = ''
  form.sourceName = ''
  form.newsType = 2
  form.isTop = 0
  form.isManual = 1
  form.publishTime = new Date().toISOString().slice(0, 16).replace('T', ' ')
  showDialog.value = true
}

// 打开编辑对话框
const openEditDialog = (row: any) => {
  isEdit.value = true
  form.id = row.id
  form.title = row.title
  form.summary = row.summary || ''
  form.content = row.content || ''
  form.imageUrl = row.imageUrl || ''
  form.sourceUrl = row.sourceUrl || ''
  form.sourceName = row.sourceName || ''
  form.newsType = row.newsType
  form.isTop = row.isTop
  form.isManual = row.isManual
  form.publishTime = row.publishTime ? row.publishTime.slice(0, 16).replace('T', ' ') : ''
  showDialog.value = true
}

// 保存新闻
const handleSave = async () => {
  try {
    await formRef.value?.validate()
  } catch {
    return
  }

  saving.value = true
  try {
    const data = {
      id: form.id || undefined,
      title: form.title,
      summary: form.summary,
      content: form.content,
      imageUrl: form.imageUrl,
      sourceUrl: form.sourceUrl,
      sourceName: form.sourceName,
      newsType: form.newsType,
      isTop: form.isTop,
      isManual: form.isManual,
      // 使用 ISO 格式 YYYY-MM-DDTHH:mm:ss
      publishTime: form.publishTime ? form.publishTime.replace(' ', 'T') + ':00' : new Date().toISOString().slice(0, 19)
    }

    const res = await saveNews(data)
    if (res.code === 200) {
      ElMessage.success('保存成功')
      showDialog.value = false
      loadNewsList()
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  } finally {
    saving.value = false
  }
}

// 删除新闻
const deleteNews = async (row: any) => {
  try {
    await ElMessageBox.confirm('确定要删除该新闻吗？', '提示', { type: 'warning' })
    const res = await apiDeleteNews(row.id)
    if (res.code === 200) {
      ElMessage.success('删除成功')
      loadNewsList()
    } else {
      ElMessage.error(res.message || '删除失败')
    }
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '删除失败')
  }
}

// 切换置顶状态
const toggleTop = async (row: any) => {
  const newTop = row.isTop === 1 ? 0 : 1
  try {
    const res = await updateNewsTopStatus(row.id, newTop)
    if (res.code === 200) {
      ElMessage.success(newTop === 1 ? '已置顶' : '已取消置顶')
      row.isTop = newTop
    } else {
      ElMessage.error(res.message || '操作失败')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

// 刷新新闻（从外部 API 抓取）
const handleRefreshNews = async () => {
  try {
    await ElMessageBox.confirm('确定要从外部 API 抓取最新新闻吗？', '提示', {
      type: 'info',
      confirmButtonText: '立即抓取',
      cancelButtonText: '取消'
    })

    refreshLoading.value = true
    const res = await manualUpdateNewsDev()
    if (res.code === 200) {
      const count = res.data?.savedCount || 0
      ElMessage.success(`抓取完成，新增 ${count} 条新闻`)
      loadNewsList()
    } else {
      ElMessage.error(res.message || '抓取失败')
    }
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '抓取失败')
  } finally {
    refreshLoading.value = false
  }
}

// 格式化时间
const formatDateTime = (datetime: string) => {
  if (!datetime) return '-'
  return datetime.replace('T', ' ').substring(0, 16)
}

// 处理表格选择变化
const handleSelectionChange = (selection: any[]) => {
  selectionList.value = selection
}

// 批量操作
const handleBatchOperation = async (command: string) => {
  if (selectionList.value.length === 0) return

  try {
    if (command === 'delete') {
      await ElMessageBox.confirm(`确定要删除选中的 ${selectionList.value.length} 条新闻吗？`, '提示', { type: 'warning' })
      const ids = selectionList.value.map(item => item.id)
      for (const id of ids) {
        await apiDeleteNews(id)
      }
      ElMessage.success('批量删除成功')
    } else if (command === 'setCarousel') {
      const ids = selectionList.value.map(item => item.id)
      const res = await batchUpdateNewsType(ids, 1)
      if (res.code === 200) {
        ElMessage.success('已设为轮播图')
      }
    } else if (command === 'setList') {
      const ids = selectionList.value.map(item => item.id)
      const res = await batchUpdateNewsType(ids, 2)
      if (res.code === 200) {
        ElMessage.success('已设为列表新闻')
      }
    }
    loadNewsList()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

// 加载候选新闻列表
const loadCarouselCandidates = async () => {
  try {
    const res = await getAdminNewsList({
      pageNum: 1,
      pageSize: 100,
      keyword: carouselKeyword.value || undefined
    })
    if (res.code === 200) {
      carouselCandidates.value = res.data?.records || []
    }
  } catch (e: any) {
    console.error('加载候选新闻失败:', e)
  }
}

// 初始化轮播图列表（从当前新闻列表中筛选 newsType=1 的）
const initCarouselList = () => {
  const carouselNews = newsList.value.filter((item: any) => item.newsType === 1)
  carouselList.value = carouselNews.map((item: any, index: number) => ({
    ...item,
    orderIndex: index + 1
  }))
}

// 检查新闻是否已在轮播图中
const isAlreadyInCarousel = (id: number) => {
  return carouselList.value.some((item: any) => item.id === id)
}

// 添加新闻到轮播图
const addToCarousel = (item: any) => {
  if (isAlreadyInCarousel(item.id) || carouselList.value.length >= 6) return
  carouselList.value.push({
    ...item,
    orderIndex: carouselList.value.length + 1
  })
}

// 从轮播图移除（仅移除，不删除新闻）
const removeFromCarousel = (item: any) => {
  const index = carouselList.value.findIndex((i: any) => i.id === item.id)
  if (index !== -1) {
    carouselList.value.splice(index, 1)
    // 重新排序
    carouselList.value.forEach((i: any, idx: number) => {
      i.orderIndex = idx + 1
    })
  }
}

// 向上移动
const moveUp = (item: any) => {
  const index = carouselList.value.findIndex((i: any) => i.id === item.id)
  if (index > 0) {
    const temp = carouselList.value[index - 1]
    carouselList.value[index - 1] = item
    carouselList.value[index] = temp
    // 重新排序
    carouselList.value.forEach((i: any, idx: number) => {
      i.orderIndex = idx + 1
    })
  }
}

// 向下移动
const moveDown = (item: any) => {
  const index = carouselList.value.findIndex((i: any) => i.id === item.id)
  if (index < carouselList.value.length - 1) {
    const temp = carouselList.value[index + 1]
    carouselList.value[index + 1] = item
    carouselList.value[index] = temp
    // 重新排序
    carouselList.value.forEach((i: any, idx: number) => {
      i.orderIndex = idx + 1
    })
  }
}

// 保存轮播图设置
const saveCarouselSettings = async () => {
  savingCarousel.value = true
  try {
    const newsIds = carouselList.value.map((item: any) => item.id)
    console.log('保存轮播图顺序，新闻 IDs:', newsIds)
    const res = await saveCarouselOrder(newsIds)
    console.log('保存轮播图响应:', res)
    if (res.code === 200) {
      ElMessage.success('轮播图设置已保存')
      showCarouselDialog.value = false
      loadNewsList()
    } else {
      ElMessage.error(res.message || '保存失败')
    }
  } catch (e: any) {
    console.error('保存轮播图失败:', e)
    ElMessage.error(e.message || '保存失败')
  } finally {
    savingCarousel.value = false
  }
}

// 轮播图对话框打开时加载候选列表
const handleCarouselDialogOpen = async () => {
  carouselKeyword.value = ''
  await loadCarouselCandidates()
  initCarouselList()
}

// 图片预览错误处理
const handlePreviewError = (event: Event) => {
  const img = event.target as HTMLImageElement
  img.src = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" width="200" height="100" viewBox="0 0 200 100"%3E%3Crect fill="%23ddd" width="200" height="100"/%3E%3Ctext fill="%23999" x="50%25" y="50%25" text-anchor="middle" dy=".3em"%3E图片加载失败%3C/text%3E%3C/svg%3E'
  ElMessage.warning('图片加载失败，请检查 URL 是否有效')
}

// 获取图片完整 URL（添加 /api 前缀）
const getImageUrl = (url: string): string => {
  if (!url) return ''
  // 如果是完整 URL（http 开头），直接返回
  if (url.startsWith('http://') || url.startsWith('https://')) {
    return url
  }
  // 否则添加 /api 前缀
  return '/api' + url
}

onMounted(() => {
  loadNewsList()
})

// 触发文件上传
const triggerUpload = () => {
  fileInputRef.value?.click()
}

// 处理文件选择
const handleFileChange = async (event: Event) => {
  const target = event.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  // 验证文件类型
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请上传图片文件（支持 JPG、PNG、GIF 等格式）')
    return
  }

  // 验证文件大小（最大 10MB）
  const maxSize = 10 * 1024 * 1024
  if (file.size > maxSize) {
    ElMessage.error('图片体积过大，请选择小于 10MB 的图片')
    return
  }

  // 上传图片
  uploading.value = true
  try {
    const res = await uploadNewsImage(file)
    if (res.code === 200) {
      form.imageUrl = res.data.imageUrl
      ElMessage.success('图片上传成功')
    } else {
      ElMessage.error(res.message || '上传失败')
    }
  } catch (e: any) {
    console.error('图片上传失败:', e)
    ElMessage.error(e.message || '上传失败')
  } finally {
    uploading.value = false
    // 清空文件输入，允许重复上传同一文件
    target.value = ''
  }
}
</script>

<style scoped lang="scss">
.news-manage-container {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;

    .header-actions {
      display: flex;
      gap: 10px;
    }
  }

  .filter-bar {
    display: flex;
    gap: 15px;
    margin-bottom: 20px;
    align-items: center;
  }

  .pagination-bar {
    display: flex;
    justify-content: center;
    margin-top: 20px;
  }

  .form-tip {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }
}

// 图片预览样式（新增/编辑对话框）
.image-preview {
  margin-top: 10px;
  border-radius: 8px;
  overflow: hidden;
  max-width: 400px;
  border: 1px solid #e4e7ed;

  img {
    width: 100%;
    height: 200px;
    object-fit: cover;
    display: block;
  }
}

// 快速选择图片样式（新增/编辑对话框）
.quick-image-selector {
  margin-top: 12px;

  .selector-label {
    font-size: 13px;
    color: #606266;
    margin-bottom: 8px;
    font-weight: 500;
  }

  .image-options {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    gap: 8px;

    .image-option {
      position: relative;
      width: 100%;
      padding-top: 56.25%; // 16:9 比例
      border-radius: 6px;
      overflow: hidden;
      cursor: pointer;
      border: 2px solid transparent;
      transition: all 0.2s ease;

      &:hover {
        border-color: #409EFF;
        transform: scale(1.02);
      }

      &.selected {
        border-color: #409EFF;
        box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
      }

      img {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        object-fit: cover;
      }

      .selected-icon {
        position: absolute;
        top: 50%;
        left: 50%;
        transform: translate(-50%, -50%);
        font-size: 24px;
        color: #fff;
        text-shadow: 0 0 4px rgba(0, 0, 0, 0.5);
      }
    }
  }
}

// 轮播图管理样式
.carousel-manage {
  .mb-10 {
    margin-bottom: 10px;
  }

  .panel {
    border: 1px solid #e4e7ed;
    border-radius: 4px;
    padding: 15px;
    background: #fff;

    .panel-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 15px;
      font-weight: 500;
      font-size: 14px;
    }

    .panel-body {
      max-height: 400px;
      overflow-y: auto;
    }
  }

  .draggable-list {
    .draggable-item {
      display: flex;
      align-items: center;
      padding: 10px 12px;
      margin-bottom: 8px;
      background: #f5f7fa;
      border-radius: 4px;
      cursor: move;

      &:hover {
        background: #ecf5ff;
      }

      .drag-handle {
        font-size: 18px;
        color: #909399;
        cursor: move;
        margin-right: 10px;
      }

      .item-content {
        flex: 1;
        display: flex;
        align-items: center;
        gap: 8px;

        .order-tag {
          min-width: 24px;
          text-align: center;
        }

        .item-title {
          flex: 1;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          font-size: 13px;
        }
      }

      .item-actions {
        display: flex;
        gap: 5px;
      }
    }
  }

  .candidate-list {
    .candidate-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 10px 12px;
      margin-bottom: 8px;
      background: #f5f7fa;
      border-radius: 4px;

      &.disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }

      .item-content {
        flex: 1;
        display: flex;
        align-items: center;
        gap: 8px;

        .item-title {
          flex: 1;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          font-size: 13px;
        }
      }
    }
  }
}
</style>
