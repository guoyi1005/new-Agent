<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppTabBar from '../components/AppTabBar.vue'
import {
  BUSINESS_SANDBOX_SCENARIO,
  BUSINESS_SANDBOX_SCENARIOS,
  calculateSandboxRound,
  completeSandboxSession,
  createSandboxSession,
  deleteSandboxSession,
  formatSandboxMoney,
  formatSandboxPercent,
  getExpansionOptions,
  getSandboxScenario,
  getSandboxRoundEvent,
  getSandboxSession,
  getSandboxStrategies,
  listSandboxSessions,
  saveSandboxSession,
} from '../utils/businessSandbox'
import { buildSandboxProcessAnalysis } from '../utils/businessSandboxAnalysis.js'

const route = useRoute()
const router = useRouter()
const scenarios = BUSINESS_SANDBOX_SCENARIOS
const showAllScenarios = ref(false)
const strategies = getSandboxStrategies()
const visibleScenarios = computed(() => showAllScenarios.value ? scenarios : scenarios.slice(0, 3))

const session = ref(null)
const sessions = ref([])
const roundResult = ref(null)
const error = ref('')
const saving = ref(false)
const eventDetailOpen = ref(false)

const decision = reactive({
  strategyId: 'steady',
  marketingBudget: 9000,
  innovationBudget: 5000,
  sellerIncentive: 3,
  serviceBudget: 6000,
  commissionRate: 6,
  expansion: 'none',
})

const currentScenario = computed(() => getSandboxScenario(
  session.value?.scenarioId || String(route.query.scenario || '') || BUSINESS_SANDBOX_SCENARIO.id,
))
const decisionConfig = computed(() => currentScenario.value.decisionConfig)
const expansionOptions = computed(() => getExpansionOptions(currentScenario.value.id))
const currentEvent = computed(() => getSandboxRoundEvent(session.value))
const currentRound = computed(() => Number(session.value?.round || 1))
const totalRounds = computed(() => Number(currentScenario.value.rounds || 4))
const roundBudget = computed(() => Number(currentScenario.value.roundBudget || 0))
const allocatedBudget = computed(() => (
  Number(decision.marketingBudget || 0)
  + Number(decision.serviceBudget || 0)
  + Number(decision.innovationBudget || 0)
))
const remainingBudget = computed(() => roundBudget.value - allocatedBudget.value)
const overBudget = computed(() => remainingBudget.value < 0)
const ranking = computed(() => session.value?.ranking || [])
const currentEventAllEffects = computed(() => {
  const event = currentEvent.value
  if (!event) return []
  const effects = [
    ratioEffect('需求', event.demand),
    ratioEffect('增长', event.growth),
    ratioEffect('收入', event.revenue),
    ratioEffect('成本', event.cost),
    deltaEffect('满意度', event.satisfaction),
    deltaEffect('风险', event.risk),
  ].filter(Boolean)
  return effects.length ? effects : [{ label: '影响', text: '保持稳定', tone: 'neutral' }]
})
const currentEventEffects = computed(() => currentEventAllEffects.value.slice(0, 4))
const roundProgress = computed(() => {
  if (!session.value) return 0
  const completed = session.value.status === 'completed' ? totalRounds.value : Math.max(0, currentRound.value - 1)
  return Math.round(completed / totalRounds.value * 100)
})
const report = computed(() => session.value?.report || null)
const processAnalysis = computed(() => report.value ? buildSandboxProcessAnalysis(session.value) : [])
const metrics = computed(() => {
  const value = session.value || {}
  const labels = currentScenario.value.labels
  return [
    { label: labels.cash, value: formatSandboxMoney(value.cash), note: labels.cashNote },
    { label: labels.users, value: Number(value.users || 0).toLocaleString('zh-CN'), note: labels.usersNote },
    { label: labels.gmv, value: formatSandboxMoney(value.gmv), note: labels.gmvNote },
    { label: labels.satisfaction, value: formatSandboxPercent(value.satisfaction), note: labels.satisfactionNote },
    { label: labels.risk, value: formatSandboxPercent(value.risk), note: labels.riskNote },
  ]
})

function resetDecision(scenarioId = currentScenario.value.id) {
  const config = getSandboxScenario(scenarioId).decisionConfig
  decision.strategyId = 'steady'
  decision.marketingBudget = config.marketing.default
  decision.innovationBudget = config.innovation.default
  decision.sellerIncentive = config.incentive.default
  decision.serviceBudget = config.service.default
  decision.commissionRate = config.rate.default
  decision.expansion = 'none'
}

function loadSession(sessionId) {
  error.value = ''
  roundResult.value = null
  session.value = getSandboxSession(sessionId)
  if (sessionId && !session.value) {
    error.value = '没有找到这次经营记录，请重新选择沙盘。'
  }
  if (session.value) resetDecision(session.value.scenarioId)
}

function refreshSessions() {
  sessions.value = listSandboxSessions()
}

function syncFromRoute() {
  refreshSessions()
  loadSession(String(route.query.session || ''))
}

function startSession(scenarioId) {
  error.value = ''
  const created = createSandboxSession(scenarioId)
  if (!saveSandboxSession(created)) {
    error.value = '浏览器存储不可用，本次记录暂时无法保存。'
  }
  session.value = created
  sessions.value = listSandboxSessions()
  resetDecision(created.scenarioId)
  router.replace({ path: '/learning/projects/sandbox', query: { session: created.id } })
}

function openSession(item) {
  loadSession(item.id)
  router.replace({ path: '/learning/projects/sandbox', query: { session: item.id } })
}

function removeSession(item) {
  deleteSandboxSession(item.id)
  refreshSessions()
  if (session.value?.id === item.id) changeScenario()
}

function changeScenario() {
  session.value = null
  roundResult.value = null
  error.value = ''
  resetDecision()
  router.replace({ path: '/learning/projects/sandbox' })
}

function submitRound() {
  if (!session.value || saving.value) return
  if (overBudget.value) {
    error.value = '本轮预算已超出，请先调整推广、服务和研发投入。'
    return
  }
  saving.value = true
  error.value = ''
  try {
    const calculated = calculateSandboxRound(session.value, decision)
    roundResult.value = calculated.roundResult
    const nextSession = calculated.round >= totalRounds.value + 1
      ? completeSandboxSession(calculated)
      : calculated
    session.value = nextSession
    saveSandboxSession(nextSession)
    sessions.value = listSandboxSessions()
  } catch (cause) {
    error.value = cause.message || '本轮经营结算失败'
  } finally {
    saving.value = false
  }
}

function continueNextRound() {
  roundResult.value = null
  resetDecision()
}

function restartSession() {
  startSession(currentScenario.value.id)
}

function backToProjects() {
  router.push({ path: '/learning', query: { tab: 'projects' } })
}

function scenarioName(item) {
  return getSandboxScenario(item?.scenarioId).title
}

function strategyName(strategyId) {
  return strategies.find((item) => item.id === strategyId)?.name || '稳健运营'
}

function ratioEffect(label, value) {
  const number = Number(value)
  if (!Number.isFinite(number) || Math.abs(number - 1) < 0.001) return null
  const percent = Math.round(Math.abs(number - 1) * 100)
  return { label, text: `${number > 1 ? '+' : '-'}${percent}%`, tone: number > 1 ? 'up' : 'down' }
}

function deltaEffect(label, value) {
  const number = Number(value)
  if (!Number.isFinite(number) || Math.abs(number) < 0.001) return null
  return { label, text: `${number > 0 ? '+' : ''}${number}`, tone: number > 0 ? 'up' : 'down' }
}
function formatTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 16)
}

function formatSignedMoney(value) {
  const number = Number(value) || 0
  return `${number >= 0 ? '+' : ''}${formatSandboxMoney(number)}`
}

function formatSignedNumber(value) {
  const number = Math.round(Number(value) || 0)
  return `${number >= 0 ? '+' : ''}${number.toLocaleString('zh-CN')}`
}
function scoreTone(score) {
  if (Number(score) >= 85) return 'excellent'
  if (Number(score) >= 70) return 'good'
  if (Number(score) >= 55) return 'fair'
  return 'low'
}

watch(() => route.query.session, () => syncFromRoute())
onMounted(syncFromRoute)
</script>

<template>
  <div class="sandbox-page">
    <AppTabBar />
    <main class="sandbox-shell">
      <header class="sandbox-hero">
        <div class="hero-info">
          <h1>商业沙盘</h1>
          <p>{{ session ? currentScenario.name : '选择一个经营项目，通过连续决策完成项目实训。' }}</p>
        </div>
        <div class="sandbox-hero__aside">
          <div v-if="!session" class="hero-meta">
            <span>{{ scenarios.length }} 个场景</span>
            <span>4 轮经营</span>
            <span>生成能力证据</span>
          </div>
          <div class="hero-actions">
            <button v-if="session" type="button" class="sandbox-link" @click="changeScenario">选择其他沙盘</button>
            <button type="button" class="sandbox-button" @click="backToProjects">返回项目实训</button>
          </div>
        </div>
      </header>

      <p v-if="error" class="sandbox-error">{{ error }}</p>

      <template v-if="!session">
        <section class="sandbox-flow" aria-label="沙盘经营流程">
          <span><b>01</b>选择场景</span>
          <i aria-hidden="true" />
          <span><b>02</b>连续决策</span>
          <i aria-hidden="true" />
          <span><b>03</b>即时结算</span>
          <i aria-hidden="true" />
          <span><b>04</b>生成报告</span>
          <i aria-hidden="true" />
          <span><b>05</b>沉淀能力证据</span>
        </section>

        <section class="panel scenario-section">
          <div class="sandbox-head">
            <div><h2>沙盘场景</h2><span>选择适合目标方向的项目进行模拟经营</span></div>
            <span>{{ scenarios.length }} 个项目</span>
          </div>
          <div class="scenario-grid">
            <article v-for="(item, index) in visibleScenarios" :key="item.id" class="scenario-card">
              <div class="scenario-card__head">
                <span>项目 {{ String(index + 1).padStart(2, '0') }}</span>
                <strong>{{ item.rounds }} 轮经营</strong>
              </div>
              <h3>{{ item.title }}</h3>
              <p>{{ item.description }}</p>
              <div class="scenario-tags">
                <span>经营决策</span>
                <span>项目实训</span>
                <span>能力证据</span>
              </div>
              <div class="scenario-card__footer">
                <span>初始资金 <strong>{{ formatSandboxMoney(item.initial.cash) }}</strong></span>
                <button type="button" class="sandbox-button sandbox-button--primary" @click="startSession(item.id)">开始经营</button>
              </div>
            </article>
          </div>
          <div v-if="scenarios.length > 3" class="scenario-toggle">
            <button type="button" @click="showAllScenarios = !showAllScenarios">
              {{ showAllScenarios ? '收起场景' : `展开查看更多（还有 ${scenarios.length - 3} 个）` }}
            </button>
          </div>
        </section>

        <section class="panel multiplayer-entry">
          <div>
            <h2>多人小组经营</h2>
            <p>创建经营房间，每家公司由 3 至 5 名成员组成，分别承担总裁、市场、运营、财务和产品角色。</p>
          </div>
          <button type="button" class="sandbox-button sandbox-button--primary" @click="router.push('/learning/projects/sandbox/rooms')">进入多人房间</button>
        </section>
        <section class="panel sandbox-history">
          <div class="sandbox-head">
            <div><h2>历史经营</h2><span>继续之前的沙盘或查看经营报告</span></div>
            <span>{{ sessions.length }} 次记录</span>
          </div>
          <div v-if="sessions.length" class="history-list">
            <article v-for="item in sessions" :key="item.id">
              <div>
                <strong>{{ scenarioName(item) }}</strong>
                <span>{{ formatTime(item.updatedAt) }} · {{ item.status === 'completed' ? '已完成' : `第 ${item.round} 轮` }}</span>
              </div>
              <em v-if="item.report">{{ item.report.grade }} {{ item.report.score }} 分</em>
              <button type="button" class="sandbox-button" @click="openSession(item)">{{ item.status === 'completed' ? '查看报告' : '继续经营' }}</button>
              <button type="button" class="sandbox-link" @click="removeSession(item)">删除</button>
            </article>
          </div>
          <p v-else class="sandbox-empty">还没有经营记录，从上方选择一个沙盘开始。</p>
        </section>
      </template>

      <template v-else-if="session.status === 'in_progress'">

        <section class="metric-grid">
          <article v-for="item in metrics" :key="item.label" class="metric-card">
            <span>{{ item.label }}</span>
            <strong>{{ item.value }}</strong>
            <small>{{ item.note }}</small>
          </article>
        </section>

        <section class="panel progress-panel">
          <div class="sandbox-head">
            <div><h2>经营进度 · 第 {{ currentRound }} / {{ totalRounds }} 轮</h2><span>{{ currentScenario.title }}</span></div>
            <strong>{{ roundProgress }}%</strong>
          </div>
          <div class="round-track"><i :style="{ width: `${roundProgress}%` }" /></div>
          <div class="round-steps">
            <span v-for="round in totalRounds" :key="round" :class="{ active: round === currentRound, done: round < currentRound }">
              {{ round < currentRound ? '完成' : `第 ${round} 轮` }}
            </span>
          </div>
        </section>

        <section class="decision-layout">
          <aside class="panel market-panel">
            <h2>决策说明</h2>
            <section class="decision-event">
              <span class="decision-event__round">第 {{ currentRound }} 轮事件</span>
              <h3>{{ currentEvent?.title }}</h3>
              <p>{{ currentEvent?.description }}</p>
              <div class="event-impact">
                <span v-for="effect in currentEventEffects" :key="effect.label" :class="`is-${effect.tone}`">{{ effect.label }} {{ effect.text }}</span>
              </div>
              <button type="button" class="event-detail-link" @click="eventDetailOpen = true">查看完整事件</button>
            </section>
            <p class="scenario-brief">{{ currentScenario.description }}</p>
            <div v-if="ranking.length" class="ranking-list">
              <article v-for="item in ranking" :key="item.id" :class="{ 'is-user': item.isUser }">
                <span>{{ item.rank }}</span>
                <div><strong>{{ item.name }}</strong><small>{{ item.personaLabel }}</small></div>
                <em>{{ Number(item.users || 0).toLocaleString('zh-CN') }} 用户</em>
              </article>
            </div>
            <div class="market-note"><span>{{ currentScenario.labels.cash }}</span><strong>{{ formatSandboxMoney(session.cash) }}</strong></div>
            <div class="market-note"><span>{{ currentScenario.labels.gmv }}</span><strong>{{ formatSandboxMoney(session.gmv) }}</strong></div>
            <div class="market-note"><span>{{ currentScenario.labels.satisfaction }}</span><strong>{{ formatSandboxPercent(session.satisfaction) }}</strong></div>
          </aside>

          <section class="panel decision-panel">
            <div class="sandbox-head">
              <div><h2>本轮经营决策</h2><span>提交后立即结算并进入下一轮</span></div>
            </div>

            <div class="event-mobile-reminder">
              <div><span>第 {{ currentRound }} 轮</span><strong>{{ currentEvent?.title }}</strong></div>
              <button type="button" @click="eventDetailOpen = true">查看事件</button>
            </div>

            <section class="strategy-section">
              <div class="strategy-section__head">
                <strong>选择经营策略</strong>
                <span>策略会改变本轮的增长、成本和风险</span>
              </div>
              <div class="strategy-grid">
                <button
                  v-for="item in strategies"
                  :key="item.id"
                  type="button"
                  :class="{ active: decision.strategyId === item.id }"
                  @click="decision.strategyId = item.id"
                >
                  <strong>{{ item.name }}</strong>
                  <span>{{ item.summary }}</span>
                </button>
              </div>
            </section>

            <div class="budget-bar" :class="{ 'is-over': overBudget }">
              <span>本轮可分配预算 {{ formatSandboxMoney(roundBudget) }}</span>
              <strong>已分配 {{ formatSandboxMoney(allocatedBudget) }} · 剩余 {{ formatSandboxMoney(remainingBudget) }}</strong>
            </div>

            <label class="decision-item">
              <span><strong>{{ decisionConfig.marketing.label }}</strong><em>{{ formatSandboxMoney(decision.marketingBudget) }}</em></span>
              <input v-model.number="decision.marketingBudget" type="range" :min="decisionConfig.marketing.min" :max="decisionConfig.marketing.max" :step="decisionConfig.marketing.step" />
              <small>{{ decisionConfig.marketing.hint }}</small>
            </label>

            <label class="decision-item">
              <span><strong>{{ decisionConfig.service.label }}</strong><em>{{ formatSandboxMoney(decision.serviceBudget) }}</em></span>
              <input v-model.number="decision.serviceBudget" type="range" :min="decisionConfig.service.min" :max="decisionConfig.service.max" :step="decisionConfig.service.step" />
              <small>{{ decisionConfig.service.hint }}</small>
            </label>

            <label class="decision-item">
              <span><strong>{{ decisionConfig.innovation.label }}</strong><em>{{ formatSandboxMoney(decision.innovationBudget) }}</em></span>
              <input v-model.number="decision.innovationBudget" type="range" :min="decisionConfig.innovation.min" :max="decisionConfig.innovation.max" :step="decisionConfig.innovation.step" />
              <small>{{ decisionConfig.innovation.hint }}</small>
            </label>

            <details class="policy-panel">
              <summary>定价与合作策略</summary>
              <label class="decision-item">
                <span><strong>{{ decisionConfig.incentive.label }}</strong><em>{{ formatSandboxPercent(decision.sellerIncentive, 1) }}</em></span>
                <input v-model.number="decision.sellerIncentive" type="range" :min="decisionConfig.incentive.min" :max="decisionConfig.incentive.max" :step="decisionConfig.incentive.step" />
                <small>{{ decisionConfig.incentive.hint }}</small>
              </label>
              <label class="decision-item">
                <span><strong>{{ decisionConfig.rate.label }}</strong><em>{{ formatSandboxPercent(decision.commissionRate, 1) }}</em></span>
                <input v-model.number="decision.commissionRate" type="range" :min="decisionConfig.rate.min" :max="decisionConfig.rate.max" :step="decisionConfig.rate.step" />
                <small>{{ decisionConfig.rate.hint }}</small>
              </label>
            </details>

            <div class="expansion-picker">
              <div><strong>{{ decisionConfig.expansionLabel }}</strong><span>{{ decisionConfig.expansionHint }}</span></div>
              <div class="expansion-options">
                <button v-for="item in expansionOptions" :key="item.id" type="button" :class="{ active: decision.expansion === item.id }" @click="decision.expansion = item.id">
                  <strong>{{ item.label }}</strong>
                  <small>{{ item.cost ? `投入 ${formatSandboxMoney(item.cost)}` : '不增加额外投入' }}</small>
                </button>
              </div>
            </div>

            <footer class="decision-footer">
              <button type="button" class="sandbox-button sandbox-button--primary" :disabled="saving || overBudget" @click="submitRound">
                {{ saving ? '正在结算…' : `提交第 ${currentRound} 轮决策` }}
              </button>
            </footer>
          </section>
        </section>
      </template>

      <template v-else-if="report">
        <section class="panel report-hero">
          <div>
            <h2>{{ report.grade }} · {{ report.score }} 分</h2>
            <span>{{ currentScenario.name }} · 已完成 {{ report.rounds }} 轮经营</span>
          </div>
          <div class="score-ring" :class="`score-ring--${scoreTone(report.score)}`">{{ report.score }}</div>
        </section>

        <section class="metric-grid">
          <article class="metric-card"><span>累计收入</span><strong>{{ formatSandboxMoney(report.totalRevenue) }}</strong><small>{{ currentScenario.labels.revenueNote }}</small></article>
          <article class="metric-card"><span>累计成本</span><strong>{{ formatSandboxMoney(report.totalCost) }}</strong><small>{{ currentScenario.labels.costNote }}</small></article>
          <article class="metric-card"><span>累计利润</span><strong>{{ formatSandboxMoney(report.totalProfit) }}</strong><small>{{ currentScenario.labels.profitNote }}</small></article>
          <article class="metric-card"><span>{{ currentScenario.labels.users }}</span><strong>{{ report.finalUsers.toLocaleString('zh-CN') }}</strong><small>{{ currentScenario.labels.finalUsersNote }}</small></article>
          <article class="metric-card"><span>{{ currentScenario.labels.satisfaction }}</span><strong>{{ formatSandboxPercent(report.finalSatisfaction) }}</strong><small>{{ currentScenario.labels.satisfactionNote }}</small></article>
        </section>

        <section class="panel report-ranking">
          <div class="sandbox-head"><div><h2>最终排名</h2><span>综合现金、用户、满意度和风险计算</span></div></div>
          <div class="final-ranking">
            <article v-for="item in report.ranking" :key="item.id" :class="{ 'is-user': item.isUser }">
              <span>{{ item.rank }}</span>
              <div><strong>{{ item.name }}</strong><small>{{ item.personaLabel }}</small></div>
              <em>{{ item.score }} 分</em>
            </article>
          </div>
        </section>

        <section v-if="processAnalysis.length" class="panel process-analysis">
          <div class="sandbox-head">
            <div><h2>过程复盘</h2><span>回放每一轮事件和选择，比较不同经营策略的结果</span></div>
            <span>{{ processAnalysis.length }} 轮分析</span>
          </div>
          <div class="analysis-timeline">
            <details v-for="item in processAnalysis" :key="item.round" :open="item.round === 1">
              <summary>
                <span class="analysis-round">第 {{ item.round }} 轮</span>
                <div><strong>{{ item.event?.title }}</strong><p>{{ item.summary }}</p></div>
                <em>{{ item.scoreGap > 0 ? `可提升 ${item.scoreGap} 分` : '接近综合最优' }}</em>
              </summary>
              <div class="analysis-body">
                <div class="analysis-compare">
                  <article>
                    <span>你的实际方案</span>
                    <h3>{{ item.actual.strategy?.name || '实际经营策略' }}</h3>
                    <div><small>利润</small><strong>{{ formatSandboxMoney(item.actual.after?.profit) }}</strong></div>
                    <div><small>用户增长</small><strong>{{ formatSignedNumber(item.actual.delta?.users) }}</strong></div>
                    <div><small>满意度</small><strong>{{ formatSandboxPercent(item.actual.after?.satisfaction) }}</strong></div>
                    <div><small>风险</small><strong>{{ formatSandboxPercent(item.actual.after?.risk) }}</strong></div>
                  </article>
                  <article class="is-best">
                    <span>综合最优候选</span>
                    <h3>{{ item.bestOverall.name }}</h3>
                    <div><small>利润</small><strong>{{ formatSandboxMoney(item.bestOverall.after?.profit) }}</strong></div>
                    <div><small>用户增长</small><strong>{{ formatSignedNumber(item.bestOverall.delta?.users) }}</strong></div>
                    <div><small>满意度</small><strong>{{ formatSandboxPercent(item.bestOverall.after?.satisfaction) }}</strong></div>
                    <div><small>风险</small><strong>{{ formatSandboxPercent(item.bestOverall.after?.risk) }}</strong></div>
                  </article>
                </div>
                <div class="analysis-alternatives">
                  <article><span>利润最高</span><strong>{{ item.profitBest.name }}</strong><em>{{ formatSignedMoney(item.profitBest.after?.profit - item.actual.after?.profit) }}</em></article>
                  <article><span>增长最快</span><strong>{{ item.growthBest.name }}</strong><em>{{ formatSignedNumber(item.growthBest.delta?.users - item.actual.delta?.users) }}</em></article>
                  <article><span>风险最低</span><strong>{{ item.riskBest.name }}</strong><em>{{ formatSignedNumber(item.riskBest.after?.risk - item.actual.after?.risk) }} 风险</em></article>
                </div>
                <div class="analysis-reasons">
                  <h4>为什么结果不同</h4>
                  <ul><li v-for="reason in item.reasons" :key="reason">{{ reason }}</li></ul>
                </div>
              </div>
            </details>
          </div>
        </section>
        <section class="report-layout">
          <section class="panel report-history">
            <div class="sandbox-head"><div><h2>每轮经营结果</h2><span>复盘四轮决策变化</span></div></div>
            <article v-for="item in report.history" :key="item.round">
              <span>第 {{ item.round }} 轮</span>
              <strong>{{ formatSandboxMoney(item.after.profit) }}</strong>
              <small>{{ currentScenario.labels.users }} {{ Number(item.after.users).toLocaleString('zh-CN') }} · {{ currentScenario.labels.satisfaction }} {{ formatSandboxPercent(item.after.satisfaction) }}</small>
            </article>
          </section>

          <section class="panel evaluation-panel">
            <div class="sandbox-head"><div><h2>能力证据</h2><span>待成长中心同步</span></div></div>
            <article v-for="item in report.evaluations" :key="item.code">
              <div><strong>{{ item.label }}</strong><span>{{ item.summary }}</span></div>
              <div class="evaluation-score">{{ item.value }}</div>
            </article>
            <p class="evidence-note">这里只生成项目实训证据，不直接修改技能等级。成长中心完成接入后，可读取这些证据更新技能树。</p>
          </section>
        </section>

        <footer class="report-actions">
          <button type="button" class="sandbox-button sandbox-button--primary" @click="restartSession">再经营一次</button>
          <button type="button" class="sandbox-button" @click="changeScenario">选择其他沙盘</button>
          <button type="button" class="sandbox-button" @click="backToProjects">返回项目实训</button>
        </footer>
      </template>
    </main>

    <Teleport to="body">
      <div v-if="eventDetailOpen" class="event-mask" @click.self="eventDetailOpen = false">
        <section class="event-dialog">
          <header>
            <div><span>第 {{ currentRound }} 轮事件</span><h2>{{ currentEvent?.title }}</h2></div>
            <button type="button" aria-label="关闭事件详情" @click="eventDetailOpen = false">×</button>
          </header>
          <p>{{ currentEvent?.description }}</p>
          <div class="event-detail-grid">
            <article v-for="effect in currentEventAllEffects" :key="effect.label">
              <span>{{ effect.label }}</span>
              <strong :class="`is-${effect.tone}`">{{ effect.text }}</strong>
            </article>
          </div>
          <footer><button type="button" class="sandbox-button sandbox-button--primary" @click="eventDetailOpen = false">知道了</button></footer>
        </section>
      </div>
    </Teleport>
    <Teleport to="body">
      <div v-if="roundResult" class="result-mask" @click.self="continueNextRound">
        <section class="result-dialog">
          <header>
            <div>
              <h2>第 {{ roundResult.round }} 轮结算完成</h2>
              <span>{{ roundResult.event.title }} · {{ roundResult.strategy.name }}</span>
            </div>
            <button type="button" aria-label="关闭经营结果" @click="continueNextRound">×</button>
          </header>
          <div class="result-grid">
            <div><span>利润</span><strong :class="{ positive: roundResult.after.profit >= 0, negative: roundResult.after.profit < 0 }">{{ formatSandboxMoney(roundResult.after.profit) }}</strong></div>
            <div><span>{{ currentScenario.labels.users }}</span><strong>+{{ Math.max(0, roundResult.delta.users) }}</strong></div>
            <div><span>{{ currentScenario.labels.gmv }}</span><strong>{{ formatSandboxMoney(roundResult.after.gmv) }}</strong></div>
            <div><span>{{ currentScenario.labels.satisfaction }}</span><strong>{{ formatSandboxPercent(roundResult.after.satisfaction) }}</strong></div>
          </div>
          <div class="result-ranking">
            <span v-for="item in roundResult.ranking" :key="item.id" :class="{ 'is-user': item.isUser }">
              {{ item.rank }}. {{ item.name }}
            </span>
          </div>
          <ul>
            <li v-for="item in roundResult.insight" :key="item">{{ item }}</li>
          </ul>
          <footer>
            <button type="button" class="sandbox-button sandbox-button--primary" @click="continueNextRound">
              {{ roundResult.round >= totalRounds ? '查看经营报告' : '进入下一轮' }}
            </button>
          </footer>
        </section>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.sandbox-page {
  min-height: 100vh;
  color: var(--hp-ink);
  background: var(--hp-bg);
  font-family: Inter, 'Segoe UI', system-ui, -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.sandbox-page *,
.sandbox-page *::before,
.sandbox-page *::after {
  box-sizing: border-box;
}

.sandbox-shell {
  width: min(1280px, calc(100% - 48px));
  margin: 0 auto;
  padding: 96px 0 80px;
}

.sandbox-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  padding: 24px 28px;
  border: 1px solid #e4ebf2;
  border-radius: var(--hp-r-lg);
  background: var(--hp-tint);
  box-shadow: var(--hp-shadow-sm);
}

.hero-info {
  display: grid;
  gap: 7px;
  min-width: 0;
}

.sandbox-hero__aside {
  display: flex;
  align-items: center;
  gap: 18px;
}

.hero-meta {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.hero-meta span {
  padding: 6px 10px;
  border: 1px solid rgba(92, 140, 180, .18);
  border-radius: 999px;
  color: var(--hp-blue-ink);
  background: rgba(255, 255, 255, .62);
  font-size: 11px;
  font-weight: 650;
}

.sandbox-hero h1 {
  margin: 0;
  font-size: 25px;
  line-height: 1.2;
  letter-spacing: -.02em;
}

.sandbox-hero p,
.sandbox-head span,
.scenario-card p,
.market-panel p,
.decision-item small {
  margin: 0;
  color: var(--hp-ink-2);
  font-size: 13.5px;
  line-height: 1.65;
}

.hero-actions,
.decision-footer,
.report-actions {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
}

.sandbox-button {
  min-height: 42px;
  padding: 0 18px;
  border: 1px solid var(--hp-line-strong);
  border-radius: 999px;
  color: var(--hp-ink);
  background: var(--hp-surface);
  font-size: 13px;
  font-weight: 650;
  cursor: pointer;
  transition: transform .18s ease, border-color .18s ease, box-shadow .18s ease;
}

.sandbox-button:hover {
  transform: translateY(-1px);
  border-color: var(--hp-ink);
}

.sandbox-button:disabled {
  opacity: .55;
  cursor: wait;
  transform: none;
}

.sandbox-button--primary {
  border-color: var(--hp-ink);
  color: #fff;
  background: var(--hp-ink);
  box-shadow: var(--hp-shadow-sm);
}

.sandbox-link {
  padding: 0;
  border: 0;
  color: var(--hp-ink-2);
  background: transparent;
  font-size: 13px;
  font-weight: 650;
  cursor: pointer;
}

.sandbox-link:hover {
  color: var(--hp-ink);
  text-decoration: underline;
}

.sandbox-error {
  margin-top: 14px;
  padding: 12px 16px;
  border-radius: var(--hp-r-sm);
  color: #8b4c49;
  background: #f8eeee;
  font-size: 13px;
}

.event-panel {
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  align-items: center;
  gap: 20px;
  margin-top: 16px;
  padding: 22px 26px;
  background: #fffaf0;
}

.event-panel__round {
  padding: 7px 10px;
  border-radius: 999px;
  color: #8a6a35;
  background: #f5e8bd;
  font-size: 11px;
  font-weight: 750;
  white-space: nowrap;
}

.event-panel__copy h2 {
  margin: 0 0 6px;
  font-size: 20px;
}

.event-panel__copy p {
  margin: 0;
  color: var(--hp-ink-2);
  font-size: 13.5px;
  line-height: 1.65;
}

.event-impact {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.event-impact span {
  padding: 5px 9px;
  border: 1px solid #eadab0;
  border-radius: 999px;
  color: #806d35;
  background: rgba(255, 255, 255, .68);
  font-size: 11px;
  font-weight: 700;
}

.event-impact span.is-up {
  border-color: #bfd6c6;
  color: #47725a;
  background: #f0f8f3;
}

.event-impact span.is-down {
  border-color: #e3c2c5;
  color: #8a535a;
  background: #fbf1f2;
}

.decision-event {
  display: grid;
  gap: 9px;
  margin-top: 16px;
  padding: 16px;
  border: 1px solid #eadab0;
  border-radius: 14px;
  background: #fffaf0;
}

.decision-event__round {
  justify-self: start;
  padding: 5px 8px;
  border-radius: 999px;
  color: #8a6a35;
  background: #f5e8bd;
  font-size: 10.5px;
  font-weight: 750;
}

.decision-event h3 {
  margin: 0;
  color: var(--hp-ink);
  font-size: 17px;
  line-height: 1.35;
}

.decision-event p {
  margin: 0;
  color: var(--hp-ink-2);
  font-size: 12.5px;
  line-height: 1.7;
}

.decision-event .event-impact {
  justify-content: flex-start;
}

.event-detail-link {
  justify-self: start;
  padding: 0;
  border: 0;
  color: var(--hp-blue-ink);
  background: transparent;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.scenario-brief {
  margin: 18px 0 0;
  padding-top: 16px;
  border-top: 1px solid var(--hp-line);
}

.event-mobile-reminder {
  display: none;
}

.event-mask {
  position: fixed;
  inset: 0;
  z-index: 2050;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(27, 35, 41, .36);
}

.event-dialog {
  width: min(620px, 100%);
  padding: 26px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-lg);
  background: var(--hp-surface);
  box-shadow: var(--hp-shadow-lg);
}

.event-dialog header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.event-dialog header span {
  color: #8a6a35;
  font-size: 11px;
  font-weight: 750;
}

.event-dialog h2 {
  margin: 7px 0 0;
}

.event-dialog header button {
  width: 36px;
  height: 36px;
  border: 0;
  border-radius: 50%;
  color: var(--hp-ink-2);
  background: var(--hp-surface-2);
  font-size: 22px;
  cursor: pointer;
}

.event-dialog > p {
  margin: 20px 0 0;
  color: var(--hp-ink-2);
  font-size: 14px;
  line-height: 1.8;
}

.event-detail-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 9px;
  margin-top: 18px;
}

.event-detail-grid article {
  display: grid;
  gap: 6px;
  padding: 13px;
  border-radius: 11px;
  background: var(--hp-surface-2);
}

.event-detail-grid span {
  color: var(--hp-muted);
  font-size: 12px;
}

.event-detail-grid strong.is-up {
  color: var(--hp-green-ink);
}

.event-detail-grid strong.is-down {
  color: #9b575d;
}

.event-dialog footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 22px;
}

.sandbox-flow {
  display: grid;
  grid-template-columns: auto minmax(20px, 1fr) auto minmax(20px, 1fr) auto minmax(20px, 1fr) auto minmax(20px, 1fr) auto;
  align-items: center;
  gap: 10px;
  margin-top: 16px;
  padding: 14px 18px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-md);
  background: var(--hp-surface);
  box-shadow: var(--hp-shadow-sm);
}

.sandbox-flow span {
  color: var(--hp-ink-2);
  font-size: 12px;
  font-weight: 650;
  white-space: nowrap;
}

.sandbox-flow b {
  margin-right: 4px;
  color: var(--hp-blue-ink);
  font-size: 10px;
}

.sandbox-flow i {
  height: 1px;
  background: var(--hp-line);
}

.panel,
.metric-card {
  border: 1px solid var(--hp-line);
  background: var(--hp-surface);
  box-shadow: var(--hp-shadow-sm);
}

.panel {
  border-radius: var(--hp-r-lg);
}

.scenario-section,
.sandbox-history,
.progress-panel,
.market-panel,
.decision-panel,
.report-history,
.evaluation-panel,
.report-ranking {
  padding: 24px 28px;
}

.scenario-section,
.sandbox-history,
.progress-panel,
.decision-layout,
.report-ranking,
.process-analysis {
  margin-top: 16px;
  padding: 24px 28px;
}

.analysis-timeline {
  display: grid;
  gap: 10px;
  margin-top: 18px;
}

.analysis-timeline details {
  overflow: hidden;
  border: 1px solid var(--hp-line);
  border-radius: 14px;
  background: var(--hp-surface);
}

.analysis-timeline summary {
  display: grid;
  grid-template-columns: 74px minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  list-style: none;
  cursor: pointer;
}

.analysis-timeline summary::-webkit-details-marker {
  display: none;
}

.analysis-round {
  padding: 5px 8px;
  border-radius: 999px;
  color: var(--hp-blue-ink);
  background: var(--hp-blue);
  font-size: 11px;
  font-weight: 750;
  text-align: center;
}

.analysis-timeline summary strong {
  display: block;
  font-size: 15px;
}

.analysis-timeline summary p {
  margin: 4px 0 0;
  color: var(--hp-muted);
  font-size: 12px;
  line-height: 1.55;
}

.analysis-timeline summary em {
  color: var(--hp-blue-ink);
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
  white-space: nowrap;
}

.analysis-body {
  display: grid;
  gap: 14px;
  padding: 0 18px 18px;
  border-top: 1px solid var(--hp-line);
}

.analysis-compare {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  padding-top: 16px;
}

.analysis-compare article {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  padding: 16px;
  border-radius: 13px;
  background: var(--hp-surface-2);
}

.analysis-compare article > span,
.analysis-compare article > h3 {
  grid-column: 1 / -1;
}

.analysis-compare article > span {
  color: var(--hp-muted);
  font-size: 11px;
  font-weight: 700;
}

.analysis-compare article h3 {
  margin: 0;
  font-size: 16px;
}

.analysis-compare article > div {
  display: grid;
  gap: 4px;
  padding-top: 10px;
  border-top: 1px solid var(--hp-line);
}

.analysis-compare small {
  color: var(--hp-muted);
  font-size: 10.5px;
}

.analysis-compare strong {
  font-size: 13px;
}

.analysis-compare article.is-best {
  border: 1px solid #bfd6c6;
  background: #f1f8f3;
}

.analysis-alternatives {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 9px;
}

.analysis-alternatives article {
  display: grid;
  gap: 5px;
  padding: 13px 14px;
  border: 1px solid var(--hp-line);
  border-radius: 11px;
  background: var(--hp-surface-2);
}

.analysis-alternatives span {
  color: var(--hp-muted);
  font-size: 11px;
}

.analysis-alternatives strong {
  font-size: 13px;
}

.analysis-alternatives em {
  color: var(--hp-blue-ink);
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
}

.analysis-reasons {
  padding: 16px;
  border-radius: 12px;
  background: #fffaf0;
}

.analysis-reasons h4 {
  margin: 0;
  font-size: 14px;
}

.analysis-reasons ul {
  display: grid;
  gap: 7px;
  margin: 11px 0 0;
  padding-left: 18px;
  color: var(--hp-ink-2);
  font-size: 12.5px;
  line-height: 1.65;
}
.report-layout {
  margin-top: 16px;
}

.sandbox-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.sandbox-head h2,
.market-panel h2 {
  margin: 0;
  font-size: 20px;
  letter-spacing: -.02em;
}

.sandbox-head > span {
  color: var(--hp-muted);
  font-size: 12px;
  white-space: nowrap;
}

.scenario-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
  margin-top: 18px;
}

.scenario-card {
  display: grid;
  grid-template-rows: auto auto minmax(66px, 1fr) auto auto;
  gap: 12px;
  min-height: 272px;
  padding: 20px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-md);
  background: var(--hp-surface);
  transition: transform .18s ease, box-shadow .18s ease;
}

.scenario-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--hp-shadow-md);
}

.scenario-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: var(--hp-muted);
  font-size: 11px;
}

.scenario-card__head strong {
  color: var(--hp-blue-ink);
}

.scenario-card h3 {
  margin: 0;
  font-size: 18px;
}

.scenario-card p {
  display: -webkit-box;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 3;
}

.scenario-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  align-self: end;
  padding-top: 14px;
  border-top: 1px solid var(--hp-line);
}

.scenario-card__footer > span {
  display: grid;
  gap: 3px;
  color: var(--hp-muted);
  font-size: 11px;
}

.scenario-card__footer strong {
  color: var(--hp-ink);
  font-size: 13px;
}

.scenario-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.scenario-tags span,
.special-chip {
  padding: 5px 9px;
  border-radius: 999px;
  color: var(--hp-blue-ink);
  background: var(--hp-blue);
  font-size: 11px;
  font-weight: 650;
}

.history-list {
  display: grid;
  gap: 8px;
  margin-top: 18px;
}

.history-list article {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto auto auto;
  align-items: center;
  gap: 16px;
  padding: 14px 0;
  border-top: 1px solid var(--hp-line);
}

.history-list article > div {
  display: grid;
  gap: 4px;
}

.history-list article span {
  color: var(--hp-muted);
  font-size: 12px;
}

.history-list em {
  color: var(--hp-blue-ink);
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
}

.sandbox-empty {
  margin: 16px 0 0;
  padding: 14px 16px;
  border: 1px dashed var(--hp-line-strong);
  border-radius: var(--hp-r-sm);
  color: var(--hp-muted);
  background: var(--hp-surface-2);
  font-size: 13px;
  text-align: center;
}

.metric-grid {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.metric-card {
  display: grid;
  gap: 7px;
  min-width: 0;
  padding: 18px;
  border-radius: var(--hp-r-md);
}

.metric-card span,
.metric-card small {
  color: var(--hp-muted);
  font-size: 12px;
}

.metric-card strong {
  overflow: hidden;
  color: var(--hp-ink);
  font-size: 22px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.progress-panel .sandbox-head > strong {
  color: var(--hp-blue-ink);
}

.round-track {
  height: 8px;
  margin-top: 18px;
  overflow: hidden;
  border-radius: 999px;
  background: var(--hp-track);
}

.round-track i {
  display: block;
  height: 100%;
  border-radius: inherit;
  background: var(--hp-blue-ink);
  transition: width .25s ease;
}

.round-steps {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin-top: 14px;
}

.round-steps span {
  padding: 8px;
  border-radius: 10px;
  color: var(--hp-muted);
  background: var(--hp-surface-2);
  font-size: 12px;
  text-align: center;
}

.round-steps span.active {
  color: #fff;
  background: var(--hp-ink);
}

.round-steps span.done {
  color: var(--hp-blue-ink);
  background: var(--hp-blue);
}

.decision-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.market-panel {
  position: sticky;
  top: 88px;
}

.market-panel > p {
  margin-top: 12px;
}

.ranking-list {
  display: grid;
  gap: 6px;
  margin-top: 18px;
}

.ranking-list article {
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) auto;
  align-items: center;
  gap: 9px;
  padding: 9px 10px;
  border: 1px solid var(--hp-line);
  border-radius: 10px;
  background: var(--hp-surface-2);
}

.ranking-list article.is-user {
  border-color: #afc4d1;
  background: var(--hp-blue);
}

.ranking-list article > span {
  display: grid;
  width: 22px;
  height: 22px;
  place-items: center;
  border-radius: 50%;
  color: var(--hp-blue-ink);
  background: #fff;
  font-size: 11px;
  font-weight: 800;
}

.ranking-list article > div {
  display: grid;
  gap: 2px;
  min-width: 0;
}

.ranking-list strong {
  overflow: hidden;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ranking-list small,
.ranking-list em {
  color: var(--hp-muted);
  font-size: 10px;
  font-style: normal;
}

.market-note {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  padding: 13px 0;
  border-top: 1px solid var(--hp-line);
  color: var(--hp-muted);
  font-size: 12px;
}

.market-note:first-of-type {
  margin-top: 20px;
}

.market-note strong {
  color: var(--hp-ink);
  font-size: 14px;
}

.decision-panel {
  display: grid;
  gap: 20px;
}

.strategy-section {
  display: grid;
  gap: 12px;
  padding-bottom: 18px;
  border-bottom: 1px solid var(--hp-line);
}

.strategy-section__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.strategy-section__head strong {
  font-size: 14px;
}

.strategy-section__head span {
  color: var(--hp-muted);
  font-size: 11px;
}

.strategy-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
}

.strategy-grid button {
  display: grid;
  gap: 7px;
  padding: 13px;
  border: 1px solid var(--hp-line);
  border-radius: 12px;
  color: var(--hp-ink);
  background: var(--hp-surface-2);
  text-align: left;
  cursor: pointer;
}

.strategy-grid button.active {
  border-color: var(--hp-blue-ink);
  background: var(--hp-blue);
}

.strategy-grid button span {
  color: var(--hp-muted);
  font-size: 11px;
  line-height: 1.55;
}

.budget-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  padding: 11px 13px;
  border: 1px solid #dbe4e9;
  border-radius: 12px;
  color: var(--hp-ink-2);
  background: #f6f9fb;
  font-size: 12px;
}

.budget-bar span {
  color: var(--hp-muted);
}

.budget-bar strong {
  color: var(--hp-blue-ink);
}

.budget-bar.is-over {
  border-color: #e4c6c8;
  background: #fbf1f1;
}

.budget-bar.is-over strong {
  color: #9b575d;
}

.decision-item {
  display: grid;
  gap: 10px;
}

.decision-item > span {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.decision-item strong {
  font-size: 14px;
}

.decision-item em {
  color: var(--hp-blue-ink);
  font-size: 13px;
  font-style: normal;
  font-weight: 750;
}

.decision-item input[type='range'] {
  width: 100%;
  accent-color: var(--hp-ink);
}

.policy-panel {
  border-top: 1px solid var(--hp-line);
}

.policy-panel summary {
  padding: 14px 0 0;
  color: var(--hp-ink);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
}

.policy-panel[open] {
  display: grid;
  gap: 18px;
}

.policy-panel[open] summary {
  padding-bottom: 0;
}

.expansion-picker {
  display: grid;
  gap: 12px;
  padding-top: 6px;
  border-top: 1px solid var(--hp-line);
}

.expansion-picker > div:first-child {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.expansion-picker span {
  color: var(--hp-muted);
  font-size: 12px;
}

.expansion-options {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
}

.expansion-options button {
  display: grid;
  gap: 6px;
  padding: 13px;
  border: 1px solid var(--hp-line);
  border-radius: 12px;
  color: var(--hp-ink);
  background: var(--hp-surface-2);
  text-align: left;
  cursor: pointer;
}

.expansion-options button.active {
  border-color: var(--hp-blue-ink);
  background: var(--hp-blue);
}

.expansion-options small {
  color: var(--hp-muted);
  font-size: 11px;
}

.decision-footer {
  justify-content: flex-end;
  padding-top: 6px;
}

.report-hero {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-top: 16px;
  padding: 28px 32px;
  background: var(--hp-tint);
}

.report-hero h2 {
  margin: 0 0 8px;
  font-size: 28px;
  letter-spacing: -.03em;
}

.report-hero span {
  color: var(--hp-ink-2);
  font-size: 14px;
}

.score-ring {
  display: grid;
  width: 84px;
  height: 84px;
  flex: none;
  place-items: center;
  border: 8px solid var(--hp-blue);
  border-radius: 50%;
  font-size: 28px;
  font-weight: 800;
}

.score-ring--excellent {
  border-color: var(--hp-green);
}

.score-ring--fair {
  border-color: var(--hp-yellow);
}

.score-ring--low {
  border-color: var(--hp-pink);
}

.process-analysis {
  margin-top: 16px;
  padding: 24px 28px;
}

.analysis-timeline {
  display: grid;
  gap: 10px;
  margin-top: 18px;
}

.analysis-timeline details {
  overflow: hidden;
  border: 1px solid var(--hp-line);
  border-radius: 14px;
  background: var(--hp-surface);
}

.analysis-timeline summary {
  display: grid;
  grid-template-columns: 74px minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  list-style: none;
  cursor: pointer;
}

.analysis-timeline summary::-webkit-details-marker {
  display: none;
}

.analysis-round {
  padding: 5px 8px;
  border-radius: 999px;
  color: var(--hp-blue-ink);
  background: var(--hp-blue);
  font-size: 11px;
  font-weight: 750;
  text-align: center;
}

.analysis-timeline summary strong {
  display: block;
  font-size: 15px;
}

.analysis-timeline summary p {
  margin: 4px 0 0;
  color: var(--hp-muted);
  font-size: 12px;
  line-height: 1.55;
}

.analysis-timeline summary em {
  color: var(--hp-blue-ink);
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
  white-space: nowrap;
}

.analysis-body {
  display: grid;
  gap: 14px;
  padding: 0 18px 18px;
  border-top: 1px solid var(--hp-line);
}

.analysis-compare {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  padding-top: 16px;
}

.analysis-compare article {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  padding: 16px;
  border-radius: 13px;
  background: var(--hp-surface-2);
}

.analysis-compare article > span,
.analysis-compare article > h3 {
  grid-column: 1 / -1;
}

.analysis-compare article > span {
  color: var(--hp-muted);
  font-size: 11px;
  font-weight: 700;
}

.analysis-compare article h3 {
  margin: 0;
  font-size: 16px;
}

.analysis-compare article > div {
  display: grid;
  gap: 4px;
  padding-top: 10px;
  border-top: 1px solid var(--hp-line);
}

.analysis-compare small {
  color: var(--hp-muted);
  font-size: 10.5px;
}

.analysis-compare strong {
  font-size: 13px;
}

.analysis-compare article.is-best {
  border: 1px solid #bfd6c6;
  background: #f1f8f3;
}

.analysis-alternatives {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 9px;
}

.analysis-alternatives article {
  display: grid;
  gap: 5px;
  padding: 13px 14px;
  border: 1px solid var(--hp-line);
  border-radius: 11px;
  background: var(--hp-surface-2);
}

.analysis-alternatives span {
  color: var(--hp-muted);
  font-size: 11px;
}

.analysis-alternatives strong {
  font-size: 13px;
}

.analysis-alternatives em {
  color: var(--hp-blue-ink);
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
}

.analysis-reasons {
  padding: 16px;
  border-radius: 12px;
  background: #fffaf0;
}

.analysis-reasons h4 {
  margin: 0;
  font-size: 14px;
}

.analysis-reasons ul {
  display: grid;
  gap: 7px;
  margin: 11px 0 0;
  padding-left: 18px;
  color: var(--hp-ink-2);
  font-size: 12.5px;
  line-height: 1.65;
}
.report-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(320px, .75fr);
  gap: 16px;
}

.final-ranking {
  display: grid;
  gap: 8px;
  margin-top: 18px;
}

.final-ranking article {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1px solid var(--hp-line);
  border-radius: 12px;
  background: var(--hp-surface-2);
}

.final-ranking article.is-user {
  border-color: #afc4d1;
  background: var(--hp-blue);
}

.final-ranking article > span {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 50%;
  color: var(--hp-blue-ink);
  background: #fff;
  font-size: 12px;
  font-weight: 800;
}

.final-ranking article > div {
  display: grid;
  gap: 4px;
}

.final-ranking strong {
  font-size: 14px;
}

.final-ranking small,
.final-ranking em {
  color: var(--hp-muted);
  font-size: 12px;
  font-style: normal;
}

.final-ranking em {
  color: var(--hp-blue-ink);
  font-weight: 750;
}

.report-history article,
.evaluation-panel article {
  display: grid;
  grid-template-columns: 72px minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  padding: 14px 0;
  border-top: 1px solid var(--hp-line);
}

.report-history article > span,
.report-history article > small {
  color: var(--hp-muted);
  font-size: 12px;
}

.report-history article > small {
  text-align: right;
}

.evaluation-panel article > div:first-child {
  display: grid;
  gap: 5px;
}

.evaluation-panel article span {
  color: var(--hp-muted);
  font-size: 12px;
}

.evaluation-score {
  display: grid;
  width: 42px;
  height: 42px;
  place-items: center;
  border-radius: 50%;
  color: var(--hp-blue-ink);
  background: var(--hp-blue);
  font-weight: 800;
}

.evidence-note {
  margin: 16px 0 0;
  padding: 12px;
  border-radius: 10px;
  color: var(--hp-ink-2);
  background: var(--hp-surface-2);
  font-size: 12px;
  line-height: 1.7;
}

.report-actions {
  justify-content: center;
  margin-top: 20px;
}

.result-mask {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: grid;
  place-items: center;
  padding: 20px;
  background: rgba(27, 35, 41, .36);
}

.result-dialog {
  width: min(560px, 100%);
  padding: 26px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-lg);
  background: var(--hp-surface);
  box-shadow: var(--hp-shadow-lg);
}

.result-dialog header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 18px;
}

.result-dialog h2 {
  margin: 0 0 6px;
}

.result-dialog header span {
  color: var(--hp-muted);
  font-size: 12px;
}

.result-dialog header button {
  width: 36px;
  height: 36px;
  border: 0;
  border-radius: 50%;
  color: var(--hp-ink-2);
  background: var(--hp-surface-2);
  font-size: 22px;
  cursor: pointer;
}

.result-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 20px;
}

.result-grid > div {
  display: grid;
  gap: 6px;
  padding: 14px;
  border-radius: 12px;
  background: var(--hp-surface-2);
}

.result-grid span {
  color: var(--hp-muted);
  font-size: 12px;
}

.result-grid strong {
  font-size: 20px;
}

.result-ranking {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 14px;
}

.result-ranking span {
  padding: 6px 9px;
  border: 1px solid var(--hp-line);
  border-radius: 999px;
  color: var(--hp-ink-2);
  background: var(--hp-surface-2);
  font-size: 11px;
  font-weight: 650;
}

.result-ranking span.is-user {
  border-color: #afc4d1;
  color: var(--hp-blue-ink);
  background: var(--hp-blue);
}

.positive {
  color: var(--hp-green-ink);
}

.negative {
  color: #a45f66;
}

.result-dialog ul {
  display: grid;
  gap: 8px;
  margin: 18px 0 0;
  padding-left: 18px;
  color: var(--hp-ink-2);
  font-size: 13px;
  line-height: 1.7;
}

.result-dialog footer {
  display: flex;
  justify-content: flex-end;
  margin-top: 22px;
}

.scenario-toggle {
  display: flex;
  justify-content: center;
  margin-top: 18px;
  padding-top: 16px;
  border-top: 1px solid var(--hp-line);
}

.scenario-toggle button {
  min-height: 40px;
  padding: 0 18px;
  border: 1px solid var(--hp-line-strong);
  border-radius: 999px;
  color: var(--hp-ink);
  background: var(--hp-surface-2);
  font-size: 13px;
  font-weight: 650;
  cursor: pointer;
}

.scenario-toggle button:hover {
  border-color: var(--hp-ink);
}
.multiplayer-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-top: 16px;
  padding: 24px 28px;
}

.multiplayer-entry h2 {
  margin: 0 0 7px;
  font-size: 20px;
}

.multiplayer-entry p {
  margin: 0;
  color: var(--hp-ink-2);
  font-size: 13.5px;
  line-height: 1.65;
}
@media (max-width: 1050px) {
  .scenario-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .sandbox-flow {
    grid-template-columns: repeat(5, auto);
    overflow-x: auto;
  }

  .sandbox-flow i {
    display: none;
  }

  .metric-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .decision-layout,
  .process-analysis {
  margin-top: 16px;
  padding: 24px 28px;
}

.analysis-timeline {
  display: grid;
  gap: 10px;
  margin-top: 18px;
}

.analysis-timeline details {
  overflow: hidden;
  border: 1px solid var(--hp-line);
  border-radius: 14px;
  background: var(--hp-surface);
}

.analysis-timeline summary {
  display: grid;
  grid-template-columns: 74px minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  padding: 16px 18px;
  list-style: none;
  cursor: pointer;
}

.analysis-timeline summary::-webkit-details-marker {
  display: none;
}

.analysis-round {
  padding: 5px 8px;
  border-radius: 999px;
  color: var(--hp-blue-ink);
  background: var(--hp-blue);
  font-size: 11px;
  font-weight: 750;
  text-align: center;
}

.analysis-timeline summary strong {
  display: block;
  font-size: 15px;
}

.analysis-timeline summary p {
  margin: 4px 0 0;
  color: var(--hp-muted);
  font-size: 12px;
  line-height: 1.55;
}

.analysis-timeline summary em {
  color: var(--hp-blue-ink);
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
  white-space: nowrap;
}

.analysis-body {
  display: grid;
  gap: 14px;
  padding: 0 18px 18px;
  border-top: 1px solid var(--hp-line);
}

.analysis-compare {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  padding-top: 16px;
}

.analysis-compare article {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  padding: 16px;
  border-radius: 13px;
  background: var(--hp-surface-2);
}

.analysis-compare article > span,
.analysis-compare article > h3 {
  grid-column: 1 / -1;
}

.analysis-compare article > span {
  color: var(--hp-muted);
  font-size: 11px;
  font-weight: 700;
}

.analysis-compare article h3 {
  margin: 0;
  font-size: 16px;
}

.analysis-compare article > div {
  display: grid;
  gap: 4px;
  padding-top: 10px;
  border-top: 1px solid var(--hp-line);
}

.analysis-compare small {
  color: var(--hp-muted);
  font-size: 10.5px;
}

.analysis-compare strong {
  font-size: 13px;
}

.analysis-compare article.is-best {
  border: 1px solid #bfd6c6;
  background: #f1f8f3;
}

.analysis-alternatives {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 9px;
}

.analysis-alternatives article {
  display: grid;
  gap: 5px;
  padding: 13px 14px;
  border: 1px solid var(--hp-line);
  border-radius: 11px;
  background: var(--hp-surface-2);
}

.analysis-alternatives span {
  color: var(--hp-muted);
  font-size: 11px;
}

.analysis-alternatives strong {
  font-size: 13px;
}

.analysis-alternatives em {
  color: var(--hp-blue-ink);
  font-size: 12px;
  font-style: normal;
  font-weight: 700;
}

.analysis-reasons {
  padding: 16px;
  border-radius: 12px;
  background: #fffaf0;
}

.analysis-reasons h4 {
  margin: 0;
  font-size: 14px;
}

.analysis-reasons ul {
  display: grid;
  gap: 7px;
  margin: 11px 0 0;
  padding-left: 18px;
  color: var(--hp-ink-2);
  font-size: 12.5px;
  line-height: 1.65;
}
.report-layout {
    grid-template-columns: 1fr;
  }

  .market-panel {
    position: static;
  }
}

@media (max-width: 680px) {
.analysis-timeline summary {
    grid-template-columns: 64px minmax(0, 1fr);
  }

  .analysis-timeline summary em {
    grid-column: 2;
  }

  .analysis-compare,
  .analysis-alternatives {
    grid-template-columns: 1fr;
  }

  .analysis-compare article {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

.event-mobile-reminder {
  position: sticky;
  top: 72px;
  z-index: 14;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid #eadab0;
  border-radius: 12px;
  background: rgba(255, 250, 240, .98);
  box-shadow: var(--hp-shadow-sm);
}

.event-mobile-reminder > div {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.event-mobile-reminder span {
  color: #8a6a35;
  font-size: 10px;
  font-weight: 700;
}

.event-mobile-reminder strong {
  overflow: hidden;
  font-size: 13px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.event-mobile-reminder button {
  flex: none;
  padding: 0;
  border: 0;
  color: var(--hp-blue-ink);
  background: transparent;
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.event-detail-grid {
  grid-template-columns: 1fr;
}

  .sandbox-shell {
    width: min(100% - 24px, 1280px);
    padding-top: 82px;
  }

  .sandbox-hero,
  .report-hero {
    display: grid;
  }

  .sandbox-hero__aside {
    display: grid;
    justify-items: start;
  }

  .hero-meta {
    justify-content: flex-start;
  }

  .scenario-grid,
  .metric-grid,
  .round-steps,
  .expansion-options {
    grid-template-columns: 1fr;
  }

.history-list article {
    grid-template-columns: 1fr;
    gap: 8px;
  }

  .report-history article,
  .evaluation-panel article {
    grid-template-columns: 1fr auto;
  }

  .report-history article > small {
    grid-column: 1 / -1;
    text-align: left;
  }
}

.history-list article:first-child {
  border-top: 0;
}
</style>
