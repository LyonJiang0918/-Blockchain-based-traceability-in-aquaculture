import axios from 'axios'
import { message } from 'antd'

const http = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json'
  },
  withCredentials: true
})

// 请求拦截器
http.interceptors.request.use(
  config => {
    return config
  },
  error => Promise.reject(error)
)

// 响应拦截器
http.interceptors.response.use(
  response => {
    const { data } = response
    if (data.success !== false) {
      return data
    } else {
      return Promise.reject(new Error(data.message || '请求失败'))
    }
  },
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('TRACE_AUTH_USER')
      if (window.location.pathname !== '/login') {
        message.warning('登录已过期，请重新登录')
        window.location.href = '/login'
      }
    }
    const msg = error.response?.data?.message || error.message || '网络错误'
    return Promise.reject(new Error(msg))
  }
)

export default http
