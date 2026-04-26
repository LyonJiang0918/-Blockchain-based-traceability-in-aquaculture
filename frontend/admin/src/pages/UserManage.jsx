import React, { useState, useEffect } from 'react'
import {
  Table, Button, Space, message, Tag, Input, Card, Modal, Form,
  Select, Popconfirm, Typography, Row, Col, Statistic
} from 'antd'
import {
  PlusOutlined, ReloadOutlined, DeleteOutlined, EditOutlined,
  UserOutlined, LockOutlined, TeamOutlined
} from '@ant-design/icons'
import userApi from '../api/user'
import { ROLE_MAP } from '../store/auth'

const { Title, Text } = Typography

const roleColorMap = {
  ADMIN: 'red',
  FARM: 'blue',
  PROCESS: 'orange',
  SALES: 'green',
}

function UserManage() {
  const [users, setUsers] = useState([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [editingUser, setEditingUser] = useState(null)
  const [searchText, setSearchText] = useState('')
  const [form] = Form.useForm()

  const loadUsers = async () => {
    setLoading(true)
    try {
      const data = await userApi.list()
      setUsers(data.list || [])
    } catch (err) {
      message.error('加载用户列表失败: ' + err.message)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { loadUsers() }, [])

  const handleCreate = () => {
    setEditingUser(null)
    form.resetFields()
    setModalVisible(true)
  }

  const handleEdit = (record) => {
    setEditingUser(record)
    form.setFieldsValue({
      username: record.username,
      role: record.role,
      farmId: record.farmId,
      nickname: record.nickname,
      location: record.location,
    })
    setModalVisible(true)
  }

  const handleDelete = async (id) => {
    try {
      await userApi.delete(id)
      message.success('删除成功')
      loadUsers()
    } catch (err) {
      message.error('删除失败: ' + err.message)
    }
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      setLoading(true)
      if (editingUser) {
        await userApi.update(editingUser.id, values)
        message.success('更新成功')
        setModalVisible(false)
        loadUsers()
      } else {
        await userApi.create(values)
        message.success('创建成功')
        setModalVisible(false)
        loadUsers()
      }
    } catch (err) {
      if (err.errorFields) return
      message.error(err.message || '操作失败')
    } finally {
      setLoading(false)
    }
  }

  const filteredUsers = users.filter(u =>
    !searchText ||
    u.username?.includes(searchText) ||
    u.nickname?.includes(searchText) ||
    u.role?.includes(searchText)
  )

  const columns = [
    {
      title: '用户名',
      dataIndex: 'username',
      key: 'username',
      render: (v, r) => (
        <Space>
          <UserOutlined style={{ color: '#1890ff' }} />
          <b>{v}</b>
        </Space>
      ),
    },
    {
      title: '昵称',
      dataIndex: 'nickname',
      key: 'nickname',
      render: v => v || '-',
    },
    {
      title: '角色',
      dataIndex: 'role',
      key: 'role',
      render: role => (
        <Tag color={roleColorMap[role] || 'default'}>
          {ROLE_MAP[role] || role}
        </Tag>
      ),
    },
    {
      title: '养殖场编号',
      dataIndex: 'farmId',
      key: 'farmId',
      render: v => v || <Text type="secondary">-</Text>,
    },
    {
      title: '地理位置',
      dataIndex: 'location',
      key: 'location',
      render: v => v || <Text type="secondary">-</Text>,
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: ts => ts ? new Date(ts * 1000).toLocaleString('zh-CN', { hour12: false }) : '-',
    },
    {
      title: '最后登录',
      dataIndex: 'lastLoginAt',
      key: 'lastLoginAt',
      render: ts => ts ? new Date(ts * 1000).toLocaleString('zh-CN', { hour12: false }) : <Text type="secondary">未登录</Text>,
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space size={4}>
          {record.username !== 'admin' && (
            <>
              <Button type="link" icon={<EditOutlined />} size="small" onClick={() => handleEdit(record)}>
                编辑
              </Button>
              <Popconfirm
                title="确定删除该用户？"
                onConfirm={() => handleDelete(record.id)}
                okText="确定"
                cancelText="取消"
              >
                <Button type="link" danger icon={<DeleteOutlined />} size="small">
                  删除
                </Button>
              </Popconfirm>
            </>
          )}
        </Space>
      ),
    },
  ]

  const roleCount = {}
  users.forEach(u => { roleCount[u.role] = (roleCount[u.role] || 0) + 1 })

  return (
    <div>
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card hoverable>
            <Statistic title="用户总数" value={users.length} prefix={<TeamOutlined />} valueStyle={{ color: '#1890ff' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card hoverable>
            <Statistic title="管理员" value={roleCount['ADMIN'] || 0} prefix={<UserOutlined />} valueStyle={{ color: '#f5222d' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card hoverable>
            <Statistic title="养殖场" value={roleCount['FARM'] || 0} prefix={<TeamOutlined />} valueStyle={{ color: '#1890ff' }} />
          </Card>
        </Col>
        <Col span={6}>
          <Card hoverable>
            <Statistic title="加工/销售" value={(roleCount['PROCESS'] || 0) + (roleCount['SALES'] || 0)} prefix={<TeamOutlined />} valueStyle={{ color: '#52c41a' }} />
          </Card>
        </Col>
      </Row>

      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
        <Input.Search
          placeholder="搜索用户名 / 昵称 / 角色"
          allowClear
          style={{ width: 280 }}
          onSearch={setSearchText}
          onChange={e => setSearchText(e.target.value)}
        />
        <Space>
          <Button icon={<ReloadOutlined />} onClick={loadUsers} loading={loading}>刷新</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
            添加用户
          </Button>
        </Space>
      </div>

      <Table
        columns={columns}
        dataSource={filteredUsers}
        loading={loading}
        rowKey="id"
        pagination={{ pageSize: 10, showSizeChanger: true, showTotal: total => `共 ${total} 条` }}
      />

      <Modal
        title={editingUser ? '编辑用户' : '添加用户'}
        open={modalVisible}
        onOk={handleSubmit}
        onCancel={() => setModalVisible(false)}
        confirmLoading={loading}
        okText={editingUser ? '保存' : '创建'}
        cancelText="取消"
      >
        <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item
            name="username"
            label="用户名"
            rules={[
              { required: true, message: '请输入用户名' },
              { min: 3, message: '至少3个字符' },
              { pattern: /^[a-zA-Z0-9_]+$/, message: '仅支持字母、数字、下划线' },
            ]}
          >
            <Input prefix={<UserOutlined />} placeholder="登录账号" />
          </Form.Item>

          {!editingUser && (
            <Form.Item
              name="password"
              label="初始密码"
              rules={[
                { required: true, message: '请输入密码' },
                { min: 6, message: '至少6位' },
              ]}
            >
              <Input.Password prefix={<LockOutlined />} placeholder="初始密码（至少6位）" />
            </Form.Item>
          )}

          <Form.Item
            name="nickname"
            label="昵称"
            rules={[{ required: true, message: '请输入昵称' }]}
          >
            <Input placeholder="显示名称" />
          </Form.Item>

          <Form.Item
            name="role"
            label="角色"
            rules={[{ required: true, message: '请选择角色' }]}
          >
            <Select
              placeholder="选择角色"
              options={Object.entries(ROLE_MAP).map(([value, label]) => ({ value, label }))}
            />
          </Form.Item>

          <Form.Item
            name="farmId"
            label="养殖场编号"
            extra="仅养殖场角色需要填写"
          >
            <Input placeholder="例如: FARM001" />
          </Form.Item>

          <Form.Item
            name="location"
            label="地理位置"
            extra={
              <span style={{ color: '#8c8c8c', fontSize: 12 }}>
                {form.getFieldValue('role') === 'FARM' ? '养殖场详细地址（如省市区+基地名称）'
                  : form.getFieldValue('role') === 'PROCESS' ? '加工厂详细地址（如园区名称、楼号）'
                  : form.getFieldValue('role') === 'SALES' ? '门店或仓库地址（如商圈+门牌号）'
                  : '填写厂商所在详细地址'}
              </span>
            }
          >
            <Input placeholder={
              form.getFieldValue('role') === 'FARM' ? '例如：山东省济南市历下区养殖基地A区'
                : form.getFieldValue('role') === 'PROCESS' ? '例如：北京市丰台区食品工业园3号楼'
                : form.getFieldValue('role') === 'SALES' ? '例如：上海市浦东新区世纪大道168号'
                : '详细地址'
            } />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default UserManage