import { useCallback, useEffect, useState } from 'react'
import {
  Button,
  Empty,
  Form,
  Input,
  InputNumber,
  Popconfirm,
  Space,
  Spin,
  Switch,
  Table,
  Tag,
  message,
} from 'antd'
import {
  DeleteOutlined,
  EditOutlined,
  PlusOutlined,
  SearchOutlined,
} from '@ant-design/icons'
import SidePanel from '../../components/SidePanel/SidePanel'
import {
  createAlumniEnterprise,
  deleteAlumniEnterprise,
  getAlumniEnterpriseList,
  updateAlumniEnterprise,
} from '../../api/employment'
import './employmentCommon.css'

const companyMark = (record) =>
  record?.shortName || String(record?.name || '').replace(/^成都/, '').slice(0, 2) || '企业'

export default function AlumniEnterpriseManage() {
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [keyword, setKeyword] = useState('')
  const [editorOpen, setEditorOpen] = useState(false)
  const [editingRecord, setEditingRecord] = useState(null)

  const fetchData = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getAlumniEnterpriseList(keyword ? { keyword } : {})
      setData(Array.isArray(res.data) ? res.data : [])
    } catch {
      setData([])
    } finally {
      setLoading(false)
    }
  }, [keyword])

  useEffect(() => {
    fetchData()
  }, [fetchData])

  const openCreate = () => {
    setEditingRecord(null)
    setEditorOpen(true)
  }

  const openEdit = (record) => {
    setEditingRecord(record)
    setEditorOpen(true)
  }

  const handleDelete = async (id) => {
    try {
      await deleteAlumniEnterprise(id)
      message.success('校友企业已删除')
      fetchData()
    } catch {
      // 请求层已提示失败原因
    }
  }

  const hiringCount = data.filter((item) => item.hiring).length
  const visibleCount = data.filter((item) => item.enabled).length

  const columns = [
    {
      title: '企业名称',
      dataIndex: 'name',
      render: (text, record) => (
        <span className="employment-company">
          <span className="employment-company__mark">{companyMark(record)}</span>
          <span className="employment-company__text">
            <strong>{text}</strong>
            {record.industry ? <small>{record.industry}</small> : null}
          </span>
        </span>
      ),
    },
    {
      title: '招聘方向',
      dataIndex: 'fields',
      width: 200,
      render: (fields) => (
        <Space size={6} wrap>
          {String(fields || '')
            .split(',')
            .map((field) => field.trim())
            .filter(Boolean)
            .map((field) => (
              <Tag key={field}>{field}</Tag>
            ))}
        </Space>
      ),
    },
    {
      title: '校友在职',
      dataIndex: 'alumniCount',
      width: 100,
      render: (value) => `${value ?? 0} 人`,
    },
    {
      title: '开放岗位',
      dataIndex: 'openPositions',
      width: 100,
      render: (value) => `${value ?? 0} 个`,
    },
    {
      title: '在招状态',
      dataIndex: 'hiring',
      width: 110,
      render: (hiring) =>
        hiring ? <Tag color="success">正在招聘</Tag> : <Tag>暂未招聘</Tag>,
    },
    {
      title: '展示',
      dataIndex: 'enabled',
      width: 90,
      render: (enabled) =>
        enabled ? <Tag color="blue">展示中</Tag> : <Tag>已隐藏</Tag>,
    },
    {
      title: '操作',
      key: 'action',
      width: 160,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button type="text" size="small" icon={<EditOutlined />} onClick={() => openEdit(record)}>
            编辑
          </Button>
          <Popconfirm title="确定删除此校友企业吗？" onConfirm={() => handleDelete(record.id)}>
            <Button type="text" size="small" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div className="employment-manage-page">
      <header className="employment-header">
        <div className="employment-header-left">
          <h2>校友企业</h2>
          <p>维护实习就业页「校友企业」板块展示的企业名单、在招状态与开放岗位。</p>
        </div>
        <div className="employment-header-right">
          <Input
            placeholder="搜索企业名称"
            prefix={<SearchOutlined />}
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            allowClear
            style={{ width: 220 }}
          />
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新增企业
          </Button>
        </div>
      </header>

      <div className="employment-stats">
        <div className="employment-stat">
          <span>企业总数</span>
          <strong>{data.length}</strong>
        </div>
        <div className="employment-stat">
          <span>正在招聘</span>
          <strong>{hiringCount}</strong>
        </div>
        <div className="employment-stat">
          <span>展示中</span>
          <strong>{visibleCount}</strong>
        </div>
      </div>

      <div className="employment-table-card">
        <Spin spinning={loading}>
          <Table
            columns={columns}
            dataSource={data}
            rowKey="id"
            scroll={{ x: 1000 }}
            locale={{ emptyText: <Empty description="暂无校友企业数据" /> }}
            pagination={false}
          />
        </Spin>
      </div>

      <AlumniEditor
        open={editorOpen}
        record={editingRecord}
        saving={saving}
        onClose={() => setEditorOpen(false)}
        onSave={async (values) => {
          setSaving(true)
          try {
            if (editingRecord) {
              await updateAlumniEnterprise(editingRecord.id, values)
              message.success('校友企业已保存')
            } else {
              await createAlumniEnterprise(values)
              message.success('校友企业已创建')
            }
            setEditorOpen(false)
            fetchData()
          } catch {
            // 请求层已提示失败原因
          } finally {
            setSaving(false)
          }
        }}
      />
    </div>
  )
}

function AlumniEditor({ open, record, saving, onClose, onSave }) {
  const [form] = Form.useForm()
  const isEdit = Boolean(record)

  useEffect(() => {
    if (!open) return
    form.setFieldsValue({
      name: record?.name || '',
      shortName: record?.shortName || '',
      industry: record?.industry || '',
      fields: record?.fields || '',
      alumniCount: record?.alumniCount ?? 0,
      openPositions: record?.openPositions ?? 0,
      hiring: record?.hiring ?? true,
      enabled: record?.enabled ?? true,
      contactName: record?.contactName || '',
      contactPhone: record?.contactPhone || '',
      sortOrder: record?.sortOrder ?? 0,
      description: record?.description || '',
    })
  }, [record, form, open])

  const submit = async () => {
    try {
      const values = await form.validateFields()
      onSave(values)
    } catch {
      // Ant Design 会展示校验信息
    }
  }

  return (
    <SidePanel
      title={isEdit ? '编辑校友企业' : '新增校友企业'}
      open={open}
      onClose={onClose}
      destroyOnHidden
      footer={
        <>
          <Button onClick={onClose}>取消</Button>
          <Button type="primary" loading={saving} onClick={submit}>
            保存
          </Button>
        </>
      }
    >
      <Form form={form} layout="vertical">
        <Form.Item label="企业名称" name="name" rules={[{ required: true, message: '请输入企业名称' }]}>
          <Input placeholder="例如：成都风雨兴科技有限公司" maxLength={120} />
        </Form.Item>
        <Form.Item label="企业简称" name="shortName" extra="用于学生端列表徽标，建议 2 个字">
          <Input placeholder="例如：风雨" maxLength={20} />
        </Form.Item>
        <Form.Item label="所属行业" name="industry">
          <Input placeholder="例如：软件与信息服务" maxLength={60} />
        </Form.Item>
        <Form.Item label="招聘方向" name="fields" extra="多个方向用英文逗号分隔">
          <Input placeholder="例如：Python,Java,AI" maxLength={200} />
        </Form.Item>
        <Space size={16} style={{ display: 'flex' }}>
          <Form.Item label="本校校友在职人数" name="alumniCount" style={{ flex: 1 }}>
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="当前开放岗位数" name="openPositions" style={{ flex: 1 }}>
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
        </Space>
        <Space size={16} style={{ display: 'flex' }}>
          <Form.Item label="正在招聘" name="hiring" valuePropName="checked" style={{ flex: 1 }}>
            <Switch />
          </Form.Item>
          <Form.Item label="在学生端展示" name="enabled" valuePropName="checked" style={{ flex: 1 }}>
            <Switch />
          </Form.Item>
        </Space>
        <Space size={16} style={{ display: 'flex' }}>
          <Form.Item label="联系人" name="contactName" style={{ flex: 1 }}>
            <Input maxLength={60} />
          </Form.Item>
          <Form.Item label="联系电话" name="contactPhone" style={{ flex: 1 }}>
            <Input maxLength={40} />
          </Form.Item>
        </Space>
        <Form.Item label="排序" name="sortOrder" extra="值越小越靠前">
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item label="企业简介" name="description">
          <Input.TextArea rows={3} maxLength={1000} showCount />
        </Form.Item>
      </Form>
    </SidePanel>
  )
}
