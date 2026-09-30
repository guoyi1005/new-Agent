<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { getCareerNebulaMap } from '../api/careerNebula'
import { resolveBossJobSearchLink } from '../api/jobRecommendations'
import AppTabBar from '../components/AppTabBar.vue'

const router = useRouter()

/* ============================================================
 * 岗位探索展示层数据
 * 1) 目标岗位与首页共用同一份本地选择（home_target_job），两处保持一致；
 * 2) 匹配度、已掌握/待提升、岗位差距与推荐岗位来自本期给定的展示数据，
 *    后端岗位匹配能力就绪后，只需替换下面这几段常量，页面结构与逻辑不变；
 * 3) 岗位星图直接读取现有星图接口，节点图与名称都是真实岗位数据。
 * ============================================================ */

const searchKeyword = ref('')
const hotSearches = ['Python开发', 'AI算法', 'Java后端', '前端', '数据分析']

function runSearch(keyword) {
  const query = String(keyword ?? searchKeyword.value).trim()
  if (!query) return
  window.open(resolveBossJobSearchLink(query), '_blank', 'noopener,noreferrer')
}

const TARGET_JOB_STORAGE_KEY = 'home_target_job'
const DEFAULT_TARGET_JOB = 'Python 开发工程师'

/* 每个目标岗位对应自己的一组结果：匹配度、已掌握/待提升、能力差距与推荐岗位。
 * 后端暂无岗位匹配接口，这里是与首页同一套的展示层数据；补齐一个岗位，
 * 只需要在下面数组里追加一条即可，页面逻辑无需改动。 */
const JOB_PROFILES = [
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
      { id: 'py', title: 'Python开发', matchRate: 86, skills: ['Python', 'FastAPI', 'MySQL'] },
      { id: 'ai-app', title: 'AI应用开发', matchRate: 73, skills: ['Python', 'LLM', 'FastAPI'] },
      { id: 'data', title: '数据分析', matchRate: 68, skills: ['Python', 'SQL', '数据分析'] },
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
      { id: 'fe', title: '前端开发', matchRate: 84, skills: ['Vue3', 'TypeScript', 'Vite'] },
      { id: 'mini', title: '小程序开发', matchRate: 66, skills: ['JavaScript', '小程序', '接口联调'] },
      { id: 'full', title: '全栈开发', matchRate: 61, skills: ['Vue3', 'Node.js', 'MySQL'] },
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
      { id: 'java', title: 'Java后端', matchRate: 80, skills: ['Java', 'Spring Boot', 'MySQL'] },
      { id: 'micro', title: '微服务开发', matchRate: 58, skills: ['Spring Cloud', 'Redis', 'Docker'] },
      { id: 'test-dev', title: '测试开发', matchRate: 55, skills: ['Java', '接口测试', 'Jenkins'] },
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
      { id: 'ai-app', title: 'AI应用开发', matchRate: 85, skills: ['Python', 'LLM', 'FastAPI'] },
      { id: 'algo', title: '算法工程', matchRate: 68, skills: ['Python', 'PyTorch', '数据处理'] },
      { id: 'py', title: 'Python开发', matchRate: 74, skills: ['Python', 'FastAPI', 'MySQL'] },
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
      { id: 'algo', title: '算法工程', matchRate: 69, skills: ['Python', 'PyTorch', '数据处理'] },
      { id: 'ai-app', title: 'AI应用开发', matchRate: 64, skills: ['Python', 'LLM', 'FastAPI'] },
      { id: 'data', title: '数据分析', matchRate: 60, skills: ['Python', 'SQL', '统计分析'] },
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
      { id: 'data', title: '数据分析', matchRate: 83, skills: ['Python', 'SQL', '可视化'] },
      { id: 'ops', title: '数据运营', matchRate: 65, skills: ['SQL', 'Excel', '指标体系'] },
      { id: 'py', title: 'Python开发', matchRate: 62, skills: ['Python', 'FastAPI', 'MySQL'] },
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
      { id: 'test-dev', title: '测试开发', matchRate: 74, skills: ['Python', '接口自动化', 'Jenkins'] },
      { id: 'qa', title: '软件测试', matchRate: 78, skills: ['用例设计', 'SQL', '抓包分析'] },
      { id: 'sre', title: '运维开发', matchRate: 58, skills: ['Linux', 'Shell', 'Docker'] },
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
      { id: 'pm', title: '产品经理', matchRate: 72, skills: ['需求分析', '原型', '数据分析'] },
      { id: 'ops', title: '数据运营', matchRate: 63, skills: ['SQL', '指标体系', '活动运营'] },
      { id: 'ux', title: '交互设计', matchRate: 58, skills: ['原型', '用户研究', '交互稿'] },
    ],
  },
]

const jobProfileMap = new Map(JOB_PROFILES.map((profile) => [profile.title, profile]))

function readStoredTargetJob() {
  try {
    return localStorage.getItem(TARGET_JOB_STORAGE_KEY) || DEFAULT_TARGET_JOB
  } catch {
    return DEFAULT_TARGET_JOB
  }
}

const targetJobTitle = ref(readStoredTargetJob())

const targetJob = computed(() => {
  const profile = jobProfileMap.get(targetJobTitle.value)
  if (profile) {
    return profile
  }
  return {
    title: targetJobTitle.value,
    matchRate: null,
    mastered: [],
    toImprove: [],
    gaps: [],
    advice: '完成岗位体检后，这里会显示匹配度、能力差距与提升建议。',
    fit: [],
  }
})

const fitJobs = computed(() => targetJob.value.fit || [])

/* ---------- 更换目标岗位 ---------- */

const isJobPickerOpen = ref(false)
const jobKeyword = ref('')

const filteredJobOptions = computed(() => {
  const keyword = jobKeyword.value.trim().toLowerCase()
  if (!keyword) return JOB_PROFILES
  return JOB_PROFILES.filter((profile) => profile.title.toLowerCase().includes(keyword))
})

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

/* ---------- 岗位星图：以目标岗位为中心，环绕真实岗位节点 ---------- */

const NODE_TONES = ['yellow', 'blue', 'green', 'pink']
const RING_COUNT = 6
const RING_RADIUS_X = 33
const RING_RADIUS_Y = 36

const nebulaCareers = ref([])
const nebulaLoading = ref(true)

const targetCareer = computed(() => nebulaCareers.value.find((career) => isTargetCareer(career.name)) || null)

const starmapNodes = computed(() => {
  const careers = nebulaCareers.value.slice(0, RING_COUNT)
  const total = careers.length
  return careers.map((career, index) => {
    // 半个步长起步，避免节点正好压在中心节点的正上/正下方。
    const step = 360 / total
    const angle = ((-90 + step / 2 + step * index) * Math.PI) / 180
    return {
      id: career.id,
      name: career.name,
      image: career.image || '',
      tone: NODE_TONES[index % NODE_TONES.length],
      x: clamp(50 + Math.cos(angle) * RING_RADIUS_X, 15, 85),
      y: clamp(50 + Math.sin(angle) * RING_RADIUS_Y, 15, 85),
    }
  })
})

const centerNode = computed(() => ({
  name: targetJobTitle.value,
  image: targetCareer.value?.image || '',
}))

function clamp(value, min, max) {
  return Math.min(max, Math.max(min, value))
}

/** 目标岗位与星图节点名称有共同核心词时高亮该节点。 */
function isTargetCareer(name) {
  const normalize = (value) => String(value || '').replace(/[\s·、/／]/g, '')
  const core = normalize(targetJobTitle.value).replace(/工程师|开发|应用|实习/g, '')
  const candidate = normalize(name)
  return core.length >= 2 && candidate.includes(core)
}

function monogram(name = '') {
  return String(name).replace(/工程师|开发|应用|实习/g, '').slice(0, 2) || '星'
}

function gapPercent(gap) {
  const required = Number(gap.required) || 0
  if (!required) return 0
  return Math.min(100, Math.round((Number(gap.current) / required) * 100))
}

async function loadNebula() {
  nebulaLoading.value = true
  try {
    const data = await getCareerNebulaMap()
    const careers = Array.isArray(data.careers) ? data.careers : []
    nebulaCareers.value = careers.filter((career) => career.status === 'enabled')
  } catch {
    nebulaCareers.value = []
  } finally {
    nebulaLoading.value = false
  }
}

onMounted(loadNebula)
</script>

<template>
  <div class="feature-page">
    <AppTabBar />

    <main class="feature-container">
      <section class="jobexplore-hero">
        <div class="jobexplore-hero__copy">
          <h1>岗位探索</h1>
          <p>找到方向，看清岗位要求，也看见自己的下一步</p>
        </div>

        <div class="jobexplore-hero__tools">
          <form class="jobexplore-search" @submit.prevent="runSearch()">
            <svg class="jobexplore-search__icon" viewBox="0 0 24 24" aria-hidden="true">
              <circle cx="11" cy="11" r="6.5" />
              <path d="M16 16.2 20.4 20.6" />
            </svg>
            <input
              v-model="searchKeyword"
              type="search"
              placeholder="搜索岗位 / 公司 / 技能"
              aria-label="搜索岗位 / 公司 / 技能"
            />
            <button type="submit">搜索</button>
          </form>

          <p class="jobexplore-hot">
            <span class="jobexplore-hot__label">热门</span>
            <template v-for="(item, index) in hotSearches" :key="item">
              <i v-if="index" aria-hidden="true">·</i>
              <button type="button" @click="runSearch(item)">{{ item }}</button>
            </template>
          </p>
        </div>
      </section>

      <section class="jobexplore-top">
        <article class="feature-card jobexplore-panel jobexplore-panel--map">
          <div class="feature-section__head">
            <h2>岗位星图</h2>
            <span class="jobexplore-meta">{{ starmapNodes.length }} 个开放岗位</span>
          </div>

          <div class="jobexplore-starmap">
            <span class="jobexplore-starmap__orbit jobexplore-starmap__orbit--one" aria-hidden="true"></span>
            <span class="jobexplore-starmap__orbit jobexplore-starmap__orbit--two" aria-hidden="true"></span>

            <p v-if="nebulaLoading" class="jobexplore-starmap__state">星图加载中…</p>
            <p v-else-if="!starmapNodes.length" class="jobexplore-starmap__state">星图数据暂不可用</p>

            <template v-else>
              <svg class="jobexplore-starmap__edges" viewBox="0 0 100 100" preserveAspectRatio="none" aria-hidden="true">
                <line
                  v-for="node in starmapNodes"
                  :key="node.id"
                  x1="50"
                  y1="50"
                  :x2="node.x"
                  :y2="node.y"
                  vector-effect="non-scaling-stroke"
                />
              </svg>

              <button
                class="jobexplore-node jobexplore-node--center"
                type="button"
                :aria-label="`进入岗位星图，我的目标岗位 ${centerNode.name}`"
                @click="router.push('/career/nebula')"
              >
                <span
                  class="jobexplore-node__dot"
                  :style="centerNode.image ? { backgroundImage: `url(${centerNode.image})` } : {}"
                >
                  <span v-if="!centerNode.image">{{ monogram(centerNode.name) }}</span>
                </span>
                <span class="jobexplore-node__label jobexplore-node__label--strong">{{ centerNode.name }}</span>
              </button>

              <button
                v-for="node in starmapNodes"
                :key="node.id"
                class="jobexplore-node"
                :class="`is-${node.tone}`"
                :style="{ left: `${node.x}%`, top: `${node.y}%`, '--node-size': '60px' }"
                type="button"
                :aria-label="`进入岗位星图查看${node.name}`"
                @click="router.push('/career/nebula')"
              >
                <span class="jobexplore-node__dot" :style="node.image ? { backgroundImage: `url(${node.image})` } : {}">
                  <span v-if="!node.image">{{ monogram(node.name) }}</span>
                </span>
                <span class="jobexplore-node__label">{{ node.name }}</span>
              </button>
            </template>
          </div>

          <div class="jobexplore-panel__foot">
            <p class="jobexplore-note">探索岗位之间的关系和职业发展路径</p>
            <button class="feature-button jobexplore-cta" type="button" @click="router.push('/career/nebula')">
              进入岗位星图 →
            </button>
          </div>
        </article>

        <article class="feature-card jobexplore-panel jobexplore-panel--target">
          <div class="feature-section__head">
            <h2>我的目标岗位</h2>
            <button class="jobexplore-edit" type="button" @click="openJobPicker">更换</button>
          </div>

          <div class="jobexplore-target">
            <p class="jobexplore-job">{{ targetJob.title }}</p>
            <template v-if="targetJob.matchRate">
              <div class="jobexplore-match">
                <span>当前匹配度</span>
                <strong>{{ targetJob.matchRate }}%</strong>
              </div>
              <span class="jobexplore-meter" aria-hidden="true">
                <i :style="{ width: `${targetJob.matchRate}%` }"></i>
              </span>
            </template>
            <p v-else class="jobexplore-match jobexplore-match--quiet">尚未完成岗位体检</p>
          </div>

          <div v-if="targetJob.mastered.length || targetJob.toImprove.length" class="jobexplore-skills">
            <div v-if="targetJob.mastered.length" class="jobexplore-skills__row">
              <span class="jobexplore-skills__label">已掌握</span>
              <span class="jobexplore-skills__group">
                <span v-for="skill in targetJob.mastered" :key="skill" class="feature-status feature-status--mastered">
                  {{ skill }}
                </span>
              </span>
            </div>
            <div v-if="targetJob.toImprove.length" class="jobexplore-skills__row">
              <span class="jobexplore-skills__label">待提升</span>
              <span class="jobexplore-skills__group">
                <span v-for="skill in targetJob.toImprove" :key="skill" class="feature-status feature-status--weak">
                  {{ skill }}
                </span>
              </span>
            </div>
          </div>
          <p v-else class="jobexplore-note">{{ targetJob.advice }}</p>

          <div class="jobexplore-panel__foot">
            <button class="feature-button jobexplore-cta" type="button" @click="router.push('/jobs/hot')">
              查看岗位详情
            </button>
          </div>
        </article>
      </section>

      <section class="jobexplore-section">
        <div class="feature-section__head">
          <h2>适合你的岗位</h2>
          <button class="feature-link" type="button" @click="router.push('/jobs/hot')">查看全部 →</button>
        </div>

        <div v-if="fitJobs.length" class="jobexplore-fit">
          <article v-for="job in fitJobs" :key="job.id" class="feature-card jobexplore-fitcard">
            <div class="jobexplore-fitcard__head">
              <h3>{{ job.title }}</h3>
              <span class="jobexplore-badge">{{ job.matchRate }}% 匹配</span>
            </div>
            <div class="jobexplore-fitcard__skills">
              <span v-for="skill in job.skills" :key="skill" class="feature-chip">{{ skill }}</span>
            </div>
            <button class="feature-link jobexplore-fitcard__cta" type="button" @click="runSearch(job.title)">
              查看岗位 →
            </button>
          </article>
        </div>
        <p v-else class="feature-empty">更换目标岗位后，这里会显示与它匹配度更高、值得先投递的岗位方向。</p>
      </section>

      <section class="jobexplore-section">
        <article class="feature-card jobexplore-gap">
          <div class="feature-section__head">
            <h2>你与目标岗位还有哪些差距</h2>
          </div>

          <div v-if="targetJob.gaps.length" class="feature-list">
            <div v-for="gap in targetJob.gaps" :key="gap.name" class="feature-row jobexplore-gaprow">
              <div class="feature-row__copy">
                <strong>{{ gap.name }}</strong>
                <span>当前 {{ gap.current }} · 岗位要求 {{ gap.required }}</span>
              </div>
              <span class="jobexplore-bar" aria-hidden="true">
                <i :style="{ width: `${gapPercent(gap)}%` }"></i>
              </span>
            </div>
          </div>
          <p v-else class="jobexplore-note">完成岗位体检后，这里会列出需要补齐的能力。</p>

          <footer class="jobexplore-gap__foot">
            <p>当前建议：{{ targetJob.advice }}</p>
            <button
              class="feature-button feature-button--primary"
              type="button"
              @click="router.push('/interview/ai-career-plan')"
            >
              生成提升计划 →
            </button>
          </footer>
        </article>
      </section>
    </main>

    <Teleport to="body">
      <div v-if="isJobPickerOpen" class="jobexplore-modal" @click.self="closeJobPicker">
        <div class="jobexplore-modal__card" role="dialog" aria-modal="true" aria-label="选择目标岗位">
          <header class="jobexplore-modal__head">
            <h3>选择目标岗位</h3>
            <button class="jobexplore-modal__close" type="button" aria-label="关闭" @click="closeJobPicker">
              <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M6 6l12 12M18 6L6 18" /></svg>
            </button>
          </header>
          <div class="jobexplore-modal__body">
            <input
              v-model="jobKeyword"
              class="jobexplore-modal__search"
              type="search"
              placeholder="搜索岗位，例如：前端、算法、数据"
              aria-label="搜索岗位"
              @keyup.esc="closeJobPicker"
            />
            <ul class="jobexplore-modal__list">
              <li v-for="option in filteredJobOptions" :key="option.title">
                <button
                  class="jobexplore-modal__item"
                  :class="{ 'is-on': option.title === targetJob.title }"
                  type="button"
                  @click="selectTargetJob(option.title)"
                >
                  <span class="jobexplore-modal__name">{{ option.title }}</span>
                  <span class="jobexplore-modal__meta">{{ option.matchRate }}% 匹配</span>
                </button>
              </li>
              <li v-if="!filteredJobOptions.length" class="jobexplore-modal__empty">
                没有找到匹配的岗位，换个关键词试试
              </li>
            </ul>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
/* ---------- 顶部：标题 + 搜索 + 热门 ---------- */

.jobexplore-hero {
  display: grid;
  grid-template-columns: minmax(0, 0.86fr) minmax(0, 1.14fr);
  gap: 18px 32px;
  align-items: center;
  padding: 32px 34px 28px;
  border: 1px solid var(--hp-line);
  border-radius: 26px;
  background: var(--hp-pink-soft);
}

.jobexplore-hero__tools {
  min-width: 0;
}

.jobexplore-hero__copy h1 {
  margin: 0;
  color: var(--hp-ink);
  font-size: 30px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.jobexplore-hero__copy p {
  margin: 10px 0 0;
  color: #6f6154;
  font-size: 14.5px;
  line-height: 1.7;
}

.jobexplore-search {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 5px 5px 5px 18px;
  border: 1px solid var(--hp-line);
  border-radius: 999px;
  background: var(--hp-cream);
}

.jobexplore-search__icon {
  flex: 0 0 auto;
  width: 18px;
  height: 18px;
  fill: none;
  stroke: var(--hp-ink);
  stroke-width: 1.6;
  stroke-linecap: round;
}

.jobexplore-search input {
  flex: 1;
  min-width: 0;
  height: 42px;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--hp-ink);
  font-size: 14px;
}

.jobexplore-search input::placeholder {
  color: #a8a196;
}

.jobexplore-search button {
  flex: 0 0 auto;
  height: 42px;
  padding: 0 28px;
  border-radius: 999px;
  background: var(--hp-ink);
  color: var(--hp-cream);
  font-size: 14px;
  font-weight: 600;
}

.jobexplore-hot {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
  margin: 14px 0 0;
  font-size: 13px;
}

.jobexplore-hot__label {
  margin-right: 4px;
  color: #9a8a80;
  font-size: 12px;
  letter-spacing: 0.04em;
}

.jobexplore-hot button {
  padding: 0;
  border: 0;
  background: transparent;
  color: #6f6154;
  font-size: 13px;
  cursor: pointer;
}

.jobexplore-hot button:hover {
  color: var(--hp-ink);
  text-decoration: underline;
}

.jobexplore-hot i {
  color: #c0b0aa;
  font-style: normal;
}

/* ---------- 两块并排卡片 ---------- */

.jobexplore-top {
  display: grid;
  grid-template-columns: minmax(0, 1.12fr) minmax(0, 1fr);
  gap: var(--hp-gap);
  align-items: stretch;
  margin-top: var(--hp-gap);
}

.jobexplore-panel {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 26px;
  border-radius: var(--hp-r-lg);
}

.jobexplore-panel__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-top: auto;
}

.jobexplore-meta {
  color: var(--hp-muted);
  font-size: 12.5px;
}

.jobexplore-note {
  margin: 0;
  color: var(--hp-muted);
  font-size: 13px;
  line-height: 1.7;
}

.jobexplore-cta {
  flex: 0 0 auto;
}

/* ---------- 岗位星图 ---------- */

.jobexplore-starmap {
  position: relative;
  min-height: 390px;
  border: 1px solid rgba(23, 23, 23, 0.16);
  border-radius: var(--hp-r-md);
  background: #f7f2e8;
  overflow: hidden;
}

.jobexplore-starmap__orbit {
  position: absolute;
  top: 50%;
  left: 50%;
  border: 1px dashed rgba(23, 23, 23, 0.14);
  border-radius: 50%;
  transform: translate(-50%, -50%);
}

.jobexplore-starmap__orbit--one {
  width: min(62%, 300px);
  aspect-ratio: 1;
}

.jobexplore-starmap__orbit--two {
  width: min(88%, 430px);
  aspect-ratio: 1;
  border-color: rgba(23, 23, 23, 0.08);
}

.jobexplore-starmap__edges {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
}

.jobexplore-starmap__edges line {
  stroke: rgba(23, 23, 23, 0.22);
  stroke-dasharray: 4 4;
  stroke-width: 1;
}

.jobexplore-starmap__state {
  position: absolute;
  inset: 0;
  display: grid;
  margin: 0;
  place-items: center;
  color: var(--hp-muted);
  font-size: 13px;
}

.jobexplore-node {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--hp-ink);
  transform: translate(-50%, -50%);
  cursor: pointer;
}

.jobexplore-node__dot {
  display: grid;
  width: var(--node-size, 68px);
  height: var(--node-size, 68px);
  border: 1px solid var(--hp-line);
  border-radius: 50%;
  background-color: var(--hp-blue);
  background-position: center;
  background-size: cover;
  place-items: center;
  color: var(--hp-ink);
  font-size: 12px;
  font-weight: 700;
  transition: transform 0.18s ease;
}

.jobexplore-node.is-yellow .jobexplore-node__dot {
  background-color: var(--hp-yellow);
}

.jobexplore-node.is-blue .jobexplore-node__dot {
  background-color: var(--hp-blue);
}

.jobexplore-node.is-green .jobexplore-node__dot {
  background-color: var(--hp-green);
}

.jobexplore-node.is-pink .jobexplore-node__dot {
  background-color: var(--hp-pink);
}

.jobexplore-node:hover .jobexplore-node__dot {
  transform: translateY(-2px);
}

.jobexplore-node--center {
  top: 50%;
  left: 50%;
  z-index: 2;
  --node-size: 84px;
}

.jobexplore-node--center .jobexplore-node__dot {
  border-width: 2px;
  background-color: var(--hp-yellow);
  box-shadow: 0 0 0 6px rgba(251, 248, 242, 0.9);
}

.jobexplore-node__label {
  max-width: 92px;
  color: var(--hp-muted);
  font-size: 12px;
  line-height: 1.4;
  text-align: center;
}

.jobexplore-node__label--strong {
  color: var(--hp-ink);
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

/* ---------- 我的目标岗位 ---------- */

.jobexplore-target {
  display: flex;
  flex: 1;
  flex-direction: column;
  justify-content: center;
  gap: 12px;
  padding: 20px;
  border: 1px solid rgba(23, 23, 23, 0.16);
  border-radius: var(--hp-r-md);
  background: #f7f2e8;
}

.jobexplore-job {
  margin: 0;
  color: var(--hp-ink);
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.jobexplore-match {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  margin: 0;
  color: var(--hp-muted);
  font-size: 13px;
}

.jobexplore-match strong {
  color: var(--hp-ink);
  font-size: 24px;
  font-weight: 700;
  line-height: 1;
}

.jobexplore-match--quiet {
  justify-content: flex-start;
  color: #9a9388;
}

.jobexplore-meter {
  display: block;
  height: 10px;
  border: 1px solid rgba(23, 23, 23, 0.16);
  border-radius: 999px;
  overflow: hidden;
}

.jobexplore-meter i {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: var(--hp-yellow);
}

.jobexplore-skills {
  display: grid;
  gap: 12px;
}

.jobexplore-skills__row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.jobexplore-skills__label {
  flex: 0 0 auto;
  width: 52px;
  color: var(--hp-muted);
  font-size: 13px;
}

.jobexplore-skills__group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

/* ---------- 适合你的岗位 ---------- */

.jobexplore-section {
  margin-top: 40px;
}

.jobexplore-fit {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--hp-gap);
}

.jobexplore-fitcard {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 24px;
  border-radius: var(--hp-r-lg);
}

.jobexplore-fitcard__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.jobexplore-fitcard__head h3 {
  margin: 0;
  color: var(--hp-ink);
  font-size: 17px;
  font-weight: 600;
}

.jobexplore-badge {
  display: inline-flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 999px;
  background: var(--hp-yellow);
  color: var(--hp-ink);
  font-size: 12px;
  font-weight: 600;
  white-space: nowrap;
}

.jobexplore-fitcard__skills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.jobexplore-fitcard__cta {
  align-self: flex-start;
  margin-top: auto;
}

/* ---------- 能力差距 ---------- */

.jobexplore-gap {
  padding: 26px;
  border-radius: var(--hp-r-lg);
}

.jobexplore-gaprow {
  gap: 20px;
}

.jobexplore-bar {
  flex: 0 0 200px;
  height: 10px;
  border: 1px solid rgba(23, 23, 23, 0.16);
  border-radius: 999px;
  overflow: hidden;
}

.jobexplore-bar i {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: var(--hp-blue);
}

.jobexplore-gap__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  margin-top: 22px;
  padding-top: 20px;
  border-top: 1px solid rgba(23, 23, 23, 0.16);
}

.jobexplore-gap__foot p {
  margin: 0;
  color: var(--hp-muted);
  font-size: 13px;
}

/* ---------- 更换目标岗位 ---------- */

.jobexplore-edit {
  flex: 0 0 auto;
  min-height: 32px;
  padding: 0 15px;
  border: 1px solid var(--hp-line);
  border-radius: 999px;
  background: transparent;
  color: var(--hp-ink);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease;
}

.jobexplore-edit:hover {
  background: var(--hp-ink);
  color: var(--hp-cream);
}

.jobexplore-modal {
  position: fixed;
  inset: 0;
  z-index: 2200;
  display: grid;
  padding: 24px;
  place-items: center;
  background: rgba(23, 23, 23, 0.42);
}

.jobexplore-modal__card {
  display: flex;
  flex-direction: column;
  width: min(540px, 100%);
  max-height: min(640px, calc(100vh - 48px));
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-lg);
  background: var(--hp-cream);
  overflow: hidden;
}

.jobexplore-modal__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 20px 24px;
  border-bottom: 1px solid rgba(23, 23, 23, 0.16);
}

.jobexplore-modal__head h3 {
  margin: 0;
  color: var(--hp-ink);
  font-size: 18px;
  font-weight: 600;
}

.jobexplore-modal__close {
  display: grid;
  width: 34px;
  height: 34px;
  border: 1px solid rgba(23, 23, 23, 0.16);
  border-radius: 50%;
  background: transparent;
  color: var(--hp-muted);
  place-items: center;
  cursor: pointer;
}

.jobexplore-modal__close:hover {
  color: var(--hp-ink);
  background: rgba(23, 23, 23, 0.06);
}

.jobexplore-modal__close svg {
  width: 16px;
  height: 16px;
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-width: 2;
}

.jobexplore-modal__body {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 18px 24px 24px;
  overflow: auto;
}

.jobexplore-modal__search {
  height: 42px;
  padding: 0 16px;
  border: 1px solid rgba(23, 23, 23, 0.24);
  border-radius: 999px;
  outline: 0;
  background: transparent;
  color: var(--hp-ink);
  font-size: 14px;
}

.jobexplore-modal__search:focus {
  border-color: var(--hp-ink);
}

.jobexplore-modal__search::placeholder {
  color: #a8a196;
}

.jobexplore-modal__list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.jobexplore-modal__item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  width: 100%;
  padding: 14px 16px;
  border: 1px solid rgba(23, 23, 23, 0.16);
  border-radius: var(--hp-r-md);
  background: #f7f2e8;
  color: var(--hp-ink);
  text-align: left;
  cursor: pointer;
  transition: border-color 0.18s ease;
}

.jobexplore-modal__item:hover {
  border-color: var(--hp-line);
}

.jobexplore-modal__item.is-on {
  border-color: var(--hp-line);
  background: var(--hp-yellow);
}

.jobexplore-modal__name {
  font-size: 14px;
  font-weight: 600;
}

.jobexplore-modal__meta {
  flex: 0 0 auto;
  color: var(--hp-muted);
  font-size: 12px;
}

.jobexplore-modal__item.is-on .jobexplore-modal__meta {
  color: #6a5f3a;
}

.jobexplore-modal__empty {
  padding: 26px 0;
  color: var(--hp-muted);
  font-size: 13px;
  text-align: center;
}

@media (max-width: 980px) {
  .jobexplore-top,
  .jobexplore-fit {
    grid-template-columns: minmax(0, 1fr);
  }

  .jobexplore-hero {
    grid-template-columns: minmax(0, 1fr);
    gap: 20px;
    padding: 26px 20px 22px;
  }

  .jobexplore-hero__copy h1 {
    font-size: 26px;
  }
}

@media (max-width: 640px) {
  .jobexplore-search {
    flex-wrap: wrap;
    padding: 10px 12px;
    border-radius: 22px;
  }

  .jobexplore-search input {
    height: 36px;
  }

  .jobexplore-search button {
    width: 100%;
    height: 40px;
  }

  .jobexplore-panel__foot {
    flex-direction: column;
    align-items: flex-start;
  }

  .jobexplore-gaprow {
    flex-direction: column;
    align-items: stretch;
  }

  .jobexplore-bar {
    flex: 1 1 auto;
  }

  .jobexplore-gap__foot {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
