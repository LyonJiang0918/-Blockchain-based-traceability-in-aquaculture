import React, { useState, useEffect } from 'react'
import { Modal, Form, Input, InputNumber, Select, DatePicker, message, Space } from 'antd'
import { InfoCircleOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import feedApi from '../api/feed'

// 饲料类型
const FEED_TYPES = [
  { value: 'CORN', label: '🌽 玉米' },
  { value: 'SOYBEAN', label: '🫘 豆粕' },
  { value: 'WHEAT', label: '🌾 小麦' },
  { value: 'FORMULA', label: '🧪 配合饲料' },
  { value: 'GREEN', label: '🥬 青绿饲料' },
  { value: 'OTHER', label: '📦 其他' },
]

// 投喂方式
const FEEDING_METHODS = [
  { value: 'MANUAL', label: '人工投喂' },
  { value: 'AUTOMATIC', label: '自动投喂' },
  { value: 'FREE_RANGE', label: '放养觅食' },
]

function CreateFeedModal({ visible, onCancel, onSuccess, groupId }) {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [submitResult, setSubmitResult] = useState(null)

  useEffect(() => {
    if (visible) {
      const now = new Date()
      const recordId = 'FEED' + String(now.getFullYear()) +
        String(now.getMonth() + 1).padStart(2, '0') +
        String(now.getDate()).padStart(2, '0') +
        String(now.getHours()).padStart(2, '0') +
        String(now.getMinutes()).padStart(2, '0') +
        String(now.getSeconds()).padStart(2, '0')
      form.setFieldsValue({
        recordId,
        groupId,
        feedDate: dayjs(),
        feedType: 'FORMULA',
        feedingMethod: 'MANUAL',
      })
    } else {
      form.resetFields()
      setSubmitResult(null)
    }
  }, [visible, form, groupId])

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      setLoading(true)

      const requestData = {
        recordId: values.recordId,
        groupId: values.groupId,
        feedType: values.feedType,
        feedBatchId: values.feedBatchId,
        feedBrand: values.feedBrand,
        feedDate: values.feedDate ? values.feedDate.unix() : null,
        amount: values.amount,
        unitCost: values.unitCost,
        totalCost: values.totalCost,
        feedingMethod: values.feedingMethod,
        operator: values.operator,
        description: values.description,
      }

      await feedApi.create(requestData)
      setSubmitResult({ success: true })
      message.success('✓ 饲料投喂记录成功！')
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

  return (
    <Modal
      title="🌾 饲料投喂记录"
      open={visible}
      onOk={handleOk}
      onCancel={onCancel}
      confirmLoading={loading}
      okText={submitResult ? '完成' : '确认录入'}
      cancelText="取消"
      width={580}
      destroyOnClose
    >
      {submitResult ? (
        <div style={{ textAlign: 'center', padding: '24px 0' }}>
          <div style={{ fontSize: 48, marginBottom: 16 }}>✅</div>
          <h3>饲料投喂记录成功！</h3>
          <p style={{ color: '#666', fontSize: 13 }}>数据已上链存证</p>
        </div>
      ) : (
        <>
          <div style={{
            background: '#fff7e6', border: '1px solid #ffd591',
            borderRadius: 6, padding: '10px 14px', marginBottom: 16, fontSize: 13, color: '#fa8c16'
          }}>
            <InfoCircleOutlined style={{ marginRight: 6 }} />
            记录饲料投喂情况，包括饲料类型、投喂量、成本等信息。
          </div>
          <Form form={form} layout="vertical" size="middle">
            <Space style={{ display: 'flex', width: '100%' }} size={16}>
              <Form.Item label="记录ID" name="recordId" style={{ flex: 1 }}>
                <Input disabled />
              </Form.Item>
              <Form.Item label="养殖群" name="groupId" style={{ flex: 1 }}>
                <Input disabled />
              </Form.Item>
            </Space>

            <Space style={{ display: 'flex', width: '100%' }} size={16}>
              <Form.Item label="饲料类型" name="feedType" rules={[{ required: true }]} style={{ flex: 1 }}>
                <Select options={FEED_TYPES} />
              </Form.Item>
              <Form.Item label="投喂方式" name="feedingMethod" style={{ flex: 1 }}>
                <Select options={FEEDING_METHODS} />
              </Form.Item>
            </Space>

            <Form.Item label="饲料品牌/名称" name="feedBrand">
              <Input placeholder="如：正大饲料、新希望等" />
            </Form.Item>

            <Space style={{ display: 'flex', width: '100%' }} size={16}>
              <Form.Item label="投喂日期" name="feedDate" style={{ flex: 1 }}>
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
              <Form.Item label="饲料批次号" name="feedBatchId" style={{ flex: 1 }}>
                <Input placeholder="如：B2026040301" />
              </Form.Item>
            </Space>

            <Space style={{ display: 'flex', width: '100%' }} size={16}>
              <Form.Item label="投喂量（kg）" name="amount" rules={[{ required: true }]} style={{ flex: 1 }}>
                <InputNumber min={0.1} step={1} style={{ width: '100%' }} placeholder="投喂量" />
              </Form.Item>
              <Form.Item label="单价（元/kg）" name="unitCost" style={{ flex: 1 }}>
                <InputNumber min={0} step={0.1} style={{ width: '100%' }} placeholder="单价" />
              </Form.Item>
            </Space>

            <Form.Item label="操作人员" name="operator">
              <Input placeholder="投喂人员姓名" />
            </Form.Item>

            <Form.Item label="备注说明" name="description">
              <Input.TextArea rows={2} placeholder="补充说明" />
            </Form.Item>
          </Form>
        </>
      )}
    </Modal>
  )
}

export default CreateFeedModal
