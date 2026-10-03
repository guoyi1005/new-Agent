import { BrowserRouter as Router, Navigate, Route, Routes } from 'react-router-dom'
import Layout from './components/Layout/Layout'
import { allNavItems } from './data/portalData'
import Home from './pages/Home/Home'
import QuestionBank from './pages/ai/QuestionBank/QuestionBank'
import KnowledgeChat from './pages/ai/KnowledgeChat/KnowledgeChat'
import KnowledgeManage from './pages/ai/KnowledgeManage/KnowledgeManage'
import ParagraphManage from './pages/ai/KnowledgeManage/ParagraphManage'
import ProfileRules from './pages/ai/ProfileRules/ProfileRules'
import RagManage from './pages/ai/RagManage/RagManage'
import AgentSettings from './pages/ai/AgentSettings/AgentSettings'
import AgentCache from './pages/ai/AgentCache/AgentCache'
import Observability from './pages/ai/Observability/Observability'

import CodeCanvas from './pages/ai/CodeCanvas/CodeCanvas'

import ToolMonitor from './pages/ai/ToolMonitor/ToolMonitor'

import AiConversation from './pages/ai/AiConversation/AiConversation'
import Login from './pages/Login/Login'
import ExamPaperCreatePage from './pages/questionBank/ExamPaperCreatePage'
import ExamPaperHistoryPage from './pages/questionBank/ExamPaperHistoryPage'
import QuestionBankGeneratePage from './pages/questionBank/QuestionBankGeneratePage'
import { QUESTION_BANK_ROUTES } from './pages/questionBank/questionBankRoutes'
import WorkspacePage from './pages/workspace/WorkspacePage'
import CampusCourseManage from './pages/learning/CampusCourseManage'
import CampusRecruitmentManage from './pages/employment/CampusRecruitmentManage'
import AlumniEnterpriseManage from './pages/employment/AlumniEnterpriseManage'
import CareerNebulaManage from './pages/careerNebula/CareerNebulaManage'
import PythonProblemManage from './pages/learning/PythonProblemManage'
import InterviewManage from './pages/interview/InterviewManage'
import './App.css'

function App() {
  // 带 pageKey 的菜单统一走通用 WorkspacePage
  const workspaceRoutes = allNavItems
    .filter((item) => item.pageKey)
    .map((item) => (
      <Route
        key={item.path}
        path={item.path}
        element={<WorkspacePage pageKey={item.pageKey} />}
      />
    ))

  return (
    <Router>
      <Routes>
        <Route path="/" element={<Login />} />
        <Route element={<Layout />}>
          <Route path="/home" element={<Home />} />
          <Route path="/ai" element={<AiConversation />} />
          <Route path="/ai/rag" element={<Navigate to="/ai/rag/agents" replace />} />
          <Route path="/ai/rag/strategy" element={<Navigate to="/ai/rag/agents" replace />} />
          <Route path="/ai/rag/agents" element={<RagManage page="agents" />} />
          <Route path="/ai/agent-settings" element={<AgentSettings />} />
          <Route path="/ai/agent-cache" element={<AgentCache />} />
          <Route path="/ai/tool-monitor" element={<ToolMonitor />} />
          <Route path="/ai/observability" element={<Observability />} />
          <Route path="/ai/code-canvas" element={<CodeCanvas />} />
          <Route path={QUESTION_BANK_ROUTES.questions} element={<QuestionBank />} />
          <Route path={QUESTION_BANK_ROUTES.generate} element={<QuestionBankGeneratePage />} />
          <Route path={QUESTION_BANK_ROUTES.createPaper} element={<ExamPaperCreatePage />} />
          <Route path={QUESTION_BANK_ROUTES.paperHistory} element={<ExamPaperHistoryPage />} />
          <Route path="/ai/question-bank" element={<Navigate to={QUESTION_BANK_ROUTES.questions} replace />} />
          <Route path="/ai/exam-papers" element={<Navigate to={QUESTION_BANK_ROUTES.createPaper} replace />} />
          <Route path="/ai/knowledge" element={<KnowledgeManage />} />
          <Route path="/admin/knowledge-chat" element={<KnowledgeChat />} />
          <Route path="/ai/knowledge/paragraph/:knowledgeId/:documentId" element={<ParagraphManage />} />
          <Route path="/admin/paragraph/:knowledgeId/:documentId" element={<ParagraphManage />} />
          <Route path="/ai/profile-rules" element={<ProfileRules />} />
          <Route path="/learning/courses" element={<CampusCourseManage />} />
          <Route path="/employment/campus-recruitment" element={<CampusRecruitmentManage />} />
          <Route path="/employment/alumni" element={<AlumniEnterpriseManage />} />
          <Route path="/career/nebula" element={<CareerNebulaManage />} />
          <Route path="/learning/python-problems" element={<PythonProblemManage />} />
          <Route path="/interview/manage" element={<InterviewManage />} />
          {workspaceRoutes}
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </Router>
  )
}

export default App
