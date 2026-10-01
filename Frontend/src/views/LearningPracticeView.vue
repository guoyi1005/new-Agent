<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppTabBar from '../components/AppTabBar.vue'
import { getCampusCourses } from '../api/campusCourse'
import { completePathItem, getPythonHome, startPathItem } from '../api/learning'
import { getPythonProblemList } from '../api/pythonProblem'
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
  { id: 'python', label: 'Python 与算法' },
  { id: 'courses', label: '课程与专项' },
  { id: 'projects', label: '项目实训' },
  { id: 'practice', label: '我的练习' },
]
const TARGET_JOB_STORAGE_KEY = 'home_target_job'

const loading = ref(true)
const pythonHome = ref({})
const problems = ref([])
const courses = ref([])
const studyGoals = ref([])
const selectedGoalId = ref(null)
const selectedGoalDetail = ref(null)
const busyAction = ref('')
const pageMessage = ref('')
const dataErrors = reactive({ python: '', problems: '', courses: '', goals: '' })
const goalDialogOpen = ref(false)
const goalSaving = ref(false)
const goalError = ref('')
const goalForm = reactive({
  title: '',
  description: '',
  planText: '',
  startDate: '',
  targetDate: '',
  dailyStudyMinutes: 60,
})

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
const weakSkills = computed(() => mastery.value.filter((item) => item.status === 'weak'))
const nextPathItem = computed(() => pathItems.value.find((item) => item.status === 'in_progress') || pathItems.value.find((item) => item.status !== 'completed') || null)
const activeGoalSummary = computed(() => studyGoals.value.find((item) => item.id === selectedGoalId.value) || studyGoals.value.find((item) => item.status === 'in_progress') || studyGoals.value.find((item) => item.status === 'pending') || studyGoals.value[0] || null)
const activeGoalTasks = computed(() => Array.isArray(selectedGoalDetail.value?.tasks) ? selectedGoalDetail.value.tasks : [])
const remainingGoalTasks = computed(() => activeGoalTasks.value.filter((task) => !task.isCompleted))
const solvedIds = ref(getSolvedIds().map((id) => String(id)))
const solvedSet = computed(() => new Set(solvedIds.value))
const solvedProblemCount = computed(() => problems.value.filter((item) => solvedSet.value.has(String(item.id))).length)
const judgeableProblems = computed(() => problems.value.filter((item) => item.judgeable))
const solveRate = computed(() => judgeableProblems.value.length ? Math.round(solvedProblemCount.value / judgeableProblems.value.length * 100) : 0)
const nextProblems = computed(() => problems.value
  .filter((item) => item.judgeable && !solvedSet.value.has(String(item.id)))
  .sort((left, right) => difficultyRank(left.difficulty) - difficultyRank(right.difficulty))
  .slice(0, 5))
const projectItems = computed(() => pathItems.value.filter((item) => {
  const text = `${item.stage || ''} ${item.knowledgePoint || ''} ${item.objective || ''}`.toLowerCase()
  return text.includes('项目') || text.includes('实训') || text.includes('实践') || text.includes('project')
}))

async function loadLearningData() {
  loading.value = true
  pageMessage.value = ''
  const [pythonResult, problemResult, courseResult, goalResult] = await Promise.allSettled([
    getPythonHome(),
    getPythonProblemList(),
    getCampusCourses(),
    listMyStudyGoals(1, 20),
  ])
  if (pythonResult.status === 'fulfilled') {
    pythonHome.value = pythonResult.value || {}
    dataErrors.python = ''
  } else {
    pythonHome.value = {}
    dataErrors.python = pythonResult.reason?.message || '学习路径加载失败'
  }
  if (problemResult.status === 'fulfilled') {
    problems.value = Array.isArray(problemResult.value?.data) ? problemResult.value.data : []
    dataErrors.problems = ''
  } else {
    problems.value = []
    dataErrors.problems = problemResult.reason?.message || '题库加载失败'
  }
  if (courseResult.status === 'fulfilled') {
    courses.value = Array.isArray(courseResult.value?.data) ? courseResult.value.data : []
    dataErrors.courses = ''
  } else {
    courses.value = []
    dataErrors.courses = courseResult.reason?.message || '课程加载失败'
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
    dataErrors.goals = goalResult.reason?.message || '学习目标加载失败'
  }
  loading.value = false
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
function openGoalDialog() {
  resetGoalForm()
  goalDialogOpen.value = true
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
async function saveGoal() {
  if (!goalForm.title.trim()) {
    goalError.value = '请先填写学习目标名称'
    return
  }
  goalSaving.value = true
  goalError.value = ''
  try {
    let payload
    if (goalForm.planText.trim()) {
      const preview = await decomposeStudyText(goalForm.planText.trim())
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
      payload = {
        goal: {
          title: goalForm.title.trim(),
          description: goalForm.description.trim(),
          startDate: goalForm.startDate || null,
          targetDate: goalForm.targetDate || null,
          dailyStudyMinutes: Number(goalForm.dailyStudyMinutes) || 60,
        },
        tasks: [{
          taskName: goalForm.description.trim().slice(0, 120) || (goalForm.title.trim() + '学习任务'),
          estimatedDays: 1,
          plannedStartDate: goalForm.startDate || null,
          priority: '中',
          orderNum: 1,
          isCompleted: false,
          progressPercent: 0,
          description: goalForm.description.trim(),
        }],
      }
    }
    const saved = await saveStudyGoal(payload)
    goalDialogOpen.value = false
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
  if (nextPathItem.value) {
    router.push('/learning/python/plan')
  } else if (remainingGoalTasks.value.length) {
    selectTab('practice')
  } else {
    openGoalDialog()
  }
}
function openRecommendation(item) {
  const topic = item?.title || item?.knowledgePoint || item?.name || ''
  if (item?.courseId) {
    router.push(`/courses/${item.courseId}`)
    return
  }
  router.push({ path: '/career/nebula/python/resources', query: topic ? { topic } : {} })
}
function openProblem(id) {
  router.push(`/career/nebula/python/practice/${id}`)
}

onMounted(loadLearningData)
</script>

<template>
  <div class="learning-page">
    <AppTabBar />
    <main class="learning-shell">
      <header class="learning-hero">
        <div>
          
          <h1>学习实践</h1>
          
        </div>
        <div class="hero-actions">
          <button type="button" class="btn" @click="openGoalDialog">设置学习目标</button>
          <button type="button" class="btn btn--primary" @click="continueLearning">继续上次学习</button>
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
              <div class="head"><div><small>今日重点</small><h2>{{ targetJobTitle || '先选择目标岗位' }}</h2></div><span v-if="targetJobTitle" class="tag">目标岗位</span></div>
              <template v-if="targetJobTitle">
                <p>根据成长中心与岗位探索中的能力差距，安排下一步学习内容。</p>
                <div v-if="weakSkills.length" class="chips"><span v-for="item in weakSkills.slice(0, 5)" :key="item.knowledgePoint || item.name">待补 {{ item.knowledgePoint || item.name }}</span></div>
                <p v-else class="note">暂无待补技能数据，完成一次学习画像后会显示在这里。</p>
                <button type="button" class="btn btn--primary" @click="selectTab('python')">开始推荐练习</button>
              </template>
              <template v-else>
                <p>选择目标岗位后，这里会显示待补技能和推荐学习内容。</p>
                <button type="button" class="btn btn--primary" @click="router.push('/jobs/explore')">选择目标岗位</button>
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
              </template>
            </article>
          </section>

          <section class="panel section">
            <div class="head"><div><h2>当前学习路径</h2></div><button type="button" class="link" @click="router.push('/learning/python/plan')">打开完整路径</button></div>
            <p v-if="dataErrors.python" class="error">{{ dataErrors.python }}</p>
            <div v-if="pathItems.length" class="path-list">
              <article v-for="item in pathItems.slice(0, 6)" :key="item.id" :class="{ done: item.status === 'completed', current: item.status === 'in_progress' }">
                <span>{{ String(item.sequenceNo || 0).padStart(2, '0') }}</span>
                <div><strong>{{ item.knowledgePoint || '未命名知识点' }}</strong><p>{{ item.objective || '暂无学习说明' }}</p></div>
                <em>{{ statusLabel(item.status) }}</em>
                <button v-if="item.status !== 'completed'" type="button" :disabled="busyAction.endsWith(`-${item.id}`)" @click="updatePathItem(item, item.status === 'in_progress' ? 'complete' : 'start')">{{ item.status === 'in_progress' ? '标记完成' : '开始' }}</button>
              </article>
            </div>
            <div v-else class="empty"><strong>暂无学习路径</strong><p>完成学习画像或选择目标岗位后，可以生成学习路径。</p><button type="button" class="btn" @click="router.push('/learning/python/plan')">进入 Python 学习计划</button></div>
          </section>

          <section class="panel section">
            <div class="head"><div><h2>推荐内容</h2></div><button type="button" class="link" @click="router.push('/learning/resources')">更多专项资源</button></div>
            <div v-if="recommendations.length" class="card-grid">
              <button v-for="item in recommendations.slice(0, 6)" :key="item.id || item.title" type="button" class="content-card" @click="openRecommendation(item)"><small>{{ item.type || item.resourceType || '学习建议' }}</small><strong>{{ item.title || item.knowledgePoint || item.name || '学习内容' }}</strong><p>{{ item.reason || item.rationale || item.description || '基于当前学习记录推荐' }}</p><em>开始学习 →</em></button>
            </div>
            <div v-else class="empty"><strong>暂无推荐内容</strong><p>继续完成课程或练习后，系统会根据真实记录更新推荐。</p><button type="button" class="btn" @click="selectTab('python')">浏览 Python 与算法</button></div>
          </section>

          <section class="recent panel">
            <div><small>最近练习</small><strong>已解决 {{ solvedProblemCount }} 道题</strong><span>在线判题完成度 {{ solveRate }}%</span></div>
            <div><small>AI 求职</small><strong>准备进入模拟面试？</strong><span>模拟面试主入口位于 AI 求职页面。</span></div>
            <button type="button" class="btn btn--primary" @click="router.push('/ai-career/interview')">进入 AI 模拟面试</button>
          </section>
        </template>

        <template v-else-if="activeTab === 'python'">
          <section class="panel toolbar"><div><h2>Python 与算法</h2><p>课程、知识图谱、在线题库和 AI 辅助练习统一从这里进入。</p></div><div class="hero-actions"><button type="button" class="btn" @click="router.push('/paper')">AI 出题</button><button type="button" class="btn btn--primary" @click="router.push('/learning/python')">进入题库</button></div></section>
          <section class="stats"><article><span>题库总量</span><strong>{{ problems.length }}</strong><small>道公开题目</small></article><article><span>已解决</span><strong>{{ solvedProblemCount }}</strong><small>{{ solveRate }}% 完成度</small></article><article><span>在线判题</span><strong>{{ judgeableProblems.length }}</strong><small>支持运行与提交</small></article><article><span>已掌握知识</span><strong>{{ mastery.filter((item) => item.status === 'mastered').length }}</strong><small>个知识点</small></article></section>
          <section class="quick-grid"><button type="button" @click="router.push('/learning/python/plan')"><strong>个性化学习路径</strong><span>按知识点和掌握状态安排下一步</span><em>打开路径 →</em></button><button type="button" @click="router.push('/learning/python/knowledge-graph')"><strong>知识图谱</strong><span>查看知识点关系和薄弱环节</span><em>打开图谱 →</em></button><button type="button" @click="router.push('/learning/python')"><strong>在线题库</strong><span>按难度、知识点和完成状态筛选</span><em>开始刷题 →</em></button><button type="button" @click="router.push('/learning/resources')"><strong>专项资源</strong><span>根据薄弱知识点生成学习资源</span><em>生成资源 →</em></button></section>
          <section class="panel section"><div class="head"><div><h2>建议练习</h2></div><span class="note">按难度从低到高排列</span></div><p v-if="dataErrors.problems" class="error">{{ dataErrors.problems }}</p><div v-if="nextProblems.length" class="question-list"><button v-for="item in nextProblems" :key="item.id" type="button" @click="openProblem(item.id)"><span :class="`diff diff--${item.difficulty}`">{{ formatDifficulty(item.difficulty) }}</span><strong>{{ item.title }}</strong><span>{{ (item.tags || []).slice(0, 3).join(' · ') || '未标注知识点' }}</span><em>开始答题 →</em></button></div><div v-else class="empty"><strong>暂无可推荐题目</strong><p>题库为空或已完成当前可练习题目。</p><button type="button" class="btn" @click="router.push('/learning/python')">查看完整题库</button></div></section>
        </template>

        <template v-else-if="activeTab === 'courses'">
          <section class="panel toolbar"><div><h2>课程与专项</h2><p>继续校内课程，并将四六级、证书和专项内容集中管理。</p></div><button type="button" class="btn" @click="router.push('/learning/resources')">进入专项资源</button></section>
          <p v-if="dataErrors.courses" class="error">{{ dataErrors.courses }}</p>
          <section v-if="courses.length" class="course-grid"><button v-for="course in courses.slice(0, 8)" :key="course.id" type="button" class="course-card" @click="router.push(`/courses/${course.id}`)"><span>{{ String(course.name || '课').slice(0, 1) }}</span><div><strong>{{ course.name }}</strong><p>{{ course.bookTitle || course.ownerName || '校内课程' }}</p><div class="progress"><i :style="{ width: `${course.progressPercent || 0}%` }" /></div><small>学习进度 {{ course.progressPercent || 0 }}%</small></div></button></section>
          <div v-else class="empty"><strong>暂无可展示的校园课程</strong><p>管理员发布课程后，会在这里显示真实课程和章节进度。</p></div>
          <section class="panel section"><div class="head"><div><h2>考试与证书专项</h2></div></div><div class="reserved"><article><strong>四六级备考</strong><p>当前后端没有对应专项数据，保留真实接入位置。</p><span>内容建设中</span></article><article><strong>证书考试</strong><p>接入证书课程后，可按目标日期生成学习计划。</p><span>内容建设中</span></article></div></section>
        </template>

        <template v-else-if="activeTab === 'projects'">
          <section class="panel toolbar"><div><h2>项目实训</h2><p>项目任务来自现有学习路径中的实践节点，不使用静态演示任务。</p></div><button type="button" class="btn" @click="router.push('/learning/python/plan')">查看学习路径</button></section>
          <section v-if="projectItems.length" class="project-list"><article v-for="item in projectItems" :key="item.id"><div><span>{{ item.stage || '实践阶段' }}</span><h3>{{ item.knowledgePoint }}</h3><p>{{ item.objective }}</p></div><em>{{ statusLabel(item.status) }}</em><button type="button" class="btn" @click="router.push('/learning/python/plan')">查看任务</button></article></section>
          <div v-else class="empty"><strong>当前学习路径中暂无项目节点</strong><p>完成基础知识点后，项目实践任务会出现在这里。</p><button type="button" class="btn" @click="router.push('/learning/python/plan')">进入个性化学习路径</button></div>
          <section class="panel section"><div class="head"><div><h2>拓展实训</h2></div></div><div class="reserved"><article><strong>企业模拟</strong><p>用于后续接入企业真实任务和角色协作。</p><span>后续开放</span></article><article><strong>商业沙盘</strong><p>用于后续接入经营决策、复盘和模拟评分。</p><span>后续开放</span></article></div></section>
        </template>

        <template v-else>
          <section class="panel toolbar"><div><h2>我的练习</h2><p>查看学习目标、任务进度、未完成题目和已完成内容。</p></div><button type="button" class="btn btn--primary" @click="openGoalDialog">新建学习目标</button></section>
          <section class="practice-layout panel">
            <aside class="goal-list"><div class="head"><h2>学习目标</h2><span>{{ studyGoals.length }} 个</span></div><p v-if="dataErrors.goals" class="error">{{ dataErrors.goals }}</p><button v-for="goal in studyGoals" :key="goal.id" type="button" :class="{ active: selectedGoalId === goal.id }" @click="loadGoalDetail(goal.id)"><span><strong>{{ goal.title }}</strong><small>{{ goal.completedTasks || 0 }}/{{ goal.totalTasks || 0 }} 项任务</small></span><em>{{ goal.progress || 0 }}%</em></button><div v-if="!studyGoals.length" class="empty empty--small"><strong>还没有学习目标</strong><p>创建四六级、证书或技能目标后，任务会显示在这里。</p></div></aside>
            <section class="goal-detail"><template v-if="selectedGoalDetail?.goal"><div class="head"><div><h2>{{ selectedGoalDetail.goal.title }}</h2></div><span class="tag">{{ goalStatusLabel(selectedGoalDetail.goal.status) }}</span></div><p>{{ selectedGoalDetail.goal.description || '暂无目标说明' }}</p><div class="meta"><span>开始 {{ formatDate(selectedGoalDetail.goal.startDate) }}</span><span>目标 {{ formatDate(selectedGoalDetail.goal.targetDate) }}</span><span>每日 {{ selectedGoalDetail.goal.dailyStudyMinutes || 60 }} 分钟</span></div><div class="progress"><i :style="{ width: `${selectedGoalDetail.goal.progress || 0}%` }" /></div><div v-if="activeGoalTasks.length" class="task-list"><article v-for="task in activeGoalTasks" :key="task.id"><button type="button" class="check" :class="{ done: task.isCompleted }" :disabled="busyAction === `task-${task.id}`" @click="toggleTask(task)">{{ task.isCompleted ? '✓' : '' }}</button><div><strong :class="{ done: task.isCompleted }">{{ task.taskName }}</strong><p>{{ task.description || task.stage || '暂无任务说明' }}</p><small>{{ task.progressPercent || 0 }}% · 预计 {{ task.estimatedDays || 1 }} 天</small><div v-if="task.subtasks?.length" class="subtasks"><button v-for="subtask in task.subtasks" :key="subtask.id" type="button" :class="{ done: subtask.isCompleted }" @click="toggleSubtask(subtask)">{{ subtask.isCompleted ? '✓' : '·' }} {{ subtask.taskName }}</button></div></div></article></div><div v-else class="empty empty--small"><strong>这个目标还没有任务</strong><p>新建目标时填写学习计划文本，可以让 AI 拆解为可勾选任务。</p></div></template><div v-else class="empty"><strong>请选择一个学习目标</strong><p>目标详情和可勾选任务会显示在这里。</p></div></section>
          </section>
          <section class="panel section"><div class="head"><div><h2>练习记录</h2></div><button type="button" class="link" @click="router.push('/learning/python')">进入题库</button></div><div class="stats"><article><span>已完成题目</span><strong>{{ solvedProblemCount }}</strong></article><article><span>待完成题目</span><strong>{{ Math.max(0, judgeableProblems.length - solvedProblemCount) }}</strong></article><article><span>学习路径完成</span><strong>{{ pathItems.filter((item) => item.status === 'completed').length }}/{{ pathItems.length }}</strong></article><article><span>已完成任务</span><strong>{{ activeGoalTasks.filter((item) => item.isCompleted).length }}/{{ activeGoalTasks.length }}</strong></article></div></section>
        </template>
      </div>
    </main>

    <Teleport to="body">
      <div v-if="goalDialogOpen" class="dialog-mask" @click.self="closeGoalDialog">
        <form class="dialog" @submit.prevent="saveGoal">
          <header><div><h2>设置学习目标</h2></div><button type="button" aria-label="关闭" @click="closeGoalDialog">×</button></header>
          <label><span>目标名称 *</span><input v-model="goalForm.title" maxlength="120" type="text" placeholder="例如：通过大学英语六级" /></label>
          <label><span>目标说明</span><textarea v-model="goalForm.description" rows="3" placeholder="说明目标、当前基础和希望达到的结果" /></label>
          <label><span>学习计划文本</span><textarea v-model="goalForm.planText" rows="5" placeholder="例如：每天背 30 个单词，周一三五练习听力，周末完成一套真题。填写后会调用 AI 拆解为任务。" /></label>
          <div class="dialog-row"><label><span>开始日期</span><input v-model="goalForm.startDate" type="date" /></label><label><span>目标日期</span><input v-model="goalForm.targetDate" type="date" /></label><label><span>每日分钟</span><input v-model.number="goalForm.dailyStudyMinutes" type="number" min="10" max="600" /></label></div>
          <p v-if="goalError" class="error">{{ goalError }}</p>
          <footer><button type="button" class="btn" :disabled="goalSaving" @click="closeGoalDialog">取消</button><button type="submit" class="btn btn--primary" :disabled="goalSaving">{{ goalSaving ? '正在保存…' : (goalForm.planText.trim() ? 'AI 拆解并保存' : '保存目标') }}</button></footer>
        </form>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.learning-page{min-height:100vh;color:#23262b;background:#f5f6f7}.learning-shell{width:min(1180px,calc(100% - 40px));margin:0 auto;padding:96px 0 80px}.learning-hero,.panel,.practice-layout,.recent{border:1px solid #e3e7eb;background:#fff}.learning-hero{display:flex;align-items:flex-end;justify-content:space-between;gap:24px;padding:28px 30px;border-radius:24px;background:#eef3f6}.learning-hero p,.panel small,.section small,.toolbar small,.goal-detail small{display:block;margin:0;color:#5f7f95;font-size:11px;font-weight:800;letter-spacing:.12em;text-transform:uppercase}.learning-hero h1{margin:10px 0 8px;font-size:clamp(32px,4vw,48px);line-height:1;letter-spacing:-.045em}.learning-hero span,.toolbar p,.panel>p,.note,p{color:#68717b;font-size:14px;line-height:1.7}.hero-actions,.toolbar>div:last-child{display:flex;flex-wrap:wrap;gap:10px}.btn{min-height:40px;padding:0 16px;border:1px solid #ccd4db;border-radius:999px;color:#28333c;background:#fff;font-size:13px;font-weight:700;cursor:pointer}.btn:hover{transform:translateY(-1px);border-color:#9baab6}.btn:disabled{opacity:.55;cursor:wait;transform:none}.btn--primary{border-color:#324958;color:#fff;background:#324958}.btn--primary:hover{background:#243844}.tabs{position:sticky;top:60px;z-index:20;display:flex;gap:4px;margin:16px 0;padding:5px;overflow-x:auto;border:1px solid #e3e7eb;border-radius:999px;background:rgba(255,255,255,.96);box-shadow:0 8px 22px rgba(31,43,51,.05)}.tabs button{flex:0 0 auto;min-height:38px;padding:0 17px;border:0;border-radius:999px;color:#65707a;background:transparent;font-size:13px;font-weight:700;cursor:pointer}.tabs button.active{color:#fff;background:#324958}.content,.section{display:grid;gap:16px}.state,.message{margin:0 0 12px;padding:14px 18px;border:1px solid #dfe6eb;border-radius:14px;color:#5e6b75;background:#fff;font-size:14px}.message{color:#3a6654;background:#eef7f2}.focus-grid{display:grid;grid-template-columns:minmax(0,1.45fr) minmax(300px,.8fr);gap:16px}.panel{border-radius:20px}.focus-panel,.goal-panel,.section,.toolbar,.recent{padding:24px}.head{display:flex;align-items:flex-start;justify-content:space-between;gap:18px}.head h2,.toolbar h2{margin:7px 0 0;color:#202c35;font-size:22px;line-height:1.25;letter-spacing:-.025em}.head>strong{color:#324958;font-size:24px}.tag{display:inline-flex;padding:5px 9px;border-radius:999px;color:#4e6d82;background:#e7f0f6;font-size:11px;font-weight:700;white-space:nowrap}.chips{display:flex;flex-wrap:wrap;gap:7px;margin:14px 0}.chips span{padding:6px 10px;border-radius:999px;color:#7a5948;background:#f5ece5;font-size:12px}.note{font-size:13px}.link{padding:0;border:0;color:#365d78;background:transparent;font-size:13px;font-weight:700;cursor:pointer}.progress{width:100%;height:7px;overflow:hidden;border-radius:999px;background:#e8ecef}.progress i{display:block;height:100%;border-radius:inherit;background:#627e90;transition:width .25s ease}.task-preview{display:grid;gap:8px;margin:14px 0;padding:0;list-style:none}.task-preview button{display:flex;width:100%;align-items:center;gap:9px;padding:9px 0;border:0;color:#34414a;background:transparent;text-align:left;cursor:pointer}.task-preview i{width:17px;height:17px;flex:none;border:1px solid #adb8c0;border-radius:5px;background:#fff}.error{padding:10px 12px;border-radius:10px;color:#8b4c49;background:#f8eeee;font-size:13px}.path-list,.project-list,.task-list{display:grid;gap:8px}.path-list article{display:grid;grid-template-columns:42px minmax(0,1fr) auto auto;align-items:center;gap:12px;padding:13px 14px;border:1px solid #e7ebee;border-radius:13px;background:#fbfcfc}.path-list article.current{border-color:#afc4d1;background:#f3f7f9}.path-list article.done{opacity:.74}.path-list article>span{color:#81909a;font-size:12px;font-weight:800}.path-list strong{display:block;color:#26343e}.path-list p{margin:4px 0 0;font-size:12px}.path-list em,.project-list em{color:#4e6575;font-size:12px;font-style:normal;font-weight:700}.path-list button{min-height:32px;padding:0 11px;border:1px solid #cbd5dc;border-radius:999px;color:#344b5c;background:#fff;font-size:12px;font-weight:700;cursor:pointer}.empty{padding:24px;border:1px dashed #ccd5db;border-radius:14px;color:#66737c;background:#fafbfb}.empty strong{color:#34414a}.empty p{margin:7px 0 14px;font-size:13px}.empty--small{padding:16px}.card-grid,.quick-grid,.course-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:10px}.content-card,.quick-grid button,.course-card{display:grid;gap:8px;min-height:142px;padding:16px;border:1px solid #e2e7ea;border-radius:14px;color:#27343d;background:#fff;text-align:left;cursor:pointer}.content-card:hover,.quick-grid button:hover,.course-card:hover{transform:translateY(-2px);border-color:#b8c5ce}.content-card strong,.quick-grid strong,.course-card strong{font-size:15px}.content-card p,.quick-grid span,.course-card p{margin:0;font-size:12px;line-height:1.6}.content-card em,.quick-grid em,.course-card small{align-self:end;color:#496b80;font-size:12px;font-style:normal;font-weight:700}.recent{display:grid;grid-template-columns:1fr 1fr auto;align-items:center;gap:20px}.recent>div{display:grid;gap:5px}.recent strong{font-size:15px}.recent span{color:#74808a;font-size:12px}.toolbar{display:flex;align-items:flex-start;justify-content:space-between;gap:18px}.toolbar p{margin:10px 0 0}.stats{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:10px}.stats article{display:grid;gap:5px;padding:18px;border:1px solid #e2e7ea;border-radius:14px;background:#fff}.stats span{color:#74808a;font-size:12px}.stats strong{color:#293843;font-size:26px}.stats small{color:#87929a;font-size:11px}.question-list{display:grid;gap:8px}.question-list>button{display:grid;grid-template-columns:58px minmax(0,1fr) minmax(120px,.7fr) auto;align-items:center;gap:12px;padding:13px 14px;border:1px solid #e4e8eb;border-radius:12px;color:#2c3942;background:#fff;text-align:left;cursor:pointer}.question-list>button:hover{border-color:#afc0cb;background:#f8fafb}.question-list strong{min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.question-list em{color:#4f7186;font-size:12px;font-style:normal;font-weight:700}.diff{display:inline-flex;justify-content:center;padding:5px 7px;border-radius:999px;font-size:11px;font-weight:700}.diff--easy{color:#3d6b53;background:#eaf4ee}.diff--medium{color:#7a6441;background:#f7f0df}.diff--hard{color:#845252;background:#f7eaea}.course-card{grid-template-columns:54px minmax(0,1fr)}.course-card>span{display:grid;width:54px;height:68px;place-items:center;border-radius:8px 12px 12px 8px;color:#36566c;background:#e6eef3;font-size:22px;font-weight:800;box-shadow:inset 4px 0 #cad8e1}.course-card>div{display:grid;gap:8px;min-width:0}.course-card .progress{height:5px}.reserved{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:10px}.reserved article{display:grid;gap:8px;padding:18px;border:1px dashed #cbd5dc;border-radius:14px;background:#fafbfb}.reserved p{margin:0;font-size:13px}.reserved span{color:#6c7f8c;font-size:12px;font-weight:700}.project-list article{display:grid;grid-template-columns:minmax(0,1fr) auto auto;align-items:center;gap:16px;padding:18px 20px;border:1px solid #e2e7ea;border-radius:16px;background:#fff}.project-list article>div>span{color:#688293;font-size:11px;font-weight:750;letter-spacing:.08em;text-transform:uppercase}.project-list h3{margin:6px 0;font-size:18px}.project-list p{margin:0;font-size:13px}.practice-layout{display:grid;grid-template-columns:300px minmax(0,1fr);gap:16px;padding:20px}.goal-list{display:grid;align-content:start;gap:8px;padding-right:16px;border-right:1px solid #e4e8eb}.goal-list .head{margin-bottom:8px}.goal-list .head span{color:#7c8790;font-size:12px}.goal-list>button{display:flex;width:100%;align-items:center;justify-content:space-between;gap:10px;padding:12px;border:1px solid #e4e8eb;border-radius:12px;color:#2e3a43;background:#fff;text-align:left;cursor:pointer}.goal-list>button.active{border-color:#95aebd;background:#eef4f7}.goal-list button span{display:grid;gap:4px;min-width:0}.goal-list button strong{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.goal-list button small{color:#7c8790;font-size:11px}.goal-list button em{color:#536c7c;font-size:12px;font-style:normal;font-weight:750}.meta{display:flex;flex-wrap:wrap;gap:8px;margin:14px 0}.meta span{padding:5px 9px;border-radius:999px;color:#687680;background:#f0f3f5;font-size:11px}.task-list{margin-top:16px}.task-list article{display:grid;grid-template-columns:30px minmax(0,1fr);gap:10px;padding:12px 0;border-top:1px solid #edf0f2}.check{display:grid;width:24px;height:24px;place-items:center;border:1px solid #aebac2;border-radius:7px;color:#fff;background:#fff;cursor:pointer}.check.done{border-color:#567b67;background:#567b67}.task-list strong.done,.subtasks button.done{color:#89939a;text-decoration:line-through}.task-list p{margin:4px 0;font-size:12px}.task-list small{color:#89939a;font-size:11px}.subtasks{display:flex;flex-wrap:wrap;gap:6px;margin-top:8px}.subtasks button{padding:5px 8px;border:1px solid #d8dfe4;border-radius:999px;color:#5b6973;background:#fafbfb;font-size:11px;cursor:pointer}.dialog-mask{position:fixed;inset:0;z-index:2000;display:grid;place-items:center;padding:20px;background:rgba(27,35,41,.36)}.dialog{display:grid;gap:15px;width:min(640px,100%);max-height:calc(100vh - 40px);overflow:auto;padding:24px;border:1px solid #dde3e7;border-radius:20px;background:#fff;box-shadow:0 24px 70px rgba(25,34,40,.18)}.dialog header{display:flex;align-items:flex-start;justify-content:space-between;gap:16px}.dialog header h2{margin:7px 0 0}.dialog header button{width:34px;height:34px;border:0;border-radius:50%;color:#4e5c66;background:#f0f3f5;font-size:21px;cursor:pointer}.dialog label{display:grid;gap:7px;color:#53616b;font-size:12px;font-weight:700}.dialog input,.dialog textarea{width:100%;box-sizing:border-box;padding:10px 12px;border:1px solid #d7dfe4;border-radius:10px;outline:none;color:#25323b;background:#fbfcfc;font:inherit;font-weight:500}.dialog input:focus,.dialog textarea:focus{border-color:#91aab9;background:#fff}.dialog textarea{resize:vertical;line-height:1.6}.dialog-row{display:grid;grid-template-columns:1fr 1fr 120px;gap:10px}.dialog footer{display:flex;justify-content:flex-end;gap:10px}@media(max-width:980px){.focus-grid,.practice-layout{grid-template-columns:1fr}.goal-list{padding-right:0;padding-bottom:16px;border-right:0;border-bottom:1px solid #e4e8eb}.card-grid,.quick-grid,.course-grid{grid-template-columns:repeat(2,minmax(0,1fr))}.recent{grid-template-columns:1fr 1fr}.recent>button{grid-column:1/-1;justify-self:start}}@media(max-width:680px){.learning-shell{width:min(100% - 24px,1180px);padding-top:82px}.learning-hero,.toolbar,.recent{display:grid}.tabs{top:52px}.card-grid,.quick-grid,.course-grid,.stats,.reserved{grid-template-columns:1fr}.path-list article{grid-template-columns:34px minmax(0,1fr) auto}.path-list article button{grid-column:2/-1;justify-self:start}.question-list>button{grid-template-columns:52px minmax(0,1fr)}.question-list>button>span:last-of-type,.question-list em{grid-column:2}.practice-layout{padding:16px}.dialog-row{grid-template-columns:1fr}}
/* 与首页统一视觉体系 */
.learning-page {
  color: var(--hp-ink);
  background: var(--hp-bg);
}

.learning-shell {
  width: min(1280px, calc(100% - 40px));
}

.learning-hero,
.panel,
.practice-layout,
.recent {
  border-color: var(--hp-line);
  border-radius: var(--hp-r-lg);
  background: var(--hp-surface);
  box-shadow: var(--hp-shadow-sm);
}

.learning-hero {
  padding: 48px 44px;
  border-color: #e4ebf2;
  background: var(--hp-tint);
}

.learning-hero h1 {
  margin: 0 0 18px;
  color: var(--hp-ink);
  font-size: clamp(38px, 5vw, 52px);
  line-height: 1.18;
  letter-spacing: -.03em;
}

.learning-hero h1::after {
  display: block;
  width: 72px;
  height: 4px;
  margin-top: 22px;
  border-radius: 999px;
  background: #9dc0dd;
  content: '';
}

.btn {
  min-height: 46px;
  padding: 0 26px;
  border-color: var(--hp-line-strong);
  color: var(--hp-ink);
  background: var(--hp-surface);
  font-size: 15px;
  font-weight: 600;
}

.btn--primary {
  border-color: var(--hp-ink);
  color: #ffffff;
  background: var(--hp-ink);
  box-shadow: var(--hp-shadow-sm);
}

.btn--primary:hover {
  background: var(--hp-ink);
  box-shadow: var(--hp-shadow-md);
}

.tabs {
  border-color: var(--hp-line);
  background: var(--hp-surface);
  box-shadow: var(--hp-shadow-sm);
}

.tabs button {
  color: var(--hp-ink-2);
  font-size: 14px;
  font-weight: 600;
}

.tabs button.active {
  color: #ffffff;
  background: var(--hp-ink);
}

.panel small,
.section small,
.toolbar small,
.goal-detail small {
  color: var(--hp-blue-ink);
  letter-spacing: .02em;
  text-transform: none;
}

.head h2,
.toolbar h2,
.learning-hero h2,
.goal-detail h2 {
  color: var(--hp-ink);
}

.learning-hero span,
.toolbar p,
.panel>p,
.note,
p {
  color: var(--hp-ink-2);
}

.state,
.message,
.error,
.empty,
.reserved article,
.content-card,
.quick-grid button,
.course-card,
.stats article,
.path-list article,
.project-list article,
.goal-list>button {
  border-color: var(--hp-line);
  background: var(--hp-surface);
}

.state,
.message {
  border-radius: var(--hp-r-sm);
  box-shadow: var(--hp-shadow-sm);
}

.tag,
.chips span {
  color: var(--hp-blue-ink);
  background: var(--hp-blue);
}

.progress i {
  background: var(--hp-blue-ink);
}

.link,
.content-card em,
.quick-grid em,
.stats strong,
.goal-list button em {
  color: var(--hp-blue-ink);
}

.dialog {
  border-color: var(--hp-line);
  border-radius: var(--hp-r-lg);
  box-shadow: var(--hp-shadow-lg);
}

.dialog input,
.dialog textarea {
  border-color: var(--hp-line-strong);
  color: var(--hp-ink);
  background: var(--hp-surface-2);
}

.dialog input:focus,
.dialog textarea:focus {
  border-color: var(--hp-blue-ink);
  background: var(--hp-surface);
}
</style>
