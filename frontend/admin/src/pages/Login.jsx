import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { Form, Input, Button, Card, message, Typography, Space, Divider } from 'antd'
import { UserOutlined, LockOutlined, SafetyOutlined } from '@ant-design/icons'
import { useAuth } from '../store/auth'
import userApi from '../api/user'

const { Title, Text } = Typography

export default function Login() {
  const [loading, setLoading] = useState(false)
  const { login } = useAuth()
  const navigate = useNavigate()
  const [form] = Form.useForm()

  const getDefaultPath = (role) => {
    switch (role) {
      case 'ADMIN': return '/'
      case 'FARM': return '/batch'
      // 加工厂/销售商进入管理端后应查看「送达给自己」的养殖群，而非消费者溯源页
      case 'PROCESS': return '/batch'
      case 'SALES': return '/batch'
      default: return '/'
    }
  }

  const handleSubmit = async ({ username, password }) => {
    setLoading(true)
    try {
      const res = await userApi.login(username, password)
      login(res.data)
      message.success('登录成功，欢迎 ' + username)
      const target = getDefaultPath(res.data.role)
      navigate(target, { replace: true })
    } catch (err) {
      message.error(err.message || '登录失败')
      form.resetFields(['password'])
    } finally {
      setLoading(false)
    }
  }

  return (
    <div style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: 'linear-gradient(135deg, #001529 0%, #003a70 50%, #0050b3 100%)',
      padding: 16,
    }}>
      <Card
        style={{
          width: 400,
          borderRadius: 12,
          boxShadow: '0 8px 32px rgba(0,0,0,0.3)',
        }}
        styles={{ body: { padding: 40 } }}
      >
        {/* Logo */}
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <SafetyOutlined style={{ fontSize: 48, color: '#52c41a', marginBottom: 8 }} />
          <Title level={4} style={{ margin: 0, color: '#262626' }}>
            养殖业品质溯源平台
          </Title>
          <Text type="secondary" style={{ fontSize: 13 }}>
            管理端登录
          </Text>
        </div>

        <Divider style={{ margin: '0 0 24' }} />

        <Form
          form={form}
          layout="vertical"
          onFinish={handleSubmit}
          size="large"
          requiredMark={false}
        >
          <Form.Item
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input
              prefix={<UserOutlined style={{ color: '#bfbfbf' }} />}
              placeholder="用户名"
              autoComplete="username"
            />
          </Form.Item>

          <Form.Item
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password
              prefix={<LockOutlined style={{ color: '#bfbfbf' }} />}
              placeholder="密码"
              autoComplete="current-password"
            />
          </Form.Item>

          <Form.Item style={{ marginBottom: 16 }}>
            <Button
              type="primary"
              htmlType="submit"
              loading={loading}
              block
              style={{ height: 44, fontSize: 16 }}
            >
              登 录
            </Button>
          </Form.Item>
        </Form>

        <Divider style={{ margin: '16 0 12' }} />

        <div style={{ textAlign: 'center', marginBottom: 12 }}>
          <Text type="secondary" style={{ fontSize: 13 }}>
            没有账号？{' '}
            <Link to="/register" style={{ fontWeight: 500 }}>
              立即注册
            </Link>
          </Text>
        </div>

        <Divider style={{ margin: '12 0 12' }} />

        <div style={{ textAlign: 'center', marginBottom: 12 }}>
          <Link to="/trace" style={{ fontWeight: 500, color: '#1890ff' }}>
            消费者溯源查询（无需登录）
          </Link>
        </div>

        {/* 演示账号 */}
        <div style={{ background: '#fafafa', borderRadius: 6, padding: '12 16' }}>
          <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 6 }}>
            演示账号（密码：对应值）
          </Text>
          <Space direction="vertical" size={2} style={{ width: '100%' }}>
            {[
              { user: 'admin', pwd: 'admin', role: '管理员' },
              { user: 'farm001', pwd: '123456', role: '养殖场' },
              { user: 'process001', pwd: '123456', role: '加工厂' },
              { user: 'sales001', pwd: '123456', role: '销售商' },
            ].map(d => (
              <div key={d.user} style={{ display: 'flex', justifyContent: 'space-between', fontSize: 12 }}>
                <Text type="secondary">{d.user} / {d.pwd}</Text>
                <Text type="secondary">{d.role}</Text>
              </div>
            ))}
          </Space>
        </div>
      </Card>
    </div>
  )
}
