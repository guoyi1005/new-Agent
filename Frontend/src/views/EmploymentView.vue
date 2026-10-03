<script setup>
import { computed, nextTick, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import { resolveBossJobSearchLink } from '../api/jobRecommendations'
import { getLocalJobSummary } from '../api/localJobs'
import { getCampusRecruitmentSummary, getEmploymentAlumni } from '../api/employment'
import AppTabBar from '../components/AppTabBar.vue'
import {
  EMPLOYMENT_AGGREGATE,
  EMPLOYMENT_ALUMNI,
  EMPLOYMENT_CAMPUS,
  EMPLOYMENT_JOBS,
  EMPLOYMENT_LOCAL,
  EMPLOYMENT_RADAR,
  EMPLOYMENT_SOURCE_NAMES,
  EMPLOYMENT_TABS,
} from '../data_tmp/employmentCatalog'
import { getTargetProfile, readStoredTargetJob } from '../data_tmp/jobCatalog'

/* 实习就业：顶部搜索 + 页签，下面依次是实习雷达 / 成都本地、推荐岗位、多平台聚合、校招与校友企业。
 * 页签和搜索框都作用于「为你推荐的岗位」这一份列表。 */

const router = useRouter()

const keyword = ref('')
const activeTab = ref('all')
const showAllJobs = ref(false)
const jobsSectionRef = ref(null)

/** 页内的「查看岗位」类按钮统一滚到推荐岗位列表，并按需要切到对应页签。 */
function goToJobs(tab = 'all') {
  activeTab.value = tab
  showAllJobs.value = false
  nextTick(() => {
    jobsSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  })
}

const targetJobTitle = ref(readStoredTargetJob())
const targetJob = computed(() => getTargetProfile(targetJobTitle.value))

/* 「为你推荐的岗位」用页面内的推荐数据（含匹配度 / 核心技能 / 推荐理由）；
 * 抓取到的真实岗位只参与「成都本地就业」的统计与来源标注，不直接渲染成卡片。 */
function toSampleJobCard(job) {
  return { ...job, salary: '', meta: `来源 ${job.sources} 个平台 · ${job.updated}`, detailUrl: '' }
}

const allJobs = computed(() => EMPLOYMENT_JOBS.map(toSampleJobCard))

/* 「成都本地就业」用后端每天抓取的本地岗位统计；接口没有数据时退回页面内的展示数据，
 * 保证板块不会是空的，同时在下面注明当前用的是哪一份数据。 */
const localSummary = ref(null)

const localStats = computed(() => {
  const data = localSummary.value
  if (!data || !data.total) {
    return {
      todayNew: EMPLOYMENT_LOCAL.todayNew,
      intern: EMPLOYMENT_LOCAL.intern,
      campus: EMPLOYMENT_LOCAL.campus,
      stateOwned: EMPLOYMENT_LOCAL.stateOwned,
    }
  }
  return {
    todayNew: data.todayNew ?? 0,
    intern: data.intern ?? 0,
    campus: data.campus ?? 0,
    stateOwned: data.stateOwned ?? 0,
  }
})

const localDistricts = computed(() => {
  const list = localSummary.value?.districts
  if (!Array.isArray(list) || !list.length) {
    return EMPLOYMENT_LOCAL.districts.map((name) => ({ name, total: 0 }))
  }
  return list
})

const localMeta = computed(() => {
  const data = localSummary.value
  if (!data || !data.total) {
    return '本地岗位数据暂未接入，下面先显示示例数据'
  }
  // 平台名由后端按全表统计返回，避免只取最新若干条时漏掉某个站
  const platforms = (data.platforms || []).map((key) => EMPLOYMENT_SOURCE_NAMES[key] || key)
  const sources = platforms.length
    ? platforms.join(' / ')
    : (data.sources || []).map((item) => item.name).filter(Boolean).join(' / ')
  const updated = formatUpdatedAt(data.updatedAt)
  return `数据来自 ${sources} · 共收录 ${data.total} 条 · 每天自动更新${updated ? ` · 更新于 ${updated}` : ''}`
})

function formatUpdatedAt(value) {
  if (!value) return ''
  const text = String(value).replace('T', ' ')
  return text.slice(5, 16)
}

async function loadLocalSummary() {
  try {
    const result = await getLocalJobSummary()
    localSummary.value = result?.data || null
  } catch {
    localSummary.value = null
  }
}

/* 「校园招聘 / 校友企业」优先用管理端维护的数据；
 * 接口没有数据时退回页面内的展示数据，保证板块不会是空的。 */
const campusSummary = ref(null)
const alumniList = ref([])

function formatMonthDay(value) {
  if (!value) return ''
  const parts = String(value).slice(0, 10).split('-')
  if (parts.length < 3) return String(value)
  return `${Number(parts[1])}.${Number(parts[2])}`
}

const campusSection = computed(() => {
  const data = campusSummary.value
  if (!data || (!data.talks && !data.fairs && !data.roles)) {
    return EMPLOYMENT_CAMPUS
  }
  return {
    season: data.season || EMPLOYMENT_CAMPUS.season,
    talks: data.talks ?? 0,
    fairs: data.fairs ?? 0,
    roles: data.roles ?? 0,
    schedule: (data.schedule || []).map((item) => ({
      date: formatMonthDay(item.date),
      title: item.title,
    })),
  }
})

const alumniSection = computed(() => {
  const list = alumniList.value
  if (!Array.isArray(list) || !list.length) {
    return EMPLOYMENT_ALUMNI
  }
  const fields = [...new Set(
    list.flatMap((item) => String(item.fields || '').split(','))
      .map((field) => field.trim())
      .filter(Boolean),
  )]
  return {
    companies: list.length,
    hiring: list.filter((item) => item.hiring).length,
    fields: fields.length ? fields.slice(0, 3) : EMPLOYMENT_ALUMNI.fields,
    alumniAtWork: list.reduce((sum, item) => sum + (Number(item.alumniCount) || 0), 0),
    openNow: list.reduce((sum, item) => sum + (Number(item.openPositions) || 0), 0),
    partners: list.map((item) => ({
      name: item.name,
      short: item.shortName || companyMark(item.name),
    })),
  }
})

async function loadEmploymentSections() {
  const [campusRes, alumniRes] = await Promise.allSettled([
    getCampusRecruitmentSummary(),
    getEmploymentAlumni(),
  ])
  campusSummary.value = campusRes.status === 'fulfilled' ? campusRes.value?.data || null : null
  const alumniData = alumniRes.status === 'fulfilled' ? alumniRes.value?.data : null
  alumniList.value = Array.isArray(alumniData) ? alumniData : []
}

onMounted(() => {
  loadLocalSummary()
  loadEmploymentSections()
})

const visibleJobs = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  return allJobs.value.filter((job) => {
    if (activeTab.value !== 'all' && !job.tags.includes(activeTab.value)) return false
    if (!key) return true
    return [job.title, job.company, job.city, job.district, job.meta, ...(job.skills || [])]
      .join(' ')
      .toLowerCase()
      .includes(key)
  })
})

/* 「重点推荐 → 更多机会」：匹配度最高的 3 个做成大卡，其余用轻量小卡 */
const featuredJobs = computed(() => visibleJobs.value.slice(0, 3))
const moreJobs = computed(() => (showAllJobs.value ? visibleJobs.value.slice(3) : visibleJobs.value.slice(3, 9)))

/* 雷达图：取匹配度最高的 5 个岗位，匹配越高离中心越近 */
const radarPoints = computed(() => {
  const jobs = [...EMPLOYMENT_JOBS].sort((a, b) => b.matchRate - a.matchRate).slice(0, 5)
  const total = jobs.length || 1
  return jobs.map((job, index) => {
    const angle = ((-90 + (360 / total) * index + 18) * Math.PI) / 180
    const radius = 0.3 + (1 - job.matchRate / 100) * 0.62
    return {
      id: job.id,
      title: job.title,
      matchRate: job.matchRate,
      x: 50 + Math.cos(angle) * radius * 46,
      y: 50 + Math.sin(angle) * radius * 46,
      alignLeft: Math.cos(angle) < 0,
    }
  })
})

const radarCenterLabel = computed(() => String(targetJobTitle.value).replace(/工程师/g, '').trim() || targetJobTitle.value)

/**
 * 岗位卡片跳真实招聘搜索。
 * 搜索词由「卡片上的岗位名 + 城市 + 区」拼成，跟卡片描述保持一致；
 * 卡片右上角的校招 / 校友企业 / 远程这类标注不进搜索词，避免搜不到结果。
 */
function searchKeywordOf(job) {
  return [String(job.title).replace(/（[^）]*）/g, '').trim(), job.city, job.district]
    .filter((item) => item && String(item).trim())
    .join(' ')
    .trim()
}

function searchJob(job) {
  if (job.detailUrl) {
    window.open(job.detailUrl, '_blank', 'noopener,noreferrer')
    return
  }
  window.open(resolveBossJobSearchLink(searchKeywordOf(job)), '_blank', 'noopener,noreferrer')
}

/** 按钮提示：有原始岗位链接就直接打开，没有才走平台搜索。 */
function jobActionHint(job) {
  return job.detailUrl ? '打开招聘网站上的原始岗位页面' : `在招聘平台搜索：${searchKeywordOf(job)}`
}

/** 企业标识：用单位名的前两个字做小徽标，不引入外部图片。 */
function companyMark(company = '') {
  return String(company).replace(/^成都/, '').slice(0, 2) || '企业'
}

/** 城市 · 区，没有区时只显示城市。 */
function placeText(job) {
  return job.district ? `${job.city} · ${job.district}` : job.city
}

function runSearch() {
  const query = keyword.value.trim()
  if (!query) return
  window.open(resolveBossJobSearchLink(query), '_blank', 'noopener,noreferrer')
}
</script>

<template>
  <div class="feature-page">
    <AppTabBar />

    <main class="feature-container employment">
      <section class="employment-hero">
        <div class="employment-hero__copy">
          <h1>实习就业</h1>
          <p>把你的能力，连接到真实的实习与就业机会</p>
        </div>

        <form class="employment-search" @submit.prevent="runSearch()">
          <svg class="employment-search__icon" viewBox="0 0 24 24" aria-hidden="true">
            <circle cx="11" cy="11" r="6.5" />
            <path d="M16 16.2 20.4 20.6" />
          </svg>
          <input
            v-model="keyword"
            type="search"
            placeholder="搜索岗位 / 公司 / 城市 / 技能"
            aria-label="搜索岗位 / 公司 / 城市 / 技能"
          />
          <button type="submit">搜索</button>
        </form>

        <div class="employment-tabs">
          <button
            v-for="tab in EMPLOYMENT_TABS"
            :key="tab.id"
            type="button"
            class="employment-tab"
            :class="{ 'is-on': activeTab === tab.id }"
            @click="activeTab = tab.id"
          >
            {{ tab.label }}
          </button>
        </div>
      </section>

      <section class="employment-top">
        <article class="feature-card employment-radar">
          <h2>实习雷达</h2>
          <div class="employment-radar__body">
            <div class="employment-radar__info">
              <p class="employment-radar__desc">根据你的目标岗位和当前能力，为你寻找更适合当前阶段的机会</p>

              <dl class="employment-radar__facts">
                <div>
                  <dt>目标岗位</dt>
                  <dd>{{ targetJobTitle }}</dd>
                </div>
                <div>
                  <dt>当前匹配</dt>
                  <dd>{{ targetJob.matchRate ? `${targetJob.matchRate}%` : '尚未完成岗位体检' }}</dd>
                </div>
              </dl>

              <div class="employment-radar__stats">
                <p>已发现 <strong>{{ EMPLOYMENT_RADAR.found }}</strong> 个匹配机会</p>
                <span>{{ EMPLOYMENT_RADAR.high }} 个高匹配 · {{ EMPLOYMENT_RADAR.priority }} 个建议优先关注</span>
              </div>

              <div class="employment-card__foot">
                <button class="feature-button feature-button--primary" type="button" @click="goToJobs('all')">
                  查看我的机会 →
                </button>
              </div>
            </div>

            <div class="employment-radar__visual" aria-hidden="true">
              <span class="employment-radar__ring employment-radar__ring--out"></span>
              <span class="employment-radar__ring employment-radar__ring--mid"></span>
              <span class="employment-radar__sweep"></span>
              <span class="employment-radar__center">{{ radarCenterLabel }}</span>
              <span
                v-for="point in radarPoints"
                :key="point.id"
                class="employment-radar__dot"
                :class="{ 'is-left': point.alignLeft }"
                :style="{ left: `${point.x}%`, top: `${point.y}%` }"
                :title="point.title"
              >
                <i></i><em>{{ point.matchRate }}%</em>
              </span>
            </div>
          </div>
        </article>

        <article class="feature-card employment-local">
          <h2>成都本地就业</h2>
          <ul class="employment-local__stats">
            <li><span>今日新增</span><strong>{{ localStats.todayNew }}</strong></li>
            <li><span>实习岗位</span><strong>{{ localStats.intern }}</strong></li>
            <li><span>校招岗位</span><strong>{{ localStats.campus }}</strong></li>
            <li><span>国企岗位</span><strong>{{ localStats.stateOwned }}</strong></li>
          </ul>
          <p class="employment-local__districts">
            <span v-for="district in localDistricts" :key="district.name">{{ district.name }}</span>
          </p>
          <ul class="employment-local__hotspots">
            <li v-for="item in EMPLOYMENT_LOCAL.hotspots" :key="item.district">
              <strong>{{ item.district }}</strong>
              <span>{{ item.direction }}</span>
            </li>
          </ul>
          <p class="employment-local__meta">{{ localMeta }}</p>
          <div class="employment-card__foot">
            <button class="feature-button" type="button" @click="goToJobs('local')">进入成都专区 →</button>
          </div>
        </article>
      </section>

      <section ref="jobsSectionRef" class="employment-section">
        <div class="feature-section__head">
          <h2>为你推荐的岗位</h2>
          <button
            v-if="visibleJobs.length > 9 || showAllJobs"
            class="feature-link"
            type="button"
            @click="showAllJobs = !showAllJobs"
          >
            {{ showAllJobs ? '收起' : `查看全部（${visibleJobs.length} 条）` }}
          </button>
        </div>


        <div v-if="visibleJobs.length" class="employment-jobs">
          <template v-if="featuredJobs.length">
            <p class="employment-jobs__label">重点推荐</p>
            <div class="employment-jobs__featured">
              <article v-for="job in featuredJobs" :key="job.id" class="feature-card employment-job employment-job--featured">
                <div class="employment-job__head">
                  <span class="employment-job__logo" aria-hidden="true">{{ companyMark(job.company) }}</span>
                  <div class="employment-job__title">
                    <h3>
                      {{ job.title }}
                      <span v-if="job.tag" class="employment-job__tag">{{ job.tag }}</span>
                    </h3>
                    <p>{{ job.company }}</p>
                  </div>
                  <span v-if="job.matchRate" class="employment-job__rate">{{ job.matchRate }}%</span>
                  <span v-else-if="job.salary" class="employment-job__salary">{{ job.salary }}</span>
                </div>
                <p class="employment-job__place">{{ placeText(job) }}</p>
                <div v-if="job.skills && job.skills.length" class="employment-job__skills">
                  <span v-for="skill in job.skills" :key="skill" class="feature-chip">{{ skill }}</span>
                </div>
                <div v-if="job.reason" class="employment-job__reason">
                  <span>为什么推荐你？</span>
                  <p>{{ job.reason }}</p>
                </div>
                <p v-if="job.meta" class="employment-job__meta">{{ job.meta }}</p>
                <button
                  class="feature-link employment-job__cta"
                  type="button"
                  :title="jobActionHint(job)"
                  @click="searchJob(job)"
                >
                  查看岗位 →
                </button>
              </article>
            </div>
          </template>

          <template v-if="moreJobs.length">
            <p class="employment-jobs__label employment-jobs__label--quiet">更多机会</p>
            <div class="employment-jobs__more">
              <article v-for="job in moreJobs" :key="job.id" class="employment-job employment-job--compact">
                <div class="employment-job__compact-head">
                  <h4>
                    {{ job.title }}
                    <span v-if="job.tag" class="employment-job__tag">{{ job.tag }}</span>
                  </h4>
                  <span v-if="job.matchRate">{{ job.matchRate }}%</span>
                  <span v-else-if="job.salary">{{ job.salary }}</span>
                </div>
                <p class="employment-job__compact-meta">{{ job.company }} · {{ placeText(job) }}</p>
                <div v-if="job.skills && job.skills.length" class="employment-job__skills">
                  <span v-for="skill in job.skills" :key="skill" class="feature-chip">{{ skill }}</span>
                </div>
                <p v-if="job.meta" class="employment-job__meta">{{ job.meta }}</p>
                <button
                  class="feature-link employment-job__cta"
                  type="button"
                  :title="jobActionHint(job)"
                  @click="searchJob(job)"
                >
                  查看岗位 →
                </button>
              </article>
            </div>
          </template>
        </div>
        <p v-else class="feature-empty">没有匹配的岗位，换个关键词或切换上面的页签试试。</p>
      </section>

      <section class="employment-section">
        <article class="feature-card employment-aggregate">
          <div class="employment-aggregate__info">
            <h2>多平台岗位聚合</h2>
            <p class="employment-aggregate__desc">同一个岗位在多个平台出现时自动合并成一条，不用来回翻</p>
            <ul class="employment-aggregate__points">
              <li v-for="point in EMPLOYMENT_AGGREGATE.points" :key="point">{{ point }}</li>
            </ul>
          </div>

          <div class="employment-aggregate__flow">
            <div class="employment-aggregate__side">
              <span v-for="source in EMPLOYMENT_AGGREGATE.sources.slice(0, 2)" :key="source">
                <i aria-hidden="true">{{ source.slice(0, 1) }}</i>{{ source }}
              </span>
            </div>
            <span class="employment-aggregate__arrow" aria-hidden="true">→</span>
            <p class="employment-aggregate__job">{{ EMPLOYMENT_AGGREGATE.job }}</p>
            <span class="employment-aggregate__arrow" aria-hidden="true">←</span>
            <div class="employment-aggregate__side">
              <span>
                <i aria-hidden="true">{{ EMPLOYMENT_AGGREGATE.sources[2].slice(0, 1) }}</i>{{ EMPLOYMENT_AGGREGATE.sources[2] }}
              </span>
            </div>
          </div>

          <ul class="employment-aggregate__facts">
            <li v-for="fact in EMPLOYMENT_AGGREGATE.facts" :key="fact.label">
              <span>{{ fact.label }}</span>
              <strong>{{ fact.value }}</strong>
            </li>
          </ul>
        </article>
      </section>

      <section class="employment-bottom">
        <article class="feature-card employment-campus">
          <h2>校园招聘</h2>
          <p class="employment-campus__season">{{ campusSection.season }}</p>
          <ul class="employment-campus__list">
            <li>本周 {{ campusSection.talks }} 场宣讲</li>
            <li>{{ campusSection.fairs }} 场双选会</li>
            <li>{{ campusSection.roles }} 个校招岗位</li>
          </ul>
          <ol class="employment-campus__timeline">
            <li v-for="item in campusSection.schedule" :key="item.title">
              <span class="employment-campus__dot" aria-hidden="true"></span>
              <strong>{{ item.date }}</strong>
              <span>{{ item.title }}</span>
            </li>
          </ol>
          <div class="employment-card__foot">
            <button class="feature-button" type="button" @click="goToJobs('campus')">查看校招岗位 →</button>
          </div>
        </article>

        <article class="feature-card employment-alumni">
          <h2>校友企业</h2>
          <p class="employment-alumni__count">{{ alumniSection.companies }} 家校友企业</p>
          <p class="employment-alumni__hiring">{{ alumniSection.hiring }} 家正在招聘</p>
          <p class="employment-alumni__note">本校校友在职</p>
          <div class="employment-alumni__partners">
            <span
              v-for="partner in alumniSection.partners"
              :key="partner.name"
              class="employment-alumni__logo"
              :title="partner.name"
            >
              {{ partner.short }}
            </span>
          </div>
          <p class="employment-alumni__stats">
            <span>本校校友 {{ alumniSection.alumniAtWork }} 人在职</span>
            <span>当前开放 {{ alumniSection.openNow }} 个岗位</span>
          </p>
          <p class="employment-alumni__fields">
            <span v-for="field in alumniSection.fields" :key="field">{{ field }}</span>
          </p>
          <div class="employment-card__foot">
            <button class="feature-button" type="button" @click="router.push('/community')">查看校友企业 →</button>
          </div>
        </article>
      </section>
    </main>
  </div>
</template>

<style scoped>
.employment {
  display: block;
}

/* ---------- 顶部：标题 + 搜索 + 页签 ---------- */

.employment-hero {
  display: grid;
  gap: 18px;
  padding: 26px 30px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-lg);
  background: var(--hp-pink-soft);
}

.employment-hero__copy h1 {
  margin: 0;
  color: var(--hp-ink);
  font-size: 30px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.employment-hero__copy p {
  margin: 8px 0 0;
  color: #6f6154;
  font-size: 14px;
  line-height: 1.7;
}

.employment-search {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 5px 5px 5px 18px;
  border: 1px solid var(--hp-line);
  border-radius: 999px;
  background: var(--hp-cream);
}

.employment-search__icon {
  flex: 0 0 auto;
  width: 18px;
  height: 18px;
  fill: none;
  stroke: var(--hp-ink);
  stroke-width: 1.6;
  stroke-linecap: round;
}

.employment-search input {
  flex: 1;
  min-width: 0;
  height: 42px;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--hp-ink);
  font-size: 14px;
}

.employment-search input::placeholder {
  color: #a8a196;
}

.employment-search button {
  flex: 0 0 auto;
  height: 42px;
  padding: 0 28px;
  border-radius: 999px;
  background: var(--hp-ink);
  color: var(--hp-cream);
  font-size: 14px;
  font-weight: 600;
}

.employment-tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.employment-tab {
  min-height: 32px;
  padding: 0 16px;
  border: 1px solid var(--hp-line);
  border-radius: 999px;
  background: transparent;
  color: var(--hp-ink-2);
  font-size: 13px;
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease, border-color 0.18s ease;
}

.employment-tab:hover {
  border-color: var(--hp-line-strong);
}

.employment-tab.is-on {
  border-color: var(--hp-ink);
  background: var(--hp-ink);
  color: var(--hp-cream);
}

/* ---------- 通用卡片 ---------- */

.employment h2 {
  margin: 0;
  color: var(--hp-ink);
  font-size: 18px;
  font-weight: 600;
}

.employment-card__foot {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: auto;
  padding-top: 4px;
}

.employment-section {
  margin-top: 40px;
}

/* ---------- 实习雷达 + 成都本地 ---------- */

.employment-top {
  display: grid;
  grid-template-columns: minmax(0, 1.35fr) minmax(0, 1fr);
  gap: var(--hp-gap);
  align-items: stretch;
  margin-top: 24px;
}

.employment-radar,
.employment-local,
.employment-campus,
.employment-alumni,
.employment-aggregate {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 26px;
  border-radius: var(--hp-r-lg);
}

.employment-radar__desc {
  margin: 0;
  color: var(--hp-ink-2);
  font-size: 14px;
  line-height: 1.8;
}

.employment-radar__body {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 20px;
  align-items: center;
}

.employment-radar__info {
  display: grid;
  gap: 16px;
  min-width: 0;
}

/* 雷达图：同心圆 + 缓慢扫描，岗位点匹配越高越靠近中心 */
.employment-radar__visual {
  position: relative;
  width: 236px;
  height: 236px;
  flex: 0 0 auto;
}

.employment-radar__ring {
  position: absolute;
  border: 1px dashed var(--hp-line-strong);
  border-radius: 50%;
}

.employment-radar__ring--out {
  inset: 0;
}

.employment-radar__ring--mid {
  inset: 26%;
  border-color: var(--hp-line);
}

.employment-radar__sweep {
  position: absolute;
  inset: 4%;
  border-radius: 50%;
  background: conic-gradient(from 0deg, rgba(92, 140, 180, 0.22), rgba(92, 140, 180, 0) 30%);
  animation: employment-radar-sweep 7s linear infinite;
}

@keyframes employment-radar-sweep {
  from {
    transform: rotate(0deg);
  }

  to {
    transform: rotate(360deg);
  }
}

@media (prefers-reduced-motion: reduce) {
  .employment-radar__sweep {
    animation: none;
  }
}

.employment-radar__center {
  position: absolute;
  top: 50%;
  left: 50%;
  z-index: 1;
  padding: 8px 14px;
  border-radius: 999px;
  background: var(--hp-ink);
  color: var(--hp-cream);
  font-size: 12.5px;
  font-weight: 600;
  white-space: nowrap;
  transform: translate(-50%, -50%);
}

.employment-radar__dot {
  position: absolute;
  z-index: 2;
  display: flex;
  align-items: center;
  gap: 6px;
  transform: translate(-50%, -50%);
}

.employment-radar__dot.is-left {
  flex-direction: row-reverse;
}

.employment-radar__dot i {
  width: 10px;
  height: 10px;
  border: 1px solid var(--hp-ink);
  border-radius: 50%;
  background: var(--hp-yellow);
}

.employment-radar__dot em {
  padding: 2px 8px;
  border: 1px solid var(--hp-line);
  border-radius: 999px;
  background: var(--hp-cream);
  color: var(--hp-ink);
  font-size: 11.5px;
  font-style: normal;
  font-weight: 600;
  white-space: nowrap;
}

.employment-radar__facts {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 16px;
  border-radius: var(--hp-r-md);
  background: var(--hp-surface-2);
}

.employment-radar__facts > div {
  display: grid;
  grid-template-columns: 64px minmax(0, 1fr);
  gap: 12px;
  align-items: baseline;
}

.employment-radar__facts dt {
  color: var(--hp-muted);
  font-size: 12.5px;
}

.employment-radar__facts dd {
  margin: 0;
  color: var(--hp-ink);
  font-size: 14px;
  font-weight: 600;
}

.employment-radar__stats p {
  margin: 0;
  color: var(--hp-ink-2);
  font-size: 14px;
}

.employment-radar__stats strong {
  color: var(--hp-ink);
  font-size: 18px;
  font-weight: 700;
}

.employment-radar__stats span {
  display: block;
  margin-top: 8px;
  color: var(--hp-muted);
  font-size: 13px;
}

.employment-local__stats {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.employment-local__stats li {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 14px;
  border-radius: var(--hp-r-sm);
  background: var(--hp-surface-2);
}

.employment-local__stats span {
  color: var(--hp-muted);
  font-size: 13px;
}

.employment-local__stats strong {
  color: var(--hp-ink);
  font-size: 18px;
  font-weight: 700;
}

.employment-local__districts {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 0;
}

.employment-local__districts span {
  padding: 4px 12px;
  border-radius: 999px;
  background: var(--hp-blue);
  color: var(--hp-ink);
  font-size: 12.5px;
}

.employment-local__hotspots {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 14px 16px;
  border-radius: var(--hp-r-sm);
  background: var(--hp-surface-2);
  list-style: none;
}

.employment-local__hotspots li {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.employment-local__hotspots strong {
  flex: 0 0 68px;
  color: var(--hp-ink);
  font-size: 13px;
  font-weight: 600;
}

.employment-local__hotspots span {
  color: var(--hp-muted);
  font-size: 12.5px;
}

.employment-local__meta {
  margin: 0;
  color: var(--hp-muted);
  font-size: 12px;
  line-height: 1.7;
}

/* ---------- 推荐岗位 ---------- */

.employment-jobs {
  display: grid;
  gap: 16px;
}

.employment-jobs__label {
  margin: 0;
  color: var(--hp-ink);
  font-size: 14px;
  font-weight: 600;
}

.employment-jobs__label--quiet {
  margin-top: 10px;
  color: var(--hp-muted);
  font-weight: 500;
}

.employment-jobs__hint {
  margin: 0;
  color: var(--hp-muted);
  font-size: 12.5px;
  line-height: 1.7;
}

.employment-jobs__featured {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--hp-gap);
}

.employment-jobs__more {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(258px, 1fr));
  gap: 14px;
}

.employment-job {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 22px;
  border-radius: var(--hp-r-lg);
}

.employment-job__head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.employment-job__logo {
  display: inline-grid;
  flex: 0 0 auto;
  width: 38px;
  height: 38px;
  border-radius: 12px;
  background: var(--hp-blue);
  color: var(--hp-ink);
  font-size: 13px;
  font-weight: 700;
  place-items: center;
}

.employment-job__title {
  min-width: 0;
}

.employment-job__title h3 {
  margin: 0;
  color: var(--hp-ink);
  font-size: 16px;
  font-weight: 600;
}

.employment-job__title p {
  margin: 4px 0 0;
  color: var(--hp-muted);
  font-size: 12.5px;
}

.employment-job__tag {
  display: inline-block;
  margin-left: 8px;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--hp-yellow);
  color: var(--hp-ink);
  font-size: 11px;
  font-weight: 600;
  vertical-align: middle;
}

.employment-job__rate {
  flex: 0 0 auto;
  margin-left: auto;
  color: var(--hp-ink);
  font-size: 20px;
  font-weight: 700;
  line-height: 1;
}

.employment-job__salary {
  flex: 0 0 auto;
  margin-left: auto;
  color: var(--hp-ink);
  font-size: 13.5px;
  font-weight: 700;
  line-height: 1.2;
  text-align: right;
}

.employment-job__place {
  margin: -6px 0 0;
  color: var(--hp-muted);
  font-size: 12.5px;
}

.employment-job__skills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.employment-job__skills .feature-chip {
  padding: 5px 12px;
  border-color: transparent;
  background: var(--hp-surface-2);
  color: var(--hp-ink);
  font-size: 12.5px;
  cursor: default;
}

.employment-job__reason {
  display: grid;
  gap: 6px;
  padding: 12px 14px;
  border-radius: var(--hp-r-sm);
  background: var(--hp-yellow);
}

.employment-job__reason span {
  color: #6a5f3a;
  font-size: 11.5px;
  font-weight: 600;
}

.employment-job__reason p {
  margin: 0;
  color: var(--hp-ink);
  font-size: 13.5px;
  line-height: 1.6;
}

.employment-job__meta {
  margin: 0;
  color: var(--hp-muted);
  font-size: 12px;
}

.employment-job__cta {
  align-self: flex-start;
  margin-top: auto;
}

/* 轻量小卡：重点推荐之后的机会列表 */

.employment-job--compact {
  display: grid;
  gap: 8px;
  padding: 16px 18px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-md);
  background: var(--hp-surface-2);
}

.employment-job__compact-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
}

.employment-job__compact-head h4 {
  margin: 0;
  color: var(--hp-ink);
  font-size: 14.5px;
  font-weight: 600;
}

.employment-job__compact-head span {
  flex: 0 0 auto;
  color: var(--hp-ink);
  font-size: 15px;
  font-weight: 700;
}

.employment-job__compact-meta {
  margin: 0;
  color: var(--hp-muted);
  font-size: 12px;
}

.employment-job--compact .feature-chip {
  background: var(--hp-cream);
}

/* ---------- 多平台聚合 ---------- */

.employment-aggregate__desc {
  margin: 0;
  color: var(--hp-muted);
  font-size: 12.5px;
  line-height: 1.7;
}

/* 一屏一行：左边说明、中间汇聚图、右边统计，整块高度压到一行以内 */
.employment-aggregate {
  display: grid;
  grid-template-columns: minmax(0, 0.85fr) minmax(0, 1.55fr) minmax(0, 0.8fr);
  gap: 24px;
  align-items: center;
}

.employment-aggregate__info {
  display: grid;
  gap: 8px;
  min-width: 0;
}

.employment-aggregate__points {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.employment-aggregate__points li {
  padding: 3px 10px;
  border-radius: 999px;
  background: var(--hp-surface-2);
  color: var(--hp-ink-2);
  font-size: 11.5px;
}

.employment-aggregate__flow {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.employment-aggregate__side {
  display: grid;
  gap: 8px;
}

.employment-aggregate__side span {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px 6px 6px;
  border: 1px solid var(--hp-line);
  border-radius: 999px;
  background: var(--hp-cream);
  color: var(--hp-ink-2);
  font-size: 12.5px;
  white-space: nowrap;
}

.employment-aggregate__side i {
  display: inline-grid;
  width: 22px;
  height: 22px;
  border-radius: 7px;
  background: var(--hp-blue);
  color: var(--hp-ink);
  font-size: 11px;
  font-style: normal;
  font-weight: 700;
  place-items: center;
}

.employment-aggregate__arrow {
  color: var(--hp-line-strong);
  font-size: 16px;
  font-weight: 700;
}

.employment-aggregate__job {
  margin: 0;
  padding: 10px 20px;
  border-radius: 999px;
  background: var(--hp-ink);
  color: var(--hp-cream);
  font-size: 14px;
  font-weight: 600;
  white-space: nowrap;
}

.employment-aggregate__facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.employment-aggregate__facts li {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  padding: 7px 12px;
  border-radius: 10px;
  background: var(--hp-surface-2);
}

.employment-aggregate__facts span {
  color: var(--hp-muted);
  font-size: 11.5px;
}

.employment-aggregate__facts strong {
  color: var(--hp-ink);
  font-size: 13px;
  font-weight: 600;
}

/* ---------- 校招 + 校友企业 ---------- */

.employment-bottom {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: var(--hp-gap);
  align-items: stretch;
  margin-top: 40px;
}

.employment-campus__season {
  margin: 0;
  color: var(--hp-ink);
  font-size: 20px;
  font-weight: 700;
}

.employment-campus__list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.employment-campus__list li {
  color: var(--hp-ink-2);
  font-size: 13.5px;
}

.employment-campus__timeline {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(90px, 1fr));
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.employment-campus__timeline li {
  position: relative;
  display: grid;
  gap: 4px;
  padding-top: 20px;
  text-align: center;
}

.employment-campus__timeline li::before {
  position: absolute;
  top: 5px;
  right: 0;
  left: 0;
  border-top: 1px dashed var(--hp-line-strong);
  content: '';
}

.employment-campus__timeline li:first-child::before {
  left: 50%;
}

.employment-campus__timeline li:last-child::before {
  right: 50%;
}

.employment-campus__dot {
  position: absolute;
  top: 0;
  left: 50%;
  width: 11px;
  height: 11px;
  margin-left: -5.5px;
  border: 1px solid var(--hp-line-strong);
  border-radius: 50%;
  background: var(--hp-yellow);
}

.employment-campus__timeline strong {
  color: var(--hp-ink);
  font-size: 13.5px;
  font-weight: 700;
}

.employment-campus__timeline span:last-child {
  color: var(--hp-muted);
  font-size: 12.5px;
}

.employment-alumni__count {
  margin: 0;
  color: var(--hp-ink);
  font-size: 20px;
  font-weight: 700;
}

.employment-alumni__hiring {
  margin: -8px 0 0;
  color: var(--hp-ink-2);
  font-size: 14px;
}

.employment-alumni__note {
  margin: 6px 0 0;
  color: var(--hp-muted);
  font-size: 12.5px;
}

.employment-alumni__fields {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 0;
}

.employment-alumni__fields span {
  padding: 4px 12px;
  border-radius: 999px;
  background: var(--hp-green);
  color: var(--hp-ink);
  font-size: 12.5px;
}

.employment-alumni__partners {
  display: flex;
  gap: 10px;
}

.employment-alumni__logo {
  display: inline-grid;
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: var(--hp-green);
  color: var(--hp-ink);
  font-size: 14px;
  font-weight: 700;
  place-items: center;
}

.employment-alumni__stats {
  display: grid;
  gap: 4px;
  margin: 0;
}

.employment-alumni__stats span {
  color: var(--hp-ink-2);
  font-size: 13px;
}

@media (max-width: 1080px) {
  .employment-top,
  .employment-bottom,
  .employment-jobs__featured,
  .employment-aggregate {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 640px) {
  .employment-hero {
    padding: 22px 20px;
  }

  .employment-hero__copy h1 {
    font-size: 26px;
  }

  .employment-aggregate__facts {
    grid-template-columns: minmax(0, 1fr);
  }

  .employment-search {
    flex-wrap: wrap;
    padding: 10px 12px;
    border-radius: 22px;
  }

  .employment-search input {
    height: 36px;
  }

  .employment-search button {
    width: 100%;
    height: 40px;
  }

  .employment-radar__body {
    grid-template-columns: minmax(0, 1fr);
    justify-items: center;
  }

  .employment-radar__info {
    width: 100%;
  }
}
</style>
