import React, { useState, useEffect } from 'react'
import { Table, Button, Space, message, Tag, Input, Select, Card, Row, Col, Statistic, Empty, Modal, Form, Typography, Popconfirm } from 'antd'

const { Text } = Typography
import {
  PlusOutlined, EyeOutlined, ReloadOutlined, UndoOutlined,
  ThunderboltOutlined, ShopOutlined, CarOutlined, CheckCircleOutlined, DollarOutlined,
  DeleteOutlined
} from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import batchApi from '../api/batch'
import userApi from '../api/user'
import { useAuth, getEffectiveFarmId } from '../store/auth'
import CreateBatchModal from '../components/CreateBatchModal'

const STATUS_MAP = {
  0: { text: '在养',        color: 'blue',    icon: <ThunderboltOutlined /> },
  1: { text: '出栏',        color: 'green',   icon: <ShopOutlined /> },
  2: { text: '加工中',      color: 'orange',  icon: <CarOutlined /> },
  3: { text: '加工完成',    color: 'purple',  icon: <CheckCircleOutlined /> },
  4: { text: '送至零售商',  color: 'cyan',     icon: <ShopOutlined /> },
  5: { text: '上架',        color: 'gold',    icon: <DollarOutlined /> },
  6: { text: '已销售',      color: 'default', icon: <DollarOutlined /> },
}

// 品种分类映射
const CATEGORY_MAP = {
  POULTRY: { text: '禽类', color: '#fa8c16' },
  LIVESTOCK: { text: '牲畜', color: '#52c41a' },
  AQUATIC: { text: '水产', color: '#1890ff' },
  OTHER: { text: '其他', color: '#722ed1' },
}

function BatchList() {
  const [batches, setBatches] = useState([])
  const [loading, setLoading] = useState(false)
  const [modalVisible, setModalVisible] = useState(false)
  const [searchText, setSearchText] = useState('')
  const [filterStatus, setFilterStatus] = useState(null)
  const [userRole, setUserRole] = useState(null)
  const navigate = useNavigate()
  const { user } = useAuth()

  // 选择加工厂/销售商的弹窗状态
  const [selectModalVisible, setSelectModalVisible] = useState(false)
  const [selectTarget, setSelectTarget] = useState({ type: null, batchId: null, nextStatus: null })
  const [processList, setProcessList] = useState([])
  const [salesList, setSalesList] = useState([])
  const [selectedTargetId, setSelectedTargetId] = useState(null)
  const [selectLoading, setSelectLoading] = useState(false)

  // 加载加工厂和销售商列表
  const loadTargetLists = async () => {
    try {
      const [processRes, salesRes] = await Promise.all([
        userApi.getByRole('PROCESS'),
        userApi.getByRole('SALES')
      ])
      setProcessList(processRes.list || [])
      setSalesList(salesRes.list || [])
    } catch (error) {
      console.error('加载目标列表失败', error)
    }
  }

  useEffect(() => {
    loadTargetLists()
  }, [])

  const loadBatches = async () => {
    setLoading(true)
    try {
      const data = await batchApi.getList(
        filterStatus === null || filterStatus === undefined
          ? {}
          : { status: filterStatus }
      )
      setBatches(data.list || [])
      if (data.userRole) setUserRole(data.userRole)
    } catch (error) {
      message.error('加载失败: ' + error.message)
    } finally {
      setLoading(false)
    }
  }

  // 挂载时加载 + 状态筛选变化时重新加载
  useEffect(() => {
    loadBatches()
  }, [filterStatus]) // eslint-disable-line react-hooks/exhaustive-deps

  const handleCreateSuccess = () => {
    setModalVisible(false)
    loadBatches()
    message.success('批次创建成功，交易已上链')
  }

  const filteredBatches = batches.filter(b =>
    !searchText || b.groupId?.includes(searchText) || b.farmId?.includes(searchText) || b.species?.includes(searchText)
  )

  // 统计
  const totalCount = batches.length
  const statusCount = {}
  batches.forEach(b => {
    statusCount[b.status] = (statusCount[b.status] || 0) + 1
  })

  const isAdmin = userRole === 'ADMIN' || user?.role === 'ADMIN'
  const isFarm = userRole === 'FARM' || user?.role === 'FARM'
  const isProcess = userRole === 'PROCESS' || user?.role === 'PROCESS'
  const isSales = userRole === 'SALES' || user?.role === 'SALES'
  const showCreateBtn = isAdmin || isFarm

  const handleStatusChange = async (groupId, newStatus, targetId = null) => {
    try {
      await batchApi.updateStatus(groupId, newStatus, targetId)
      message.success('状态更新成功')
      loadBatches()
    } catch (err) {
      message.error(err.message || '操作失败')
    }
  }

  // 打开选择目标弹窗
  const openSelectModal = (batchId, nextStatus, type) => {
    setSelectTarget({ type, batchId, nextStatus })
    setSelectedTargetId(null)
    setSelectModalVisible(true)
  }

  // 确认选择并执行状态变更
  const confirmSelectAndUpdate = async () => {
    if (!selectedTargetId) {
      message.error('请选择目标')
      return
    }
    setSelectLoading(true)
    try {
      await batchApi.updateStatus(selectTarget.batchId, selectTarget.nextStatus, selectedTargetId)
      message.success('状态更新成功')
      setSelectModalVisible(false)
      loadBatches()
    } catch (err) {
      message.error(err.message || '操作失败')
    } finally {
      setSelectLoading(false)
    }
  }

  const getActionButtons = (record) => {
    const btns = [
      <Button key="view" type="link" icon={<EyeOutlined />} onClick={() => navigate(`/batch/${record.groupId}`)}>
        详情
      </Button>
    ]

    const currentStatus = Number(record.status)
    const farmBizId = getEffectiveFarmId(user)
    const isOwnBatch = farmBizId && record.farmId === farmBizId

    // ========== 养殖场操作 ==========
    // 出栏 (status=0→1)：仅自己养殖场的批次或管理员可操作，需指定加工厂
    if ((isAdmin && isOwnBatch) || (isFarm && isOwnBatch)) {
      btns.push(
        <Button key="slaughter" type="link" icon={<ShopOutlined />} onClick={() => openSelectModal(record.groupId, 1, 'process')} style={{ color: '#52c41a' }}>
          出栏
        </Button>
      )
    }

    // ========== 管理员操作 ==========
    // 管理员可以对任何批次操作（通过状态按钮），但不在此处重复显示养殖场按钮
    // 为清晰起见：管理员不显示"出栏"按钮（让养殖场自己操作），管理员可操作其他非自有的状态变更

    // ========== 加工厂操作 ==========
    // 送至加工 (status=1→2)：加工厂或管理员可见（后端已校验送达权限，前端只做角色判断）
    if ((isAdmin || isProcess) && currentStatus === 1) {
      btns.push(
        <Button key="processing" type="link" icon={<CarOutlined />} onClick={() => handleStatusChange(record.groupId, 2)} style={{ color: '#fa8c16' }}>
          送至加工
        </Button>
      )
    }

    // 加工完成 (status=2→3)：加工厂或管理员可见
    if ((isAdmin || isProcess) && currentStatus === 2) {
      btns.push(
        <Button key="processed" type="link" icon={<CheckCircleOutlined />} onClick={() => handleStatusChange(record.groupId, 3)} style={{ color: '#722ed1' }}>
          加工完成
        </Button>
      )
    }

    // 送至零售商 (status=3→4)：加工厂或管理员可见，需指定销售商
    if ((isAdmin || isProcess) && currentStatus === 3) {
      btns.push(
        <Button key="toRetailer" type="link" icon={<ShopOutlined />} onClick={() => openSelectModal(record.groupId, 4, 'sales')} style={{ color: '#13c2c2' }}>
          送至零售商
        </Button>
      )
    }

    // ========== 零售商操作 ==========
    // 上架 (status=4→5)：零售商或管理员可操作，不可撤回
    if ((isAdmin || isSales) && currentStatus === 4) {
      btns.push(
        <Button key="onShelf" type="link" icon={<DollarOutlined />} onClick={() => handleStatusChange(record.groupId, 5)} style={{ color: '#faad14' }}>
          上架
        </Button>
      )
    }

    // 销售完成 (status=5→6)：零售商或管理员可操作，不可撤回
    if ((isAdmin || isSales) && currentStatus === 5) {
      btns.push(
        <Button key="sold" type="link" icon={<DollarOutlined />} onClick={() => handleStatusChange(record.groupId, 6)} style={{ color: '#1890ff' }}>
          销售完成
        </Button>
      )
    }

    // ========== 管理员撤回操作 ==========
    // 只有管理员可以撤回（返回上一步），非管理员不可撤回
    // 撤回规则：只能从当前状态撤回至上一个状态
    if (isAdmin) {
      if (currentStatus === 1) { // 从出栏撤回至在养
        btns.push(
          <Button key="rollback" type="link" danger icon={<UndoOutlined />} onClick={() => handleRollback(record.groupId, 0)}>
            撤回至在养
          </Button>
        )
      }
      if (currentStatus === 2) { // 从加工中撤回至出栏
        btns.push(
          <Button key="rollback" type="link" danger icon={<UndoOutlined />} onClick={() => handleRollback(record.groupId, 1)}>
            撤回至出栏
          </Button>
        )
      }
      if (currentStatus === 3) { // 从加工完成撤回至加工中
        btns.push(
          <Button key="rollback" type="link" danger icon={<UndoOutlined />} onClick={() => handleRollback(record.groupId, 2)}>
            撤回至加工中
          </Button>
        )
      }
      if (currentStatus === 4) { // 从送至零售商撤回至加工完成
        btns.push(
          <Button key="rollback" type="link" danger icon={<UndoOutlined />} onClick={() => handleRollback(record.groupId, 3)}>
            撤回至加工完成
          </Button>
        )
      }
      if (currentStatus === 5) { // 从上架撤回至送至零售商
        btns.push(
          <Button key="rollback" type="link" danger icon={<UndoOutlined />} onClick={() => handleRollback(record.groupId, 4)}>
            撤回至送至零售商
          </Button>
        )
      }
      if (currentStatus === 6) { // 从已销售撤回至上架
        btns.push(
          <Button key="rollback" type="link" danger icon={<UndoOutlined />} onClick={() => handleRollback(record.groupId, 5)}>
            撤回至上架
          </Button>
        )
      }
      // 作废按钮（仅管理员可见，且只在非作废状态时显示）
      if (!record.invalidated) {
        btns.push(
          <Button key="invalidate" type="link" danger icon={<DeleteOutlined />} onClick={() => showInvalidateConfirm(record.groupId)}>
            作废
          </Button>
        )
      }
    }

    return btns
  }

  // 撤回/返回上一步（仅管理员可操作）
  const handleRollback = async (groupId, targetStatus) => {
    try {
      await batchApi.updateStatus(groupId, targetStatus)
      message.success('已撤回至' + STATUS_MAP[targetStatus]?.text)
      loadBatches()
    } catch (err) {
      message.error(err.message || '撤回失败')
    }
  }

  // 作废单个养殖群（仅管理员）
  const handleInvalidate = async (groupId) => {
    try {
      await batchApi.invalidate(groupId, '管理员手动作废')
      message.success('养殖群已作废（区块链数据不可删除）')
      loadBatches()
    } catch (err) {
      message.error(err.message || '作废失败')
    }
  }

  // 作废所有养殖群（仅管理员）
  const handleInvalidateAll = async () => {
    try {
      await batchApi.invalidateAll('管理员批量作废所有养殖群')
      message.success('所有养殖群已作废（区块链数据不可删除）')
      loadBatches()
    } catch (err) {
      message.error(err.message || '作废失败')
    }
  }

  // 批量删除所有养殖群（旧功能，已废弃）
  const handleDeleteAll = async () => {
    try {
      await batchApi.deleteAll()
      message.success('所有养殖群数据已删除')
      loadBatches()
    } catch (err) {
      message.error(err.message || '删除失败')
    }
  }

  // 作废按钮点击（需确认）
  const showInvalidateConfirm = (groupId) => {
    Modal.confirm({
      title: '确认作废此养殖群？',
      content: (
        <div>
          <p style={{ color: '#ff4d4f', fontWeight: 'bold' }}>区块链数据不可删除！</p>
          <p>作废后将：</p>
          <ul style={{ margin: '8px 0', paddingLeft: 20 }}>
            <li>养殖群数据仍然保存在链上</li>
            <li>该养殖群不再显示在列表中</li>
            <li>所有流转记录不可篡改</li>
          </ul>
          <p>确认要作废吗？</p>
        </div>
      ),
      okText: '确认作废',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: () => handleInvalidate(groupId)
    })
  }

  // 批量作废按钮点击（需确认）
  const showInvalidateAllConfirm = () => {
    Modal.confirm({
      title: '确认作废所有养殖群？',
      content: (
        <div>
          <p style={{ color: '#ff4d4f', fontWeight: 'bold' }}>区块链数据不可删除！</p>
          <p>批量作废后将：</p>
          <ul style={{ margin: '8px 0', paddingLeft: 20 }}>
            <li>所有养殖群数据仍然保存在链上</li>
            <li>所有养殖群不再显示在列表中</li>
            <li>所有流转记录不可篡改</li>
          </ul>
          <p>确认要全部作废吗？</p>
        </div>
      ),
      okText: '确认全部作废',
      cancelText: '取消',
      okButtonProps: { danger: true },
      onOk: handleInvalidateAll
    })
  }

  const columns = [
    {
      title: '养殖群ID',
      dataIndex: 'groupId',
      key: 'groupId',
      render: id => <b style={{ color: '#1890ff' }}>{id}</b>,
      sorter: (a, b) => (a.groupId || '').localeCompare(b.groupId || ''),
    },
    {
      title: '养殖场ID',
      dataIndex: 'farmId',
      key: 'farmId',
      filters: [...new Set(batches.map(b => b.farmId))].map(v => ({ text: v, value: v })),
      onFilter: (value, record) => record.farmId === value,
    },
    {
      title: '品种',
      dataIndex: 'species',
      key: 'species',
      render: (species, record) => {
        const cat = CATEGORY_MAP[record.speciesCategory]
        return (
          <span>
            {species}
            {cat && <Tag color={cat.color} style={{ marginLeft: 6, fontSize: 11 }}>{cat.text}</Tag>}
          </span>
        )
      },
    },
    {
      title: '数量（只）',
      dataIndex: 'quantity',
      key: 'quantity',
      sorter: (a, b) => (a.quantity || 0) - (b.quantity || 0),
      render: v => v?.toLocaleString() || '-',
    },
    {
      title: '养殖区域',
      dataIndex: 'location',
      key: 'location',
      ellipsis: true,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      filters: Object.entries(STATUS_MAP).map(([value, { text }]) => ({ text, value: Number(value) })),
      onFilter: (value, record) => record.status === value,
      render: status => {
        const s = STATUS_MAP[status] || { text: '未知', color: 'default', icon: null }
        return (
          <Tag color={s.color} icon={s.icon} style={{ borderRadius: 12 }}>
            {s.text}
          </Tag>
        )
      },
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      sorter: (a, b) => (a.createdAt || 0) - (b.createdAt || 0),
      render: ts => ts ? new Date(ts * 1000).toLocaleString('zh-CN', { hour12: false }) : '-',
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: isAdmin ? 400 : 120,
      render: (_, record) => (
        <Space size={0} wrap>
          {getActionButtons(record)}
        </Space>
      ),
    },
  ]

  return (
    <div>
      {/* 统计卡片 */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={4}>
          <Card hoverable>
            <Statistic
              title="全部批次"
              value={totalCount}
              valueStyle={{ color: '#1890ff' }}
              prefix={<ShopOutlined />}
            />
          </Card>
        </Col>
        <Col span={4}>
          <Card hoverable>
            <Statistic
              title="在养中"
              value={statusCount[0] || 0}
              valueStyle={{ color: '#1890ff' }}
              prefix={<ThunderboltOutlined />}
            />
          </Card>
        </Col>
        <Col span={4}>
          <Card hoverable>
            <Statistic
              title="出栏"
              value={statusCount[1] || 0}
              valueStyle={{ color: '#52c41a' }}
              prefix={<ShopOutlined />}
            />
          </Card>
        </Col>
        <Col span={4}>
          <Card hoverable>
            <Statistic
              title="加工中"
              value={statusCount[2] || 0}
              valueStyle={{ color: '#fa8c16' }}
              prefix={<CarOutlined />}
            />
          </Card>
        </Col>
        <Col span={4}>
          <Card hoverable>
            <Statistic
              title="加工完成"
              value={statusCount[3] || 0}
              valueStyle={{ color: '#722ed1' }}
              prefix={<CheckCircleOutlined />}
            />
          </Card>
        </Col>
        <Col span={4}>
          <Card hoverable>
            <Statistic
              title="已销售"
              value={statusCount[6] || 0}
              valueStyle={{ color: '#fa8c16' }}
              prefix={<DollarOutlined />}
            />
          </Card>
        </Col>
      </Row>

      {/* 操作栏 */}
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: 8 }}>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
          <Input.Search
            placeholder="搜索批次ID / 养殖场 / 品种"
            allowClear
            style={{ width: 280 }}
            onSearch={setSearchText}
            onChange={e => setSearchText(e.target.value)}
          />
          <Select
            placeholder="按状态筛选"
            allowClear
            style={{ width: 140 }}
            onChange={v => setFilterStatus(v)}
            options={Object.entries(STATUS_MAP).map(([value, { text }]) => ({ label: text, value: Number(value) }))}
          />
        </div>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={loadBatches} loading={loading}>刷新</Button>
          {showCreateBtn && (
            <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalVisible(true)}>
              创建养殖群
            </Button>
          )}
          {isAdmin && (
            <Button icon={<CarOutlined />} onClick={() => setFilterStatus(2)} style={{ color: '#fa8c16' }}>
              加工中
            </Button>
          )}
          {isAdmin && (
            <Button icon={<DollarOutlined />} onClick={() => setFilterStatus(5)} style={{ color: '#faad14' }}>
              待上架
            </Button>
          )}
          {isAdmin && (
            <Popconfirm
              title="确认作废所有养殖群？"
              description={
                <div>
                  <p style={{ color: '#ff4d4f', fontWeight: 'bold' }}>区块链数据不可删除！</p>
                  <p>作废后将隐藏所有养殖群，但数据仍保存在链上。</p>
                </div>
              }
              onConfirm={showInvalidateAllConfirm}
              okText="确认作废"
              cancelText="取消"
              okButtonProps={{ danger: true }}
            >
              <Button danger icon={<DeleteOutlined />}>作废所有</Button>
            </Popconfirm>
          )}
        </Space>
      </div>

      {/* 表格 */}
      <Table
        columns={columns}
        dataSource={filteredBatches}
        loading={loading}
        rowKey="groupId"
        pagination={{ pageSize: 10, showSizeChanger: true, showTotal: total => `共 ${total} 条` }}
        scroll={{ x: 1100 }}
        locale={{
          emptyText: (
            <Empty
              image={Empty.PRESENTED_IMAGE_SIMPLE}
              description={
                <span>
                  {batches.length === 0
                    ? (isProcess
                      ? '暂无送达给您的养殖群。请确认养殖场「出栏」时在弹窗中选择了您的加工厂账号（登录名），例如 process002。'
                      : isSales
                        ? '暂无送达给您的养殖群。请确认加工厂「送至零售商」时选择了您的销售商账号。'
                        : '暂无养殖群数据')
                    : '没有符合条件的数据'}
                </span>
              }
            >
              {showCreateBtn && (
                <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalVisible(true)}>
                  创建第一个养殖群
                </Button>
              )}
            </Empty>
          ),
        }}
      />

      <CreateBatchModal
        visible={modalVisible}
        onCancel={() => setModalVisible(false)}
        onCreated={loadBatches}
        onSuccess={handleCreateSuccess}
        userRole={userRole}
      />

      {/* 选择目标加工厂/销售商弹窗 */}
      <Modal
        title={selectTarget.type === 'process' ? '选择目标加工厂' : '选择目标销售商'}
        open={selectModalVisible}
        onOk={confirmSelectAndUpdate}
        onCancel={() => setSelectModalVisible(false)}
        confirmLoading={selectLoading}
        okText="确认"
        cancelText="取消"
      >
        <Form layout="vertical" style={{ marginTop: 16 }}>
          <Form.Item label="养殖群ID" required>
            <Input value={selectTarget.batchId} disabled />
          </Form.Item>
          <Form.Item label={selectTarget.type === 'process' ? '请选择目标加工厂' : '请选择目标销售商'} required>
            <Select
              placeholder="请输入名称或ID搜索..."
              value={selectedTargetId}
              onChange={setSelectedTargetId}
              showSearch
              optionFilterProp="label"
              options={
                selectTarget.type === 'process'
                  ? processList.map(p => ({ label: `${p.nickname || p.username} (${p.farmId || p.username})`, value: p.username }))
                  : salesList.map(s => ({ label: `${s.nickname || s.username} (${s.farmId || s.username})`, value: s.username }))
              }
              notFoundContent={<Text type="secondary">暂无数据，请先在用户管理中添加加工厂/销售商</Text>}
              style={{ width: '100%' }}
            />
          </Form.Item>
          {selectedTargetId && (
            <Form.Item label="确认选择">
              <Text type="secondary">
                已选择ID：<Text strong copyable>{selectedTargetId}</Text>
              </Text>
            </Form.Item>
          )}
        </Form>
      </Modal>
    </div>
  )
}

export default BatchList
