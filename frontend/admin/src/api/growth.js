import http from './http'

/**
 * 成长记录API
 */
export const growthApi = {
  create: (data) => http.post('/growth', data),
  listByGroupId: (groupId) => http.get(`/growth/group/${groupId}`),
  getById: (recordId) => http.get(`/growth/${recordId}`),
  voidRecord: (recordId) => http.put(`/growth/${recordId}/void`),
}

export default growthApi
