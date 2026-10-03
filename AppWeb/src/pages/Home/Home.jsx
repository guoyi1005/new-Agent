import { useCallback, useEffect, useMemo, useState } from 'react'
import { Button, Card, Skeleton } from 'antd'
import { ReloadOutlined, RightOutlined } from '@ant-design/icons'
import { useNavigate } from 'react-router-dom'
import { getUserList } from '../../api/user'
import { QUESTION_BANK_ROUTES } from '../questionBank/questionBankRoutes'
import './Home.css'

const formatNumber = (value) => {
  const numeric = Number(value)
  if (!Number.isFinite(numeric)) return '-'
  return new Intl.NumberFormat('zh-CN').format(numeric)
}

function Home() {
  const navigate = useNavigate()
  const [loading, setLoading] = useState(true)
  const [updatedAt, setUpdatedAt] = useState('')
  const [totalUsers, setTotalUsers] = useState(0)

  const loadDashboard = useCallback(async () => {
    setLoading(true)
    try {
      const userRes = await getUserList({ page: 1, size: 1 }).catch(() => null)
      setTotalUsers(userRes?.data?.total || 0)
      setUpdatedAt(new Date().toLocaleString('zh-CN', { hour12: false }))
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    loadDashboard()
  }, [loadDashboard])

  const quickLinks = useMemo(() => ([
    { label: '用户与角色', desc: '账号、角色与状态管理', route: '/user/manage' },
    { label: '会议历史', desc: '转写记录与会议智能体结果', route: '/meeting/history' },
    { label: '语音模型配置', desc: '会议语音转写模型维护', route: '/meeting/voice-model' },
    { label: '校园课程管理', desc: '课程内容维护', route: '/learning/courses' },
    { label: 'Python 题库管理', desc: 'Python 题目维护', route: '/learning/python-problems' },
    { label: '题库', desc: '查看已导入的标准题库', route: QUESTION_BANK_ROUTES.questions },
    { label: '试卷生成', desc: '随机或手工组卷并下载 Word 试卷', route: QUESTION_BANK_ROUTES.createPaper },
    { label: '知识库管理', desc: '维护知识库账号与文档', route: '/ai/knowledge' },
    { label: '智能体设置', desc: '智能体开关、默认模型与运行边界', route: '/ai/agent-settings' },
    { label: '智能体测试', desc: '测试智能体调用与示例输入', route: '/ai/rag/agents' },
    { label: 'AI 面试配置', desc: '面试题库、岗位技术栈与面试配置', route: '/interview/manage' },
    { label: '岗位星图', desc: '岗位与能力发展的星图数据', route: '/career/nebula' },
  ]), [])

  return (
    <div className="home-container">
      <div className="home-toolbar">
        <div className="home-toolbar__actions">
          {updatedAt ? <span>更新于 {updatedAt}</span> : null}
          <Button icon={<ReloadOutlined />} loading={loading} onClick={loadDashboard}>刷新</Button>
        </div>
      </div>

      <section className="home-metric-grid home-metric-grid--primary">
        <Card className="home-metric-card home-metric-card--primary" styles={{ body: { padding: '18px 20px' } }}>
          {loading ? <Skeleton active paragraph={{ rows: 1 }} title={false} /> : (
            <>
              <span>用户总数</span>
              <strong>{formatNumber(totalUsers)}</strong>
              <em>注册账号规模</em>
            </>
          )}
        </Card>
      </section>

      <section className="home-quick-links">
        <div className="home-section-head">
          <h3>快捷入口</h3>
          <span>常用后台模块</span>
        </div>
        <div className="home-quick-links__grid">
          {quickLinks.map((item) => (
            <button key={item.route} type="button" className="home-quick-link" onClick={() => navigate(item.route)}>
              <strong>{item.label}</strong>
              <span>{item.desc}</span>
              <RightOutlined />
            </button>
          ))}
        </div>
      </section>
    </div>
  )
}

export default Home
