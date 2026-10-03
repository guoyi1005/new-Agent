<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppTabBar from '../components/AppTabBar.vue'
import HomeSection from '../components/home/HomeSection.vue'
import SkillProgress from '../components/home/SkillProgress.vue'
import campusHeroArt from '../assets/campus-hero.svg'
import { getHotMarketJobs, getInternshipRecommendations } from '../api/jobRecommendations'
import { getCampusCourses } from '../api/campusCourse'
import { getPythonHome, completePathItem } from '../api/learning'
import { getUserInfo } from '../utils/auth'

const router = useRouter()
const categoryDetails = {
  it_ai: {
    title: '互联网与人工智能',
    groups: [
      { name: '开发与技术', tags: ['前端开发工程师', '后端开发工程师', '全栈工程师', '测试工程师', '运维工程师'] },
      { name: 'AI与算法', tags: ['算法工程师', '机器学习工程师', '大模型工程师', 'AI应用工程师', '提示词工程师'] },
      { name: '产品与设计', tags: ['产品经理', '数据分析师', 'UI设计师', 'UX设计师'] },
    ],
  },
  chip: {
    title: '电子与通信技术',
    groups: [
      { name: '研发与设计', tags: ['电子工程师', '通信工程师', '嵌入式工程师', 'PCB设计工程师'] },
      { name: '测试与运维', tags: ['射频工程师', '设备测试工程师', '网络优化工程师', '技术支持'] },
    ],
  },
  finance: {
    title: '金融与保险',
    groups: [
      { name: '金融业务', tags: ['投资经理', '风控专员', '财富顾问', '保险顾问'] },
      { name: '财务支持', tags: ['审计专员', '财务分析师', '税务专员', '会计'] },
    ],
  },
  education: {
    title: '教育与培训',
    groups: [
      { name: '教学岗位', tags: ['学科教师', '课程顾问', '教研老师', '升学顾问'] },
      { name: '支持岗位', tags: ['班主任', '教学运营', '心理咨询师'] },
    ],
  },
  health: {
    title: '医疗与健康',
    groups: [
      { name: '临床方向', tags: ['临床医生', '护士', '康复治疗师', '医技人员'] },
      { name: '健康服务', tags: ['健康管理师', '营养师', '心理咨询师'] },
    ],
  },
  biotech: {
    title: '生物制药与化工',
    groups: [
      { name: '研发岗位', tags: ['生物研发工程师', '制药工程师', '化学分析师'] },
      { name: '质量岗位', tags: ['质量专员', '检验工程师', '注册申报专员'] },
    ],
  },
  manufacturing: {
    title: '制造业与工业生产',
    groups: [
      { name: '生产方向', tags: ['机械工程师', '工艺工程师', '设备工程师', '生产主管'] },
      { name: '供应链方向', tags: ['计划专员', '采购专员', '质量工程师'] },
    ],
  },
  automobile: {
    title: '汽车与交通装备',
    groups: [
      { name: '研发方向', tags: ['整车工程师', '智驾工程师', '测试工程师'] },
      { name: '服务方向', tags: ['售后工程师', '服务顾问', '供应链专员'] },
    ],
  },
  construction: {
    title: '建筑工程与地产',
    groups: [
      { name: '工程方向', tags: ['建筑设计师', '施工员', '造价工程师', '项目经理'] },
      { name: '地产方向', tags: ['招商主管', '物业经理', '策划专员'] },
    ],
  },
  energy: {
    title: '能源矿业与环保',
    groups: [
      { name: '能源方向', tags: ['电气工程师', '新能源工程师', '储能工程师'] },
      { name: '环保方向', tags: ['环保工程师', 'EHS专员', '安全工程师'] },
    ],
  },
  retail: {
    title: '电商与零售',
    groups: [
      { name: '电商方向', tags: ['电商运营', '选品专员', '直播运营', '投流专员'] },
      { name: '零售方向', tags: ['门店店长', '陈列专员', '招商主管'] },
    ],
  },
  marketing: {
    title: '市场广告与公关',
    groups: [
      { name: '品牌方向', tags: ['品牌经理', '媒介经理', '活动策划', '广告优化师'] },
      { name: '内容方向', tags: ['内容运营', '文案策划', '公关专员'] },
    ],
  },
  media: {
    title: '文化传媒与内容',
    groups: [
      { name: '内容方向', tags: ['编辑', '编导', '摄像师', '新媒体运营'] },
      { name: '创作方向', tags: ['短视频策划', '主播', '后期剪辑'] },
    ],
  },
  design: {
    title: '艺术与设计',
    groups: [
      { name: '视觉方向', tags: ['平面设计师', '三维设计师', '插画师'] },
      { name: '空间方向', tags: ['室内设计师', '展陈设计师', '交互设计师'] },
    ],
  },
  legal: {
    title: '法律咨询与知识产权',
    groups: [
      { name: '法律方向', tags: ['律师', '法务', '合规专员', '知识产权顾问'] },
      { name: '咨询方向', tags: ['咨询顾问', '项目顾问'] },
    ],
  },
  admin: {
    title: '企业管理与行政',
    groups: [
      { name: '行政方向', tags: ['行政专员', '前台', '秘书', '总助'] },
      { name: '人力方向', tags: ['招聘专员', 'HRBP', '培训专员'] },
    ],
  },
  sales: {
    title: '销售与客户服务',
    groups: [
      { name: '销售方向', tags: ['大客户经理', '渠道经理', '招商主管', '商务经理'] },
      { name: '服务方向', tags: ['客服专员', '售后专员', '呼叫中心专员'] },
    ],
  },
  logistics: {
    title: '物流仓储与供应链',
    groups: [
      { name: '仓配方向', tags: ['仓储主管', '配送专员', '物流专员'] },
      { name: '供应链方向', tags: ['采购专员', '计划专员', '报关专员'] },
    ],
  },
  hospitality: {
    title: '餐饮酒店与旅游',
    groups: [
      { name: '餐饮方向', tags: ['厨师', '餐厅经理', '店长'] },
      { name: '文旅方向', tags: ['酒店管家', '导游', '会展执行'] },
    ],
  },
  public: {
    title: '公共服务与政府',
    groups: [
      { name: '服务方向', tags: ['社区工作者', '外事专员', '政务服务专员'] },
      { name: '应急方向', tags: ['消防员', '应急专员'] },
    ],
  },
  sports: {
    title: '体育与健身',
    groups: [
      { name: '训练方向', tags: ['健身教练', '体育教练', '康复师'] },
      { name: '赛事方向', tags: ['赛事运营', '裁判', '场馆管理员'] },
    ],
  },
  service: {
    title: '家政与生活服务',
    groups: [
      { name: '家庭服务', tags: ['家政服务员', '月嫂', '育婴师'] },
      { name: '生活服务', tags: ['维修师傅', '美甲师', '宠物美容师'] },
    ],
  },
  security: {
    title: '安保与应急服务',
    groups: [
      { name: '安保方向', tags: ['保安', '安检员', '安全管理员'] },
      { name: '风险方向', tags: ['风险评估师', '安全工程师'] },
    ],
  },
  freelance: {
    title: '自由职业与新兴职业',
    groups: [
      { name: '创作方向', tags: ['自由撰稿人', '独立设计师', '自媒体博主', 'AI内容创作者'] },
      { name: '服务方向', tags: ['线上顾问', '配音员', '翻译'] },
    ],
  },
}


const userInfo = computed(() => getUserInfo() || {})
const displayName = computed(() => userInfo.value.realName || userInfo.value.username || '同学')

const greeting = computed(() => {
  const hour = new Date().getHours()
  if (hour < 6) return '凌晨好'
  if (hour < 11) return '早上好'
  if (hour < 14) return '中午好'
  if (hour < 18) return '下午好'
  return '晚上好'
})

const todayLabel = computed(() => {
  const now = new Date()
  const weekday = ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][now.getDay()]
  return `${now.getMonth() + 1} 月 ${now.getDate()} 日 · ${weekday}`
})


const TARGET_JOB_STORAGE_KEY = 'home_target_job'
const targetJobTitle = ref(localStorage.getItem(TARGET_JOB_STORAGE_KEY) || '')
const isJobPickerOpen = ref(false)
const jobKeyword = ref('')

// 岗位库直接复用页面里已有的行业方向数据，不额外造数据
const jobOptions = computed(() => {
  const seen = new Map()
  for (const detail of Object.values(categoryDetails)) {
    for (const group of detail.groups || []) {
      for (const title of group.tags || []) {
        if (!seen.has(title)) {
          seen.set(title, { title, category: detail.title, group: group.name })
        }
      }
    }
  }
  return [...seen.values()]
})

const filteredJobOptions = computed(() => {
  const keyword = jobKeyword.value.trim().toLowerCase()
  const list = keyword
    ? jobOptions.value.filter(
        (job) =>
          job.title.toLowerCase().includes(keyword) ||
          job.category.includes(keyword) ||
          job.group.includes(keyword),
      )
    : jobOptions.value
  return list.slice(0, 80)
})


/* 目标岗位详情入口：复用已有的岗位探索页 */
function openTargetJobDetail() {
  router.push('/career/gap')
}

function openJobPicker() {
  jobKeyword.value = ''
  isJobPickerOpen.value = true
}

function closeJobPicker() {
  isJobPickerOpen.value = false
  jobKeyword.value = ''
}

function selectTargetJob(title) {
  targetJobTitle.value = title
  try {
    localStorage.setItem(TARGET_JOB_STORAGE_KEY, title)
  } catch {
    /* 本地存储不可用时仅本次会话生效 */
  }
  closeJobPicker()
}


const learningHome = ref(null)
const campusCourses = ref([])
const hotJobs = ref([])
const internshipJobs = ref([])
const activeRecommendationTab = ref('internships')
const learningLoading = ref(true)
const coursesLoading = ref(true)
const hotJobsLoading = ref(true)
const learningError = ref('')
const coursesError = ref('')
const hotJobsError = ref('')
const taskError = ref('')
const taskBusy = ref(null)
const serverTasks = ref([])
const showAllTasks = ref(false)

const mastery = computed(() => learningHome.value?.mastery || [])
const pathItems = computed(() => learningHome.value?.activePath?.items || [])
const targetJob = computed(() => {
  const option = jobOptions.value.find(job => job.title === targetJobTitle.value)
  // 学习掌握度不是岗位匹配度；没有岗位评估结果时不显示示例百分比。
  const pythonTarget = /python/i.test(targetJobTitle.value)
  return {
    title: targetJobTitle.value || '尚未设置目标岗位',
    category: option?.category || '',
    matchRate: null,
    skillProgress: pythonTarget
      ? mastery.value.filter(item => item.knowledgePointName).slice(0, 4).map(item => ({ name: item.knowledgePointName, value: item.score }))
      : [],
    note: pythonTarget && mastery.value.some(item => item.status === 'weak')
      ? '学习记录中仍有需巩固的知识点，完整能力差距请前往岗位探索查看。'
      : targetJobTitle.value ? '岗位匹配度待评估，完成岗位体检后查看能力差距。' : '尚未选择目标岗位，请调整目标设置当前方向。',
  }
})

// 个人任务沿用原新增/勾选交互，仅保存用户实际输入；按账号与日期隔离。
const now = new Date()
const taskStorageKey = `home_personal_tasks:${userInfo.value.id || userInfo.value.userId || userInfo.value.username || 'guest'}:${now.getFullYear()}-${now.getMonth() + 1}-${now.getDate()}`
const todayPlan = ref(readPersonalTasks())
const isAddingPlan = ref(false)
const newPlanTitle = ref('')
const planInputRef = ref(null)

function readPersonalTasks() {
  try {
    const saved = JSON.parse(localStorage.getItem(taskStorageKey) || '[]')
    return Array.isArray(saved)
      ? saved.filter(task => task && typeof task.id === 'string' && typeof task.title === 'string' && ['todo', 'done'].includes(task.state))
      : []
  } catch {
    return []
  }
}

function savePersonalTasks() {
  try {
    localStorage.setItem(taskStorageKey, JSON.stringify(todayPlan.value))
  } catch {
    taskError.value = '本地存储不可用，个人任务仅在本次会话保留。'
  }
}

const allTasks = computed(() => [...serverTasks.value, ...todayPlan.value])
const visibleTasks = computed(() => showAllTasks.value ? allTasks.value : allTasks.value.slice(0, 5))
const todayPlanDone = computed(() => allTasks.value.filter(task => task.state === 'done').length)
const todayPlanProgress = computed(() => allTasks.value.length ? Math.round(todayPlanDone.value / allTasks.value.length * 100) : 0)

async function togglePlanTask(task) {
  if (!task.pathItemId) {
    task.state = task.state === 'done' ? 'todo' : 'done'
    savePersonalTasks()
    return
  }
  // 复用学习页的完成接口；服务端已完成任务不支持撤销，避免伪造本地回退。
  if (task.state === 'done' || taskBusy.value !== null) return
  taskBusy.value = task.id
  taskError.value = ''
  try {
    const updated = await completePathItem(task.pathItemId)
    task.state = updated.status === 'completed' ? 'done' : 'doing'
    task.meta = updated.objective || task.meta
    if (learningHome.value.activePath) {
      learningHome.value.activePath.items = pathItems.value.map(item => item.id === updated.id ? updated : item)
    }
  } catch (cause) {
    taskError.value = cause.message || '任务更新失败，请重试。'
  } finally {
    taskBusy.value = null
  }
}

async function startAddPlan() {
  isAddingPlan.value = true
  await nextTick()
  planInputRef.value?.focus()
}

function submitPlanTask() {
  const title = newPlanTitle.value.trim()
  if (title) {
    todayPlan.value.push({ id: `plan-${Date.now()}`, title, meta: '个人任务', state: 'todo' })
    savePersonalTasks()
    if (allTasks.value.length > 5) showAllTasks.value = true
  }
  newPlanTitle.value = ''
  isAddingPlan.value = false
}

function cancelPlanTask() {
  newPlanTitle.value = ''
  isAddingPlan.value = false
}


const myCourses = computed(() => campusCourses.value
  .filter(course => course.progressPercent > 0 && course.progressPercent < 100)
  .slice(0, 3)
  .map(course => ({
    ...course,
    progress: course.progressPercent,
    tone: /fastapi/i.test(course.name) ? 'pink' : /mysql/i.test(course.name) ? 'blue' : /python/i.test(course.name) ? 'green' : 'blue',
  })))

const growthStats = computed(() => [
  { label: '已掌握知识点', value: learningHome.value ? mastery.value.filter(item => item.status === 'mastered').length : null },
  { label: '已完成课程', value: coursesError.value || coursesLoading.value ? null : campusCourses.value.filter(course => course.progressPercent === 100).length },
  { label: '已完成学习任务', value: learningHome.value ? pathItems.value.filter(item => item.status === 'completed').length : null },
])
const growthRecords = computed(() => pathItems.value
  .filter(item => item.status === 'completed' && item.completedAt)
  .slice()
  .sort((a, b) => new Date(b.completedAt) - new Date(a.completedAt))
  .slice(0, 3))

function formatRecordDate(value) {
  return new Date(value).toLocaleDateString('zh-CN', { month: 'long', day: 'numeric' })
}

const recommendJobs = computed(() => internshipJobs.value.slice(0, 4))

async function loadLearning() {
  learningLoading.value = true
  learningError.value = ''
  try {
    learningHome.value = await getPythonHome() || {}
    const tasks = learningHome.value.todayTasks || []
    const today = new Date().toDateString()
    const completedToday = pathItems.value.filter(item => item.status === 'completed' && item.completedAt && new Date(item.completedAt).toDateString() === today)
    serverTasks.value = [...tasks, ...completedToday.filter(item => !tasks.some(task => task.id === item.id))]
      .map(item => ({
        id: `learning-${item.id}`,
        pathItemId: item.id,
        title: item.knowledgePoint,
        meta: item.objective || '学习计划',
        state: item.status === 'completed' ? 'done' : item.status === 'in_progress' ? 'doing' : 'todo',
      }))
  } catch (cause) {
    learningError.value = cause.message || '学习记录暂时无法加载。'
  } finally {
    learningLoading.value = false
  }
}

async function loadCourses() {
  coursesLoading.value = true
  coursesError.value = ''
  try {
    const response = await getCampusCourses()
    campusCourses.value = Array.isArray(response.data) ? response.data : []
  } catch (cause) {
    coursesError.value = cause.message || '课程暂时无法加载。'
  } finally {
    coursesLoading.value = false
  }
}

async function loadHotJobs() {
  hotJobsLoading.value = true
  hotJobsError.value = ''
  try {
    const [internshipResult, hotResult] = await Promise.allSettled([
      getInternshipRecommendations(4),
      getHotMarketJobs(6),
    ])
    if (internshipResult.status === 'fulfilled') {
      internshipJobs.value = Array.isArray(internshipResult.value.data?.items) ? internshipResult.value.data.items : []
    }
    if (hotResult.status === 'fulfilled') {
      hotJobs.value = Array.isArray(hotResult.value.data?.items) ? hotResult.value.data.items : []
    }
    if (internshipResult.status === 'rejected' && hotResult.status === 'rejected') throw internshipResult.reason
  } catch (cause) {
    hotJobsError.value = cause.message || '岗位推荐暂时无法加载。'
  } finally {
    hotJobsLoading.value = false
  }
}

onMounted(() => {
  loadLearning()
  loadCourses()
  loadHotJobs()
})
</script>

<template>
  <div class="home-view">
    <AppTabBar embedded />
    <main class="hp-main">
      <section class="hp-hero" aria-label="成长工作台欢迎区">
        <div class="hp-hero__body">
          <p class="hp-eyebrow"><span class="hp-eyebrow__mark" aria-hidden="true" />{{ todayLabel }} · 学生个人成长工作台</p>
          <h1 class="hp-hero__title">{{ greeting }}，<span>{{ displayName }}</span></h1>
          <p class="hp-hero__desc">今天也在向更好的自己靠近。<br />从今日任务开始，让每一步学习都更有方向。</p>
          <div class="hp-hero__actions">
            <button class="hp-btn hp-btn--solid" type="button" @click="router.push('/ai-career/resume')">开始岗位体检</button>
            <RouterLink class="hp-link" to="/learning/python/plan">查看成长路径 →</RouterLink>
          </div>
        </div>
        <div class="hp-hero__visual">
          <img class="hp-hero__art" :src="campusHeroArt" alt="校园中学习与探索职业的学生" />
          <span class="hp-hero__caption" aria-hidden="true">学习 · 探索 · 成长</span>
        </div>
      </section>

      <div class="hp-dashboard">
        <HomeSection id="today-tasks" title="今日任务" class="hp-card hp-dashboard__tile hp-plan">
          <template #aside>
            <span class="hp-plan__count">{{ todayPlanDone }} / {{ allTasks.length }}</span>
            <button class="hp-plan__add" type="button" aria-label="新增任务" @click="startAddPlan">+</button>
          </template>
          <div class="hp-plan__overview">
            <div class="hp-plan__progress" role="progressbar" aria-label="今日任务完成进度" :aria-valuenow="todayPlanProgress" aria-valuemin="0" aria-valuemax="100" :style="{ '--progress': todayPlanProgress + '%' }">
              <strong>{{ todayPlanProgress }}%</strong>
            </div>
            <div class="hp-plan__overview-copy">
              <strong>今日完成进度</strong>
              <span>{{ allTasks.length ? `已完成 ${todayPlanDone} 项，继续向目标前进` : '添加第一项任务，开启今天的计划' }}</span>
            </div>
          </div>
          <p v-if="learningLoading" class="hp-empty" role="status">正在加载学习任务…</p>
          <p v-if="learningError" class="hp-empty hp-error" role="status">{{ learningError }} <button class="hp-link" @click="loadLearning">重试</button></p>
          <ul class="hp-plan__list">
            <li v-for="task in visibleTasks" :key="task.id">
              <button class="hp-plan__item" :class="{ 'is-done': task.state === 'done', 'is-doing': task.state === 'doing' }" type="button"
                role="checkbox" :aria-checked="task.state === 'done'"
                :disabled="!!task.pathItemId && (task.state === 'done' || taskBusy !== null)" @click="togglePlanTask(task)">
                <span class="hp-plan__mark" aria-hidden="true" />
                <span class="hp-plan__copy"><span class="hp-plan__title">{{ task.title }}</span><span class="hp-plan__meta">{{ task.meta }}</span></span>
                <span class="hp-plan__status">{{ taskBusy === task.id ? '保存中' : task.state === 'done' ? '已完成' : task.state === 'doing' ? '进行中' : '待开始' }}</span>
              </button>
            </li>
          </ul>
          <p v-if="!learningLoading && !allTasks.length" class="hp-empty">今天还没有任务，点击右上角 + 添加你的计划。</p>
          <form v-if="isAddingPlan" class="hp-plan__edit" @submit.prevent="submitPlanTask">
            <input ref="planInputRef" v-model="newPlanTitle" aria-label="任务名称" placeholder="今天想完成什么？" @keydown.esc="cancelPlanTask" />
            <button class="hp-link" type="submit">添加</button>
            <button class="hp-link" type="button" @click="cancelPlanTask">取消</button>
          </form>
          <p v-if="taskError" class="hp-empty hp-error" role="alert">{{ taskError }}</p>
          <button class="hp-link hp-tile__footer" type="button" @click="showAllTasks = !showAllTasks">{{ showAllTasks ? '收起任务' : '查看全部任务 →' }}</button>
        </HomeSection>

        <HomeSection title="目标岗位" class="hp-card hp-dashboard__tile hp-target">
          <template #aside><button class="hp-link" type="button" @click="openJobPicker">调整目标</button></template>
          <div class="hp-target__top">
            <div class="hp-target__info">
              <p class="hp-target__meta">我的目标岗位 · {{ targetJob.category }}</p>
              <h3 class="hp-target__job">{{ targetJob.title }}</h3>
              <p class="hp-target__note">{{ targetJob.note }}</p>
            </div>
            <div class="hp-match hp-match--quiet"><span class="hp-match__value">待评估</span><span class="hp-match__label">岗位匹配度</span></div>
          </div>
          <div v-if="targetJob.skillProgress.length" class="hp-target__tags">
            <p class="hp-target__meta">当前知识点掌握度 · 来自学习记录</p>
            <div class="hp-target__skills">
              <SkillProgress v-for="skill in targetJob.skillProgress" :key="skill.name" :label="skill.name" :value="skill.value" tone="pink" />
            </div>
          </div>
          <p v-else class="hp-empty">{{ learningLoading ? '正在加载能力记录…' : '暂无该目标岗位的能力进度，先完成学习与岗位体检。' }}</p>
          <div class="hp-target__actions"><button class="hp-link" type="button" @click="openTargetJobDetail">查看岗位详情 →</button></div>
        </HomeSection>

        <HomeSection title="继续学习" class="hp-card hp-dashboard__tile hp-course-tile">
          <template #aside><RouterLink class="hp-link" to="/learning">全部课程 →</RouterLink></template>
          <p v-if="coursesLoading" class="hp-empty" role="status">正在加载课程…</p>
          <p v-else-if="coursesError" class="hp-empty hp-error" role="status">{{ coursesError }} <button class="hp-link" @click="loadCourses">重试</button></p>
          <div v-else-if="!myCourses.length" class="hp-course-empty">
            <svg viewBox="0 0 64 64" aria-hidden="true"><path d="M9 16c10-3 19-2 23 2 4-4 13-5 23-2v34c-10-3-19-2-23 2-4-4-13-5-23-2V16Zm23 2v34M16 26c4-.5 8-.2 11 1M37 27c3-1.2 7-1.5 11-1M16 34c4-.5 8-.2 11 1M37 35c3-1.2 7-1.5 11-1" /></svg>
            <p>暂无正在学习的课程。<br />下一段成长，从一门感兴趣的课程开始。</p>
            <RouterLink class="hp-link" to="/learning">去学习实践看看 →</RouterLink>
          </div>
          <ul v-else class="hp-courses__list">
            <li v-for="course in myCourses" :key="course.id">
              <RouterLink class="hp-course" :to="'/courses/' + course.id">
                <span class="hp-course__cover" :class="'is-' + course.tone">
                  <img v-if="course.displayImageUrl || course.coverUrl" :src="course.displayImageUrl || course.coverUrl" alt="" />
                  <svg v-else viewBox="0 0 64 48" aria-hidden="true"><path d="M16 12h26v25H16zM22 20h14M22 26h10" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" /></svg>
                </span>
                <span class="hp-course__body">
                  <span class="hp-course__title">{{ course.name }}</span>
                  <span class="hp-course__type">{{ course.chapterCount }} 章节<span v-if="course.currentChapterTitle"> · {{ course.currentChapterTitle }}</span></span>
                  <span class="hp-course__progress"><span class="hp-bar__track"><i :style="{ width: course.progress + '%' }" /></span><span class="hp-course__value">{{ course.progress }}%</span></span>
                </span>
              </RouterLink>
            </li>
          </ul>
        </HomeSection>

        <HomeSection title="成长动态" class="hp-card hp-dashboard__tile hp-growth">
          <template #aside><RouterLink class="hp-link" to="/growth">查看成长档案 →</RouterLink></template>
          <div class="hp-growth__stats">
            <div v-for="stat in growthStats" :key="stat.label"><strong>{{ stat.value ?? '—' }}</strong><span>{{ stat.label }}</span></div>
          </div>
          <p class="hp-section__meta">当前累计记录，不代表今日新增</p>
          <p v-if="learningLoading" class="hp-empty" role="status">正在加载成长记录…</p>
          <p v-else-if="learningError" class="hp-empty hp-error">{{ learningError }}</p>
          <ol v-else-if="growthRecords.length" class="hp-growth__timeline">
            <li v-for="record in growthRecords" :key="record.id"><time :datetime="record.completedAt">{{ formatRecordDate(record.completedAt) }}</time><span>完成学习任务：{{ record.knowledgePoint }}</span></li>
          </ol>
          <p v-else class="hp-empty">暂无带完成时间的成长记录。完成学习任务后，会在这里留下足迹。</p>
        </HomeSection>

        <HomeSection title="实习推荐" class="hp-card hp-dashboard__tile hp-internships">
          <template #aside>
            <div class="hp-recommendation-tabs" role="tablist" aria-label="岗位推荐类型">
              <button type="button" role="tab" :aria-selected="activeRecommendationTab === 'internships'" :class="{ 'is-active': activeRecommendationTab === 'internships' }" @click="activeRecommendationTab = 'internships'">为你推荐</button>
              <button type="button" role="tab" :aria-selected="activeRecommendationTab === 'hot'" :class="{ 'is-active': activeRecommendationTab === 'hot' }" @click="activeRecommendationTab = 'hot'">热门岗位</button>
            </div>
            <RouterLink class="hp-link" :to="activeRecommendationTab === 'hot' ? '/employment#market-trends' : '/employment#recommended-jobs'">{{ activeRecommendationTab === 'hot' ? '查看岗位趋势 →' : '查看更多 →' }}</RouterLink>
          </template>
          <p class="hp-section__meta">岗位、薪资与技能均来自已采集并核验来源的有效职位；无可用数据时不会以 AI 内容补位。</p>
          <p v-if="hotJobsLoading" class="hp-empty" role="status">正在加载岗位推荐…</p>
          <p v-else-if="hotJobsError" class="hp-empty hp-error" role="status">岗位数据暂时不可用，正在保留已采集数据。<button class="hp-link" @click="loadHotJobs">重试</button></p>
          <template v-else-if="activeRecommendationTab === 'internships'">
            <p v-if="!recommendJobs.length" class="hp-empty">岗位数据正在准备中；接入获授权的数据源并完成采集后，将在这里显示真实实习机会。</p>
            <ul v-else class="hp-jobs__grid">
              <li v-for="job in recommendJobs" :key="job.jobId" class="hp-job">
                <div class="hp-job__main">
                  <div class="hp-job__head"><span class="hp-job__logo" aria-hidden="true">{{ job.title?.slice(0, 1) }}</span><div class="hp-job__copy"><h3 class="hp-job__title">{{ job.title }}</h3><p class="hp-job__meta">{{ [job.company, job.city, '实习'].filter(Boolean).join(' · ') }}</p></div></div>
                  <div class="hp-job__reward">
                    <p class="hp-job__salary">{{ job.salaryText }}</p>
                    <p v-if="job.matchScore !== null && job.matchScore !== undefined" class="hp-job__match">{{ job.matchScore }}% 匹配</p>
                    <p v-else class="hp-job__gaps">完善目标与能力档案后显示个性化匹配度</p>
                  </div>
                </div>
                <div class="hp-job__details">
                  <div v-if="job.matchedSkills?.length" class="hp-tags"><span v-for="skill in job.matchedSkills" :key="skill" class="hp-tag">{{ skill }}</span></div>
                  <p v-if="job.missingSkills?.length" class="hp-job__gaps">待提升：{{ job.missingSkills.join(' · ') }}</p>
                </div>
                <div class="hp-job__footer">
                  <p class="hp-job__gaps">来源：{{ job.source }}<span v-if="job.updatedAt"> · 更新于 {{ new Date(job.updatedAt).toLocaleDateString() }}</span></p>
                  <a class="hp-link hp-job__link" :href="job.sourceUrl" target="_blank" rel="noopener noreferrer">查看原职位 →</a>
                </div>
              </li>
            </ul>
          </template>
          <template v-else>
            <p v-if="!hotJobs.length" class="hp-empty">暂无足够的有效岗位数据，岗位热度将在采集数据后更新。</p>
            <ol v-else class="hp-hotjobs-list">
              <li v-for="(job, index) in hotJobs" :key="job.title">
                <div class="hp-hotjobs-list__top"><span class="hp-hotjobs-list__rank">{{ String(index + 1).padStart(2, '0') }}</span><strong>{{ job.title }}</strong></div>
                <div class="hp-hotjobs-list__meta"><span>热度 {{ job.hotScore }}</span><span>有效岗位 {{ job.activeJobCount }} 个</span><span v-if="job.salaryP50">薪资中位数 {{ Math.round(job.salaryP50 / 1000) }}K</span><span v-else>薪资样本不足</span></div>
              </li>
            </ol>
          </template>
        </HomeSection>
      </div>
      <!-- 选择目标岗位 -->
      <Teleport to="body">
        <div v-if="isJobPickerOpen" class="hp-modal" @click.self="closeJobPicker">
          <div class="hp-modal__card" role="dialog" aria-label="选择目标岗位">
            <header class="hp-modal__head">
              <h3>选择目标岗位</h3>
              <button class="hp-modal__close" type="button" aria-label="关闭" @click="closeJobPicker">
                <svg viewBox="0 0 24 24" aria-hidden="true">
                  <path d="M6 6l12 12M18 6L6 18" />
                </svg>
              </button>
            </header>
            <div class="hp-modal__body">
              <input
                v-model="jobKeyword"
                class="hp-modal__search"
                type="search"
                placeholder="搜索岗位，例如：前端、算法、产品"
              />
              <ul class="hp-modal__list">
                <li v-for="job in filteredJobOptions" :key="job.title">
                  <button
                    class="hp-modal__item"
                    :class="{ 'is-on': job.title === targetJob.title }"
                    type="button"
                    @click="selectTargetJob(job.title)"
                  >
                    <span class="hp-modal__job">{{ job.title }}</span>
                    <span class="hp-modal__cat">{{ job.category }}</span>
                  </button>
                </li>
                <li v-if="!filteredJobOptions.length" class="hp-modal__empty">
                  没有找到匹配的岗位，换个关键词试试
                </li>
              </ul>
            </div>
          </div>
        </div>
      </Teleport>
      <footer class="hp-footer">
        <p>© 2026 数智诊断港 | 本平台数据仅用于学术研究与个人职业发展规划</p>
        <p>ICP备案号：粤 ICP 备 XXXXXXX 号</p>
      </footer>
    </main>
  </div>
</template>

<style scoped>
/* ============================================================
 * 首页视觉：现代校园插画 + 内容型职业成长平台
 * 1) 暖白底色、白色卡片、极轻阴影与弱边框，靠留白与尺寸层级拉开层次；
 * 2) 圆角 16～24px，卡片内边距 20～28px，模块间距 24～36px；
 * 3) 辅助色只用低饱和粉 / 浅蓝 / 浅绿 / 浅黄，每个区块一种强调色；
 * 4) 不使用玻璃拟态、霓虹色、复杂渐变；插画为独立图片素材。
 * ============================================================ */

.home-view {
  position: relative;
  min-height: 100vh;
  overflow-x: clip;
  background: var(--hp-bg);
  color: var(--hp-ink);
  font-family: inherit;
}

.home-view *,
.home-view *::before,
.home-view *::after {
  box-sizing: border-box;
}

.hp-main {
  position: relative;
  width: min(1440px, calc(100% - 48px));
  margin: 0 auto;
  padding: 28px 0 56px;
}

/* ---------- Hero ---------- */

.hp-hero {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 44%);
  grid-template-areas:
    'body visual'
    'route route';
  align-items: center;
  gap: 24px 28px;
  padding: 48px 44px 0;
  border: 1px solid #e4ebf2;
  border-radius: var(--hp-r-lg);
  background: var(--hp-tint);
  box-shadow: var(--hp-shadow-sm);
  overflow: hidden;
}

.hp-hero__body {
  grid-area: body;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.hp-eyebrow {
  margin: 0 0 16px;
  color: var(--hp-blue-ink);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.01em;
}

.hp-hero__title {
  margin: 0;
  color: var(--hp-ink);
  font-size: 52px;
  font-weight: 700;
  letter-spacing: -0.03em;
  line-height: 1.18;
}

.hp-hero__desc {
  margin: 18px 0 0;
  max-width: 30em;
  color: var(--hp-ink-2);
  font-size: 16px;
  line-height: 1.8;
}

.hp-hero__actions {
  display: flex;
  align-items: center;
  gap: 22px;
  margin-top: 28px;
}

.hp-hero__visual {
  grid-area: visual;
  display: flex;
  align-items: flex-end;
  justify-content: flex-end;
  min-width: 0;
}

.hp-hero__art {
  display: block;
  width: 100%;
  max-width: 680px;
  height: auto;
  /* 向右出血，被卡片圆角裁掉一点，形成海报式构图 */
  margin: 0 -70px -12px 0;
}

/* ---------- 按钮与文字链接 ---------- */

.hp-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 46px;
  padding: 0 26px;
  border: 1px solid var(--hp-line-strong);
  border-radius: 999px;
  background: var(--hp-surface);
  color: var(--hp-ink);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease, border-color 0.18s ease, transform 0.18s ease;
}

.hp-btn--solid {
  border-color: var(--hp-ink);
  background: var(--hp-ink);
  color: #ffffff;
  box-shadow: var(--hp-shadow-sm);
}

.hp-btn--solid:hover {
  transform: translateY(-1px);
  box-shadow: var(--hp-shadow-md);
}

.hp-btn--ghost:hover {
  border-color: var(--hp-ink);
  background: var(--hp-surface-2);
}

.hp-link {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--hp-ink-2);
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  white-space: nowrap;
  cursor: pointer;
  transition: color 0.18s ease;
}

.hp-link:hover {
  color: var(--hp-ink);
  text-decoration: underline;
}

.hp-section__meta {
  margin: 0;
  color: var(--hp-muted);
  font-size: 12px;
}

.hp-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hp-tag {
  padding: 5px 12px;
  border-radius: 999px;
  background: var(--hp-surface-2);
  border: 1px solid var(--hp-line);
  color: var(--hp-ink-2);
  font-size: 12.5px;
  line-height: 1.5;
  white-space: nowrap;
}

/* ---------- 目标岗位 + 今日计划 ---------- */

.hp-card {
  display: flex;
  flex-direction: column;
  gap: 20px;
  flex: 1;
  padding: 26px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-md);
  background: var(--hp-surface);
  box-shadow: var(--hp-shadow-sm);
}

.hp-target {
  gap: 24px;
}

.hp-target__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
}

.hp-target__info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.hp-target__job {
  margin: 0;
  color: var(--hp-ink);
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.hp-target__note {
  margin: 0;
  color: var(--hp-muted);
  font-size: 13px;
  line-height: 1.75;
}

.hp-target__meta {
  margin: 0;
  color: var(--hp-muted);
  font-size: 13px;
}

.hp-match {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  flex: 0 0 auto;
  min-width: 96px;
  padding: 12px 16px;
  border-radius: var(--hp-r-sm);
  background: var(--hp-pink);
}

.hp-match__value {
  color: var(--hp-pink-ink);
  font-size: 24px;
  font-weight: 700;
  line-height: 1.1;
}

.hp-match__label {
  color: var(--hp-pink-ink);
  font-size: 12px;
  opacity: 0.85;
}

.hp-match--quiet {
  background: var(--hp-surface-2);
  border: 1px solid var(--hp-line);
}

.hp-match--quiet .hp-match__value,
.hp-match--quiet .hp-match__label {
  color: var(--hp-muted);
}

.hp-target__skills {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px 28px;
}

.hp-target__tags {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.hp-target__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: auto;
  padding-top: 4px;
}

/* 今日计划（浅绿强调） */

.hp-plan__count {
  color: var(--hp-muted);
  font-size: 12px;
}

.hp-plan__add {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border: 1px solid var(--hp-line-strong);
  border-radius: 50%;
  background: var(--hp-surface);
  color: var(--hp-ink-2);
  font-size: 16px;
  line-height: 1;
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease, border-color 0.18s ease;
}

.hp-plan__add:hover {
  border-color: var(--hp-ink);
  background: var(--hp-ink);
  color: #ffffff;
}

.hp-plan__list {
  display: flex;
  flex: 1;
  flex-direction: column;
  justify-content: space-between;
  margin: 0;
  padding: 0;
  list-style: none;
}

.hp-plan__item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 13px 0;
  border: 0;
  border-bottom: 1px solid var(--hp-line);
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
}

.hp-plan__list > li:last-child .hp-plan__item {
  padding-bottom: 0;
  border-bottom: 0;
}

.hp-plan__mark {
  display: grid;
  place-items: center;
  width: 20px;
  height: 20px;
  flex: 0 0 auto;
  border: 1.5px solid var(--hp-line-strong);
  border-radius: 50%;
  transition: border-color 0.18s ease, background 0.18s ease;
}

.hp-plan__item.is-done .hp-plan__mark {
  border-color: #b9d0ae;
  background: var(--hp-green);
}

.hp-plan__item.is-done .hp-plan__mark::after {
  content: '';
  width: 5px;
  height: 9px;
  border-right: 1.8px solid var(--hp-green-ink);
  border-bottom: 1.8px solid var(--hp-green-ink);
  transform: translateY(-1px) rotate(45deg);
}

.hp-plan__item.is-doing .hp-plan__mark {
  border: 4px solid var(--hp-green);
}

.hp-plan__title {
  color: var(--hp-ink);
  font-size: 14px;
  font-weight: 600;
}

.hp-plan__meta {
  margin-left: auto;
  color: var(--hp-muted);
  font-size: 12px;
  white-space: nowrap;
}

.hp-plan__item.is-done .hp-plan__title {
  color: var(--hp-muted);
}

.hp-plan__item:hover .hp-plan__title {
  color: var(--hp-blue-ink);
}

.hp-plan__edit {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 13px 0 0;
}

.hp-plan__edit input {
  flex: 1;
  min-width: 0;
  height: 36px;
  padding: 0 14px;
  border: 1px solid var(--hp-line-strong);
  border-radius: 999px;
  background: var(--hp-surface);
  color: var(--hp-ink);
  font-size: 13px;
  outline: none;
}

.hp-plan__edit input:focus {
  border-color: var(--hp-ink);
}

/* ---------- 继续学习 ---------- */

/* 课程在阶段拼贴里改成纵向列表，封面缩成缩略图 */
.hp-course-tile {
  padding: 18px 24px;
}

.hp-courses__list {
  display: flex;
  flex: 1;
  flex-direction: column;
  justify-content: space-between;
  margin: 0;
  padding: 0;
  list-style: none;
}

.hp-courses__list > li + li {
  border-top: 1px solid var(--hp-line);
}

.hp-course {
  display: grid;
  grid-template-columns: 104px minmax(0, 1fr);
  gap: 16px;
  align-items: center;
  width: 100%;
  padding: 13px 0;
  border: 0;
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.hp-course__cover {
  display: block;
  width: 104px;
  aspect-ratio: 16 / 10;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-sm);
  overflow: hidden;
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.hp-course__body {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.hp-course:hover .hp-course__cover {
  transform: translateY(-2px);
  box-shadow: var(--hp-shadow-md);
}

.hp-course__cover.is-pink {
  background: #f8ecee;
}

.hp-course__cover.is-blue {
  background: #eef4f9;
}

.hp-course__cover.is-green {
  background: #f0f5ec;
}

.hp-course__cover svg {
  display: block;
  width: 100%;
  height: 100%;
}

.hp-course__title {
  color: var(--hp-ink);
  font-size: 15px;
  font-weight: 600;
}

.hp-course__type {
  color: var(--hp-muted);
  font-size: 12.5px;
}

.hp-course__progress {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 4px;
}

.hp-bar__track {
  display: block;
  flex: 1;
  height: 6px;
  border-radius: 999px;
  background: var(--hp-track);
  overflow: hidden;
}

.hp-bar__track > i {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: var(--hp-blue-ink);
}

.hp-course__value {
  color: var(--hp-muted);
  font-size: 12px;
}

/* ---------- 推荐岗位 ---------- */

.hp-jobs__state {
  margin: 0;
  color: var(--hp-muted);
  font-size: 13px;
}

.hp-jobs__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--hp-gap);
  margin: 0;
  padding: 0;
  list-style: none;
}

.hp-recommendation-tabs {
  display: inline-flex;
  gap: 4px;
  margin-right: 8px;
  padding: 3px;
  border-radius: 999px;
  background: var(--hp-tint);
}

.hp-recommendation-tabs button {
  padding: 6px 10px;
  border: 0;
  border-radius: 999px;
  color: var(--hp-muted);
  background: transparent;
  cursor: pointer;
  font: inherit;
  font-size: 12px;
}

.hp-recommendation-tabs button.is-active {
  color: var(--hp-ink);
  background: var(--hp-surface);
  box-shadow: var(--hp-shadow-sm);
}

.hp-job {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 24px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-md);
  background: var(--hp-surface);
  box-shadow: var(--hp-shadow-sm);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.hp-job:hover {
  transform: translateY(-2px);
  box-shadow: var(--hp-shadow-md);
}

.hp-job__head {
  display: flex;
  align-items: center;
  gap: 14px;
}

.hp-job__logo {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  flex: 0 0 auto;
  border-radius: 14px;
  background: var(--hp-pink);
  color: var(--hp-pink-ink);
  font-size: 17px;
  font-weight: 700;
}

.hp-job__copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.hp-job__title {
  margin: 0;
  color: var(--hp-ink);
  font-size: 16px;
  font-weight: 600;
}

.hp-job__meta {
  margin: 0;
  color: var(--hp-muted);
  font-size: 12px;
  line-height: 1.6;
}

.hp-job__salary { margin: 0; color: var(--hp-ink); font-size: 14px; font-weight: 600; }
.hp-job__match { margin: 0; color: var(--hp-pink-ink); font-size: 13px; font-weight: 700; }
.hp-job__gaps { margin: 0; color: var(--hp-muted); font-size: 12px; line-height: 1.6; }

.hp-hotjobs-list { display: grid; gap: 8px; margin: 0; padding: 0; list-style: none; }
.hp-hotjobs-list li { display: grid; grid-template-columns: 36px minmax(160px, 1.2fr) repeat(3, minmax(110px, 1fr)); gap: 12px; align-items: center; padding: 12px 14px; border: 1px solid var(--hp-line); border-radius: var(--hp-r-sm); background: var(--hp-surface); font-size: 13px; }
.hp-hotjobs-list li > span { color: var(--hp-muted); }
.hp-hotjobs-list__rank { color: var(--hp-pink-ink) !important; font-weight: 700; }

.hp-job__link {
  align-self: flex-start;
  margin-top: auto;
}

/* ---------- 目标岗位选择弹窗 ---------- */

.hp-modal {
  position: fixed;
  inset: 0;
  z-index: 2200;
  display: grid;
  place-items: center;
  padding: 24px;
  background: rgba(35, 38, 43, 0.36);
  color: var(--hp-ink);
  font-family: inherit;
}

.hp-modal__card {
  display: flex;
  flex-direction: column;
  width: min(560px, 100%);
  max-height: min(640px, calc(100vh - 48px));
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-lg);
  background: var(--hp-surface);
  box-shadow: var(--hp-shadow-lg);
  overflow: hidden;
}

.hp-modal__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 24px;
  border-bottom: 1px solid var(--hp-line);
}

.hp-modal__head h3 {
  margin: 0;
  color: var(--hp-ink);
  font-size: 17px;
  font-weight: 600;
}

.hp-modal__close {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--hp-line-strong);
  border-radius: 50%;
  background: transparent;
  color: var(--hp-muted);
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease, border-color 0.18s ease;
}

.hp-modal__close:hover {
  border-color: var(--hp-ink);
  background: var(--hp-surface-2);
  color: var(--hp-ink);
}

.hp-modal__close svg {
  width: 15px;
  height: 15px;
  fill: none;
  stroke: currentColor;
  stroke-width: 2;
  stroke-linecap: round;
}

.hp-modal__body {
  display: flex;
  flex-direction: column;
  gap: 14px;
  min-height: 0;
  padding: 20px 24px 24px;
}

.hp-modal__search {
  width: 100%;
  height: 42px;
  padding: 0 18px;
  border: 1px solid var(--hp-line-strong);
  border-radius: 999px;
  background: var(--hp-surface);
  color: var(--hp-ink);
  font-size: 14px;
  outline: none;
  transition: border-color 0.18s ease;
}

.hp-modal__search:focus {
  border-color: var(--hp-ink);
}

.hp-modal__search::placeholder {
  color: #a7adb6;
}

.hp-modal__list {
  display: flex;
  flex-direction: column;
  gap: 4px;
  max-height: 400px;
  margin: 0;
  padding: 0;
  list-style: none;
  overflow-y: auto;
}

.hp-modal__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  width: 100%;
  padding: 12px 14px;
  border: 1px solid transparent;
  border-radius: var(--hp-r-sm);
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: background 0.16s ease, border-color 0.16s ease;
}

.hp-modal__item:hover {
  background: var(--hp-surface-2);
}

.hp-modal__item.is-on {
  border-color: var(--hp-line-strong);
  background: var(--hp-surface-2);
}

.hp-modal__job {
  color: var(--hp-ink);
  font-size: 14px;
  font-weight: 600;
}

.hp-modal__cat {
  color: var(--hp-muted);
  font-size: 12px;
  white-space: nowrap;
}

.hp-modal__empty {
  padding: 22px 0;
  color: var(--hp-muted);
  font-size: 13px;
  text-align: center;
}

/* ---------- 页脚 ---------- */

.hp-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 40px;
  padding-top: 24px;
  border-top: 1px solid var(--hp-line);
  color: var(--hp-muted);
  font-size: 12px;
}

.hp-footer p {
  margin: 0;
}


/* 首页专属编排：沿用全站配色变量，增加暖色层次与更明确的内容节奏。 */
.home-view {
  background: radial-gradient(circle at 50% 2%, #fff9ee 0, transparent 42%), var(--hp-bg);
}

.hp-main { padding-top: 30px; }
.hp-hero {
  grid-template-areas: 'body visual';
  grid-template-columns: minmax(0, 1fr) minmax(0, 43%);
  min-height: 358px;
  padding: 40px 48px;
  border-color: #eadbc7;
  background: linear-gradient(112deg, #fff8e8 0%, #fbeedc 50%, #f7e4db 100%);
  box-shadow: 0 16px 38px rgba(114, 77, 43, .08);
}
.hp-hero::before {
  content: '';
  position: absolute;
  width: 470px;
  height: 470px;
  right: -64px;
  top: -68px;
  border: 1px solid rgba(178, 123, 78, .13);
  border-radius: 50%;
  box-shadow: 0 0 0 55px rgba(255, 255, 255, .13), 0 0 0 110px rgba(255, 255, 255, .08);
  pointer-events: none;
}
.hp-hero__body, .hp-hero__visual { position: relative; z-index: 1; }
.hp-eyebrow {
  display: inline-flex;
  align-items: center;
  align-self: flex-start;
  gap: 9px;
  margin-bottom: 20px;
  padding: 8px 13px;
  border: 1px solid #eadbc2;
  border-radius: 999px;
  color: #8b6544;
  background: rgba(255, 255, 255, .58);
  font-size: 12px;
  letter-spacing: .04em;
}
.hp-eyebrow__mark { width: 7px; height: 7px; border-radius: 50%; background: #d78959; }
.hp-hero__title { font-size: clamp(34px, 3.7vw, 54px); font-weight: 800; line-height: 1.24; overflow-wrap: anywhere; }
.hp-hero__title span {
  color: #b66f50;
  background: linear-gradient(transparent 76%, rgba(244, 200, 117, .62) 76%);
}
.hp-hero__desc { margin-top: 20px; color: #66594e; font-size: 17px; line-height: 1.85; }
.hp-hero__actions { margin-top: 30px; }
.hp-hero__visual { align-self: stretch; align-items: center; }
.hp-hero__art { max-height: 330px; object-fit: contain; margin: 0 -28px -20px 0; filter: drop-shadow(0 16px 14px rgba(84, 75, 62, .08)); }
.hp-hero__caption {
  position: absolute;
  right: 2px;
  bottom: 8px;
  padding: 9px 15px;
  border: 1px solid #e8d5bf;
  border-radius: 999px;
  color: #8d674b;
  background: rgba(255, 251, 242, .92);
  box-shadow: var(--hp-shadow-sm);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: .12em;
}
.hp-btn--solid { border-color: #bb7752; color: #fff; background: #c77d55; box-shadow: 0 7px 14px rgba(153, 89, 52, .15); }
.hp-btn--solid:hover { border-color: #ad6946; background: #ad6946; }
.hp-hero .hp-link { color: #865d44; }

.hp-dashboard {
  display: grid;
  grid-template-columns: minmax(0, 1.2fr) minmax(0, 1fr);
  align-items: stretch;
  gap: 24px;
  margin-top: 28px;
}
.hp-dashboard__tile {
  position: relative;
  min-width: 0;
  margin-top: 0;
  gap: 24px;
  padding: 30px;
  overflow: hidden;
  border-color: #e8dfd3;
  border-radius: 28px;
  box-shadow: 0 8px 30px rgba(86, 68, 48, .055);
  transition: transform .2s ease, box-shadow .2s ease;
}
.hp-dashboard__tile:hover { transform: translateY(-2px); box-shadow: 0 16px 36px rgba(86, 68, 48, .085); }
.hp-dashboard__tile :deep(.home-section__head) { align-items: center; margin-bottom: 0; padding-bottom: 18px; border-bottom: 1px solid rgba(87, 67, 45, .11); }
.hp-dashboard__tile :deep(.home-section__title) { align-items: center; gap: 13px; font-size: 24px; font-weight: 800; letter-spacing: -.025em; }
.hp-dashboard__tile :deep(.home-section__title)::before {
  content: var(--tile-number);
  display: inline-grid;
  place-items: center;
  width: 36px;
  height: 36px;
  border: 1px solid var(--tile-accent);
  border-radius: 50%;
  color: var(--tile-ink);
  background: rgba(255,255,255,.5);
  font-family: Inter, sans-serif;
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0;
}
.hp-dashboard__tile :deep(.home-section__aside) { flex-wrap: wrap; }
.hp-plan { --tile-number: '01'; --tile-accent: #e4bd70; --tile-ink: #936c27; background: #fffaf0; }
.hp-target { --tile-number: '02'; --tile-accent: #d7a0a2; --tile-ink: #a15e62; background: #f9e8e7; }
.hp-course-tile { --tile-number: '03'; --tile-accent: #a9c5d5; --tile-ink: #527b92; background: #f8faf9; }
.hp-growth { --tile-number: '04'; --tile-accent: #a7c69a; --tile-ink: #5c8056; background: #edf4e9; }
.hp-internships { --tile-number: '05'; --tile-accent: #d8b997; --tile-ink: #926d4c; grid-column: 1 / -1; background: #fffaf3; }
.hp-internships > .hp-section__meta { max-width: 72em; line-height: 1.65; }
.hp-link { color: #8b674d; }
.hp-link:hover { color: #ad6946; }
.hp-empty { margin: 0; color: var(--hp-ink-2); font-size: 13px; line-height: 1.8; }
.hp-error { overflow-wrap: anywhere; }

.hp-plan__count { padding: 6px 10px; border-radius: 999px; color: #8f6d35; background: #f6e9c9; font-weight: 700; }
.hp-plan__add { width: 32px; height: 32px; color: #815a3d; background: #fff7df; }
.hp-plan__overview { display: flex; align-items: center; gap: 20px; min-height: 138px; padding: 18px 22px; border: 1px solid #eee0c2; border-radius: 20px; background: rgba(255,255,255,.66); }
.hp-plan__progress { display: grid; place-items: center; flex: 0 0 96px; width: 96px; height: 96px; border-radius: 50%; background: conic-gradient(#c99b55 var(--progress), #f0e8d8 0); }
.hp-plan__progress::before { content: ''; grid-area: 1 / 1; width: 75px; height: 75px; border-radius: 50%; background: #fffaf0; }
.hp-plan__progress strong { z-index: 1; grid-area: 1 / 1; color: #8f6535; font-size: 25px; font-weight: 800; }
.hp-plan__overview-copy { display: flex; flex-direction: column; gap: 9px; min-width: 0; }
.hp-plan__overview-copy strong { color: var(--hp-ink); font-size: 18px; }
.hp-plan__overview-copy span { color: #867a6d; font-size: 13px; line-height: 1.55; }
.hp-bar__track > i { transition: width .25s ease; }
.hp-plan__list { justify-content: flex-start; flex: 0; }
.hp-plan__item { padding: 15px 8px; border-radius: 10px; transition: background .18s ease; }
.hp-plan__item:hover { background: rgba(255,255,255,.72); }
.hp-plan__copy { display: flex; flex: 1; min-width: 0; flex-direction: column; gap: 5px; }
.hp-plan__title, .hp-plan__meta { overflow-wrap: anywhere; }
.hp-plan__meta { margin-left: 0; white-space: normal; }
.hp-plan__status { flex-shrink: 0; font-size: 12px; color: var(--hp-muted); }
.hp-plan__item.is-doing .hp-plan__mark { border: 4px solid var(--hp-yellow); background: var(--hp-yellow-ink); }
.hp-plan__item.is-doing .hp-plan__status { color: var(--hp-yellow-ink); }
.hp-plan__item.is-done .hp-plan__status { color: var(--hp-green-ink); }
.hp-plan__item:disabled { cursor: default; }
.hp-tile__footer { align-self: flex-start; margin-top: auto; padding: 8px 0; }

.hp-target::after { content: ''; position: absolute; right: -70px; bottom: 70px; width: 220px; height: 220px; border: 1px solid rgba(161,94,98,.15); border-radius: 50%; box-shadow: 0 0 0 38px rgba(255,255,255,.09), 0 0 0 76px rgba(255,255,255,.07); pointer-events: none; }
.hp-target > * { position: relative; z-index: 1; }
.hp-target__top { align-items: center; min-height: 142px; }
.hp-target__job { max-width: 15em; font-size: clamp(27px, 2.4vw, 35px); line-height: 1.3; }
.hp-target__meta { color: #9b6970; font-weight: 600; }
.hp-target__note { color: var(--hp-ink-2); }
.hp-target__tags { padding: 20px; border: 1px solid rgba(194, 145, 142, .18); border-radius: 18px; background: rgba(255,255,255,.55); }
.hp-target__actions { padding-top: 16px; border-top: 1px solid rgba(161,94,98,.18); }
.hp-match--quiet { min-width: 112px; min-height: 112px; border: 1px solid #e9cbc7; border-radius: 50%; background: #fff7f4; }
.hp-match--quiet .hp-match__value { color: #a66d61; font-size: 20px; }
.hp-match--quiet .hp-match__label { color: #a66d61; }

.hp-courses__list { justify-content: flex-start; }
.hp-courses__list > li + li { border-top: 0; margin-top: 12px; }
.hp-course { grid-template-columns: 68px minmax(0, 1fr); padding: 14px; border: 1px solid var(--hp-line); border-radius: 14px; color: inherit; background: var(--hp-surface-2); text-decoration: none; transition: transform .18s ease, box-shadow .18s ease; }
.hp-course:hover { transform: translateY(-2px); box-shadow: var(--hp-shadow-sm); }
.hp-course:has(.is-pink) { background: #f8e8e9; }
.hp-course:has(.is-blue) { background: #e9f1f6; }
.hp-course:has(.is-green) { background: #eaf1e6; }
.hp-course__cover { width: 68px; aspect-ratio: 1; color: var(--hp-ink-2); }
.hp-course__cover img { width: 100%; height: 100%; object-fit: cover; }
.hp-course__type { overflow-wrap: anywhere; }
.hp-course-empty { display: flex; flex: 1; flex-direction: column; align-items: flex-start; justify-content: center; min-height: 236px; padding: 26px; border: 1px dashed #c9d8df; border-radius: 20px; background: #f0f6f7; }
.hp-course-empty svg { width: 58px; height: 58px; margin-bottom: 14px; fill: none; stroke: #7b9faa; stroke-width: 2.2; stroke-linecap: round; stroke-linejoin: round; }
.hp-course-empty p { margin: 0 0 16px; color: #5e7480; font-size: 14px; line-height: 1.7; }
.hp-course-empty .hp-link { color: #507f91; }

.hp-growth__stats { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 0; padding: 18px 0; border-top: 1px solid rgba(82,114,73,.14); border-bottom: 1px solid rgba(82,114,73,.14); }
.hp-growth__stats > div { display: flex; flex-direction: column; gap: 7px; min-width: 0; padding: 4px 18px; }
.hp-growth__stats > div + div { border-left: 1px solid rgba(82,114,73,.18); }
.hp-growth__stats > div:first-child { padding-left: 0; }
.hp-growth__stats strong { color: #65875a; font-size: 38px; font-weight: 800; line-height: 1.15; }
.hp-growth__stats span { font-size: 12px; color: var(--hp-ink-2); }
.hp-growth__timeline { padding: 0; margin: 0; list-style: none; }
.hp-growth__timeline li { display: flex; flex-direction: column; gap: 5px; padding: 12px 0 12px 16px; border-left: 2px solid #bad1ad; font-size: 13px; line-height: 1.6; }
.hp-growth__timeline time { color: #65875a; font-size: 12px; font-weight: 700; }

.hp-jobs__grid { grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 16px; }
.hp-job { min-width: 0; gap: 16px; padding: 22px; border-color: #e8ddcf; border-radius: 20px; background: #fff; box-shadow: none; }
.hp-job:first-child { grid-column: 1 / -1; gap: 22px; padding: 30px 34px; border-color: #e5d2bc; background: linear-gradient(112deg, #fff8ed, #f7eadb); }
.hp-job__main { display: flex; flex-direction: column; align-items: flex-start; gap: 16px; }
.hp-job:first-child .hp-job__main { flex-direction: row; align-items: center; justify-content: space-between; gap: 24px; }
.hp-job__head { align-items: flex-start; }
.hp-job__logo { width: 48px; height: 48px; background: #f3dfcb; color: #936945; }
.hp-job:first-child .hp-job__logo { width: 64px; height: 64px; border-radius: 18px; background: #ebd2b3; font-size: 25px; }
.hp-job__title { font-weight: 750; line-height: 1.35; }
.hp-job:first-child .hp-job__title { font-size: 25px; }
.hp-job__meta { margin-top: 4px; line-height: 1.5; }
.hp-job__reward { display: flex; flex-wrap: wrap; align-items: center; gap: 10px; }
.hp-job:first-child .hp-job__reward { justify-content: flex-end; }
.hp-job__salary { color: #a7603e; font-size: 18px; font-weight: 800; }
.hp-job:first-child .hp-job__salary { font-size: 25px; }
.hp-job__match { align-self: flex-start; padding: 5px 10px; border-radius: 999px; color: #9b644b; background: #f6e9dd; }
.hp-job__details { display: flex; flex: 1; flex-direction: column; gap: 9px; }
.hp-job__footer { display: flex; flex-wrap: wrap; align-items: center; justify-content: space-between; gap: 8px 16px; margin-top: auto; padding-top: 14px; border-top: 1px solid #eee6dc; }
.hp-job__footer .hp-job__gaps { flex: 1; min-width: 0; }
.hp-job__link { flex-shrink: 0; }
.hp-job:first-child .hp-job__footer { border-color: #e7d8c5; }
.hp-tag { white-space: normal; overflow-wrap: anywhere; }
.hp-recommendation-tabs { background: #f5ecdf; }
.hp-recommendation-tabs button.is-active { background: #fff; }
.hp-hotjobs-list { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 14px; }
.hp-hotjobs-list li { display: flex; flex-direction: column; align-items: flex-start; gap: 16px; padding: 22px; border-color: #ecdfd0; border-radius: 18px; }
.hp-hotjobs-list__top { display: flex; align-items: center; gap: 12px; width: 100%; }
.hp-hotjobs-list__top strong { font-size: 16px; line-height: 1.45; }
.hp-hotjobs-list__rank { display: grid; place-items: center; flex: 0 0 36px; height: 36px; border-radius: 50%; background: #f4e8d9; }
.hp-hotjobs-list__meta { display: flex; flex-wrap: wrap; gap: 6px 12px; padding-left: 48px; color: var(--hp-muted); font-size: 12px; }

@media (max-width: 1200px) {
  .hp-dashboard { gap: 20px; }
  .hp-card { padding: 22px; }
  .hp-hero { padding: 34px; }
  .hp-jobs__grid, .hp-hotjobs-list { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
@media (max-width: 960px) {
  .hp-dashboard { grid-template-columns: minmax(0, 1fr); }
  .hp-hero { grid-template-columns: minmax(0, 1.3fr) minmax(0, 1fr); gap: 16px; }
  .hp-hero__actions { flex-wrap: wrap; gap: 16px; }
}
@media (max-width: 680px) {
  .hp-main { width: calc(100% - 32px); padding: 20px 0 44px; }
  .hp-hero { grid-template-columns: minmax(0, 1fr); grid-template-areas: 'body' 'visual'; gap: 8px; padding: 26px 23px 12px; }
  .hp-hero::before { width: 290px; height: 290px; right: -90px; top: auto; bottom: -90px; }
  .hp-hero__title { font-size: clamp(30px, 8vw, 40px); }
  .hp-hero__desc { font-size: 14px; }
  .hp-hero__visual { justify-content: center; }
  .hp-hero__art { max-height: 190px; margin: 0; }
  .hp-hero__caption { right: 0; bottom: 0; font-size: 10px; }
  .hp-card { padding: 20px; }
  .hp-dashboard__tile :deep(.home-section__head) { flex-wrap: wrap; gap: 10px; }
  .hp-dashboard__tile :deep(.home-section__title) { font-size: 19px; }
  .hp-target__top { flex-direction: column; gap: 16px; }
  .hp-target__skills, .hp-jobs__grid { grid-template-columns: minmax(0, 1fr); }
  .hp-growth__stats > div { padding: 4px 9px; }
  .hp-growth__stats strong { font-size: 27px; }
  .hp-jobs__grid, .hp-hotjobs-list { grid-template-columns: minmax(0, 1fr); }
  .hp-job:first-child { grid-column: auto; padding: 22px; }
  .hp-job:first-child .hp-job__main { flex-direction: column; align-items: flex-start; }
  .hp-job:first-child .hp-job__reward { justify-content: flex-start; }
  .hp-job:first-child .hp-job__title { font-size: 20px; }
  .hp-job:first-child .hp-job__salary { font-size: 19px; }
  .hp-plan__overview { padding: 14px; gap: 14px; }
  .hp-plan__progress { flex-basis: 80px; width: 80px; height: 80px; }
  .hp-plan__progress::before { width: 63px; height: 63px; }
  .hp-plan__progress strong { font-size: 21px; }
  .hp-target__job { font-size: 25px; overflow-wrap: anywhere; }
  .hp-course { grid-template-columns: minmax(0, 1fr); }
  .hp-course__cover { display: none; }
  .hp-plan__edit { flex-wrap: wrap; }
  .hp-plan__edit input { flex-basis: 100%; }
  .hp-modal { padding: 16px; }
  .hp-modal__head, .hp-modal__body { padding: 16px; }
  .hp-modal__item { flex-wrap: wrap; gap: 6px; }
  .hp-modal__cat { white-space: normal; }
  .hp-footer { flex-direction: column; align-items: flex-start; }
}
@media (prefers-reduced-motion: reduce) {
  .hp-dashboard__tile, .hp-course, .hp-job, .hp-btn, .hp-bar__track > i { transition: none; }
}
</style>
