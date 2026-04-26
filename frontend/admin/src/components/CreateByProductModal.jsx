import React, { useState, useEffect } from 'react'
import {
  Modal, Form, Input, InputNumber, Select, DatePicker, message, Space, Divider
} from 'antd'
import { InfoCircleOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import byProductApi from '../api/byproduct'

// 副产品类型配置
const PRODUCT_TYPES = [
  { value: 'EGG', label: '🥚 蛋类', desc: '鸡蛋、鸭蛋、鹅蛋等' },
  { value: 'WOOL', label: '🐑 毛类', desc: '羊毛、羊绒等' },
  { value: 'MILK', label: '🥛 奶类', desc: '牛奶、羊奶等' },
  { value: 'MEAT', label: '🥩 肉类', desc: '鸡肉、羊肉、牛肉等' },
  { value: 'OTHER', label: '📦 其他', desc: '蜂蜜、皮张等其他副产品' },
]

// 副产品名称选项
const PRODUCT_NAME_OPTIONS = {
  EGG: ['鸡蛋', '鸭蛋', '鹅蛋', '鹌鹑蛋', '鸵鸟蛋'],
  WOOL: ['羊毛', '羊绒', '驼绒'],
  MILK: ['生鲜牛奶', '羊奶', '驼奶'],
  MEAT: ['鸡肉', '鸭肉', '鹅肉', '羊肉', '牛肉', '猪肉', '兔肉'],
  OTHER: ['蜂蜜', '蜂王浆', '皮张', '骨粉'],
}

// 单位选项
const UNIT_OPTIONS = {
  EGG: [{ value: '枚', label: '枚' }, { value: '个', label: '个' }, { value: '箱', label: '箱' }],
  WOOL: [{ value: 'kg', label: '公斤(kg)' }, { value: 'g', label: '克(g)' }, { value: '吨', label: '吨' }],
  MILK: [{ value: 'L', label: '升(L)' }, { value: 'kg', label: '公斤(kg)' }],
  MEAT: [{ value: 'kg', label: '公斤(kg)' }, { value: 'g', label: '克(g)' }, { value: '吨', label: '吨' }],
  OTHER: [{ value: 'kg', label: '公斤(kg)' }, { value: 'g', label: '克(g)' }, { value: '箱', label: '箱' }],
}

// 质量等级
const QUALITY_GRADES = [
  { value: '优', label: '优等品', color: '#52c41a' },
  { value: '良', label: '良好品', color: '#1890ff' },
  { value: '合格', label: '合格品', color: '#faad14' },
]

// 存储方式
const STORAGE_METHODS = [
  { value: '冷藏', label: '冷藏保存' },
  { value: '冷冻', label: '冷冻保存' },
  { value: '常温', label: '常温保存' },
  { value: '干燥', label: '干燥保存' },
]

function CreateByProductModal({ visible, onCancel, onSuccess, groupId }) {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [submitResult, setSubmitResult] = useState(null)
  const [selectedType, setSelectedType] = useState('EGG')

  useEffect(() => {
    if (visible) {
      // 自动生成副产品ID
      const now = new Date()
      const productId = 'PRD' +
        String(now.getFullYear()) +
        String(now.getMonth() + 1).padStart(2, '0') +
        String(now.getDate()).padStart(2, '0') +
        String(now.getHours()).padStart(2, '0') +
        String(now.getMinutes()).padStart(2, '0') +
        String(now.getSeconds()).padStart(2, '0')
      form.setFieldsValue({
        productId,
        groupId,
        productType: 'EGG',
        productName: '鸡蛋',
        unit: '枚',
        productionDate: dayjs(),
        qualityGrade: '优',
      })
      setSelectedType('EGG')
    } else {
      form.resetFields()
      setSubmitResult(null)
    }
  }, [visible, form, groupId])

  const handleTypeChange = (type) => {
    setSelectedType(type)
    const defaultName = PRODUCT_NAME_OPTIONS[type]?.[0] || ''
    const defaultUnit = UNIT_OPTIONS[type]?.[0]?.value || 'kg'
    form.setFieldsValue({ productName: defaultName, unit: defaultUnit })
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      setLoading(true)

      const requestData = {
        productId: values.productId,
        groupId: values.groupId,
        productType: values.productType,
        productName: values.productName,
        quantity: values.quantity,
        unit: values.unit,
        productionDate: values.productionDate ? values.productionDate.unix() : null,
        productionBatch: values.productionBatch,
        qualityGrade: values.qualityGrade,
        storageMethod: values.storageMethod,
        description: values.description,
      }

      const result = await byProductApi.create(requestData)
      setSubmitResult(result)
      message.success('✓ 农副产品记录成功！')
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

  const currentNames = PRODUCT_NAME_OPTIONS[selectedType] || []
  const currentUnits = UNIT_OPTIONS[selectedType] || [{ value: 'kg', label: '公斤(kg)' }]

  return (
    <Modal
      title="📦 录入农副产品"
      open={visible}
      onOk={handleOk}
      onCancel={onCancel}
      confirmLoading={loading}
      okText={submitResult ? '完成' : '确认录入'}
      cancelText="取消"
      width={640}
      destroyOnClose
    >
      {submitResult ? (
        <div style={{ textAlign: 'center', padding: '24px 0' }}>
          <div style={{ fontSize: 48, marginBottom: 16 }}>
            <span role="img" aria-label="success">✅</span>
          </div>
          <h3>农副产品录入成功！</h3>
          <p style={{ color: '#666', fontSize: 13 }}>
            数据已保存，生成区块链哈希用于溯源验证。
          </p>
        </div>
      ) : (
        <>
          <div style={{
            background: '#f6ffed', border: '1px solid #b7eb8f',
            borderRadius: 6, padding: '10px 14px', marginBottom: 16, fontSize: 13, color: '#52c41a'
          }}>
            <InfoCircleOutlined style={{ marginRight: 6 }} />
            农副产品记录将关联到养殖群，产出信息会上链存证，方便消费者溯源查询。
          </div>

          <Form form={form} layout="vertical" size="middle">
            <Space style={{ display: 'flex', width: '100%' }} size={16}>
              <Form.Item
                label="副产品ID"
                name="productId"
                rules={[{ required: true, message: '副产品ID不能为空' }]}
                style={{ flex: 1 }}
              >
                <Input disabled style={{ color: '#1890ff', fontWeight: 'bold' }} />
              </Form.Item>

              <Form.Item
                label="所属养殖群"
                name="groupId"
                rules={[{ required: true, message: '养殖群ID不能为空' }]}
                style={{ flex: 1 }}
              >
                <Input disabled />
              </Form.Item>
            </Space>

            <Form.Item
              label="副产品类型"
              name="productType"
              rules={[{ required: true, message: '请选择副产品类型' }]}
            >
              <Select
                placeholder="请选择副产品类型"
                onChange={handleTypeChange}
                options={PRODUCT_TYPES.map(t => ({
                  value: t.value,
                  label: (
                    <span>
                      {t.label}
                      <span style={{ color: '#999', fontSize: 12, marginLeft: 8 }}>{t.desc}</span>
                    </span>
                  ),
                }))}
              />
            </Form.Item>

            <Space style={{ display: 'flex', width: '100%' }} size={16}>
              <Form.Item
                label="具体名称"
                name="productName"
                rules={[{ required: true, message: '请输入名称' }]}
                style={{ flex: 1 }}
              >
                <Select
                  showSearch
                  allowClear
                  options={currentNames.map(n => ({ value: n, label: n }))}
                  placeholder="选择或输入具体名称"
                  filterOption={(input, option) =>
                    (option?.label ?? '').toLowerCase().includes(input.toLowerCase())
                  }
                />
              </Form.Item>

              <Form.Item
                label="质量等级"
                name="qualityGrade"
                rules={[{ required: true, message: '请选择质量等级' }]}
                style={{ width: 120 }}
              >
                <Select
                  options={QUALITY_GRADES.map(g => ({ value: g.value, label: g.label }))}
                />
              </Form.Item>
            </Space>

            <Space style={{ display: 'flex', width: '100%' }} size={16}>
              <Form.Item
                label="产出数量"
                name="quantity"
                rules={[{ required: true, message: '请输入数量' }]}
                style={{ flex: 1 }}
              >
                <InputNumber min={0.01} step={1} style={{ width: '100%' }} placeholder="产出数量" />
              </Form.Item>

              <Form.Item
                label="单位"
                name="unit"
                rules={[{ required: true, message: '请选择单位' }]}
                style={{ width: 120 }}
              >
                <Select options={currentUnits} />
              </Form.Item>
            </Space>

            <Space style={{ display: 'flex', width: '100%' }} size={16}>
              <Form.Item
                label="生产日期"
                name="productionDate"
                style={{ flex: 1 }}
              >
                <DatePicker style={{ width: '100%' }} placeholder="选择生产日期" />
              </Form.Item>

              <Form.Item
                label="生产批次"
                name="productionBatch"
                style={{ flex: 1 }}
              >
                <Input placeholder="例如: B2026040301" />
              </Form.Item>
            </Space>

            <Form.Item
              label="存储方式"
              name="storageMethod"
            >
              <Select
                placeholder="请选择存储方式"
                allowClear
                options={STORAGE_METHODS}
              />
            </Form.Item>

            <Form.Item
              label="备注说明"
              name="description"
            >
              <Input.TextArea rows={2} placeholder="补充说明（如特殊处理、检测信息等）" />
            </Form.Item>
          </Form>
        </>
      )}
    </Modal>
  )
}

export default CreateByProductModal
