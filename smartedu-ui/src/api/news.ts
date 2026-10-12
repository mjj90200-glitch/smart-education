import { get, post, put, del } from '@/utils/request'

// API 响应类型定义
export interface News {
  id: number
  title: string
  summary: string
  content?: string
  imageUrl: string
  sourceUrl: string
  sourceName: string
  newsType: number  // 1=轮播图 2=列表
  isTop: number     // 0=否 1=是
  isManual: number  // 0=自动 1=手动
  publishTime: string
  createTime: string
  updateTime: string
}

/**
 * 获取轮播图新闻
 */
export function getCarouselNews() {
  return get<News[]>('/news/carousel')
}

/**
 * 获取列表新闻
 * @param limit 数量限制，默认 10
 */
export function getListNews(limit: number = 10) {
  return get<News[]>('/news/list', { limit })
}

/**
 * 获取新闻详情
 * @param id 新闻 ID
 */
export function getNewsDetail(id: number) {
  return get<News>(`/news/${id}`)
}

/**
 * 手动触发新闻更新（管理员）
 */
export function manualUpdateNews() {
  return post<{ savedCount: number; message: string }>('/news/manual-update')
}

/**
 * 手动触发新闻更新（开发测试，无需权限）
 */
export function manualUpdateNewsDev() {
  return post<{ savedCount: number; message: string }>('/news/manual-update-dev')
}

/**
 * 删除新闻
 */
export function deleteNews(id: number) {
  return del(`/news/${id}`)
}

/**
 * 更新新闻置顶状态
 * @param id 新闻 ID
 * @param isTop 是否置顶：1-置顶，0-取消
 */
export function updateNewsTopStatus(id: number, isTop: number) {
  return put(`/news/${id}/top`, { isTop })
}

/**
 * 获取新闻列表（管理后台用，支持分页）
 * @param params 分页参数
 */
export function getAdminNewsList(params?: { pageNum?: number; pageSize?: number; keyword?: string; newsType?: number }) {
  return get<any>('/news/admin/list', params || {})
}

/**
 * 保存新闻（新增或更新）
 */
export function saveNews(data: {
  id?: number
  title: string
  summary?: string
  content?: string
  imageUrl?: string
  sourceUrl?: string
  sourceName?: string
  newsType?: number
  isTop?: number
  isManual?: number
  publishTime?: string
}) {
  return post<any>('/news/save', data)
}

/**
 * 保存轮播图排序
 * @param newsIds 新闻 ID 列表（按顺序）
 */
export function saveCarouselOrder(newsIds: number[]) {
  return post<any>('/news/admin/carousel/order', { newsIds })
}

/**
 * 批量更新新闻类型
 * @param ids 新闻 ID 列表
 * @param newsType 新闻类型：1=轮播图，2=列表
 */
export function batchUpdateNewsType(ids: number[], newsType: number) {
  return put('/news/admin/batch-update-type', { ids, newsType })
}

/**
 * 上传新闻图片
 * @param file 图片文件
 */
export function uploadNewsImage(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  return post<{ imageUrl: string; filename: string; originalFilename: string }>('/news/admin/upload-image', formData, {
    headers: {
      'Content-Type': 'multipart/form-data'
    }
  })
}

/**
 * 获取新闻图片 URL
 * @param filename 文件名
 */
export function getNewsImageUrl(filename: string): string {
  return `/news/image/${filename}`
}