import request from '@/utils/request'

/**
 * 获取首页视频（6个）
 */
export function getHomeVideos() {
  return request.get('/video/home')
}

/**
 * 获取视频列表（分页）
 */
export function getVideoList(params: { page?: number; size?: number; keyword?: string }) {
  return request.get('/video/list', { params })
}

/**
 * 获取视频详情
 */
export function getVideoDetail(id: number) {
  return request.get(`/video/${id}`)
}

/**
 * 用户投稿视频
 */
export function submitVideo(data: { title: string; coverUrl: string; videoUrl: string; description?: string }) {
  return request.post('/video/submit', data)
}

/**
 * 收藏/取消收藏视频
 */
export function toggleCollection(id: number) {
  return request.post(`/video/${id}/collect`)
}

/**
 * 获取用户收藏列表
 */
export function getUserCollections(params: { page?: number; size?: number }) {
  return request.get('/video/collections', { params })
}

/**
 * 获取待审核视频列表（管理员）
 */
export function getPendingVideos(params: { page?: number; size?: number }) {
  return request.get('/video/pending', { params })
}

/**
 * 获取已审核通过的视频列表（管理员）
 */
export function getApprovedVideos(params: { page?: number; size?: number; keyword?: string }) {
  return request.get('/video/list', { params })
}

/**
 * 审核视频（管理员）
 */
export function auditVideo(data: { videoId: number; status: number; rejectReason?: string }) {
  return request.post('/video/audit', data)
}

/**
 * 删除视频（管理员）
 */
export function deleteVideo(id: number) {
  console.log('调用删除视频API，ID:', id)
  console.log('请求URL:', `/video/${id}`)
  // 确保使用正确的 DELETE 请求
  return request({
    url: `/video/${id}`,
    method: 'delete'
  })
}