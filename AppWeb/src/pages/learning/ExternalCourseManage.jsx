import { useCallback, useEffect, useState } from 'react'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import {
  Button,
  Card,
  Form,
  Input,
  InputNumber,
  message,
  Modal,
  Popconfirm,
  Select,
  Space,
  Switch,
  Table,
  Tag,
  Typography,
} from 'antd'
import {
  changeExternalCourseStatus,
  createExternalCourse,
  deleteExternalCourse,
  getExternalCourseList,
  getExternalCourseSkillOptions,
  updateExternalCourse,
} from '../../api/externalCourse'

const { Title, Text } = Typography
const { TextArea } = Input

const LEVEL_OPTIONS = [
  { value: '入门', label: '入门' },
  { value: '中级', label: '中级' },
  { value: '高级', label: '高级' },
]

const LEVEL_COLORS = { 入门: 'green', 中级: 'blue', 高级: 'purple' }

/** 管理端：学生端「课程与专项 → 外部精选」的维护页。 */
function ExternalCourseManage() {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [skillOptions, setSkillOptions] = useState([])
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [saving, setSaving] = useState(false)
  const [form] = Form.useForm()

  const loadRows = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getExternalCourseList()
      setRows(Array.isArray(res?.data) ? res.data : [])
    } catch (error) {
      message.error(error.message || '外部精选课程加载失败')
    } finally {
      setLoading(false)
    }
  }, [])

  const loadSkills = useCallback(async () => {
    try {
      const res = await getExternalCourseSkillOptions()
      const list = Array.isArray(res?.data) ? res.data : []
      setSkillOptions(list.map((item) => ({
        value: item.id,
        label: item.category ? `${item.name}（${item.category}）` : item.name,
      })))
    } catch (error) {
      message.error(error.message || '技能列表加载失败')
    }
  }, [])

  useEffect(() => {
    loadSkills()
    loadRows()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({ free: true, sortOrder: 0, status: 'ACTIVE', skillIds: [] })
    setModalOpen(true)
  }

  const openEdit = (record) => {
    setEditing(record)
    form.setFieldsValue({
      title: record.title,
      provider: record.provider,
      url: record.url,
      description: record.description,
      level: record.level,
      free: record.free !== false,
      sortOrder: record.sortOrder ?? 0,
      status: record.status || 'ACTIVE',
      skillIds: record.skillIds || [],
    })
    setModalOpen(true)
  }

  const submit = async () => {
    let values
    try {
      values = await form.validateFields()
    } catch {
      return
    }
    setSaving(true)
    try {
      if (editing) {
        await updateExternalCourse(editing.id, values)
      } else {
        await createExternalCourse(values)
      }
      message.success(editing ? '已保存' : '已创建')
      setModalOpen(false)
      await loadRows()
    } catch (error) {
      message.error(error.message || '保存失败')
    } finally {
      setSaving(false)
    }
  }

  const toggleStatus = async (record) => {
    const next = record.status === 'ACTIVE' ? 'OFFLINE' : 'ACTIVE'
    try {
      await changeExternalCourseStatus(record.id, next)
      message.success(next === 'ACTIVE' ? '已上架' : '已下架')
      await loadRows()
    } catch (error) {
      message.error(error.message || '状态更新失败')
    }
  }

  const remove = async (record) => {
    try {
      await deleteExternalCourse(record.id)
      message.success('已删除')
      await loadRows()
    } catch (error) {
      message.error(error.message || '删除失败')
    }
  }

  const columns = [
    {
      title: '课程', dataIndex: 'title', ellipsis: true,
      render: (value, record) => (
        <Space direction="vertical" size={0}>
          <span>{value}</span>
          <Text type="secondary" style={{ fontSize: 12 }}>{record.provider}</Text>
        </Space>
      ),
    },
    {
      title: '难度', dataIndex: 'level', width: 90,
      render: (value) => (value ? <Tag color={LEVEL_COLORS[value] || 'default'}>{value}</Tag> : <Text type="secondary">未标注</Text>),
    },
    {
      title: '费用', dataIndex: 'free', width: 90,
      render: (value) => (value === false ? <Tag color="gold">付费</Tag> : <Tag color="green">免费</Tag>),
    },
    {
      title: '关联技能', dataIndex: 'skills', width: 260,
      render: (value) => {
        const list = value || []
        if (!list.length) return <Text type="secondary">未打标签</Text>
        const shown = list.slice(0, 3)
        return (
          <Space size={4} wrap>
            {shown.map((name) => <Tag key={name}>{name}</Tag>)}
            {list.length > shown.length ? <Tag>+{list.length - shown.length}</Tag> : null}
          </Space>
        )
      },
    },
    {
      title: '状态', dataIndex: 'status', width: 90,
      render: (value) => (value === 'OFFLINE' ? <Tag>已下架</Tag> : <Tag color="green">已上架</Tag>),
    },
    { title: '排序', dataIndex: 'sortOrder', width: 70 },
    {
      title: '操作', width: 190, fixed: 'right',
      render: (_, record) => (
        <Space size={4}>
          <Button type="link" size="small" onClick={() => openEdit(record)}>编辑</Button>
          <Button type="link" size="small" onClick={() => toggleStatus(record)}>
            {record.status === 'ACTIVE' ? '下架' : '上架'}
          </Button>
          <Popconfirm title="删除后学生端不再展示，确定吗？" onConfirm={() => remove(record)}>
            <Button type="link" size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div style={{ padding: 24 }}>
      <Space style={{ marginBottom: 16, justifyContent: 'space-between', width: '100%' }}>
        <Space direction="vertical" size={0}>
          <Title level={4} style={{ margin: 0 }}>外部精选资源</Title>
          <Text type="secondary">对应学生端「学习实践 → 课程与专项」里的外部课程，只保存标题、来源和官方链接。</Text>
        </Space>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={loadRows}>刷新</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新建外部课程</Button>
        </Space>
      </Space>

      <Card bodyStyle={{ padding: 0 }}>
        <Table
          rowKey="id"
          columns={columns}
          dataSource={rows}
          loading={loading}
          scroll={{ x: 1100 }}
          pagination={{ pageSize: 10, showSizeChanger: true, showTotal: (total) => `共 ${total} 条` }}
        />
      </Card>

      <Modal
        title={editing ? '编辑外部课程' : '新建外部课程'}
        open={modalOpen}
        onOk={submit}
        confirmLoading={saving}
        onCancel={() => setModalOpen(false)}
        width={640}
        destroyOnClose
      >
        <Form form={form} layout="vertical">
          <Form.Item name="title" label="课程标题" rules={[{ required: true, message: '请填写课程标题' }]}>
            <Input placeholder="例：Python 入门到实战" maxLength={160} />
          </Form.Item>
          <Form.Item name="provider" label="来源平台" rules={[{ required: true, message: '请填写来源平台' }]}>
            <Input placeholder="例：B 站 / 菜鸟教程 / 官方文档" maxLength={80} />
          </Form.Item>
          <Form.Item name="url" label="官方学习链接" rules={[{ required: true, message: '请填写学习链接' }]}>
            <Input placeholder="https://..." maxLength={500} />
          </Form.Item>
          <Form.Item name="description" label="简介">
            <TextArea rows={3} maxLength={600} showCount placeholder="这门课适合谁、学什么" />
          </Form.Item>
          <Space size={16} style={{ display: 'flex' }} align="start">
            <Form.Item name="level" label="难度" style={{ width: 160 }}>
              <Select allowClear options={LEVEL_OPTIONS} placeholder="选择难度" />
            </Form.Item>
            <Form.Item name="free" label="免费" valuePropName="checked" style={{ width: 120 }}>
              <Switch checkedChildren="免费" unCheckedChildren="付费" />
            </Form.Item>
            <Form.Item name="sortOrder" label="排序" style={{ width: 120 }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="status" label="状态" style={{ width: 140 }}>
              <Select options={[{ value: 'ACTIVE', label: '上架' }, { value: 'OFFLINE', label: '下架' }]} />
            </Form.Item>
          </Space>
          <Form.Item name="skillIds" label="关联技能（决定推荐时匹配哪些岗位）">
            <Select mode="multiple" allowClear showSearch optionFilterProp="label" options={skillOptions} placeholder="可选，不选则不参与岗位匹配" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  )
}

export default ExternalCourseManage
