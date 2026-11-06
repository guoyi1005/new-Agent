<template>
  <div class="page-layout interview-workspace">
    <Sidebar />
    <main class="main-content">
      <EditorialToolbar
        v-model="dashboardSearch"
        eyebrow="CAREER / 02"
        placeholder="搜索真题、技术方向或面试趋势"
      />
      <div class="content-area">
        <!-- 左侧主要内容区 -->
        <div class="main-body">
          <section class="interview-hero">
            <div class="interview-hero__copy">
              <span class="interview-hero__eyebrow">AI INTERVIEW STUDIO</span>
              <h1>智能模拟与职业准备</h1>
              <p>从简历准备到模拟训练，再到能力反馈，在一个工作台完成每一步。</p>
            </div>
            <div class="interview-hero__actions">
              <button class="hero-action hero-action--primary" type="button" @click="router.push(PATHS.AI_MOCK_INTERVIEW)">
                <span>开始模拟面试</span>
                <b>→</b>
              </button>
              <button class="hero-action hero-action--resume" type="button" @click="router.push('/ai-tools/resume')">
                <small>RESUME</small>
                <span>{{ resume.name || '我的简历' }}</span>
                <b>查看与完善 →</b>
              </button>
            </div>
          </section>

          <!-- 顶部技术栈选择器 -->
          <div class="tech-stack-header">
            <!-- 第一层：大方向 -->
            <div class="selector-row">
              <span class="selector-label">方向</span>
              <div class="selector-options">
                <button
                  v-for="cat in categoryOptions"
                  :key="cat"
                  class="selector-btn"
                  :class="{ active: selectedCategory === cat }"
                  @click="selectCategory(cat)"
                >
                  {{ cat }}
                </button>
              </div>
            </div>
            <!-- 第二层：细分岗位 -->
            <div class="selector-row" v-if="selectedCategory || selectedTargetPosition">
              <span class="selector-label">岗位</span>
              <div class="selector-options">
                <button
                  v-for="(pos, idx) in filteredTargetPositions"
                  :key="`pos-${idx}-${pos}`"
                  class="selector-btn"
                  :class="{ active: selectedTargetPosition === pos }"
                  @click="selectTargetPosition(pos)"
                >
                  {{ pos }}
                </button>
                <span v-if="filteredTargetPositions.length === 0" class="no-data">
                  暂无可选岗位
                </span>
              </div>
            </div>
          </div>

          <!-- 技术栈：仅保留已掌握 -->
          <div class="tech-tags-section">
            <div class="tech-tags-row">
              <span class="tech-tag mastered">已掌握</span>
              <div class="tech-tags-items">
                <span class="tech-tag-item active" v-for="(tech, idx) in masteredTechStacks" :key="`m-${idx}-${tech}`">{{ tech }}</span>
                <span class="tech-empty" v-if="masteredTechStacks.length === 0">暂无已掌握技术栈</span>
              </div>
            </div>
          </div>

          <!-- 三列统计卡片 -->
          <div class="stats-row">
            <div class="stat-card">
              <div class="stat-card-label">当前评分</div>
              <div class="stat-card-value">
                <span class="stat-big">85</span>
                <span class="stat-unit">/ 100</span>
              </div>
              <div class="stat-trend up">
                <span class="trend-icon">↗</span>
                <span>较上周 +5%</span>
              </div>
            </div>
            <div class="stat-card">
              <div class="stat-card-label">竞争力评估</div>
              <div class="stat-card-value">
                <span class="stat-big">良好</span>
              </div>
              <div class="stat-subtitle">超过 72% 同届毕业生</div>
            </div>
            <div class="stat-card">
              <div class="stat-card-label">掌握知识点</div>
              <div class="stat-card-value">
                <span class="stat-big">1,204</span>
              </div>
              <div class="progress-bar">
                <div class="progress-fill" style="width: 82%"></div>
              </div>
              <div class="stat-subtitle"></div>
            </div>
          </div>

           <!-- 能力分析图表区 -->
          <div class="charts-row">
            <!-- 面试能力维度图 -->
            <div class="chart-card radar-card">
              <div class="chart-header">
                <h3 class="chart-title">面试能力维度</h3>
              </div>
              <div class="radar-chart-container">
                <svg class="radar-chart" viewBox="0 0 200 200">
                  <!-- 背景网格 -->
                  <polygon
                    v-for="scale in [0.2, 0.4, 0.6, 0.8, 1]"
                    :key="scale"
                    :points="radarGridPoints(scale)"
                    fill="none"
                    stroke="#2d3a4f"
                    stroke-width="0.5"
                  />
                  <!-- 轴线 -->
                  <line
                    v-for="(axis, i) in radarAxes"
                    :key="i"
                    :x1="100"
                    :y1="100"
                    :x2="axis.x"
                    :y2="axis.y"
                    stroke="#2d3a4f"
                    stroke-width="0.5"
                  />
                  <!-- 数据区域 -->
                  <polygon
                    :points="radarDataPoints"
                    fill="rgba(58, 123, 200, 0.3)"
                    stroke="#3a7bc8"
                    stroke-width="2"
                  />
                  <!-- 数据点 -->
                  <circle
                    v-for="(coord, i) in radarDataCoords"
                    :key="i"
                    :cx="coord.x"
                    :cy="coord.y"
                    r="4"
                    fill="#3a7bc8"
                  />
                  <!-- 标签 -->
                  <text
                    v-for="(axis, i) in radarAxes"
                    :key="`label-${i}`"
                    :x="axis.lx"
                    :y="axis.ly"
                    text-anchor="middle"
                    dominant-baseline="middle"
                    fill="#9ca3af"
                    font-size="10"
                  >{{ axis.label }}</text>
                  <!-- 数值 -->
                  <text
                    v-for="(coord, i) in radarDataCoords"
                    :key="`val-${i}`"
                    :x="coord.x + radarValueOffset(i).dx"
                    :y="coord.y + radarValueOffset(i).dy"
                    text-anchor="middle"
                    dominant-baseline="middle"
                    fill="#ffffff"
                    font-size="8"
                    font-weight="500"
                  >{{ radarValueAt(i) }}</text>
                </svg>
              </div>
            </div>

            <!-- 核心能力分布图表 -->
            <div class="chart-card">
              <div class="chart-header">
                <h3 class="chart-title">核心能力分布</h3>
                <div class="chart-legend">
                  <span class="legend-item">
                    <span class="legend-dot blue"></span>
                    我的能力
                  </span>
                  <span class="legend-item">
                    <span class="legend-dot gray"></span>
                    行业基准
                  </span>
                </div>
              </div>
              <div class="ability-chart">
                <div class="chart-bars">
                  <div class="bar-item" v-for="(item, index) in abilityData" :key="index">
                    <div class="bar-wrapper">
                      <div class="bar-bg" :style="{ height: (item.benchmark || 85) + '%' }"></div>
                      <div class="bar-fill" :style="{ height: item.value + '%' }"></div>
                    </div>
                    <div class="bar-label">{{ item.name }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="plan-section">
            <div class="plan-header">
              <h3 class="plan-title">下一步提升计划</h3>
              <span class="plan-subtitle">基于当前能力评估生成</span>
            </div>
            <div class="plan-grid">
              <div class="plan-card" v-for="plan in improvementPlans" :key="plan.title">
                <div class="plan-card-top">
                  <div class="plan-card-title">{{ plan.title }}</div>
                  <div class="plan-progress-text">{{ plan.progress }}%</div>
                </div>
                <div class="plan-progress">
                  <div class="plan-progress-fill" :style="{ width: `${plan.progress}%` }"></div>
                </div>
                <ul class="plan-tasks">
                  <li v-for="(task, idx) in plan.tasks" :key="`${plan.title}-${idx}`">{{ task }}</li>
                </ul>
              </div>
            </div>
          </div>



        </div>

        <!-- 右侧侧边栏 -->
        <aside class="right-sidebar">
          <!-- 1. 行业热门真题 -->
          <div class="sidebar-section hot-questions">
            <div class="section-header">
              <img class="section-icon" src="@/assets/interview/11.png" alt="真题" />
              <span class="section-title">行业热门真题</span>
            </div>
            <div class="question-list" :class="{ 'fading': questionFading }">
              <div 
                class="question-item" 
                v-for="q in paginatedQuestions" 
                :key="q.source + q.title"
              >
                <div class="question-source">{{ q.source }}</div>
                <div class="question-title">{{ q.title }}</div>
                <div class="question-tags">
                  <span class="tag" v-for="(tag, i) in q.tags" :key="i">{{ tag }}</span>
                </div>
              </div>
            </div>
            <div class="pagination">
              <span class="page-dots">
                <span 
                  class="dot" 
                  v-for="i in questionPageCount" 
                  :key="i"
                  :class="{ active: i - 1 === questionPage }"
                ></span>
              </span>
            </div>
          </div>

          <!-- 2. 2024 面试趋势 -->
          <div class="sidebar-section trends">
            <div class="section-header">
              <img class="section-icon" src="@/assets/interview/10.png" alt="趋势" />
              <span class="section-title">2026 面试趋势</span>
            </div>
            <div class="trend-list" :class="{ 'fading': trendFading }">
              <div 
                class="trend-item-wrapper" 
                v-for="trend in paginatedTrends" 
                :key="trend.highlight + trend.text"
              >
                <div class="trend-item">
                  <span class="trend-dot">●</span>
                  <div class="trend-content">
                    <span class="trend-highlight">{{ trend.highlight }}：</span>
                    <span class="trend-text">{{ trend.text }}</span>
                  </div>
                </div>
              </div>
            </div>
            <div class="pagination">
              <span class="page-dots">
                <span 
                  class="dot" 
                  v-for="i in trendPageCount" 
                  :key="i"
                  :class="{ active: i - 1 === trendPage }"
                ></span>
              </span>
            </div>
          </div>
        </aside>
      </div>
    </main>
  </div>

  <!-- ───── 编辑资料弹窗 ───── -->
  <Teleport to="body">
    <div v-if="editVisible" class="edit-overlay" @click.self="editVisible = false">
      <div class="edit-modal">
        <div class="edit-header">
          <span class="edit-title">编辑资料</span>
          <button class="edit-close" @click="editVisible = false">✕</button>
        </div>
        <div class="edit-body">
          <div class="edit-grid">
            <div class="form-item">
              <label>昵称</label>
              <input v-model="editForm.nickname" type="text" placeholder="请输入昵称" />
            </div>
            <div class="form-item">
              <label>手机号</label>
              <input v-model="editForm.phone" type="text" placeholder="请输入手机号" />
            </div>
            <div class="form-item">
              <label>邮箱</label>
              <input v-model="editForm.email" type="email" placeholder="请输入邮箱" />
            </div>
            <div class="form-item">
              <label>性别</label>
              <select v-model="editForm.gender">
                <option :value="0">未知</option>
                <option :value="1">男</option>
                <option :value="2">女</option>
              </select>
            </div>
            <div class="form-item">
              <label>学历</label>
              <select v-model="editForm.education">
                <option value="highschool">高中</option>
                <option value="junior_college">大专</option>
                <option value="bachelor">本科</option>
                <option value="master">硕士</option>
                <option value="doctor">博士</option>
                <option value="other">其他</option>
              </select>
            </div>
            <div class="form-item">
              <label>工作年限</label>
              <input v-model.number="editForm.work_experience_years" type="number" min="0" placeholder="0" />
            </div>
            <div class="form-item">
              <label>专业</label>
              <input v-model="editForm.major" type="text" placeholder="请输入专业" />
            </div>
            <div class="form-item">
              <label>毕业年份</label>
              <input v-model.number="editForm.graduation_year" type="number" min="2000" max="2035" placeholder="2025" />
            </div>
            <div class="form-item">
              <label>学校</label>
              <input v-model="editForm.school" type="text" placeholder="请输入学校名称" />
            </div>
            <div class="form-item">
              <label>目标岗位</label>
              <input v-model="editForm.target_position" type="text" placeholder="如：前端工程师" />
            </div>
            <div class="form-item form-item-full">
              <label>技能标签 <span class="form-hint">（英文逗号分隔）</span></label>
              <input v-model="editForm.skill_tags" type="text" placeholder="如：Vue3, TypeScript, React" />
            </div>
          </div>
          <p v-if="editError" class="edit-error">{{ editError }}</p>
        </div>
        <div class="edit-footer">
          <button class="btn-outline" @click="editVisible = false">取消</button>
          <button class="btn-primary" :disabled="editSaving" @click="saveProfile">
            {{ editSaving ? '保存中...' : '保存' }}
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, reactive, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import Sidebar from '../components/Sidebar.vue'
import EditorialToolbar from '../../../components/EditorialToolbar.vue'
import { PATHS } from '../routes/paths'
import { conversationApi, type ConversationItem } from '../api/conversation'
import { authApi, type InterviewJobPositionItem } from '../api/auth'

const router = useRouter()
const dashboardSearch = ref('')

// ───── 用户基本信息 ─────
const user = ref({
  nickname: '-',
  avatar: 'https://api.dicebear.com/7.x/adventurer/svg?seed=default',
  phone: '-',
  email: '-',
  identity: '-',
  targetJob: '-',
  graduationYear: '-',
  workYears: '-',
  skills: [] as string[],
})

const resume = ref({ name: '', uploadTime: '' })
const jobPositionOptions = ref<InterviewJobPositionItem[]>([])

// ───── AI面试能力数据 ─────
const stats = ref({
  totalInterviews: 0,
  lastScore: 0,
  avgScore: 0,
  strength: '专业知识、逻辑表达',
  weakness: '应变能力、压力问答',
})

// 雷达图维度
const radarDimensions = [
  { label: '表达力', value: 82 },
  { label: '逻辑性', value: 75 },
  { label: '专业度', value: 88 },
  { label: '应变力', value: 65 },
  { label: '综合', value: 78 },
]
const RADAR_CENTER = 100
const RADAR_RADIUS = 65

const radarAngles: number[] = radarDimensions.map((_, i) =>
  ((-90 + i * (360 / radarDimensions.length)) * Math.PI) / 180
)

const radarAxes = computed(() =>
  radarDimensions.map((d, i) => {
    const a: number = radarAngles[i] ?? 0
    return {
      x: RADAR_CENTER + RADAR_RADIUS * Math.cos(a),
      y: RADAR_CENTER + RADAR_RADIUS * Math.sin(a),
      lx: RADAR_CENTER + (RADAR_RADIUS + 20) * Math.cos(a),
      ly: RADAR_CENTER + (RADAR_RADIUS + 20) * Math.sin(a),
      label: d.label,
    }
  })
)

const radarGridPoints = (scale: number) =>
  radarAngles
    .map(a => `${RADAR_CENTER + RADAR_RADIUS * scale * Math.cos(a)},${RADAR_CENTER + RADAR_RADIUS * scale * Math.sin(a)}`)
    .join(' ')

const radarDataCoords = computed(() =>
  radarDimensions.map((d, i) => {
    const s = d.value / 100
    const a: number = radarAngles[i] ?? 0
    return { x: RADAR_CENTER + RADAR_RADIUS * s * Math.cos(a), y: RADAR_CENTER + RADAR_RADIUS * s * Math.sin(a) }
  })
)

const radarDataPoints = computed(() =>
  radarDataCoords.value.map(p => `${p.x},${p.y}`).join(' ')
)

const radarValueOffset = (i: number) => {
  const a: number = radarAngles[i] ?? 0
  return { dx: 12 * Math.cos(a), dy: 12 * Math.sin(a) }
}

const radarValueAt = (i: number) => (radarDimensions[i]?.value ?? 0)

// ───── 成长曲线 ─────
const scoreHistory = ref([
  { date: '1/10', score: 62 },
  { date: '1/18', score: 70 },
  { date: '1/25', score: 68 },
  { date: '2/03', score: 74 },
  { date: '2/10', score: 80 },
  { date: '2/18', score: 78 },
  { date: '2/25', score: 85 },
  { date: '3/04', score: 88 },
])

const lineDataCoords = computed(() => {
  const n = scoreHistory.value.length
  const xStart = 50, xEnd = 405
  const yTop = 30, yBottom = 148
  return scoreHistory.value.map((item, i) => ({
    x: xStart + (i / (n - 1)) * (xEnd - xStart),
    y: yBottom - (item.score / 100) * (yBottom - yTop),
  }))
})

const lineChartPoints = computed(() =>
  lineDataCoords.value.map(p => `${p.x},${p.y}`).join(' ')
)

const lineChartFillPoints = computed(() => {
  const pts = lineDataCoords.value
  if (!pts.length) return ''
  const first = pts[0]!
  const last = pts[pts.length - 1]!
  return [...pts.map(p => `${p.x},${p.y}`), `${last.x},148`, `${first.x},148`].join(' ')
})

// ───── 历史面试（拉取后端会话记录） ─────
type UIHistory = { id: number; title: string; date: string; duration: string; score: string; level: string }
const historyInterviews = ref<UIHistory[]>([])

// ───── 行业热门真题 ─────
const hotQuestions = ref([
  { source: '字节跳动 · 2025秋招', title: '如何设计一个支持千万级 QPS 的分布式限流器？', tags: ['高并发', '架构设计'] },
  { source: '阿里巴巴 · P7社招', title: '详细说明 MySQL MVCC 机制及其在 RR 隔离级别下的工作原理。', tags: ['数据库', '内核分析'] },
  { source: '腾讯 · 2025春招', title: '介绍 HashMap 的底层实现以及 JDK8 后的优化。', tags: ['Java', '集合源码'] },
  { source: '美团 · 技术岗', title: 'Redis 数据淘汰策略有哪些？Cache Aside 模式注意什么？', tags: ['Redis', '缓存'] },
  { source: '拼多多 · 校招', title: '讲解一下 HTTP 三次握手和四次挥手的过程及原因。', tags: ['计算机网络', '协议'] },
  { source: '快手 · 后端开发', title: 'MySQL 索引失效的常见原因有哪些？如何优化？', tags: ['数据库', '优化'] },
  { source: '滴滴 · 架构组', title: '如何在分布式系统中保证数据一致性？', tags: ['分布式', '一致性'] },
  { source: 'B站 · 基础设施', title: 'Docker 与 Kubernetes 的区别及各自适用场景。', tags: ['容器', 'DevOps'] },
  { source: '百度 · 搜索部门', title: '讲解一下 Redis 的持久化机制 RDB 和 AOF 的区别。', tags: ['Redis', '持久化'] },
  { source: '网易 · 游戏事业部', title: '介绍 Actor 模型与 Go CSP 的区别与联系。', tags: ['并发', 'Go'] },
  { source: '京东 · 物流研发', title: '讲解一下 MySQL 中 InnoDB 的 Buffer Pool 工作原理。', tags: ['数据库', ' InnoDB'] },
  { source: '蔚来 · 车联网', title: '介绍消息队列如何保证不丢消息？', tags: ['消息队列', '中间件'] },
  { source: '米哈游 · 后端开发', title: '讲解一下游戏服务器架构设计要点。', tags: ['游戏', '架构'] },
  { source: '小红书 · 基础架构', title: '介绍一下 K8s 中 Pod 的生命周期。', tags: ['K8s', '容器'] },
  { source: '得物 · 技术中台', title: '讲解一下 CAP 定理与 BASE 理论。', tags: ['分布式', '理论'] },
])

const questionPage = ref(0)
const questionsPerPage = 3
const filteredHotQuestions = computed(() => {
  const query = dashboardSearch.value.trim().toLowerCase()
  if (!query) return hotQuestions.value
  return hotQuestions.value.filter((item) => {
    return `${item.source} ${item.title} ${item.tags.join(' ')}`.toLowerCase().includes(query)
  })
})

const questionPageCount = computed(() => Math.max(1, Math.ceil(filteredHotQuestions.value.length / questionsPerPage)))

const paginatedQuestions = computed(() => {
  const start = questionPage.value * questionsPerPage
  return filteredHotQuestions.value.slice(start, start + questionsPerPage)
})

const nextQuestionPage = () => {
  questionPage.value = (questionPage.value + 1) % questionPageCount.value
}

// ───── 面试趋势 ─────
const interviewTrends = ref([
  { highlight: 'AI工具链', text: '大模型(LLM)赋能研发流程成为新热点。' },
  { highlight: '降本增效', text: '更侧重对系统性能极致优化的考察。' },
  { highlight: '全栈意识', text: '后端更需理解云原生与前端交互成本。' },
  { highlight: '项目深度', text: '不仅考察技术实现，更注重业务价值与系统设计能力。' },
  { highlight: '场景题增多', text: '从八股文转向实际业务场景，考察问题分析与解决能力。' },
  { highlight: '基础为王', text: '计算机基础（操作系统、网络、数据结构）仍是必考项。' },
  { highlight: '八股文弱化', text: '不再死记硬背，更考验理解与实际应用。' },
  { highlight: '项目为王', text: '深入考察项目中的技术选型、难点与优化方案。' },
  { highlight: '云原生', text: '容器化、微服务架构成为后端面试必备知识。' },
  { highlight: '工程实践', text: 'CI/CD、监控告警等运维知识受到重视。' },
  { highlight: '软技能', text: '沟通表达、团队协作、跨部门协调能力同样重要。' },
  { highlight: '技术广度', text: '除后端技术外，前端、大数据、安全等领域知识加分。' },
  { highlight: '架构思维', text: '能从架构角度思考问题，具备长期演进意识。' },
  { highlight: '学习能力', text: '考察候选人学习新技术、适应业务变化的能力。' },
  { highlight: '代码质量', text: '代码规范、单元测试、Code Review 意识受关注。' },
])

const trendPage = ref(0)
const trendsPerPage = 5
const trendPageCount = computed(() => Math.ceil(interviewTrends.value.length / trendsPerPage))

const paginatedTrends = computed(() => {
  const start = trendPage.value * trendsPerPage
  return interviewTrends.value.slice(start, start + trendsPerPage)
})

const nextTrendPage = () => {
  trendPage.value = (trendPage.value - 1 + trendPageCount.value) % trendPageCount.value
}

// 自动轮播
let questionAutoPlay: ReturnType<typeof setInterval> | null = null
let trendAutoPlay: ReturnType<typeof setInterval> | null = null

// 渐变动画控制
const questionFading = ref(false)
const trendFading = ref(false)

const fadeToNextQuestion = () => {
  questionFading.value = true
  setTimeout(() => {
    nextQuestionPage()
    questionFading.value = false
  }, 300)
}

const fadeToNextTrend = () => {
  trendFading.value = true
  setTimeout(() => {
    nextTrendPage()
    trendFading.value = false
  }, 300)
}

onMounted(() => {
  questionAutoPlay = setInterval(fadeToNextQuestion, 3500)
  trendAutoPlay = setInterval(fadeToNextTrend, 3500)
})

onUnmounted(() => {
  if (questionAutoPlay) clearInterval(questionAutoPlay)
  if (trendAutoPlay) clearInterval(trendAutoPlay)
})

// ───── 核心能力分布数据 ─────
const abilityData = ref([
  { name: '算法', value: 65, benchmark: 85 },
  { name: '架构', value: 78, benchmark: 90 },
  { name: '并发', value: 85, benchmark: 88 },
  { name: '源码', value: 45, benchmark: 70 },
  { name: '工程', value: 92, benchmark: 80 },
  { name: '协作', value: 70, benchmark: 85 },
  { name: '安全', value: 68, benchmark: 82 },
])

// ───── 编辑资料弹窗 ─────
const improvementPlans = ref([
  {
    title: '短板突破',
    progress: 42,
    tasks: ['2 道场景追问题复盘', '源码专项 45 分钟', '输出 1 份答题模板'],
  },
  {
    title: '本周目标',
    progress: 67,
    tasks: ['完成 2 次模拟面试', '每次整理 3 条改进点', '补齐 1 个项目亮点故事'],
  },
  {
    title: '进度追踪',
    progress: 78,
    tasks: ['连续训练 5 天', '本周完成率 78%', '下周目标分数 88+'],
  },
])

const editVisible = ref(false)
const editSaving = ref(false)
const editError = ref('')
const editForm = reactive({
  nickname: '',
  phone: '',
  email: '',
  gender: 0,
  education: 'bachelor',
  work_experience_years: 0,
  major: '',
  graduation_year: null as number | null,
  school: '',
  target_position: '',
  skill_tags: '',
})

const USER_PROFILE_CACHE_KEY = 'user_profile_detail_cache'
const INDEX_SELECTED_POSITION_CACHE_KEY = 'index_selected_position_cache'

// 保存原始 profile，用于表单回填
const rawProfile = ref<any>(null)

// ───── 大方向与细分岗位选择 ─────
const categoryOptions = ['开发', '运维', '测试', '产品', '设计', '其他']
const selectedCategory = ref('')
const selectedTargetPosition = ref('')

// 从数据库岗位列表获取所有细分岗位
const targetPositionOptions = computed(() => {
  const arr = jobPositionOptions.value.map(item => item.job_position).filter(Boolean)
  return Array.from(new Set(arr))
})

// 根据大方向筛选细分岗位
const filteredTargetPositions = computed(() => {
  if (!selectedCategory.value) return targetPositionOptions.value
  return targetPositionOptions.value.filter(pos => getCategory(pos) === selectedCategory.value)
})

// 大方向转细分岗位的映射函数
function getCategory(pos: string): string {
  const p = pos || ''
  if (p.includes('前端') || p.includes('后端') || p.includes('全栈') || p.includes('开发') || p.includes('工程师')) return '开发'
  if (p.includes('运维')) return '运维'
  if (p.includes('测试')) return '测试'
  if (p.includes('产品')) return '产品'
  if (p.includes('设计')) return '设计'
  return '其他'
}

const activeTargetPosition = computed(() => {
  const v = String(selectedTargetPosition.value || '').trim()
  return v || ''
})

function saveSelectedPositionCache() {
  try {
    localStorage.setItem(INDEX_SELECTED_POSITION_CACHE_KEY, JSON.stringify({
      userId: Number(localStorage.getItem('user_id') || 0),
      category: selectedCategory.value,
      targetPosition: selectedTargetPosition.value,
    }))
  } catch (e) {
    console.warn('[Index] save selected position cache failed', e)
  }
}

function restoreSelectedPositionCache() {
  try {
    const raw = localStorage.getItem(INDEX_SELECTED_POSITION_CACHE_KEY)
    if (!raw) return false
    const cached = JSON.parse(raw)
    const currentUserId = Number(localStorage.getItem('user_id') || 0)
    const cachedUserId = Number(cached?.userId || 0)
    if (cachedUserId && currentUserId && cachedUserId !== currentUserId) {
      localStorage.removeItem(INDEX_SELECTED_POSITION_CACHE_KEY)
      return false
    }
    const targetPosition = String(cached?.targetPosition || '').trim()
    const category = String(cached?.category || '').trim()
    if (!targetPosition && !category) return false
    if (targetPosition && targetPositionOptions.value.length > 0 && !targetPositionOptions.value.includes(targetPosition)) {
      localStorage.removeItem(INDEX_SELECTED_POSITION_CACHE_KEY)
      return false
    }
    selectedTargetPosition.value = targetPosition
    selectedCategory.value = targetPosition ? getCategory(targetPosition) : category
    return true
  } catch (e) {
    console.warn('[Index] restore selected position cache failed', e)
    return false
  }
}

// 选择大方向
function selectCategory(category: string) {
  selectedCategory.value = category
  // 如果当前选中的岗位不属于新类别，重置
  if (selectedTargetPosition.value && getCategory(selectedTargetPosition.value) !== category) {
    selectedTargetPosition.value = ''
  }
  saveSelectedPositionCache()
}

// 选择细分岗位
function selectTargetPosition(position: string) {
  selectedTargetPosition.value = position
  // 同步更新大方向
  if (position) {
    selectedCategory.value = getCategory(position)
  }
  saveSelectedPositionCache()
}

// 清除选择
function _clearSelection() {
  selectedTargetPosition.value = ''
  selectedCategory.value = ''
  saveSelectedPositionCache()
}

function parseSkillList(value: unknown): string[] {
  return String(value || '')
    .replace(/\r\n/g, '\n')
    .split(/,|，|\n|\//)
    .map((s) => s.trim())
    .filter(Boolean)
}

const masteredTechStacks = computed(() => parseSkillList(user.value.skills.join(',')))

function applyProfileToUI(p: any) {
  rawProfile.value = p
  const expYears = p.work_experience_years ?? 0
  const targetPosition = (p.target_position || '').trim()
  user.value = {
    nickname: p.nickname || '-',
    avatar: p.avatar_url || `https://api.dicebear.com/7.x/adventurer/svg?seed=${encodeURIComponent(p.nickname || 'default')}`,
    phone: maskPhone(p.phone || ''),
    email: p.email || '',
    identity: expYears === 0 ? '应届生' : `${expYears} 年经验`,
    targetJob: targetPosition || '-',
    graduationYear: p.graduation_year ? String(p.graduation_year) : '-',
    workYears: expYears === 0 ? '在校实习' : `${expYears} 年工作经验`,
      skills: (p.tech_stack || p.skill_tags)
        ? String(p.tech_stack || p.skill_tags).split(',').map((s: string) => s.trim()).filter(Boolean)
        : [],
  }
  // 同步头像和昵称到 localStorage，供 Sidebar 读取
  if (p.avatar_url) {
    localStorage.setItem('avatar_url', p.avatar_url)
  }
  if (p.nickname) {
    localStorage.setItem('nickname', p.nickname)
  }
  stats.value.totalInterviews = p.interview_count ?? 0
  stats.value.avgScore = Math.round(p.average_score ?? 0)
  if (p.resume_url) {
    resume.value = { name: '我的简历', uploadTime: p.resume_url }
  }
  // 加载时同步大方向和细分岗位
  if (targetPosition && targetPositionOptions.value.includes(targetPosition)) {
    selectedTargetPosition.value = targetPosition
    selectedCategory.value = getCategory(targetPosition)
  }
  // 注意：不再 fallback 到第一个岗位，保留用户设置的 target_position
}

function saveProfileCache(p: any) {
  try {
    localStorage.setItem(USER_PROFILE_CACHE_KEY, JSON.stringify(p))
  } catch (e) {
    console.warn('[Index] save profile cache failed', e)
  }
}

function loadProfileFromCache(uid: number): boolean {
  try {
    const raw = localStorage.getItem(USER_PROFILE_CACHE_KEY)
    if (!raw) return false
    const cached = JSON.parse(raw)
    if (!cached || Number(cached.id || 0) !== uid) return false
    applyProfileToUI(cached)
    return true
  } catch {
    return false
  }
}

const _goMock = () => {
  router.push(PATHS.AI_MOCK_INTERVIEW)
}

const _goResume = () => {
  if (!rawProfile.value) return
  const p = rawProfile.value
  editForm.nickname = p.nickname || ''
  editForm.phone = p.phone || ''
  editForm.email = p.email || ''
  editForm.gender = p.gender ?? 0
  editForm.education = p.education || 'bachelor'
  editForm.work_experience_years = p.work_experience_years ?? 0
  editForm.major = p.major || ''
  editForm.graduation_year = p.graduation_year ?? null
  editForm.school = p.school || ''
  editForm.target_position = p.target_position || ''
  editForm.skill_tags = p.skill_tags || ''
  editError.value = ''
  editVisible.value = true
  selectedTargetPosition.value = p.target_position || ''
  selectedCategory.value = selectedTargetPosition.value ? getCategory(selectedTargetPosition.value) : ''
  saveSelectedPositionCache()
}

const saveProfile = async () => {
  const uid = Number(localStorage.getItem('user_id') || 0)
  if (!uid) return
  editSaving.value = true
  editError.value = ''
  try {
    await authApi.updateProfile({
      id: uid,
      nickname: editForm.nickname || undefined,
      phone: editForm.phone || undefined,
      email: editForm.email || undefined,
      gender: editForm.gender,
      education: editForm.education || undefined,
      work_experience_years: editForm.work_experience_years,
      major: editForm.major || null,
      graduation_year: editForm.graduation_year,
      school: editForm.school || null,
      target_position: editForm.target_position || null,
      skill_tags: editForm.skill_tags || null,
    })
    // 重新拉取最新档案并刷新页面数据
    const p = await authApi.getProfile(uid)
    applyProfileToUI(p)
    saveSelectedPositionCache()
    saveProfileCache(p)
    editVisible.value = false
  } catch (err: any) {
    editError.value = err?.message || '保存失败，请稍后重试'
  } finally {
    editSaving.value = false
  }
}

// ───── 页面跳转方法 ─────
function _goToCareerPlan() {
  router.push(PATHS.AI_CAREER_PLAN)
}

// ───── 手机号脱敏：保留前3位和后4位，中间替换为 **** ─────
function maskPhone(phone: string): string {
  if (!phone || phone.length < 8) return phone
  return phone.slice(0, 3) + '****' + phone.slice(-4)
}

onMounted(async () => {
  restoreSelectedPositionCache()

  const localUid = Number(localStorage.getItem('user_id') || 0)
  if (localUid > 0) {
    // 先用本地缓存回填，提升首屏速度；随后再请求后端刷新。
    loadProfileFromCache(localUid)
  }

  // 没有本地 user_id 时，先通过 session 获取当前用户档案。
  const profilePromise = localUid > 0 ? authApi.getProfile(localUid) : authApi.getCurrentProfile()

  // 历史接口已按 session 过滤，无需强依赖前端 user_id。
  const [profileResult, convResult, jobOptionsResult] = await Promise.allSettled([
    profilePromise,
    conversationApi.list({ order_by: '-started_at', page: 1, page_size: 8 }),
    authApi.getInterviewJobPositions(),
  ])

  if (jobOptionsResult.status === 'fulfilled') {
    jobPositionOptions.value = jobOptionsResult.value.items || []
    restoreSelectedPositionCache()
  } else {
    console.warn('[Index] load interview job positions failed:', jobOptionsResult.reason)
  }

  // 填充用户档案
  if (profileResult.status === 'fulfilled') {
    const p = profileResult.value
    applyProfileToUI(p)
    saveProfileCache(p)
    localStorage.setItem('user_id', String(p.id || ''))
    restoreSelectedPositionCache()
  } else {
    console.warn('[Index] get profile failed:', profileResult.reason)
  }

  // 填充历史面试
  if (convResult.status === 'fulfilled') {
    const res = convResult.value
    historyInterviews.value = (res.items || []).map((it: ConversationItem) => {
      const title = `${it.job_role || '未知岗位'} · 模拟面试`
      const s = it.started_at ? new Date(it.started_at) : null
      const e = it.ended_at ? new Date(it.ended_at) : null
      const date = s ? s.toLocaleDateString() : '-'
      const durationMin = s && e ? Math.max(1, Math.round((e.getTime() - s.getTime()) / 60000)) : 0
      const duration = durationMin ? `${durationMin} min` : ''
      const level = it.status === 'finished' ? 'high' : 'mid'
      const score = it.status === 'finished' ? '已结束' : '进行中'
      return { id: it.id, title, date, duration, score, level }
    })
    // 更新最近一次得分（取第一条已结束会话，暂用总次数做示意）
    if (historyInterviews.value.length > 0) {
      stats.value.lastScore = stats.value.avgScore
    }
  } else {
    console.warn('[Index] load conversation list failed:', convResult.reason)
  }
})

</script>

<style scoped>
.tech-selector {
  position: relative;
}
/* ─── 布局 ─── */
.page-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: #0a0c12;
}
.main-content {
  flex: 1;
  margin-left: clamp(200px, 25vw, 320px);
  margin-right: clamp(200px, 25vw, 320px);
  height: 100vh;
  background: #0a0c12;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.content-area {
  display: flex;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}
.main-body {
  flex: 1;
  padding: 24px 28px;
  display: flex;
  flex-direction: column;
  gap: 24px;
  overflow: hidden;
  min-height: 0;
}

/* 右侧侧边栏 */
.right-sidebar {
  position: fixed;
  right: 0;
  top: 0;
  width: clamp(200px, 25vw, 320px);
  height: 100vh;
  max-height: 100vh;
  background: #0f121d;
  box-sizing: border-box;
  z-index: 100;
  padding: clamp(16px, 2.5vh, 24px) clamp(16px, 1.8vw, 22px);
  display: flex;
  flex-direction: column;
  gap: clamp(12px, 2vh, 20px);
  overflow: hidden;
  border-left: 1px solid #171a25;
}

/* ─── 顶部技术栈选择器 ─── */
.tech-stack-header {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.selector-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.selector-label {
  font-size: 13px;
  color: #6b7280;
  min-width: 32px;
  flex-shrink: 0;
}

.selector-options {
  display: flex;
  flex-wrap: nowrap;
  gap: 8px;
  overflow-x: auto;
  scrollbar-width: thin;
  scrollbar-color: #2d3a4f #0f121d;
  -webkit-overflow-scrolling: touch;
  scroll-behavior: smooth;
}

.selector-options::-webkit-scrollbar {
  height: 6px;
}

.selector-options::-webkit-scrollbar-track {
  background: #0f121d;
  border-radius: 3px;
}

.selector-options::-webkit-scrollbar-thumb {
  background: #2d3a4f;
  border-radius: 3px;
}

.selector-options::-webkit-scrollbar-thumb:hover {
  background: #3d4a5f;
}

.selector-btn {
  padding: 6px 14px;
  background: #0f121d;
  border: 1px solid #171a25;
  border-radius: 6px;
  color: #9ca3af;
  font-size: 13px;
  font-weight: 400;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.selector-btn:hover {
  background: #16233b;
  border-color: #2d3a4f;
  color: #ffffff;
}

.selector-btn.active {
  background: #3a7bc8;
  border-color: #3a7bc8;
  color: #ffffff;
}

.selector-btn.clear-btn {
  padding: 6px 10px;
  background: #1a2332;
  border-color: #2d3a4f;
  color: #9ca3af;
}

.selector-btn.clear-btn:hover {
  background: #2d3a4f;
  color: #ffffff;
}

.no-data {
  font-size: 13px;
  color: #6b7280;
}

.selected-position {
  white-space: nowrap;
  padding: 8px 16px;
  background: #3a7bc8;
  border: 1px solid #3a7bc8;
  border-radius: 8px;
  color: #ffffff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.selected-position:hover:not(.active) {
  background: #2d6bb3;
  border-color: #2d6bb3;
}

.more-btn {
  white-space: nowrap;
  padding: 8px 12px;
  background: #0f121d;
  border: 1px dashed #2d3a4f;
  border-radius: 8px;
  color: #9ca3af;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.more-btn:hover {
  background: #16233b;
  border-color: #3a7bc8;
  color: #ffffff;
}

.more-btn.expanded {
  border-color: #3a7bc8;
  color: #3a7bc8;
  transform: rotate(90deg);
}

.more-icon {
  font-size: 16px;
  line-height: 1;
  transition: transform 0.2s ease-out;
}

.tech-selector-item {
  white-space: nowrap;
  padding: 8px 16px;
  background: #0f121d;
  border: 1px solid #171a25;
  border-radius: 8px;
  color: #9ca3af;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.tech-selector-item.active {
  background: #3a7bc8;
  border-color: #3a7bc8;
  color: #ffffff;
  position: relative;
}

.tech-selector-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 60%;
  background: #ffffff;
  border-radius: 0 2px 2px 0;
}

.tech-selector-expanded .tech-selector-item.active {
  background: #3a7bc8;
  border-color: #3a7bc8;
  color: #ffffff;
  position: relative;
}

.tech-selector-expanded .tech-selector-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 60%;
  background: #ffffff;
  border-radius: 0 2px 2px 0;
}

.expand-enter-active,
.expand-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.expand-enter-from,
.expand-leave-to {
  opacity: 0;
  transform: scaleX(0);
}

.expand-enter-to,
.expand-leave-from {
  opacity: 1;
  transform: scaleX(1);
}

.tech-selector-item:hover:not(.active) {
  background: #16233b;
  border-color: #2d3a4f;
  color: #9ca3af;
}

.tech-selector-expanded .tech-selector-item:hover:not(.active) {
  background: #16233b;
  border-color: #2d3a4f;
  color: #9ca3af;
}

.tech-selector-expanded .tech-selector-item.active {
  background: #3a7bc8;
  border-color: #3a7bc8;
  color: #ffffff;
  position: relative;
}

.tech-selector-expanded .tech-selector-item.active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 60%;
  background: #ffffff;
  border-radius: 0 2px 2px 0;
}

/* ─── 技术栈标签区域（仅已掌握） ─── */
.tech-tags-section {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.tech-tags-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.tech-tag {
  flex-shrink: 0;
  margin-top: 4px;
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 4px;
  font-weight: 500;
}

.tech-tag.mastered {
  background: #10b981;
  color: #0a0c12;
}

.tech-tags-items {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
  flex: 1;
}

.tech-tag-item {
  font-size: 12px;
  padding: 6px 14px;
  background: #0f121d;
  border: 1px solid #171a25;
  border-radius: 6px;
  color: #9ca3af;
  transition: all 0.2s;
}

.tech-tag-item.active {
  background: #1a2332;
  border-color: #2d3a4f;
  color: #ffffff;
}

.tech-empty {
  font-size: 12px;
  color: #6b7280;
}

/* ─── 统计卡片行 ─── */
.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.stat-card {
  background: #0f121d;
  border: 1px solid #171a25;
  border-radius: 16px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.stat-card-label {
  font-size: 13px;
  color: #6b7280;
}

.stat-card-value {
  display: flex;
  align-items: baseline;
  gap: 4px;
}

.stat-big {
  font-size: 42px;
  font-weight: 700;
  color: #ffffff;
  line-height: 1;
}

.stat-unit {
  font-size: 14px;
  color: #6b7280;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  margin-top: 4px;
}

.stat-trend.up {
  color: #10b981;
}

.trend-icon {
  font-size: 14px;
}

.stat-subtitle {
  font-size: 12px;
  color: #6b7280;
  margin-top: 4px;
}

.progress-bar {
  width: 100%;
  height: 4px;
  background: #1a2332;
  border-radius: 2px;
  margin-top: 8px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #3a7bc8, #60a5fa);
  border-radius: 2px;
}

/* 能力分析图表区 */
.charts-row {
  position: relative;
  display: grid;
  grid-template-columns: 0.74fr 1.26fr;
  gap: 20px;
  margin-bottom: 10px;
}

.charts-row > .chart-card:last-child {
  position: relative;
}

.charts-row > .chart-card:last-child::before {
  content: '';
  position: absolute;
  left: -10px;
  top: 12px;
  bottom: 12px;
  width: 1px;
  background: #273247;
}

@media (max-width: 1200px) {
  .charts-row {
    grid-template-columns: 1fr;
  }

  .charts-row > .chart-card:last-child::before {
    display: none;
  }

  .radar-card {
    width: 100%;
  }
}

.radar-card {
  display: flex;
  flex-direction: column;
  width: 100%;
  justify-self: stretch;
}

.radar-chart-container {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 205px;
}

.radar-chart {
  width: 100%;
  height: 100%;
  max-width: 220px;
  max-height: 220px;
}

/* 成长曲线图 */
.growth-chart-container {
  height: 200px;
  padding: 20px;
}

.growth-chart {
  width: 100%;
  height: 100%;
}
.chart-card {
  background: #0f121d;
  border: 1px solid #171a25;
  border-radius: 16px;
  padding: 20px;
  margin-bottom: 20px;
}

.chart-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 32px;
}

.chart-title {
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
  margin: 0;
}

.chart-legend {
  display: flex;
  align-items: center;
  gap: 16px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #9ca3af;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.legend-dot.blue {
  background: #3a7bc8;
}

.legend-dot.gray {
  background: #374151;
}

.ability-chart {
  height: 200px;
}

.chart-bars {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  height: 100%;
  padding: 0 8px;
}

.bar-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  flex: 1;
}

.bar-wrapper {
  position: relative;
  width: 48px;
  height: 160px;
  display: flex;
  align-items: flex-end;
}

.bar-bg {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  width: 100%;
  background: #1a1f2e;
  border-radius: 24px 24px 12px 12px;
  opacity: 0.8;
}

.bar-fill {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  width: 100%;
  background: linear-gradient(180deg, #4a9eff 0%, #3a7bc8 100%);
  border-radius: 24px 24px 12px 12px;
  min-height: 4px;
}

.bar-label {
  font-size: 12px;
  color: #6b7280;
}

.plan-section {
  background: #0f121d;
  border: 1px solid #171a25;
  border-radius: 16px;
  padding: 20px;
  margin-top: -10px;
}

.plan-header {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 16px;
  gap: 10px;
}

.plan-title {
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
  margin: 0;
}

.plan-subtitle {
  font-size: 12px;
  color: #6b7280;
}

.plan-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.plan-card {
  background: #131825;
  border: 1px solid #1d2536;
  border-radius: 12px;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.plan-card-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.plan-card-title {
  font-size: 14px;
  color: #e5e7eb;
  font-weight: 600;
}

.plan-progress-text {
  font-size: 12px;
  color: #60a5fa;
  font-weight: 600;
}

.plan-progress {
  width: 100%;
  height: 6px;
  background: #1a2332;
  border-radius: 999px;
  overflow: hidden;
}

.plan-progress-fill {
  height: 100%;
  background: linear-gradient(90deg, #3a7bc8, #60a5fa);
  border-radius: 999px;
}

.plan-tasks {
  margin: 0;
  padding-left: 16px;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.plan-tasks li {
  font-size: 12px;
  color: #9ca3af;
  line-height: 1.5;
}

@media (max-width: 1200px) {
  .plan-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

.sidebar-section {
  display: flex;
  flex-direction: column;
  gap: clamp(8px, 1.5vh, 12px);
  flex-shrink: 0;
}

.sidebar-section.hot-questions {
  flex: 1;
  min-height: 0;
}

.sidebar-section.trends {
  flex: 1;
  min-height: 0;
}

.sidebar-section.ai-mentor {
  flex-shrink: 0;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-icon {
  width: 20px;
  height: 20px;
  object-fit: contain;
}

.section-title {
  font-size: clamp(14px, 1.8vw, 16px);
  font-weight: 600;
  color: #ffffff;
}

/* 行业热门真题 */
.question-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.question-list.fading {
  opacity: 0;
  transform: translateY(10px);
}

.question-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px;
  background: #131825;
  border: 1px solid #171a25;
  border-radius: 10px;
  transition: all 0.4s ease;
}

.question-item:nth-child(1) { transition-delay: 0ms; }
.question-item:nth-child(2) { transition-delay: 50ms; }
.question-item:nth-child(3) { transition-delay: 100ms; }

/* 面试趋势 */
.trend-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
  transition: opacity 0.3s ease, transform 0.3s ease;
}

.trend-list.fading {
  opacity: 0;
  transform: translateY(10px);
}

.trend-item-wrapper {
  background: #131825;
  border: 1px solid #171a25;
  border-radius: 10px;
  padding: 10px 12px;
  transition: all 0.4s ease;
}

.trend-item-wrapper:nth-child(1) { transition-delay: 0ms; }
.trend-item-wrapper:nth-child(2) { transition-delay: 50ms; }
.trend-item-wrapper:nth-child(3) { transition-delay: 100ms; }
.trend-item-wrapper:nth-child(4) { transition-delay: 150ms; }
.trend-item-wrapper:nth-child(5) { transition-delay: 200ms; }

/* 分页 */
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-top: 12px;
  flex-shrink: 0;
}

.page-dots {
  display: flex;
  gap: 6px;
}

.page-dots .dot {
  width: 8px;
  height: 4px;
  border-radius: 2px;
  background: #2d3a4f;
  transition: all 0.4s ease;
}

.page-dots .dot.active {
  background: linear-gradient(90deg, #3a7bc8, #60a5fa);
  width: 20px;
  box-shadow: 0 0 8px rgba(58, 123, 200, 0.5);
}

.question-carousel .question-item {
  transition: all 0.4s ease;
}

.question-carousel .question-item.highlighted {
  border-color: #3a7bc8;
  box-shadow: 0 0 12px rgba(58, 123, 200, 0.2);
}

.carousel-dots {
  display: flex;
  justify-content: center;
  gap: 6px;
  margin-top: 8px;
}

.carousel-dots .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #2d3a4f;
  cursor: pointer;
  transition: all 0.3s;
}

.carousel-dots .dot.active {
  background: #3a7bc8;
  transform: scale(1.2);
}

.question-source {
  font-size: clamp(11px, 1.4vw, 12px);
  color: #6b7280;
}

.question-title {
  font-size: clamp(12px, 1.6vw, 14px);
  font-weight: 500;
  color: #ffffff;
  line-height: 1.5;
}

.question-tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.question-tags .tag {
  font-size: clamp(10px, 1.2vw, 11px);
  padding: 3px 8px;
  border-radius: 4px;
  background: rgba(58, 123, 200, 0.15);
  color: #3a7bc8;
}

/* 面试趋势 */
.trend-carousel {
  display: flex;
  flex-direction: column;
  gap: clamp(8px, 1.2vh, 10px);
  flex: 1;
  position: relative;
}

.trend-carousel .trend-item-wrapper {
  transition: all 0.4s ease;
}

.trend-carousel .trend-item-wrapper.highlighted {
  border-color: #3a7bc8;
  box-shadow: 0 0 12px rgba(58, 123, 200, 0.2);
}

/* 轮播过渡动画 */
.carousel-enter-active,
.carousel-leave-active {
  transition: all 0.5s ease;
}

.carousel-enter-from {
  opacity: 0;
  transform: translateY(20px);
}

.carousel-leave-to {
  opacity: 0;
  transform: translateY(-20px);
}

.carousel-move {
  transition: transform 0.5s ease;
}

.trend-item {
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.trend-dot {
  color: #3a7bc8;
  font-size: 8px;
  margin-top: 5px;
  flex-shrink: 0;
}

.trend-content {
  font-size: clamp(12px, 1.5vw, 13px);
  line-height: 1.6;
}

.trend-highlight {
  color: #3a7bc8;
  font-weight: 500;
}

.trend-text {
  color: #9ca3af;
}

/* AI 职业导师 */
.ai-mentor {
  border: 1px solid #171a25;
  border-radius: 12px;
  padding: clamp(12px, 2vh, 16px) clamp(14px, 1.8vw, 18px);
  background: #131825;
  flex-shrink: 0;
}

.mentor-card {
  background: #1a2332;
  border-radius: 12px;
  padding: clamp(12px, 2vh, 16px) clamp(14px, 1.8vw, 18px);
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: 6px;
}

.mentor-avatar {
  width: clamp(36px, 4vw, 44px);
  height: clamp(36px, 4vw, 44px);
  border-radius: 50%;
  background: rgba(58, 123, 200, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: clamp(16px, 2vw, 20px);
  border: 1px solid #171a25;
}

.mentor-title {
  font-size: clamp(13px, 1.6vw, 15px);
  font-weight: 600;
  color: #ffffff;
}

.mentor-desc {
  font-size: clamp(10px, 1.2vw, 11px);
  color: #9ca3af;
  line-height: 1.4;
}

.mentor-btn {
  width: 100%;
  padding: clamp(8px, 1.2vh, 10px);
  margin-top: 4px;
  background: #3a7bc8;
  color: #ffffff;
  font-size: clamp(11px, 1.3vw, 13px);
  font-weight: 500;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.mentor-btn:hover {
  background: #2d6bb3;
}

/* ─── 响应式 ─── */
@media (max-width: 1200px) {
  .stats-row { grid-template-columns: 1fr; }
}
@media (max-width: 900px) {
  .main-content { margin-right: 0; }
  .right-sidebar { display: none; }
  .plan-grid { grid-template-columns: 1fr; }
}

/* ─── 编辑资料弹窗 ─── */
.edit-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 9999;
  padding: 20px;
}
.edit-modal {
  background: #fff;
  border-radius: 16px;
  width: 100%;
  max-width: 560px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 8px 40px rgba(0, 0, 0, 0.18);
  overflow: hidden;
}
.edit-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 22px 14px;
  border-bottom: 1px solid #f3f4f6;
  flex-shrink: 0;
}
.edit-title {
  font-size: 16px;
  font-weight: 600;
  color: #111827;
}
.edit-close {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  background: #f3f4f6;
  color: #6b7280;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.15s;
}
.edit-close:hover { background: #e5e7eb; }
.edit-body {
  flex: 1;
  overflow-y: auto;
  padding: 18px 22px;
}
.edit-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px 16px;
}
.form-item {
  display: flex;
  flex-direction: column;
  gap: 5px;
}
.form-item-full {
  grid-column: 1 / -1;
}
.form-item label {
  font-size: 12px;
  font-weight: 500;
  color: #374151;
}
.form-hint {
  font-size: 11px;
  color: #9ca3af;
  font-weight: 400;
}
.form-item input,
.form-item select {
  height: 36px;
  padding: 0 10px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  font-size: 13px;
  color: #111827;
  outline: none;
  transition: border-color 0.2s;
  background: #fff;
  box-sizing: border-box;
}
.form-item input:focus,
.form-item select:focus { border-color: #3a7bc8; }
.edit-error {
  margin: 12px 0 0;
  font-size: 12px;
  color: #ef4444;
  text-align: center;
}
.edit-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 22px 18px;
  border-top: 1px solid #f3f4f6;
  flex-shrink: 0;
}
.edit-footer .btn-primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
/* Editorial SaaS / retro pastel dashboard refresh */
.page-layout {
  --ink: var(--hp-ink);
  --muted: var(--hp-muted);
  --paper: var(--hp-bg);
  --surface: var(--hp-surface);
  --pink: var(--hp-pink);
  --mustard: var(--hp-surface-2);
  --olive: var(--hp-green);
  --blue: var(--hp-blue);
  --lilac: var(--hp-pink-soft);
  height: calc(100vh - 60px);
  color: var(--ink);
  background: var(--paper);
}

.main-content {
  height: calc(100vh - 60px);
  margin-left: clamp(248px, 22vw, 296px);
  margin-right: 304px;
  padding: 12px 14px 14px 0;
  gap: 12px;
  background: var(--paper);
  box-sizing: border-box;
}

.main-content > .editorial-toolbar { flex: 0 0 auto; }

.content-area {
  overflow: auto;
  gap: 14px;
  scrollbar-color: #aaa398 transparent;
}

.main-body {
  padding: 0;
  gap: 14px;
  overflow: visible;
}

.interview-hero {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(300px, .72fr);
  gap: 14px;
}

.interview-hero__copy,
.interview-hero__actions {
  border: 1px solid var(--ink);
  border-radius: 26px;
}

.interview-hero__copy {
  min-height: 154px;
  padding: 26px 28px;
  background: var(--mustard);
}

.interview-hero__eyebrow {
  color: #615c51;
  font-size: 10px;
  font-weight: 800;
  letter-spacing: .18em;
}

.interview-hero h1 {
  margin: 15px 0 7px;
  color: var(--ink);
  font-size: clamp(28px, 2.7vw, 43px);
  font-weight: 850;
  line-height: 1.04;
  letter-spacing: -.05em;
}

.interview-hero p { max-width: 600px; margin: 0; color: #5f594e; font-size: 13px; line-height: 1.65; }

.interview-hero__actions {
  display: grid;
  grid-template-columns: .92fr 1.08fr;
  gap: 10px;
  padding: 10px;
  background: var(--surface);
}

.hero-action {
  display: flex;
  min-width: 0;
  flex-direction: column;
  align-items: flex-start;
  justify-content: space-between;
  padding: 18px;
  border: 1px solid var(--ink);
  border-radius: 19px;
  color: var(--ink);
  text-align: left;
  transition: transform .18s ease, box-shadow .18s ease;
}
.hero-action:hover { border-color: var(--hp-line-strong); transform: translateY(-2px); box-shadow: var(--hp-shadow-sm); }
.hero-action--primary { color: var(--hp-pink-ink); background: var(--hp-yellow); }
.hero-action--resume { background: var(--pink); }
.hero-action small { font-size: 9px; font-weight: 800; letter-spacing: .14em; }
.hero-action span { font-size: 15px; font-weight: 750; line-height: 1.3; }
.hero-action b { font-size: 11px; font-weight: 650; }

.tech-stack-header,
.tech-tags-section,
.stat-card,
.chart-card,
.plan-section,
.sidebar-section {
  border: 1px solid var(--ink);
  border-radius: 22px;
  box-shadow: none;
}

.tech-stack-header { padding: 16px 18px; background: var(--surface); }
.tech-tags-section { padding: 14px 18px; background: #e7ece2; }
.selector-label { color: var(--muted); font-weight: 700; }
.selector-options { scrollbar-color: #a9a397 transparent; }
.selector-btn,
.tech-tag-item { border-color: #c7c0b4; border-radius: 999px; color: #56524b; background: #f1ece3; }
.selector-btn:hover { border-color: var(--ink); color: var(--ink); background: #fffdf7; }
.selector-btn.active { border-color: var(--hp-line-strong); color: var(--hp-pink-ink); background: var(--hp-yellow); }
.tech-tag { border: 1px solid var(--ink); border-radius: 999px; }
.tech-tag.mastered { color: var(--ink); background: var(--olive); }
.tech-tag-item.active { border-color: #898278; color: var(--ink); background: #fffdf7; }
.tech-empty,
.no-data { color: var(--muted); }

.stats-row { gap: 14px; }
.stat-card { min-height: 124px; padding: 20px; }
.stat-card:nth-child(1) { background: var(--pink); }
.stat-card:nth-child(2) { background: var(--blue); }
.stat-card:nth-child(3) { background: var(--olive); }
.stat-card-label,
.stat-unit,
.stat-subtitle { color: #645f57; }
.stat-big { color: var(--ink); font-size: 38px; letter-spacing: -.045em; }
.stat-trend.up { color: #46553e; font-weight: 700; }
.progress-bar { background: rgba(36,35,31,.14); }
.progress-fill { background: var(--hp-pink-ink); }

.charts-row { gap: 14px; margin-bottom: 0; }
.chart-card { margin-bottom: 0; padding: 20px; background: var(--surface); }
.radar-card { background: var(--lilac); }
.chart-title,
.plan-title,
.plan-card-title,
.section-title,
.question-title { color: var(--ink); }
.chart-header { margin-bottom: 18px; }
.legend-item,
.bar-label { color: var(--muted); }
.legend-dot.blue { background: #7b8f88; }
.legend-dot.gray { background: #c3bbb0; }
.radar-chart polygon[stroke="#2d3a4f"],
.radar-chart line { stroke: #90899a !important; }
.radar-chart polygon[stroke="#3a7bc8"] { fill: rgba(125, 145, 132, .28) !important; stroke: #52645b !important; }
.radar-chart circle { fill: #52645b !important; }
.radar-chart text { fill: #625e68 !important; }
.bar-bg { background: #ddd7ce; }
.bar-fill { background: #81958a; }
.charts-row > .chart-card:last-child::before { background: #cfc8bc; }

.plan-section { margin-top: 0; background: #e5dfd1; }
.plan-subtitle { color: var(--muted); }
.plan-card { border-color: var(--ink); border-radius: 17px; background: var(--surface); }
.plan-progress-text { color: #59685d; }
.plan-progress { background: #ddd7cb; }
.plan-progress-fill { background: #829589; }
.plan-tasks li { color: #666158; }

.right-sidebar {
  top: 72px;
  right: 14px;
  width: 276px;
  height: calc(100vh - 86px);
  padding: 12px;
  gap: 12px;
  border: 1px solid var(--ink);
  border-radius: 26px;
  color: var(--ink);
  background: #eee6d9;
  min-width: 0;
  min-height: 0;
  overflow-x: hidden;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(90, 84, 75, .38) transparent;
}
.right-sidebar::-webkit-scrollbar { width: 5px; }
.right-sidebar::-webkit-scrollbar-track { background: transparent; }
.right-sidebar::-webkit-scrollbar-thumb {
  border-radius: 999px;
  background: rgba(90, 84, 75, .34);
}
.right-sidebar::-webkit-scrollbar-thumb:hover { background: rgba(90, 84, 75, .5); }

.sidebar-section {
  flex: 0 0 auto;
  min-width: 0;
  min-height: 0;
  height: auto;
  padding: 16px;
  gap: 12px;
  overflow: visible;
  box-sizing: border-box;
  background: var(--surface);
}
.sidebar-section.hot-questions { background: #e4e9df; }
.sidebar-section.trends { background: #e9d8da; }
.sidebar-section.hot-questions,
.sidebar-section.trends { flex: 0 0 auto; }

.section-header { min-width: 0; }
.section-title {
  min-width: 0;
  font-size: 15px;
  line-height: 1.35;
  white-space: normal;
  word-break: break-word;
  overflow-wrap: anywhere;
}
.section-icon { filter: grayscale(1) contrast(.85); }

.question-list,
.trend-list {
  flex: 0 0 auto;
  min-width: 0;
  min-height: 0;
  height: auto;
  overflow: visible;
}
.question-list { gap: 12px; }
.trend-list { gap: 10px; }

.question-item,
.trend-item-wrapper {
  flex: 0 0 auto;
  min-width: 0;
  min-height: 0;
  height: auto;
  border-color: #bdb5a9;
  border-radius: 14px;
  overflow: visible;
  box-sizing: border-box;
  background: rgba(255,253,247,.72);
}
.question-item {
  gap: 8px;
  padding: 14px;
}
.question-source,
.question-title,
.trend-content,
.trend-highlight,
.trend-text {
  min-width: 0;
  white-space: normal;
  word-break: break-word;
  overflow-wrap: anywhere;
}
.question-source {
  color: var(--muted);
  font-size: 11px;
  font-weight: 650;
  line-height: 1.45;
}
.question-title {
  font-size: 13px;
  font-weight: 560;
  line-height: 1.62;
}
.question-tags {
  min-width: 0;
  margin-top: 2px;
  row-gap: 6px;
}
.question-tags .tag {
  max-width: 100%;
  border: 1px solid #a9a094;
  border-radius: 999px;
  color: #544f48;
  background: #eee7dc;
  white-space: normal;
  word-break: break-word;
  overflow-wrap: anywhere;
}

.trend-item-wrapper { padding: 13px 14px; }
.trend-item {
  min-width: 0;
  min-height: 0;
  gap: 9px;
  align-items: flex-start;
}
.trend-dot {
  flex: 0 0 auto;
  margin-top: 6px;
}
.trend-content {
  flex: 1 1 auto;
  font-size: 12px;
  line-height: 1.65;
}
.trend-dot,
.trend-highlight { color: #5f6959; }
.trend-highlight { font-weight: 700; }
.trend-text { color: #656059; }
.pagination { margin-top: 2px; }
.page-dots .dot { background: #b6aea2; }
.page-dots .dot.active { width: 20px; background: var(--ink); box-shadow: none; }

.edit-overlay { background: rgba(27,26,23,.5); }
.edit-modal { border: 1px solid var(--ink); border-radius: 24px; background: var(--surface); box-shadow: 9px 10px 0 rgba(36,35,31,.2); }
.edit-title { color: var(--ink); }
.edit-header,
.edit-footer { border-color: #d2cbbf; }
.edit-close { border: 1px solid var(--ink); color: var(--ink); background: #eee7dc; }
.form-item label { color: #4f4b45; }
.form-item input,
.form-item select { height: 40px; border-color: #bdb6aa; border-radius: 12px; color: var(--ink); background: #f7f2e9; }
.form-item input:focus,
.form-item select:focus { border-color: var(--ink); }
.edit-footer .btn-outline,
.edit-footer .btn-primary { border: 1px solid var(--ink); border-radius: 999px; }
.edit-footer .btn-outline { color: var(--ink); background: var(--surface); }
.edit-footer .btn-primary { color: #fff; background: var(--ink); }

@media (max-width: 1200px) {
  .main-content { margin-right: 14px; }
  .right-sidebar { display: none; }
  .stats-row { grid-template-columns: repeat(3, minmax(0, 1fr)); }
}

@media (max-width: 900px) {
  .main-content { margin-left: 88px; margin-right: 8px; padding-right: 0; }
  .interview-hero { grid-template-columns: 1fr; }
  .stats-row { grid-template-columns: 1fr; }
}

@media (max-width: 620px) {
  .interview-hero__actions { grid-template-columns: 1fr; }
  .plan-grid { grid-template-columns: 1fr; }
}
</style>
