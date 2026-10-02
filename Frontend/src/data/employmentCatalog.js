/* 实习就业页的展示层数据：结构、统计和示例岗位都集中在这里，接真实招聘接口时整份替换即可。
 * 「实习雷达」里的目标岗位与匹配度不在这里 —— 它们取自本地的目标岗位选择
 * （与首页、岗位探索共用同一份），保证三处显示的是同一个目标。 */

export const EMPLOYMENT_TABS = [
  { id: 'all', label: '推荐' },
  { id: 'intern', label: '实习' },
  { id: 'campus', label: '校招' },
  { id: 'local', label: '成都本地' },
  { id: 'alumni', label: '校友企业' },
]

/** 抓取来源 key → 平台名称，页面上展示岗位来自哪些平台时用。 */
export const EMPLOYMENT_SOURCE_NAMES = {
  rc114: '成都人才网',
  'cd-sc91': '成都公共招聘网',
  sc91: '四川公共招聘网',
}

export const EMPLOYMENT_RADAR = {
  found: 12,
  high: 4,
  priority: 3,
}

export const EMPLOYMENT_LOCAL = {
  todayNew: 36,
  intern: 128,
  campus: 86,
  stateOwned: 24,
  districts: ['高新区', '天府新区', '武侯区', '锦江区'],
  /* 热门区域 + 方向：本地就业专区里给学生看的机会分布 */
  hotspots: [
    { district: '高新区', direction: 'AI / 软件开发' },
    { district: '天府新区', direction: '数据 / 算法' },
    { district: '武侯区', direction: '前端 / 产品' },
  ],
}

export const EMPLOYMENT_AGGREGATE = {
  sources: ['企业官网', '成都公共招聘', '学校就业网'],
  job: 'Python 开发实习生',
  points: ['自动去重', '保留来源', '更新时间同步', '岗位状态校验'],
  facts: [
    { label: '来源一致', value: '3 个' },
    { label: '最新更新', value: '今天 09:20' },
    { label: '招聘状态', value: '招聘中' },
    { label: '去重结果', value: '已自动去重' },
  ],
}

export const EMPLOYMENT_CAMPUS = {
  season: '2027 届秋招',
  talks: 12,
  fairs: 3,
  roles: 42,
  schedule: [
    { date: '10.8', title: '企业宣讲' },
    { date: '10.12', title: '秋季双选会' },
    { date: '10.18', title: '专场招聘' },
  ],
}

export const EMPLOYMENT_ALUMNI = {
  companies: 28,
  hiring: 16,
  fields: ['Python', 'Java', 'AI'],
  alumniAtWork: 6,
  openNow: 3,
  /* 校友企业名单：用抓取到的真实成都企业，页签筛选和底部企业徽标都从这里取 */
  partners: [
    { name: '成都风雨兴科技有限公司', short: '风雨' },
    { name: '成都巡洋船舶管理有限公司', short: '巡洋' },
    { name: '成都屿西半导体科技有限公司', short: '屿西' },
  ],
}

/* tags 决定岗位出现在哪个筛选页签下：intern 实习 / campus 校招 / local 成都本地 / alumni 校友企业 */
export const EMPLOYMENT_JOBS = [
  {
    id: 'py-intern',
    title: 'Python 开发实习生',
    company: '成都云启科技',
    matchRate: 86,
    city: '成都',
    district: '高新区',
    skills: ['Python', 'FastAPI', 'MySQL'],
    reason: 'Python 基础符合要求',
    sources: 3,
    updated: '2 小时前更新',
    tags: ['intern', 'local'],
  },
  {
    id: 'ai-intern',
    title: 'AI 应用开发实习生',
    company: '成都智算科技',
    matchRate: 79,
    city: '成都',
    district: '天府新区',
    skills: ['Python', 'LLM', 'RAG'],
    reason: 'Python 基础较匹配',
    sources: 2,
    updated: '今日更新',
    tags: ['intern', 'local'],
  },
  {
    id: 'java-intern',
    title: 'Java 后端开发实习生',
    company: '成都数联信息',
    matchRate: 74,
    city: '成都',
    district: '武侯区',
    skills: ['Java', 'Spring Boot', 'MySQL'],
    reason: '后端基础较扎实',
    sources: 2,
    updated: '今日更新',
    tags: ['intern', 'local'],
  },
  {
    id: 'fe-intern',
    title: '前端开发实习生',
    company: '成都微光网络',
    matchRate: 71,
    city: '成都',
    district: '锦江区',
    skills: ['Vue3', 'JavaScript', 'Vite'],
    reason: '前端项目经历匹配',
    sources: 3,
    updated: '昨天更新',
    tags: ['intern', 'local'],
  },
  {
    id: 'data-intern',
    title: '数据分析实习生',
    company: '成都数知科技',
    matchRate: 68,
    city: '成都',
    district: '高新区',
    skills: ['SQL', 'Excel', '可视化'],
    reason: 'SQL 查询熟练',
    sources: 2,
    updated: '3 天前更新',
    tags: ['intern', 'local'],
  },
  {
    id: 'campus-py',
    title: 'Python 后端开发',
    tag: '校招',
    company: '成都星桥软件',
    matchRate: 76,
    city: '成都',
    district: '高新区',
    skills: ['Python', 'Java', 'MySQL'],
    reason: '与你的目标岗位方向一致',
    sources: 4,
    updated: '今日更新',
    tags: ['campus', 'local'],
  },
  {
    id: 'alumni-ai',
    title: 'AI 应用开发',
    tag: '校友企业',
    company: '成都校友智联',
    matchRate: 81,
    city: '成都',
    district: '天府新区',
    skills: ['Python', 'LLM', 'FastAPI'],
    reason: '校友企业在招同类岗位',
    sources: 2,
    updated: '今日更新',
    tags: ['intern', 'alumni', 'local'],
  },
  {
    id: 'alumni-fe',
    title: '前端开发',
    tag: '校友企业',
    company: '成都南山科技',
    matchRate: 70,
    city: '成都',
    district: '武侯区',
    skills: ['Vue3', 'TypeScript'],
    reason: '技术栈与项目经历匹配',
    sources: 2,
    updated: '昨天更新',
    tags: ['intern', 'alumni', 'local'],
  },
  {
    id: 'remote-py',
    title: 'Python 开发实习生',
    tag: '远程',
    company: '成都远景信息',
    matchRate: 65,
    city: '远程',
    district: '',
    skills: ['Python', 'MySQL'],
    reason: '可远程，时间安排灵活',
    sources: 2,
    updated: '3 天前更新',
    tags: ['intern'],
  },
]
