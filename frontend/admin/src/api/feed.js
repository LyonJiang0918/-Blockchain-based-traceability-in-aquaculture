import http from './http'

/**
 * 饲料投喂记录API
 */
export const feedApi = {
  create: (data) => http.post('/feed', data),
  listByGroupId: (groupId) => http.get(`/feed/group/${groupId}`),
  getById: (recordId) => http.get(`/feed/${recordId}`),
  voidRecord: (recordId) => http.put(`/feed/${recordId}/void`),
}

export default feedApi
