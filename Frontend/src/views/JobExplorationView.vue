<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'

import { getCareerFitJobs, getCareerJobFit, getCareerNebulaMap } from '../api/careerNebula'
import { resolveBossJobSearchLink } from '../api/jobRecommendations'
import AppTabBar from '../components/AppTabBar.vue'
import {
  JOB_PROFILES,
  TARGET_JOB_STORAGE_KEY,
  getJobDetailId,
  getJobDirection,
  getTargetProfile,
  readStoredTargetJob,
  resolveFitJobs,
} from '../data_tmp/jobCatalog'

const router = useRouter()

/* ============================================================
 * 岗位探索展示层数据
 * 1) 目标岗位与首页共用同一份本地选择（home_target_job），两处保持一致；
 * 2) 匹配度、已掌握/待提升、岗位差距与推荐岗位都来自 data/jobCatalog.js，
 *    岗位详情页读的是同一份数据，后端岗位匹配能力就绪后整体替换该文件即可；
 * 3) 岗位星图直接读取现有星图接口，节点图与名称都是真实岗位数据。
 * ============================================================ */

const searchKeyword = ref('')
const hotSearches = ['Python开发', 'AI算法', 'Java后端', '前端', '数据分析']

function runSearch(keyword) {
  const query = String(keyword ?? searchKeyword.value).trim()
  if (!query) return
  window.open(resolveBossJobSearchLink(query), '_blank', 'noopener,noreferrer')
}

const targetJobTitle = ref(readStoredTargetJob())

const targetJob = computed(() => getTargetProfile(targetJobTitle.value))

const fitJobs = computed(() => resolveFitJobs(targetJob.value))

/* ---------- 真实人岗匹配 ----------
 * 匹配度、已掌握、待提升与能力差距优先用后端 /api/app/career/job-fit：
 * 后端按「岗位技能要求 × 我在各技能上的学习记录等级」加权计算，可解释、可追溯。
 * 接口不可用或学习记录不足时，回落到页面内的展示数据，保证页面不空。 */

const remoteFit = ref(null)
const remoteFitJobs = ref([])

const fitData = computed(() => {
  const data = remoteFit.value
  if (!data || data.jobName !== targetJob.value.title) return null
  if (data.dataStatus === 'insufficient' || !data.totalSkills) return null
  return data
})

/** 页面统一读这份数据：有真实结果用真实结果，否则用展示层数据兜底。 */
const viewProfile = computed(() => {
  const base = targetJob.value
  const data = fitData.value
  const remote = remoteFit.value
  const remoteMatches = Boolean(remote) && remote.jobName === base.title

  if (!data && remoteMatches && remote.dataStatus === 'insufficient') {
    // 后端有结论但学习记录不足：不显示任何匹配度数字，只提示先积累证据。
    return {
      ...base,
      matchRate: null,
      mastered: [],
      toImprove: [],
      gaps: [],
      advice: remote.dataStatusText || base.advice,
      dataStatus: remote.dataStatus,
      dataStatusText: remote.dataStatusText,
      evidenceCount: remote.evidenceCount || 0,
    }
  }
  if (!data) {
    // 接口不可用：回落到展示层数据，保证页面结构完整。
    return {
      ...base,
      dataStatus: 'unavailable',
      dataStatusText: '',
      evidenceCount: 0,
    }
  }
  return {
    ...base,
    matchRate: data.matchRate,
    mastered: data.mastered.map((item) => item.skillName),
    toImprove: data.toImprove.map((item) => item.skillName),
    gaps: data.gaps.map((item) => ({
      name: item.skillName,
      current: item.current,
      required: item.required,
    })),
    advice: data.advice || base.advice,
    dataStatus: data.dataStatus,
    dataStatusText: data.dataStatusText,
    evidenceCount: data.evidenceCount,
  }
})

/** 「适合你的岗位」：后端排行优先，回落展示层数据。 */
const fitJobCards = computed(() => {
  if (!usableRemoteJobs.value.length) {
    // 后端有响应但学习记录不足：不展示任何匹配度，交给空状态提示。
    if (fitJobsLoaded.value) return []
    return fitJobs.value.map((job) => ({
      ...job,
      reasonText: `你的${job.strength}`,
      improveText: `需要提升：${job.improve}`,
    }))
  }
  return usableRemoteJobs.value.slice(0, 3).map((item) => ({
    id: getJobDetailId(item.jobName) || item.jobCode,
    title: item.jobName,
    direction: item.direction || '',
    type: item.type || '',
    matchRate: item.matchRate,
    skills: item.topSkills || [],
    reasonText: item.topStrength
      ? `你在「${item.topStrength}」上已经有基础`
      : '还没有足够的学习记录，先做题或完成课程章节',
    improveText: item.topGap ? `需要提升：${item.topGap}` : '继续完成课程与项目练习',
  }))
})

/** 有真实匹配结论的岗位；证据不足的岗位不参与排行展示。 */
const usableRemoteJobs = computed(() =>
  remoteFitJobs.value.filter((item) => item.dataStatus && item.dataStatus !== 'insufficient'))

/** 岗位排行接口是否成功返回（用于区分「接口不可用」和「证据不足」）。 */
const fitJobsLoaded = ref(false)

async function loadJobFit() {
  const jobName = targetJob.value.title
  const [fitRes, jobsRes] = await Promise.allSettled([
    getCareerJobFit(jobName),
    getCareerFitJobs(jobName, 10),
  ])
  remoteFit.value = fitRes.status === 'fulfilled' ? fitRes.value : null
  remoteFitJobs.value = jobsRes.status === 'fulfilled' ? jobsRes.value : []
  fitJobsLoaded.value = jobsRes.status === 'fulfilled'
}

/** 更换岗位弹窗里显示的真实匹配度；没有真实结果的岗位不显示数字。 */
const remoteMatchByJob = computed(() => {
  const map = new Map()
  if (remoteFit.value?.dataStatus && remoteFit.value.dataStatus !== 'insufficient') {
    map.set(remoteFit.value.jobName, remoteFit.value.matchRate)
  }
  usableRemoteJobs.value.forEach((item) => map.set(item.jobName, item.matchRate))
  return map
})

/* 「我的目标岗位」卡里的三条轻量信息，全部由现有数据推导，不额外维护一份。 */
const targetDirection = computed(() => getJobDirection(viewProfile.value.title))
const targetSkills = computed(() => [...viewProfile.value.mastered, ...viewProfile.value.toImprove].slice(0, 3))
const targetNextStep = computed(() => viewProfile.value.toImprove[0] || viewProfile.value.gaps[0]?.name || '')
const targetDetailId = computed(() => getJobDetailId(viewProfile.value.title))

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
  // 中心节点已经是目标岗位，环绕一圈只放其它岗位，避免同一个岗位在图上出现两次。
  const careers = nebulaCareers.value.filter((career) => !isTargetCareer(career.name)).slice(0, RING_COUNT)
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

/* 双层进度条里「岗位要求」和「我的能力」都按 0-100 分转换成宽度。 */
function barWidth(value) {
  return clamp(Number(value) || 0, 0, 100)
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

onMounted(() => {
  loadNebula()
  loadJobFit()
})

/* 更换目标岗位后重新拉取匹配结果 */
watch(targetJobTitle, loadJobFit)
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
            <h2>职业路径图谱</h2>
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
                :aria-label="`进入职业路径图谱，我的目标岗位 ${centerNode.name}`"
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
                :aria-label="`进入职业路径图谱查看${node.name}`"
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
            <p class="jobexplore-note">看见岗位之间的关系，也看见你的下一步</p>
            <button
              class="feature-button feature-button--primary jobexplore-cta"
              type="button"
              @click="router.push('/career/nebula')"
            >
              进入职业路径图谱 →
            </button>
          </div>
        </article>

        <article class="feature-card jobexplore-panel jobexplore-panel--target">
          <div class="feature-section__head">
            <h2>我的目标岗位</h2>
            <button class="jobexplore-edit" type="button" @click="openJobPicker">更换</button>
          </div>

          <div class="jobexplore-target">
            <p class="jobexplore-job">{{ viewProfile.title }}</p>
            <template v-if="viewProfile.matchRate">
              <div class="jobexplore-match">
                <span>当前匹配度</span>
                <strong>{{ viewProfile.matchRate }}%</strong>
              </div>
              <span class="jobexplore-meter" aria-hidden="true">
                <i :style="{ width: `${viewProfile.matchRate}%` }"></i>
              </span>
              <p class="jobexplore-note">{{ viewProfile.dataStatusText }}</p>
            </template>
            <p v-else class="jobexplore-match jobexplore-match--quiet">
              {{ viewProfile.dataStatusText || '尚未完成岗位体检' }}
            </p>
          </div>

          <dl class="jobexplore-facts">
            <div v-if="targetDirection" class="jobexplore-facts__row">
              <dt>岗位方向</dt>
              <dd>{{ targetDirection }}</dd>
            </div>
            <div v-if="targetSkills.length" class="jobexplore-facts__row">
              <dt>核心技能</dt>
              <dd>{{ targetSkills.join(' / ') }}</dd>
            </div>
            <div v-if="targetNextStep" class="jobexplore-facts__row">
              <dt>下一步</dt>
              <dd>优先补齐 {{ targetNextStep }}</dd>
            </div>
          </dl>
          <p v-if="!viewProfile.matchRate" class="jobexplore-note">{{ viewProfile.advice }}</p>

          <div class="jobexplore-panel__foot jobexplore-panel__foot--actions">
            <RouterLink
              v-if="targetDetailId"
              class="feature-button jobexplore-cta"
              :to="`/career/job/${targetDetailId}`"
            >
              岗位画像 →
            </RouterLink>
            <button class="feature-button feature-button--primary jobexplore-cta" type="button" @click="router.push('/employment')">
              查看在招岗位
            </button>
          </div>
        </article>
      </section>

      <section class="jobexplore-section">
        <div class="feature-section__head">
          <h2>适合你的岗位</h2>
          <button class="feature-link" type="button" @click="router.push('/employment')">查看全部 →</button>
        </div>

        <div v-if="fitJobCards.length" class="jobexplore-fit">
          <RouterLink
            v-for="job in fitJobCards"
            :key="job.id"
            class="jobexplore-fitcard"
            :to="`/career/job/${job.id}`"
          >
            <div class="jobexplore-fitcard__head">
              <h3>{{ job.title }}</h3>
              <span class="jobexplore-fitcard__rate">{{ job.matchRate }}%</span>
            </div>
            <p class="jobexplore-fitcard__meta">{{ job.direction }} · {{ job.type }}</p>

            <div class="jobexplore-fitcard__block">
              <span class="jobexplore-fitcard__label">核心技能</span>
              <div class="jobexplore-fitcard__skills">
                <span v-for="skill in job.skills" :key="skill" class="feature-chip">{{ skill }}</span>
              </div>
            </div>

            <p class="jobexplore-fitcard__reason">
              <span>推荐理由</span>{{ job.reasonText }}
            </p>
            <p class="jobexplore-fitcard__weak">{{ job.improveText }}</p>

            <span class="feature-link jobexplore-fitcard__cta">查看岗位详情 →</span>
          </RouterLink>
        </div>
        <p v-else class="feature-empty">
          {{
            fitJobsLoaded
              ? '还没有足够的学习记录。先做题或完成课程章节，这里会按你的真实水平推荐岗位。'
              : '更换目标岗位后，这里会显示与它匹配度更高、值得先投递的岗位方向。'
          }}
        </p>
      </section>

      <section class="jobexplore-section">
        <article class="feature-card jobexplore-gap">
          <div class="feature-section__head">
            <h2>你与目标岗位的差距</h2>
          </div>

          <div v-if="viewProfile.gaps.length" class="jobexplore-gaps">
            <div v-for="gap in viewProfile.gaps" :key="gap.name" class="jobexplore-gaprow">
              <div class="jobexplore-gaprow__head">
                <strong>{{ gap.name }}</strong>
                <span>我的 {{ gap.current }} · 岗位 {{ gap.required }}</span>
              </div>
              <span class="jobexplore-gapbar" aria-hidden="true">
                <i class="jobexplore-gapbar__require" :style="{ width: `${barWidth(gap.required)}%` }"></i>
                <i class="jobexplore-gapbar__mine" :style="{ width: `${barWidth(gap.current)}%` }"></i>
              </span>
            </div>
          </div>
          <p v-else class="jobexplore-note">完成岗位体检后，这里会列出需要补齐的能力。</p>

          <footer class="jobexplore-gap__foot">
            <div>
              <p v-if="viewProfile.gaps.length" class="jobexplore-gap__summary">
                已识别 {{ viewProfile.gaps.length }} 项关键能力缺口
              </p>
              <p class="jobexplore-gap__advice">当前建议：{{ viewProfile.advice }}</p>
            </div>
            <button
              class="feature-button feature-button--primary jobexplore-cta"
              type="button"
              @click="router.push({ path: '/learning', query: { tab: 'practice', plan: 'gaps' } })"
            >
              生成我的提升计划 →
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
                  <span v-if="remoteMatchByJob.has(option.title)" class="jobexplore-modal__meta">
                    {{ remoteMatchByJob.get(option.title) }}% 匹配
                  </span>
                  <span v-else-if="!fitJobsLoaded" class="jobexplore-modal__meta">
                    {{ option.matchRate }}% 匹配
                  </span>
                  <span v-else class="jobexplore-modal__meta">待积累学习记录</span>
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
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 14px 32px;
  padding: 20px 28px;
  border: 1px solid var(--hp-line);
  border-radius: 22px;
  background: var(--hp-pink-soft);
}

.jobexplore-hero__tools {
  flex: 1 1 420px;
  max-width: 640px;
  min-width: 0;
}

.jobexplore-hero__copy h1 {
  margin: 0;
  color: var(--hp-ink);
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.jobexplore-hero__copy p {
  margin: 6px 0 0;
  color: #6f6154;
  font-size: 13px;
  line-height: 1.6;
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
  grid-template-columns: minmax(0, 0.85fr) minmax(0, 1.15fr);
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

.jobexplore-panel__foot--actions {
  flex-wrap: wrap;
  justify-content: flex-start;
  gap: 10px;
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

/* 目标岗位卡里的三条轻量信息：岗位方向 / 核心技能 / 下一步 */

.jobexplore-facts {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
}

.jobexplore-facts__row {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr);
  gap: 12px;
  align-items: baseline;
}

.jobexplore-facts dt {
  color: var(--hp-muted);
  font-size: 12.5px;
}

.jobexplore-facts dd {
  margin: 0;
  color: var(--hp-ink);
  font-size: 13.5px;
  line-height: 1.6;
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
  gap: 12px;
  padding: 22px;
  border: 1px solid transparent;
  border-radius: var(--hp-r-lg);
  background: var(--hp-surface-2);
  color: var(--hp-ink);
  text-decoration: none;
  cursor: pointer;
  transition: background 0.18s ease, border-color 0.18s ease, transform 0.18s ease;
}

.jobexplore-fitcard:hover {
  border-color: var(--hp-line);
  background: var(--hp-cream);
  transform: translateY(-2px);
}

.jobexplore-fitcard__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.jobexplore-fitcard__head h3 {
  margin: 0;
  color: var(--hp-ink);
  font-size: 17px;
  font-weight: 600;
}

.jobexplore-fitcard__rate {
  flex: 0 0 auto;
  color: var(--hp-ink);
  font-size: 20px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: -0.01em;
  white-space: nowrap;
}

.jobexplore-fitcard__meta {
  margin: -4px 0 0;
  color: var(--hp-muted);
  font-size: 12.5px;
}

.jobexplore-fitcard__block {
  display: grid;
  gap: 8px;
}

.jobexplore-fitcard__label {
  color: var(--hp-muted);
  font-size: 12px;
  letter-spacing: 0.04em;
}

.jobexplore-fitcard__reason {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin: 0;
  padding: 10px 12px;
  border-radius: 12px;
  background: var(--hp-blue);
  color: var(--hp-ink);
  font-size: 13px;
  font-weight: 600;
}

.jobexplore-fitcard__reason span {
  font-size: 11.5px;
  font-weight: 600;
  opacity: 0.72;
}

.jobexplore-fitcard__weak {
  margin: 0;
  color: var(--hp-muted);
  font-size: 12px;
}

.jobexplore-fitcard__skills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

/* 核心技能是每张卡的重点，用页面强调色填充，与下面两段文字区分开 */
.jobexplore-fitcard__skills .feature-chip {
  padding: 6px 13px;
  border-color: var(--hp-line);
  color: var(--hp-ink);
  background: var(--hp-yellow);
  font-weight: 600;
  cursor: default;
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

/* 双层进度条：浅灰是岗位要求，彩色是我的能力，露出来的灰段就是差距 */

.jobexplore-gaps {
  display: grid;
  gap: 12px;
}

.jobexplore-gaprow {
  display: grid;
  gap: 10px;
  padding: 14px 16px;
  border-radius: var(--hp-r-md);
  background: var(--hp-surface-2);
}

.jobexplore-gaprow__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
}

.jobexplore-gaprow__head strong {
  color: var(--hp-ink);
  font-size: 14.5px;
  font-weight: 600;
}

.jobexplore-gaprow__head span {
  color: var(--hp-muted);
  font-size: 12.5px;
}

.jobexplore-gapbar {
  position: relative;
  display: block;
  height: 12px;
}

.jobexplore-gapbar__require,
.jobexplore-gapbar__mine {
  position: absolute;
  top: 0;
  left: 0;
  height: 100%;
  border-radius: 999px;
}

.jobexplore-gapbar__require {
  background: var(--hp-line);
}

.jobexplore-gapbar__mine {
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

.jobexplore-gap__foot > div {
  min-width: 0;
}

.jobexplore-gap__summary {
  margin: 0;
  color: var(--hp-ink);
  font-size: 14px;
  font-weight: 600;
}

.jobexplore-gap__advice {
  margin: 6px 0 0;
  color: var(--hp-muted);
  font-size: 12.5px;
  line-height: 1.7;
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
    align-items: stretch;
    padding: 20px;
  }

  .jobexplore-hero__copy h1 {
    font-size: 24px;
  }

  .jobexplore-hero__tools {
    flex: 1 1 100%;
    max-width: none;
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

  .jobexplore-gaprow__head {
    flex-direction: column;
    align-items: flex-start;
    gap: 4px;
  }

  .jobexplore-gap__foot {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
