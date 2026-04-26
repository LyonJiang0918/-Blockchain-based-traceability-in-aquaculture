import http from './http'

const userApi = {
  login: (username, password) =>
    http.post('/user/login', null, { params: { username, password } }),

  logout: () => http.post('/user/logout'),

  register: (data) => http.post('/user/register', null, { params: data }),

  getMe: () => http.get('/user/me'),

  check: () => http.get('/user/check'),

  list: () => http.get('/user'),

  create: (data) => http.post('/user', null, { params: data }),

  update: (id, data) => http.put(`/user/${id}`, null, { params: data }),

  delete: (id) => http.delete(`/user/${id}`),

  // 根据角色获取用户列表
  getByRole: (role) => http.get(`/user/by-role/${role}`),
}

export default userApi
