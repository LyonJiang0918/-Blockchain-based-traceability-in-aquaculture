import React from 'react'
import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { ConfigProvider } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import { AuthProvider } from './store/auth'
import PrivateRoute from './components/PrivateRoute'
import MainLayout from './components/MainLayout'
import Login from './pages/Login'
import Register from './pages/Register'
import BatchList from './pages/BatchList'
import BatchDetail from './pages/BatchDetail'
import TraceQuery from './pages/TraceQuery'
import UserManage from './pages/UserManage'
import Settings from './pages/Settings'
import ProcessList from './pages/ProcessList'

function App() {
  return (
    <ConfigProvider locale={zhCN}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            <Route path="/login" element={<Login />} />
            <Route path="/register" element={<Register />} />
            {/* 溯源查询：无需登录，消费者可直接访问 */}
            <Route path="/trace" element={<TraceQuery />} />
            {/* 以下页面需要登录 */}
            <Route
              path="/*"
              element={
                <PrivateRoute>
                  <MainLayout>
                    <Routes>
                      <Route index element={<BatchList />} />
                      <Route path="batch" element={<BatchList />} />
                      <Route path="batch/:id" element={<BatchDetail />} />
                      <Route path="users" element={<UserManage />} />
                      <Route path="process" element={<ProcessList />} />
                      <Route path="settings" element={<Settings />} />
                    </Routes>
                  </MainLayout>
                </PrivateRoute>
              }
            />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </ConfigProvider>
  )
}

export default App
