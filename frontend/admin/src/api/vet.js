import http from './http'

/**
 * 兽医/疫苗记录API
 */
export const vetApi = {
  create: (data) => http.post('/vet', data),
  listByGroupId: (groupId) => http.get(`/vet/group/${groupId}`),
  listByType: (groupId, type) => http.get(`/vet/group/${groupId}/type/${type}`),
  getById: (recordId) => http.get(`/vet/${recordId}`),
  voidRecord: (recordId) => http.put(`/vet/${recordId}/void`),
}

export default vetApi
