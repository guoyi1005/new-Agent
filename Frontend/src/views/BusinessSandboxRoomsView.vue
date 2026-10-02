<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import AppTabBar from '../components/AppTabBar.vue'
import { createSandboxRoom, listMySandboxRooms } from '../api/businessSandboxRoom'
import { BUSINESS_SANDBOX_SCENARIOS, getSandboxScenario } from '../utils/businessSandbox'

const router = useRouter()
const scenarios = BUSINESS_SANDBOX_SCENARIOS
const rooms = ref([])
const loading = ref(true)
const creating = ref(false)
const error = ref('')
const inviteInput = ref('')
const copyFeedback = ref('复制最近邀请链接')
let copyResetTimer = null
const createOpen = ref(false)
const form = reactive({ scenarioId: scenarios[0]?.id, name: '小组经营房间', maxCompanies: 4, maxMembers: 5 })

const statusLabel = computed(() => ({
  WAITING: '等待成员',
  IN_PROGRESS: '经营中',
  COMPLETED: '已结束',
}))

async function loadRooms() {
  loading.value = true
  error.value = ''
  try {
    rooms.value = await listMySandboxRooms() || []
  } catch (cause) {
    error.value = cause.message || '房间加载失败'
  } finally {
    loading.value = false
  }
}

async function submitCreate() {
  if (!form.scenarioId || creating.value) return
  creating.value = true
  error.value = ''
  try {
    const room = await createSandboxRoom({
      scenarioId: form.scenarioId,
      name: form.name,
      maxCompanies: form.maxCompanies,
      maxMembers: form.maxMembers,
    })
    createOpen.value = false
    await loadRooms()
    router.push(`/learning/projects/sandbox/rooms/${room.id}`)
  } catch (cause) {
    error.value = cause.message || '房间创建失败'
  } finally {
    creating.value = false
  }
}

function scenarioTitle(room) {
  return getSandboxScenario(room.scenarioId).title
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

function roomUrl(roomId) {
  return `${window.location.origin}/learning/projects/sandbox/rooms/${roomId}`
}

function rememberRoom(roomId) {
  if (!roomId) return
  try { localStorage.setItem('last_sandbox_room_id', String(roomId)) } catch { /* ignore */ }
}

async function writeClipboard(text) {
  try {
    if (navigator.clipboard?.writeText) await navigator.clipboard.writeText(text)
    else if (!fallbackCopy(text)) throw new Error('copy_not_supported')
    copyFeedback.value = '已复制邀请链接'
  } catch {
    if (fallbackCopy(text)) {
      copyFeedback.value = '已复制邀请链接'
    } else {
      copyFeedback.value = '请手动复制'
      window.prompt('复制邀请链接', text)
    }
  }
  window.clearTimeout(copyResetTimer)
  copyResetTimer = window.setTimeout(() => { copyFeedback.value = '复制最近邀请链接' }, 2200)
}

async function copyInvite(room) {
  if (!room?.id) return
  rememberRoom(room.id)
  await writeClipboard(roomUrl(room.id))
}

async function copyLastInvite() {
  let roomId = rooms.value[0]?.id
  if (!roomId) {
    try { roomId = localStorage.getItem('last_sandbox_room_id') } catch { roomId = null }
  }
  if (!roomId) {
    error.value = '还没有可复制的房间，请先创建房间或粘贴邀请链接进入'
    return
  }
  await writeClipboard(roomUrl(roomId))
}

function extractRoomId(value) {
  const text = String(value || '').trim()
  if (!text) return ''
  const match = text.match(/\/rooms\/([^/?#]+)/)
  return match ? match[1] : text
}

function openInvite() {
  const roomId = extractRoomId(inviteInput.value)
  if (!roomId) {
    error.value = '请粘贴邀请链接或输入房间编号'
    return
  }
  rememberRoom(roomId)
  router.push(`/learning/projects/sandbox/rooms/${roomId}`)
}

function formatTime(value) {
  return value ? String(value).replace('T', ' ').slice(0, 16) : ''
}

onMounted(loadRooms)
</script>

<template>
  <div class="room-page">
    <AppTabBar />
    <main class="room-shell">
      <header class="room-hero">
        <div>
          <h1>多人经营房间</h1>
          <p>2 至 4 家公司分组经营，每家公司由 3 至 5 名成员承担不同角色。</p>
        </div>
        <div class="room-actions">
          <button type="button" class="room-button" @click="router.push('/learning/projects/sandbox')">返回单机沙盘</button>
          <button type="button" class="room-button" @click="copyLastInvite">{{ copyFeedback }}</button>
          <button type="button" class="room-button room-button--primary" @click="createOpen = true">创建房间</button>
        </div>
      </header>

      <p v-if="error" class="room-error">{{ error }}</p>

      <section class="panel invite-entry">
        <div><h2>进入邀请房间</h2><p>粘贴同学发来的邀请链接，或者直接输入房间编号。</p></div>
        <form @submit.prevent="openInvite"><input v-model="inviteInput" placeholder="粘贴邀请链接或房间编号" /><button class="room-button room-button--primary" type="submit">进入房间</button></form>
      </section>
      <section class="panel room-list-panel">
        <div class="panel-head"><div><h2>我的房间</h2><span>加入过的多人经营房间</span></div><span>{{ rooms.length }} 个</span></div>
        <div v-if="loading" class="room-state">正在加载房间…</div>
        <div v-else-if="rooms.length" class="room-grid">
          <article v-for="room in rooms" :key="room.id" class="room-card">
            <div class="room-card__top"><span>{{ room.roomCode }}</span><em>{{ statusLabel[room.status] }}</em></div>
            <h3>{{ room.name }}</h3>
            <p>{{ scenarioTitle(room) }}</p>
            <div class="room-meta">
              <span>{{ room.companyCount }}/{{ room.maxCompanies }} 家公司</span>
              <span>{{ room.memberCount }} 名成员</span>
              <span>第 {{ room.currentRound }}/{{ room.roundCount }} 轮</span>
            </div>
            <footer>
              <button type="button" class="room-button room-button--primary" @click="router.push(`/learning/projects/sandbox/rooms/${room.id}`)">进入房间</button>
              <button type="button" class="room-link" @click="copyInvite(room)">复制邀请链接</button>
            </footer>
          </article>
        </div>
        <div v-else class="room-empty"><strong>还没有多人经营房间</strong><p>创建房间后，邀请同学组成公司并分配角色。</p><button type="button" class="room-button room-button--primary" @click="createOpen = true">创建第一个房间</button></div>
      </section>

      <section class="panel room-rules">
        <div class="panel-head"><div><h2>经营规则</h2><span>多人协作需要统一确认后才能结算</span></div></div>
        <div class="rule-grid">
          <article><strong>角色协作</strong><p>总裁、市场、运营、财务和产品分别提交负责的决策。</p></article>
          <article><strong>队长确认</strong><p>所有成员提交后，由公司队长统一确认本轮方案。</p></article>
          <article><strong>统一结算</strong><p>房主在所有公司确认后推进下一轮，并生成公司排名。</p></article>
        </div>
      </section>
    </main>

    <Teleport to="body">
      <div v-if="createOpen" class="room-mask" @click.self="createOpen = false">
        <form class="room-dialog" @submit.prevent="submitCreate">
          <header><div><h2>创建经营房间</h2><p>先创建公司席位，再邀请成员加入角色。</p></div><button type="button" @click="createOpen = false">×</button></header>
          <label><span>经营场景</span><select v-model="form.scenarioId"><option v-for="item in scenarios" :key="item.id" :value="item.id">{{ item.title }}</option></select></label>
          <label><span>房间名称</span><input v-model="form.name" maxlength="120" type="text" /></label>
          <div class="dialog-row">
            <label><span>公司席位</span><select v-model.number="form.maxCompanies"><option :value="2">2 家</option><option :value="3">3 家</option><option :value="4">4 家</option></select></label>
            <label><span>每家公司人数</span><select v-model.number="form.maxMembers"><option :value="3">3 人</option><option :value="4">4 人</option><option :value="5">5 人</option></select></label>
          </div>
          <p v-if="error" class="room-error">{{ error }}</p>
          <footer><button type="button" class="room-button" @click="createOpen = false">取消</button><button type="submit" class="room-button room-button--primary" :disabled="creating">{{ creating ? '创建中…' : '创建并进入' }}</button></footer>
        </form>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.room-page{min-height:100vh;color:var(--hp-ink);background:var(--hp-bg);font-family:Inter,'Segoe UI',system-ui,-apple-system,'PingFang SC','Microsoft YaHei',sans-serif}.room-page *{box-sizing:border-box}.room-shell{width:min(1280px,calc(100% - 48px));margin:0 auto;padding:96px 0 80px}.room-hero,.panel{border:1px solid var(--hp-line);background:var(--hp-surface);box-shadow:var(--hp-shadow-sm)}.room-hero{display:flex;align-items:center;justify-content:space-between;gap:20px;padding:24px 28px;border-color:#e4ebf2;border-radius:var(--hp-r-lg);background:var(--hp-tint)}.room-hero h1{margin:0 0 7px;font-size:25px}.room-hero p,.panel-head span,.room-card p,.room-empty p,.rule-grid p{margin:0;color:var(--hp-ink-2);font-size:13px;line-height:1.65}.room-actions,.room-card footer,.room-dialog footer{display:flex;align-items:center;gap:10px}.room-button{min-height:42px;padding:0 18px;border:1px solid var(--hp-line-strong);border-radius:999px;color:var(--hp-ink);background:var(--hp-surface);font-size:13px;font-weight:650;cursor:pointer}.room-button--primary{border-color:var(--hp-ink);color:#fff;background:var(--hp-ink)}.room-button:disabled{opacity:.55;cursor:wait}.room-link{padding:0;border:0;color:var(--hp-blue-ink);background:transparent;font-size:12px;font-weight:700;cursor:pointer}.room-error{margin-top:14px;padding:12px 16px;border-radius:12px;color:#8b4c49;background:#f8eeee;font-size:13px}.panel{margin-top:16px;border-radius:var(--hp-r-lg)}.invite-entry{display:flex;align-items:center;justify-content:space-between;gap:20px;padding:20px 28px}.invite-entry h2{margin:0 0 5px;font-size:18px}.invite-entry p{margin:0;color:var(--hp-ink-2);font-size:13px}.invite-entry form{display:flex;gap:8px;min-width:min(100%,520px)}.invite-entry input{flex:1;min-width:0;padding:10px 12px;border:1px solid var(--hp-line-strong);border-radius:10px;background:var(--hp-surface-2)}.room-list-panel,.room-rules{padding:24px 28px}.panel-head{display:flex;align-items:flex-start;justify-content:space-between;gap:16px}.panel-head h2{margin:0 0 5px;font-size:20px}.room-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px;margin-top:18px}.room-card{display:grid;gap:12px;min-height:230px;padding:20px;border:1px solid var(--hp-line);border-radius:var(--hp-r-md);background:var(--hp-surface)}.room-card__top{display:flex;align-items:center;justify-content:space-between;gap:10px}.room-card__top span{color:var(--hp-blue-ink);font-size:12px;font-weight:800;letter-spacing:.08em}.room-card__top em{padding:5px 8px;border-radius:999px;color:var(--hp-ink-2);background:var(--hp-surface-2);font-size:11px;font-style:normal}.room-card h3{margin:0;font-size:18px}.room-card footer{margin-top:auto}.room-meta{display:flex;flex-wrap:wrap;gap:6px}.room-meta span{padding:5px 8px;border-radius:999px;color:var(--hp-ink-2);background:var(--hp-surface-2);font-size:11px}.room-state,.room-empty{padding:24px;border:1px dashed var(--hp-line-strong);border-radius:12px;color:var(--hp-muted);background:var(--hp-surface-2);text-align:center}.room-empty strong{color:var(--hp-ink)}.room-empty p{margin:7px 0 16px}.rule-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:10px;margin-top:18px}.rule-grid article{padding:18px;border-radius:12px;background:var(--hp-surface-2)}.rule-grid p{margin-top:7px}.room-mask{position:fixed;inset:0;z-index:2000;display:grid;place-items:center;padding:20px;background:rgba(27,35,41,.36)}.room-dialog{display:grid;gap:15px;width:min(560px,100%);padding:24px;border:1px solid var(--hp-line);border-radius:var(--hp-r-lg);background:var(--hp-surface);box-shadow:var(--hp-shadow-lg)}.room-dialog header{display:flex;align-items:flex-start;justify-content:space-between;gap:16px}.room-dialog h2{margin:0 0 6px}.room-dialog header p{margin:0;color:var(--hp-muted);font-size:12px}.room-dialog header button{width:34px;height:34px;border:0;border-radius:50%;background:var(--hp-surface-2);cursor:pointer}.room-dialog label{display:grid;gap:7px;color:var(--hp-ink-2);font-size:12px;font-weight:650}.room-dialog input,.room-dialog select{width:100%;box-sizing:border-box;padding:10px 12px;border:1px solid var(--hp-line-strong);border-radius:10px;color:var(--hp-ink);background:var(--hp-surface-2);font:inherit}.dialog-row{display:grid;grid-template-columns:1fr 1fr;gap:10px}.room-dialog footer{justify-content:flex-end}@media(max-width:1000px){.room-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(max-width:680px){.invite-entry{display:grid}.invite-entry form{min-width:0;flex-direction:column}.room-shell{width:min(100% - 24px,1280px);padding-top:82px}.room-hero{display:grid}.room-grid,.rule-grid,.dialog-row{grid-template-columns:1fr}.room-actions,.room-card footer,.room-dialog footer{flex-wrap:wrap}}
</style>