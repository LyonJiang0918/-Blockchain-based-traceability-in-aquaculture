import React, { useState, useEffect } from 'react'
import { Modal, Form, Input, InputNumber, Select, DatePicker, message, Space } from 'antd'
import { InfoCircleOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import growthApi from '../api/growth'

// 健康状态
const HEALTH_STATUS = [
  { value: 'HEALTHY', label: '💚 健康' },
  { value: 'NORMAL', label: '💛 正常' },
  { value: 'SICK', label: '❤️ 患病' },
  { value: 'WEAK', label: '💜 弱雏' },
]

// 发育阶段
const GROWTH_STAGES = [
  { value: 'CHICK', label: '🐣 育雏期' },
  { value: 'GROWING', label: '📈 生长期' },
  { value: 'FATTENING', label: '🐔 育肥期' },
  { value: 'LAYING', label: '🥚 产蛋期' },
]

function CreateGrowthModal({ visible, onCancel, onSuccess, groupId }) {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [submitResult, setSubmitResult] = useState(null)

  useEffect(() => {
    if (visible) {
      const now = new Date()
      const recordId = 'GRW' + String(now.getFullYear()) +
        String(now.getMonth() + 1).padStart(2, '0') +
        String(now.getDate()).padStart(2, '0') +
        String(now.getHours()).padStart(2, '0') +
        String(now.getMinutes()).padStart(2, '0') +
        String(now.getSeconds()).padStart(2, '0')
      form.setFieldsValue({
        recordId,
        groupId,
        recordDate: dayjs(),
        healthStatus: 'HEALTHY',
        growthStage: 'GROWING',
        vitalityScore: 8,
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
        recordDate: values.recordDate ? values.recordDate.unix() : null,
        avgWeight: values.avgWeight,
        maxWeight: values.maxWeight,
        minWeight: values.minWeight,
        healthStatus: values.healthStatus,
        survivalCount: values.survivalCount,
        deathCount: values.deathCount,
        cullCount: values.cullCount,
        growthStage: values.growthStage,
        appearanceCondition: values.appearanceCondition,
        vitalityScore: values.vitalityScore,
        description: values.description,
        inspector: values.inspector,
      }

      await growthApi.create(requestData)
      setSubmitResult({ success: true })
      message.success('✓ 成长记录成功！')
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
      title="📊 成长记录"
      open={visible}
      onOk={handleOk}
      onCancel={onCancel}
      confirmLoading={loading}
      okText={submitResult ? '完成' : '确认录入'}
      cancelText="取消"
      width={600}
      destroyOnClose
    >
      {submitResult ? (
        <div style={{ textAlign: 'center', padding: '24px 0' }}>
          <div style={{ fontSize: 48, marginBottom: 16 }}>✅</div>
          <h3>成长记录成功！</h3>
          <p style={{ color: '#666', fontSize: 13 }}>数据已上链存证</p>
        </div>
      ) : (
        <>
          <div style={{
            background: '#f6ffed', border: '1px solid #b7eb8f',
            borderRadius: 6, padding: '10px 14px', marginBottom: 16, fontSize: 13, color: '#52c41a'
          }}>
            <InfoCircleOutlined style={{ marginRight: 6 }} />
            记录养殖群的体重、健康状态、发育阶段等成长情况。
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
              <Form.Item label="记录日期" name="recordDate" style={{ flex: 1 }}>
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
              <Form.Item label="发育阶段" name="growthStage" style={{ flex: 1 }}>
                <Select options={GROWTH_STAGES} />
              </Form.Item>
            </Space>

            <div style={{ background: '#fafafa', padding: 12, borderRadius: 6, marginBottom: 16 }}>
              <div style={{ fontWeight: 500, marginBottom: 12 }}>体重记录（kg）</div>
              <Space style={{ display: 'flex', width: '100%' }} size={12}>
                <Form.Item label="平均体重" name="avgWeight" style={{ flex: 1 }}>
                  <InputNumber min={0} step={0.1} style={{ width: '100%' }} placeholder="平均" />
                </Form.Item>
                <Form.Item label="最大" name="maxWeight" style={{ flex: 1 }}>
                  <InputNumber min={0} step={0.1} style={{ width: '100%' }} placeholder="最大" />
                </Form.Item>
                <Form.Item label="最小" name="minWeight" style={{ flex: 1 }}>
                  <InputNumber min={0} step={0.1} style={{ width: '100%' }} placeholder="最小" />
                </Form.Item>
              </Space>
            </div>

            <Space style={{ display: 'flex', width: '100%' }} size={16}>
              <Form.Item label="健康状态" name="healthStatus" style={{ flex: 1 }}>
                <Select options={HEALTH_STATUS} />
              </Form.Item>
              <Form.Item label="活力评分" name="vitalityScore" style={{ width: 120 }}>
                <InputNumber min={1} max={10} style={{ width: '100%' }} />
              </Form.Item>
            </Space>

            <div style={{ background: '#fafafa', padding: 12, borderRadius: 6, marginBottom: 16 }}>
              <div style={{ fontWeight: 500, marginBottom: 12 }}>数量统计</div>
              <Space style={{ display: 'flex', width: '100%' }} size={12}>
                <Form.Item label="存活数" name="survivalCount" style={{ flex: 1 }}>
                  <InputNumber min={0} style={{ width: '100%' }} placeholder="存活" />
                </Form.Item>
                <Form.Item label="死亡数" name="deathCount" style={{ flex: 1 }}>
                  <InputNumber min={0} style={{ width: '100%' }} placeholder="死亡" />
                </Form.Item>
                <Form.Item label="淘汰数" name="cullCount" style={{ flex: 1 }}>
                  <InputNumber min={0} style={{ width: '100%' }} placeholder="淘汰" />
                </Form.Item>
              </Space>
            </div>

            <Form.Item label="外观状态" name="appearanceCondition">
              <Input placeholder="如：羽毛光亮、精神活泼等" />
            </Form.Item>

            <Space style={{ display: 'flex', width: '100%' }} size={16}>
              <Form.Item label="检测员" name="inspector" style={{ flex: 1 }}>
                <Input placeholder="检测员姓名" />
              </Form.Item>
            </Space>

            <Form.Item label="备注说明" name="description">
              <Input.TextArea rows={2} placeholder="其他补充说明" />
            </Form.Item>
          </Form>
        </>
      )}
    </Modal>
  )
}

export default CreateGrowthModal
