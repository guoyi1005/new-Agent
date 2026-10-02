<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'

import AppTabBar from '../components/AppTabBar.vue'
import {
  confirmSandboxCompany,
  createSandboxCompany,
  getSandboxRoom,
  joinSandboxCompany,
  saveSandboxDraft,
  settleSandboxRoom,
  startSandboxRoom,
} from '../api/businessSandboxRoom'
import { getSandboxScenario } from '../utils/businessSandbox'

const route = useRoute()

const ROLE_OPTIONS = [
  { id: 'PRESIDENT', label: '总裁', description: '选择整体战略和扩张方向' },
  { id: 'MARKET', label: '市场负责人', description: '负责推广预算和市场增长' },
  { id: 'OPERATIONS', label: '运营负责人', description: '负责服务质量和治理投入' },
  { id: 'FINANCE', label: '财务负责人', description: '负责费率、激励和成本平衡' },
  { id: 'PRODUCT', label: '产品负责人', description: '负责产品研发和创新投入' },
]
const room = ref(null)
const loading = ref(true)
const busy = ref('')
const error = ref('')
const notice = ref('')
const copyFeedback = ref('复制邀请链接')
let copyResetTimer = null
const companyName = ref('')
let timer = null

const decision = reactive({
  strategyId: 'steady',
  marketingBudget: 9000,
  serviceBudget: 6000,
  innovationBudget: 5000,
  commissionRate: 6,
  sellerIncentive: 3,
  expansion: 'none',
})

const scenario = computed(() => getSandboxScenario(room.value?.scenarioId))
const companies = computed(() => Array.isArray(room.value?.companies) ? room.value.companies : [])
const myCompany = computed(() => companies.value.find((item) => item.isMyCompany) || null)
const myRole = computed(() => room.value?.myRole || '')
const isParticipant = computed(() => Boolean(myCompany.value))
const isCaptain = computed(() => Boolean(myCompany.value?.isCaptain))
const roleLabel = (role) => ROLE_OPTIONS.find((item) => item.id === role)?.label || role
const statusLabel = computed(() => ({ WAITING: '等待成员', IN_PROGRESS: '经营中', COMPLETED: '已结束' })[room.value?.status] || '')
const allMembersSubmitted = computed(() => {
  const statuses = myCompany.value?.draftStatuses || []
  const memberRoles = new Set((myCompany.value?.members || []).map((item) => item.role))
  return statuses.filter((item) => memberRoles.has(item.role)).every((item) => item.submitted)
})

function availableRoles(company) {
  const occupied = new Set((company.members || []).map((item) => item.role))
  return ROLE_OPTIONS.filter((item) => !occupied.has(item.id))
}

function applyMyDraft() {
  const draft = myCompany.value?.myDraft?.decision || {}
  decision.strategyId = draft.strategyId || 'steady'
  decision.marketingBudget = Number(draft.marketingBudget || decision.marketingBudget)
  decision.serviceBudget = Number(draft.serviceBudget || decision.serviceBudget)
  decision.innovationBudget = Number(draft.innovationBudget || decision.innovationBudget)
  decision.commissionRate = Number(draft.commissionRate || decision.commissionRate)
  decision.sellerIncentive = Number(draft.sellerIncentive || decision.sellerIncentive)
  decision.expansion = draft.expansion || 'none'
}

async function loadRoom(silent = false) {
  if (!silent) loading.value = true
  error.value = ''
  try {
    room.value = await getSandboxRoom(route.params.roomId)
    applyMyDraft()
  } catch (cause) {
    error.value = cause.message || '房间加载失败'
  } finally {
    if (!silent) loading.value = false
  }
}

async function createCompany() {
  if (!companyName.value.trim() || busy.value) return
  busy.value = 'create-company'
  error.value = ''
  try {
    room.value = await createSandboxCompany(room.value.id, companyName.value.trim())
    companyName.value = ''
    notice.value = '公司已创建，你已成为队长和总裁'
  } catch (cause) {
    error.value = cause.message || '公司创建失败'
  } finally {
    busy.value = ''
  }
}

async function joinCompany(companyId, role) {
  if (busy.value) return
  busy.value = `join-${companyId}-${role}`
  error.value = ''
  try {
    room.value = await joinSandboxCompany(room.value.id, companyId, role)
    notice.value = `已加入公司，角色：${roleLabel(role)}`
    applyMyDraft()
  } catch (cause) {
    error.value = cause.message || '加入公司失败'
  } finally {
    busy.value = ''
  }
}

async function submitDraft() {
  if (!myCompany.value || !myRole.value || busy.value) return
  busy.value = 'draft'
  error.value = ''
  try {
    room.value = await saveSandboxDraft(room.value.id, myCompany.value.id, myRole.value, { ...decision })
    notice.value = '本轮决策已提交给队长'
    applyMyDraft()
  } catch (cause) {
    error.value = cause.message || '决策提交失败'
  } finally {
    busy.value = ''
  }
}

async function confirmCompany() {
  if (!myCompany.value || busy.value) return
  busy.value = 'confirm'
  error.value = ''
  try {
    room.value = await confirmSandboxCompany(room.value.id, myCompany.value.id)
    notice.value = '公司决策已统一确认'
  } catch (cause) {
    error.value = cause.message || '确认失败'
  } finally {
    busy.value = ''
  }
}

async function startRoom() {
  if (busy.value) return
  busy.value = 'start'
  error.value = ''
  try {
    room.value = await startSandboxRoom(room.value.id)
    notice.value = '经营活动已开始'
  } catch (cause) {
    error.value = cause.message || '开局失败'
  } finally {
    busy.value = ''
  }
}

async function settleRoom() {
  if (busy.value) return
  busy.value = 'settle'
  error.value = ''
  try {
    room.value = await settleSandboxRoom(room.value.id)
    notice.value = room.value.status === 'COMPLETED' ? '全部经营已完成' : '本轮结算完成，进入下一轮'
    applyMyDraft()
  } catch (cause) {
    error.value = cause.message || '结算失败'
  } finally {
    busy.value = ''
  }
}

function fallbackCopy(text) {
  const input = document.createElement('textarea')
  input.value = text
  input.setAttribute('readonly', '')
  input.style.position = 'fixed'
  input.style.opacity = '0'
  document.body.appendChild(input)
  input.select()
  input.setSelectionRange(0, input.value.length)
  const copied = document.execCommand('copy')
  document.body.removeChild(input)
  return copied
}

async function copyInvite() {
  const roomId = room.value?.id || route.params.roomId
  if (!roomId) {
    error.value = '无法获取房间编号，请刷新页面后重试'
    return
  }
  const url = `${window.location.origin}/learning/projects/sandbox/rooms/${roomId}`
  try {
    if (navigator.clipboard?.writeText) {
      await navigator.clipboard.writeText(url)
    } else if (!fallbackCopy(url)) {
      throw new Error('copy_not_supported')
    }
    copyFeedback.value = '已复制邀请链接'
    notice.value = '邀请链接已复制，可以发给小组成员'
  } catch {
    if (fallbackCopy(url)) {
      copyFeedback.value = '已复制邀请链接'
      notice.value = '邀请链接已复制，可以发给小组成员'
    } else {
      copyFeedback.value = '请手动复制'
      notice.value = '浏览器阻止了自动复制，请在弹窗中手动复制'
      window.prompt('复制邀请链接', url)
    }
  }
  window.clearTimeout(copyResetTimer)
  copyResetTimer = window.setTimeout(() => { copyFeedback.value = '复制邀请链接' }, 2200)
}


function formatTime(value) { return value ? String(value).replace('T', ' ').slice(0, 16) : '' }

onMounted(() => {
  loadRoom()
  timer = window.setInterval(() => loadRoom(true), 5000)
})
onBeforeUnmount(() => { window.clearInterval(timer); window.clearTimeout(copyResetTimer) })
</script>

<template>
  <div class="room-detail-page">
    <AppTabBar />
    <main class="room-detail-shell">
      <header class="room-detail-hero">
        <div><h1>{{ room?.name || '经营房间' }}</h1><p v-if="room">{{ scenario.title }} · 房间码 {{ room.roomCode }}</p></div>
        <div class="room-detail-actions"><button class="room-btn" :class="{ copied: copyFeedback === '已复制邀请链接' }" @click="copyInvite">{{ copyFeedback }}</button><a href="/learning/projects/sandbox/rooms" class="room-btn">返回房间大厅</a></div>
      </header>

      <p v-if="error" class="room-error">{{ error }}</p>
      <p v-if="notice" class="room-notice">{{ notice }}</p>

      <div v-if="loading" class="room-state">正在加载房间…</div>
      <template v-else-if="room">
        <section class="room-summary">
          <article><span>房间状态</span><strong>{{ statusLabel }}</strong></article>
          <article><span>经营轮次</span><strong>{{ room.currentRound }}/{{ room.roundCount }}</strong></article>
          <article><span>公司数量</span><strong>{{ room.companyCount }}/{{ room.maxCompanies }}</strong></article>
          <article><span>成员数量</span><strong>{{ room.memberCount }}</strong></article>
          <article><span>我的角色</span><strong>{{ roleLabel(myRole) }}</strong></article>
        </section>

        <section class="panel company-section">
          <div class="section-head"><div><h2>公司与成员</h2><span>每个成员承担一个角色，队长负责统一确认</span></div></div>
          <div class="company-grid">
            <article v-for="company in companies" :key="company.id" class="company-card" :class="{ 'is-mine': company.isMyCompany }">
              <div class="company-head"><div><h3>{{ company.name }}</h3><span>{{ company.members.length }} 名成员</span></div><em>{{ company.confirmed ? '已确认' : '待确认' }}</em></div>
              <div class="member-list">
                <div v-for="member in company.members" :key="member.userId"><span>{{ member.username }}</span><strong>{{ roleLabel(member.role) }}</strong></div>
              </div>
              <div v-if="room.status !== 'WAITING'" class="company-metrics"><span>现金 {{ Number(company.cash || 0).toLocaleString('zh-CN') }}</span><span>用户 {{ Number(company.users || 0).toLocaleString('zh-CN') }}</span><span>满意度 {{ company.satisfaction }}%</span></div>
              <div v-if="room.status === 'WAITING' && !isParticipant && availableRoles(company).length" class="role-join">
                <button v-for="role in availableRoles(company)" :key="role.id" type="button" :disabled="busy === `join-${company.id}-${role.id}`" @click="joinCompany(company.id, role.id)">加入 · {{ role.label }}</button>
              </div>
            </article>
          </div>
        </section>

        <section v-if="room.status === 'WAITING' && !isParticipant && companies.length < room.maxCompanies" class="panel create-company">
          <div><h2>创建一家公司</h2><p>创建后你将成为公司队长和总裁，并邀请其他成员加入。</p></div>
          <form @submit.prevent="createCompany"><input v-model="companyName" maxlength="80" placeholder="输入公司名称" /><button class="room-btn room-btn--primary" :disabled="busy === 'create-company'">创建公司</button></form>
        </section>

        <section v-if="room.isHost && room.status === 'WAITING'" class="panel host-action">
          <div><h2>房主控制</h2><p>至少 2 家公司，每家公司至少 3 名成员后才能开始经营。</p></div>
          <button class="room-btn room-btn--primary" :disabled="!room.canStart || busy === 'start'" @click="startRoom">开始经营</button>
        </section>

        <section v-if="isParticipant && room.status === 'IN_PROGRESS'" class="panel decision-panel">
          <div class="section-head"><div><h2>{{ myCompany.name }} · {{ roleLabel(myRole) }}</h2><span>提交你负责的本轮经营决策</span></div><em :class="{ done: myCompany.myDraft }">{{ myCompany.myDraft ? '已提交' : '未提交' }}</em></div>

          <template v-if="myRole === 'PRESIDENT'">
            <label class="field"><span>整体经营策略</span><select v-model="decision.strategyId"><option value="growth">激进增长</option><option value="steady">稳健运营</option><option value="experience">体验优先</option><option value="innovation">产品创新</option></select></label>
            <label class="field"><span>扩张选择</span><select v-model="decision.expansion"><option value="none">暂不扩张</option><option value="pilot">试点扩张</option><option value="new">新增服务区域</option></select></label>
          </template>
          <label v-else-if="myRole === 'MARKET'" class="field"><span>推广预算</span><input v-model.number="decision.marketingBudget" type="range" min="0" max="16000" step="1000" /><em>¥{{ decision.marketingBudget }}</em></label>
          <label v-else-if="myRole === 'OPERATIONS'" class="field"><span>服务与治理投入</span><input v-model.number="decision.serviceBudget" type="range" min="0" max="16000" step="1000" /><em>¥{{ decision.serviceBudget }}</em></label>
          <template v-else-if="myRole === 'FINANCE'">
            <label class="field"><span>服务费率</span><input v-model.number="decision.commissionRate" type="range" min="3" max="20" step="0.5" /><em>{{ decision.commissionRate }}%</em></label>
            <label class="field"><span>合作激励</span><input v-model.number="decision.sellerIncentive" type="range" min="0" max="10" step="0.5" /><em>{{ decision.sellerIncentive }}%</em></label>
          </template>
          <label v-else class="field"><span>产品与创新投入</span><input v-model.number="decision.innovationBudget" type="range" min="0" max="14000" step="1000" /><em>¥{{ decision.innovationBudget }}</em></label>

          <footer class="panel-actions"><button class="room-btn room-btn--primary" :disabled="busy === 'draft'" @click="submitDraft">{{ busy === 'draft' ? '提交中…' : '提交我的决策' }}</button><button v-if="isCaptain" class="room-btn" :disabled="!allMembersSubmitted || busy === 'confirm'" @click="confirmCompany">{{ allMembersSubmitted ? '队长统一确认' : '等待成员提交' }}</button></footer>
        </section>

        <section v-if="room.isHost && room.status === 'IN_PROGRESS'" class="panel host-action"><div><h2>本轮统一结算</h2><p>所有公司确认后，由房主结算并推进下一轮。</p></div><button class="room-btn room-btn--primary" :disabled="!room.canSettle || busy === 'settle'" @click="settleRoom">{{ room.canSettle ? '结算本轮' : '等待公司确认' }}</button></section>

        <section v-if="room.lastRoundResult" class="panel result-panel">
          <div class="section-head"><div><h2>第 {{ room.lastRoundResult.round }} 轮结果</h2><span>{{ room.lastRoundResult.event?.title }}</span></div></div>
          <div class="ranking-list"><article v-for="item in room.lastRoundResult.ranking" :key="item.companyId" :class="{ 'is-mine': item.companyId === room.myCompanyId }"><span>{{ item.rank }}</span><strong>{{ item.companyName }}</strong><em>{{ item.score }} 分</em></article></div>
        </section>
      </template>
    </main>
  </div>
</template>

<style scoped>
.room-detail-page{min-height:100vh;color:var(--hp-ink);background:var(--hp-bg);font-family:Inter,'Segoe UI',system-ui,-apple-system,'PingFang SC','Microsoft YaHei',sans-serif}.room-detail-page *{box-sizing:border-box}.room-detail-shell{width:min(1280px,calc(100% - 48px));margin:0 auto;padding:96px 0 80px}.room-detail-hero,.panel,.room-summary article{border:1px solid var(--hp-line);background:var(--hp-surface);box-shadow:var(--hp-shadow-sm)}.room-detail-hero{display:flex;align-items:center;justify-content:space-between;gap:20px;padding:24px 28px;border-color:#e4ebf2;border-radius:var(--hp-r-lg);background:var(--hp-tint)}.room-detail-hero h1{margin:0 0 7px;font-size:25px}.room-detail-hero p,.section-head span,.company-card p,.host-action p,.create-company p{margin:0;color:var(--hp-ink-2);font-size:13px;line-height:1.65}.room-detail-actions,.panel-actions{display:flex;flex-wrap:wrap;gap:10px}.room-btn{display:inline-flex;align-items:center;justify-content:center;min-height:42px;padding:0 17px;border:1px solid var(--hp-line-strong);border-radius:999px;color:var(--hp-ink);background:var(--hp-surface);font-size:13px;font-weight:650;text-decoration:none;cursor:pointer;box-sizing:border-box}.room-btn--primary{border-color:var(--hp-ink);color:#fff;background:var(--hp-ink)}.room-btn:disabled{opacity:.5;cursor:wait}.room-btn.copied{border-color:#9fb9aa;color:#3f6a52;background:#edf7f1}.room-error,.room-notice{margin-top:14px;padding:12px 16px;border-radius:12px;font-size:13px}.room-error{color:#8b4c49;background:#f8eeee}.room-notice{color:#3a6654;background:#eef7f2}.room-state{padding:40px;text-align:center;color:var(--hp-muted)}.room-summary{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:12px;margin-top:16px}.room-summary article{display:grid;gap:7px;padding:18px;border-radius:var(--hp-r-md)}.room-summary span{color:var(--hp-muted);font-size:12px}.room-summary strong{font-size:18px}.panel{margin-top:16px;padding:24px 28px;border-radius:var(--hp-r-lg)}.section-head{display:flex;align-items:flex-start;justify-content:space-between;gap:16px}.section-head h2{margin:0 0 5px;font-size:20px}.section-head em{padding:5px 9px;border-radius:999px;color:var(--hp-muted);background:var(--hp-surface-2);font-size:11px;font-style:normal}.section-head em.done{color:var(--hp-blue-ink);background:var(--hp-blue)}.company-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px;margin-top:18px}.company-card{display:grid;gap:14px;padding:20px;border:1px solid var(--hp-line);border-radius:var(--hp-r-md);background:var(--hp-surface-2)}.company-card.is-mine{border-color:#afc4d1;background:#f2f7fa}.company-head{display:flex;align-items:flex-start;justify-content:space-between;gap:12px}.company-head h3{margin:0 0 5px;font-size:18px}.company-head span{color:var(--hp-muted);font-size:12px}.company-head em{padding:5px 8px;border-radius:999px;color:var(--hp-ink-2);background:var(--hp-surface);font-size:11px;font-style:normal}.member-list{display:grid;gap:7px}.member-list div{display:flex;align-items:center;justify-content:space-between;gap:10px;padding:8px 10px;border-radius:9px;background:var(--hp-surface)}.member-list strong{color:var(--hp-blue-ink);font-size:12px}.company-metrics{display:flex;flex-wrap:wrap;gap:7px}.company-metrics span{padding:5px 8px;border-radius:999px;color:var(--hp-ink-2);background:var(--hp-surface);font-size:11px}.role-join{display:flex;flex-wrap:wrap;gap:6px}.role-join button{padding:6px 9px;border:1px solid var(--hp-line-strong);border-radius:999px;color:var(--hp-blue-ink);background:#fff;font-size:11px;cursor:pointer}.create-company,.host-action{display:flex;align-items:center;justify-content:space-between;gap:20px}.create-company form{display:flex;gap:8px}.create-company input{min-width:220px;padding:10px 12px;border:1px solid var(--hp-line-strong);border-radius:10px;background:var(--hp-surface-2)}.decision-panel{display:grid;gap:18px}.field{display:grid;gap:9px}.field>span{font-size:13px;font-weight:700}.field input,.field select{width:100%;padding:10px;border:1px solid var(--hp-line-strong);border-radius:10px;background:var(--hp-surface-2)}.field em{color:var(--hp-blue-ink);font-style:normal;font-weight:700}.ranking-list{display:grid;gap:8px;margin-top:16px}.ranking-list article{display:grid;grid-template-columns:34px minmax(0,1fr) auto;align-items:center;gap:12px;padding:11px 13px;border:1px solid var(--hp-line);border-radius:11px;background:var(--hp-surface-2)}.ranking-list article.is-mine{border-color:#afc4d1;background:var(--hp-blue)}.ranking-list article>span{display:grid;width:28px;height:28px;place-items:center;border-radius:50%;background:#fff;color:var(--hp-blue-ink);font-weight:800}.ranking-list em{color:var(--hp-blue-ink);font-style:normal;font-weight:700}@media(max-width:900px){.room-summary{grid-template-columns:repeat(3,minmax(0,1fr))}.company-grid{grid-template-columns:1fr}.create-company,.host-action{display:grid}}@media(max-width:680px){.room-detail-shell{width:min(100% - 24px,1280px);padding-top:82px}.room-detail-hero{display:grid}.room-summary{grid-template-columns:1fr}.create-company form{flex-direction:column}.create-company input{min-width:0;width:100%}}
</style>