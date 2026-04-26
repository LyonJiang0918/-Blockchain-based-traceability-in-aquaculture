import http from './http'

/**
 * 养殖群管理API
 */
const batchApi = {
  // 创建养殖群
  create: (data) => http.post('/batch', data),

  // 查询养殖群详情
  getById: (groupId) => http.get(`/batch/${groupId}`),

  // 更新养殖群状态
  // targetId: 目标加工厂ID或目标销售商ID（出栏和送至零售商时必填）
  // status 1=出栏 → targetProcessId, status 4=送至零售商 → targetSalesId
  updateStatus: (groupId, status, targetId = null) => {
    let url = `/batch/${groupId}/status?status=${status}`
    if (targetId) {
      if (status === 1) {
        // 出栏：指定目标加工厂
        url += `&targetProcessId=${encodeURIComponent(targetId)}`
      } else if (status === 4) {
        // 送至零售商：指定目标销售商
        url += `&targetSalesId=${encodeURIComponent(targetId)}`
      }
    }
    return http.put(url)
  },

  // 返回在栏（撤回出栏）
  returnToFarm: (groupId) => http.put(`/batch/${groupId}/return`),

  // 作废单个养殖群
  invalidate: (groupId, reason) => {
    const params = reason ? `?reason=${encodeURIComponent(reason)}` : ''
    return http.put(`/batch/${groupId}/invalidate${params}`)
  },

  // 作废所有养殖群
  invalidateAll: (reason) => {
    const params = reason ? `?reason=${encodeURIComponent(reason)}` : ''
    return http.put(`/batch/all/invalidate${params}`)
  },

  // 批量删除所有养殖群（已废弃，改用 invalidateAll）
  deleteAll: () => http.delete('/batch/all'),

  // 查询养殖群列表
  getList: (params) => http.get('/batch', { params }),

  // 消费者溯源查询（无需登录）
  trace: (groupId) => http.get(`/batch/trace/${groupId}`)
}

export default batchApi



