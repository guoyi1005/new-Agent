import { useCallback, useEffect, useState } from 'react'
import { ReloadOutlined } from '@ant-design/icons'
import { Button, Card, Col, Empty, message, Row, Space, Spin, Table, Tag, Typography } from 'antd'
import { getLearningStatsOverview } from '../../api/learningStats'

const { Title, Text } = Typography

const SOURCE_LABELS = {
  COURSE: '校内课程',
  COURSE_CHAPTER: '课程章节',
  PROBLEM: '算法题',
  PROJECT: '项目实训',
  PROJECT_TASK: '岗位实战任务',
  PATH: '学习路径',
  EXTERNAL_COURSE: '外部精选',
}

const ACTION_LABELS = {
  COURSE_PROGRESS: '课程学习进度',
  COURSE_ENROLL: '加入课程',
  COURSE_CHAPTER_COMPLETED: '完成章节',
  PROBLEM_SOLVED: '通过题目',
  PROJECT_TASK_COMPLETED: '完成实战任务',
  PATH_ITEM_COMPLETED: '完成学习节点',
  EXAM_SUBMITTED: '交卷',
}

const SOURCE_COLORS = {
  COURSE: 'blue',
  COURSE_CHAPTER: 'geekblue',
  PROBLEM: 'green',
  PROJECT: 'purple',
  PROJECT_TASK: 'purple',
  PATH: 'gold',
  EXTERNAL_COURSE: 'cyan',
}

function StatCard({ label, value, suffix, hint }) {
  return (
    <Card size="small" bodyStyle={{ padding: 16 }}>
      <Text type="secondary" style={{ fontSize: 13 }}>{label}</Text>
      <div style={{ fontSize: 26, fontWeight: 600, lineHeight: 1.4 }}>
        {value}<span style={{ fontSize: 14, fontWeight: 400, marginLeft: 4 }}>{suffix}</span>
      </div>
      {hint ? <Text type="secondary" style={{ fontSize: 12 }}>{hint}</Text> : null}
    </Card>
  )
}

/** 管理端：学生学习行为的只读汇总。 */
function LearningStatsDashboard() {
  const [data, setData] = useState(null)
  const [loading, setLoading] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)
    try {
      const res = await getLearningStatsOverview()
      setData(res?.data || null)
    } catch (error) {
      message.error(error.message || '学习数据加载失败')
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    load()
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const sourceRows = (data?.sourceBreakdown || []).map((item) => ({
    ...item,
    key: item.sourceType,
    label: SOURCE_LABELS[item.sourceType] || item.sourceType,
  }))

  const actionRows = Object.entries(data?.actionBreakdown || {}).map(([actionType, count]) => ({
    key: actionType,
    actionType,
    label: ACTION_LABELS[actionType] || actionType,
    count,
  }))

  const sourceColumns = [
    { title: '来源', dataIndex: 'label', render: (value, record) => <Tag color={SOURCE_COLORS[record.sourceType] || 'default'}>{value}</Tag> },
    { title: '学习记录数', dataIndex: 'recordCount', width: 140 },
    { title: '涉及学生数', dataIndex: 'studentCount', width: 140 },
  ]

  const actionColumns = [
    { title: '行为', dataIndex: 'label' },
    { title: '次数', dataIndex: 'count', width: 140 },
  ]

  if (loading && !data) {
    return (
      <div style={{ padding: 48, textAlign: 'center' }}>
        <Spin />
      </div>
    )
  }

  if (!data) {
    return (
      <div style={{ padding: 24 }}>
        <Empty description="暂无学习数据" />
      </div>
    )
  }

  return (
    <div style={{ padding: 24 }}>
      <Space style={{ marginBottom: 16, justifyContent: 'space-between', width: '100%' }}>
        <Space direction="vertical" size={0}>
          <Title level={4} style={{ margin: 0 }}>学习数据看板</Title>
          <Text type="secondary">汇总学生在课程、算法题、项目与考试上的实际学习行为。</Text>
        </Space>
        <Button icon={<ReloadOutlined />} loading={loading} onClick={load}>刷新</Button>
      </Space>

      <Row gutter={[12, 12]} style={{ marginBottom: 16 }}>
        <Col xs={24} sm={12} lg={6}>
          <StatCard label="学习记录总数" value={data.learningRecordCount} suffix="条"
            hint={`涉及 ${data.activeStudentCount} 位学生`} />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard label="课程报名" value={data.enrollmentCount} suffix="条"
            hint={`涉及 ${data.enrolledStudentCount} 位学生`} />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard label="考试作答" value={data.examAttemptCount} suffix="次"
            hint={`平均得分率 ${data.averageExamScoreRate}%`} />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatCard label="学习目标" value={data.studyGoalCount} suffix="个"
            hint={`进行中 ${data.studyGoalInProgress} · 已完成 ${data.studyGoalCompleted} · 平均 ${data.averageGoalProgress}%`} />
        </Col>
      </Row>

      <Row gutter={[12, 12]}>
        <Col xs={24} lg={12}>
          <Card size="small" title="学习记录来源分布">
            <Table rowKey="key" columns={sourceColumns} dataSource={sourceRows} pagination={false} size="small"
              locale={{ emptyText: '暂无学习记录' }} />
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card size="small" title="学习行为分布">
            <Table rowKey="key" columns={actionColumns} dataSource={actionRows} pagination={false} size="small"
              locale={{ emptyText: '暂无学习行为' }} />
          </Card>
        </Col>
      </Row>
    </div>
  )
}

export default LearningStatsDashboard
