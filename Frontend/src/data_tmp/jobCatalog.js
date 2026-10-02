/* 岗位探索的展示层数据：岗位探索页与岗位详情页共用这一份。
 * 后端岗位匹配能力就绪后，整体替换本文件即可，页面结构不用改。
 *
 * 1) JOB_PROFILES —— 每个目标岗位一组结果：匹配度、已掌握/待提升、能力差距、推荐岗位卡。
 *    推荐岗位卡（fit）只存「该岗位与我」相关的字段：id、匹配度、与你的优势、需要提升；
 *    岗位名、方向、类型、核心技能统一取自 JOB_DETAILS，避免同一岗位在两处各写一遍。
 * 2) JOB_DETAILS —— 岗位详情页内容，按岗位 id 索引：岗位做什么、岗位要求、我会什么、提升建议。
 *    能力差距 = 岗位要求 - 我会什么，由页面按这两个列表算出来，不另存一份数字。
 */

export const TARGET_JOB_STORAGE_KEY = 'home_target_job'
export const DEFAULT_TARGET_JOB = 'Python 开发工程师'

export const JOB_PROFILES = [
  {
    title: 'Python 开发工程师',
    matchRate: 72,
    mastered: ['Python', 'MySQL'],
    toImprove: ['FastAPI', 'Linux'],
    gaps: [
      { name: 'FastAPI', current: 48, required: 70 },
      { name: 'Linux', current: 30, required: 60 },
      { name: '项目经验', current: 56, required: 80 },
    ],
    advice: '优先补齐 FastAPI、Linux 与项目实践',
    fit: [
      { id: 'py', matchRate: 86, strength: 'Python 基础较好', improve: '项目经验' },
      { id: 'ai-app', matchRate: 73, strength: 'Python 基础较好', improve: 'LLM 应用经验' },
      { id: 'data', matchRate: 68, strength: 'SQL 查询熟练', improve: '可视化与业务分析' },
    ],
  },
  {
    title: '前端开发工程师',
    matchRate: 68,
    mastered: ['HTML / CSS', 'JavaScript'],
    toImprove: ['Vue3', 'TypeScript'],
    gaps: [
      { name: 'Vue3', current: 55, required: 75 },
      { name: 'TypeScript', current: 42, required: 70 },
      { name: '工程化实践', current: 45, required: 70 },
    ],
    advice: '先补齐 Vue3 组件化，再补 TypeScript 与构建工程化',
    fit: [
      { id: 'fe', matchRate: 84, strength: '页面还原度好', improve: 'TypeScript' },
      { id: 'mini', matchRate: 66, strength: '接口联调熟练', improve: '工程化实践' },
      { id: 'full', matchRate: 61, strength: '前后端都能上手', improve: 'Node.js' },
    ],
  },
  {
    title: 'Java 后端开发工程师',
    matchRate: 61,
    mastered: ['Java', 'MySQL'],
    toImprove: ['Spring Boot', 'Redis'],
    gaps: [
      { name: 'Spring Boot', current: 46, required: 75 },
      { name: 'Redis', current: 34, required: 65 },
      { name: '并发与调优', current: 38, required: 70 },
    ],
    advice: '优先补齐 Spring Boot 与 Redis，再补并发与调优',
    fit: [
      { id: 'java', matchRate: 80, strength: 'Java 基础扎实', improve: 'Redis' },
      { id: 'micro', matchRate: 58, strength: '数据库基础较好', improve: 'Spring Cloud' },
      { id: 'test-dev', matchRate: 55, strength: '接口联调熟练', improve: '自动化测试' },
    ],
  },
  {
    title: 'AI 应用工程师',
    matchRate: 70,
    mastered: ['Python', '提示词'],
    toImprove: ['LLM 应用', '向量检索'],
    gaps: [
      { name: 'LLM 应用', current: 52, required: 75 },
      { name: '向量检索', current: 36, required: 65 },
      { name: '服务部署', current: 44, required: 70 },
    ],
    advice: '优先补齐 LLM 应用与向量检索，再把服务部署跑通',
    fit: [
      { id: 'ai-app', matchRate: 85, strength: 'Python 基础较好', improve: 'LLM 应用经验' },
      { id: 'algo', matchRate: 68, strength: '数学基础扎实', improve: 'PyTorch 训练' },
      { id: 'py', matchRate: 74, strength: 'Python 基础较好', improve: '项目经验' },
    ],
  },
  {
    title: '算法工程师',
    matchRate: 57,
    mastered: ['Python', '数学基础'],
    toImprove: ['PyTorch', '模型调优'],
    gaps: [
      { name: 'PyTorch', current: 44, required: 70 },
      { name: '模型调优', current: 38, required: 70 },
      { name: '项目 / 竞赛', current: 30, required: 60 },
    ],
    advice: '先补齐 PyTorch 训练流程，再用项目或竞赛补经历',
    fit: [
      { id: 'algo', matchRate: 69, strength: '数学基础扎实', improve: 'PyTorch 训练' },
      { id: 'ai-app', matchRate: 64, strength: 'Python 基础较好', improve: 'LLM 应用经验' },
      { id: 'data', matchRate: 60, strength: 'SQL 查询熟练', improve: '可视化与业务分析' },
    ],
  },
  {
    title: '数据分析师',
    matchRate: 71,
    mastered: ['SQL', 'Excel'],
    toImprove: ['Python 数据分析', '可视化'],
    gaps: [
      { name: 'Python 数据分析', current: 50, required: 70 },
      { name: '可视化看板', current: 45, required: 70 },
      { name: '业务分析', current: 52, required: 75 },
    ],
    advice: '优先补齐 Python 数据分析与可视化看板',
    fit: [
      { id: 'data', matchRate: 83, strength: 'SQL 查询熟练', improve: '可视化与业务分析' },
      { id: 'ops', matchRate: 65, strength: 'SQL 查询熟练', improve: '指标体系搭建' },
      { id: 'py', matchRate: 62, strength: 'Python 基础较好', improve: '项目经验' },
    ],
  },
  {
    title: '软件测试工程师',
    matchRate: 66,
    mastered: ['测试基础', '用例设计'],
    toImprove: ['自动化测试', '性能测试'],
    gaps: [
      { name: '自动化测试', current: 48, required: 70 },
      { name: '性能测试', current: 32, required: 60 },
      { name: 'Linux', current: 40, required: 65 },
    ],
    advice: '优先补齐自动化测试，再补性能测试与 Linux',
    fit: [
      { id: 'test-dev', matchRate: 74, strength: '接口联调熟练', improve: '自动化测试' },
      { id: 'qa', matchRate: 78, strength: '用例设计规范', improve: '性能测试' },
      { id: 'sre', matchRate: 58, strength: 'Linux 基础较好', improve: 'Docker 与部署' },
    ],
  },
  {
    title: '产品经理',
    matchRate: 59,
    mastered: ['需求分析', '文档撰写'],
    toImprove: ['数据分析', '原型设计'],
    gaps: [
      { name: '数据分析', current: 42, required: 70 },
      { name: '原型设计', current: 46, required: 70 },
      { name: '项目推进', current: 50, required: 75 },
    ],
    advice: '先补齐数据分析与原型设计，再补一个完整项目经历',
    fit: [
      { id: 'pm', matchRate: 72, strength: '需求分析清晰', improve: '项目推进' },
      { id: 'ops', matchRate: 63, strength: 'SQL 查询熟练', improve: '指标体系搭建' },
      { id: 'ux', matchRate: 58, strength: '原型上手快', improve: '用户研究' },
    ],
  },
]

/* 岗位详情：同一份内容同时支撑推荐岗位卡与岗位详情页。
 * requirements 就是岗位卡上的「核心技能」（也决定岗位画像里「用什么」那一列），
 * mySkills 是「我现在会什么」，两者相减得到「能力差距」；
 * portrait 是岗位画像的三列（做什么 / 用什么 / 发展方向），
 * path 是提升路径上可选的实训项目与预计提升。 */
export const JOB_DETAILS = {
  py: {
    title: 'Python 开发工程师',
    direction: '后端开发 / 服务端',
    type: '应届 / 实习',
    summary: '负责后端接口、业务服务与数据处理，把产品需求落成可上线的服务。',
    portrait: [
      { task: '业务接口开发', direction: 'Python 开发' },
      { task: '服务端模块实现', direction: '后端负责人' },
      { task: '数据库表设计', direction: '服务架构' },
    ],
    requirements: [
      { name: 'Python', level: 70, note: '岗位的主要开发语言，要能读懂并改写现有代码' },
      { name: 'FastAPI', level: 65, note: '用来写对外接口，需要能独立跑通一个完整服务' },
      { name: 'MySQL', level: 60, note: '要会建表、写查询，并能看懂慢查询' },
    ],
    mySkills: [
      { name: 'Python', level: 78 },
      { name: 'MySQL', level: 55 },
    ],
    path: [
      { title: 'FastAPI 接口实训', gain: 8 },
      { title: 'MySQL 查询优化练习', gain: 6 },
    ],
    advice: '先把 FastAPI 跑通一个完整项目，再补 MySQL 索引与查询优化。',
  },
  'ai-app': {
    title: 'AI 应用开发工程师',
    direction: 'AI 应用 / 后端',
    type: '实习',
    summary: '把大模型能力接进真实业务，负责提示词、检索与对外接口。',
    portrait: [
      { task: 'RAG 开发', direction: 'AI 应用开发' },
      { task: '提示词设计', direction: 'AI 技术负责人' },
      { task: '接口开发', direction: 'AI 产品技术' },
    ],
    requirements: [
      { name: 'Python', level: 70, note: '调用模型、写服务都靠它，是每天的开发语言' },
      { name: 'LLM 应用', level: 65, note: '要懂提示词、上下文和检索，能把效果调到可用' },
      { name: 'FastAPI', level: 60, note: '把模型能力包成接口给前端调用' },
    ],
    mySkills: [
      { name: 'Python', level: 76 },
      { name: '提示词', level: 58 },
    ],
    path: [
      { title: 'RAG 知识库项目', gain: 9 },
      { title: 'FastAPI 接口实训', gain: 7 },
    ],
    advice: '先做一个 RAG 小项目，把提示词、检索和接口串成完整链路。',
  },
  data: {
    title: '数据分析师',
    direction: '数据分析 / 业务',
    type: '应届 / 实习',
    summary: '用数据和看板回答业务问题，支撑活动与产品决策。',
    portrait: [
      { task: '多表取数与核对', direction: '数据分析师' },
      { task: '看板搭建', direction: '业务分析负责人' },
      { task: '业务复盘', direction: '数据策略' },
    ],
    requirements: [
      { name: 'SQL', level: 70, note: '日常取数的主力工具，要能自己写出多表查询' },
      { name: '可视化', level: 60, note: '把结论做成别人一眼能看懂的图' },
      { name: '业务分析', level: 60, note: '能把数据结论翻译成业务能落地的动作' },
    ],
    mySkills: [
      { name: 'SQL', level: 72 },
      { name: 'Excel', level: 66 },
    ],
    path: [
      { title: 'SQL 多表查询专项', gain: 8 },
      { title: '业务看板作品集', gain: 6 },
    ],
    advice: '先补齐一个完整的看板作品，再练习把结论讲成业务语言。',
  },
  fe: {
    title: '前端开发工程师',
    direction: '前端开发 / Web',
    type: '应届 / 实习',
    summary: '负责页面还原与交互实现，把设计稿变成稳定好用的界面。',
    portrait: [
      { task: '页面与组件开发', direction: '前端开发工程师' },
      { task: '类型化改造', direction: '前端负责人' },
      { task: '工程配置与构建', direction: '前端架构' },
    ],
    requirements: [
      { name: 'Vue3', level: 70, note: '岗位主要框架，要会组件拆分与状态管理' },
      { name: 'TypeScript', level: 65, note: '团队代码以 TS 为主，要能读懂类型定义' },
      { name: 'Vite', level: 55, note: '开发与打包都靠它，要会基础配置' },
    ],
    mySkills: [
      { name: 'HTML / CSS', level: 80 },
      { name: 'JavaScript', level: 70 },
    ],
    path: [
      { title: 'Vue3 组件化实训', gain: 9 },
      { title: 'TypeScript 重构练习', gain: 6 },
    ],
    advice: '先补齐 Vue3 组件化，再把 TypeScript 用进现有项目。',
  },
  mini: {
    title: '小程序开发工程师',
    direction: '小程序 / 前端',
    type: '实习',
    summary: '在微信生态里开发小程序，完成页面、接口与发布上架。',
    portrait: [
      { task: '页面与组件开发', direction: '小程序开发工程师' },
      { task: '登录与支付接入', direction: '前端负责人' },
      { task: '提审与发版', direction: '全栈开发' },
    ],
    requirements: [
      { name: 'JavaScript', level: 68, note: '小程序开发的基础语言' },
      { name: '小程序', level: 65, note: '熟悉页面生命周期、组件与分包' },
      { name: '接口联调', level: 60, note: '接口对不上时能自己定位是哪一端的问题' },
    ],
    mySkills: [
      { name: 'JavaScript', level: 70 },
      { name: 'HTML / CSS', level: 72 },
    ],
    path: [
      { title: '完整小程序发布实战', gain: 8 },
      { title: '接口联调专项', gain: 6 },
    ],
    advice: '先独立发布一个完整小程序，把接口联调和审核流程走一遍。',
  },
  full: {
    title: '全栈开发工程师',
    direction: '全栈开发 / 前后端',
    type: '应届 / 实习',
    summary: '前后端一起做，独立完成从页面到数据的完整功能。',
    portrait: [
      { task: '模块前端实现', direction: '全栈开发工程师' },
      { task: '服务端接口开发', direction: '技术负责人' },
      { task: '数据表设计', direction: '系统架构' },
    ],
    requirements: [
      { name: 'Vue3', level: 65, note: '前端页面主要的实现方式' },
      { name: 'Node.js', level: 60, note: '用同一套语言写后端接口' },
      { name: 'MySQL', level: 60, note: '负责数据表设计与查询' },
    ],
    mySkills: [
      { name: 'Vue3', level: 62 },
      { name: 'MySQL', level: 55 },
    ],
    path: [
      { title: '一个小功能端到端实战', gain: 8 },
      { title: 'Node.js 接口实训', gain: 6 },
    ],
    advice: '先挑一个小功能端到端做完，把前端、接口和数据库连起来。',
  },
  java: {
    title: 'Java 后端开发工程师',
    direction: '后端开发 / 服务端',
    type: '应届 / 实习',
    summary: '用 Java 技术栈开发业务系统，负责接口、数据与稳定性。',
    portrait: [
      { task: '业务接口开发', direction: 'Java 开发工程师' },
      { task: '服务模块搭建', direction: '后端负责人' },
      { task: '数据层优化', direction: '服务架构' },
    ],
    requirements: [
      { name: 'Java', level: 70, note: '岗位主要语言，要熟悉集合与并发基础' },
      { name: 'Spring Boot', level: 68, note: '业务服务都用它搭，要能独立写一个模块' },
      { name: 'MySQL', level: 62, note: '要会建表、写查询并看懂执行计划' },
    ],
    mySkills: [
      { name: 'Java', level: 74 },
      { name: 'MySQL', level: 58 },
    ],
    path: [
      { title: 'Spring Boot 项目实战', gain: 9 },
      { title: 'Redis 缓存专项', gain: 6 },
    ],
    advice: '先完成一个 Spring Boot 项目，再补 Redis 缓存与接口优化。',
  },
  micro: {
    title: '微服务开发工程师',
    direction: '后端开发 / 微服务',
    type: '实习',
    summary: '把系统拆成多个服务，负责服务拆分、调用与治理。',
    portrait: [
      { task: '服务拆分与开发', direction: '微服务开发工程师' },
      { task: '缓存与调用治理', direction: '后端负责人' },
      { task: '容器化部署', direction: '系统架构' },
    ],
    requirements: [
      { name: 'Spring Cloud', level: 65, note: '服务拆分与治理的核心组件' },
      { name: 'Redis', level: 60, note: '缓存与分布式场景里最常用的一环' },
      { name: 'Docker', level: 55, note: '本地和线上都靠容器跑服务' },
    ],
    mySkills: [
      { name: 'Java', level: 70 },
      { name: 'MySQL', level: 58 },
    ],
    path: [
      { title: '单体项目拆分实战', gain: 8 },
      { title: 'Docker 部署专项', gain: 6 },
    ],
    advice: '先补 Spring Cloud 基础，再把一个单体项目拆成两个服务。',
  },
  'test-dev': {
    title: '测试开发工程师',
    direction: '测试开发 / 质量',
    type: '实习',
    summary: '写自动化测试与测试工具，帮团队把质量卡在发布之前。',
    portrait: [
      { task: '自动化脚本编写', direction: '测试开发工程师' },
      { task: '接口用例建设', direction: '质量负责人' },
      { task: '流水线接入', direction: '工程效能' },
    ],
    requirements: [
      { name: 'Python', level: 65, note: '写自动化脚本与测试工具的语言' },
      { name: '接口自动化', level: 62, note: '把回归测试交给脚本，而不是手点' },
      { name: 'Jenkins', level: 55, note: '把测试接到流水线上自动跑' },
    ],
    mySkills: [
      { name: 'Python', level: 74 },
      { name: '用例设计', level: 66 },
    ],
    path: [
      { title: '接口自动化框架实战', gain: 8 },
      { title: 'CI 流水线搭建', gain: 6 },
    ],
    advice: '先用 Python 搭一套接口自动化用例，再接到流水线上跑。',
  },
  algo: {
    title: '算法工程师',
    direction: '算法工程 / 模型',
    type: '实习',
    summary: '用模型解决业务问题，负责数据、训练与效果评估。',
    portrait: [
      { task: '模型训练', direction: '算法工程师' },
      { task: '数据清洗与特征', direction: '算法负责人' },
      { task: '效果调优与上线', direction: 'AI 技术专家' },
    ],
    requirements: [
      { name: 'PyTorch', level: 70, note: '训练与推理的主要框架' },
      { name: '数据处理', level: 65, note: '数据质量直接决定模型效果' },
      { name: '模型调优', level: 60, note: '指标不对时要知道该改哪里' },
    ],
    mySkills: [
      { name: 'Python', level: 76 },
      { name: '数学基础', level: 72 },
    ],
    path: [
      { title: 'PyTorch 训练实战', gain: 9 },
      { title: '公开数据集完整实验', gain: 7 },
    ],
    advice: '先把 PyTorch 训练流程跑通，再用一个公开数据集做完整实验。',
  },
  ops: {
    title: '数据运营',
    direction: '数据运营 / 业务',
    type: '应届 / 实习',
    summary: '围绕业务指标做数据跟踪与运营动作，推动数据驱动决策。',
    portrait: [
      { task: '日常取数与复盘', direction: '数据运营' },
      { task: '指标体系搭建', direction: '运营负责人' },
      { task: '活动策略调整', direction: '增长策略' },
    ],
    requirements: [
      { name: 'SQL', level: 65, note: '自己取数，不依赖别人排期' },
      { name: '指标体系', level: 60, note: '把零散数据组织成能看的指标' },
      { name: '活动运营', level: 55, note: '结合活动节奏给出数据结论' },
    ],
    mySkills: [
      { name: 'SQL', level: 70 },
      { name: 'Excel', level: 68 },
    ],
    path: [
      { title: '指标体系搭建练习', gain: 8 },
      { title: '活动数据复盘实战', gain: 6 },
    ],
    advice: '先补指标体系搭建方法，再独立完成一次活动数据复盘。',
  },
  qa: {
    title: '软件测试工程师',
    direction: '软件测试 / 质量',
    type: '应届 / 实习',
    summary: '负责功能与性能测试，保证上线质量。',
    portrait: [
      { task: '用例设计与执行', direction: '软件测试工程师' },
      { task: '数据校验', direction: '测试负责人' },
      { task: '问题定位', direction: '质量保障' },
    ],
    requirements: [
      { name: '用例设计', level: 65, note: '保证每个功能点都被覆盖到' },
      { name: 'SQL', level: 58, note: '查数据验证结果是否正确' },
      { name: '抓包分析', level: 55, note: '定位问题出在前端还是后端' },
    ],
    mySkills: [
      { name: '用例设计', level: 72 },
      { name: '测试基础', level: 70 },
    ],
    path: [
      { title: '用例设计专项', gain: 8 },
      { title: '抓包定位实战', gain: 6 },
    ],
    advice: '先补性能测试与抓包分析，再尝试写一部分自动化用例。',
  },
  sre: {
    title: '运维开发工程师',
    direction: '运维开发 / 平台',
    type: '实习',
    summary: '负责服务部署与稳定性，把上线和运维做成自动化。',
    portrait: [
      { task: '服务部署与维护', direction: '运维开发工程师' },
      { task: '自动化脚本', direction: 'SRE 负责人' },
      { task: '容器化与监控', direction: '平台工程' },
    ],
    requirements: [
      { name: 'Linux', level: 65, note: '服务器上的日常操作基础' },
      { name: 'Shell', level: 58, note: '批量处理与自动化脚本' },
      { name: 'Docker', level: 60, note: '服务打包与部署都靠它' },
    ],
    mySkills: [
      { name: 'Linux', level: 60 },
      { name: 'Shell', level: 50 },
    ],
    path: [
      { title: 'Docker 部署实战', gain: 8 },
      { title: 'Shell 自动化脚本练习', gain: 6 },
    ],
    advice: '先把 Linux 常用命令练熟，再用 Docker 部署一个自己的服务。',
  },
  pm: {
    title: '产品经理',
    direction: '产品经理 / 业务',
    type: '应届 / 实习',
    summary: '定义做什么和为什么做，推动需求从想法走到上线。',
    portrait: [
      { task: '需求梳理', direction: '产品经理' },
      { task: '原型与方案', direction: '产品负责人' },
      { task: '上线数据复盘', direction: '业务负责人' },
    ],
    requirements: [
      { name: '需求分析', level: 68, note: '判断做什么、不做什么' },
      { name: '原型', level: 60, note: '把想法画成能评审的图' },
      { name: '数据分析', level: 58, note: '用数据验证上线效果' },
    ],
    mySkills: [
      { name: '需求分析', level: 72 },
      { name: '文档撰写', level: 70 },
    ],
    path: [
      { title: '完整需求文档实战', gain: 8 },
      { title: '原型工具专项', gain: 6 },
    ],
    advice: '先补原型与数据分析，再完整跟一个小功能的落地过程。',
  },
  ux: {
    title: '交互设计师',
    direction: '交互设计 / 体验',
    type: '实习',
    summary: '设计用户怎么用产品，负责流程、原型与交互细节。',
    portrait: [
      { task: '流程梳理', direction: '交互设计师' },
      { task: '用户访谈与测试', direction: '设计负责人' },
      { task: '交互稿交付', direction: '体验设计' },
    ],
    requirements: [
      { name: '原型', level: 65, note: '把流程落成可点击的稿子' },
      { name: '用户研究', level: 60, note: '知道用户真正卡在哪一步' },
      { name: '交互稿', level: 58, note: '交付给开发和视觉的最终说明' },
    ],
    mySkills: [
      { name: '原型', level: 64 },
      { name: '视觉基础', level: 60 },
    ],
    path: [
      { title: '完整交互稿实战', gain: 8 },
      { title: '可用性测试专项', gain: 6 },
    ],
    advice: '先补用户研究方法，再独立完成一次完整交互稿。',
  },
}

export function readStoredTargetJob() {
  try {
    return localStorage.getItem(TARGET_JOB_STORAGE_KEY) || DEFAULT_TARGET_JOB
  } catch {
    return DEFAULT_TARGET_JOB
  }
}

/* 目标岗位（JOB_PROFILES）与岗位详情（JOB_DETAILS）用下面这张表对应：
 * 「AI 应用工程师」与详情里的「AI 应用开发工程师」是同一个方向，其余同名。 */
const TARGET_JOB_DETAIL_ID = {
  'Python 开发工程师': 'py',
  前端开发工程师: 'fe',
  'Java 后端开发工程师': 'java',
  'AI 应用工程师': 'ai-app',
  算法工程师: 'algo',
  数据分析师: 'data',
  软件测试工程师: 'qa',
  产品经理: 'pm',
}

/** 目标岗位对应的岗位详情 id，没有对应详情时返回空。 */
export function getJobDetailId(title) {
  return TARGET_JOB_DETAIL_ID[title] || null
}

/** 目标岗位的岗位方向，取自岗位详情里同一份定义。 */
export function getJobDirection(title) {
  return JOB_DETAILS[getJobDetailId(title)]?.direction || ''
}

/** 取某个目标岗位的展示数据，找不到时给出空壳，页面按「尚未完成岗位体检」展示。 */
export function getTargetProfile(title) {
  const profile = JOB_PROFILES.find((item) => item.title === title)
  if (profile) return profile
  return {
    title,
    matchRate: null,
    mastered: [],
    toImprove: [],
    gaps: [],
    advice: '完成岗位体检后，这里会显示匹配度、能力差距与提升建议。',
    fit: [],
  }
}

/** 岗位名、方向、类型、核心技能取自 JOB_DETAILS，匹配度与「与我相关」的字段取自目标岗位。 */
export function resolveFitJobs(profile) {
  return (profile?.fit || []).map((item) => {
    const detail = JOB_DETAILS[item.id]
    return {
      id: item.id,
      matchRate: item.matchRate,
      strength: item.strength,
      improve: item.improve,
      title: detail?.title || item.id,
      direction: detail?.direction || '',
      type: detail?.type || '',
      skills: (detail?.requirements || []).map((requirement) => requirement.name),
    }
  })
}

/** 岗位详情页：岗位要求与我的能力相减，得到「我还缺什么」。 */
export function buildSkillGaps(detail) {
  if (!detail) return []
  const mine = new Map(detail.mySkills.map((skill) => [skill.name, skill.level]))
  return detail.requirements.map((requirement) => {
    const current = mine.get(requirement.name) ?? 0
    return {
      name: requirement.name,
      current,
      required: requirement.level,
      reached: current >= requirement.level,
      diff: Math.max(0, requirement.level - current),
    }
  })
}

export function getJobDetail(jobId) {
  return JOB_DETAILS[String(jobId || '')] || null
}

/** 岗位画像三列：做什么 / 用什么 / 发展方向，其中「用什么」直接取岗位要求的技能，顺序一一对应。 */
export function buildPortraitRows(detail) {
  const requirements = detail?.requirements || []
  return (detail?.portrait || []).map((row, index) => ({
    task: row.task,
    tool: requirements[index]?.name || '',
    direction: row.direction,
  }))
}

/** 岗位卡/详情页显示的匹配度：优先用当前目标岗位的匹配结果，目标岗位没有这项数据时退回它最高的一条。 */
export function getJobMatchRate(jobId, targetTitle) {
  const current = resolveFitJobs(getTargetProfile(targetTitle)).find((item) => item.id === jobId)
  if (current) return current.matchRate
  const rates = JOB_PROFILES.flatMap((profile) => resolveFitJobs(profile))
    .filter((item) => item.id === jobId)
    .map((item) => item.matchRate)
  return rates.length ? Math.max(...rates) : null
}
