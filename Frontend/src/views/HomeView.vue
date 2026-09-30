<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import AppTabBar from '../components/AppTabBar.vue'
import HomeSection from '../components/home/HomeSection.vue'
import SkillProgress from '../components/home/SkillProgress.vue'
import campusHeroArt from '../assets/campus-hero.svg'
import { getLatestJobRecommendations, JOB_BOSS_CTA, JOB_SALARY_HINT, resolveBossJobSearchLink, resolveBossJobSearchLinkFromJob } from '../api/jobRecommendations'
import { getUserInfo } from '../utils/auth'

const router = useRouter()
const searchKeyword = ref('')
const hotJobsLoading = ref(true)
const hotJobs = ref([])

const hotSearches = ['AI算法', 'Java开发', '前端架构', '云原生', '产品经理', '数据分析']

const categoryPages = [
  [
    {
      id: 'it_ai',
      main: '互联网与人工智能',
      sub: '前端 后端 AI算法 产品 运营',
    },
    {
      id: 'chip',
      main: '电子与通信技术',
      sub: '硬件 芯片 通信 网络',
    },
    {
      id: 'finance',
      main: '金融与保险',
      sub: '银行 投资 风控 审计',
    },
    {
      id: 'education',
      main: '教育与培训',
      sub: '教师 教研 留学 心理',
    },
    {
      id: 'health',
      main: '医疗与健康',
      sub: '临床 护理 医技 康复',
    },
    {
      id: 'biotech',
      main: '生物制药与化工',
      sub: '基因 制药 化学 质检',
    },
  ],
  [
    {
      id: 'manufacturing',
      main: '制造业与工业生产',
      sub: '机械 电气 生产 供应链',
    },
    {
      id: 'automobile',
      main: '汽车与交通装备',
      sub: '研发 测试 智驾 服务',
    },
    {
      id: 'construction',
      main: '建筑工程与地产',
      sub: '设计 施工 造价 物业',
    },
    {
      id: 'energy',
      main: '能源矿业与环保',
      sub: '电力 新能源 环保 安全',
    },
    {
      id: 'retail',
      main: '电商与零售',
      sub: '选品 运营 直播 门店',
    },
    {
      id: 'marketing',
      main: '市场广告与公关',
      sub: '品牌 投流 内容 活动',
    },
  ],
  [
    {
      id: 'media',
      main: '文化传媒与内容',
      sub: '编辑 摄像 编导 自媒体',
    },
    {
      id: 'design',
      main: '艺术与设计',
      sub: '平面 三维 室内 交互',
    },
    {
      id: 'legal',
      main: '法律咨询与知识产权',
      sub: '律师 法务 咨询 合规',
    },
    {
      id: 'admin',
      main: '企业管理与行政',
      sub: '行政 人事 秘书 经理',
    },
    {
      id: 'sales',
      main: '销售与客户服务',
      sub: '大客户 渠道 客服 商务',
    },
    {
      id: 'logistics',
      main: '物流仓储与供应链',
      sub: '仓储 配送 采购 报关',
    },
  ],
  [
    {
      id: 'hospitality',
      main: '餐饮酒店与旅游',
      sub: '厨师 酒店 导游 会展',
    },
    {
      id: 'public',
      main: '公共服务与政府',
      sub: '社区 外事 消防 应急',
    },
    {
      id: 'sports',
      main: '体育与健身',
      sub: '教练 康复 赛事 电竟',
    },
    {
      id: 'service',
      main: '家政与生活服务',
      sub: '月嫂 维修 美业 宠物',
    },
    {
      id: 'security',
      main: '安保与应急服务',
      sub: '安检 消防 安全 风险',
    },
    {
      id: 'freelance',
      main: '自由职业与新兴职业',
      sub: '自媒体 写手 AI创作 顾问',
    },
  ],
]

/* 行业卡片的简单线性插图：统一 32×32 视图、描边无填充，颜色跟随卡片文字色 */
const CATEGORY_ICONS = {
  it_ai:
    '<rect x="9" y="9" width="14" height="14" rx="2.5"/><rect x="13.5" y="13.5" width="5" height="5" rx="1.2"/><path d="M13 9V4.6M19 9V4.6M13 27.4v-4.4M19 27.4v-4.4M9 13H4.6M9 19H4.6M27.4 13H23M27.4 19H23"/>',
  chip:
    '<path d="M16 27.5V14"/><path d="M11.2 14a7.4 7.4 0 0 1 9.6 0"/><path d="M6.6 13.4a13.4 13.4 0 0 1 18.8 0"/><circle cx="16" cy="26.4" r="1.4"/><path d="M12.4 20.6l3.6 3.2 3.6-3.2"/>',
  finance:
    '<circle cx="16" cy="16" r="10.4"/><path d="M16 8.6v14.8M12.6 12.4h5.8a2.4 2.4 0 0 1 0 4.8h-4.4a2.4 2.4 0 0 0 0 4.8h6.2"/>',
  education:
    '<path d="M6 9.6c3.6-1.7 6.8-1.7 10 0 3.2-1.7 6.4-1.7 10 0v13.8c-3.6-1.7-6.8-1.7-10 0-3.2-1.7-6.4-1.7-10 0z"/><path d="M16 9.6v13.8"/>',
  health:
    '<circle cx="16" cy="16" r="10.6"/><path d="M16 10.4v11.2M10.4 16h11.2"/>',
  biotech:
    '<path d="M13 5.6h6M14.2 5.6v7L9.8 21.8a3 3 0 0 0 2.6 4.6h7.2a3 3 0 0 0 2.6-4.6l-4.4-9.2v-7"/><path d="M11.6 18.6h8.8"/>',
  manufacturing:
    '<circle cx="16" cy="16" r="5.4"/><path d="M16 4.6v3.4M16 24v3.4M4.6 16H8M24 16h3.4M8.2 8.2l2.4 2.4M21.4 21.4l2.4 2.4M23.8 8.2l-2.4 2.4M10.6 21.4l-2.4 2.4"/>',
  automobile:
    '<path d="M5 20.8v-4.2l2.7-5.3a2.5 2.5 0 0 1 2.2-1.4h12.2a2.5 2.5 0 0 1 2.2 1.4L27 16.6v4.2"/><path d="M5 20.8h22"/><circle cx="10.4" cy="21.6" r="2.4"/><circle cx="21.6" cy="21.6" r="2.4"/><path d="M8.2 16.6h15.6"/>',
  construction:
    '<path d="M7.6 27.4V6.4M7.6 8.6h14.8M7.6 13.6h5.4M22.4 8.6v4.6M18.8 13.2h7.2l-3.6 5z"/>',
  energy:
    '<path d="M17.8 4.6L8.4 18h6.4l-2 9.4L22.4 14h-6.4z"/>',
  retail:
    '<path d="M7.8 11h16.4l-1.4 14.6a2 2 0 0 1-2 1.8H11.2a2 2 0 0 1-2-1.8z"/><path d="M12 11V9.6a4 4 0 0 1 8 0V11"/><path d="M12.6 16.4v5.2M19.4 16.4v5.2"/>',
  marketing:
    '<path d="M5.6 13.4h4.6l10.4-5.4v16l-10.4-5.4H5.6z"/><path d="M10.2 13.4v6.4a2.6 2.6 0 0 0 5.2 0"/><path d="M23.4 11.6c1.8 2.4 1.8 6.4 0 8.8"/>',
  media:
    '<rect x="4.6" y="10.6" width="14" height="12.8" rx="2.4"/><circle cx="11.6" cy="17" r="3.4"/><path d="M18.6 15.2l7.6-3.8v11.2l-7.6-3.8z"/>',
  design:
    '<path d="M16 5.6c6.1 0 10.6 3.9 10.6 8.8 0 3.4-2.6 5.2-5 5.2h-2.2a2.2 2.2 0 0 0-1.7 3.7c.6.7.2 1.9-.8 2.2-1 .3-2 .4-3 .4-6.1 0-10.5-4.7-10.5-9.9C3.4 9.9 9.9 5.6 16 5.6z"/><circle cx="11.8" cy="12.6" r="1.6"/><circle cx="19.8" cy="12.6" r="1.6"/><circle cx="12.4" cy="19.2" r="1.6"/>',
  legal:
    '<path d="M16 6.4v18M10.6 25.4h10.8M6.6 11.2h18.8"/><path d="M6.6 11.2l-2.6 6.2a3.4 3.4 0 0 0 5.2 0z"/><path d="M25.4 11.2l-2.6 6.2a3.4 3.4 0 0 0 5.2 0z"/>',
  admin:
    '<path d="M5.2 10.6a2.4 2.4 0 0 1 2.4-2.4h4l2.6 3h10.6a2.4 2.4 0 0 1 2.4 2.4v10a2.4 2.4 0 0 1-2.4 2.4H7.6a2.4 2.4 0 0 1-2.4-2.4z"/><path d="M5.2 15.4h21.6"/>',
  sales:
    '<circle cx="12" cy="12.4" r="3.6"/><path d="M5.6 25.4a6.6 6.6 0 0 1 12.8 0"/><path d="M20.4 7.2h8v6.2h-4.4l-2.6 2.4v-2.4h-1z"/>',
  logistics:
    '<path d="M16 5.6l10 4.8v11.2l-10 4.8-10-4.8V10.4z"/><path d="M6 10.4l10 4.8 10-4.8M16 15.2v11.2"/>',
  hospitality:
    '<path d="M7.8 11.4h13.2v7.4a6 6 0 0 1-6 6h-1.2a6 6 0 0 1-6-6z"/><path d="M21 13.4h2.6a2.8 2.8 0 0 1 0 5.6H21"/><path d="M11.4 7.8V4.6M15.8 7.8V4.6"/>',
  public:
    '<path d="M16 5.4l9.2 3.4v7.4c0 5.2-3.9 8.8-9.2 10.4-5.3-1.6-9.2-5.2-9.2-10.4V8.8z"/><path d="M11.8 16.2l3 3 5.4-5.6"/>',
  sports:
    '<path d="M11.4 16h9.2"/><rect x="6.2" y="11.4" width="4.6" height="9.2" rx="1.6"/><rect x="21.2" y="11.4" width="4.6" height="9.2" rx="1.6"/><path d="M3.4 14.2v3.6M28.6 14.2v3.6"/>',
  service:
    '<path d="M20.6 5.6a6.4 6.4 0 0 0-7.8 8.6L5.6 21.4l3.9 3.9 7-7.2a6.4 6.4 0 0 0 8.8-7.6l-4.3 4.3-3.4-3.4z"/>',
  security:
    '<rect x="7" y="14.2" width="18" height="13" rx="2.6"/><path d="M11.2 14.2v-3.6a4.8 4.8 0 0 1 9.6 0v3.6"/><path d="M16 19v3.8"/>',
  freelance:
    '<path d="M20.6 5.6l5.8 5.8-14.6 14.6-6.8 1 1-6.8z"/><path d="M17.6 8.6l5.8 5.8"/>',
}

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

const displayHotJobs = computed(() => hotJobs.value.slice(0, 3))

const hotDirections = computed(() => {
  const directions = []
  const seen = new Set()
  for (const job of hotJobs.value) {
    const query = String(job?.jobTitle || '').trim()
    if (!query || seen.has(query)) continue
    seen.add(query)
    directions.push({
      label: query,
      query,
      skills: parseJobSkills(job.skills),
    })
  }
  if (directions.length) {
    return directions.slice(0, 6)
  }
  return hotSearches.map((item) => ({
    label: item,
    query: item,
    skills: [item],
  }))
})

const hotJobsWeekLabel = computed(() => {
  const first = hotJobs.value[0]
  if (!first?.weekStartDate || !first?.weekEndDate) return ''
  return `${String(first.weekStartDate).slice(0, 10)} — ${String(first.weekEndDate).slice(0, 10)}`
})

function parseJobSkills(skillsText) {
  return String(skillsText || '')
    .split(/[,，、]/)
    .map((item) => item.trim())
    .filter(Boolean)
}

function resolveJobSearchLink(job) {
  return resolveBossJobSearchLinkFromJob(job)
}

function openBossSearch(keyword) {
  const query = String(keyword || '').trim() || '软件工程师'
  window.open(resolveBossJobSearchLink(query), '_blank', 'noopener,noreferrer')
}

async function loadHotJobs() {
  hotJobsLoading.value = true
  try {
    const result = await getLatestJobRecommendations()
    hotJobs.value = Array.isArray(result?.data) ? result.data : []
  } catch {
    hotJobs.value = []
  } finally {
    hotJobsLoading.value = false
  }
}

onMounted(loadHotJobs)

const currentPage = ref(0)
const activeCategoryId = ref('')
const detailPinned = ref(false)

const pageCount = computed(() => categoryPages.length)
const currentCategories = computed(() => categoryPages[currentPage.value] ?? [])
const activeCategory = computed(() => categoryDetails[activeCategoryId.value] ?? null)

function changePage(step) {
  const nextPage = currentPage.value + step
  if (nextPage < 0 || nextPage >= pageCount.value) {
    return
  }
  currentPage.value = nextPage
  activeCategoryId.value = ''
  detailPinned.value = false
}

function showCategory(id) {
  activeCategoryId.value = id
}

function resetPreview() {
  if (!detailPinned.value) {
    activeCategoryId.value = ''
  }
}

function keepPreview() {
  if (activeCategoryId.value) {
    detailPinned.value = true
  }
}

function releasePreview() {
  detailPinned.value = false
  activeCategoryId.value = ''
}

/* ============================================================
 * 首页展示层数据
 * 1) 问候语、日期、岗位分类编号来自现有数据与登录信息；
 * 2) 个人成长区（目标岗位 / 能力进度 / 今日计划 / 课程进度）
 *    后端暂无对应接口，这里统一集中为展示用示例数据，
 *    接入真实数据时只需替换下面这一段常量，其余结构与逻辑不变。
 * ============================================================ */

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

/* 目标岗位：由用户从「岗位探索」已有的岗位方向里自选，选择结果记录在本地，刷新后依然生效 */
const TARGET_JOB_STORAGE_KEY = 'home_target_job'
const DEFAULT_TARGET_JOB = 'Python 开发工程师'

const defaultTargetProfile = {
  title: DEFAULT_TARGET_JOB,
  matchRate: 72,
  note: '距离目标岗位还差两项能力，先补齐 FastAPI 与项目实战。',
  skills: ['Python', 'MySQL', 'FastAPI', '项目实战'],
  /* 核心技能进度：与匹配度一样属于展示层数据，后端岗位匹配接口就绪后替换这一段即可 */
  skillProgress: [
    { name: 'Python', value: 78 },
    { name: 'MySQL', value: 66 },
    { name: 'FastAPI', value: 48 },
    { name: '项目实战', value: 56 },
  ],
}

const targetJobTitle = ref(localStorage.getItem(TARGET_JOB_STORAGE_KEY) || DEFAULT_TARGET_JOB)
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
  if (!seen.has(DEFAULT_TARGET_JOB)) {
    seen.set(DEFAULT_TARGET_JOB, { title: DEFAULT_TARGET_JOB, category: '互联网与人工智能', group: '开发与技术' })
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

const targetJob = computed(() => {
  const title = targetJobTitle.value
  const option = jobOptions.value.find((job) => job.title === title)
  if (title === DEFAULT_TARGET_JOB) {
    return { ...defaultTargetProfile, category: option?.category || '', group: option?.group || '' }
  }
  return {
    title,
    category: option?.category || '',
    group: option?.group || '',
    matchRate: null,
    note: '完成岗位体检后，这里会显示匹配度与技能差距。',
    skills: [],
    skillProgress: [],
  }
})

/* 当前能力成长：后端暂无对应接口，沿用上面的展示层数据约定，接口就绪后替换这一段 */
const abilityGrowth = [
  { id: 'skill', name: '技能掌握', value: 68 },
  { id: 'project', name: '项目完成', value: 45 },
  { id: 'interview', name: '面试准备', value: 32 },
]

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

const todayPlan = ref([
  { id: 'plan-python-basic', title: 'Python 基础', meta: '已完成 2 个课时', state: 'done' },
  { id: 'plan-fastapi', title: 'FastAPI 实训', meta: '进行中 · 接口调试', state: 'doing' },
  { id: 'plan-interview', title: 'AI 模拟面试', meta: '安排在今天 20:00', state: 'todo' },
])

const isAddingPlan = ref(false)
const newPlanTitle = ref('')
const planInputRef = ref(null)

const myCourses = [
  { id: 'fastapi', title: 'FastAPI 接口开发', type: '实训课 · 12 课时', progress: 62, tone: 'pink', cover: 'api' },
  { id: 'mysql', title: 'MySQL 多表查询', type: '基础课 · 8 课时', progress: 45, tone: 'blue', cover: 'db' },
  { id: 'project', title: 'Python 项目实战', type: '项目课 · 6 个项目', progress: 28, tone: 'green', cover: 'code' },
]

const todayPlanDone = computed(() => todayPlan.value.filter((task) => task.state === 'done').length)

function togglePlanTask(task) {
  task.state = task.state === 'done' ? 'todo' : 'done'
}

async function startAddPlan() {
  isAddingPlan.value = true
  await nextTick()
  planInputRef.value?.focus()
}

function submitPlanTask() {
  const title = newPlanTitle.value.trim()
  if (title) {
    todayPlan.value.push({ id: `plan-${Date.now()}`, title, meta: '刚刚添加', state: 'todo' })
  }
  newPlanTitle.value = ''
  isAddingPlan.value = false
}

function cancelPlanTask() {
  newPlanTitle.value = ''
  isAddingPlan.value = false
}

function formatCategorySub(sub) {
  return String(sub || '')
    .split(/[\s、,，·]+/)
    .filter(Boolean)
    .join(' · ')
}

function categoryIndex(index) {
  return String(currentPage.value * 6 + index + 1).padStart(2, '0')
}

// 岗位探索：默认展示当前这一组的第一个方向，鼠标经过时切换，避免出现空白区
const featuredCategoryId = computed(() => activeCategoryId.value || currentCategories.value[0]?.id || '')
const featuredCategory = computed(() => categoryDetails[featuredCategoryId.value] ?? null)

// 推荐岗位：优先展示本周真实岗位，岗位雷达暂无数据时退回热门方向入口
const recommendJobs = computed(() => {
  if (displayHotJobs.value.length) {
    return displayHotJobs.value.map((job) => ({
      id: job.id || job.jobTitle,
      title: job.jobTitle,
      skills: parseJobSkills(job.skills),
      meta: JOB_SALARY_HINT,
      href: resolveJobSearchLink(job),
      cta: JOB_BOSS_CTA,
    }))
  }
  return hotDirections.value.slice(0, 3).map((direction) => ({
    id: direction.query,
    title: direction.label,
    skills: direction.skills,
    meta: '岗位方向 · 前往 BOSS 直聘查看真实公司与薪资',
    href: resolveBossJobSearchLink(direction.query),
    cta: '查看岗位',
  }))
})
</script>

<template>
  <div class="home-view">
    <AppTabBar embedded />

    <div class="hp-main">
      <!-- Hero：校园插画海报 + 搜索 + 成长路线入口 -->
      <section class="hp-hero">
        <div class="hp-hero__body">
          <p class="hp-eyebrow">{{ todayLabel }} · {{ greeting }}，{{ displayName }}</p>
          <h1 class="hp-hero__title">找准方向，再开始成长</h1>
          <p class="hp-hero__desc">
            上传简历，AI 分析你的能力差距，并生成岗位匹配结果与专属学习路径。
          </p>

          <form class="hp-search" @submit.prevent="openBossSearch(searchKeyword)">
            <svg class="hp-search__icon" viewBox="0 0 24 24" aria-hidden="true">
              <circle cx="11" cy="11" r="6.5" />
              <path d="M16 16.2 20.4 20.6" />
            </svg>
            <input
              v-model="searchKeyword"
              type="text"
              placeholder="搜索职位、公司，例如：AI 大模型工程师"
            />
            <button type="submit">搜索</button>
          </form>

          <p class="hp-hotline">
            <span class="hp-hotline__label">热门</span>
            <template v-for="(item, index) in hotSearches" :key="item">
              <span v-if="index" class="hp-hotline__sep">·</span>
              <button class="hp-hotline__item" type="button" @click="openBossSearch(item)">{{ item }}</button>
            </template>
          </p>

          <div class="hp-hero__actions">
            <button class="hp-btn hp-btn--solid" type="button" @click="router.push('/ai-career/resume')">
              开始岗位体检
            </button>
            <button class="hp-link" type="button" @click="router.push('/career/star-map')">
              查看岗位星图 →
            </button>
          </div>
        </div>

        <div class="hp-hero__visual">
          <img class="hp-hero__art" :src="campusHeroArt" alt="校园插画：学生坐在校园里阅读求职资料" />
        </div>

        <nav class="hp-route" aria-label="成长路线">
          <a class="hp-route__item" href="#stage-1">
            <span class="hp-route__num">01</span>
            <span class="hp-route__name">找准方向</span>
          </a>
          <a class="hp-route__item" href="#stage-2">
            <span class="hp-route__num">02</span>
            <span class="hp-route__name">锁定目标</span>
          </a>
          <a class="hp-route__item" href="#stage-3">
            <span class="hp-route__num">03</span>
            <span class="hp-route__name">补齐能力</span>
          </a>
          <a class="hp-route__item" href="#stage-4">
            <span class="hp-route__num">04</span>
            <span class="hp-route__name">走向未来</span>
          </a>
        </nav>
      </section>

      <!-- 01 找准方向 -->
      <section id="stage-1" class="hp-stage">
        <div class="hp-stage__rail">
          <span class="hp-stage__num">01</span>
          <h2 class="hp-stage__title">找准方向</h2>
          <p class="hp-stage__desc">先看清行业和岗位方向，再决定往哪走。</p>
        </div>

        <div class="hp-stage__body">
          <HomeSection size="sm" title="岗位探索" hint="了解方向后再确定目标">
            <article class="hp-card hp-starmap">
              <p class="hp-starmap__desc">从行业、岗位方向与技能要求出发，找到适合自己的职业方向。</p>
              <div class="hp-tags">
                <span class="hp-tag">行业探索</span>
                <span class="hp-tag">岗位方向</span>
                <span class="hp-tag">岗位搜索</span>
              </div>
              <button class="hp-btn hp-btn--solid" type="button" @click="router.push('/career')">
                进入岗位探索
              </button>
            </article>
          </HomeSection>
        </div>
      </section>

      <!-- 02 锁定目标 -->
      <section id="stage-2" class="hp-stage">
        <div class="hp-stage__rail">
          <span class="hp-stage__num">02</span>
          <h2 class="hp-stage__title">锁定目标</h2>
          <p class="hp-stage__desc">知道差距在哪，才知道下一步补什么。</p>
        </div>

        <div class="hp-stage__body hp-stage__body--target">
          <HomeSection class="hp-tile" size="sm" title="目标岗位">
            <article class="hp-card hp-target">
              <div class="hp-target__top">
                <div class="hp-target__info">
                  <p class="hp-target__job">{{ targetJob.title }}</p>
                  <p v-if="targetJob.category" class="hp-target__meta">
                    所属方向：{{ targetJob.category }}<template v-if="targetJob.group"> · {{ targetJob.group }}</template>
                  </p>
                  <p class="hp-target__note">{{ targetJob.note }}</p>
                </div>
                <div v-if="targetJob.matchRate" class="hp-match">
                  <span class="hp-match__value">{{ targetJob.matchRate }}%</span>
                  <span class="hp-match__label">匹配度</span>
                </div>
                <div v-else class="hp-match hp-match--quiet">
                  <span class="hp-match__value">—</span>
                  <span class="hp-match__label">待体检</span>
                </div>
              </div>

              <div v-if="targetJob.skillProgress.length" class="hp-target__skills">
                <SkillProgress
                  v-for="item in targetJob.skillProgress"
                  :key="item.name"
                  :label="item.name"
                  :value="item.value"
                  tone="pink"
                />
              </div>
              <div v-else class="hp-target__tags">
                <div v-if="targetJob.skills.length" class="hp-tags">
                  <span v-for="skill in targetJob.skills" :key="skill" class="hp-tag">{{ skill }}</span>
                </div>
                <p v-else class="hp-target__meta">完成岗位体检后，这里会显示核心技能进度。</p>
              </div>

              <div class="hp-target__actions">
                <button class="hp-btn hp-btn--solid" type="button" @click="openTargetJobDetail">
                  查看岗位详情
                </button>
                <button class="hp-btn hp-btn--ghost" type="button" @click="openJobPicker">调整目标</button>
              </div>
            </article>
          </HomeSection>

          <HomeSection class="hp-tile" size="sm" title="岗位星图">
            <article class="hp-card hp-starmap">
              <p class="hp-starmap__desc">把目标岗位和相邻岗位放在同一张图里，看清能力之间的关系再决定往哪走。</p>
              <ul class="hp-starmap__list">
                <li>岗位节点可点击查看</li>
                <li>配套课程与技能一起展开</li>
              </ul>
              <button class="hp-btn hp-btn--ghost" type="button" @click="router.push('/career/star-map')">
                打开岗位星图
              </button>
            </article>
          </HomeSection>
        </div>
      </section>

      <!-- 03 补齐能力 -->
      <section id="stage-3" class="hp-stage">
        <div class="hp-stage__rail">
          <span class="hp-stage__num">03</span>
          <h2 class="hp-stage__title">补齐能力</h2>
          <p class="hp-stage__desc">把目标拆成今天能完成的事，边学边看进度。</p>
        </div>

        <div class="hp-stage__body hp-stage__body--grow">
          <HomeSection class="hp-tile hp-tile--plan" size="sm" title="今日任务">
            <template #aside>
              <span class="hp-plan__count">{{ todayPlanDone }} / {{ todayPlan.length }}</span>
              <button
                class="hp-plan__add"
                type="button"
                aria-label="添加今日计划"
                title="添加今日计划"
                @click="startAddPlan"
              >+</button>
            </template>

            <article class="hp-card hp-plan">
              <ul class="hp-plan__list">
                <li v-for="task in todayPlan" :key="task.id">
                  <button
                    class="hp-plan__item"
                    :class="`is-${task.state}`"
                    type="button"
                    :aria-pressed="task.state === 'done'"
                    :title="task.state === 'done' ? '点击标记为未完成' : '点击标记为已完成'"
                    @click="togglePlanTask(task)"
                  >
                    <span class="hp-plan__mark"></span>
                    <span class="hp-plan__title">{{ task.title }}</span>
                    <span class="hp-plan__meta">{{ task.meta }}</span>
                  </button>
                </li>
                <li v-if="isAddingPlan" class="hp-plan__edit">
                  <span class="hp-plan__mark hp-plan__mark--todo"></span>
                  <input
                    ref="planInputRef"
                    v-model="newPlanTitle"
                    type="text"
                    maxlength="40"
                    placeholder="输入计划内容，回车保存"
                    @keyup.enter="submitPlanTask"
                    @keyup.esc="cancelPlanTask"
                  />
                </li>
              </ul>
            </article>
          </HomeSection>

          <HomeSection class="hp-tile hp-tile--ability" size="sm" title="成长动态">
            <article class="hp-card hp-ability">
              <SkillProgress
                v-for="item in abilityGrowth"
                :key="item.id"
                :label="item.name"
                :value="item.value"
                tone="blue"
              />
            </article>
          </HomeSection>

          <HomeSection class="hp-tile hp-tile--courses" size="sm" title="继续学习">
            <template #aside>
              <button class="hp-link" type="button" @click="router.push('/learning/python')">
                全部课程 →
              </button>
            </template>

            <article class="hp-card hp-course-tile">
              <ul class="hp-courses__list">
                <li v-for="course in myCourses" :key="course.id">
                  <button
                    class="hp-course hp-course--row"
                    type="button"
                    @click="router.push('/learning/python')"
                  >
                    <span class="hp-course__cover" :class="`is-${course.tone}`">
                  <svg v-if="course.cover === 'api'" viewBox="0 0 320 200" aria-hidden="true">
                    <rect x="70" y="48" width="180" height="104" rx="14" fill="#FFFFFF" stroke="#C9D3DE" stroke-width="3" />
                    <path d="M70 78h180" stroke="#C9D3DE" stroke-width="3" />
                    <circle cx="88" cy="63" r="3.6" fill="#DCE8F3" />
                    <circle cx="100" cy="63" r="3.6" fill="#DEE9D8" />
                    <circle cx="112" cy="63" r="3.6" fill="#F7E9C6" />
                    <rect x="90" y="94" width="76" height="8" rx="4" fill="#DCE8F3" />
                    <rect x="90" y="112" width="112" height="8" rx="4" fill="#DEE9D8" />
                    <rect x="90" y="130" width="56" height="8" rx="4" fill="#F7E9C6" />
                  </svg>
                  <svg v-else-if="course.cover === 'db'" viewBox="0 0 320 200" aria-hidden="true">
                    <path d="M106 62v76c0 11 25 20 54 20s54-9 54-20V62" fill="#FFFFFF" stroke="#C9D3DE" stroke-width="3" />
                    <ellipse cx="160" cy="62" rx="54" ry="20" fill="#DCE8F3" stroke="#C9D3DE" stroke-width="3" />
                    <path d="M106 90c0 11 25 20 54 20s54-9 54-20" fill="none" stroke="#C9D3DE" stroke-width="3" />
                    <path d="M106 118c0 11 25 20 54 20s54-9 54-20" fill="none" stroke="#C9D3DE" stroke-width="3" />
                  </svg>
                  <svg v-else viewBox="0 0 320 200" aria-hidden="true">
                    <path
                      d="M124 70 98 100l26 30"
                      fill="none"
                      stroke="#8FA2B4"
                      stroke-width="7"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    />
                    <path
                      d="M196 70l26 30-26 30"
                      fill="none"
                      stroke="#8FA2B4"
                      stroke-width="7"
                      stroke-linecap="round"
                      stroke-linejoin="round"
                    />
                    <path d="M174 60l-28 80" fill="none" stroke="#E3B3BD" stroke-width="7" stroke-linecap="round" />
                  </svg>
                    </span>
                    <span class="hp-course__body">
                      <span class="hp-course__title">{{ course.title }}</span>
                      <span class="hp-course__type">{{ course.type }}</span>
                      <span class="hp-course__progress">
                        <span class="hp-bar__track"><i :style="{ width: `${course.progress}%` }"></i></span>
                        <span class="hp-course__value">{{ course.progress }}%</span>
                      </span>
                    </span>
                  </button>
                </li>
              </ul>
            </article>
          </HomeSection>
        </div>
      </section>

      <!-- 04 走向未来 -->
      <section id="stage-4" class="hp-stage">
        <div class="hp-stage__rail">
          <span class="hp-stage__num">04</span>
          <h2 class="hp-stage__title">走向未来</h2>
          <p class="hp-stage__desc">看看真实岗位在招什么，再回到自己的节奏。</p>
        </div>

        <div class="hp-stage__body">
          <HomeSection size="sm" title="实习推荐">
            <template #aside>
              <span v-if="hotJobsWeekLabel" class="hp-section__meta">{{ hotJobsWeekLabel }}</span>
              <button class="hp-link" type="button" @click="router.push('/employment')">查看全部 →</button>
            </template>

            <p v-if="hotJobsLoading" class="hp-jobs__state">正在整理本周岗位…</p>

            <ul v-else class="hp-jobs__grid">
              <!-- 公司 / 地点 / 匹配度：岗位雷达接口暂未返回这几个字段，卡片只展示真实返回的岗位名与技能 -->
              <li v-for="job in recommendJobs" :key="job.id" class="hp-job">
                <div class="hp-job__head">
                  <span class="hp-job__logo">{{ job.title.charAt(0) }}</span>
                  <div class="hp-job__copy">
                    <p class="hp-job__title">{{ job.title }}</p>
                    <p class="hp-job__meta">{{ job.meta }}</p>
                  </div>
                </div>
                <div class="hp-tags">
                  <span v-for="item in job.skills" :key="item" class="hp-tag">{{ item }}</span>
                </div>
                <a class="hp-link hp-job__link" :href="job.href" target="_blank" rel="noreferrer">
                  {{ job.cta }} →
                </a>
              </li>
            </ul>
          </HomeSection>

          <div class="hp-closing">
            <div class="hp-closing__copy">
              <p class="hp-closing__title">想知道自己和目标岗位还差多少？</p>
              <p class="hp-closing__desc">上传简历，AI 会给出匹配度、能力差距和提升建议。</p>
            </div>
            <button class="hp-btn hp-btn--solid" type="button" @click="router.push('/ai-career/resume')">
              开始岗位体检
            </button>
          </div>
        </div>
      </section>

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
    </div>
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

.home-view,
.hp-modal {
  --hp-bg: #f7f5f1;
  --hp-surface: #ffffff;
  --hp-surface-2: #fbf9f5;
  --hp-ink: #23262b;
  --hp-ink-2: #5a6069;
  --hp-muted: #8a9099;
  --hp-line: #eae4da;
  --hp-line-strong: #ded7cb;
  --hp-pink: #f2d9de;
  --hp-pink-ink: #b4707f;
  --hp-blue: #dce8f3;
  --hp-blue-ink: #5c8cb4;
  --hp-green: #dee9d8;
  --hp-green-ink: #6f9463;
  --hp-yellow: #f7e9c6;
  --hp-yellow-ink: #a8822b;
  --hp-tint: #eff4f9;
  --hp-track: #efeae1;
  --hp-r-lg: 24px;
  --hp-r-md: 20px;
  --hp-r-sm: 12px;
  --hp-shadow-sm: 0 1px 2px rgba(35, 38, 43, 0.04), 0 4px 12px rgba(35, 38, 43, 0.04);
  --hp-shadow-md: 0 2px 4px rgba(35, 38, 43, 0.04), 0 12px 28px rgba(35, 38, 43, 0.06);
  --hp-shadow-lg: 0 4px 8px rgba(35, 38, 43, 0.04), 0 20px 44px rgba(35, 38, 43, 0.08);
  --hp-gap-section: 32px;
  --hp-gap: 24px;
  --hp-font: Inter, 'Segoe UI', system-ui, -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.home-view {
  position: relative;
  min-height: 100vh;
  overflow-x: clip;
  background: var(--hp-bg);
  color: var(--hp-ink);
  font-family: var(--hp-font);
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

/* ---------- 成长路线：每个阶段 = 左侧路线轴 + 右侧模块拼贴 ---------- */

.hp-stage {
  position: relative;
  display: grid;
  grid-template-columns: 200px minmax(0, 1fr);
  gap: 32px;
  margin-top: 44px;
  padding-left: 34px;
  align-items: stretch;
}

/* 连接各阶段的路线虚线 */
.hp-stage::before {
  content: '';
  position: absolute;
  left: 8px;
  top: 10px;
  bottom: -44px;
  width: 1px;
  background: repeating-linear-gradient(180deg, #e2dbd0 0 6px, rgba(0, 0, 0, 0) 6px 12px);
}

.hp-stage:last-of-type::before {
  bottom: 40px;
}

.hp-stage__rail {
  position: sticky;
  top: 84px;
  align-self: start;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 路线轴上的节点 */
.hp-stage__rail::after {
  content: '';
  position: absolute;
  left: -30px;
  top: 6px;
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: #9dc0dd;
  box-shadow: 0 0 0 3px var(--hp-bg);
}

.hp-stage__num {
  color: var(--hp-blue-ink);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.18em;
}

.hp-stage__title {
  margin: 0;
  color: var(--hp-ink);
  font-size: 30px;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1.2;
}

.hp-stage__desc {
  margin: 2px 0 0;
  color: var(--hp-muted);
  font-size: 13.5px;
  line-height: 1.75;
}

.hp-stage__body {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

/* 阶段内的模块拼贴 */
.hp-stage__body--target {
  display: grid;
  grid-template-columns: minmax(0, 8fr) minmax(0, 4fr);
  gap: 24px;
  align-items: stretch;
}

.hp-stage__body--grow {
  display: grid;
  grid-template-columns: minmax(0, 4fr) minmax(0, 8fr);
  gap: 24px;
  align-items: stretch;
}

.hp-tile--plan {
  grid-column: 1;
  grid-row: 1;
}

.hp-tile--ability {
  grid-column: 1;
  grid-row: 2;
}

.hp-tile--courses {
  grid-column: 2;
  grid-row: 1 / span 2;
}

.hp-tile {
  display: flex;
  flex-direction: column;
}

.hp-tile .hp-card {
  flex: 1;
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

.hp-search {
  display: flex;
  align-items: center;
  gap: 12px;
  width: min(100%, 560px);
  margin-top: 28px;
  padding: 6px 6px 6px 18px;
  border: 1px solid var(--hp-line-strong);
  border-radius: 999px;
  background: var(--hp-surface);
  box-shadow: var(--hp-shadow-sm);
}

.hp-search__icon {
  width: 18px;
  height: 18px;
  flex: 0 0 auto;
  fill: none;
  stroke: var(--hp-muted);
  stroke-width: 1.8;
  stroke-linecap: round;
}

.hp-search input {
  flex: 1;
  min-width: 0;
  height: 42px;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--hp-ink);
  font-size: 14px;
}

.hp-search input::placeholder {
  color: #a7adb6;
}

.hp-search button {
  flex: 0 0 auto;
  height: 42px;
  padding: 0 26px;
  border-radius: 999px;
  background: var(--hp-ink);
  color: #ffffff;
  font-size: 14px;
  font-weight: 600;
  transition: opacity 0.18s ease;
}

.hp-search button:hover {
  opacity: 0.88;
}

.hp-hotline {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin: 14px 0 0;
  font-size: 13px;
}

.hp-hotline__label {
  margin-right: 4px;
  color: var(--hp-muted);
  font-size: 12px;
}

.hp-hotline__item {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--hp-ink-2);
  font-size: 13px;
  cursor: pointer;
}

.hp-hotline__item:hover {
  color: var(--hp-blue-ink);
  text-decoration: underline;
}

.hp-hotline__sep {
  color: #c8ccd2;
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

/* Hero 底部的成长路线入口 */
.hp-route {
  grid-area: route;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  margin: 6px -44px 0;
  border-top: 1px solid #e2eaf1;
}

.hp-route__item {
  display: flex;
  align-items: baseline;
  gap: 8px;
  padding: 16px 24px;
  border-right: 1px solid #e2eaf1;
  color: var(--hp-ink-2);
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  transition: background 0.18s ease, color 0.18s ease;
}

.hp-route__item:last-child {
  border-right: 0;
}

.hp-route__item:hover {
  background: rgba(255, 255, 255, 0.65);
  color: var(--hp-ink);
}

.hp-route__num {
  color: var(--hp-blue-ink);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.1em;
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

.hp-pager {
  display: flex;
  align-items: center;
  gap: 12px;
  color: var(--hp-muted);
  font-size: 12px;
}

.hp-pager button {
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--hp-ink-2);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
}

.hp-pager button:hover:not(:disabled) {
  color: var(--hp-ink);
  text-decoration: underline;
}

.hp-pager button:disabled {
  color: #c8ccd2;
  cursor: not-allowed;
}

/* ---------- 岗位探索（浅黄强调） ---------- */

.hp-cats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.hp-cats li {
  display: flex;
}

.hp-cat {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
  min-height: 176px;
  padding: 20px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-md);
  background: var(--hp-surface);
  box-shadow: var(--hp-shadow-sm);
  text-align: left;
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease, background 0.18s ease;
}

.hp-cat:hover {
  transform: translateY(-2px);
  box-shadow: var(--hp-shadow-md);
}

.hp-cat.is-active {
  border-color: #f0dfaf;
  background: #fdf8ea;
}

.hp-cat__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.hp-cat__num {
  color: var(--hp-muted);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.06em;
}

.hp-cat__icon {
  width: 34px;
  height: 34px;
  color: var(--hp-ink-2);
  fill: none;
  stroke: currentColor;
  stroke-width: 1.7;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.hp-cat.is-active .hp-cat__icon {
  color: var(--hp-yellow-ink);
}

.hp-cat__main {
  color: var(--hp-ink);
  font-size: 16px;
  font-weight: 600;
  line-height: 1.45;
}

.hp-cat__sub {
  margin-top: auto;
  color: var(--hp-muted);
  font-size: 12.5px;
  line-height: 1.7;
}

/* 岗位方向 */

.hp-cat-detail {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 16px 44px;
  min-height: 190px;
  margin-top: 20px;
  padding: 24px 26px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-md);
  background: var(--hp-surface);
  box-shadow: var(--hp-shadow-sm);
}

.hp-cat-detail__title {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 0 0 100%;
  margin: 0;
  color: var(--hp-ink);
  font-size: 17px;
  font-weight: 600;
}

.hp-cat-detail__label {
  padding: 3px 10px;
  border-radius: 999px;
  background: var(--hp-yellow);
  color: var(--hp-yellow-ink);
  font-size: 12px;
  font-weight: 600;
}

.hp-cat-detail__group {
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-width: 210px;
}

.hp-cat-detail__group-name {
  margin: 0;
  color: var(--hp-ink-2);
  font-size: 13px;
  font-weight: 600;
  letter-spacing: 0.02em;
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

/* 岗位星图模块 */
.hp-starmap {
  justify-content: space-between;
  gap: 16px;
}

.hp-starmap__desc {
  margin: 0;
  color: var(--hp-ink-2);
  font-size: 13.5px;
  line-height: 1.8;
}

.hp-starmap__list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.hp-starmap__list li {
  position: relative;
  padding-left: 16px;
  color: var(--hp-muted);
  font-size: 13px;
}

.hp-starmap__list li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 7px;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--hp-blue);
}

.hp-starmap .hp-btn {
  align-self: flex-start;
  margin-top: auto;
}

.hp-course__value {
  color: var(--hp-muted);
  font-size: 12px;
}

/* ---------- 当前能力成长（浅蓝强调） ---------- */

.hp-ability {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  gap: 20px;
  padding: 26px;
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

.hp-job__link {
  align-self: flex-start;
  margin-top: auto;
}

/* 收尾的行动条 */
.hp-closing {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-top: 24px;
  padding: 24px 28px;
  border: 1px solid #e4ebf2;
  border-radius: var(--hp-r-md);
  background: var(--hp-tint);
}

.hp-closing__copy {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.hp-closing__title {
  margin: 0;
  color: var(--hp-ink);
  font-size: 18px;
  font-weight: 600;
}

.hp-closing__desc {
  margin: 0;
  color: var(--hp-ink-2);
  font-size: 13px;
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
  font-family: var(--hp-font);
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

/* ---------- 响应式 ---------- */

@media (max-width: 1200px) {
  .hp-stage {
    grid-template-columns: 168px minmax(0, 1fr);
    gap: 24px;
  }

  .hp-stage__body--target,
  .hp-stage__body--grow {
    grid-template-columns: minmax(0, 1fr);
  }

  .hp-tile--plan,
  .hp-tile--ability,
  .hp-tile--courses {
    grid-column: auto;
    grid-row: auto;
  }

  .hp-jobs__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 1000px) {
  .hp-hero {
    grid-template-columns: minmax(0, 1fr);
    grid-template-areas:
      'body'
      'visual'
      'route';
    padding: 36px 28px 0;
  }

  .hp-hero__visual {
    justify-content: flex-start;
  }

  .hp-hero__art {
    margin: 0 -28px -10px 0;
  }

  .hp-route {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    margin: 6px -28px 0;
  }

  .hp-route__item:nth-child(2n) {
    border-right: 0;
  }

  .hp-route__item:nth-child(-n + 2) {
    border-bottom: 1px solid #e2eaf1;
  }

  .hp-stage {
    grid-template-columns: minmax(0, 1fr);
    gap: 16px;
    margin-top: 40px;
    padding-left: 0;
  }

  .hp-stage::before,
  .hp-stage__rail::after {
    display: none;
  }

  .hp-stage__rail {
    position: static;
    flex-direction: row;
    align-items: baseline;
    gap: 12px;
  }

  .hp-stage__desc {
    margin: 0 0 0 auto;
    max-width: 48%;
    font-size: 12.5px;
  }

  .hp-cats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hp-ability {
    gap: 18px;
  }
}

@media (max-width: 680px) {
  .hp-main {
    width: calc(100% - 32px);
    padding: 20px 0 44px;
  }

  .hp-hero {
    padding: 28px 20px 0;
  }

  .hp-hero__art {
    margin: 0 -20px -8px 0;
  }

  .hp-route {
    margin: 6px -20px 0;
  }

  .hp-route__item {
    padding: 13px 16px;
    font-size: 13px;
  }

  .hp-hero__title {
    font-size: 34px;
  }

  .hp-hero__desc {
    font-size: 15px;
  }

  .hp-hero__actions {
    gap: 16px;
  }

  .hp-cats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .hp-cat {
    min-height: 164px;
  }

  .hp-cat-detail {
    padding: 20px;
  }

  .hp-target__top {
    flex-direction: column;
    gap: 16px;
  }

  .hp-target__skills {
    grid-template-columns: minmax(0, 1fr);
  }

  .hp-jobs__grid {
    grid-template-columns: minmax(0, 1fr);
  }

  .hp-cat {
    min-height: 156px;
  }

  .hp-closing {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
    padding: 22px;
  }

  .hp-plan__item {
    flex-wrap: wrap;
  }

  .hp-plan__meta {
    margin-left: 32px;
    white-space: normal;
  }

  .hp-footer {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
