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
  Tabs,
  Tag,
  Typography,
} from 'antd'
import {
  changeProjectStatus,
  createProject,
  createRequirement,
  createSkill,
  deleteProject,
  deleteRequirement,
  deleteSkill,
  getProjectList,
  getRequirementList,
  getSkillList,
  getSkillOptions,
  getTaxonomyJobs,
  updateProject,
  updateRequirement,
  updateSkill,
} from '../../api/learningTaxonomy'

const { Title, Text } = Typography
const { TextArea } = Input

const DIFFICULTY_META = {
  BEGINNER: { label: '入门', color: 'green' },
  INTERMEDIATE: { label: '中级', color: 'blue' },
  ADVANCED: { label: '高级', color: 'purple' },
}

const DIFFICULTY_OPTIONS = Object.entries(DIFFICULTY_META).map(([value, meta]) => ({
  value,
  label: meta.label,
}))

const asList = (response) => {
  const payload = response?.data
  if (Array.isArray(payload)) return payload
  if (Array.isArray(payload?.records)) return payload.records
  return []
}

function useSkillOptions() {
  const [skillOptions, setSkillOptions] = useState([])
  const load = useCallback(async () => {
    try {
      const list = asList(await getSkillOptions())
      setSkillOptions(list.map((item) => ({
        value: item.id,
        label: item.category ? `${item.name}（${item.category}）` : item.name,
      })))
    } catch (error) {
      message.error(error.message || '技能列表加载失败')
    }
  }, [])
  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])
  return skillOptions
}

// ---------------- 技能字典 ----------------
function SkillTab() {
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [saving, setSaving] = useState(false)
  const [form] = Form.useForm()

  const load = useCallback(async () => {
    setLoading(true)
    try {
      setRows(asList(await getSkillList()))
    } catch (error) {
      message.error(error.message || '技能字典加载失败')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({ sortOrder: 0, status: 'ACTIVE' })
    setModalOpen(true)
  }

  const openEdit = (record) => {
    setEditing(record)
    form.setFieldsValue({
      code: record.code,
      name: record.name,
      category: record.category,
      description: record.description,
      sortOrder: record.sortOrder ?? 0,
      status: record.status || 'ACTIVE',
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
        await updateSkill(editing.id, values)
      } else {
        await createSkill(values)
      }
      message.success(editing ? '已保存' : '已创建')
      setModalOpen(false)
      await load()
    } catch (error) {
      message.error(error.message || '保存失败')
    } finally {
      setSaving(false)
    }
  }

  const remove = async (record) => {
    try {
      await deleteSkill(record.id)
      message.success('已删除')
      await load()
    } catch (error) {
      message.error(error.message || '删除失败')
    }
  }

  const columns = [
    { title: '技能编码', dataIndex: 'code', width: 160 },
    { title: '技能名称', dataIndex: 'name', width: 180 },
    { title: '分类', dataIndex: 'category', width: 130, render: (value) => (value ? <Tag>{value}</Tag> : '-') },
    { title: '被岗位要求引用', dataIndex: 'jobRequirementCount', width: 140 },
    { title: '被内容引用', dataIndex: 'contentUsageCount', width: 120 },
    {
      title: '状态', dataIndex: 'status', width: 90,
      render: (value) => (value === 'INACTIVE' ? <Tag>已停用</Tag> : <Tag color="green">启用</Tag>),
    },
    { title: '排序', dataIndex: 'sortOrder', width: 70 },
    {
      title: '操作', width: 140, fixed: 'right',
      render: (_, record) => (
        <Space size={4}>
          <Button type="link" size="small" onClick={() => openEdit(record)}>编辑</Button>
          <Popconfirm title="删除前请确认已无岗位/内容引用" onConfirm={() => remove(record)}>
            <Button type="link" size="small" danger>删除</Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <Space style={{ marginBottom: 12, justifyContent: 'space-between', width: '100%' }}>
        <Text type="secondary">技能字典是推荐的基础：课程、题目、项目、岗位要求都挂在这里。</Text>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={load}>刷新</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新增技能</Button>
        </Space>
      </Space>
      <Table rowKey="id" columns={columns} dataSource={rows} loading={loading} scroll={{ x: 1050 }}
        pagination={{ pageSize: 15, showSizeChanger: true, showTotal: (total) => `共 ${total} 项` }} />
      <Modal title={editing ? '编辑技能' : '新增技能'} open={modalOpen} onOk={submit}
        confirmLoading={saving} onCancel={() => setModalOpen(false)} width={560} destroyOnClose>
        <Form form={form} layout="vertical">
          <Form.Item name="code" label="技能编码" rules={[{ required: true, message: '请填写编码' }]}>
            <Input placeholder="例：python-basics" maxLength={80} />
          </Form.Item>
          <Form.Item name="name" label="技能名称" rules={[{ required: true, message: '请填写名称' }]}>
            <Input placeholder="例：Python 基础" maxLength={120} />
          </Form.Item>
          <Form.Item name="category" label="分类">
            <Input placeholder="例：编程语言" maxLength={80} />
          </Form.Item>
          <Form.Item name="description" label="说明">
            <TextArea rows={3} maxLength={500} showCount />
          </Form.Item>
          <Space size={16} style={{ display: 'flex' }} align="start">
            <Form.Item name="sortOrder" label="排序" style={{ width: 140 }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="status" label="状态" style={{ width: 160 }}>
              <Select options={[{ value: 'ACTIVE', label: '启用' }, { value: 'INACTIVE', label: '停用' }]} />
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </div>
  )
}

// ---------------- 岗位技能要求 ----------------
function RequirementTab() {
  const skillOptions = useSkillOptions()
  const [jobs, setJobs] = useState([])
  const [jobCode, setJobCode] = useState()
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [saving, setSaving] = useState(false)
  const [form] = Form.useForm()

  const loadJobs = useCallback(async () => {
    try {
      setJobs(asList(await getTaxonomyJobs()))
    } catch (error) {
      message.error(error.message || '岗位列表加载失败')
    }
  }, [])

  const load = useCallback(async () => {
    setLoading(true)
    try {
      setRows(asList(await getRequirementList(jobCode)))
    } catch (error) {
      message.error(error.message || '岗位技能要求加载失败')
    } finally {
      setLoading(false)
    }
  }, [jobCode])

  useEffect(() => {
    loadJobs()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [jobCode])

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    const current = jobs.find((item) => item.code === jobCode)
    form.setFieldsValue({
      jobCode,
      jobName: current?.name,
      requiredLevel: 60,
      importance: 0.5,
      requiredFlag: true,
      sortOrder: rows.length + 1,
    })
    setModalOpen(true)
  }

  const openEdit = (record) => {
    setEditing(record)
    form.setFieldsValue({
      jobCode: record.jobCode,
      jobName: record.jobName,
      skillId: record.skillId,
      requiredLevel: record.requiredLevel ?? 60,
      importance: record.importance ?? 0.5,
      requiredFlag: record.requiredFlag !== false,
      sortOrder: record.sortOrder ?? 0,
    })
    setModalOpen(true)
  }

  const onJobCodeChange = (value) => {
    const matched = jobs.find((item) => item.code === value)
    if (matched) form.setFieldsValue({ jobName: matched.name })
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
        await updateRequirement(editing.id, values)
      } else {
        await createRequirement(values)
      }
      message.success(editing ? '已保存' : '已新增')
      setModalOpen(false)
      await Promise.all([load(), loadJobs()])
    } catch (error) {
      message.error(error.message || '保存失败')
    } finally {
      setSaving(false)
    }
  }

  const remove = async (record) => {
    try {
      await deleteRequirement(record.id)
      message.success('已删除')
      await Promise.all([load(), loadJobs()])
    } catch (error) {
      message.error(error.message || '删除失败')
    }
  }

  const columns = [
    { title: '岗位', dataIndex: 'jobName', width: 200, render: (value, record) => (
      <Space direction="vertical" size={0}><span>{value}</span><Text type="secondary" style={{ fontSize: 12 }}>{record.jobCode}</Text></Space>
    ) },
    { title: '技能', dataIndex: 'skillName', width: 180, render: (value, record) => (
      <Space direction="vertical" size={0}><span>{value || '未知技能'}</span><Text type="secondary" style={{ fontSize: 12 }}>{record.skillCode}</Text></Space>
    ) },
    { title: '目标等级', dataIndex: 'requiredLevel', width: 100 },
    { title: '重要度', dataIndex: 'importance', width: 100 },
    { title: '必备', dataIndex: 'requiredFlag', width: 90, render: (value) => (value === false ? <Tag>选修</Tag> : <Tag color="red">必备</Tag>) },
    { title: '排序', dataIndex: 'sortOrder', width: 70 },
    { title: '操作', width: 140, fixed: 'right', render: (_, record) => (
      <Space size={4}>
        <Button type="link" size="small" onClick={() => openEdit(record)}>编辑</Button>
        <Popconfirm title="删除后该岗位不再要求此技能，确定吗？" onConfirm={() => remove(record)}>
          <Button type="link" size="small" danger>删除</Button>
        </Popconfirm>
      </Space>
    ) },
  ]

  return (
    <div>
      <Space style={{ marginBottom: 12, justifyContent: 'space-between', width: '100%' }} wrap>
        <Space wrap>
          <Text type="secondary">岗位要求决定「推荐学习」算出什么待补技能。</Text>
          <Select
            allowClear
            placeholder="按岗位筛选"
            style={{ width: 220 }}
            value={jobCode}
            onChange={setJobCode}
            options={jobs.map((job) => ({ value: job.code, label: `${job.name}（${job.skillCount} 项）` }))}
          />
        </Space>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={load}>刷新</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新增岗位要求</Button>
        </Space>
      </Space>
      <Table rowKey="id" columns={columns} dataSource={rows} loading={loading} scroll={{ x: 1000 }}
        pagination={{ pageSize: 15, showSizeChanger: true, showTotal: (total) => `共 ${total} 项` }} />
      <Modal title={editing ? '编辑岗位要求' : '新增岗位要求'} open={modalOpen} onOk={submit}
        confirmLoading={saving} onCancel={() => setModalOpen(false)} width={560} destroyOnClose>
        <Form form={form} layout="vertical">
          <Form.Item name="jobCode" label="岗位编码" rules={[{ required: true, message: '请填写或选择岗位编码' }]}>
            <Select
              showSearch
              optionFilterProp="label"
              onChange={onJobCodeChange}
              placeholder="选择已有岗位"
              options={jobs.map((job) => ({ value: job.code, label: `${job.name} / ${job.code}` }))}
            />
          </Form.Item>
          <Form.Item name="jobName" label="岗位名称" rules={[{ required: true, message: '请填写岗位名称' }]}>
            <Input placeholder="例：Python 开发工程师" maxLength={120} />
          </Form.Item>
          <Form.Item name="skillId" label="技能" rules={[{ required: true, message: '请选择技能' }]}>
            <Select showSearch optionFilterProp="label" options={skillOptions} placeholder="选择技能" />
          </Form.Item>
          <Space size={16} style={{ display: 'flex' }} align="start">
            <Form.Item name="requiredLevel" label="目标等级" style={{ width: 140 }}>
              <InputNumber min={0} max={100} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="importance" label="重要度" style={{ width: 140 }}>
              <InputNumber min={0} max={1} step={0.05} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="requiredFlag" label="必备" valuePropName="checked" style={{ width: 120 }}>
              <Switch checkedChildren="必备" unCheckedChildren="选修" />
            </Form.Item>
            <Form.Item name="sortOrder" label="排序" style={{ width: 120 }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </div>
  )
}

// ---------------- 岗位实战任务 ----------------
function ProjectTab() {
  const skillOptions = useSkillOptions()
  const [rows, setRows] = useState([])
  const [loading, setLoading] = useState(false)
  const [modalOpen, setModalOpen] = useState(false)
  const [editing, setEditing] = useState(null)
  const [saving, setSaving] = useState(false)
  const [form] = Form.useForm()

  const load = useCallback(async () => {
    setLoading(true)
    try {
      setRows(asList(await getProjectList()))
    } catch (error) {
      message.error(error.message || '岗位实战任务加载失败')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const openCreate = () => {
    setEditing(null)
    form.resetFields()
    form.setFieldsValue({ difficulty: 'BEGINNER', sortOrder: 0, status: 'ACTIVE', skillIds: [] })
    setModalOpen(true)
  }

  const openEdit = (record) => {
    setEditing(record)
    form.setFieldsValue({
      code: record.code,
      title: record.title,
      summary: record.summary,
      objective: record.objective,
      deliverable: record.deliverable,
      difficulty: record.difficulty || 'BEGINNER',
      estimatedHours: record.estimatedHours,
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
        await updateProject(editing.id, values)
      } else {
        await createProject(values)
      }
      message.success(editing ? '已保存' : '已创建')
      setModalOpen(false)
      await load()
    } catch (error) {
      message.error(error.message || '保存失败')
    } finally {
      setSaving(false)
    }
  }

  const toggleStatus = async (record) => {
    const next = record.status === 'ACTIVE' ? 'OFFLINE' : 'ACTIVE'
    try {
      await changeProjectStatus(record.id, next)
      message.success(next === 'ACTIVE' ? '已上架' : '已下架')
      await load()
    } catch (error) {
      message.error(error.message || '状态更新失败')
    }
  }

  const remove = async (record) => {
    try {
      await deleteProject(record.id)
      message.success('已删除')
      await load()
    } catch (error) {
      message.error(error.message || '删除失败')
    }
  }

  const columns = [
    { title: '编码', dataIndex: 'code', width: 160 },
    { title: '任务标题', dataIndex: 'title', width: 220, ellipsis: true },
    { title: '难度', dataIndex: 'difficulty', width: 90, render: (value) => {
      const meta = DIFFICULTY_META[value]
      return meta ? <Tag color={meta.color}>{meta.label}</Tag> : '-'
    } },
    { title: '关联技能', dataIndex: 'skills', width: 240, render: (value) => {
      const list = value || []
      if (!list.length) return <Text type="secondary">未打标签</Text>
      const shown = list.slice(0, 3)
      return (
        <Space size={4} wrap>
          {shown.map((name) => <Tag key={name}>{name}</Tag>)}
          {list.length > shown.length ? <Tag>+{list.length - shown.length}</Tag> : null}
        </Space>
      )
    } },
    { title: '对应岗位', dataIndex: 'jobs', width: 200, render: (value) => {
      const list = value || []
      return list.length ? list.slice(0, 2).join('、') : <Text type="secondary">-</Text>
    } },
    { title: '状态', dataIndex: 'status', width: 90, render: (value) => (value === 'OFFLINE' ? <Tag>已下架</Tag> : <Tag color="green">已上架</Tag>) },
    { title: '排序', dataIndex: 'sortOrder', width: 70 },
    { title: '操作', width: 190, fixed: 'right', render: (_, record) => (
      <Space size={4}>
        <Button type="link" size="small" onClick={() => openEdit(record)}>编辑</Button>
        <Button type="link" size="small" onClick={() => toggleStatus(record)}>
          {record.status === 'ACTIVE' ? '下架' : '上架'}
        </Button>
        <Popconfirm title="删除后学生端不再推荐，确定吗？" onConfirm={() => remove(record)}>
          <Button type="link" size="small" danger>删除</Button>
        </Popconfirm>
      </Space>
    ) },
  ]

  return (
    <div>
      <Space style={{ marginBottom: 12, justifyContent: 'space-between', width: '100%' }}>
        <Text type="secondary">岗位实战任务对应学生端「项目实训」，打上技能标签后会随目标岗位一起推荐。</Text>
        <Space>
          <Button icon={<ReloadOutlined />} onClick={load}>刷新</Button>
          <Button type="primary" icon={<PlusOutlined />} onClick={openCreate}>新建实战任务</Button>
        </Space>
      </Space>
      <Table rowKey="id" columns={columns} dataSource={rows} loading={loading} scroll={{ x: 1250 }}
        pagination={{ pageSize: 10, showSizeChanger: true, showTotal: (total) => `共 ${total} 项` }} />
      <Modal title={editing ? '编辑实战任务' : '新建实战任务'} open={modalOpen} onOk={submit}
        confirmLoading={saving} onCancel={() => setModalOpen(false)} width={640} destroyOnClose>
        <Form form={form} layout="vertical">
          <Space size={16} style={{ display: 'flex' }} align="start">
            <Form.Item name="code" label="任务编码" rules={[{ required: true, message: '请填写编码' }]} style={{ width: 260 }}>
              <Input placeholder="例：proj-campus-api" maxLength={80} />
            </Form.Item>
            <Form.Item name="difficulty" label="难度" style={{ width: 160 }}>
              <Select options={DIFFICULTY_OPTIONS} />
            </Form.Item>
            <Form.Item name="estimatedHours" label="预计小时" style={{ width: 140 }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
          </Space>
          <Form.Item name="title" label="任务标题" rules={[{ required: true, message: '请填写标题' }]}>
            <Input placeholder="例：校园服务接口实训" maxLength={160} />
          </Form.Item>
          <Form.Item name="summary" label="简介">
            <TextArea rows={2} maxLength={500} showCount />
          </Form.Item>
          <Form.Item name="objective" label="任务目标">
            <TextArea rows={2} maxLength={500} showCount placeholder="做完要达到什么效果" />
          </Form.Item>
          <Form.Item name="deliverable" label="交付物">
            <Input placeholder="例：可运行的接口代码 + 接口文档" maxLength={300} />
          </Form.Item>
          <Form.Item name="skillIds" label="关联技能">
            <Select mode="multiple" allowClear showSearch optionFilterProp="label" options={skillOptions} placeholder="选择该任务要练的技能" />
          </Form.Item>
          <Space size={16} style={{ display: 'flex' }} align="start">
            <Form.Item name="sortOrder" label="排序" style={{ width: 140 }}>
              <InputNumber min={0} style={{ width: '100%' }} />
            </Form.Item>
            <Form.Item name="status" label="状态" style={{ width: 160 }}>
              <Select options={[{ value: 'ACTIVE', label: '上架' }, { value: 'OFFLINE', label: '下架' }]} />
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </div>
  )
}

/** 管理端：学生端「推荐学习」依赖的基础配置。 */
function LearningTaxonomyManage() {
  return (
    <div style={{ padding: 24 }}>
      <Space direction="vertical" size={0} style={{ marginBottom: 12 }}>
        <Title level={4} style={{ margin: 0 }}>学习内容配置</Title>
        <Text type="secondary">这里维护的技能字典、岗位技能要求与岗位实战任务，直接决定学生端「推荐学习」算出什么。</Text>
      </Space>
      <Tabs
        items={[
          { key: 'skills', label: '技能字典', children: <SkillTab /> },
          { key: 'requirements', label: '岗位技能要求', children: <RequirementTab /> },
          { key: 'projects', label: '岗位实战任务', children: <ProjectTab /> },
        ]}
      />
    </div>
  )
}

export default LearningTaxonomyManage
