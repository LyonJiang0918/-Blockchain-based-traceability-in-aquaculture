import React, { useState, useEffect } from 'react'
import { Card, Typography, Descriptions, Tag, Row, Col, Statistic, message, List, Spin } from 'antd'
import {
  CheckCircleOutlined, SyncOutlined,
  ApiOutlined, DatabaseOutlined, SettingOutlined, RocketOutlined
} from '@ant-design/icons'

const { Title, Text } = Typography

function Settings() {
  const [sysInfo, setSysInfo] = useState(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    fetchSysInfo()
  }, [])

  const fetchSysInfo = async () => {
    setLoading(true)
    try {
      const res = await fetch('http://localhost:8080/api/system/info')
      const data = await res.json()
      if (data.success) {
        setSysInfo(data)
      }
    } catch (err) {
      message.error('获取系统信息失败')
    } finally {
      setLoading(false)
    }
  }

  if (loading) return <div style={{ textAlign: 'center', padding: 60 }}><Spin size="large" /></div>

  return (
    <div>
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={8}>
          <Card hoverable>
            <Statistic
              title="系统状态"
              value="运行中"
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card hoverable>
            <Statistic
              title="区块链连接"
              value="已连接"
              prefix={<ApiOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card hoverable>
            <Statistic
              title="数据库连接"
              value="正常"
              prefix={<DatabaseOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={24}>
          <Card
            title={<><SettingOutlined /> 系统配置</>}
            extra={<a onClick={fetchSysInfo}><SyncOutlined spin={loading} /> 刷新</a>}
          >
            <Descriptions column={2} bordered size="small">
              <Descriptions.Item label="应用名称">养殖业品质溯源平台</Descriptions.Item>
              <Descriptions.Item label="版本">v1.0.0</Descriptions.Item>
              <Descriptions.Item label="后端框架">Spring Boot 2.7.x</Descriptions.Item>
              <Descriptions.Item label="前端框架">React 18 + Ant Design 5</Descriptions.Item>
              <Descriptions.Item label="区块链平台">FISCO BCOS</Descriptions.Item>
              <Descriptions.Item label="数据库">MySQL 8.0</Descriptions.Item>
              <Descriptions.Item label="Java版本">JDK 11+</Descriptions.Item>
              <Descriptions.Item label="运行时环境">生产环境</Descriptions.Item>
            </Descriptions>
          </Card>
        </Col>
      </Row>

      <Row gutter={16}>
        <Col span={12}>
          <Card title={<><RocketOutlined /> 技术架构</>}>
            <List
              size="small"
              dataSource={[
                '前后端分离架构 (REST API)',
                'Spring Security 认证授权',
                'JWT / Session 双模式认证',
                '区块链不可篡改数据存证',
                '链上哈希 + 链下详细数据分离存储',
                '跨链溯源查询',
                '分布式 FISCO BCOS 网络',
              ]}
              renderItem={item => (
                <List.Item>
                  <CheckCircleOutlined style={{ color: '#52c41a', marginRight: 8 }} />
                  {item}
                </List.Item>
              )}
            />
          </Card>
        </Col>
        <Col span={12}>
          <Card title={<><ApiOutlined /> 区块链配置</>}>
            <Descriptions column={1} size="small">
              <Descriptions.Item label="网络类型">FISCO BCOS 2.0+</Descriptions.Item>
              <Descriptions.Item label="链ID">1</Descriptions.Item>
              <Descriptions.Item label="群组ID">1</Descriptions.Item>
              <Descriptions.Item label="合约地址">BatchRegistry</Descriptions.Item>
              <Descriptions.Item label="共识机制">PBFT</Descriptions.Item>
              <Descriptions.Item label="区块状态">
                <Tag color="green">正常</Tag>
              </Descriptions.Item>
            </Descriptions>
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default Settings