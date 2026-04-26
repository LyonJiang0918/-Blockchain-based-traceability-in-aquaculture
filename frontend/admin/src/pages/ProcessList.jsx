import React, { useState, useEffect } from 'react'
import { Table, Button, Space, message, Tag, Card, Row, Col, Statistic, Modal, Form, InputNumber, Select, Input } from 'antd'
import { CarOutlined, CheckCircleOutlined, ReloadOutlined, PlusOutlined } from '@ant-design/icons'
import { useAuth } from '../store/auth'
import http from '../api/http'

const PROCESS_TYPE_MAP = {
  SLAUGHTER: { text: '屠宰分割', color: '#fa8c16' },
  PACKAGING: { text: '包装', color: '#1890ff' },
  PROCESSING: { text: '深加工', color: '#722ed1' },
}

const STATUS_MAP = {
  0: { text: '加工中', color: 'orange' },
  1: { text: '已完成', color: 'green' },
}

function ProcessList() {
  const [records, setRecords] = useState([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [form] = Form.useForm()
  const { user } = useAuth()
  const userRole = user?.role

  const isAdmin = userRole === 'ADMIN'
  const isProcess = userRole === 'PROCESS'

  const loadRecords = async () => {
    setLoading(true)
    try {
      const res = await http.get('/process/processing')
      if (res.success) {
        setRecords(res.list || [])
      }
    } catch (error) {
      message.error('加载失败: ' + (error.message || '未知错误'))
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (userRole === 'PROCESS' || userRole === 'ADMIN') {
      loadRecords()
    }
  }, [userRole])

  const handleStartProcess = async () => {
    try {
      const values = await form.validateFields()
      setLoading(true)
      const res = await http.post('/process', {
        ...values,
        inputCount: values.inputCount || 1,
      })
      if (res.success) {
        message.success('加工已启动')
        setModalVisible(false)
        form.resetFields()
        loadRecords()
      } else {
        message.error(res.message || '启动加工失败')
      }
    } catch (error) {
      if (!error.errorFields) {
        message.error(error.message || '启动加工失败')
      }
    } finally {
      setLoading(false)
    }
  }

  const handleComplete = async (recordId) => {
    try {
      const res = await http.put(`/process/${recordId}/complete`, {})
      if (res.success) {
        message.success('加工已完成')
        loadRecords()
      } else {
        message.error(res.message || '完成加工失败')
      }
    } catch (error) {
      message.error(error.message || '完成加工失败')
    }
  }

  const columns = [
    {
      title: '加工记录ID',
      dataIndex: 'recordId',
      key: 'recordId',
      render: id => <b style={{ color: '#1890ff' }}>{id}</b>,
    },
    {
      title: '养殖群ID',
      dataIndex: 'batchId',
      key: 'batchId',
      render: id => <b>{id}</b>,
    },
    {
      title: '加工类型',
      dataIndex: 'processType',
      key: 'processType',
      render: type => {
        const t = PROCESS_TYPE_MAP[type] || { text: type, color: 'default' }
        return <Tag color={t.color}>{t.text}</Tag>
      },
    },
    {
      title: '输入数量',
      dataIndex: 'inputCount',
      key: 'inputCount',
      render: v => v?.toLocaleString() || '-',
    },
    {
      title: '产出数量',
      dataIndex: 'outputCount',
      key: 'outputCount',
      render: v => v?.toLocaleString() || '-',
    },
    {
      title: '操作员',
      dataIndex: 'operator',
      key: 'operator',
    },
    {
      title: '开始时间',
      dataIndex: 'processStartTime',
      key: 'processStartTime',
      render: t => t ? new Date(t).toLocaleString('zh-CN', { hour12: false }) : '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: status => {
        const s = STATUS_MAP[status] || { text: '未知', color: 'default' }
        return <Tag color={s.color}>{s.text}</Tag>
      },
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        record.status === 0 ? (
          <Space>
            <Button type="link" icon={<CheckCircleOutlined />} onClick={() => handleComplete(record.recordId)} style={{ color: '#52c41a' }}>
              完成加工
            </Button>
          </Space>
        ) : (
          <Tag color="green">已完成</Tag>
        )
      ),
    },
  ]

  if (!isAdmin && !isProcess) {
    return (
      <div style={{ textAlign: 'center', padding: '60px 0', color: '#999' }}>
        您的角色（{userRole}）没有访问此页面的权限。
      </div>
    )
  }

  const processingCount = records.filter(r => r.status === 0).length
  const completedCount = records.filter(r => r.status === 1).length

  return (
    <div>
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card hoverable>
            <Statistic title="加工中" value={processingCount} valueStyle={{ color: '#fa8c16' }} prefix={<CarOutlined />} />
          </Card>
        </Col>
        <Col span={6}>
          <Card hoverable>
            <Statistic title="已完成" value={completedCount} valueStyle={{ color: '#52c41a' }} prefix={<CheckCircleOutlined />} />
          </Card>
        </Col>
      </Row>

      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div />
        <Space>
          <Button icon={<ReloadOutlined />} onClick={loadRecords} loading={loading}>刷新</Button>
        </Space>
      </div>

      <Table
        columns={columns}
        dataSource={records}
        loading={loading}
        rowKey="recordId"
        pagination={{ pageSize: 10 }}
      />

      <Modal
        title="启动加工"
        open={modalVisible}
        onOk={handleStartProcess}
        onCancel={() => { setModalVisible(false); form.resetFields() }}
        confirmLoading={loading}
        okText="确认启动"
        cancelText="取消"
      >
        <Form form={form} layout="vertical">
          <Form.Item label="养殖群ID" name="batchId" rules={[{ required: true, message: '请输入养殖群ID' }]}>
            <Input placeholder="例如: GROUP20260405001" />
          </Form.Item>
          <Form.Item label="加工类型" name="processType" rules={[{ required: true, message: '请选择加工类型' }]}>
            <Select placeholder="请选择加工类型">
              <Select.Option value="SLAUGHTER">屠宰分割</Select.Option>
              <Select.Option value="PACKAGING">包装</Select.Option>
              <Select.Option value="PROCESSING">深加工</Select.Option>
            </Select>
          </Form.Item>
          <Form.Item label="输入动物数量" name="inputCount" rules={[{ required: true, message: '请输入数量' }]}>
            <InputNumber min={1} style={{ width: '100%' }} placeholder="请输入本次加工的动物数量" />
          </Form.Item>
          <Form.Item label="操作员" name="operator">
            <Input placeholder="操作员姓名" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default ProcessList
