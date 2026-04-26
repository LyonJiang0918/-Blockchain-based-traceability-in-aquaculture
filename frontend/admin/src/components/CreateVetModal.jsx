import React, { useState, useEffect } from 'react'
import { Modal, Form, Input, InputNumber, Select, DatePicker, message, Space, Divider } from 'antd'
import { InfoCircleOutlined } from '@ant-design/icons'
import dayjs from 'dayjs'
import vetApi from '../api/vet'

// 记录类型
const RECORD_TYPES = [
  { value: 0, label: '💉 免疫（疫苗接种）' },
  { value: 1, label: '💊 用药' },
  { value: 2, label: '🏥 治疗' },
]

// 疫苗类型
const VACCINE_TYPES = [
  { value: 'NEWCASTLE', label: '新城疫' },
  { value: 'BIRD_FLU', label: '禽流感' },
  { value: 'BURSAL', label: '法氏囊' },
  { value: 'RABIES', label: '狂犬病（羊）' },
  { value: 'FOOT_MOUTH', label: '口蹄疫（牛羊）' },
  { value: 'BRUCELLOSIS', label: '布病（羊）' },
  { value: 'OTHER', label: '其他' },
]

// 用药途径
const ADMIN_ROUTES = [
  { value: 'INJECTION', label: '注射' },
  { value: 'ORAL', label: '口服' },
  { value: 'DRINKING_WATER', label: '饮水' },
  { value: 'SPRAY', label: '喷雾' },
]

function CreateVetModal({ visible, onCancel, onSuccess, groupId }) {
  const [form] = Form.useForm()
  const [loading, setLoading] = useState(false)
  const [submitResult, setSubmitResult] = useState(null)
  const [recordType, setRecordType] = useState(0)

  useEffect(() => {
    if (visible) {
      const now = new Date()
      const recordId = 'VET' + String(now.getFullYear()) +
        String(now.getMonth() + 1).padStart(2, '0') +
        String(now.getDate()).padStart(2, '0') +
        String(now.getHours()).padStart(2, '0') +
        String(now.getMinutes()).padStart(2, '0') +
        String(now.getSeconds()).padStart(2, '0')
      form.setFieldsValue({
        recordId,
        groupId,
        recordType: 0,
        operationDate: dayjs(),
      })
      setRecordType(0)
    } else {
      form.resetFields()
      setSubmitResult(null)
    }
  }, [visible, form, groupId])

  const handleTypeChange = (type) => {
    setRecordType(type)
    form.setFieldsValue({ vaccineType: undefined })
  }

  const handleSubmit = async () => {
    try {
      const values = await form.validateFields()
      setLoading(true)

      const requestData = {
        recordId: values.recordId,
        groupId: values.groupId,
        recordType: values.recordType,
        medicineName: values.medicineName,
        medicineId: values.medicineId,
        vaccineType: values.vaccineType,
        manufacturer: values.manufacturer,
        batchNumber: values.batchNumber,
        expiryDate: values.expiryDate ? values.expiryDate.unix() : null,
        operationDate: values.operationDate ? values.operationDate.unix() : null,
        dosage: values.dosage,
        dosageUnit: values.dosageUnit,
        administrationRoute: values.administrationRoute,
        vetName: values.vetName,
        vetLicense: values.vetLicense,
        vetInstitution: values.vetInstitution,
        targetAnimals: values.targetAnimals,
        diagnosis: values.diagnosis,
        description: values.description,
      }

      await vetApi.create(requestData)
      setSubmitResult({ success: true })
      message.success('✓ 兽医记录成功！')
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
      title="💉 疫苗/兽医记录"
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
          <div style={{ fontSize: 48, marginBottom: 16 }}>✅</div>
          <h3>兽医记录录入成功！</h3>
          <p style={{ color: '#666', fontSize: 13 }}>数据已上链存证</p>
        </div>
      ) : (
        <>
          <div style={{
            background: '#fff1f0', border: '1px solid #ffccc7',
            borderRadius: 6, padding: '10px 14px', marginBottom: 16, fontSize: 13, color: '#ff4d4f'
          }}>
            <InfoCircleOutlined style={{ marginRight: 6 }} />
            记录疫苗接种、用药、治疗情况，兽医信息将上链存证。
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

            <Form.Item label="记录类型" name="recordType" rules={[{ required: true }]}>
              <Select options={RECORD_TYPES} onChange={handleTypeChange} />
            </Form.Item>

            {/* 免疫专用字段 */}
            {recordType === 0 && (
              <>
                <Divider plain>疫苗信息</Divider>
                <Space style={{ display: 'flex', width: '100%' }} size={16}>
                  <Form.Item label="疫苗类型" name="vaccineType" style={{ flex: 1 }}>
                    <Select placeholder="选择疫苗类型" allowClear options={VACCINE_TYPES} />
                  </Form.Item>
                  <Form.Item label="药品名称" name="medicineName" rules={[{ required: true }]} style={{ flex: 1 }}>
                    <Input placeholder="疫苗商品名称" />
                  </Form.Item>
                </Space>
                <Space style={{ display: 'flex', width: '100%' }} size={16}>
                  <Form.Item label="生产厂家" name="manufacturer" style={{ flex: 1 }}>
                    <Input placeholder="疫苗生产厂家" />
                  </Form.Item>
                  <Form.Item label="批号" name="batchNumber" style={{ flex: 1 }}>
                    <Input placeholder="疫苗批号" />
                  </Form.Item>
                </Space>
                <Space style={{ display: 'flex', width: '100%' }} size={16}>
                  <Form.Item label="有效期" name="expiryDate" style={{ flex: 1 }}>
                    <DatePicker style={{ width: '100%' }} />
                  </Form.Item>
                  <Form.Item label="免疫对象" name="targetAnimals" style={{ flex: 1 }}>
                    <Input placeholder="如：全群、雏鸡等" />
                  </Form.Item>
                </Space>
              </>
            )}

            {/* 用药/治疗专用字段 */}
            {(recordType === 1 || recordType === 2) && (
              <>
                <Divider plain>{recordType === 1 ? '用药' : '治疗'}信息</Divider>
                <Space style={{ display: 'flex', width: '100%' }} size={16}>
                  <Form.Item label="药品名称" name="medicineName" rules={[{ required: true }]} style={{ flex: 1 }}>
                    <Input placeholder="药品名称" />
                  </Form.Item>
                  <Form.Item label="病症描述" name="diagnosis" style={{ flex: 1 }}>
                    <Input placeholder="诊断的病症" />
                  </Form.Item>
                </Space>
              </>
            )}

            {/* 通用字段 */}
            <Space style={{ display: 'flex', width: '100%' }} size={16}>
              <Form.Item label="操作日期" name="operationDate" style={{ flex: 1 }}>
                <DatePicker style={{ width: '100%' }} />
              </Form.Item>
              <Form.Item label="用药途径" name="administrationRoute" style={{ flex: 1 }}>
                <Select placeholder="选择途径" allowClear options={ADMIN_ROUTES} />
              </Form.Item>
            </Space>

            <Space style={{ display: 'flex', width: '100%' }} size={16}>
              <Form.Item label="剂量" name="dosage" style={{ flex: 1 }}>
                <InputNumber min={0} step={0.1} style={{ width: '100%' }} placeholder="用量" />
              </Form.Item>
              <Form.Item label="剂量单位" name="dosageUnit" style={{ width: 120 }}>
                <Select
                  options={[
                    { value: 'ml', label: 'ml' },
                    { value: 'L', label: 'L' },
                    { value: 'g', label: 'g' },
                    { value: 'mg', label: 'mg' },
                    { value: 'IU', label: 'IU' },
                  ]}
                  placeholder="单位"
                />
              </Form.Item>
            </Space>

            <Divider plain>兽医信息</Divider>
            <Space style={{ display: 'flex', width: '100%' }} size={16}>
              <Form.Item label="兽医姓名" name="vetName" style={{ flex: 1 }}>
                <Input placeholder="兽医姓名" />
              </Form.Item>
              <Form.Item label="执照号" name="vetLicense" style={{ flex: 1 }}>
                <Input placeholder="执业执照号" />
              </Form.Item>
            </Space>

            <Form.Item label="执业机构" name="vetInstitution">
              <Input placeholder="兽医所属机构" />
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

export default CreateVetModal
