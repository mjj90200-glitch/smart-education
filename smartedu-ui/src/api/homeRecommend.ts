import request from '@/utils/request'

/**
 * 获取首页推荐视频列表（公开）
 */
export function getHomeRecommendList() {
  return request({
    url: '/home-recommend/list',
    method: 'get'
  })
}

/**
 * 获取指定位置的推荐列表（管理员）
 * @param type 位置类型：1=轮播, 2=网格
 */
export function getRecommendByPosition(type: number) {
  return request({
    url: `/home-recommend/position/${type}`,
    method: 'get'
  })
}

/**
 * 获取未推荐的已审核视频列表（管理员）
 * 用于添加推荐时选择
 */
export function getUnrecommendedVideos(params: { keyword?: string; page?: number; size?: number }) {
  return request({
    url: '/home-recommend/unrecommended',
    method: 'get',
    params
  })
}

/**
 * 搜索可推荐的视频（管理员）
 * @deprecated 请使用 getUnrecommendedVideos
 */
export function searchAvailableVideos(params: { keyword?: string; page?: number; size?: number }) {
  return request({
    url: '/home-recommend/search',
    method: 'get',
    params
  })
}

/**
 * 添加推荐视频（管理员）
 */
export function addRecommend(data: { videoPostId: number; positionType: number; sortOrder?: number }) {
  return request({
    url: '/home-recommend/add',
    method: 'post',
    data
  })
}

/**
 * 移除推荐视频（管理员）
 */
export function removeRecommend(id: number) {
  return request({
    url: `/home-recommend/${id}`,
    method: 'delete'
  })
}

/**
 * 上移排序（管理员）
 */
export function moveUpRecommend(id: number) {
  return request({
    url: `/home-recommend/${id}/move-up`,
    method: 'put'
  })
}

/**
 * 下移排序（管理员）
 */
export function moveDownRecommend(id: number) {
  return request({
    url: `/home-recommend/${id}/move-down`,
    method: 'put'
  })
}