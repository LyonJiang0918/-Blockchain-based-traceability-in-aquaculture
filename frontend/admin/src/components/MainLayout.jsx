import React, { useState, useMemo } from 'react'
import { Layout, Menu, Button, Tag, Popconfirm } from 'antd'
import { useNavigate, useLocation } from 'react-router-dom'
import {
  HomeOutlined,
  DatabaseOutlined,
  SearchOutlined,
  SafetyOutlined,
  LogoutOutlined,
  UserOutlined,
  TeamOutlined,
  SettingOutlined,
} from '@ant-design/icons'
import { useAuth, ROLE_MAP } from '../store/auth'
import userApi from '../api/user'

const { Header, Sider, Content, Footer } = Layout

function MainLayout({ children }) {
  const [collapsed, setCollapsed] = useState(false)
  const navigate = useNavigate()
  const location = useLocation()
  const { user, logout } = useAuth()

  const visibleMenus = useMemo(() => {
    const batchLabel =
      user?.role === 'PROCESS' ? '待处理养殖群'
        : user?.role === 'SALES' ? '待销售养殖群'
          : '养殖群管理'
    const all = [
      { key: '/batch', icon: <DatabaseOutlined />, label: batchLabel, roles: ['ADMIN', 'FARM', 'PROCESS', 'SALES'] },
      { key: '/users', icon: <TeamOutlined />, label: '用户管理', roles: ['ADMIN'] },
      { key: '/settings', icon: <SettingOutlined />, label: '系统设置', roles: ['ADMIN'] },
      { key: '/process', icon: <TeamOutlined />, label: '加工管理', roles: ['ADMIN', 'PROCESS'] },
    ]
    return all.filter(m => m.roles.includes(user?.role))
  }, [user?.role])

  const handleMenuClick = ({ key }) => navigate(key)

  const handleLogout = async () => {
    try {
      await userApi.logout()
    } catch (_) {}
    logout()
    navigate('/login')
  }

  const roleColor = {
    ADMIN: 'red',
    FARM: 'blue',
    PROCESS: 'orange',
    SALES: 'green',
  }[user?.role] || 'default'

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        theme="dark"
        style={{
          background: 'linear-gradient(180deg, #001529 0%, #002140 100%)',
          boxShadow: '2px 0 8px rgba(0,0,0,0.3)',
        }}
      >
        <div style={{
          height: 64,
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          paddingTop: 12,
        }}>
          <SafetyOutlined style={{ fontSize: 28, color: '#52c41a', marginBottom: 4 }} />
          {!collapsed && (
            <span style={{
              color: '#fff',
              fontSize: 13,
              fontWeight: 'bold',
              letterSpacing: 1,
              textAlign: 'center',
              lineHeight: 1.2,
            }}>
              品质溯源平台
            </span>
          )}
        </div>

        <Menu
          theme="dark"
          selectedKeys={[location.pathname]}
          mode="inline"
          items={visibleMenus}
          onClick={handleMenuClick}
          style={{ borderRight: 0, marginTop: 8 }}
        />

        {collapsed && (
          <div style={{
            position: 'absolute',
            bottom: 16,
            width: '100%',
            textAlign: 'center',
            color: 'rgba(255,255,255,0.25)',
            fontSize: 10,
          }}>
            FISCO BCOS
          </div>
        )}
      </Sider>

      <Layout>
        <Header style={{
          background: '#fff',
          padding: '0 24px',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
          position: 'sticky',
          top: 0,
          zIndex: 100,
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <HomeOutlined style={{ fontSize: 18, color: '#1890ff' }} />
            <h2 style={{ margin: 0, fontSize: 16, fontWeight: 'bold', color: '#262626' }}>
              养殖业品质溯源管理平台
            </h2>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            {user && (
              <>
                <Tag icon={<UserOutlined />} color={roleColor}>
                  {ROLE_MAP[user.role] || user.role}
                </Tag>
                <span style={{ fontSize: 13, color: '#595959' }}>
                  {user.username}
                </span>
              </>
            )}
            <Popconfirm
              title="确定退出登录？"
              onConfirm={handleLogout}
              okText="确定"
              cancelText="取消"
            >
              <Button
                type="text"
                icon={<LogoutOutlined />}
                size="small"
              >
                退出
              </Button>
            </Popconfirm>
          </div>
        </Header>

        <Content style={{
          margin: 0,
          padding: '24px',
          background: '#f0f2f5',
          minHeight: 'calc(100vh - 64px - 48px)',
        }}>
          {children}
        </Content>

        <Footer style={{
          background: '#fff',
          textAlign: 'center',
          padding: '12px 24px',
          fontSize: 12,
          color: '#8c8c8c',
          borderTop: '1px solid #f0f0f0',
        }}>
          养殖业品质溯源平台 &copy; {new Date().getFullYear()} &middot;
          基于 <span style={{ color: '#1890ff' }}>FISCO BCOS</span> 区块链技术
        </Footer>
      </Layout>
    </Layout>
  )
}

export default MainLayout
