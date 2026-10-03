import { useCallback, useEffect, useState } from 'react'
import {
  Button,
  DatePicker,
  Empty,
  Form,
  Input,
  InputNumber,
  Popconfirm,
  Select,
  Space,
  Spin,
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
import dayjs from 'dayjs'
import SidePanel from '../../components/SidePanel/SidePanel'
import {
  createCampusRecruitment,
  deleteCampusRecruitment,
  getCampusRecruitmentList,
  updateCampusRecruitment,
} from '../../api/employment'
import './employmentCommon.css'

const TYPE_OPTIONS = [
  { value: 'TALK', label: '宣讲会' },
  { value: 'FAIR', label: '双选会' },
  { value: 'JOB', label: '校招岗位' },
]

const TYPE_LABEL = { TALK: '宣讲会', FAIR: '双选会', JOB: '校招岗位' }
const TYPE_COLOR = { TALK: 'blue', FAIR: 'purple', JOB: 'green' }

const STATUS_OPTIONS = [
  { value: 'PUBLISHED', label: '已发布' },
  { value: 'DRAFT', label: '草稿' },
]

const formatEventDate = (value) => {
  if (!value) return '-'
  const date = dayjs(value)
  return date.isValid() ? date.format('YYYY-MM-DD') : String(value)
}

export default function CampusRecruitmentManage() {
  const [data, setData] = useState([])
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [keyword, setKeyword] = useState('')
  const [editorOpen, setEditorOpen] = useState(false)
  const [editingRecord, setEditingRecord] = useState(null)

  const fetchData = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getCampusRecruitmentList(keyword ? { keyword } : {})
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
      await deleteCampusRecruitment(id)
      message.success('校园招聘条目已删除')
      fetchData()
    } catch {
      // 请求层已提示失败原因
    }
  }

  const talksCount = data.filter((item) => item.type === 'TALK').length
  const fairsCount = data.filter((item) => item.type === 'FAIR').length
  const rolesCount = data.reduce((sum, item) => sum + (Number(item.roleCount) || 0), 0)

  const columns = [
    {
      title: '名称',
      dataIndex: 'title',
      render: (text, record) => (
        <span className="employment-company__text">
          <strong>{text}</strong>
          {record.company ? <small>{record.company}</small> : null}
        </span>
      ),
    },
    {
      title: '类型',
      dataIndex: 'type',
      width: 110,
      render: (type) => <Tag color={TYPE_COLOR[type] || 'default'}>{TYPE_LABEL[type] || type}</Tag>,
    },
    {
      title: '招聘季',
      dataIndex: 'season',
      width: 130,
      render: (value) => value || '-',
    },
    {
      title: '日期',
      dataIndex: 'eventDate',
      width: 120,
      render: (value) => formatEventDate(value),
    },
    {
      title: '地点',
      dataIndex: 'location',
      width: 160,
      render: (value) => value || '-',
    },
    {
      title: '岗位数',
      dataIndex: 'roleCount',
      width: 90,
      render: (value) => `${value ?? 0} 个`,
    },
    {
      title: '状态',
      dataIndex: 'status',
      width: 100,
      render: (status) =>
        status === 'PUBLISHED' ? <Tag color="success">已发布</Tag> : <Tag>草稿</Tag>,
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
          <Popconfirm title="确定删除此条目吗？" onConfirm={() => handleDelete(record.id)}>
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
          <h2>校园招聘</h2>
          <p>维护实习就业页「校园招聘」板块的宣讲会、双选会与校招岗位日程。</p>
        </div>
        <div className="employment-header-right">
          <Input
            placeholder="搜索名称"
            prefix={<SearchOutlined />}
            value={keyword}
            onChange={(event) => setKeyword(event.target.value)}
            allowClear
            style={{ width: 220 }}
          />
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>
            新增条目
          </Button>
        </div>
      </header>

      <div className="employment-stats">
        <div className="employment-stat">
          <span>条目总数</span>
          <strong>{data.length}</strong>
        </div>
        <div className="employment-stat">
          <span>宣讲会</span>
          <strong>{talksCount}</strong>
        </div>
        <div className="employment-stat">
          <span>双选会</span>
          <strong>{fairsCount}</strong>
        </div>
        <div className="employment-stat">
          <span>校招岗位</span>
          <strong>{rolesCount}</strong>
        </div>
      </div>

      <div className="employment-table-card">
        <Spin spinning={loading}>
          <Table
            columns={columns}
            dataSource={data}
            rowKey="id"
            scroll={{ x: 1100 }}
            locale={{ emptyText: <Empty description="暂无校园招聘数据" /> }}
            pagination={false}
          />
        </Spin>
      </div>

      <RecruitmentEditor
        open={editorOpen}
        record={editingRecord}
        saving={saving}
        onClose={() => setEditorOpen(false)}
        onSave={async (values) => {
          const payload = {
            ...values,
            eventDate: values.eventDate ? values.eventDate.format('YYYY-MM-DD') : null,
          }
          setSaving(true)
          try {
            if (editingRecord) {
              await updateCampusRecruitment(editingRecord.id, payload)
              message.success('校园招聘条目已保存')
            } else {
              await createCampusRecruitment(payload)
              message.success('校园招聘条目已创建')
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

function RecruitmentEditor({ open, record, saving, onClose, onSave }) {
  const [form] = Form.useForm()
  const isEdit = Boolean(record)

  useEffect(() => {
    if (!open) return
    form.setFieldsValue({
      title: record?.title || '',
      company: record?.company || '',
      type: record?.type || 'TALK',
      season: record?.season || '2027 届秋招',
      city: record?.city || '成都',
      location: record?.location || '',
      eventDate: record?.eventDate ? dayjs(record.eventDate) : null,
      roleCount: record?.roleCount ?? 0,
      status: record?.status || 'PUBLISHED',
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
      title={isEdit ? '编辑校园招聘条目' : '新增校园招聘条目'}
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
        <Form.Item label="名称" name="title" rules={[{ required: true, message: '请输入活动或岗位名称' }]}>
          <Input placeholder="例如：企业宣讲" maxLength={160} />
        </Form.Item>
        <Form.Item label="主办单位 / 招聘企业" name="company">
          <Input placeholder="例如：成都风雨兴科技有限公司" maxLength={120} />
        </Form.Item>
        <Space size={16} style={{ display: 'flex' }}>
          <Form.Item label="类型" name="type" style={{ flex: 1 }}>
            <Select options={TYPE_OPTIONS} />
          </Form.Item>
          <Form.Item label="招聘季" name="season" style={{ flex: 1 }}>
            <Input placeholder="例如：2027 届秋招" maxLength={40} />
          </Form.Item>
        </Space>
        <Space size={16} style={{ display: 'flex' }}>
          <Form.Item label="城市" name="city" style={{ flex: 1 }}>
            <Input maxLength={40} />
          </Form.Item>
          <Form.Item label="活动日期" name="eventDate" style={{ flex: 1 }}>
            <DatePicker style={{ width: '100%' }} />
          </Form.Item>
        </Space>
        <Form.Item label="活动地点" name="location">
          <Input placeholder="例如：大学生活动中心" maxLength={160} />
        </Form.Item>
        <Space size={16} style={{ display: 'flex' }}>
          <Form.Item label="校招岗位数" name="roleCount" style={{ flex: 1 }}>
            <InputNumber min={0} style={{ width: '100%' }} />
          </Form.Item>
          <Form.Item label="状态" name="status" style={{ flex: 1 }}>
            <Select options={STATUS_OPTIONS} />
          </Form.Item>
        </Space>
        <Form.Item label="排序" name="sortOrder" extra="值越小越靠前">
          <InputNumber min={0} style={{ width: '100%' }} />
        </Form.Item>
        <Form.Item label="说明" name="description">
          <Input.TextArea rows={3} maxLength={1000} showCount />
        </Form.Item>
      </Form>
    </SidePanel>
  )
}
