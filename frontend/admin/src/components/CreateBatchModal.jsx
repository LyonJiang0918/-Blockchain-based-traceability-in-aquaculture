import React, { useState, useEffect } from 'react'
import {
  Modal, Form, Input, InputNumber, Select, message, Space, Divider
} from 'antd'
import { InfoCircleOutlined } from '@ant-design/icons'
import batchApi from '../api/batch'
import userApi from '../api/user'
import { useAuth, getEffectiveFarmId } from '../store/auth'

function resolveFarmBusinessId(u) {
  return getEffectiveFarmId(u) || ''
}

/** 与后端/会话一致：养殖场 DTO 的业务编号 */
function farmBusinessIdFromDto(u) {
  if (!u) return ''
  const fid = typeof u.farmId === 'string' ? u.farmId.trim() : ''
  if (fid) return fid
  return typeof u.username === 'string' ? u.username.trim() : ''
}

function isFarmRole(userRole, user) {
  return userRole === 'FARM' || user?.role === 'FARM'
}

// 品种分类配置
const SPECIES_CATEGORIES = [
  { value: 'POULTRY', label: '🐔 禽类', desc: '鸡、鸭、鹅等' },
  { value: 'LIVESTOCK', label: '🐄 牲畜', desc: '牛、羊、猪等' },
  { value: 'AQUATIC', label: '🐟 水产', desc: '鱼、虾、蟹等' },
  { value: 'OTHER', label: '🐝 其他', desc: '蜜蜂、特种养殖等' },
]

// 常见品种推荐
const SPECIES_OPTIONS = {
  POULTRY: [
    { value: '白羽鸡', label: '白羽鸡' },
    { value: '黄羽鸡', label: '黄羽鸡' },
    { value: '土鸡（草鸡）', label: '土鸡（草鸡）' },
    { value: '北京油鸡', label: '北京油鸡' },
    { value: '乌鸡', label: '乌鸡' },
    { value: '麻鸭', label: '麻鸭' },
    { value: '鹅', label: '鹅' },
  ],
  LIVESTOCK: [
    { value: '荷斯坦奶牛', label: '荷斯坦奶牛' },
    { value: '西门塔尔牛', label: '西门塔尔牛' },
    { value: '小尾寒羊', label: '小尾寒羊' },
    { value: '湖羊', label: '湖羊' },
    { value: '杜泊羊', label: '杜泊羊' },
    { value: '育肥猪', label: '育肥猪' },
  ],
  AQUATIC: [
    { value: '草鱼', label: '草鱼' },
    { value: '鲤鱼', label: '鲤鱼' },
    { value: '南美白对虾', label: '南美白对虾' },
    { value: '小龙虾', label: '小龙虾' },
    { value: '大闸蟹', label: '大闸蟹' },
  ],
  OTHER: [
    { value: '蜜蜂', label: '蜜蜂' },
    { value: '蝎子', label: '蝎子' },
    { value: '鸵鸟', label: '鸵鸟' },
  ],
}

function CreateBatchModal({ visible, onCancel, onCreated, onSuccess, userRole }) {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [submitResult, setSubmitResult] = useState(null)
  const [selectedCategory, setSelectedCategory] = useState(null)
  const [farmUsers, setFarmUsers] = useState([])
  const [farmListLoading, setFarmListLoading] = useState(false)
  const { user } = useAuth()

  useEffect(() => {
    if (!visible) {
      // 当 Modal 关闭时，不再需要手动重置表单，因为 destroyOnHidden 属性会处理
      // form.resetFields() // <--- 已移除此行以修复警告
      setSubmitResult(null)
      setSelectedCategory(null)
      return
    }
    // 必须先清空表单，否则会残留上一次打开时的养殖场ID（例如先登 farm001 再登 farm002）
    form.resetFields()
    const now = new Date()
    const groupId = 'GROUP' +
      String(now.getFullYear()) +
      String(now.getMonth() + 1).padStart(2, '0') +
      String(now.getDate()).padStart(2, '0') +
      String(now.getHours()).padStart(2, '0') +
      String(now.getMinutes()).padStart(2, '0') +
      String(now.getSeconds()).padStart(2, '0')
    const initValues = {
      batchId: groupId,
      speciesCategory: 'POULTRY',
      unit: '只',
    }
    if (isFarmRole(userRole, user)) {
      initValues.farmId = resolveFarmBusinessId(user)
    }
    form.setFieldsValue(initValues)
    setSelectedCategory('POULTRY')
  }, [visible, form, userRole, user])

  useEffect(() => {
    if (!visible || isFarmRole(userRole, user)) {
      if (!visible) setFarmUsers([])
      return
    }
    setFarmListLoading(true)
    userApi
      .getByRole('FARM')
      .then((data) => {
        setFarmUsers(Array.isArray(data.list) ? data.list : [])
      })
      .catch((e) => {
        message.error('加载养殖场列表失败: ' + e.message)
        setFarmUsers([])
      })
      .finally(() => setFarmListLoading(false))
  }, [visible, userRole, user])

  const handleCategoryChange = (category) => {
    setSelectedCategory(category)
    form.setFieldsValue({ species: undefined }) // 切换分类时清空品种
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      setLoading(true)
      const farmIdForRequest = isFarmRole(userRole, user)
        ? resolveFarmBusinessId(user)
        : values.farmId

      const requestData = {
        batchId: values.batchId,
        farmId: farmIdForRequest,
        species: values.species,
        speciesCategory: values.speciesCategory,
        quantity: values.quantity,
        location: values.location,
        metaJson: JSON.stringify({
          description: values.description,
          source: values.source,
          breedType: values.breedType,
          feedingCycle: values.feedingCycle,
          weight: values.weight,
          breeder: values.breeder,
        }),
      }

      const result = await batchApi.create(requestData)
      setSubmitResult(result)
      onCreated?.()
      message.success(
        '✓ 养殖群创建成功！交易哈希：' + String(result.txHash || '').substring(0, 18) + '...',
        5
      )
    } catch (error) {
      if (error.errorFields) return
      message.error('创建失败: ' + error.message)
    } finally {
      setLoading(false)
    }
  }

  const handleOk = () => {
    if (submitResult) {
      form.resetFields()
      setSubmitResult(null)
      onSuccess()
    } else {
      handleSubmit()
    }
  }

  const currentSpeciesOptions = selectedCategory ? (SPECIES_OPTIONS[selectedCategory] || []) : []

  return (
    <Modal
      title="创建新养殖群"
      open={visible}
      onOk={handleOk}
      onCancel={onCancel}
      confirmLoading={loading}
      okText={submitResult ? '完成' : '确认创建'}
      cancelText="取消"
      width={680}
      destroyOnHidden
    >
      {submitResult ? (
        <div style={{ textAlign: 'center', padding: '24px 0' }}>
          <div style={{ fontSize: 48, marginBottom: 16 }}>
            <span role="img" aria-label="success">✅</span>
          </div>
          <h3>养殖群创建成功！</h3>
          <p style={{ color: '#666', fontSize: 13 }}>
            交易哈希（TX Hash）已生成，数据正在上链中……
          </p>
          <div style={{
            background: '#f5f5f5', borderRadius: 6, padding: 12,
            fontFamily: 'monospace', fontSize: 11, wordBreak: 'break-all',
            marginTop: 12, textAlign: 'left'
          }}>
            {submitResult.txHash}
          </div>
        </div>
      ) : null}

      <div style={{ display: submitResult ? 'none' : 'block' }}>
        {!submitResult && (
          <div style={{
            background: '#e6f7ff', border: '1px solid #91d5ff',
            borderRadius: 6, padding: '10px 14px', marginBottom: 16, fontSize: 13, color: '#1890ff'
          }}>
            <InfoCircleOutlined style={{ marginRight: 6 }} />
            养殖群代表一个养殖单元（一窝鸡 / 一圈牛羊 / 一片蜂箱），信息提交后将生成不可篡改的区块链哈希。
          </div>
        )}

        {/* 传递 form 实例以解决 useForm 警告 */}
        <Form form={form} layout="vertical" size="middle">
            <Form.Item
              label="养殖群ID"
              name="batchId"
              rules={[{ required: true, message: '请输入养殖群ID' }]}
              tooltip="系统将自动生成，也可手动输入"
            >
              <Input placeholder="GROUP20260403120000" disabled style={{ color: '#1890ff', fontWeight: 'bold' }} />
            </Form.Item>

            <Form.Item
              label="养殖场"
              name="farmId"
              rules={[{ required: true, message: '请选择已注册的养殖场' }]}
              extra={
                isFarmRole(userRole, user)
                  ? `已锁定为当前登录账号所属养殖场：${resolveFarmBusinessId(user) || '（未设置，请联系管理员）'}`
                  : '请从已注册养殖场中选择；创建后该养殖场账号可见并可操作本养殖群'
              }
            >
              {isFarmRole(userRole, user) ? (
                <Input
                  placeholder="自动填充"
                  disabled
                  style={{ color: '#1890ff', fontWeight: 'bold' }}
                />
              ) : (
                <Select
                  showSearch
                  allowClear
                  placeholder="搜索或选择已注册的养殖场"
                  loading={farmListLoading}
                  notFoundContent={farmListLoading ? '加载中…' : '暂无养殖场账号，请先在用户管理中注册'}
                  optionFilterProp="searchText"
                  filterOption={(input, option) =>
                    String(option?.searchText ?? '')
                      .toLowerCase()
                      .includes(String(input).toLowerCase())
                  }
                  options={farmUsers
                    .map((fu) => {
                      const bid = farmBusinessIdFromDto(fu)
                      if (!bid) return null
                      const title = fu.nickname || fu.username || bid
                      return {
                        value: bid,
                        label: `${title}（${bid}）`,
                        searchText: `${title} ${fu.username || ''} ${bid} ${fu.location || ''}`,
                      }
                    })
                    .filter(Boolean)}
                />
              )}
            </Form.Item>

            <Form.Item
              label="养殖类型"
              name="speciesCategory"
              rules={[{ required: true, message: '请选择养殖类型' }]}
            >
              <Select
                placeholder="请选择养殖类型"
                onChange={handleCategoryChange}
                options={SPECIES_CATEGORIES.map(c => ({
                  value: c.value,
                  label: (
                    <span>
                      {c.label}
                      <span style={{ color: '#999', fontSize: 12, marginLeft: 8 }}>{c.desc}</span>
                    </span>
                  ),
                }))}
              />
            </Form.Item>

            <Form.Item
              label="养殖品种"
              name="species"
              rules={[{ required: true, message: '请输入养殖品种' }]}
            >
              <Select
                placeholder="请先选择养殖类型，再选择具体品种"
                showSearch
                allowClear
                options={currentSpeciesOptions}
                filterOption={(input, option) =>
                  (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
                }
                popupRender={menu => (
                  <>
                    {menu}
                    <Divider style={{ margin: '8px 0' }} />
                    <div style={{ padding: '4px 8px', color: '#999', fontSize: 12 }}>
                      也可以直接输入自定义品种名称
                    </div>
                  </>
                )}
              >
                {/* 会通过 options 渲染 */}
              </Select>
            </Form.Item>

            <Form.Item
              label="品系"
              name="breedType"
              tooltip="如：AA+、Ross 308、Hubbard（可不填）"
            >
              <Input placeholder="例如: AA+" />
            </Form.Item>

            <Space style={{ display: 'flex', width: '100%' }} size={16}>
              <Form.Item
                label="存栏数量"
                name="quantity"
                rules={[{ required: true, message: '请输入数量' }]}
                style={{ flex: 1 }}
              >
                <InputNumber min={1} max={999999} style={{ width: '100%' }} placeholder="存栏数量" />
              </Form.Item>

              <Form.Item
                label="单位"
                name="unit"
                initialValue="只"
                style={{ width: 100 }}
              >
                <Select
                  options={[
                    { value: '只', label: '只' },
                    { value: '头', label: '头' },
                    { value: '羽', label: '羽' },
                    { value: '尾', label: '尾' },
                    { value: '匹', label: '匹' },
                  ]}
                />
              </Form.Item>
            </Space>

            <Form.Item
              label="养殖区域"
              name="location"
              rules={[{ required: true, message: '请输入所在区域' }]}
            >
              <Input placeholder="例如: A区3号圈 / 1号鸡舍" />
            </Form.Item>

            <Form.Item
              label="养殖周期（天）"
              name="feedingCycle"
            >
              <Space.Compact style={{ width: '100%' }}>
                <InputNumber min={1} max={365} style={{ width: '100%' }} placeholder="预计养殖天数" />
                <Input readOnly value="天" style={{ width: 44, textAlign: 'center', color: '#666', background: '#fafafa' }} />
              </Space.Compact>
            </Form.Item>

            <Form.Item
              label="出栏均重（kg）"
              name="weight"
            >
              <Space.Compact style={{ width: '100%' }}>
                <InputNumber min={0.1} max={50} step={0.1} style={{ width: '100%' }} placeholder="预计出栏均重" />
                <Input readOnly value="kg" style={{ width: 44, textAlign: 'center', color: '#666', background: '#fafafa' }} />
              </Space.Compact>
            </Form.Item>

            <Form.Item
              label="批次来源"
              name="source"
            >
              <Input placeholder="例如: 自孵化 / 外购雏鸡 / 合作养殖场" />
            </Form.Item>

            <Form.Item
              label="饲养员"
              name="breeder"
            >
              <Input placeholder="负责该养殖群的饲养员姓名" />
            </Form.Item>

            <Form.Item
              label="备注说明"
              name="description"
            >
              <Input.TextArea rows={3} placeholder="补充说明（如饲料品牌、用药记录、特殊养殖要求等）" />
            </Form.Item>
        </Form>
      </div>
    </Modal>
  )
}

export default CreateBatchModal
