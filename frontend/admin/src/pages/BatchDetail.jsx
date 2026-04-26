import React, { useState, useEffect } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Card, Descriptions, Button, Spin, message, Tag, Row, Col,
  Steps, Progress, Divider, Popconfirm, Space, Table, Tabs, Modal, Form, Select, Input
} from 'antd'
import {
  ArrowLeftOutlined, ReloadOutlined, ThunderboltOutlined,
  ShopOutlined, CarOutlined, CheckCircleOutlined, SafetyCertificateOutlined,
  PlusOutlined, DeleteOutlined, DollarOutlined, UndoOutlined
} from '@ant-design/icons'
import batchApi from '../api/batch'
import byProductApi from '../api/byproduct'
import feedApi from '../api/feed'
import vetApi from '../api/vet'
import growthApi from '../api/growth'
import userApi from '../api/user'
import { useAuth, getEffectiveFarmId } from '../store/auth'
import CreateByProductModal from '../components/CreateByProductModal'
import CreateFeedModal from '../components/CreateFeedModal'
import CreateVetModal from '../components/CreateVetModal'
import CreateGrowthModal from '../components/CreateGrowthModal'

const STATUS_LIST = [
  { status: 0, text: '在养',        color: 'blue',    icon: <ThunderboltOutlined />, desc: '养殖群已进入养殖阶段' },
  { status: 1, text: '出栏',        color: 'green',   icon: <ShopOutlined />,        desc: '养殖群已达到出栏标准，送往加工厂' },
  { status: 2, text: '加工中',      color: 'orange',  icon: <CarOutlined />,         desc: '养殖群正在接受加工处理' },
  { status: 3, text: '加工完成',    color: 'purple',  icon: <CheckCircleOutlined />, desc: '养殖群已完成加工，等待送至零售商' },
  { status: 4, text: '送至零售商',  color: 'cyan',    icon: <ShopOutlined />,        desc: '已送往零售商，等待上架' },
  { status: 5, text: '上架',        color: 'gold',    icon: <DollarOutlined />,      desc: '商品已上架，等待销售' },
  { status: 6, text: '已销售',      color: 'default', icon: <DollarOutlined />,      desc: '商品已完成销售，流程结束' },
]
const STATUS_TEXT = {
  0: '在养', 1: '出栏', 2: '加工中', 3: '加工完成', 4: '送至零售商', 5: '上架', 6: '已销售'
}

const PRODUCT_TYPE_MAP = {
  EGG: { text: '蛋类', color: '#fa8c16' },
  WOOL: { text: '毛类', color: '#52c41a' },
  MILK: { text: '奶类', color: '#1890ff' },
  MEAT: { text: '肉类', color: '#ff4d4f' },
  OTHER: { text: '其他', color: '#722ed1' },
}

const FEED_TYPE_MAP = {
  CORN: { text: '玉米', color: '#fa8c16' },
  SOYBEAN: { text: '豆粕', color: '#52c41a' },
  WHEAT: { text: '小麦', color: '#d48806' },
  FORMULA: { text: '配合饲料', color: '#1890ff' },
  GREEN: { text: '青绿饲料', color: '#73d13d' },
  OTHER: { text: '其他', color: '#722ed1' },
}

const VET_TYPE_MAP = {
  0: { text: '免疫', color: '#1890ff' },
  1: { text: '用药', color: '#fa8c16' },
  2: { text: '治疗', color: '#ff4d4f' },
}

const HEALTH_MAP = {
  HEALTHY: { text: '健康', color: 'green' },
  NORMAL: { text: '正常', color: 'blue' },
  SICK: { text: '患病', color: 'red' },
  WEAK: { text: '弱雏', color: 'orange' },
}

function BatchDetail() {
  const { id } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const [loading, setLoading] = useState(false)
  const [updating, setUpdating] = useState(false)
  const [batch, setBatch] = useState(null)

  // 各类记录数据
  const [byProducts, setByProducts] = useState([])
  const [feedRecords, setFeedRecords] = useState([])
  const [vetRecords, setVetRecords] = useState([])
  const [growthRecords, setGrowthRecords] = useState([])

  // 弹窗状态
  const [byProductModalVisible, setByProductModalVisible] = useState(false)
  const [feedModalVisible, setFeedModalVisible] = useState(false)
  const [vetModalVisible, setVetModalVisible] = useState(false)
  const [growthModalVisible, setGrowthModalVisible] = useState(false)

  // 选择目标弹窗状态
  const [selectModalVisible, setSelectModalVisible] = useState(false)
  const [selectTarget, setSelectTarget] = useState({ type: null, batchId: null, nextStatus: null })
  const [processList, setProcessList] = useState([])
  const [salesList, setSalesList] = useState([])
  const [selectedTargetId, setSelectedTargetId] = useState(null)
  const [selectLoading, setSelectLoading] = useState(false)

  // 加载状态
  const [recordsLoading, setRecordsLoading] = useState(false)

  // 权限判断
  const userRole = user?.role
  const isAdmin = userRole === 'ADMIN'
  const isFarm = userRole === 'FARM'
  const isProcess = userRole === 'PROCESS'
  const isSales = userRole === 'SALES'

  useEffect(() => { loadBatch() }, [id])

  // 加载加工厂和销售商列表
  useEffect(() => {
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
    loadTargetLists()
  }, [])

  const loadBatch = async () => {
    setLoading(true)
    try {
      const data = await batchApi.getById(id)
      setBatch(data.data)
      loadAllRecords(id)
    } catch (error) {
      message.error('加载失败: ' + error.message)
    } finally {
      setLoading(false)
    }
  }

  const loadAllRecords = async (groupId) => {
    setRecordsLoading(true)
    try {
      const [bp, feed, vet, growth] = await Promise.all([
        byProductApi.listByGroupId(groupId).catch(() => ({ list: [] })),
        feedApi.listByGroupId(groupId).catch(() => ({ list: [] })),
        vetApi.listByGroupId(groupId).catch(() => ({ list: [] })),
        growthApi.listByGroupId(groupId).catch(() => ({ list: [] })),
      ])
      setByProducts(bp.list || [])
      setFeedRecords(feed.list || [])
      setVetRecords(vet.list || [])
      setGrowthRecords(growth.list || [])
    } catch (error) {
      console.error('加载记录失败', error)
    } finally {
      setRecordsLoading(false)
    }
  }

  const handleUpdateStatus = async (newStatus) => {
    // 出栏(1)和送至零售商(4)需要选择目标
    if (newStatus === 1 || newStatus === 4) {
      setSelectTarget({ type: newStatus === 1 ? 'process' : 'sales', batchId: id, nextStatus: newStatus })
      setSelectedTargetId(null)
      setSelectModalVisible(true)
      return
    }

    setUpdating(true)
    try {
      await batchApi.updateStatus(id, newStatus)
      message.success('状态已更新为「' + STATUS_TEXT[newStatus] + '」')
      loadBatch()
    } catch (error) {
      message.error('更新失败: ' + error.message)
    } finally {
      setUpdating(false)
    }
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
      loadBatch()
    } catch (err) {
      message.error(err.message || '操作失败')
    } finally {
      setSelectLoading(false)
    }
  }

  // 撤回/返回上一步（仅管理员可操作）
  const handleRollback = async (targetStatus) => {
    setUpdating(true)
    try {
      await batchApi.updateStatus(id, targetStatus)
      message.success('已撤回至「' + STATUS_TEXT[targetStatus] + '」')
      loadBatch()
    } catch (error) {
      message.error('撤回失败: ' + error.message)
    } finally {
      setUpdating(false)
    }
  }

  // 根据当前状态和权限判断可以执行的操作
  const getAvailableActions = () => {
    const currentStatus = batch.status ?? 0
    const actions = []
    const farmBizId = getEffectiveFarmId(user)
    const isOwnBatch = farmBizId && batch.farmId === farmBizId

    // ========== 养殖场操作 ==========
    // 出栏 (status=0→1)：仅自己养殖场的批次可操作（管理员可操作任何自有的），需指定加工厂
    if ((isFarm && isOwnBatch) || (isAdmin && isOwnBatch)) {
      actions.push({ key: 'slaughter', text: '出栏', icon: <ShopOutlined />, color: '#52c41a' })
    }

    // ========== 加工厂操作（后端已保证只能看到送达给自己的批次） ==========
    // 送至加工 (status=1→2)：加工厂或管理员可操作
    if ((isProcess || isAdmin) && currentStatus === 1) {
      actions.push({ key: 'processing', text: '送至加工', icon: <CarOutlined />, color: '#fa8c16' })
    }

    // 加工完成 (status=2→3)：加工厂或管理员可操作
    if ((isProcess || isAdmin) && currentStatus === 2) {
      actions.push({ key: 'processed', text: '加工完成', icon: <CheckCircleOutlined />, color: '#722ed1' })
    }

    // 送至零售商 (status=3→4)：加工厂或管理员可操作，需指定零售商
    if ((isProcess || isAdmin) && currentStatus === 3) {
      actions.push({ key: 'toRetailer', text: '送至零售商', icon: <ShopOutlined />, color: '#13c2c2' })
    }

    // ========== 零售商操作（后端已保证只能看到送达给自己的批次） ==========
    // 上架 (status=4→5)：零售商或管理员可操作
    if ((isSales || isAdmin) && currentStatus === 4) {
      actions.push({ key: 'onShelf', text: '上架', icon: <DollarOutlined />, color: '#faad14' })
    }

    // 销售完成 (status=5→6)：零售商或管理员可操作
    if ((isSales || isAdmin) && currentStatus === 5) {
      actions.push({ key: 'sold', text: '销售完成', icon: <DollarOutlined />, color: '#1890ff' })
    }

    // ========== 管理员撤回操作 ==========
    // 只有管理员可以撤回（返回上一步），非管理员不可撤回
    if (isAdmin) {
      const rollbackActions = {
        1: { key: 'rollback1', text: '撤回至在养', targetStatus: 0 },
        2: { key: 'rollback2', text: '撤回至出栏', targetStatus: 1 },
        3: { key: 'rollback3', text: '撤回至加工中', targetStatus: 2 },
        4: { key: 'rollback4', text: '撤回至加工完成', targetStatus: 3 },
        5: { key: 'rollback5', text: '撤回至送至零售商', targetStatus: 4 },
        6: { key: 'rollback6', text: '撤回至上架', targetStatus: 5 },
      }
      const rollback = rollbackActions[currentStatus]
      if (rollback) {
        actions.push({
          key: rollback.key,
          text: rollback.text,
          icon: <UndoOutlined />,
          color: '#ff4d4f',
          isRollback: true,
          targetStatus: rollback.targetStatus
        })
      }
    }

    return actions
  }

  // 作废记录
  const handleVoidRecord = async (type, recordId) => {
    try {
      if (type === 'feed') await feedApi.voidRecord(recordId)
      else if (type === 'vet') await vetApi.voidRecord(recordId)
      else if (type === 'growth') await growthApi.voidRecord(recordId)
      else if (type === 'byproduct') {
        await byProductApi.updateStatus(recordId, -1)
      }
      message.success('记录已作废')
      loadAllRecords(id)
    } catch (error) {
      message.error('操作失败: ' + error.message)
    }
  }

  if (loading) {
    return <Spin size="large" style={{ display: 'block', textAlign: 'center', marginTop: 100 }} />
  }

  if (!batch) {
    return (
      <Card>
        <p style={{ marginBottom: 16 }}>未加载到养殖群信息，可能不存在或网络异常。</p>
        <Button type="primary" icon={<ArrowLeftOutlined />} onClick={() => navigate('/batch')}>返回列表</Button>
      </Card>
    )
  }

  const currentStatus = batch.status ?? 0
  const currentStep = STATUS_LIST.findIndex(s => s.status === currentStatus)

  // 副产品列
  const byProductColumns = [
    { title: '副产品ID', dataIndex: 'productId', key: 'productId', render: id => <b style={{ color: '#1890ff' }}>{id}</b> },
    { title: '类型', dataIndex: 'productType', key: 'productType', render: type => <Tag color={PRODUCT_TYPE_MAP[type]?.color}>{PRODUCT_TYPE_MAP[type]?.text || type}</Tag> },
    { title: '名称', dataIndex: 'productName', key: 'productName' },
    { title: '数量', key: 'quantity', render: (_, r) => `${r.quantity} ${r.unit}` },
    { title: '质量等级', dataIndex: 'qualityGrade', key: 'qualityGrade', render: g => g ? <Tag color={g === '优' ? 'green' : 'blue'}>{g}</Tag> : '-' },
    { title: '生产日期', dataIndex: 'productionDate', key: 'productionDate', render: ts => ts ? new Date(ts * 1000).toLocaleDateString() : '-' },
    { title: '状态', dataIndex: 'status', key: 'status', render: s => s === 1 ? <Tag color="red">已作废</Tag> : <Tag color="blue">正常</Tag> },
    {
      title: '操作',
      key: 'action',
      render: (_, r) => r.status !== 1 && (
        <Button type="link" danger size="small" icon={<DeleteOutlined />} onClick={() => handleVoidRecord('byproduct', r.productId)}>
          作废
        </Button>
      ),
    },
  ]

  // 饲料记录列
  const feedColumns = [
    { title: '记录ID', dataIndex: 'recordId', key: 'recordId', render: id => <b style={{ color: '#1890ff' }}>{id}</b> },
    { title: '饲料类型', dataIndex: 'feedType', key: 'feedType', render: type => <Tag color={FEED_TYPE_MAP[type]?.color}>{FEED_TYPE_MAP[type]?.text || type}</Tag> },
    { title: '品牌', dataIndex: 'feedBrand', key: 'feedBrand' },
    { title: '投喂量', key: 'amount', render: (_, r) => `${r.amount} kg` },
    { title: '投喂日期', dataIndex: 'feedDate', key: 'feedDate', render: ts => ts ? new Date(ts * 1000).toLocaleDateString() : '-' },
    { title: '操作人', dataIndex: 'operator', key: 'operator' },
    { title: '状态', dataIndex: 'status', key: 'status', render: s => s === 1 ? <Tag color="red">已作废</Tag> : <Tag color="blue">正常</Tag> },
    {
      title: '操作',
      key: 'action',
      render: (_, r) => r.status !== 1 && (
        <Button type="link" danger size="small" icon={<DeleteOutlined />} onClick={() => handleVoidRecord('feed', r.recordId)}>
          作废
        </Button>
      ),
    },
  ]

  // 兽医记录列
  const vetColumns = [
    { title: '记录ID', dataIndex: 'recordId', key: 'recordId', render: id => <b style={{ color: '#1890ff' }}>{id}</b> },
    { title: '类型', dataIndex: 'recordType', key: 'recordType', render: type => <Tag color={VET_TYPE_MAP[type]?.color}>{VET_TYPE_MAP[type]?.text || type}</Tag> },
    { title: '疫苗/药品名', dataIndex: 'medicineName', key: 'medicineName' },
    { title: '剂量', key: 'dosage', render: (_, r) => r.dosage ? `${r.dosage} ${r.dosageUnit || ''}` : '-' },
    { title: '兽医', dataIndex: 'vetName', key: 'vetName' },
    { title: '操作日期', dataIndex: 'operationDate', key: 'operationDate', render: ts => ts ? new Date(ts * 1000).toLocaleDateString() : '-' },
    { title: '状态', dataIndex: 'status', key: 'status', render: s => s === 1 ? <Tag color="red">已作废</Tag> : <Tag color="blue">正常</Tag> },
    {
      title: '操作',
      key: 'action',
      render: (_, r) => r.status !== 1 && (
        <Button type="link" danger size="small" icon={<DeleteOutlined />} onClick={() => handleVoidRecord('vet', r.recordId)}>
          作废
        </Button>
      ),
    },
  ]

  // 成长记录列
  const growthColumns = [
    { title: '记录ID', dataIndex: 'recordId', key: 'recordId', render: id => <b style={{ color: '#1890ff' }}>{id}</b> },
    { title: '记录日期', dataIndex: 'recordDate', key: 'recordDate', render: ts => ts ? new Date(ts * 1000).toLocaleDateString() : '-' },
    { title: '发育阶段', dataIndex: 'growthStage', key: 'growthStage', render: s => s },
    { title: '平均体重', dataIndex: 'avgWeight', key: 'avgWeight', render: w => w ? `${w} kg` : '-' },
    { title: '健康状态', dataIndex: 'healthStatus', key: 'healthStatus', render: s => <Tag color={HEALTH_MAP[s]?.color}>{HEALTH_MAP[s]?.text || s}</Tag> },
    { title: '存活数', dataIndex: 'survivalCount', key: 'survivalCount', render: n => n ?? '-' },
    { title: '检测员', dataIndex: 'inspector', key: 'inspector' },
    { title: '状态', dataIndex: 'status', key: 'status', render: s => s === 1 ? <Tag color="red">已作废</Tag> : <Tag color="blue">正常</Tag> },
    {
      title: '操作',
      key: 'action',
      render: (_, r) => r.status !== 1 && (
        <Button type="link" danger size="small" icon={<DeleteOutlined />} onClick={() => handleVoidRecord('growth', r.recordId)}>
          作废
        </Button>
      ),
    },
  ]

  const tablePagination = { pageSize: 5, showSizeChanger: false, showTotal: total => `共 ${total} 条` }

  const tabItems = [
    {
      key: 'byproduct',
      label: '🏠 农副产品',
      children: (
        <Table columns={byProductColumns} dataSource={byProducts} rowKey="productId" loading={recordsLoading}
          pagination={tablePagination} locale={{ emptyText: '暂无副产品记录' }}
        />
      ),
    },
    {
      key: 'feed',
      label: '🌾 饲料投喂',
      children: (
        <Table columns={feedColumns} dataSource={feedRecords} rowKey="recordId" loading={recordsLoading}
          pagination={tablePagination} locale={{ emptyText: '暂无饲料记录' }}
        />
      ),
    },
    {
      key: 'vet',
      label: '💉 疫苗/兽医',
      children: (
        <Table columns={vetColumns} dataSource={vetRecords} rowKey="recordId" loading={recordsLoading}
          pagination={tablePagination} locale={{ emptyText: '暂无兽医记录' }}
        />
      ),
    },
    {
      key: 'growth',
      label: '📊 成长记录',
      children: (
        <Table columns={growthColumns} dataSource={growthRecords} rowKey="recordId" loading={recordsLoading}
          pagination={tablePagination} locale={{ emptyText: '暂无成长记录' }}
        />
      ),
    },
  ]

  return (
    <div>
      {/* 顶部操作栏 */}
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Button icon={<ArrowLeftOutlined />} onClick={() => navigate('/batch')}>返回列表</Button>
        <Button icon={<ReloadOutlined />} onClick={loadBatch} loading={loading}>刷新</Button>
      </div>

      {/* 养殖群流转进度 */}
      <Card title={<span><SafetyCertificateOutlined style={{ marginRight: 8, color: '#1890ff' }} />养殖群流转进度</span>} style={{ marginBottom: 16 }}>
        <Steps current={currentStep} items={STATUS_LIST.map(s => ({ title: s.text, icon: s.icon, description: s.desc }))} />
        <div style={{ marginTop: 8 }}>
          <Progress percent={Math.round(((currentStep + 1) / STATUS_LIST.length) * 100)} status="active" strokeColor="#1890ff" format={p => `${p}% 完成`} />
        </div>
      </Card>

      {/* 基础信息 + 溯源哈希 */}
      <Row gutter={16} style={{ marginBottom: 16 }}>
        <Col span={16}>
          <Card title="基础信息">
            <Descriptions column={2} bordered size="small">
              <Descriptions.Item label="养殖群ID"><b style={{ color: '#1890ff' }}>{batch.groupId}</b></Descriptions.Item>
              <Descriptions.Item label="养殖场ID">{batch.farmId}</Descriptions.Item>
              <Descriptions.Item label="品种">{batch.species}</Descriptions.Item>
              <Descriptions.Item label="存栏数量">{batch.quantity ? batch.quantity.toLocaleString() + ' 只' : '-'}</Descriptions.Item>
              <Descriptions.Item label="养殖区域" span={2}>{batch.location}</Descriptions.Item>
              <Descriptions.Item label="当前状态">
                <Tag color={STATUS_LIST[currentStep]?.color} icon={STATUS_LIST[currentStep]?.icon}>{STATUS_TEXT[currentStatus] || '未知'}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="创建时间">
                {batch.createdAt ? new Date(batch.createdAt * 1000).toLocaleString('zh-CN', { hour12: false }) : '-'}
              </Descriptions.Item>
            </Descriptions>
          </Card>
        </Col>
        <Col span={8}>
          <Card title="溯源哈希">
            <div style={{ background: '#f5f5f5', borderRadius: 6, padding: 12, fontFamily: 'monospace', fontSize: 11, wordBreak: 'break-all' }}>
              {batch.metaHash || '暂无哈希数据'}
            </div>
            <Divider style={{ margin: '12px 0' }} />
            <div style={{ fontSize: 12, color: '#888' }}>
              <SafetyCertificateOutlined style={{ marginRight: 4 }} />
              此哈希由 SHA-256 算法生成，存储于区块链中，确保数据不可篡改。
            </div>
          </Card>
        </Col>
      </Row>

      {/* 记录管理（Tab切换） */}
      <Card
        title="📋 养殖记录管理"
        extra={
          <Space>
            <Button type="primary" icon={<PlusOutlined />} size="small" onClick={() => setByProductModalVisible(true)}>副产品</Button>
            <Button type="primary" icon={<PlusOutlined />} size="small" onClick={() => setFeedModalVisible(true)}>饲料</Button>
            <Button type="primary" icon={<PlusOutlined />} size="small" onClick={() => setVetModalVisible(true)}>疫苗</Button>
            <Button type="primary" icon={<PlusOutlined />} size="small" onClick={() => setGrowthModalVisible(true)}>成长</Button>
          </Space>
        }
        style={{ marginBottom: 16 }}
      >
        <Tabs items={tabItems} />
      </Card>

      {/* 状态操作 - 根据权限显示可用操作 */}
      <Card title="状态管理">
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
          {getAvailableActions().map(action => (
            <Button
              key={action.key}
              type={action.isRollback ? 'default' : 'primary'}
              icon={action.icon}
              loading={updating}
              style={action.isRollback
                ? { borderColor: action.color, color: action.color }
                : { backgroundColor: action.color, borderColor: action.color }
              }
              onClick={() => {
                if (action.key === 'slaughter') handleUpdateStatus(1)
                else if (action.key === 'processing') handleUpdateStatus(2)
                else if (action.key === 'processed') handleUpdateStatus(3)
                else if (action.key === 'toRetailer') handleUpdateStatus(4)
                else if (action.key === 'onShelf') handleUpdateStatus(5)
                else if (action.key === 'sold') handleUpdateStatus(6)
                else if (action.isRollback) handleRollback(action.targetStatus)
              }}
            >
              {action.text}
            </Button>
          ))}
          {getAvailableActions().length === 0 && (
            <span style={{ color: '#999' }}>当前状态下您没有可执行的操作</span>
          )}
        </div>
        <Divider />
        <div style={{ fontSize: 13, color: '#595959' }}>
          <b>提示：</b>状态变更会生成一条新的区块链交易记录，确保全流程可溯源。
          <Tag color={STATUS_LIST[currentStep]?.color} style={{ marginLeft: 8 }}>{STATUS_TEXT[currentStatus]}</Tag>
          <span style={{ marginLeft: 16, color: '#8c8c8c' }}>只有管理员可以撤回状态</span>
        </div>
      </Card>

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
              notFoundContent={<span style={{ color: '#999' }}>暂无数据，请先在用户管理中添加加工厂/销售商</span>}
              style={{ width: '100%' }}
            />
          </Form.Item>
          {selectedTargetId && (
            <Form.Item label="确认选择">
              <span style={{ color: '#595959' }}>
                已选择ID：<b style={{ color: '#1890ff' }}>{selectedTargetId}</b>
              </span>
            </Form.Item>
          )}
        </Form>
      </Modal>

      {/* 弹窗 */}
      <CreateByProductModal visible={byProductModalVisible} onCancel={() => setByProductModalVisible(false)} onSuccess={() => { setByProductModalVisible(false); loadAllRecords(id); }} groupId={id} />
      <CreateFeedModal visible={feedModalVisible} onCancel={() => setFeedModalVisible(false)} onSuccess={() => { setFeedModalVisible(false); loadAllRecords(id); }} groupId={id} />
      <CreateVetModal visible={vetModalVisible} onCancel={() => setVetModalVisible(false)} onSuccess={() => { setVetModalVisible(false); loadAllRecords(id); }} groupId={id} />
      <CreateGrowthModal visible={growthModalVisible} onCancel={() => setGrowthModalVisible(false)} onSuccess={() => { setGrowthModalVisible(false); loadAllRecords(id); }} groupId={id} />
    </div>
  )
}

export default BatchDetail
