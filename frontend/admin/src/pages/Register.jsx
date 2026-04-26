import React, { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { Form, Input, Button, Card, message, Typography, Divider, Select, Space } from 'antd'
import { UserOutlined, LockOutlined, SafetyOutlined, IdcardOutlined } from '@ant-design/icons'
import userApi from '../api/user'

const { Title, Text } = Typography

const ROLE_OPTIONS = [
  { label: '养殖场', value: 'FARM' },
  { label: '加工厂', value: 'PROCESS' },
  { label: '销售商', value: 'SALES' },
]

export default function Register() {
  const [loading, setLoading] = useState(false)
  const navigate = useNavigate()
  const [form] = Form.useForm()

  const handleSubmit = async ({ username, password, confirmPassword, role, farmId, nickname, location }) => {
    if (password !== confirmPassword) {
      message.error('两次输入的密码不一致')
      return
    }
    if (password.length < 6) {
      message.error('密码长度不能少于6位')
      return
    }
    setLoading(true)
    try {
      await userApi.register({ username, password, role, farmId, nickname, location })
      message.success('注册成功，请登录')
      navigate('/login', { replace: true })
    } catch (err) {
      message.error(err.message || '注册失败')
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
          width: 420,
          borderRadius: 12,
          boxShadow: '0 8px 32px rgba(0,0,0,0.3)',
        }}
        styles={{ body: { padding: 40 } }}
      >
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <SafetyOutlined style={{ fontSize: 48, color: '#1890ff', marginBottom: 8 }} />
          <Title level={4} style={{ margin: 0, color: '#262626' }}>
            养殖业品质溯源平台
          </Title>
          <Text type="secondary" style={{ fontSize: 13 }}>
            创建新账号
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
            rules={[
              { required: true, message: '请输入用户名' },
              { min: 3, message: '用户名至少3个字符' },
              { pattern: /^[a-zA-Z0-9_]+$/, message: '仅支持字母、数字、下划线' },
            ]}
          >
            <Input
              prefix={<UserOutlined style={{ color: '#bfbfbf' }} />}
              placeholder="用户名（登录账号）"
              autoComplete="username"
              onBlur={(e) => {
                const v = e.target.value?.trim()
                if (form.getFieldValue('role') === 'FARM' && v && !form.getFieldValue('farmId')) {
                  form.setFieldValue('farmId', v)
                }
              }}
            />
          </Form.Item>

          <Form.Item
            name="nickname"
            rules={[{ required: true, message: '请输入昵称' }]}
          >
            <Input
              prefix={<IdcardOutlined style={{ color: '#bfbfbf' }} />}
              placeholder="昵称（显示名称）"
            />
          </Form.Item>

          <Form.Item
            name="role"
            rules={[{ required: true, message: '请选择角色类型' }]}
            initialValue="FARM"
          >
            <Select
              placeholder="选择角色类型"
              options={ROLE_OPTIONS}
            />
          </Form.Item>

          <Form.Item
            noStyle
            shouldUpdate={(prev, curr) => prev.role !== curr.role}
          >
            {({ getFieldValue }) =>
              getFieldValue('role') === 'FARM' ? (
                <Space direction="vertical" style={{ width: '100%' }}>
                  <Form.Item
                    name="farmId"
                    rules={[{ required: true, message: '请输入养殖场编号' }]}
                    extra="建议与登录用户名一致（如 farm002），便于系统识别您的养殖场"
                  >
                    <Input
                      prefix={<IdcardOutlined style={{ color: '#bfbfbf' }} />}
                      placeholder="例如与用户名相同：farm002"
                    />
                  </Form.Item>
                  <Form.Item
                    name="location"
                    extra="填写养殖场详细地址（如省市区+基地名称），便于溯源展示"
                  >
                    <Input
                      prefix={<IdcardOutlined style={{ color: '#bfbfbf' }} />}
                      placeholder="养殖场详细地址（选填，建议填写）"
                    />
                  </Form.Item>
                </Space>
              ) : null
            }
          </Form.Item>

          <Form.Item
            name="password"
            rules={[
              { required: true, message: '请输入密码' },
              { min: 6, message: '密码至少6位' },
            ]}
            hasFeedback
          >
            <Input.Password
              prefix={<LockOutlined style={{ color: '#bfbfbf' }} />}
              placeholder="密码（至少6位）"
              autoComplete="new-password"
            />
          </Form.Item>

          <Form.Item
            name="confirmPassword"
            dependencies={['password']}
            rules={[
              { required: true, message: '请再次输入密码' },
              ({ getFieldValue }) => ({
                validator(_, value) {
                  if (!value || getFieldValue('password') === value) {
                    return Promise.resolve()
                  }
                  return Promise.reject(new Error('两次输入的密码不一致'))
                },
              }),
            ]}
            hasFeedback
          >
            <Input.Password
              prefix={<LockOutlined style={{ color: '#bfbfbf' }} />}
              placeholder="确认密码"
              autoComplete="new-password"
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
              注 册
            </Button>
          </Form.Item>
        </Form>

        <Divider style={{ margin: '16 0 12' }} />

        <div style={{ textAlign: 'center' }}>
          <Text type="secondary" style={{ fontSize: 13 }}>
            已有账号？{' '}
            <Link to="/login" style={{ fontWeight: 500 }}>
              立即登录
            </Link>
          </Text>
        </div>
      </Card>
    </div>
  )
}
