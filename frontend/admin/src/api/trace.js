import http from './http'

/**
 * 溯源查询API
 */
export const traceApi = {
  // 查询完整溯源链
  getTraceInfo: (batchId) => http.get(`/trace/${batchId}`)
}

export default traceApi



