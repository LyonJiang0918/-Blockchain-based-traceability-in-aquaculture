import React, { useState, useEffect, useRef } from 'react'
import { Card, Input, Button, Tag, Timeline, Descriptions, Row, Col, message, Spin, Result, Typography, Collapse, Statistic, Empty, Table } from 'antd'
import { SearchOutlined, QrcodeOutlined, CheckCircleOutlined, CarOutlined, ShopOutlined, ThunderboltOutlined, DollarOutlined, SafetyOutlined, HomeOutlined, BuildOutlined, TruckOutlined } from '@ant-design/icons'
import { Html5Qrcode } from 'html5-qrcode'
import http from '../api/http'

const { Title, Text, Paragraph } = Typography

const STATUS_MAP = {
  0: { text: '在养', color: 'blue', icon: <ThunderboltOutlined /> },
  1: { text: '出栏', color: 'green', icon: <ShopOutlined /> },
  2: { text: '加工中', color: 'orange', icon: <CarOutlined /> },
  3: { text: '加工完成', color: 'purple', icon: <CheckCircleOutlined /> },
  4: { text: '送至零售商', color: 'cyan', icon: <TruckOutlined /> },
  5: { text: '上架', color: 'gold', icon: <DollarOutlined /> },
  6: { text: '已销售', color: 'default', icon: <DollarOutlined /> },
}

const TIMELINE_LABELS = [
  '在养',
  '出栏（待加工厂接收）',
  '加工中',
  '加工完成',
  '送至零售商',
  '上架',
  '销售完成',
]

function normalizeStatus(s) {
  if (s === null || s === undefined) return 0
  const n = typeof s === 'number' ? s : parseInt(String(s), 10)
  return Number.isFinite(n) ? n : 0
}

function fmtQty(q) {
  if (q === null || q === undefined) return '-'
  try {
    if (typeof q === 'object' && q !== null && typeof q.toLocaleString === 'function') {
      return q.toLocaleString()
    }
    return Number(q).toLocaleString()
  } catch {
    return String(q)
  }
}

/** 后端存的是秒级时间戳 */
function formatEpochSeconds(sec) {
  if (sec == null || sec === '') return '-'
  const n = Number(sec)
  if (!Number.isFinite(n)) return '-'
  return new Date(n * 1000).toLocaleString('zh-CN', { hour12: false })
}

/** LocalDateTime 可能为 ISO 字符串或数组 */
function formatDateTimeAny(v) {
  if (v == null || v === '') return '-'
  if (typeof v === 'string') {
    const t = Date.parse(v)
    if (!Number.isNaN(t)) return new Date(t).toLocaleString('zh-CN', { hour12: false })
    return v
  }
  if (Array.isArray(v) && v.length >= 3) {
    const y = v[0]
    const mo = v[1] || 1
    const d = v[2] || 1
    const h = v[3] ?? 0
    const mi = v[4] ?? 0
    const s = v[5] ?? 0
    return new Date(y, mo - 1, d, h, mi, s).toLocaleString('zh-CN', { hour12: false })
  }
  return String(v)
}

function formatNum(v) {
  if (v == null || v === '') return '-'
  if (typeof v === 'number') return Number.isFinite(v) ? String(v) : '-'
  try {
    const n = Number(v)
    return Number.isFinite(n) ? String(n) : String(v)
  } catch {
    return String(v)
  }
}

const PROCESS_TYPE_LABEL = {
  SLAUGHTER: '屠宰分割',
  PACKAGING: '包装',
  PROCESSING: '深加工',
}

export default function TraceQuery() {
  const [batchId, setBatchId] = useState('')
  const [loading, setLoading] = useState(false)
  const [trace, setTrace] = useState(null)
  const [error, setError] = useState(null)
  const [scanMode, setScanMode] = useState(false)
  const scannerRef = useRef(null)
  const [cameraReady, setCameraReady] = useState(false)
  const [cameraError, setCameraError] = useState(null)

  useEffect(() => {
    return () => {
      stopScanner()
    }
  }, [])

  const stopScanner = async () => {
    if (scannerRef.current) {
      try {
        await scannerRef.current.stop()
        scannerRef.current = null
      } catch (e) {
        // ignore
      }
    }
    setScanMode(false)
    setCameraReady(false)
  }

  const startScanner = async () => {
    setCameraError(null)
    try {
      const scanner = new Html5Qrcode('qr-reader')
      scannerRef.current = scanner
      await scanner.start(
        { facingMode: 'environment' },
        {
          fps: 10,
          qrbox: { width: 250, height: 250 },
        },
        (decodedText) => {
          setBatchId(decodedText)
          message.success('扫码成功')
          stopScanner()
          handleSearch(decodedText)
        },
        () => {}
      )
      setCameraReady(true)
    } catch (err) {
      setCameraError('无法访问摄像头，请检查权限设置或手动输入ID')
      setCameraReady(false)
    }
  }

  const handleSearch = async (maybeId) => {
    const raw =
      typeof maybeId === 'string' && maybeId.trim() !== ''
        ? maybeId
        : typeof batchId === 'string'
          ? batchId
          : String(batchId ?? '')
    const targetId = raw.trim().replace(/\uFEFF/g, '')
    if (!targetId) {
      message.warning('请输入养殖群ID')
      return
    }
    setLoading(true)
    setError(null)
    setTrace(null)
    try {
      const res = await http.get(`/trace/${encodeURIComponent(targetId)}`)
      if (res.success && res.data) {
        setTrace(res.data)
      } else {
        setError(res.message || '未找到该养殖群信息')
      }
    } catch (err) {
      setError(err.message || '查询失败，请确认养殖群ID是否正确')
    } finally {
      setLoading(false)
    }
  }

  const handleKeyPress = (e) => {
    if (e.key === 'Enter') {
      handleSearch()
    }
  }

  const batchData = trace?.batch
  const participants = trace?.participants || {}
  const deliveryRecords = trace?.deliveryRecords || []
  const feedRecords = trace?.feedRecords || []
  const vetRecords = trace?.vetRecords || []
  const growthRecords = trace?.growthRecords || []
  const byProducts = trace?.byProducts || []
  const processRecords = trace?.processRecords || []

  const currentStatus = normalizeStatus(batchData?.status)
  const st = STATUS_MAP[currentStatus] || { text: '未知', color: 'default', icon: null }

  const renderParticipant = (key, icon, title) => {
    const p = participants[key]
    if (!p) {
      return (
        <Card size="small" style={{ height: '100%' }} styles={{ body: { padding: 16 } }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
            {icon}
            <Text strong>{title}</Text>
          </div>
          <Text type="secondary">暂无关联（流程未到该环节或未登记账号信息）</Text>
        </Card>
      )
    }
    return (
      <Card size="small" style={{ height: '100%', borderColor: '#d9f7be' }} styles={{ body: { padding: 16 } }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
          {icon}
          <Text strong>{title}</Text>
        </div>
        <Descriptions column={1} size="small" colon={false}>
          <Descriptions.Item label="名称">{p.displayName || p.username || '-'}</Descriptions.Item>
          <Descriptions.Item label="账号">{p.username || '-'}</Descriptions.Item>
          {(p.businessId || key === 'farm') && (
            <Descriptions.Item label="业务编号">{p.businessId || '—'}</Descriptions.Item>
          )}
          <Descriptions.Item label="经营地址">
            <Text>{p.address || '未填写地址，请在管理端「用户/设置」中补全'}</Text>
          </Descriptions.Item>
        </Descriptions>
      </Card>
    )
  }

  return (
    <div style={{ maxWidth: 960, margin: '0 auto', padding: '0 12px 24px' }}>
      <Card style={{ marginBottom: 24, borderRadius: 12 }}>
        <div style={{ textAlign: 'center', marginBottom: 24 }}>
          <SafetyOutlined style={{ fontSize: 48, color: '#52c41a', marginBottom: 12 }} />
          <Title level={4} style={{ margin: 0 }}>养殖业品质溯源查询</Title>
          <Paragraph type="secondary" style={{ marginBottom: 0 }}>
            输入养殖群ID或扫描二维码查询完整溯源信息（任意环节均可查询，包括在养阶段）
          </Paragraph>
        </div>

        <div style={{ display: 'flex', gap: 12, marginBottom: 16, flexWrap: 'wrap' }}>
          <Input
            size="large"
            placeholder="请输入养殖群ID，例如：GROUP20260405001"
            prefix={<SearchOutlined />}
            value={batchId}
            onChange={e => setBatchId(e.target.value)}
            onKeyPress={handleKeyPress}
            allowClear
            style={{ flex: 1, minWidth: 220 }}
          />
          <Button size="large" type="primary" icon={<SearchOutlined />} onClick={() => handleSearch()} loading={loading}>
            查询
          </Button>
        </div>

        <div style={{ textAlign: 'center', marginTop: 8 }}>
          <Button
            type={scanMode ? 'default' : 'dashed'}
            icon={<QrcodeOutlined />}
            onClick={() => {
              if (scanMode) {
                stopScanner()
              } else {
                startScanner()
              }
            }}
          >
            {scanMode ? '关闭扫码' : '扫码查询'}
          </Button>
        </div>

        {scanMode && (
          <div style={{ marginTop: 16, textAlign: 'center' }}>
            <div id="qr-reader" style={{ width: '100%', maxWidth: 300, margin: '0 auto', display: cameraReady ? 'block' : 'none' }} />
            {cameraError && (
              <Text type="danger" style={{ display: 'block', marginTop: 8 }}>{cameraError}</Text>
            )}
          </div>
        )}
      </Card>

      {loading && (
        <div style={{ textAlign: 'center', padding: 60 }}>
          <Spin size="large" />
          <div style={{ marginTop: 16 }}>正在查询溯源信息...</div>
        </div>
      )}

      {error && !loading && (
        <Result
          status="warning"
          title="未找到相关记录"
          subTitle={error}
          extra={
            <Text type="secondary">
              请确认养殖群ID正确无误，或联系相关人员获取正确的ID
            </Text>
          }
        />
      )}

      {batchData && !loading && (
        <>
          <Card title="养殖群概览" style={{ borderRadius: 12, marginBottom: 16 }}>
            <Descriptions bordered column={{ xs: 1, sm: 2 }} size="small">
              <Descriptions.Item label="养殖群ID" span={2}>
                <Text strong copyable style={{ fontSize: 16 }}>{batchData.groupId}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="养殖品种">{batchData.species || '-'}</Descriptions.Item>
              <Descriptions.Item label="品种分类">{batchData.speciesCategory || '-'}</Descriptions.Item>
              <Descriptions.Item label="存栏数量">{fmtQty(batchData.quantity)}</Descriptions.Item>
              <Descriptions.Item label="圈舍/区域">{batchData.location || '-'}</Descriptions.Item>
              <Descriptions.Item label="当前流程状态" span={2}>
                <Tag color={st.color} icon={st.icon} style={{ borderRadius: 12, fontSize: 14, padding: '4px 12px' }}>
                  {st.text}
                </Tag>
                <Text type="secondary" style={{ marginLeft: 8 }}>（链上记录不可篡改；作废批次仍可查历史）</Text>
              </Descriptions.Item>
              <Descriptions.Item label="数据指纹（哈希）" span={2}>
                <Text code copyable style={{ fontSize: 12 }}>{batchData.metaHash || '-'}</Text>
              </Descriptions.Item>
            </Descriptions>

            <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
              <Col xs={12} sm={6}><Statistic title="投喂记录" value={feedRecords.length} suffix="条" /></Col>
              <Col xs={12} sm={6}><Statistic title="防疫/兽医" value={vetRecords.length} suffix="条" /></Col>
              <Col xs={12} sm={6}><Statistic title="成长测定" value={growthRecords.length} suffix="条" /></Col>
              <Col xs={12} sm={6}><Statistic title="副产品" value={byProducts.length} suffix="条" /></Col>
            </Row>
          </Card>

          <Title level={5} style={{ marginBottom: 12 }}>参与方与经营地址</Title>
          <Paragraph type="secondary" style={{ marginTop: -8, marginBottom: 12 }}>
            以下为系统登记的养殖场、加工厂、销售商账号信息；地址取自各账号档案中的「经营地址」字段（与后台用户管理一致）。
          </Paragraph>
          <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
            <Col xs={24} md={8}>
              {renderParticipant('farm', <HomeOutlined style={{ color: '#52c41a' }} />, '养殖场')}
            </Col>
            <Col xs={24} md={8}>
              {renderParticipant('process', <BuildOutlined style={{ color: '#fa8c16' }} />, '加工厂')}
            </Col>
            <Col xs={24} md={8}>
              {renderParticipant('sales', <ShopOutlined style={{ color: '#1890ff' }} />, '销售商')}
            </Col>
          </Row>

          <Card title="全流程时间线" style={{ borderRadius: 12, marginBottom: 16 }}>
            <Timeline
              items={TIMELINE_LABELS.map((text, idx) => {
                const done = idx <= currentStatus
                return {
                  color: done ? 'green' : 'gray',
                  dot: done ? <CheckCircleOutlined /> : undefined,
                  children: (
                    <span style={{ color: done ? '#262626' : '#bfbfbf' }}>
                      {text}
                      {idx === currentStatus && '（当前状态）'}
                    </span>
                  ),
                }
              })}
            />
          </Card>

          {deliveryRecords.length > 0 && (
            <Card title="流转与送达关系" style={{ borderRadius: 12, marginBottom: 16 }}>
              {deliveryRecords.map((r, i) => (
                <div key={i} style={{ marginBottom: i < deliveryRecords.length - 1 ? 12 : 0 }}>
                  <Text>
                    阶段 {r.stage === 1 ? '出栏→加工厂' : r.stage === 2 ? '加工→零售' : r.stage}
                    ：{r.fromRole}「{r.fromId}」→ {r.toRole}「{r.toId}」
                    {r.status === 0 ? '（待处理）' : r.status === 1 ? '（已完成）' : ''}
                  </Text>
                </div>
              ))}
            </Card>
          )}

          <Collapse
            style={{ marginBottom: 24 }}
            items={[
              {
                key: 'feed',
                label: `投喂记录（${feedRecords.length}）`,
                children: feedRecords.length ? (
                  <Table
                    size="small"
                    pagination={false}
                    rowKey={(r, i) => r.id ?? r.recordId ?? `feed-${i}`}
                    dataSource={feedRecords}
                    scroll={{ x: 'max-content' }}
                    columns={[
                      { title: '饲料类型', dataIndex: 'feedTypeText', render: v => v || '-' },
                      { title: '品牌', dataIndex: 'feedBrand', render: v => v || '-' },
                      { title: '投喂日期', dataIndex: 'feedDate', render: formatEpochSeconds },
                      { title: '用量', render: (_, r) => (r.amount != null ? `${formatNum(r.amount)}` : '-') },
                      { title: '方式', dataIndex: 'feedingMethodText', render: v => v || '-' },
                      { title: '操作人', dataIndex: 'operator', render: v => v || '-' },
                      { title: '状态', dataIndex: 'statusText', render: v => v || '-' },
                      { title: '备注', dataIndex: 'description', ellipsis: true, render: v => v || '-' },
                    ]}
                  />
                ) : <Empty description="暂无投喂记录" />,
              },
              {
                key: 'vet',
                label: `防疫 / 兽医记录（${vetRecords.length}）`,
                children: vetRecords.length ? (
                  <Table
                    size="small"
                    pagination={false}
                    rowKey={(r, i) => r.id ?? r.recordId ?? `vet-${i}`}
                    dataSource={vetRecords}
                    scroll={{ x: 'max-content' }}
                    columns={[
                      { title: '类型', dataIndex: 'recordTypeText', width: 80, render: v => v || '-' },
                      { title: '药品/疫苗', dataIndex: 'medicineName', render: v => v || '-' },
                      { title: '疫苗种类', dataIndex: 'vaccineTypeText', render: v => v || '-' },
                      { title: '操作日期', dataIndex: 'operationDate', width: 170, render: formatEpochSeconds },
                      { title: '兽医', dataIndex: 'vetName', render: v => v || '-' },
                      { title: '厂家', dataIndex: 'manufacturer', ellipsis: true, render: v => v || '-' },
                      { title: '用量', render: (_, r) => (r.dosage != null ? `${formatNum(r.dosage)}${r.dosageUnit || ''}` : '-') },
                      { title: '状态', dataIndex: 'statusText', width: 80, render: v => v || '-' },
                      { title: '备注', dataIndex: 'description', ellipsis: true, render: v => v || '-' },
                      {
                        title: '链上哈希',
                        width: 120,
                        render: (_, r) => {
                          const h = r.txHash || r.metaHash
                          return h ? <Text copyable={{ text: h }} style={{ fontSize: 12 }}>复制哈希</Text> : '-'
                        },
                      },
                    ]}
                  />
                ) : <Empty description="暂无防疫/兽医记录" />,
              },
              {
                key: 'growth',
                label: `成长测定（${growthRecords.length}）`,
                children: growthRecords.length ? (
                  <Table
                    size="small"
                    pagination={false}
                    rowKey={(r, i) => r.id ?? r.recordId ?? `growth-${i}`}
                    dataSource={growthRecords}
                    scroll={{ x: 'max-content' }}
                    columns={[
                      { title: '测定日期', dataIndex: 'recordDate', width: 170, render: formatEpochSeconds },
                      { title: '均重(kg)', dataIndex: 'avgWeight', render: formatNum },
                      { title: '健康', dataIndex: 'healthStatusText', render: v => v || '-' },
                      { title: '存栏', dataIndex: 'survivalCount', render: formatNum },
                      { title: '死亡', dataIndex: 'deathCount', render: formatNum },
                      { title: '阶段', dataIndex: 'growthStageText', render: v => v || '-' },
                      { title: '巡检人', dataIndex: 'inspector', render: v => v || '-' },
                      { title: '状态', dataIndex: 'statusText', render: v => v || '-' },
                      { title: '备注', dataIndex: 'description', ellipsis: true, render: v => v || '-' },
                    ]}
                  />
                ) : <Empty description="暂无成长记录" />,
              },
              {
                key: 'process',
                label: `加工记录（${processRecords.length}）`,
                children: processRecords.length ? (
                  <Table
                    size="small"
                    pagination={false}
                    rowKey={(r, i) => r.id ?? r.recordId ?? `proc-${i}`}
                    dataSource={processRecords}
                    scroll={{ x: 'max-content' }}
                    columns={[
                      { title: '记录编号', dataIndex: 'recordId', ellipsis: true, render: v => v || '-' },
                      {
                        title: '加工类型',
                        dataIndex: 'processType',
                        render: v => PROCESS_TYPE_LABEL[v] || v || '-',
                      },
                      { title: '加工厂编号', dataIndex: 'processFactoryId', render: v => v || '-' },
                      { title: '投入数量', dataIndex: 'inputCount', render: formatNum },
                      { title: '产出数量', dataIndex: 'outputCount', render: formatNum },
                      { title: '操作员', dataIndex: 'operator', render: v => v || '-' },
                      { title: '开始时间', dataIndex: 'processStartTime', width: 170, render: formatDateTimeAny },
                      { title: '完成时间', dataIndex: 'processEndTime', width: 170, render: formatDateTimeAny },
                      {
                        title: '状态',
                        dataIndex: 'status',
                        width: 90,
                        render: s => (s === 1 ? '已完成' : s === 0 ? '加工中' : formatNum(s)),
                      },
                    ]}
                  />
                ) : <Empty description="暂无加工记录" />,
              },
              {
                key: 'byproduct',
                label: `副产品（${byProducts.length}）`,
                children: byProducts.length ? (
                  <Table
                    size="small"
                    pagination={false}
                    rowKey={(r, i) => r.id ?? r.productId ?? `bp-${i}`}
                    dataSource={byProducts}
                    scroll={{ x: 'max-content' }}
                    columns={[
                      { title: '类型', dataIndex: 'productTypeText', render: v => v || '-' },
                      { title: '名称', dataIndex: 'productName', render: v => v || '-' },
                      { title: '数量', render: (_, r) => (r.quantity != null ? `${formatNum(r.quantity)}${r.unit || ''}` : '-') },
                      { title: '生产日期', dataIndex: 'productionDate', render: formatEpochSeconds },
                      { title: '批次号', dataIndex: 'productionBatch', ellipsis: true, render: v => v || '-' },
                      { title: '等级', dataIndex: 'qualityGrade', render: v => v || '-' },
                      { title: '状态', dataIndex: 'statusText', render: v => v || '-' },
                      { title: '备注', dataIndex: 'description', ellipsis: true, render: v => v || '-' },
                    ]}
                  />
                ) : <Empty description="暂无副产品记录" />,
              },
            ]}
          />
        </>
      )}
    </div>
  )
}
