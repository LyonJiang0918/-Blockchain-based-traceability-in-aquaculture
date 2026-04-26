import http from './http'

/**
 * 农副产品管理API
 */
export const byProductApi = {
  // 创建副产品
  create: (data) => http.post('/byproduct', data),

  // 根据养殖群ID查询副产品列表
  listByGroupId: (groupId) => http.get(`/byproduct/group/${groupId}`),

  // 查询副产品详情
  getById: (productId) => http.get(`/byproduct/${productId}`),

  // 更新副产品状态
  updateStatus: (productId, status) => http.put(`/byproduct/${productId}/status?status=${status}`),
}

export default byProductApi
