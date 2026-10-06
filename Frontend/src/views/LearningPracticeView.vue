<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppTabBar from '../components/AppTabBar.vue'
import { getCampusCourses } from '../api/campusCourse'
import { listExamPapers } from '../api/exam'
import { completePathItem, completeProjectTask, getContentTags, getExternalCourses, getLearningRecommendations, getPracticeSummary, getPythonHome, startPathItem } from '../api/learning'
import { getPythonProblemList } from '../api/pythonProblem'
import { listPythonPapers } from '../api/pythonPaper'
import {
  decomposeStudyText,
  getStudyGoalDetail,
  listMyStudyGoals,
  saveStudyGoal,
  updateStudyGoalSubtaskCompletion,
  updateStudyGoalTaskCompletion,
} from '../api/studyGoal'
import { getSolvedIds } from '../composables/usePythonProblemBank'

const route = useRoute()
const router = useRouter()

const TABS = [
  { id: 'recommended', label: '推荐学习' },
  { id: 'python', label: '技能练习' },
  { id: 'courses', label: '课程与专项' },
  { id: 'projects', label: '项目实训' },
  { id: 'practice', label: '我的练习' },
]
const TARGET_JOB_STORAGE_KEY = 'home_target_job'

const loading = ref(true)
const pythonHome = ref({})
const unifiedRecommendations = ref([])
const courseTags = ref({})
const practiceSummary = ref(null)
/* 已保存的练习卷：用于「我的练习」里的回看入口 */
const myPapers = ref([])
/* 课程考试记录：用于「课程与专项」里的历史试卷入口 */
const examPapers = ref([])

/** 参加过的试卷（按作答次数排序），用于列表展示 */
const takenExamPapers = computed(() => examPapers.value
  .filter((item) => Number(item.attemptCount || 0) > 0)
  .sort((left, right) => Number(right.attemptCount || 0) - Number(left.attemptCount || 0)))

/** 考试概览：试卷总数 / 参加过几套 / 累计作答次数 / 未完成场次 */
const examSummary = computed(() => ({
  total: examPapers.value.length,
  taken: takenExamPapers.value.length,
  attempts: examPapers.value.reduce((sum, item) => sum + Number(item.attemptCount || 0), 0),
  unfinished: examPapers.value.filter((item) => item.inProgressAttemptId).length,
}))
const externalCourses = ref([])
const problems = ref([])
const courses = ref([])
const studyGoals = ref([])
const selectedGoalId = ref(null)
const selectedGoalDetail = ref(null)
const busyAction = ref('')
const pageMessage = ref('')
const dataErrors = reactive({ python: '', problems: '', courses: '', goals: '', recommendations: '', summary: '', external: '' })
const goalDialogOpen = ref(false)
const goalSaving = ref(false)
const goalError = ref('')
// AI 拆解不可用时置为 true，弹窗里会出现「直接保存（不拆解）」降级按钮
const goalAiUnavailable = ref(false)
const goalForm = reactive({
  title: '',
  description: '',
  planText: '',
  startDate: '',
  targetDate: '',
  dailyStudyMinutes: 60,
})
// 「按能力缺口生成」时置为 true：任务由缺口直接确定，保存时不再调用 AI 拆解
const goalFromGaps = ref(false)

const activeTab = computed(() => {
  const tab = String(route.query.tab || 'recommended')
  return TABS.some((item) => item.id === tab) ? tab : 'recommended'
})
const targetJobTitle = computed(() => {
  try { return localStorage.getItem(TARGET_JOB_STORAGE_KEY) || '' } catch { return '' }
})
const mastery = computed(() => Array.isArray(pythonHome.value.mastery) ? pythonHome.value.mastery : [])
const pathItems = computed(() => Array.isArray(pythonHome.value.activePath?.items) ? pythonHome.value.activePath.items : [])
const recommendations = computed(() => Array.isArray(pythonHome.value.recommendations) ? pythonHome.value.recommendations : [])
/* ---------- 岗位学习路径 ---------- */
// 把"按岗位算出的推荐结果"按技能聚合成有序步骤：
// 每一步 = 一个待补技能 + 该技能下最先该学的内容。
// 目标岗位一变，unifiedRecommendations 重新请求，整条路径随之变化。
const JOB_PATH_LIMIT = 5
const jobPathSteps = computed(() => {
  const steps = []
  const seen = new Set()
  unifiedRecommendations.value.forEach((item) => {
    if (!item?.skillCode || seen.has(item.skillCode)) return
    seen.add(item.skillCode)
    steps.push({
      skillCode: item.skillCode,
      skillName: item.skillName || item.skillCode,
      current: item.currentLevel ?? 0,
      required: item.requiredLevel ?? 0,
      next: item,
    })
  })
  return steps.slice(0, JOB_PATH_LIMIT)
})

// 路径步骤的类型标签：学（课程）/ 练（题目）/ 做（项目）
// 岗位实战任务：按目标岗位算出的推荐里，类型为 PROJECT 的内容。
// 默认只展开前几项，避免一次铺满整页，其余通过「展开全部」查看。
const JOB_PROJECT_PREVIEW = 2
const jobProjectsExpanded = ref(false)
const jobProjects = computed(() =>
  unifiedRecommendations.value.filter((item) => item?.sourceType === 'PROJECT_TASK'))
const visibleJobProjects = computed(() =>
  jobProjectsExpanded.value ? jobProjects.value : jobProjects.value.slice(0, JOB_PROJECT_PREVIEW))

function pathStepKind(item) {
  const map = { COURSE: '学', EXTERNAL_COURSE: '学', PROBLEM: '练', PROJECT: '做', PROJECT_TASK: '做', SPECIAL_TRAINING: '专', PYTHON: '学' }
  return map[item?.sourceType] || '学'
}

function stepPercent(step) {
  if (!step?.required) return 0
  return Math.min(100, Math.round((step.current / step.required) * 100))
}

const displayedRecommendations = computed(() => {
  if (unifiedRecommendations.value.length) {
    return unifiedRecommendations.value.map((item) => ({
      key: `${item.sourceType}-${item.sourceId}-${item.skillCode}`,
      type: sourceTypeLabel(item.sourceType),
      title: item.title,
      reason: item.reason,
      skill: item.skillName,
      required: item.requiredLevel,
      current: item.currentLevel,
      raw: item,
    }))
  }
  return recommendations.value.map((item) => ({
    key: item.id || item.title || item.knowledgePoint,
    type: item.type || item.resourceType || '学习建议',
    title: item.title || item.knowledgePoint || item.name || '学习内容',
    reason: item.reason || item.rationale || item.description || '基于当前学习记录推荐',
    raw: item,
  }))
})
// 目标岗位的技能差距：直接取统一推荐结果里的技能去重，代表"还差什么才能达到岗位要求"
const targetSkillGaps = computed(() => {
  const seen = new Map()
  unifiedRecommendations.value.forEach((item) => {
    if (!item?.skillCode || seen.has(item.skillCode)) return
    seen.set(item.skillCode, {
      code: item.skillCode,
      name: item.skillName || item.skillCode,
      current: item.currentLevel ?? 0,
      required: item.requiredLevel ?? 0,
    })
  })
  return [...seen.values()]
})
// 首页 hero 用一行摘要展示待补技能，避免和“岗位学习路径”重复铺一遍
const targetGapNames = computed(() =>
  targetSkillGaps.value.slice(0, 4).map((item) => item.name).join('、'))
const nextPathItem = computed(() => pathItems.value.find((item) => item.status === 'in_progress') || pathItems.value.find((item) => item.status !== 'completed') || null)
const activeGoalSummary = computed(() => studyGoals.value.find((item) => item.id === selectedGoalId.value) || studyGoals.value.find((item) => item.status === 'in_progress') || studyGoals.value.find((item) => item.status === 'pending') || studyGoals.value[0] || null)
const activeGoalTasks = computed(() => Array.isArray(selectedGoalDetail.value?.tasks) ? selectedGoalDetail.value.tasks : [])
const remainingGoalTasks = computed(() => activeGoalTasks.value.filter((task) => !task.isCompleted))
const solvedIds = ref(getSolvedIds().map((id) => String(id)))
const solvedSet = computed(() => new Set(solvedIds.value))
const solvedProblemCount = computed(() => problems.value.filter((item) => solvedSet.value.has(String(item.id))).length)
const pathDoneCount = computed(() => pathItems.value.filter((item) => item.status === 'completed').length)
const hasLearningPath = computed(() => pathItems.value.length > 0)
const nextGoalTask = computed(() => activeGoalTasks.value.find((task) => !task.isCompleted) || null)
const goalTasksExpanded = ref(false)
const completedTasksExpanded = ref(false)
const pendingGoalTasks = computed(() => activeGoalTasks.value.filter((task) => !task.isCompleted))
const completedGoalTasks = computed(() => activeGoalTasks.value.filter((task) => task.isCompleted))
const GOAL_TASK_PREVIEW = 4
const visiblePendingTasks = computed(() => goalTasksExpanded.value ? pendingGoalTasks.value : pendingGoalTasks.value.slice(0, GOAL_TASK_PREVIEW))
const hiddenPendingTaskCount = computed(() => Math.max(0, pendingGoalTasks.value.length - visiblePendingTasks.value.length))
const visiblePendingTaskGroups = computed(() => groupGoalTasksByDay(visiblePendingTasks.value))
const pendingGoalDayCount = computed(() => groupGoalTasksByDay(pendingGoalTasks.value).length)

function formatGoalTaskDay(value) {
  const matched = String(value || '').match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (!matched) return ''
  return `${Number(matched[2])}月${Number(matched[3])}日`
}

function groupGoalTasksByDay(list) {
  const groups = []
  const index = new Map()
  for (const task of list) {
    const start = formatGoalTaskDay(task.plannedStartDate)
    const end = formatGoalTaskDay(task.plannedEndDate || task.plannedStartDate)
    const label = start ? (end && end !== start ? `${start} - ${end}` : start) : (task.stage || '未排期')
    if (!index.has(label)) {
      const group = { key: label, label, tasks: [] }
      index.set(label, group)
      groups.push(group)
    }
    index.get(label).tasks.push(task)
  }
  return groups
}
const judgeableProblems = computed(() => problems.value.filter((item) => item.judgeable))
const solveRate = computed(() => judgeableProblems.value.length ? Math.round(solvedProblemCount.value / judgeableProblems.value.length * 100) : 0)
const nextProblems = computed(() => problems.value
  .filter((item) => item.judgeable && !solvedSet.value.has(String(item.id)))
  .sort((left, right) => difficultyRank(left.difficulty) - difficultyRank(right.difficulty))
  .slice(0, 5))

const summarySkills = computed(() => Array.isArray(practiceSummary.value?.skills) ? practiceSummary.value.skills : [])
const summaryCourses = computed(() => Array.isArray(practiceSummary.value?.courses) ? practiceSummary.value.courses : [])
const summaryProblemStats = computed(() => practiceSummary.value?.problems || {})
const summaryProjectStats = computed(() => practiceSummary.value?.projects || { total: 0, completed: 0, inProgress: 0, items: [] })

/* ---------- 目标岗位相关性：课程与专项跟随用户选的目标岗位 ---------- */

// 只在用户选了目标岗位时才启用岗位相关性排序与筛选
// 课程筛选三态：all=全部 / matched=只看匹配岗位 / enrolled=我加入的课程
// 状态写在 URL 的 ?filter= 里：从课程详情页跳回来能保持筛选，也方便分享
const COURSE_FILTERS = ['all', 'matched', 'enrolled']
const courseFilter = computed(() => {
  const value = String(route.query.filter || 'all')
  return COURSE_FILTERS.includes(value) ? value : 'all'
})

function jobNamesOf(list) {
  if (!Array.isArray(list)) return []
  return list
    .map((item) => (typeof item === 'string' ? item : item?.name))
    .filter(Boolean)
}

function matchesTargetJob(list) {
  const target = targetJobTitle.value
  if (!target) return false
  return jobNamesOf(list).includes(target)
}

// 校内课程的岗位信息来自课程技能标签里的 jobs
function matchesCourse(course) {
  return matchesTargetJob(tagOf(course?.id)?.jobs)
}

function sortByJobMatch(list, matcher) {
  if (!targetJobTitle.value) return [...list]
  return [...list].sort((left, right) => Number(matcher(right)) - Number(matcher(left)))
}

const sortedCourses = computed(() => sortByJobMatch(courses.value, matchesCourse))
// 已加入的课程（与目标岗位无关，单独作为一个筛选维度）
const enrolledCourses = computed(() => courses.value.filter((course) => course.enrolled === true))

const visibleCourses = computed(() => {
  if (courseFilter.value === 'enrolled') return enrolledCourses.value
  if (courseFilter.value === 'matched' && targetJobTitle.value) {
    return sortedCourses.value.filter(matchesCourse)
  }
  return sortedCourses.value
})

const sortedExternalCourses = computed(() =>
  sortByJobMatch(externalCourses.value, (course) => matchesTargetJob(course?.jobs)))
const visibleExternalCourses = computed(() => {
  if (courseFilter.value === 'matched' && targetJobTitle.value) {
    return sortedExternalCourses.value.filter((course) => matchesTargetJob(course?.jobs))
  }
  return sortedExternalCourses.value
})

const matchedCourseCount = computed(() => courses.value.filter(matchesCourse).length)
const matchedExternalCount = computed(() => externalCourses.value.filter((course) => matchesTargetJob(course?.jobs)).length)

/* ---------- 默认只展示少量，其余点「展开」查看，避免页面过长 ---------- */

const PREVIEW_COUNT = 6
const courseExpanded = ref(false)
const externalExpanded = ref(false)

// 统一用路由写入筛选：切换后重新收起，避免出现"展开了但没内容"的困惑
function goToCourses(filter) {
  courseExpanded.value = false
  externalExpanded.value = false
  const query = { tab: 'courses' }
  if (filter && filter !== 'all') query.filter = filter
  router.replace({ path: '/learning', query })
}

function setCourseFilter(value) {
  goToCourses(value)
}

const displayedCourses = computed(() =>
  courseExpanded.value ? visibleCourses.value : visibleCourses.value.slice(0, PREVIEW_COUNT))
const displayedExternalCourses = computed(() =>
  externalExpanded.value ? visibleExternalCourses.value : visibleExternalCourses.value.slice(0, PREVIEW_COUNT))
const summaryProjectItems = computed(() => Array.isArray(summaryProjectStats.value.items) ? summaryProjectStats.value.items : [])
const trendPoints = computed(() => Array.isArray(practiceSummary.value?.trend) ? practiceSummary.value.trend : [])

// 「考试与证书专项」只展示备考类目标（四六级 / 证书 / 考研等），技能类目标仍在「我的练习」里
const EXAM_GOAL_KEYWORDS = ['四六级', '四级', '六级', '英语', '雅思', '托福', '证书', '考证', '考试', '软考', '计算机等级', '教师资格', '考研', '资格证']
const examGoals = computed(() => studyGoals.value.filter((goal) => {
  const text = `${goal?.title || ''} ${goal?.description || ''}`
  return EXAM_GOAL_KEYWORDS.some((keyword) => text.includes(keyword))
}))
const trendPeak = computed(() => Math.max(1, ...trendPoints.value.map((point) => point.averageLevel || 0)))
function trendHeight(point) {
  return `${Math.max(6, Math.round(((point.averageLevel || 0) / trendPeak.value) * 100))}%`
}

function friendlyError(message) {
  const text = String(message || '')
  if (/failed to fetch|networkerror|load failed|connection/i.test(text)) return '网络连接失败，请确认服务已启动后重试'
  return text || '加载失败'
}

async function loadExamPapers() {
  try {
    const page = await listExamPapers({ page: 0, size: 100 })
    examPapers.value = Array.isArray(page?.content) ? page.content : []
  } catch {
    examPapers.value = []
  }
}

/** 有未完成的考试就直接继续，否则看这门试卷的历史记录 */
function openExamPaper(item) {
  if (item?.inProgressAttemptId) {
    router.push(`/mine/papers/attempts/${item.inProgressAttemptId}`)
    return
  }
  router.push(`/mine/papers/${item.id}/history`)
}

async function loadMyPapers() {
  try {
    const list = await listPythonPapers()
    myPapers.value = Array.isArray(list) ? list : []
  } catch {
    myPapers.value = []
  }
}

async function loadLearningData() {
  loading.value = true
  pageMessage.value = ''
  const [pythonResult, problemResult, courseResult, goalResult, tagResult] = await Promise.allSettled([
    getPythonHome(),
    getPythonProblemList(),
    getCampusCourses(),
    listMyStudyGoals(1, 20),
    getContentTags('COURSE'),
  ])
  if (pythonResult.status === 'fulfilled') {
    pythonHome.value = pythonResult.value || {}
    dataErrors.python = ''
  } else {
    pythonHome.value = {}
    dataErrors.python = friendlyError(pythonResult.reason?.message || '学习路径加载失败')
  }
  if (problemResult.status === 'fulfilled') {
    problems.value = Array.isArray(problemResult.value?.data) ? problemResult.value.data : []
    dataErrors.problems = ''
  } else {
    problems.value = []
    dataErrors.problems = friendlyError(problemResult.reason?.message || '题库加载失败')
  }
  if (courseResult.status === 'fulfilled') {
    courses.value = Array.isArray(courseResult.value?.data) ? courseResult.value.data : []
    dataErrors.courses = ''
  } else {
    courses.value = []
    dataErrors.courses = friendlyError(courseResult.reason?.message || '课程加载失败')
  }
  if (goalResult.status === 'fulfilled') {
    const page = goalResult.value || {}
    studyGoals.value = Array.isArray(page.records) ? page.records : []
    dataErrors.goals = ''
    const preferred = studyGoals.value.find((item) => item.status === 'in_progress')?.id || studyGoals.value[0]?.id
    if (preferred) await loadGoalDetail(preferred)
  } else {
    studyGoals.value = []
    selectedGoalDetail.value = null
    dataErrors.goals = friendlyError(goalResult.reason?.message || '学习目标加载失败')
  }
  if (tagResult.status === 'fulfilled') {
    courseTags.value = indexTags(tagResult.value)
  } else {
    courseTags.value = {}
  }
  try {
    externalCourses.value = await getExternalCourses() || []
    dataErrors.external = ''
  } catch (error) {
    externalCourses.value = []
    dataErrors.external = friendlyError(error.message || '外部课程加载失败')
  }
  try {
    practiceSummary.value = await getPracticeSummary()
    dataErrors.summary = ''
  } catch (error) {
    practiceSummary.value = null
    dataErrors.summary = friendlyError(error.message || '学习记录加载失败')
  }
  await loadUnifiedRecommendations()
  loading.value = false
}

async function loadUnifiedRecommendations() {
  const jobName = targetJobTitle.value
  unifiedRecommendations.value = []
  if (!jobName) {
    dataErrors.recommendations = ''
    return
  }
  try {
    const items = await getLearningRecommendations(jobName, 24)
    unifiedRecommendations.value = Array.isArray(items) ? items : []
    dataErrors.recommendations = ''
  } catch (error) {
    unifiedRecommendations.value = []
    dataErrors.recommendations = friendlyError(error.message || '推荐学习加载失败')
  }
}
function indexTags(list) {
  const map = {}
  if (Array.isArray(list)) {
    list.forEach((item) => {
      if (item && item.sourceId != null) map[item.sourceId] = item
    })
  }
  return map
}
function tagOf(courseId) {
  return courseTags.value[courseId] || null
}
function difficultyLabel(value) {
  return { BEGINNER: '入门', INTERMEDIATE: '进阶', ADVANCED: '挑战' }[value] || ''
}
function sourceTypeLabel(value) {
  return { COURSE: '推荐课程', PROBLEM: '推荐算法题', PROJECT: '项目实训', PROJECT_TASK: '项目实训', SPECIAL_TRAINING: '专项资源', PYTHON: 'Python 学习', EXTERNAL_COURSE: '外部精选课程' }[value] || '推荐学习'
}
function selectTab(tabId) {
  router.replace({ path: '/learning', query: tabId === 'recommended' ? {} : { tab: tabId } })
}
function difficultyRank(value) {
  return { easy: 0, medium: 1, hard: 2 }[value] ?? 9
}
function formatDifficulty(value) {
  return { easy: '简单', medium: '中等', hard: '困难' }[value] || '未标注'
}
function statusLabel(value) {
  return { pending: '未开始', in_progress: '进行中', blocked: '受阻', skipped: '已跳过', completed: '已完成' }[value] || value || '未开始'
}
function goalStatusLabel(value) {
  return { pending: '待开始', in_progress: '进行中', completed: '已完成' }[value] || value || '待开始'
}
function formatDate(value) {
  return value ? String(value).slice(0, 10) : '未设置'
}
function todayString() {
  const date = new Date()
  date.setMinutes(date.getMinutes() - date.getTimezoneOffset())
  return date.toISOString().slice(0, 10)
}
function addDaysString(days) {
  const date = new Date()
  date.setDate(date.getDate() + days)
  date.setMinutes(date.getMinutes() - date.getTimezoneOffset())
  return date.toISOString().slice(0, 10)
}
function resetGoalForm() {
  goalForm.title = ''
  goalForm.description = ''
  goalForm.planText = ''
  goalForm.startDate = todayString()
  goalForm.targetDate = addDaysString(30)
  goalForm.dailyStudyMinutes = 60
  goalError.value = ''
}
function openGoalForCheckin(goalId) {
  loadGoalDetail(goalId)
  selectTab('practice')
}

function openGoalDialog() {
  resetGoalForm()
  goalAiUnavailable.value = false
  goalFromGaps.value = false
  goalDialogOpen.value = true
}

/** 用岗位探索页算出的真实能力缺口，拼一份可以直接拆解的学习计划。 */
function buildGapGoalPlan() {
  const jobName = targetJobTitle.value || '目标岗位'
  const gaps = targetSkillGaps.value.slice(0, 5)
  if (!gaps.length) return null
  const lines = gaps.map((gap, index) => {
    const need = Math.max(0, Number(gap.required || 0) - Number(gap.current || 0))
    return `${index + 1}. ${gap.name}（当前 ${gap.current} / 岗位要求 ${gap.required}，需要提升 ${need}）`
  })
  const names = gaps.map((gap) => gap.name).join('、')
  return {
    title: `${jobName}能力提升计划`,
    description: `针对 ${jobName} 的能力缺口，优先补齐 ${names}。`,
    planText: [
      `目标岗位：${jobName}`,
      '需要补齐的能力缺口：',
      ...lines,
      '请按缺口优先级安排学习任务，每天学习 60 分钟，30 天内完成。',
    ].join('\n'),
  }
}

/** 打开「按能力缺口生成」：预填缺口计划，保存时仍走原有的 AI 拆解流程。 */
function openGapGoalDialog() {
  const plan = buildGapGoalPlan()
  if (!plan) {
    pageMessage.value = '还没有可用的能力缺口数据：先设置目标岗位，或在题库、课程里积累一些学习记录'
    openGoalDialog()
    return
  }
  resetGoalForm()
  goalForm.title = plan.title
  goalForm.description = plan.description
  goalForm.planText = plan.planText
  goalAiUnavailable.value = false
  goalFromGaps.value = true
  goalDialogOpen.value = true
}

/** 每个能力缺口直接生成一项任务，不依赖 AI 拆解。 */
function buildGapGoalTasks() {
  const recommended = new Map(jobPathSteps.value.map((step) => [step.skillCode, step.next]))
  const gaps = targetSkillGaps.value.slice(0, 5)
  // 每项任务的天数按「开始日期 → 目标日期」的总跨度平均分配，避免排期超出目标日期
  const spanDays = goalSpanDays()
  const daysPerTask = Math.max(1, Math.floor(spanDays / Math.max(1, gaps.length)))
  return gaps.map((gap, index) => {
    const need = Math.max(0, Number(gap.required || 0) - Number(gap.current || 0))
    const next = recommended.get(gap.code)
    return {
      taskName: `${gap.name}：从 ${gap.current} 提升到 ${gap.required}`.slice(0, 120),
      stage: '能力补齐',
      estimatedDays: daysPerTask,
      plannedStartDate: todayString(),
      priority: index === 0 ? '高' : '中',
      orderNum: index + 1,
      isCompleted: false,
      progressPercent: 0,
      description: `当前 ${gap.current}，岗位要求 ${gap.required}，需要提升 ${need}。${next?.title ? `建议先从「${next.title}」开始。` : ''}`,
      subtasks: [],
    }
  })
}

/** 目标日期与开始日期之间的天数，缺省按 30 天。 */
function goalSpanDays() {
  const start = goalForm.startDate ? new Date(goalForm.startDate) : null
  const end = goalForm.targetDate ? new Date(goalForm.targetDate) : null
  if (start && end && !Number.isNaN(start.getTime()) && !Number.isNaN(end.getTime())) {
    return Math.max(1, Math.round((end.getTime() - start.getTime()) / 86400000))
  }
  return 30
}

function closeGoalDialog() {
  if (!goalSaving.value) goalDialogOpen.value = false
}
function normalizeSubtask(item) {
  return {
    taskName: item.taskName || item.task_name || '未命名子任务',
    description: item.description || '',
    estimatedDays: Number(item.estimatedDays ?? item.estimated_days ?? 1),
    orderNum: Number(item.orderNum ?? item.order_num ?? 0),
    progressPercent: Number(item.progressPercent ?? item.progress_percent ?? 0),
    isCompleted: Boolean(item.isCompleted ?? item.is_completed),
    status: item.status || 'pending',
  }
}
function normalizeTask(item, index) {
  return {
    taskName: item.taskName || item.task_name || `任务 ${index + 1}`,
    stage: item.stage || '',
    estimatedDays: Number(item.estimatedDays ?? item.estimated_days ?? 1),
    plannedStartDate: item.plannedStartDate || item.planned_start_date || goalForm.startDate || null,
    plannedEndDate: item.plannedEndDate || item.planned_end_date || null,
    priority: item.priority || '中',
    orderNum: Number(item.orderNum ?? item.order_num ?? index + 1),
    isCompleted: false,
    progressPercent: 0,
    description: item.description || '',
    subtasks: Array.isArray(item.subtasks) ? item.subtasks.map(normalizeSubtask) : [],
  }
}
async function saveGoal(skipDecompose = false) {
  if (!goalForm.title.trim()) {
    goalError.value = '请先填写学习目标名称'
    return
  }
  goalSaving.value = true
  goalError.value = ''
  try {
    let payload
    if (goalFromGaps.value && !skipDecompose && targetSkillGaps.value.length) {
      // 能力缺口已经拆好每一项任务，直接入库，不依赖 AI 拆解服务
      payload = {
        goal: {
          title: goalForm.title.trim(),
          description: goalForm.description.trim(),
          startDate: goalForm.startDate || null,
          targetDate: goalForm.targetDate || null,
          dailyStudyMinutes: Number(goalForm.dailyStudyMinutes) || 60,
        },
        tasks: buildGapGoalTasks(),
      }
    } else if (goalForm.planText.trim() && !skipDecompose) {
      let preview
      try {
        preview = await decomposeStudyText(goalForm.planText.trim())
      } catch {
        // 不把底层的网络错误抛给用户，直接给出可执行的替代方案
        goalAiUnavailable.value = true
        goalError.value = 'AI 拆解服务暂时不可用，无法把学习计划自动拆成任务。你可以点下方「直接保存（不拆解）」先创建目标，稍后再到「我的练习」里补充任务。'
        return
      }
      goalAiUnavailable.value = false
      payload = {
        goal: {
          ...(preview?.goal || {}),
          title: goalForm.title.trim(),
          description: goalForm.description.trim() || preview?.goal?.description || '',
          startDate: goalForm.startDate || null,
          targetDate: goalForm.targetDate || null,
          dailyStudyMinutes: Number(goalForm.dailyStudyMinutes) || 60,
        },
        tasks: Array.isArray(preview?.tasks) ? preview.tasks.map(normalizeTask) : [],
      }
    } else {
      // 未填计划文本，或 AI 拆解不可用时的降级保存：整段计划作为一条任务的说明
      const taskDetail = goalForm.planText.trim() || goalForm.description.trim()
      payload = {
        goal: {
          title: goalForm.title.trim(),
          description: goalForm.description.trim(),
          startDate: goalForm.startDate || null,
          targetDate: goalForm.targetDate || null,
          dailyStudyMinutes: Number(goalForm.dailyStudyMinutes) || 60,
        },
        tasks: [{
          taskName: (goalForm.description.trim() || (goalForm.title.trim() + '学习任务')).slice(0, 120),
          estimatedDays: 1,
          plannedStartDate: goalForm.startDate || null,
          priority: '中',
          orderNum: 1,
          isCompleted: false,
          progressPercent: 0,
          description: taskDetail,
        }],
      }
    }
    const saved = await saveStudyGoal(payload)
    goalDialogOpen.value = false
    goalAiUnavailable.value = false
    goalFromGaps.value = false
    await reloadGoals(saved?.goal?.id)
    pageMessage.value = '学习目标已保存'
  } catch (error) {
    goalError.value = error.message || '学习目标保存失败'
  } finally {
    goalSaving.value = false
  }
}
async function reloadGoals(preferredGoalId = null) {
  try {
    const page = await listMyStudyGoals(1, 20)
    studyGoals.value = Array.isArray(page?.records) ? page.records : []
    dataErrors.goals = ''
    const goalId = preferredGoalId || selectedGoalId.value || studyGoals.value[0]?.id
    if (goalId) await loadGoalDetail(goalId)
    else selectedGoalDetail.value = null
  } catch (error) {
    dataErrors.goals = error.message || '学习目标加载失败'
  }
}
async function loadGoalDetail(goalId) {
  if (!goalId) {
    selectedGoalId.value = null
    selectedGoalDetail.value = null
    return
  }
  try {
    selectedGoalId.value = goalId
    goalTasksExpanded.value = false
    completedTasksExpanded.value = false
    selectedGoalDetail.value = await getStudyGoalDetail(goalId)
    dataErrors.goals = ''
  } catch (error) {
    dataErrors.goals = error.message || '目标详情加载失败'
  }
}
async function toggleTask(task) {
  if (!task?.id || busyAction.value) return
  busyAction.value = `task-${task.id}`
  try {
    await updateStudyGoalTaskCompletion(task.id, !task.isCompleted)
    await reloadGoals(selectedGoalId.value)
  } catch (error) {
    dataErrors.goals = error.message || '任务状态更新失败'
  } finally {
    busyAction.value = ''
  }
}
async function toggleSubtask(subtask) {
  if (!subtask?.id || busyAction.value) return
  busyAction.value = `subtask-${subtask.id}`
  try {
    await updateStudyGoalSubtaskCompletion(subtask.id, !subtask.isCompleted)
    await loadGoalDetail(selectedGoalId.value)
    await reloadGoals(selectedGoalId.value)
  } catch (error) {
    dataErrors.goals = error.message || '子任务状态更新失败'
  } finally {
    busyAction.value = ''
  }
}
async function updatePathItem(item, action) {
  if (!item?.id || busyAction.value) return
  busyAction.value = `${action}-${item.id}`
  try {
    if (action === 'start') await startPathItem(item.id)
    else await completePathItem(item.id)
    pythonHome.value = await getPythonHome() || {}
  } catch (error) {
    dataErrors.python = error.message || '学习路径更新失败'
  } finally {
    busyAction.value = ''
  }
}
function continueLearning() {
  // 优先级：未完成的学习目标任务（最容易中断）→ 岗位学习路径 → Python 专项路径
  if (remainingGoalTasks.value.length) {
    selectTab('practice')
  } else if (jobPathSteps.value.length) {
    scrollToJobPath()
  } else if (nextPathItem.value) {
    router.push('/learning/python/plan')
  } else {
    openGoalDialog()
  }
}

// 把「岗位学习路径」滚进视野，而不是跳去别的板块
function scrollToJobPath() {
  const target = document.querySelector('.job-path')
  if (target && typeof target.scrollIntoView === 'function') {
    target.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }
}
function openRecommendation(item) {
  const raw = item?.raw || item
  if (raw?.sourceType) {
    if (raw.sourceType === 'COURSE') { router.push(`/courses/${raw.sourceId}`); return }
    if (raw.sourceType === 'EXTERNAL_COURSE') { if (raw.url) window.open(raw.url, '_blank', 'noopener'); return }
    if (raw.sourceType === 'PROBLEM') { router.push(`/career/nebula/python/practice/${raw.sourceId}`); return }
    if (raw.sourceType === 'PROJECT') { selectTab('projects'); return }
    if (raw.sourceType === 'PROJECT_TASK') { selectTab('projects'); return }
    if (raw.sourceType === 'SPECIAL_TRAINING') { router.push('/career/nebula/python/resources'); return }
    if (raw.sourceType === 'PYTHON') { router.push('/learning/python'); return }
  }
  const topic = item?.title || item?.knowledgePoint || item?.name || ''
  if (item?.courseId) {
    router.push(`/courses/${item.courseId}`)
    return
  }
  router.push({ path: '/career/nebula/python/resources', query: topic ? { topic } : {} })
}
/** 岗位实战任务标记完成：写入统一学习记录，技能进度与趋势随之更新。 */
async function completeJobProject(item) {
  if (!item?.sourceId || item.completed || busyAction.value) return
  busyAction.value = `project-${item.sourceId}`
  try {
    await completeProjectTask(item.sourceId)
    await loadUnifiedRecommendations()
    pageMessage.value = '已记录完成，技能进度与增长趋势已更新'
  } catch (error) {
    pageMessage.value = friendlyError(error.message || '任务完成上报失败')
  } finally {
    busyAction.value = ''
  }
}

function openProblem(id) {
  router.push(`/career/nebula/python/practice/${id}`)
}

onMounted(async () => {
  await loadLearningData()
  // 从岗位探索页的「生成我的提升计划」进来时，直接按能力缺口预填学习目标
  if (String(route.query.plan || '') === 'gaps') {
    openGapGoalDialog()
  }
  await loadMyPapers()
  await loadExamPapers()
})
</script>

<template>
  <div class="learning-page">
    <AppTabBar />
    <main class="learning-shell">
      <header class="learning-hero">
        <div class="hero-main">
          <div class="hero-info">
            <span class="learning-eyebrow">LEARNING STUDIO <i aria-hidden="true"></i> 学习成长空间</span>
            <h1>学习实践</h1>
            <p class="hero-line">
              <template v-if="activeGoalSummary">当前目标：<strong>{{ activeGoalSummary.title }}</strong> · 总进度 {{ activeGoalSummary.progress || 0 }}%</template>
              <template v-else>尚未设置学习目标，设置后可按任务跟踪进度</template>
            </p>
          </div>
          <div class="hero-actions">
            <template v-if="hasLearningPath">
              <button type="button" class="btn btn--primary" @click="continueLearning">继续上次学习</button>
              <button type="button" class="link" @click="openGoalDialog">设置学习目标</button>
            </template>
            <template v-else>
              <button type="button" class="btn btn--primary" @click="openGoalDialog">设置学习目标</button>
              <button type="button" class="link" @click="continueLearning">继续上次学习</button>
            </template>
          </div>
        </div>
        <div class="hero-stats">
          <div class="hero-stat"><strong>{{ solvedProblemCount }}</strong><span>已解决题目</span></div>
          <div class="hero-stat"><strong>{{ solveRate }}%</strong><span>判题完成度</span></div>
          <div class="hero-stat"><strong>{{ pathDoneCount }}/{{ pathItems.length }}</strong><span>学习路径</span></div>
          <div class="hero-stat"><strong>{{ activeGoalSummary?.progress || 0 }}%</strong><span>目标进度</span></div>
        </div>
      </header>

      <nav class="tabs" aria-label="学习实践分类">
        <button v-for="tab in TABS" :key="tab.id" type="button" :class="{ active: activeTab === tab.id }" @click="selectTab(tab.id)">{{ tab.label }}</button>
      </nav>

      <p v-if="pageMessage" class="message">{{ pageMessage }}</p>
      <div v-if="loading" class="state">正在加载学习数据…</div>

      <div v-else class="content">
        <template v-if="activeTab === 'recommended'">
          <section class="focus-grid">
            <article class="panel focus-panel">
              <div class="head"><div><small>目标岗位</small><h2>{{ targetJobTitle || '尚未选择目标岗位' }}</h2></div><span v-if="targetJobTitle" class="tag">目标岗位</span></div>
              <template v-if="targetJobTitle">
                <p v-if="targetSkillGaps.length">共 {{ targetSkillGaps.length }} 项技能待补齐：{{ targetGapNames }}{{ targetSkillGaps.length > 4 ? ' 等' : '' }}</p>
                <p v-else class="note">当前目标岗位的技能要求已全部达到。</p>
                <p class="note">具体的学习顺序见下方「岗位学习路径」。</p>
                <div class="focus-actions">
                  <button type="button" class="btn btn--primary" @click="router.push('/career')">更换目标岗位</button>
                  <button type="button" class="link" @click="goToCourses('matched')">查看匹配课程</button>
                </div>
              </template>
              <template v-else>
                <p>选择目标岗位后，系统会按岗位技能要求生成学习路径与推荐内容。</p>
                <button type="button" class="btn btn--primary" @click="router.push('/career')">选择目标岗位</button>
              </template>
            </article>

            <article class="panel goal-panel">
              <div class="head"><div><small>学习目标</small><h2>{{ activeGoalSummary?.title || '暂未设置学习目标' }}</h2></div><strong v-if="activeGoalSummary">{{ activeGoalSummary.progress || 0 }}%</strong></div>
              <template v-if="activeGoalSummary">
                <div class="progress"><i :style="{ width: `${activeGoalSummary.progress || 0}%` }" /></div>
                <p class="note">{{ activeGoalSummary.completedTasks || 0 }}/{{ activeGoalSummary.totalTasks || 0 }} 项任务已完成</p>
                <ul v-if="remainingGoalTasks.length" class="task-preview">
                  <li v-for="task in remainingGoalTasks.slice(0, 3)" :key="task.id"><button type="button" :disabled="busyAction === `task-${task.id}`" @click="toggleTask(task)"><i />{{ task.taskName }}</button></li>
                </ul>
                <p v-else class="note">当前目标没有待完成任务。</p>
                <button type="button" class="link" @click="selectTab('practice')">查看完整目标</button>
              </template>
              <template v-else>
                <p>设置四六级、证书或技能学习目标，系统会按任务记录进度。</p>
                <button type="button" class="link" @click="openGoalDialog">立即设置目标</button>
                <button type="button" class="link" @click="openGapGoalDialog">按能力缺口生成</button>
              </template>
            </article>
          </section>

          <section class="panel section">
            <div class="head"><div><small>按岗位要求推进</small><h2>岗位学习路径</h2></div><button type="button" class="link" @click="selectTab('courses')">查看匹配课程</button></div>
            <div v-if="!targetJobTitle" class="empty empty--small"><strong>先选择目标岗位</strong><p>到岗位探索选定目标岗位后，这里会按岗位技能要求生成学习路径。</p><button type="button" class="btn" @click="router.push('/jobs/explore')">选择目标岗位</button></div>
            <ol v-else-if="jobPathSteps.length" class="job-path">
              <li v-for="(step, index) in jobPathSteps" :key="step.skillCode">
                <span class="job-path__no">{{ String(index + 1).padStart(2, '0') }}</span>
                <div class="job-path__body">
                  <div class="job-path__head">
                    <strong>{{ step.skillName }}</strong>
                    <span class="job-path__level">{{ step.current }} / {{ step.required }}</span>
                  </div>
                  <div class="progress"><i :style="{ width: `${stepPercent(step)}%` }" /></div>
                  <p class="job-path__next"><em>{{ pathStepKind(step.next) }}</em>{{ step.next.title }}</p>
                </div>
                <button type="button" class="btn" @click="openRecommendation(step.next)">去学这一步</button>
              </li>
            </ol>
            <div v-else class="empty empty--small"><strong>暂无岗位学习路径</strong><p>该岗位还没有匹配的学习内容，可以先到「课程与专项」浏览课程。</p><button type="button" class="btn" @click="selectTab('courses')">查看课程与专项</button></div>
          </section>

          <section class="panel section">
            <div class="head"><div><small>{{ unifiedRecommendations.length ? '基于目标岗位技能差距' : '基于学习记录' }}</small><h2>推荐内容</h2></div><button type="button" class="link" @click="selectTab('python')">更多练习内容</button></div>
            <div v-if="displayedRecommendations.length" class="card-grid">
              <button v-for="item in displayedRecommendations.slice(0, 6)" :key="item.key" type="button" class="content-card" @click="openRecommendation(item)"><small>{{ item.type }}</small><strong>{{ item.title }}</strong><p>{{ item.reason }}</p><span v-if="item.skill" class="skill-line">{{ item.skill }} · 要求 {{ item.required }} / 当前 {{ item.current }}</span><em>开始学习 →</em></button>
            </div>
            <div v-else class="empty"><strong>暂无推荐内容</strong><p>{{ dataErrors.recommendations || (targetJobTitle ? '目标岗位的学习内容准备中，完成课程或练习后会更新推荐。' : '选择目标岗位后，这里会根据技能差距推荐学习内容。') }}</p><button type="button" class="btn" @click="targetJobTitle ? selectTab('courses') : router.push('/jobs/explore')">{{ targetJobTitle ? '浏览课程与专项' : '选择目标岗位' }}</button></div>
          </section>

        </template>

        <template v-else-if="activeTab === 'python'">
          <section class="panel toolbar"><div><h2>技能练习</h2><p>按技能方向练习：算法与数据结构，以及 Python 专项内容。</p></div></section>

          <section class="panel section">
            <div class="head"><div><small>方向一</small><h2>算法与数据结构</h2><span class="head-note">用 Python 判题，覆盖数组、字符串、动态规划等</span></div><button type="button" class="link" @click="router.push('/learning/python')">进入算法题库</button></div>
            <div class="stats"><article><span>题库总量</span><strong>{{ problems.length }}</strong><small>道公开题目</small></article><article><span>已解决</span><strong>{{ solvedProblemCount }}</strong><small>{{ solveRate }}% 完成度</small></article><article><span>在线判题</span><strong>{{ judgeableProblems.length }}</strong><small>支持运行与提交</small></article></div>
            <p v-if="dataErrors.problems" class="error"><span>{{ dataErrors.problems }}</span><button type="button" class="link" @click="loadLearningData">重新加载</button></p>
            <div class="sub-head"><h3>建议练习</h3><span>按难度从低到高排列</span></div>
            <div v-if="nextProblems.length" class="question-list"><button v-for="item in nextProblems" :key="item.id" type="button" @click="openProblem(item.id)"><span :class="`diff diff--${item.difficulty}`">{{ formatDifficulty(item.difficulty) }}</span><strong>{{ item.title }}</strong><span>{{ (item.tags || []).slice(0, 3).join(' · ') || '未标注知识点' }}</span><em>开始答题 →</em></button></div>
            <div v-else class="empty empty--small"><strong>暂无可推荐题目</strong><p>题库为空或已完成当前可练习题目。</p><button type="button" class="btn" @click="router.push('/learning/python')">进入算法题库</button></div>
          </section>

          <section class="panel section">
            <div class="head"><div><small>方向二</small><h2>Python 专项</h2><span class="head-note">按知识点组织，与岗位技能路径互补</span></div></div>
            <div class="quick-grid"><button type="button" @click="router.push('/learning/python/plan')"><strong>个性化学习路径</strong><span>按知识点和掌握状态安排下一步</span><em>打开路径 →</em></button><button type="button" @click="router.push('/learning/python/knowledge-graph')"><strong>知识图谱</strong><span>查看知识点关系和薄弱环节</span><em>打开图谱 →</em></button><button type="button" @click="router.push('/paper?from=learning')"><strong>AI 出题</strong><span>按知识点生成练习卷，支持打印与导出</span><em>生成练习卷 →</em></button></div>
          </section>
        </template>

        <template v-else-if="activeTab === 'courses'">
          <section class="panel toolbar"><div><h2>课程与专项</h2><p>校内课程、公开课与官方文档，以及四六级、证书等备考目标。</p></div><div class="job-filter">
              <span v-if="targetJobTitle" class="job-filter__label">目标岗位：<strong>{{ targetJobTitle }}</strong></span>
              <div class="job-filter__buttons">
                <button type="button" :class="{ active: courseFilter === 'all' }" @click="setCourseFilter('all')">全部课程</button>
                <button v-if="targetJobTitle" type="button" :class="{ active: courseFilter === 'matched' }" @click="setCourseFilter('matched')">匹配我的岗位<span v-if="matchedCourseCount">{{ matchedCourseCount }}</span></button>
                <button type="button" :class="{ active: courseFilter === 'enrolled' }" @click="setCourseFilter('enrolled')">我加入的<span v-if="enrolledCourses.length">{{ enrolledCourses.length }}</span></button>
              </div>
            </div></section>
          <p v-if="dataErrors.courses" class="error"><span>{{ dataErrors.courses }}</span><button type="button" class="link" @click="loadLearningData">重新加载</button></p>
          <div v-if="displayedCourses.length" class="course-grid">
            <button v-for="course in displayedCourses" :key="course.id" type="button" class="course-card" @click="router.push(`/courses/${course.id}`)"><span>{{ String(course.name || '课').slice(0, 1) }}</span><div><strong>{{ course.name }}</strong><span v-if="course.enrolled" class="enrolled-badge">已加入</span><span v-if="matchesCourse(course)" class="match-badge">匹配目标岗位</span><p>{{ course.bookTitle || course.ownerName || '校内课程' }}</p><div class="progress"><i :style="{ width: `${course.progressPercent || 0}%` }" /></div><small>学习进度 {{ course.progressPercent || 0 }}%</small><div v-if="tagOf(course.id)" class="course-tags"><span v-for="skill in tagOf(course.id).skills.slice(0, 3)" :key="skill.code" class="course-tag" :class="{ 'is-primary': skill.primary }">{{ skill.name }}</span><span v-if="difficultyLabel(tagOf(course.id).difficulty)" class="course-tag course-tag--level">{{ difficultyLabel(tagOf(course.id).difficulty) }}</span></div><p v-if="tagOf(course.id) && tagOf(course.id).jobs.length" class="course-jobs">适合岗位：{{ tagOf(course.id).jobs.map((job) => job.name).join('、') }}</p><p v-if="tagOf(course.id) && tagOf(course.id).prerequisites.length" class="course-jobs">先修：{{ tagOf(course.id).prerequisites.map((skill) => skill.name).join('、') }}</p></div></button>
          </div>
          <div v-if="displayedCourses.length && visibleCourses.length > PREVIEW_COUNT" class="expand-row"><span class="note">共 {{ visibleCourses.length }} 门课程</span><button type="button" class="expand-toggle" @click="courseExpanded = !courseExpanded">{{ courseExpanded ? '收起' : '展开全部 ' + visibleCourses.length + ' 门' }}</button></div>
          <div v-if="!displayedCourses.length && courseFilter === 'enrolled'" class="empty empty--small"><strong>你还没有加入课程</strong><p>打开任意课程，点「加入课程」后就会出现在这里，学习进度也会一并记录。</p><button type="button" class="btn" @click="setCourseFilter('all')">去看看全部课程</button></div>
          <div v-if="!displayedCourses.length && courseFilter !== 'enrolled' && courses.length" class="empty empty--small"><strong>目标岗位暂无匹配的校内课程</strong><p>「{{ targetJobTitle }}」目前没有直接相关的课程，可以切回全部课程浏览完整目录。</p><button type="button" class="btn" @click="setCourseFilter('all')">查看全部课程</button></div>
          <div v-if="!displayedCourses.length && !courses.length" class="empty"><strong>暂无可展示的校园课程</strong><p>管理员发布课程后，会在这里显示真实课程和章节进度。可以先去刷题，或浏览下面的公开课程。</p><button type="button" class="btn" @click="selectTab('python')">去 Python 与算法</button></div>
          
          <section class="panel section">
            <div class="head">
              <div><small>课程考试</small><h2>我的考试记录</h2></div>
              <button type="button" class="link" @click="router.push('/mine/papers')">查看全部试卷</button>
            </div>
            <p class="exam-summary">
              共 {{ examSummary.total }} 套试卷 · 已参加 {{ examSummary.taken }} 套 · 累计作答 {{ examSummary.attempts }} 次<span v-if="examSummary.unfinished"> · {{ examSummary.unfinished }} 场未完成</span>
            </p>
            <div v-if="takenExamPapers.length" class="exam-record-list">
              <button v-for="paper in takenExamPapers.slice(0, 5)" :key="paper.id" type="button" class="exam-record" @click="openExamPaper(paper)">
                <div>
                  <strong>{{ paper.title }}</strong>
                  <small>{{ paper.questionCount }} 题 · 满分 {{ paper.totalScore }} 分 · 已作答 {{ paper.attemptCount }} 次</small>
                </div>
                <em>{{ paper.inProgressAttemptId ? '继续考试' : '查看历史记录' }} →</em>
              </button>
            </div>
            <div v-else class="empty empty--small">
              <strong>还没有考试记录</strong>
              <p>进入任意课程的「课程考试」完成一次作答，记录就会汇总到这里。</p>
              <button type="button" class="btn" @click="router.push('/mine/papers')">浏览全部试卷</button>
            </div>
          </section>

          <section v-if="courseFilter !== 'enrolled'" class="panel section"><div class="head"><div><small>外部精选</small><h2>公开课程与官方文档</h2></div><span class="note">{{ targetJobTitle ? `匹配目标岗位 ${matchedExternalCount} 门 · 跳转原站学习` : '跳转原站学习' }}</span></div><p v-if="dataErrors.external" class="error"><span>{{ dataErrors.external }}</span><button type="button" class="link" @click="loadLearningData">重新加载</button></p><div v-if="displayedExternalCourses.length" class="external-grid"><a v-for="course in displayedExternalCourses" :key="course.id" class="external-card" :href="course.url" target="_blank" rel="noreferrer noopener"><div class="external-card__top"><small>{{ course.provider }}</small><span class="external-card__badges"><span v-if="matchesTargetJob(course.jobs)" class="match-badge">匹配目标岗位</span><span v-if="course.free" class="external-badge">免费</span></span></div><strong>{{ course.title }}</strong><p>{{ course.description || '前往原站查看课程详情。' }}</p><div v-if="course.skills && course.skills.length" class="course-tags"><span v-for="skill in course.skills.slice(0, 3)" :key="skill" class="course-tag">{{ skill }}</span></div><em>去原站学习 ↗</em></a></div><div v-if="displayedExternalCourses.length && visibleExternalCourses.length > PREVIEW_COUNT" class="expand-row"><span class="note">共 {{ visibleExternalCourses.length }} 门</span><button type="button" class="expand-toggle" @click="externalExpanded = !externalExpanded">{{ externalExpanded ? '收起' : '展开全部 ' + visibleExternalCourses.length + ' 门' }}</button></div><div v-if="!displayedExternalCourses.length && courseFilter !== 'enrolled' && externalCourses.length" class="empty empty--small"><strong>目标岗位暂无匹配的外部课程</strong><p>可以切回全部课程查看公开课程与官方文档。</p><button type="button" class="btn" @click="setCourseFilter('all')">查看全部课程</button></div><div v-if="!displayedExternalCourses.length && !externalCourses.length" class="empty empty--small"><strong>暂无外部课程</strong><p>登记外部精选课程后会显示在这里。</p></div></section>
          <section class="panel section"><div class="head"><div><small>备考与证书</small><h2>考试与证书专项</h2></div><button type="button" class="link" @click="openGoalDialog">新建备考目标</button></div><div v-if="examGoals.length" class="exam-goal-grid"><article v-for="goal in examGoals" :key="goal.id" class="exam-goal"><div class="exam-goal__head"><strong>{{ goal.title }}</strong><span class="tag">{{ goalStatusLabel(goal.status) }}</span></div><p>{{ goal.description || '按任务推进备考计划' }}</p><div class="progress"><i :style="{ width: `${goal.progress || 0}%` }" /></div><div class="exam-goal__foot"><small>{{ goal.completedTasks || 0 }}/{{ goal.totalTasks || 0 }} 项任务 · {{ goal.progress || 0 }}%</small><button type="button" class="link" @click="openGoalForCheckin(goal.id)">去打卡</button></div></article></div><div v-else class="empty empty--small"><strong>还没有备考目标</strong><p>四六级、证书考试都可以建目标，系统会拆解成任务并记录打卡进度。</p><button type="button" class="btn" @click="openGoalDialog">新建备考目标</button></div></section>
        </template>

        <template v-else-if="activeTab === 'projects'">
          <section class="panel toolbar"><div><h2>项目实训</h2><p>把学到的技能用起来：岗位实战任务，以及商业沙盘模拟经营。</p></div><button type="button" class="btn btn--primary" @click="router.push('/learning/projects/sandbox')">进入商业沙盘</button></section>

          <section class="panel section">
            <div class="head"><div><small>按目标岗位</small><h2>岗位实战任务</h2></div><div class="head-actions"><span class="note">{{ targetJobTitle || '未选择目标岗位' }}<template v-if="jobProjects.length"> · 共 {{ jobProjects.length }} 项</template></span><button v-if="jobProjects.length > JOB_PROJECT_PREVIEW" type="button" class="link" @click="jobProjectsExpanded = !jobProjectsExpanded">{{ jobProjectsExpanded ? '收起' : `展开全部 ${jobProjects.length} 项` }}</button></div></div>
            <div v-if="jobProjects.length" class="project-list"><article v-for="item in visibleJobProjects" :key="`${item.sourceType}-${item.sourceId}`"><div><span>岗位实战<span v-if="difficultyLabel(item.difficulty)"> · {{ difficultyLabel(item.difficulty) }}</span><span v-if="item.completed" class="project-done-badge">已完成</span></span><h3>{{ item.title }}</h3><p>{{ item.objective || item.reason }}</p><p v-if="item.deliverable" class="project-deliverable">交付物：{{ item.deliverable }}</p><p v-if="item.estimatedHours" class="project-hours">预计 {{ item.estimatedHours }} 小时 · 对应技能 {{ item.skillName }}</p></div><em>{{ item.skillName }}</em><div class="project-actions"><button type="button" class="btn" @click="goToCourses('matched')">去学相关课程</button><button type="button" class="btn btn--primary" :disabled="item.completed || busyAction === `project-${item.sourceId}`" @click="completeJobProject(item)">{{ item.completed ? '已完成' : (busyAction === `project-${item.sourceId}` ? '提交中' : '标记完成') }}</button></div></article></div>
            <button v-if="jobProjects.length > JOB_PROJECT_PREVIEW && !jobProjectsExpanded" type="button" class="task-more" @click="jobProjectsExpanded = true">还有 {{ jobProjects.length - visibleJobProjects.length }} 项实战任务，展开查看</button>
            <div v-if="!jobProjects.length" class="empty empty--small"><strong>{{ targetJobTitle ? '该岗位的实战任务正在整理中' : '先选择目标岗位' }}</strong><p>{{ targetJobTitle ? '当前可以先按岗位要求补齐技能，也可以直接进入商业沙盘练习经营决策。' : '到岗位探索选定目标岗位后，这里会按岗位组织实战任务。' }}</p><button v-if="!targetJobTitle" type="button" class="btn" @click="router.push('/jobs/explore')">选择目标岗位</button></div>
          </section>

          <section class="panel section">
            <div class="head"><div><small>拓展实训</small><h2>商业沙盘</h2></div><button type="button" class="btn btn--primary" @click="router.push('/learning/projects/sandbox')">进入沙盘</button></div>
            <p class="note">经营一家校园二手交易平台，连续完成四轮推广、服务、手续费和校区扩张决策，结束后生成经营报告和能力证据。</p>
            <div class="special-row"><span class="special-chip">经营决策</span><span class="special-chip">用户增长</span><span class="special-chip">风险控制</span><span class="special-chip">企业模拟 · 后续开放</span></div>
          </section>
        </template>

        <template v-else>
          <section class="panel toolbar"><div><h2>我的练习</h2><p>课程、刷题、项目和技能进度汇总在同一个视图里。</p></div><div class="toolbar-actions"><button type="button" class="btn" @click="openGapGoalDialog">按能力缺口生成</button><button type="button" class="btn btn--primary" @click="openGoalDialog">新建学习目标</button></div></section>
          <section class="practice-layout panel">
            <aside class="goal-list"><div class="head"><h2>学习目标</h2><span>{{ studyGoals.length }} 个</span></div><p v-if="dataErrors.goals" class="error"><span>{{ dataErrors.goals }}</span><button type="button" class="link" @click="loadLearningData">重新加载</button></p><button v-for="goal in studyGoals" :key="goal.id" type="button" :class="{ active: selectedGoalId === goal.id }" @click="loadGoalDetail(goal.id)"><span><strong>{{ goal.title }}</strong><small>{{ goal.completedTasks || 0 }}/{{ goal.totalTasks || 0 }} 项任务</small><small v-if="goal.nextTaskName" class="goal-list__next">下一步 · {{ goal.nextTaskName }}</small></span><em>{{ goal.progress || 0 }}%</em></button><div v-if="!studyGoals.length" class="empty empty--small"><strong>还没有学习目标</strong><p>创建四六级、证书或技能目标后，任务会显示在这里。</p></div></aside>
            <section class="goal-detail"><template v-if="selectedGoalDetail?.goal"><div class="head"><div><h2>{{ selectedGoalDetail.goal.title }}</h2></div><span class="tag">{{ goalStatusLabel(selectedGoalDetail.goal.status) }}</span></div><p>{{ selectedGoalDetail.goal.description || '暂无目标说明' }}</p><div class="meta"><span>开始 {{ formatDate(selectedGoalDetail.goal.startDate) }}</span><span>目标 {{ formatDate(selectedGoalDetail.goal.targetDate) }}</span><span>每日 {{ selectedGoalDetail.goal.dailyStudyMinutes || 60 }} 分钟</span></div><div class="progress"><i :style="{ width: `${selectedGoalDetail.goal.progress || 0}%` }" /></div><div v-if="nextGoalTask" class="next-task"><div><small>下一步</small><strong>{{ nextGoalTask.taskName }}</strong><p>{{ nextGoalTask.description || nextGoalTask.stage || '完成这项任务，推进当前学习目标' }}</p></div><button type="button" class="btn" :disabled="busyAction === `task-${nextGoalTask.id}`" @click="toggleTask(nextGoalTask)">标记完成</button></div><div v-if="activeGoalTasks.length" class="task-list">
            <div class="task-list__head">
              <div><strong>任务清单</strong><small>共 {{ activeGoalTasks.length }} 项 · 待完成 {{ pendingGoalTasks.length }} 项<span v-if="pendingGoalDayCount"> · {{ pendingGoalDayCount }} 个时间节点</span></small></div>
              <button v-if="pendingGoalTasks.length > GOAL_TASK_PREVIEW" type="button" class="link" @click="goalTasksExpanded = !goalTasksExpanded">{{ goalTasksExpanded ? '收起' : `展开全部 ${pendingGoalTasks.length} 项` }}</button>
            </div>
            <section v-for="group in visiblePendingTaskGroups" :key="group.key" class="task-group">
              <header class="task-group__head"><span>{{ group.label }}</span><em>{{ group.tasks.length }} 项</em></header>
              <article v-for="task in group.tasks" :key="task.id"><button type="button" class="check" :class="{ done: task.isCompleted }" :disabled="busyAction === `task-${task.id}`" :title="task.isCompleted ? '点击取消完成' : '点击标记完成'" :aria-label="(task.isCompleted ? '取消完成：' : '标记完成：') + task.taskName" @click="toggleTask(task)">{{ task.isCompleted ? '✓' : '' }}</button><div><strong :class="{ done: task.isCompleted }">{{ task.taskName }}</strong><p>{{ task.description || task.stage || '暂无任务说明' }}</p><small>{{ task.progressPercent || 0 }}% · 预计 {{ task.estimatedDays || 1 }} 天</small><div v-if="task.subtasks?.length" class="subtasks"><button v-for="subtask in task.subtasks" :key="subtask.id" type="button" :class="{ done: subtask.isCompleted }" :disabled="busyAction === `subtask-${subtask.id}`" :title="subtask.isCompleted ? '点击取消完成' : '点击标记完成'" :aria-label="(subtask.isCompleted ? '取消完成：' : '标记完成：') + subtask.taskName" @click="toggleSubtask(subtask)">{{ subtask.isCompleted ? '✓' : '·' }} {{ subtask.taskName }}</button></div></div></article>
            </section>
            <p v-if="!pendingGoalTasks.length" class="task-group__empty">这个目标的待完成任务已全部完成。</p>
            <button v-if="hiddenPendingTaskCount" type="button" class="task-more" @click="goalTasksExpanded = true">还有 {{ hiddenPendingTaskCount }} 项待完成任务，展开查看</button>
            <div v-if="completedGoalTasks.length" class="task-group task-group--done">
              <button type="button" class="task-done-toggle" @click="completedTasksExpanded = !completedTasksExpanded"><span>已完成任务</span><em>{{ completedGoalTasks.length }} 项</em><i>{{ completedTasksExpanded ? '收起' : '展开' }}</i></button>
              <template v-if="completedTasksExpanded"><article v-for="task in completedGoalTasks" :key="task.id"><button type="button" class="check" :class="{ done: task.isCompleted }" :disabled="busyAction === `task-${task.id}`" :title="task.isCompleted ? '点击取消完成' : '点击标记完成'" :aria-label="(task.isCompleted ? '取消完成：' : '标记完成：') + task.taskName" @click="toggleTask(task)">{{ task.isCompleted ? '✓' : '' }}</button><div><strong :class="{ done: task.isCompleted }">{{ task.taskName }}</strong><p>{{ task.description || task.stage || '暂无任务说明' }}</p><small>{{ task.progressPercent || 0 }}% · 预计 {{ task.estimatedDays || 1 }} 天</small><div v-if="task.subtasks?.length" class="subtasks"><button v-for="subtask in task.subtasks" :key="subtask.id" type="button" :class="{ done: subtask.isCompleted }" :disabled="busyAction === `subtask-${subtask.id}`" :title="subtask.isCompleted ? '点击取消完成' : '点击标记完成'" :aria-label="(subtask.isCompleted ? '取消完成：' : '标记完成：') + subtask.taskName" @click="toggleSubtask(subtask)">{{ subtask.isCompleted ? '✓' : '·' }} {{ subtask.taskName }}</button></div></div></article></template>
            </div>
          </div><div v-else class="empty empty--small"><strong>这个目标还没有任务</strong><p>新建目标时填写学习计划文本，可以让 AI 拆解为可勾选任务。</p></div></template><div v-else class="empty"><strong>请选择一个学习目标</strong><p>目标详情和可勾选任务会显示在这里。</p></div></section>
          </section>
          <p v-if="dataErrors.summary" class="error"><span>{{ dataErrors.summary }}</span><button type="button" class="link" @click="loadLearningData">重新加载</button></p>

          <section class="stats">
            <article><span>已加入课程</span><strong>{{ summaryCourses.length }}</strong><small>门课程</small></article>
            <article><span>已完成题目</span><strong>{{ summaryProblemStats.solvedCount || 0 }}</strong><small>{{ summaryProblemStats.solveRate || 0 }}% 完成度</small></article>
            <article><span>学习任务</span><strong>{{ summaryProjectStats.completed || 0 }}/{{ summaryProjectStats.total || 0 }}</strong><small>已完成 / 全部</small></article>
            <article><span>技能项</span><strong>{{ summarySkills.length }}</strong><small>个技能有进度</small></article>
          </section>

          <section class="panel section">
            <div class="head"><div><small>技能成长</small><h2>技能进度</h2></div><button type="button" class="link" @click="selectTab('recommended')">查看推荐学习</button></div>
            <div v-if="summarySkills.length" class="skill-progress-list">
              <article v-for="skill in summarySkills.slice(0, 8)" :key="skill.code">
                <div><strong>{{ skill.name }}</strong><small>{{ skill.category || '技能' }}</small></div>
                <div class="progress"><i :style="{ width: `${skill.level}%` }" /></div>
                <em>{{ skill.level }}</em>
              </article>
            </div>
            <div v-else class="empty empty--small"><strong>还没有技能进度</strong><p>完成课程章节、通过算法题或完成学习任务后，这里会显示技能成长。</p></div>
          </section>

          <section class="panel section">
            <div class="head"><div><small>最近 30 个学习日</small><h2>技能增长趋势</h2></div></div>
            <div v-if="trendPoints.length" class="trend-chart">
              <div v-for="point in trendPoints" :key="point.date" class="trend-col" :title="`${point.date} · 平均技能等级 ${point.averageLevel} · ${point.eventCount} 条记录`">
                <i :style="{ height: trendHeight(point) }" />
                <small>{{ point.date.slice(5) }}</small>
              </div>
            </div>
            <div v-else class="empty empty--small"><strong>暂无趋势数据</strong><p>有学习记录后，会按天展示技能平均等级变化。</p></div>
          </section>

          <section class="panel section">
            <div class="head"><div><small>课程学习</small><h2>课程进度</h2></div><button type="button" class="link" @click="goToCourses('enrolled')">查看我加入的课程</button></div>
            <div v-if="summaryCourses.length" class="summary-list">
              <article v-for="course in summaryCourses" :key="course.courseId">
                <div><strong>{{ course.name }}</strong><small>{{ course.skills.join('、') || '暂无技能标签' }}</small></div>
                <div class="progress"><i :style="{ width: `${course.progressPercent}%` }" /></div>
                <em>{{ course.completedChapters }}/{{ course.totalChapters }}</em>
                <button type="button" class="link" @click="router.push(`/courses/${course.courseId}`)">继续</button>
              </article>
            </div>
            <div v-else class="empty empty--small"><strong>还没有加入课程</strong><p>在「课程与专项」中加入课程后，这里会显示章节进度。</p></div>
          </section>

          <section class="panel section">
            <div class="head"><div><small>来自学习路径</small><h2>学习任务进度</h2></div><button type="button" class="link" @click="selectTab('projects')">查看项目实训</button></div>
            <div v-if="summaryProjectItems.length" class="summary-list">
              <article v-for="item in summaryProjectItems" :key="item.itemId">
                <div><strong>{{ item.title }}</strong><small>{{ item.skills.join('、') || '暂无技能标签' }}</small></div>
                <em>{{ statusLabel(item.status) }}</em>
              </article>
            </div>
            <div v-else class="empty empty--small"><strong>暂无学习任务</strong><p>生成学习路径后，路径里的任务节点会汇总到这里。</p></div>
          </section>

          <section class="panel section">
            <div class="head"><div><small>试卷生成</small><h2>我的练习卷</h2></div><button type="button" class="link" @click="router.push('/paper')">生成新试卷</button></div>
            <div v-if="myPapers.length" class="summary-list">
              <article v-for="paper in myPapers.slice(0, 5)" :key="paper.id">
                <div><strong>{{ paper.title }}</strong><small>{{ paper.questionCount }} 题 · 满分 {{ paper.totalScore }} 分 · {{ paper.createdAt }}</small></div>
                <button type="button" class="link" @click="router.push({ path: '/paper', query: { paperId: paper.id, from: 'learning' } })">打开</button>
              </article>
            </div>
            <div v-else class="empty empty--small"><strong>还没有保存过试卷</strong><p>到「Python 试卷生成」按知识点组一份卷子，保存后就能在这里回看和打印。</p><button type="button" class="btn" @click="router.push('/paper')">去生成试卷</button></div>
          </section>
        </template>
      </div>
    </main>

    <Teleport to="body">
      <div v-if="goalDialogOpen" class="dialog-mask" @click.self="closeGoalDialog">
        <form class="dialog" @submit.prevent="saveGoal()">
          <header><div><h2>设置学习目标</h2></div><button type="button" aria-label="关闭" @click="closeGoalDialog">×</button></header>
          <label><span>目标名称 *</span><input v-model="goalForm.title" maxlength="120" type="text" placeholder="例如：通过大学英语六级" /></label>
          <label><span>目标说明</span><textarea v-model="goalForm.description" rows="3" placeholder="说明目标、当前基础和希望达到的结果" /></label>
          <label><span>学习计划文本</span><textarea v-model="goalForm.planText" rows="5" placeholder="例如：每天背 30 个单词，周一三五练习听力，周末完成一套真题。填写后会调用 AI 拆解为任务。" /></label>
          <div class="dialog-row"><label><span>开始日期</span><input v-model="goalForm.startDate" type="date" /></label><label><span>目标日期</span><input v-model="goalForm.targetDate" type="date" /></label><label><span>每日分钟</span><input v-model.number="goalForm.dailyStudyMinutes" type="number" min="10" max="600" /></label></div>
          <p v-if="goalError" class="error">{{ goalError }}</p>
          <footer><button type="button" class="btn" :disabled="goalSaving" @click="closeGoalDialog">取消</button><button v-if="goalAiUnavailable" type="button" class="btn btn--primary" :disabled="goalSaving" @click="saveGoal(true)">{{ goalSaving ? '正在保存…' : '直接保存（不拆解）' }}</button><button v-else type="submit" class="btn btn--primary" :disabled="goalSaving">{{ goalSaving ? '正在保存…' : (goalFromGaps ? '生成学习目标' : (goalForm.planText.trim() ? 'AI 拆解并保存' : '保存目标')) }}</button></footer>
        </form>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
/* ============================================================
 * 学习实践页 · 与首页统一的 hp 视觉体系
 * 暖白底色 + 白色卡片 + 低饱和蓝灰强调，轻边框轻阴影；
 * 动效只做有目的的入场、进度填充与悬停反馈。
 * ============================================================ */

.learning-page {
  min-height: 100vh;
  color: var(--hp-ink);
  background: var(--hp-bg);
  font-family: Inter, 'Segoe UI', system-ui, -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.learning-page *,
.learning-page *::before,
.learning-page *::after {
  box-sizing: border-box;
}

.learning-shell {
  width: min(1280px, calc(100% - 40px));
  margin: 0 auto;
  padding: 96px 0 80px;
}

/* ---------- 动画关键帧 ---------- */

@keyframes lp-fade-up {
  from { opacity: 0; transform: translateY(16px); }
  to { opacity: 1; transform: none; }
}

@keyframes lp-pop {
  from { opacity: 0; transform: translateY(12px) scale(0.97); }
  to { opacity: 1; transform: none; }
}

@keyframes lp-mask-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes lp-fill {
  from { width: 0; }
}

/* ---------- Hero：紧凑横向信息区 ---------- */

.learning-hero {
  position: relative;
  display: flex;
  flex-direction: column;
  padding: 30px 34px 0;
  border: 1px solid #4e3a30;
  border-radius: 28px 28px 76px 28px;
  background: #3d3029;
  box-shadow: 0 18px 42px rgba(68, 45, 30, .12);
  overflow: hidden;
  animation: lp-fade-up 0.5s cubic-bezier(0.22, 0.61, 0.36, 1) both;
}

.learning-hero::after {
  position: absolute;
  top: -210px;
  right: -85px;
  width: 400px;
  height: 400px;
  border: 1px solid rgba(239, 204, 158, .2);
  border-radius: 50%;
  box-shadow: 0 0 0 38px rgba(239, 204, 158, .035), 0 0 0 78px rgba(239, 204, 158, .025);
  content: '';
  pointer-events: none;
}

.hero-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 2px 0 20px;
}

.hero-info {
  display: grid;
  gap: 7px;
  min-width: 0;
}

.learning-hero h1 {
  margin: 0;
  color: #fff8ed;
  font-size: clamp(34px, 4vw, 48px);
  font-weight: 800;
  letter-spacing: -.05em;
  line-height: 1.2;
}

.learning-eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 9px;
  color: #e9bd92;
  font-size: 10px;
  font-weight: 750;
  letter-spacing: .14em;
}

.learning-eyebrow i { width: 20px; height: 1px; background: currentColor; }

.hero-line {
  margin: 0;
  color: #e4d6c8;
  font-size: 13.5px;
  line-height: 1.6;
}

.hero-line strong {
  color: #f1bd87;
  font-weight: 600;
}

/* 目标岗位卡片底部的两个操作入口 */
.focus-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 14px;
  margin-top: auto;
  padding-top: 16px;
}

/* 提高优先级，覆盖 .focus-panel .btn--primary 的 margin-top:auto */
.focus-panel .focus-actions .btn--primary {
  margin-top: 0;
  align-self: center;
}

.hero-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.hero-actions .link {
  padding: 0 6px;
  color: #f0d8c1;
  font-size: 14px;
}

.learning-hero .btn--primary {
  border-color: #e4a273;
  color: #35251d;
  background: #e4a273;
  box-shadow: 0 8px 18px rgba(15, 10, 7, .16);
}

.learning-hero .btn--primary:hover {
  background: #f0b88c;
  box-shadow: 0 12px 22px rgba(15, 10, 7, .2);
}

/* Hero 底部真实数据条 */

.hero-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin: 0 -34px;
  border-top: 1px solid rgba(255, 245, 230, .18);
  position: relative;
  z-index: 1;
}

.hero-stat {
  display: grid;
  align-content: center;
  gap: 2px;
  padding: 12px 24px;
  border-right: 1px solid rgba(255, 245, 230, .16);
  transition: background 0.18s ease;
}

.hero-stat:last-child {
  border-right: 0;
}

.hero-stat:hover {
  background: rgba(255, 255, 255, .07);
}

.hero-stat strong {
  color: #fff8ed;
  font-size: 23px;
  font-weight: 700;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
}

.hero-stat span {
  color: #d5c1ae;
  font-size: 12px;
}

/* ---------- Tabs ---------- */

.tabs {
  position: sticky;
  top: 60px;
  z-index: 20;
  display: flex;
  gap: 4px;
  margin: 20px 0 18px;
  padding: 5px;
  overflow-x: auto;
  border: 1px solid var(--hp-line);
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: var(--hp-shadow-sm);
  backdrop-filter: blur(6px);
  animation: lp-fade-up 0.5s cubic-bezier(0.22, 0.61, 0.36, 1) 0.1s both;
}

.tabs button {
  flex: 0 0 auto;
  min-height: 40px;
  padding: 0 18px;
  border: 0;
  border-radius: 999px;
  color: var(--hp-ink-2);
  background: transparent;
  font-size: 13.5px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.22s ease, color 0.22s ease;
}

.tabs button:hover {
  color: var(--hp-ink);
  background: var(--hp-surface-2);
}

.tabs button.active {
  color: #ffffff;
  background: #9c5d40;
}

/* ---------- 按钮与链接 ---------- */

.btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 46px;
  padding: 0 24px;
  border: 1px solid var(--hp-line-strong);
  border-radius: 999px;
  color: var(--hp-ink);
  background: var(--hp-surface);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.btn:hover {
  transform: translateY(-1px);
  border-color: var(--hp-ink);
  box-shadow: var(--hp-shadow-sm);
}

.btn:disabled {
  opacity: 0.55;
  cursor: wait;
  transform: none;
  box-shadow: none;
}

.btn--primary {
  border-color: var(--hp-ink);
  color: #ffffff;
  background: var(--hp-ink);
  box-shadow: var(--hp-shadow-sm);
}

.btn--primary:hover {
  box-shadow: var(--hp-shadow-md);
}

.link {
  padding: 0;
  border: 0;
  color: #9b6046;
  background: transparent;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: opacity 0.18s ease;
}

.link:hover {
  opacity: 0.72;
}

/* ---------- 布局骨架与区块入场 ---------- */

.content {
  display: grid;
  gap: 18px;
}

.section {
  display: grid;
  gap: 14px;
}

.panel {
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-md);
  background: var(--hp-surface);
  box-shadow: var(--hp-shadow-sm);
}

.focus-panel,
.goal-panel,
.section,
.toolbar {
  padding: 26px 28px;
}

.focus-panel { background: #f3e1d6; border-color: #e8cec0; }
.goal-panel { background: #e8eadf; border-color: #d7dfc9; }
.section { background: #fffaf3; border-color: #e8ddcf; }
.toolbar { background: #f4e9db; border-color: #e7d6c4; }

.content > * {
  animation: lp-fade-up 0.5s cubic-bezier(0.22, 0.61, 0.36, 1) both;
}

.content > *:nth-child(1) { animation-delay: 0.05s; }
.content > *:nth-child(2) { animation-delay: 0.12s; }
.content > *:nth-child(3) { animation-delay: 0.19s; }
.content > *:nth-child(4) { animation-delay: 0.26s; }
.content > *:nth-child(5) { animation-delay: 0.33s; }
.content > *:nth-child(6) { animation-delay: 0.4s; }

/* 卡片级瀑布入场：各分页网格与列表的子项依次浮现 */
.content :is(.card-grid, .quick-grid, .course-grid, .external-grid, .exam-goal-grid, .stats, .path-list, .project-list, .question-list, .skill-progress-list, .summary-list) > * {
  animation: lp-fade-up 0.45s cubic-bezier(0.22, 0.61, 0.36, 1) both;
}

.content :is(.card-grid, .quick-grid, .course-grid, .external-grid, .exam-goal-grid, .stats, .path-list, .project-list, .question-list, .skill-progress-list, .summary-list) > *:nth-child(1) { animation-delay: 0.1s; }
.content :is(.card-grid, .quick-grid, .course-grid, .external-grid, .exam-goal-grid, .stats, .path-list, .project-list, .question-list, .skill-progress-list, .summary-list) > *:nth-child(2) { animation-delay: 0.16s; }
.content :is(.card-grid, .quick-grid, .course-grid, .external-grid, .exam-goal-grid, .stats, .path-list, .project-list, .question-list, .skill-progress-list, .summary-list) > *:nth-child(3) { animation-delay: 0.22s; }
.content :is(.card-grid, .quick-grid, .course-grid, .external-grid, .exam-goal-grid, .stats, .path-list, .project-list, .question-list, .skill-progress-list, .summary-list) > *:nth-child(4) { animation-delay: 0.28s; }
.content :is(.card-grid, .quick-grid, .course-grid, .external-grid, .exam-goal-grid, .stats, .path-list, .project-list, .question-list, .skill-progress-list, .summary-list) > *:nth-child(5) { animation-delay: 0.34s; }
.content :is(.card-grid, .quick-grid, .course-grid, .external-grid, .exam-goal-grid, .stats, .path-list, .project-list, .question-list, .skill-progress-list, .summary-list) > *:nth-child(6) { animation-delay: 0.4s; }
.content :is(.card-grid, .quick-grid, .course-grid, .external-grid, .exam-goal-grid, .stats, .path-list, .project-list, .question-list, .skill-progress-list, .summary-list) > *:nth-child(7) { animation-delay: 0.46s; }
.content :is(.card-grid, .quick-grid, .course-grid, .external-grid, .exam-goal-grid, .stats, .path-list, .project-list, .question-list, .skill-progress-list, .summary-list) > *:nth-child(n + 8) { animation-delay: 0.52s; }

.focus-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.92fr) minmax(320px, 1.08fr);
  gap: 20px;
}

/* 两卡纵向弹性布局：内容少的卡片由底部按钮吸收富余空间，左右 CTA 保持对齐 */
.focus-panel,
.goal-panel {
  display: flex;
  flex-direction: column;
}

.focus-panel .btn--primary,
.goal-panel .link {
  margin-top: auto;
  align-self: flex-start;
}

/* ---------- 区块头部 ---------- */

.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.head small {
  display: block;
  margin-bottom: 6px;
  color: #a65f44;
  font-size: 12.5px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.head h2,
.toolbar h2 {
  margin: 0;
  color: var(--hp-ink);
  font-size: 21px;
  line-height: 1.3;
  letter-spacing: -0.02em;
}

.head > strong {
  color: var(--hp-ink);
  font-size: 26px;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
}

.tag {
  display: inline-flex;
  align-items: center;
  padding: 5px 11px;
  border-radius: 999px;
  color: #8b573e;
  background: #f0dfcf;
  font-size: 11.5px;
  font-weight: 600;
  white-space: nowrap;
}

.focus-panel p,
.goal-panel p,
.toolbar p {
  margin: 12px 0 0;
  color: var(--hp-ink-2);
  font-size: 14px;
  line-height: 1.75;
}

.note {
  color: var(--hp-muted);
  font-size: 13px;
  line-height: 1.7;
}

.head-note {
  display: block;
  margin-top: 6px;
  color: var(--hp-muted);
  font-size: 12.5px;
  line-height: 1.6;
}

/* ---------- 进度条与任务预览 ---------- */

.progress {
  width: 100%;
  height: 8px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--hp-track);
}

.progress i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--hp-blue-ink);
  animation: lp-fill 0.9s cubic-bezier(0.25, 0.7, 0.3, 1) 0.15s backwards;
  transition: width 0.3s ease;
}

.task-preview {
  display: grid;
  gap: 2px;
  margin: 14px 0 0;
  padding: 0;
  list-style: none;
}

.task-preview button {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 10px;
  padding: 9px 2px;
  border: 0;
  border-radius: 8px;
  color: var(--hp-ink-2);
  background: transparent;
  font-size: 13.5px;
  text-align: left;
  cursor: pointer;
  transition: color 0.18s ease, background 0.18s ease;
}

.task-preview button:hover {
  color: var(--hp-ink);
  background: var(--hp-surface-2);
}

.task-preview button:disabled {
  opacity: 0.55;
  cursor: wait;
}

.task-preview i {
  width: 17px;
  height: 17px;
  flex: none;
  border: 1px solid var(--hp-line-strong);
  border-radius: 5px;
  background: var(--hp-surface);
  transition: border-color 0.18s ease;
}

.task-preview button:hover i {
  border-color: var(--hp-blue-ink);
}

/* ---------- 状态与提示 ---------- */

.error {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 0;
  padding: 10px 14px;
  border-radius: 10px;
  color: #a14d55;
  background: var(--hp-pink-soft);
  font-size: 13px;
}

.error .link {
  flex: none;
  color: #a14d55;
  text-decoration: underline;
}

.state,
.message {
  margin: 0 0 4px;
  padding: 14px 18px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-sm);
  color: var(--hp-ink-2);
  background: var(--hp-surface);
  font-size: 14px;
  box-shadow: var(--hp-shadow-sm);
}

.message {
  color: var(--hp-green-ink);
  background: #eef4ea;
}

.empty {
  display: grid;
  justify-items: center;
  gap: 6px;
  padding: 28px 22px;
  border: 1px dashed var(--hp-line-strong);
  border-radius: var(--hp-r-sm);
  color: var(--hp-muted);
  background: var(--hp-surface-2);
  text-align: center;
}

.empty strong {
  color: var(--hp-ink);
  font-size: 15px;
}

.empty p {
  margin: 6px 0 0;
  max-width: 480px;
  font-size: 13px;
  line-height: 1.7;
}

.empty .btn {
  margin-top: 14px;
}

.empty--small {
  padding: 20px 16px;
}

/* ---------- 卡片网格 ---------- */

.card-grid,
.quick-grid,
.course-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.content-card,
.quick-grid button,
.course-card {
  display: grid;
  gap: 8px;
  align-content: start;
  min-height: 150px;
  padding: 18px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-sm);
  color: var(--hp-ink);
  background: var(--hp-surface);
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.content-card:hover,
.quick-grid button:hover,
.course-card:hover {
  transform: translateY(-3px);
  border-color: var(--hp-line-strong);
  box-shadow: var(--hp-shadow-md);
}

.content-card:nth-child(3n + 1),
.quick-grid button:nth-child(3n + 1) { background: #f7e8dc; border-color: #ecd5c3; }
.content-card:nth-child(3n + 2),
.quick-grid button:nth-child(3n + 2) { background: #e8edf0; border-color: #d8e1e5; }
.content-card:nth-child(3n),
.quick-grid button:nth-child(3n) { background: #e9ecdf; border-color: #dbe1ce; }

.course-grid .course-card:nth-child(3n + 1) { background: #f5e1dc; border-color: #e9cdc6; }
.course-grid .course-card:nth-child(3n + 2) { background: #e4ebf1; border-color: #d2dfe8; }
.course-grid .course-card:nth-child(3n) { background: #e7ebdf; border-color: #d6dfcb; }

.content-card small {
  color: #9b6046;
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.content-card strong,
.quick-grid strong,
.course-card strong {
  color: var(--hp-ink);
  font-size: 15px;
}

.content-card p,
.quick-grid span,
.course-card p {
  margin: 0;
  color: var(--hp-muted);
  font-size: 12.5px;
  line-height: 1.65;
}

.content-card em,
.quick-grid em {
  align-self: end;
  color: #9b6046;
  font-size: 12.5px;
  font-style: normal;
  font-weight: 600;
}
.skill-line {
  color: var(--hp-muted);
  font-size: 11.5px;
  font-variant-numeric: tabular-nums;
}

/* ---------- 学习路径列表 ---------- */

.path-list {
  display: grid;
  gap: 10px;
}

.path-list article {
  display: grid;
  grid-template-columns: 44px minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-sm);
  background: var(--hp-surface);
  transition: border-color 0.18s ease, background 0.18s ease, transform 0.18s ease;
}

.path-list article:hover {
  transform: translateY(-1px);
  border-color: var(--hp-line-strong);
}

.path-list article.current {
  border-color: #c4d8e8;
  background: var(--hp-tint);
}

.path-list article.done {
  opacity: 0.72;
}

.path-list article > span {
  color: var(--hp-blue-ink);
  font-size: 12px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.path-list strong {
  display: block;
  color: var(--hp-ink);
  font-size: 14.5px;
}

.path-list p {
  margin: 4px 0 0;
  color: var(--hp-muted);
  font-size: 12.5px;
}

.path-list em {
  color: var(--hp-muted);
  font-size: 12px;
  font-style: normal;
  font-weight: 600;
}

.path-list article.current em {
  color: var(--hp-blue-ink);
}

.path-list article.done em {
  color: var(--hp-green-ink);
}

.path-list button {
  min-height: 34px;
  padding: 0 13px;
  border: 1px solid var(--hp-line-strong);
  border-radius: 999px;
  color: var(--hp-ink);
  background: var(--hp-surface);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.18s ease, background 0.18s ease;
}

.path-list button:hover {
  border-color: var(--hp-ink);
  background: var(--hp-surface-2);
}

.path-list button:disabled {
  opacity: 0.55;
  cursor: wait;
}

/* ---------- 工具条与统计 ---------- */

.toolbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.toolbar-actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.toolbar p {
  margin: 9px 0 0;
  color: var(--hp-ink-2);
  font-size: 13.5px;
  line-height: 1.7;
}

/* ---------- 目标岗位筛选 ---------- */

.expand-row {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 14px;
  margin-top: 16px;
}

.expand-row .note {
  margin: 0;
}

.expand-toggle {
  padding: 8px 18px;
  border: 1px solid var(--hp-line-strong);
  border-radius: 999px;
  color: var(--hp-ink-2);
  background: var(--hp-surface);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.2s ease, color 0.2s ease;
}

.expand-toggle:hover {
  border-color: var(--hp-blue-ink);
  color: var(--hp-blue-ink);
}

.job-filter {
  display: grid;
  flex-shrink: 0;
  gap: 8px;
  justify-items: end;
}

.job-filter__label {
  color: var(--hp-muted);
  font-size: 12.5px;
}

.job-filter__label strong {
  color: var(--hp-ink);
}

.job-filter__buttons {
  display: inline-flex;
  padding: 3px;
  border: 1px solid var(--hp-line);
  border-radius: 999px;
  background: var(--hp-surface-2);
}

.job-filter__buttons button {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border: 0;
  border-radius: 999px;
  color: var(--hp-muted);
  background: transparent;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
}

.job-filter__buttons button.active {
  color: #fff;
  background: var(--hp-ink);
}

.job-filter__buttons button span {
  padding: 0 6px;
  border-radius: 999px;
  background: rgba(255, 255, 255, .22);
  font-size: 11px;
  font-variant-numeric: tabular-nums;
}

.match-badge {
  display: inline-block;
  margin-left: 8px;
  padding: 2px 8px;
  border: 1px solid #bcd2e2;
  border-radius: 999px;
  color: var(--hp-blue-ink);
  background: #eef5fa;
  font-size: 11px;
  font-weight: 600;
  vertical-align: middle;
}

.course-card .match-badge {
  justify-self: start;
  margin-left: 0;
}

.external-card__badges {
  display: inline-flex;
  flex-shrink: 0;
  align-items: center;
  gap: 6px;
}

.external-card__badges .match-badge {
  margin-left: 0;
}

.stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.stats article {
  display: grid;
  align-content: start;
  gap: 5px;
  padding: 20px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-sm);
  background: var(--hp-surface);
  box-shadow: var(--hp-shadow-sm);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.stats article:hover {
  transform: translateY(-2px);
  box-shadow: var(--hp-shadow-md);
}

.stats span {
  color: var(--hp-muted);
  font-size: 12.5px;
}

.stats strong {
  color: var(--hp-ink);
  font-size: 27px;
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
}

.stats small {
  color: var(--hp-muted);
  font-size: 11.5px;
}

/* ---------- 建议练习题目 ---------- */

.question-list {
  display: grid;
  gap: 10px;
}

.question-list > button {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr) minmax(120px, 0.7fr) auto;
  align-items: center;
  gap: 12px;
  padding: 13px 16px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-sm);
  color: var(--hp-ink);
  background: var(--hp-surface);
  text-align: left;
  cursor: pointer;
  transition: border-color 0.18s ease, background 0.18s ease, transform 0.18s ease, box-shadow 0.18s ease;
}

.question-list > button:hover {
  border-color: var(--hp-line-strong);
  background: var(--hp-surface-2);
  transform: translateY(-1px);
  box-shadow: var(--hp-shadow-sm);
}

.question-list strong {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.question-list > button > span:last-of-type {
  color: var(--hp-muted);
  font-size: 12.5px;
}

.question-list em {
  color: var(--hp-blue-ink);
  font-size: 12.5px;
  font-style: normal;
  font-weight: 600;
}

.diff {
  display: inline-flex;
  justify-content: center;
  padding: 5px 10px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 600;
}

.diff--easy {
  color: var(--hp-green-ink);
  background: var(--hp-green);
}

.diff--medium {
  color: var(--hp-yellow-ink);
  background: var(--hp-yellow);
}

.diff--hard {
  color: var(--hp-pink-ink);
  background: var(--hp-pink);
}

/* ---------- 我的练习聚合 ---------- */

.skill-progress-list {
  display: grid;
  gap: 12px;
}

.skill-progress-list article {
  display: grid;
  grid-template-columns: minmax(120px, 200px) minmax(0, 1fr) 44px;
  align-items: center;
  gap: 12px;
}

.skill-progress-list article > div:first-child {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.skill-progress-list strong {
  font-size: 13px;
}

.skill-progress-list small {
  color: var(--hp-muted);
  font-size: 11.5px;
}

.skill-progress-list em {
  color: var(--hp-blue-ink);
  font-size: 13px;
  font-style: normal;
  font-weight: 700;
  text-align: right;
  font-variant-numeric: tabular-nums;
}

.trend-chart {
  display: flex;
  align-items: flex-end;
  gap: 6px;
  height: 150px;
  padding: 12px 4px 0;
  border-bottom: 1px solid var(--hp-line);
}

.trend-col {
  display: flex;
  flex: 1 1 0;
  flex-direction: column;
  align-items: center;
  justify-content: flex-end;
  gap: 6px;
  height: 100%;
  min-width: 0;
}

.trend-col i {
  display: block;
  width: 100%;
  max-width: 26px;
  border-radius: 4px 4px 0 0;
  background: var(--hp-blue);
}

.trend-col small {
  color: var(--hp-muted);
  font-size: 10px;
  white-space: nowrap;
}

.summary-list {
  display: grid;
  gap: 12px;
}

.summary-list article {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(120px, 220px) auto auto;
  align-items: center;
  gap: 12px;
}

.summary-list article > div:first-child {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.summary-list strong {
  font-size: 13.5px;
}

.summary-list small {
  color: var(--hp-muted);
  font-size: 11.5px;
}

.summary-list em {
  color: var(--hp-ink-2);
  font-size: 12.5px;
  font-style: normal;
  font-variant-numeric: tabular-nums;
}

/* ---------- 外部精选课程 ---------- */

.external-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.external-card {
  display: grid;
  gap: 8px;
  align-content: start;
  min-height: 150px;
  padding: 18px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-sm);
  color: var(--hp-ink);
  background: var(--hp-surface);
  text-decoration: none;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.external-card:hover {
  transform: translateY(-3px);
  border-color: var(--hp-line-strong);
  box-shadow: var(--hp-shadow-md);
}

.external-card__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.external-card__top small {
  color: var(--hp-blue-ink);
  font-size: 11.5px;
  font-weight: 600;
}

.external-badge {
  padding: 2px 8px;
  border: 1px solid var(--hp-line);
  border-radius: 999px;
  color: var(--hp-muted);
  background: var(--hp-surface-2);
  font-size: 11px;
}

.external-card strong {
  color: var(--hp-ink);
  font-size: 15px;
}

.external-card p {
  margin: 0;
  color: var(--hp-muted);
  font-size: 12.5px;
  line-height: 1.65;
}

.external-card em {
  align-self: end;
  color: var(--hp-blue-ink);
  font-size: 12.5px;
  font-style: normal;
  font-weight: 600;
}

/* ---------- 课程卡片 ---------- */

.course-card {
  grid-template-columns: 54px minmax(0, 1fr);
  align-items: center;
}

.course-card > span {
  display: grid;
  width: 54px;
  height: 68px;
  place-items: center;
  border-radius: 10px 14px 14px 10px;
  color: var(--hp-blue-ink);
  background: var(--hp-blue);
  font-size: 22px;
  font-weight: 700;
  box-shadow: inset 4px 0 rgba(92, 140, 180, 0.35);
}

.course-card > div {
  display: grid;
  gap: 7px;
  min-width: 0;
}

.course-card .progress {
  height: 6px;
}

.course-card small {
  color: var(--hp-muted);
  font-size: 11.5px;
}

.course-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.course-tag {
  padding: 3px 9px;
  border: 1px solid var(--hp-line);
  border-radius: 999px;
  color: var(--hp-muted);
  background: var(--hp-surface-2);
  font-size: 11px;
  font-weight: 600;
}

.course-tag.is-primary {
  border-color: var(--hp-line-strong);
  color: var(--hp-blue-ink);
}

.course-tag--level {
  color: var(--hp-ink-2);
}

.course-jobs {
  margin: 0;
  color: var(--hp-muted);
  font-size: 11.5px;
}

/* ---------- 我的考试记录（课程与专项页） ---------- */

.exam-summary {
  margin: 12px 0 0;
  color: var(--hp-muted);
  font-size: 13px;
}

.exam-record-list {
  display: grid;
  gap: 10px;
  margin-top: 16px;
}

.exam-record {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  width: 100%;
  padding: 14px 16px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-sm);
  color: var(--hp-ink);
  background: var(--hp-surface);
  text-align: left;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.exam-record:hover {
  border-color: var(--hp-line-strong);
  box-shadow: var(--hp-shadow-sm);
}

.exam-record > div {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.exam-record strong {
  font-size: 14.5px;
}

.exam-record small {
  color: var(--hp-muted);
  font-size: 12.5px;
}

.exam-record em {
  flex: none;
  color: var(--hp-blue-ink);
  font-size: 12.5px;
  font-style: normal;
  font-weight: 600;
  white-space: nowrap;
}

/* ---------- 考试与证书专项（展示真实备考目标） ---------- */

.exam-goal-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.exam-goal {
  display: grid;
  gap: 8px;
  padding: 16px 18px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-sm);
  background: var(--hp-surface);
}

.exam-goal__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.exam-goal__head strong {
  color: var(--hp-ink);
  font-size: 14px;
}

.exam-goal p {
  margin: 0;
  color: var(--hp-muted);
  font-size: 12.5px;
  line-height: 1.6;
}

.exam-goal__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.exam-goal__foot small {
  color: var(--hp-muted);
  font-size: 11.5px;
  font-variant-numeric: tabular-nums;
}

/* ---------- 规划中入口（紧凑占位，不展示虚构内容） ---------- */

.special-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.special-row .note {
  flex: 1 1 260px;
}

.special-chip {
  padding: 7px 14px;
  border: 1px solid var(--hp-line-strong);
  border-radius: 999px;
  color: var(--hp-ink-2);
  background: var(--hp-surface-2);
  font-size: 12.5px;
  font-weight: 600;
}

/* ---------- 项目实训 ---------- */

.project-list {
  display: grid;
  gap: 12px;
}

.project-list article {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto;
  align-items: center;
  gap: 16px;
  padding: 20px 22px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-sm);
  background: var(--hp-surface);
  box-shadow: var(--hp-shadow-sm);
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.project-list article:hover {
  transform: translateY(-2px);
  box-shadow: var(--hp-shadow-md);
}

.project-list article > div > span {
  color: var(--hp-blue-ink);
  font-size: 11.5px;
  font-weight: 600;
  letter-spacing: 0.02em;
}

.project-list h3 {
  margin: 6px 0 4px;
  color: var(--hp-ink);
  font-size: 17px;
}

.project-list p {
  margin: 0;
  color: var(--hp-muted);
  font-size: 13px;
}

.project-list em {
  color: var(--hp-ink-2);
  font-size: 12.5px;
  font-style: normal;
  font-weight: 600;
}

.project-list .project-deliverable {
  margin-top: 4px;
  color: var(--hp-ink-2);
}

.project-list .project-hours {
  margin-top: 4px;
  color: var(--hp-muted);
  font-size: 12px;
}

.project-list .project-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.project-list .project-done-badge {
  display: inline-block;
  margin-left: 8px;
  padding: 2px 8px;
  border: 1px solid #cfe3d4;
  border-radius: 999px;
  color: var(--hp-green-ink);
  background: var(--hp-green);
  font-size: 11px;
  font-weight: 600;
  vertical-align: middle;
}

.project-list .project-actions .btn:disabled {
  opacity: 0.6;
  cursor: default;
}

.head-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

/* ---------- 我的练习：目标 + 详情 ---------- */

.next-task {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-top: 16px;
  padding: 14px 16px;
  border: 1px solid #d5e3ef;
  border-radius: var(--hp-r-sm);
  background: var(--hp-tint);
}

.next-task small {
  display: block;
  margin-bottom: 4px;
  color: var(--hp-blue-ink);
  font-size: 11.5px;
  font-weight: 600;
}

.next-task strong {
  color: var(--hp-ink);
  font-size: 14.5px;
}

.next-task p {
  margin: 4px 0 0;
  color: var(--hp-muted);
  font-size: 12.5px;
  line-height: 1.6;
}

.next-task .btn {
  flex: none;
  min-height: 38px;
  padding: 0 16px;
  font-size: 13px;
}

.practice-layout {
  display: grid;
  grid-template-columns: 300px minmax(0, 1fr);
  gap: 0;
  padding: 0;
  overflow: hidden;
}

.goal-list {
  display: grid;
  align-content: start;
  gap: 8px;
  padding: 22px 18px;
  border-right: 1px solid var(--hp-line);
  background: var(--hp-surface-2);
}

.goal-list .head {
  align-items: center;
  margin-bottom: 10px;
}

.goal-list .head h2 {
  font-size: 16px;
}

.goal-list .head span {
  color: var(--hp-muted);
  font-size: 12px;
}

.goal-list > button {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 14px;
  border: 1px solid transparent;
  border-radius: var(--hp-r-sm);
  color: var(--hp-ink);
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: background 0.18s ease, border-color 0.18s ease;
}

.goal-list > button:hover {
  background: rgba(255, 255, 255, 0.8);
}

.goal-list > button.active {
  border-color: #c4d8e8;
  background: var(--hp-tint);
}

.goal-list button span {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.goal-list button strong {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13.5px;
}

.goal-list button small {
  color: var(--hp-muted);
  font-size: 11.5px;
}

.goal-list button em {
  color: var(--hp-blue-ink);
  font-size: 12.5px;
  font-style: normal;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.goal-detail {
  min-width: 0;
  padding: 24px 26px;
}

.goal-detail .head h2 {
  font-size: 19px;
}

.goal-detail > p {
  margin: 12px 0 0;
  color: var(--hp-ink-2);
  font-size: 13.5px;
  line-height: 1.75;
}

.meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 16px 0;
}

.meta span {
  padding: 5px 11px;
  border: 1px solid var(--hp-line);
  border-radius: 999px;
  color: var(--hp-ink-2);
  background: var(--hp-surface-2);
  font-size: 11.5px;
}

.task-list {
  margin-top: 18px;
}

.task-list article {
  display: grid;
  grid-template-columns: 30px minmax(0, 1fr);
  gap: 12px;
  padding: 14px 0;
  border-top: 1px solid var(--hp-line);
}

.check {
  display: grid;
  width: 24px;
  height: 24px;
  place-items: center;
  border: 1px solid var(--hp-line-strong);
  border-radius: 8px;
  color: #ffffff;
  background: var(--hp-surface);
  font-size: 12px;
  cursor: pointer;
  transition: background 0.18s ease, border-color 0.18s ease;
}

.check:hover {
  border-color: var(--hp-green-ink);
}

/* 已完成时悬停变灰，提示"再点一下可以取消" */
.check.done:hover {
  border-color: var(--hp-muted);
  background: var(--hp-muted);
}

.check:disabled {
  opacity: 0.55;
  cursor: wait;
}

.check.done {
  border-color: var(--hp-green-ink);
  background: var(--hp-green-ink);
}

.task-list strong {
  color: var(--hp-ink);
  font-size: 14px;
  transition: color 0.18s ease;
}

.task-list strong.done,
.subtasks button.done {
  color: var(--hp-muted);
  text-decoration: line-through;
}
.task-list__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--hp-line);
}

.task-list__head > div {
  display: grid;
  gap: 4px;
}

.task-list__head strong {
  color: var(--hp-ink);
  font-size: 14px;
}

.task-list__head small {
  color: var(--hp-muted);
  font-size: 11.5px;
}

.task-group__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 0 2px;
  color: var(--hp-blue-ink);
  font-size: 12px;
  font-weight: 700;
}

.task-group__head em {
  color: var(--hp-muted);
  font-style: normal;
  font-weight: 500;
}

.task-group__empty {
  margin: 14px 0 0;
  color: var(--hp-muted);
  font-size: 12.5px;
}

.task-more {
  width: 100%;
  margin-top: 12px;
  padding: 10px;
  border: 1px dashed var(--hp-line-strong);
  border-radius: var(--hp-r-sm);
  color: var(--hp-blue-ink);
  background: var(--hp-surface-2);
  font-size: 12.5px;
  cursor: pointer;
  transition: border-color 0.18s ease, background 0.18s ease;
}

.task-more:hover {
  border-color: var(--hp-blue-ink);
  background: var(--hp-tint);
}

.task-group--done {
  margin-top: 16px;
  border-top: 1px solid var(--hp-line);
}

.task-done-toggle {
  display: flex;
  width: 100%;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 12px 0;
  border: 0;
  color: var(--hp-ink-2);
  background: transparent;
  font-size: 12.5px;
  cursor: pointer;
}

.task-done-toggle em {
  color: var(--hp-muted);
  font-style: normal;
}

.task-done-toggle i {
  color: var(--hp-blue-ink);
  font-style: normal;
}

.goal-list__next {
  overflow: hidden;
  color: var(--hp-blue-ink) !important;
  text-overflow: ellipsis;
  white-space: nowrap;
}


.task-list p {
  margin: 4px 0;
  color: var(--hp-muted);
  font-size: 12.5px;
}

.task-list small {
  color: var(--hp-muted);
  font-size: 11.5px;
}

.subtasks {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
}

.subtasks button {
  padding: 5px 10px;
  border: 1px solid var(--hp-line);
  border-radius: 999px;
  color: var(--hp-ink-2);
  background: var(--hp-surface-2);
  font-size: 11.5px;
  cursor: pointer;
  transition: border-color 0.18s ease, background 0.18s ease;
}

.subtasks button:hover {
  border-color: var(--hp-line-strong);
}

.subtasks button:disabled {
  opacity: 0.6;
  cursor: wait;
}

.subtasks button.done {
  border-color: transparent;
  color: var(--hp-green-ink);
  background: var(--hp-green);
}

/* ---------- 学习目标对话框 ---------- */

.dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(35, 38, 43, 0.36);
  animation: lp-mask-in 0.2s ease both;
}

.dialog {
  display: grid;
  gap: 15px;
  width: min(640px, 100%);
  max-height: calc(100vh - 40px);
  overflow: auto;
  padding: 26px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-md);
  background: var(--hp-surface);
  box-shadow: var(--hp-shadow-lg);
  animation: lp-pop 0.28s cubic-bezier(0.22, 0.61, 0.36, 1) both;
}

.dialog header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.dialog header h2 {
  margin: 0;
  color: var(--hp-ink);
  font-size: 19px;
}

.dialog header button {
  width: 34px;
  height: 34px;
  border: 0;
  border-radius: 50%;
  color: var(--hp-ink-2);
  background: var(--hp-surface-2);
  font-size: 20px;
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease;
}

.dialog header button:hover {
  color: var(--hp-ink);
  background: var(--hp-line);
}

.dialog label {
  display: grid;
  gap: 7px;
  color: var(--hp-ink-2);
  font-size: 12.5px;
  font-weight: 600;
}

.dialog input,
.dialog textarea {
  width: 100%;
  box-sizing: border-box;
  padding: 10px 13px;
  border: 1px solid var(--hp-line-strong);
  border-radius: 10px;
  outline: none;
  color: var(--hp-ink);
  background: var(--hp-surface);
  font: inherit;
  font-weight: 500;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.dialog input:focus,
.dialog textarea:focus {
  border-color: var(--hp-blue-ink);
  box-shadow: 0 0 0 3px rgba(92, 140, 180, 0.12);
}

.dialog textarea {
  resize: vertical;
  line-height: 1.6;
}

.dialog-row {
  display: grid;
  grid-template-columns: 1fr 1fr 120px;
  gap: 10px;
}

.dialog footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

/* ---------- 响应式 ---------- */

@media (max-width: 1080px) {
  .hero-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hero-stat:nth-child(2) {
    border-right: 0;
  }

  .hero-stat:nth-child(-n + 2) {
    border-bottom: 1px solid #e2eaf1;
  }
}

@media (max-width: 980px) {
  .learning-hero {
    padding: 22px 24px 0;
  }

  .hero-main {
    flex-direction: column;
    align-items: flex-start;
    gap: 14px;
  }

  .hero-stats {
    margin: 0 -24px;
  }

  .focus-grid,
  .practice-layout {
    grid-template-columns: 1fr;
  }

  .goal-list {
    border-right: 0;
    border-bottom: 1px solid var(--hp-line);
  }

  .card-grid,
  .quick-grid,
  .course-grid,
  .external-grid,
  .exam-goal-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

}

@media (max-width: 680px) {
  .learning-shell {
    width: calc(100% - 32px);
    padding: 82px 0 64px;
  }

  .tabs {
    top: 52px;
  }

  .learning-hero {
    padding: 20px 20px 0;
  }

  .hero-stats {
    margin: 0 -20px;
  }

  .card-grid,
  .quick-grid,
  .course-grid,
  .external-grid,
  .exam-goal-grid,
  .stats {
    grid-template-columns: 1fr;
  }

  .toolbar {
    display: grid;
  }

  .path-list article {
    grid-template-columns: 34px minmax(0, 1fr) auto;
  }

  .path-list article button {
    grid-column: 2 / -1;
    justify-self: start;
  }

  .question-list > button {
    grid-template-columns: 52px minmax(0, 1fr);
  }

  .question-list > button > span:last-of-type,
  .question-list em {
    grid-column: 2;
  }

  .dialog-row {
    grid-template-columns: 1fr;
  }
}

@media (prefers-reduced-motion: reduce) {
  .learning-page *,
  .learning-page *::before,
  .learning-page *::after {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
/* ---------- 岗位学习路径 ---------- */
.job-path{display:grid;gap:10px;margin:0;padding:0;list-style:none}
.job-path li{display:grid;grid-template-columns:34px minmax(0,1fr) auto;align-items:center;gap:14px;padding:14px 16px;border:1px solid var(--hp-line);border-radius:var(--hp-r-sm);background:var(--hp-surface)}
.job-path__no{display:grid;place-items:center;width:30px;height:30px;border-radius:50%;color:var(--hp-blue-ink);background:var(--hp-tint);font-size:12px;font-weight:700;font-variant-numeric:tabular-nums}
.job-path__body{display:grid;gap:6px;min-width:0}
.job-path__head{display:flex;align-items:baseline;justify-content:space-between;gap:10px}
.job-path__head strong{color:var(--hp-ink);font-size:14px}
.job-path__level{flex:none;color:var(--hp-muted);font-size:12px;font-variant-numeric:tabular-nums}
.job-path__next{display:flex;align-items:center;gap:7px;margin:0;color:var(--hp-muted);font-size:12.5px;line-height:1.5}
.job-path__next em{flex:none;padding:1px 7px;border:1px solid var(--hp-line-strong);border-radius:999px;color:var(--hp-blue-ink);background:var(--hp-surface-2);font-size:11px;font-style:normal;font-weight:600}
@media (max-width: 680px){.job-path li{grid-template-columns:30px minmax(0,1fr)}.job-path li > button{grid-column:1 / -1;justify-self:stretch}}
.sub-head{display:flex;align-items:baseline;justify-content:space-between;gap:12px;margin:22px 0 12px;padding-top:16px;border-top:1px solid var(--hp-line)}
.sub-head h3{margin:0;color:var(--hp-ink);font-size:15px}
.sub-head span{color:var(--hp-muted);font-size:12px}
.enrolled-badge{display:inline-block;margin-left:8px;padding:2px 8px;border:1px solid #cfe3d4;border-radius:999px;color:var(--hp-green-ink);background:var(--hp-green);font-size:11px;font-weight:600;vertical-align:middle}
.course-card .enrolled-badge{justify-self:start;margin-left:0}
</style>
