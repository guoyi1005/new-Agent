import { createRouter, createWebHistory } from 'vue-router'

import AiAssistantView from '../views/AiAssistantView.vue'
import AiToolsView from '../views/AiToolsView.vue'
import CampusActivitiesView from '../views/CampusActivitiesView.vue'
import HomeView from '../views/HomeView.vue'
import GrowthTreeView from '../views/GrowthTreeView.vue'
import LoginView from '../views/LoginView.vue'
import MapView from '../views/MapView.vue'
import MessageCenterView from '../views/MessageCenterView.vue'
import PythonLearningView from '../views/PythonLearningView.vue'
import PythonLearningShell from '../views/pythonOnline/PythonLearningShell.vue'
import PythonQuestionBankView from '../views/pythonOnline/PythonQuestionBankView.vue'
import PythonPracticeView from '../views/pythonOnline/PythonPracticeView.vue'
import KnowledgeGraphView from '../views/KnowledgeGraphView.vue'
import LearningResourceView from '../views/LearningResourceView.vue'
import LearningPracticeView from '../views/LearningPracticeView.vue'
import BusinessSandboxView from '../views/BusinessSandboxView.vue'
import BusinessSandboxRoomsView from '../views/BusinessSandboxRoomsView.vue'
import BusinessSandboxRoomView from '../views/BusinessSandboxRoomView.vue'
import CommunityView from '../views/CommunityView.vue'
import AiWritingView from '../views/aiStudio/AiWritingView.vue'
import AiImageView from '../views/aiStudio/AiImageView.vue'
import AiPresentationView from '../views/aiStudio/AiPresentationView.vue'
import AiMindMapView from '../views/aiStudio/AiMindMapView.vue'
import AiArchitectureView from '../views/aiStudio/AiArchitectureView.vue'
import AiFlowchartView from '../views/aiStudio/AiFlowchartView.vue'
import { AI_STUDIO_TOOL_IDS } from '../config/aiStudioTools'
import AiOriginalView from '../views/AiOriginalView.vue'
import WatermarkAddView from '../views/WatermarkAddView.vue'
import WatermarkBatchView from '../views/WatermarkBatchView.vue'
import WatermarkHistoryView from '../views/WatermarkHistoryView.vue'
import WatermarkHelpView from '../views/WatermarkHelpView.vue'
import ActivityPublishView from '../views/ActivityPublishView.vue'
import ActivitySignInView from '../views/ActivitySignInView.vue'
import ActivityDetailView from '../views/ActivityDetailView.vue'
import ActivitySignupView from '../views/ActivitySignupView.vue'
import AccountSettingsView from '../views/AccountSettingsView.vue'
import ProfileRadarView from '../views/ProfileRadarView.vue'
import MineActivitiesView from '../views/MineActivitiesView.vue'
import MineAiHistoryView from '../views/MineAiHistoryView.vue'
import ScheduleWorkspaceView from '../views/ScheduleWorkspaceView.vue'
import ScheduleSettingsWorkspaceView from '../views/ScheduleSettingsWorkspaceView.vue'
import MineView from '../views/MineView.vue'
import ExamPapersView from '../views/ExamPapersView.vue'
import ExamTakingView from '../views/ExamTakingView.vue'
import ExamResultView from '../views/ExamResultView.vue'
import ExamDetailView from '../views/ExamDetailView.vue'
import ExamHistoryView from '../views/ExamHistoryView.vue'
import PaperHistoryView from '../views/paper/PaperHistoryView.vue'
import PaperHomeView from '../views/paper/PaperHomeView.vue'
import ResumeView from '../views/ResumeView.vue'
import ResumeWorkspaceView from '../views/ResumeWorkspaceView.vue'
import ResumeDesigner from '../views/ResumeDesigner.vue'
import ResumeWizard from '../views/ResumeWizard.vue'
import CampusCourseView from '../views/CampusCourseView.vue'
import CampusDiscountView from '../views/CampusDiscountView.vue'
import DocumentConvertView from '../views/DocumentConvertView.vue'
import CareerNebulaView from '../views/CareerNebulaView.vue'
import CareerPlanetView from '../views/CareerPlanetView.vue'
import JobExplorationView from '../views/JobExplorationView.vue'
import JobDetailView from '../views/JobDetailView.vue'
import EmploymentView from '../views/EmploymentView.vue'
import AlumniEnterpriseView from '../views/AlumniEnterpriseView.vue'
import CampusRecruitmentView from '../views/CampusRecruitmentView.vue'
import SectionHubView from '../views/SectionHubView.vue'
import InterviewShell from '../views/interview/InterviewShell.vue'
import InterviewIndex from '../views/interview/pages/Index.vue'
import InterviewAiMockInterview from '../views/interview/pages/AiMockInterview.vue'
import InterviewAiConfig from '../views/interview/pages/AiConfig.vue'
import InterviewAiChat from '../views/interview/pages/AiChat.vue'
import InterviewAiCareerPlan from '../views/interview/pages/AiCareerPlan.vue'
import InterviewQuestionBank from '../views/interview/pages/QuestionBank.vue'
import InterviewAbilityImprovement from '../views/interview/pages/AbilityImprovement.vue'
import InterviewMy from '../views/interview/pages/My.vue'
import InterviewMyNote from '../views/interview/pages/MyNote.vue'
import InterviewWrongQuestionBook from '../views/interview/pages/WrongQuestionBook.vue'
import InterviewCreateQuestionBank from '../views/interview/pages/CreateQuestionBank.vue'
import InterviewMockQuestionFilter from '../views/interview/pages/MockQuestionFilter.vue'
import InterviewAIReport from '../views/interview/pages/AIReport.vue'
import { getToken } from '../utils/auth'

const routes = [
  { path: '/', redirect: '/home' },
  { path: '/login', name: 'login', component: LoginView, meta: { public: true } },
  { path: '/home', name: 'home', component: HomeView },
  { path: '/growth', name: 'growth-center', component: SectionHubView, meta: { section: 'growth' } },
  { path: '/career', name: 'career-exploration', component: JobExplorationView },
  { path: '/career/job/:jobId', name: 'career-job-detail', component: JobDetailView },
  { path: '/learning', name: 'learning-practice', component: LearningPracticeView },
  { path: '/employment', name: 'employment', component: EmploymentView },
  { path: '/employment/alumni', name: 'employment-alumni', component: AlumniEnterpriseView },
  { path: '/employment/campus-recruitment', name: 'employment-campus-recruitment', component: CampusRecruitmentView },
  { path: '/community', name: 'community', component: CommunityView, meta: { public: true } },
  { path: '/ai-career', name: 'ai-career', component: SectionHubView, meta: { section: 'aiCareer' } },
  { path: '/growth/campus-map', redirect: '/map' },
  { path: '/growth/campus-activity', redirect: '/activities' },
  { path: '/community/experience', redirect: { name: 'community', query: { category: 'experience' } } },
  { path: '/community/cases', redirect: { name: 'community', query: { category: 'cases' } } },
  { path: '/community/referrals', redirect: { name: 'community', query: { category: 'referrals' } } },
  { path: '/community/qa', redirect: { name: 'community', query: { category: 'qa' } } },
  { path: '/community/following', redirect: { name: 'community', query: { category: 'following' } } },
  { path: '/community/map', redirect: '/growth/campus-map' },
  { path: '/community/activities', redirect: '/growth/campus-activity' },
  { path: '/growth/star-map', redirect: '/career/nebula' },
  { path: '/growth/tree', name: 'growth-tree', component: GrowthTreeView },
  { path: '/growth/skills', redirect: '/profile-radar' },
  { path: '/growth/grades', redirect: '/mine/papers' },
  { path: '/growth/profile', redirect: '/profile-radar' },
  { path: '/career/star-map', redirect: '/career/nebula' },
  { path: '/career/search', redirect: '/career' },
  { path: '/career/profile', redirect: '/career?view=profile' },
  { path: '/career/skills', redirect: '/career?view=skills' },
  { path: '/career/gap', redirect: '/career?view=gap' },
  { path: '/learning/python', redirect: '/career/nebula/python' },
  { path: '/learning/python/plan', redirect: '/career/nebula/python/plan' },
  { path: '/learning/python/knowledge-graph', redirect: '/career/nebula/python/knowledge-graph' },
  { path: '/learning/question-bank', redirect: '/career/nebula/python' },
  { path: '/learning/special-training', redirect: '/career/nebula/python/resources' },
  { path: '/learning/recommended', redirect: { name: 'learning-practice', query: { tab: 'recommended' } } },
  { path: '/learning/python-and-algorithm', redirect: { name: 'learning-practice', query: { tab: 'python' } } },
  { path: '/learning/courses-and-special', redirect: { name: 'learning-practice', query: { tab: 'courses' } } },
  { path: '/learning/project-practice', redirect: { name: 'learning-practice', query: { tab: 'projects' } } },
  { path: '/learning/projects/sandbox', name: 'business-sandbox', component: BusinessSandboxView },
  { path: '/learning/projects/sandbox/rooms', name: 'business-sandbox-rooms', component: BusinessSandboxRoomsView },
  { path: '/learning/projects/sandbox/rooms/:roomId', name: 'business-sandbox-room', component: BusinessSandboxRoomView },
  { path: '/learning/my-practice', redirect: { name: 'learning-practice', query: { tab: 'practice' } } },
  { path: '/employment/radar', redirect: '/employment' },
  { path: '/employment/aggregate', redirect: '/employment' },
  { path: '/ai-career/resume', redirect: '/ai-tools/resume' },
  { path: '/ai-career/interview', redirect: '/interview' },
  { path: '/ai-career/report', redirect: '/mine/ai-history' },
  { path: '/campus-map', redirect: '/growth/campus-map' },
  { path: '/campus-activity', redirect: '/growth/campus-activity' },
  { path: '/python-learning', redirect: '/learning/python' },
  { path: '/ai-interview', redirect: '/ai-career/interview' },
  { path: '/star-map', redirect: '/growth/tree' },
  { path: '/jobs/explore', name: 'job-exploration', redirect: '/career' },
  // 岗位雷达页已下线，旧链接统一回到实习就业页的真实岗位列表
  { path: '/jobs/hot', redirect: '/employment' },
  { path: '/map', name: 'map', component: MapView },
  { path: '/activities', name: 'activities', component: CampusActivitiesView },
  { path: '/ai', name: 'ai', component: AiAssistantView },
  { path: '/ai-tools', name: 'ai-tools', component: AiToolsView },
  { path: '/ai-tools/resume', name: 'ai-tools-resume', component: ResumeView },
  { path: '/ai-tools/resume/workspace', name: 'ai-tools-resume-workspace', component: ResumeWorkspaceView },
  { path: '/ai-tools/resume/designer', name: 'ai-tools-resume-designer', component: ResumeDesigner },
  { path: '/ai-tools/resume/wizard', name: 'ai-tools-resume-wizard', component: ResumeWizard },
  { path: '/ai-tools/resume/wizard/edit', name: 'ai-tools-resume-edit', component: ResumeWizard },
  { path: '/ai-original', name: 'ai-original', component: AiOriginalView },
  { path: '/ai-original/add', name: 'ai-original-add', component: WatermarkAddView },
  { path: '/ai-original/batch', name: 'ai-original-batch', component: WatermarkBatchView },
  { path: '/ai-original/history', name: 'ai-original-history', component: WatermarkHistoryView },
  { path: '/ai-original/help', name: 'ai-original-help', component: WatermarkHelpView },
  { path: '/ai-studio', redirect: '/ai-tools' },
  { path: '/ai-studio/writing', name: 'ai-studio-writing', component: AiWritingView },
  { path: '/ai-studio/image', name: 'ai-studio-image', component: AiImageView },
  { path: '/ai-studio/exam', redirect: '/paper' },
  { path: '/paper', name: 'paper-home', component: PaperHomeView },
  { path: '/paper/mine', name: 'paper-mine', component: PaperHistoryView },
  { path: '/ai-studio/presentation', name: 'ai-studio-presentation', component: AiPresentationView },
  { path: '/ai-studio/mind_map', name: 'ai-studio-mind-map', component: AiMindMapView },
  { path: '/ai-studio/architecture', name: 'ai-studio-architecture', component: AiArchitectureView },
  { path: '/ai-studio/flowchart', name: 'ai-studio-flowchart', component: AiFlowchartView },
  {
    path: '/ai-studio/:tool',
    redirect: (to) => (AI_STUDIO_TOOL_IDS.includes(String(to.params.tool || '')) ? `/ai-studio/${to.params.tool}` : '/ai-tools'),
  },
  { path: '/profile-radar', name: 'profile-radar', component: ProfileRadarView },
  {
    path: '/career/nebula/python',
    component: PythonLearningShell,
    children: [
      { path: '', name: 'career-python-bank', component: PythonQuestionBankView },
      { path: 'plan', name: 'career-python-plan', component: PythonLearningView },
      { path: 'knowledge-graph', name: 'career-python-knowledge-graph', component: KnowledgeGraphView },
    ],
  },
  { path: '/career/nebula/python/practice/:id', name: 'career-python-practice', component: PythonPracticeView },
  { path: '/career/nebula/python/resources', name: 'career-python-resources', component: LearningResourceView },
  { path: '/learning/plan', redirect: '/career/nebula/python/plan' },
  { path: '/learning/problems/:id', redirect: (to) => `/career/nebula/python/practice/${to.params.id}` },
  { path: '/learning/practice/:id', redirect: (to) => `/career/nebula/python/practice/${to.params.id}` },
  { path: '/learning/knowledge-graph', redirect: '/career/nebula/python/knowledge-graph' },
  { path: '/learning/resources', redirect: '/career/nebula/python/resources' },
  { path: '/discount', name: 'discount', component: CampusDiscountView },
  { path: '/activities/publish', name: 'activity-publish', component: ActivityPublishView },
  { path: '/activities/:activityId', name: 'activity-detail', component: ActivityDetailView },
  { path: '/activities/:activityId/sign-in', name: 'activity-sign-in', component: ActivitySignInView },
  { path: '/activities/:activityId/signup', name: 'activity-signup', component: ActivitySignupView },
  { path: '/courses/:courseId', name: 'campus-course', component: CampusCourseView },
  { path: '/mine', name: 'mine', component: MineView },
  { path: '/messages', name: 'messages', component: MessageCenterView },
  { path: '/mine/messages', name: 'mine-messages', component: MessageCenterView },
  { path: '/mine/schedule', name: 'mine-schedule', component: ScheduleWorkspaceView },
  { path: '/mine/schedule-settings', name: 'mine-schedule-settings', component: ScheduleSettingsWorkspaceView },
  { path: '/mine/period-time', redirect: '/mine/schedule-settings?tab=periods' },
  { path: '/mine/semester', redirect: '/mine/schedule-settings?tab=semesters' },
  { path: '/mine/edu-account', redirect: '/mine/schedule-settings?tab=account' },
  { path: '/mine/activities', name: 'mine-activities', component: MineActivitiesView },
  { path: '/mine/ai-history', name: 'mine-ai-history', component: MineAiHistoryView },
  { path: '/mine/papers', name: 'mine-papers', component: ExamPapersView },
  { path: '/mine/papers/attempts/:attemptId', name: 'exam-taking', component: ExamTakingView },
  { path: '/mine/papers/:paperId/history', name: 'exam-history', component: ExamHistoryView },
  { path: '/mine/papers/results/:attemptId', name: 'exam-result', component: ExamResultView },
  { path: '/mine/papers/results/:attemptId/details', name: 'exam-detail', component: ExamDetailView },
  { path: '/mine/account-settings', name: 'account-settings', component: AccountSettingsView },
  { path: '/resume', redirect: '/ai-tools/resume' },
  { path: '/resume/legacy', redirect: '/ai-tools/resume' },
  { path: '/resume/workspace', redirect: '/ai-tools/resume/workspace' },
  { path: '/resume/designer', redirect: '/ai-tools/resume/designer' },
  { path: '/resume/wizard', redirect: '/ai-tools/resume/wizard' },
  { path: '/resume/wizard/edit', redirect: '/ai-tools/resume/wizard/edit' },
  { path: '/interview/resume', redirect: '/ai-tools/resume' },
  { path: '/interview/resume/workspace', redirect: '/ai-tools/resume/workspace' },
  { path: '/interview/resume/designer', redirect: '/ai-tools/resume/designer' },
  { path: '/interview/resume/wizard', redirect: '/ai-tools/resume/wizard' },
  { path: '/interview/resume/wizard/edit', redirect: '/ai-tools/resume/wizard/edit' },
  { path: '/career/nebula/:careerId?', name: 'career-nebula', component: CareerNebulaView },
  { path: '/career/nebula/:careerId/planet/:skillId', name: 'career-planet', component: CareerPlanetView },
  { path: '/convert', name: 'convert', component: DocumentConvertView },
  {
    path: '/interview',
    component: InterviewShell,
    children: [
      { path: '', redirect: '/interview/index' },
      { path: 'index', name: 'interview-index', component: InterviewIndex },
      { path: 'ai-mock-interview', name: 'interview-ai-mock', component: InterviewAiMockInterview },
      { path: 'ai-interview-config', name: 'interview-ai-config', component: InterviewAiConfig },
      { path: 'ai-chat', name: 'interview-ai-chat', component: InterviewAiChat },
      { path: 'ai-career-plan', name: 'interview-ai-career-plan', component: InterviewAiCareerPlan },
      { path: 'question-bank', name: 'interview-question-bank', component: InterviewQuestionBank },
      { path: 'ability-improvement', name: 'interview-ability', component: InterviewAbilityImprovement },
      { path: 'my', name: 'interview-my', component: InterviewMy },
      { path: 'my-note', name: 'interview-my-note', component: InterviewMyNote },
      { path: 'wrong-question-book', name: 'interview-wrong-book', component: InterviewWrongQuestionBook },
      { path: 'create-question-bank', name: 'interview-create-bank', component: InterviewCreateQuestionBank },
      { path: 'mock-question-filter', name: 'interview-mock-filter', component: InterviewMockQuestionFilter },
      { path: 'evaluation-report', name: 'interview-evaluation-report', component: InterviewAIReport },
    ],
  },
  { path: '/:pathMatch(.*)*', redirect: '/home' },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(to, from, savedPosition) {
    if (savedPosition) return savedPosition
    return undefined
  },
})

router.beforeEach((to) => {
  if (!to.meta.public && !getToken()) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
})

export default router
