<script setup>
import { computed, onMounted, ref, watch } from 'vue'

import { getCareerExploration, getCareerNebulaMap } from '../api/careerNebula'
import { getPythonHome } from '../api/learning'
import AppTabBar from '../components/AppTabBar.vue'
import { JOB_DETAILS, JOB_PROFILES, TARGET_JOB_STORAGE_KEY, getJobDetailId } from '../data_tmp/jobCatalog'

const storedTarget = (() => {
  try { return localStorage.getItem(TARGET_JOB_STORAGE_KEY) || '' } catch { return '' }
})()
const selectedTitle = ref(JOB_PROFILES.some((profile) => profile.title === storedTarget) ? storedTarget : JOB_PROFILES[0]?.title || '')
const selectedPointKey = ref('')
const mapCareers = ref([])
const mapSkills = ref([])
const mastery = ref([])
const masteryLoaded = ref(false)
const mapError = ref('')
const masteryError = ref('')
const careerError = ref('')
const careerLoading = ref(false)
const planets = ref([])
let careerRequest = 0

const selectedProfile = computed(() => JOB_PROFILES.find((profile) => profile.title === selectedTitle.value))
const selectedMapCareer = computed(() => {
  const name = roleKey(selectedTitle.value)
  return mapCareers.value.find((career) => career.status === 'enabled' && roleKey(career.name) === name)
})

function roleKey(value) {
  return String(value || '').toLowerCase().replace(/\s|后端|开发|工程师|应用/g, '')
}

function skillKey(value) {
  return String(value || '').toLowerCase().replace(/[\s·、/／_-]/g, '')
}

const learningEvidence = computed(() => new Map(mastery.value
  .filter((item) => item.knowledgePointName)
  .map((item) => [skillKey(item.knowledgePointName), item])))
const planetEvidence = computed(() => new Map(planets.value.map((planet) => [String(planet.id), planet])))

function skillState(name, planetId = '') {
  const linkedSkill = selectedMapCareer.value && mapSkills.value.find((skill) => skill.status === 'enabled'
    && (skill.careerId || 'testing') === selectedMapCareer.value.id
    && skillKey(skill.name) === skillKey(name))
  const linkedPlanetId = planetId || linkedSkill?.id
  const planet = linkedPlanetId ? planetEvidence.value.get(String(linkedPlanetId)) : null
  if (planet) {
    const progress = Number(planet.explorationProgress || 0)
    if (planet.progressStatus === 'completed' || progress >= 100) return { kind: 'lit', detail: '岗位课程已完成' }
    if (progress > 0) return { kind: 'learning', detail: `岗位课程已完成 ${progress}%` }
    return { kind: 'unlit', detail: planet.configured ? '岗位课程尚未完成' : '尚未关联可学习课程' }
  }
  const record = learningEvidence.value.get(skillKey(name))
  if (record) {
    if (record.status === 'mastered') return { kind: 'lit', detail: '学习记录显示已掌握' }
    if (record.status === 'learning' || record.status === 'weak') return { kind: 'learning', detail: '学习记录显示仍在巩固' }
    return { kind: 'unlit', detail: '学习记录中尚未掌握' }
  }
  if (linkedPlanetId && (careerLoading.value || careerError.value)) return { kind: 'unknown', detail: careerError.value || '正在核对岗位课程记录' }
  if (!masteryLoaded.value) return { kind: 'unknown', detail: masteryError.value || '正在核对学习记录' }
  return { kind: 'unlit', detail: '暂无完成记录' }
}

function stateLabel(kind) {
  return { lit: '已点亮', learning: '成长中', unknown: '未核实', unlit: '未点亮' }[kind] || '未点亮'
}

const treeData = computed(() => {
  if (!selectedProfile.value) return { shared: [], branches: [] }
  const detailIds = [...new Set([
    getJobDetailId(selectedProfile.value.title),
    ...(selectedProfile.value.fit || []).map((item) => item.id),
  ].filter((id) => JOB_DETAILS[id]))].slice(0, 3)
  const details = detailIds.map((id) => ({ id, ...JOB_DETAILS[id] }))
  const occurrences = new Map()
  details.forEach((detail) => (detail.requirements || []).forEach((skill) => {
    const key = skillKey(skill.name)
    occurrences.set(key, (occurrences.get(key) || 0) + 1)
  }))
  const sharedKeys = new Set([...occurrences].filter(([, count]) => count > 1).map(([key]) => key))
  const seenShared = new Set()
  const shared = details.flatMap((detail) => (detail.requirements || []).filter((skill) => {
    const key = skillKey(skill.name)
    if (!sharedKeys.has(key) || seenShared.has(key)) return false
    seenShared.add(key)
    return true
  }).map((skill) => point(skill, 'shared')))
  const branches = details.map((detail, index) => {
    const skillRows = (detail.requirements || [])
      .filter((skill) => !sharedKeys.has(skillKey(skill.name)))
      .map((skill) => point(skill, detail.id))
    if (index === 0 && selectedMapCareer.value) {
      const existing = new Set([...shared, ...skillRows].map((skill) => skillKey(skill.name)))
      mapSkills.value.filter((skill) => skill.status === 'enabled'
        && (skill.careerId || 'testing') === selectedMapCareer.value.id).forEach((skill) => {
        if (existing.has(skillKey(skill.name))) return
        existing.add(skillKey(skill.name))
        skillRows.push(point(skill, detail.id, true))
      })
    }
    return { id: detail.id, title: detail.title, direction: detail.direction, skills: skillRows }
  })
  return { shared, branches }
})

function point(skill, branchId, isPlanet = false) {
  return {
    key: `${branchId}:${skillKey(skill.name)}`,
    name: skill.name,
    note: skill.note || skill.description || '',
    branchId,
    ...skillState(skill.name, isPlanet ? skill.id : ''),
  }
}

const allPoints = computed(() => [...treeData.value.shared, ...treeData.value.branches.flatMap((branch) => branch.skills)])
const litCount = computed(() => allPoints.value.filter((skill) => skill.kind === 'lit').length)
const unknownCount = computed(() => allPoints.value.filter((skill) => skill.kind === 'unknown').length)
const selectedPoint = computed(() => allPoints.value.find((skill) => skill.key === selectedPointKey.value))

watch(selectedTitle, () => { selectedPointKey.value = '' })
watch(selectedMapCareer, async (career) => {
  const requestId = ++careerRequest
  planets.value = []
  careerError.value = ''
  careerLoading.value = Boolean(career)
  if (!career) return
  try {
    const result = await getCareerExploration(career.id)
    if (requestId === careerRequest) planets.value = Array.isArray(result.planets) ? result.planets : []
  } catch (error) {
    if (requestId === careerRequest) careerError.value = error.message || '岗位课程记录暂不可用'
  } finally {
    if (requestId === careerRequest) careerLoading.value = false
  }
}, { immediate: true })

onMounted(async () => {
  await Promise.all([
    getCareerNebulaMap().then((result) => {
      mapCareers.value = Array.isArray(result.careers) ? result.careers : []
      mapSkills.value = Array.isArray(result.skills) ? result.skills : []
    }).catch((error) => { mapError.value = error.message || '岗位课程结构暂不可用' }),
    getPythonHome().then((result) => {
      mastery.value = Array.isArray(result?.mastery) ? result.mastery : []
      masteryLoaded.value = true
    }).catch((error) => { masteryError.value = error.message || '学习记录暂不可用' }),
  ])
})
</script>

<template>
  <div class="growth-tree-page">
    <AppTabBar />
    <main class="growth-tree">
      <nav class="growth-tree__breadcrumb" aria-label="当前位置"><RouterLink to="/growth">成长中心</RouterLink><span>/</span><span>成长树</span></nav>
      <header class="growth-tree__hero">
        <div><p class="growth-tree__eyebrow">YOUR GROWTH PATH</p><h1>成长树</h1><p>从岗位要求出发，看到已点亮的技能，也看清下一步要走的分支。</p></div>
        <div class="growth-tree__legend" aria-label="技能点状态说明"><span><i class="is-lit" />已点亮</span><span><i class="is-learning" />成长中</span><span><i class="is-unlit" />未点亮</span><span><i class="is-unknown" />未核实</span></div>
      </header>

      <section class="growth-tree__selector" aria-label="选择岗位方向">
        <div class="growth-tree__selector-head"><h2>选择岗位方向</h2><p>切换只预览不同成长分支，不会修改你的目标岗位。</p></div>
        <div class="growth-tree__roles" role="tablist" aria-label="岗位方向">
          <button v-for="profile in JOB_PROFILES" :key="profile.title" type="button" role="tab" :aria-selected="selectedTitle === profile.title" :class="{ 'is-active': selectedTitle === profile.title }" @click="selectedTitle = profile.title">{{ profile.title }}</button>
        </div>
      </section>

      <section class="growth-tree__board" aria-label="岗位成长树">
        <div class="growth-tree__board-head"><div><p>当前预览方向</p><h2>{{ selectedTitle }}</h2></div><div class="growth-tree__count"><strong>{{ litCount }} <small>/ {{ allPoints.length }}</small></strong><span>已确认点亮的技能点</span></div></div>
        <p v-if="unknownCount || mapError || masteryError || careerError" class="growth-tree__notice" role="status">{{ unknownCount ? `${unknownCount} 个技能点的状态尚未核实。` : '' }}{{ mapError || masteryError || careerError }}</p>
        <div v-if="treeData.branches.length" class="growth-tree__diagram">
          <div class="growth-tree__branches">
            <section v-for="(branch, index) in treeData.branches" :key="branch.id" class="growth-tree__branch">
              <div class="growth-tree__branch-head"><span>{{ String(index + 1).padStart(2, '0') }} / 职业分支</span><h3>{{ branch.title }}</h3><p>{{ branch.direction }}</p></div>
              <ul><li v-for="skill in branch.skills" :key="skill.key"><button type="button" class="growth-tree__node" :class="[`is-${skill.kind}`, { 'is-selected': selectedPointKey === skill.key }]" :aria-label="`查看${skill.name}技能点详情，${stateLabel(skill.kind)}`" @click="selectedPointKey = skill.key"><span class="growth-tree__node-icon" aria-hidden="true">{{ skill.kind === 'lit' ? '✓' : skill.kind === 'learning' ? '◐' : '○' }}</span><span><strong>{{ skill.name }}</strong><small>{{ stateLabel(skill.kind) }}</small></span></button></li></ul>
              <p v-if="!branch.skills.length" class="growth-tree__branch-empty">当前要求已归入共同基础。</p>
            </section>
          </div>
          <div class="growth-tree__junction" aria-hidden="true" />
          <div v-if="treeData.shared.length" class="growth-tree__shared"><span>共同基础</span><div><button v-for="skill in treeData.shared" :key="skill.key" type="button" class="growth-tree__shared-node" :class="[`is-${skill.kind}`, { 'is-selected': selectedPointKey === skill.key }]" :aria-label="`查看${skill.name}技能点详情，${stateLabel(skill.kind)}`" @click="selectedPointKey = skill.key"><i aria-hidden="true" />{{ skill.name }}</button></div></div>
          <div class="growth-tree__root"><span>当前方向</span><strong>{{ selectedTitle }}</strong></div>
        </div>
        <p v-else class="growth-tree__empty">当前岗位尚无可展示的技能要求。</p>
      </section>

      <section class="growth-tree__detail" aria-live="polite"><div v-if="selectedPoint"><p>技能点详情 · {{ stateLabel(selectedPoint.kind) }}</p><h2>{{ selectedPoint.name }}</h2><span>{{ selectedPoint.note || '岗位探索中未提供该技能的详细说明。' }}</span><small>{{ selectedPoint.detail }}</small></div><div v-else><p>技能点详情</p><h2>选择树上的技能点</h2><span>点击任意节点，查看岗位要求与点亮依据。</span></div><RouterLink to="/learning">前往学习实践 →</RouterLink></section>
    </main>
  </div>
</template>

<style scoped>
.growth-tree-page { min-height: 100vh; color: var(--hp-ink); background: var(--hp-bg); }
.growth-tree { width: min(1360px, calc(100% - 48px)); margin: 0 auto; padding: 30px 0 72px; }
.growth-tree__breadcrumb { display: flex; gap: 9px; align-items: center; color: var(--hp-muted); font-size: 13px; }
.growth-tree__breadcrumb a { color: var(--hp-ink-2); text-decoration: none; }
.growth-tree__breadcrumb a:hover { color: var(--hp-green-ink); }
.growth-tree__hero { display: flex; justify-content: space-between; align-items: end; gap: 24px; padding: 42px 0 34px; }
.growth-tree__hero h1 { margin: 7px 0 10px; font-size: clamp(36px, 4vw, 54px); letter-spacing: -.05em; }
.growth-tree__hero p { margin: 0; color: var(--hp-ink-2); line-height: 1.7; }
.growth-tree__eyebrow { color: var(--hp-green-ink) !important; font-size: 11px; font-weight: 800; letter-spacing: .16em; }
.growth-tree__legend { display: flex; flex-wrap: wrap; gap: 10px 17px; padding-bottom: 7px; color: var(--hp-ink-2); font-size: 12px; }
.growth-tree__legend span { display: inline-flex; align-items: center; gap: 7px; white-space: nowrap; }
.growth-tree__legend i { width: 9px; height: 9px; border-radius: 50%; }
.growth-tree__legend .is-lit { background: #80aa71; }.growth-tree__legend .is-learning { background: #d5ad60; }.growth-tree__legend .is-unlit { border: 1px solid #aeb6ab; background: #f8f7f2; }.growth-tree__legend .is-unknown { background: #b7bfcb; }
.growth-tree__selector, .growth-tree__board, .growth-tree__detail { border: 1px solid var(--hp-line); border-radius: 24px; background: var(--hp-surface); box-shadow: var(--hp-shadow-sm); }
.growth-tree__selector { padding: 22px 26px 24px; }
.growth-tree__selector-head { display: flex; align-items: baseline; justify-content: space-between; gap: 14px; margin-bottom: 18px; }
.growth-tree__selector-head h2 { margin: 0; font-size: 17px; }.growth-tree__selector-head p { margin: 0; color: var(--hp-muted); font-size: 12px; }
.growth-tree__roles { display: flex; flex-wrap: wrap; gap: 9px; }
.growth-tree__roles button { padding: 9px 15px; border: 1px solid var(--hp-line-strong); border-radius: 999px; color: var(--hp-ink-2); background: var(--hp-surface-2); font: inherit; font-size: 13px; cursor: pointer; transition: background .18s ease, border-color .18s ease; }
.growth-tree__roles button:hover { border-color: #a1bd93; }.growth-tree__roles button.is-active { border-color: #9db98f; color: #41673e; background: var(--hp-green); font-weight: 700; }
.growth-tree__board { margin-top: 20px; padding: 30px; background: #fbfaf6; }
.growth-tree__board-head { display: flex; justify-content: space-between; align-items: center; gap: 24px; padding-bottom: 23px; border-bottom: 1px solid #e7e5db; }
.growth-tree__board-head p { margin: 0 0 6px; color: var(--hp-muted); font-size: 12px; }.growth-tree__board-head h2 { margin: 0; font-size: clamp(22px, 2.4vw, 30px); }
.growth-tree__count { display: flex; flex-direction: column; align-items: flex-end; gap: 2px; }.growth-tree__count strong { color: #59814f; font-size: 32px; line-height: 1; }.growth-tree__count small { color: var(--hp-muted); font-size: 15px; }.growth-tree__count span { color: var(--hp-muted); font-size: 11px; }
.growth-tree__notice { margin: 18px 0 0; padding: 11px 14px; border: 1px solid #eee1c8; border-radius: 10px; color: #8a6d40; background: #fff7e8; font-size: 12px; }
.growth-tree__diagram { padding: 30px 0 4px; }.growth-tree__branches { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); align-items: stretch; gap: 18px; }
.growth-tree__branch { position: relative; display: flex; flex-direction: column; min-width: 0; padding: 20px; border: 1px solid #dfe8d8; border-radius: 20px; background: #f3f8f0; }
.growth-tree__branch:nth-child(2) { border-color: #e8e2d2; background: #faf6eb; }.growth-tree__branch:nth-child(3) { border-color: #dfdee7; background: #f3f3f8; }
.growth-tree__branch::after { content: ''; position: absolute; bottom: -31px; left: 50%; height: 31px; border-left: 2px solid #adc5a4; }.growth-tree__branch:nth-child(2)::after { border-color: #d5c293; }.growth-tree__branch:nth-child(3)::after { border-color: #c3bed3; }
.growth-tree__branch-head { min-height: 104px; padding-bottom: 15px; border-bottom: 1px solid rgba(64,78,61,.12); }.growth-tree__branch-head span { color: var(--hp-green-ink); font-size: 10px; font-weight: 800; letter-spacing: .12em; }.growth-tree__branch-head h3 { margin: 8px 0 5px; font-size: 20px; line-height: 1.35; }.growth-tree__branch-head p { margin: 0; color: var(--hp-muted); font-size: 12px; }
.growth-tree__branch ul { display: grid; align-content: start; gap: 10px; margin: 17px 0 0; padding: 0; list-style: none; }.growth-tree__branch-empty { margin: 18px 0 0; color: var(--hp-muted); font-size: 12px; }
.growth-tree__node { display: flex; align-items: center; gap: 12px; width: 100%; min-height: 59px; padding: 10px 12px; border: 1px solid #dce3d8; border-radius: 13px; color: var(--hp-ink); background: rgba(255,255,255,.7); text-align: left; font: inherit; cursor: pointer; transition: transform .18s ease, border-color .18s ease; }
.growth-tree__node:hover { transform: translateY(-2px); }.growth-tree__node.is-selected { border-color: #6f9463; outline: 2px solid rgba(141,187,118,.2); }.growth-tree__node > span:last-child { display: flex; flex-direction: column; gap: 3px; min-width: 0; }.growth-tree__node strong { overflow-wrap: anywhere; font-size: 14px; }.growth-tree__node small { color: var(--hp-muted); font-size: 11px; }
.growth-tree__node-icon { display: grid; place-items: center; flex: 0 0 33px; width: 33px; height: 33px; border-radius: 50%; color: #8a9588; background: #edf0ea; font-size: 17px; font-weight: 700; }
.growth-tree__node.is-lit .growth-tree__node-icon { color: #fff; background: #81a973; }.growth-tree__node.is-learning .growth-tree__node-icon { color: #926b2f; background: #f5e7bc; }.growth-tree__node.is-unknown .growth-tree__node-icon { color: #778493; background: #e9edf1; }
.growth-tree__junction { position: relative; width: 66.7%; height: 42px; margin: 29px auto 0; border-top: 2px solid #c1cbb7; }.growth-tree__junction::after { content: ''; position: absolute; top: 0; left: 50%; height: 42px; border-left: 2px solid #c1cbb7; }
.growth-tree__shared { position: relative; display: flex; align-items: center; flex-direction: column; gap: 12px; width: fit-content; max-width: 100%; margin: 0 auto 44px; padding: 18px 23px; border: 1px solid #d5e3cd; border-radius: 16px; background: #ebf3e6; }.growth-tree__shared::after { content: ''; position: absolute; bottom: -45px; left: 50%; height: 45px; border-left: 2px solid #c1cbb7; }.growth-tree__shared > span { color: #5b8152; font-size: 11px; font-weight: 800; letter-spacing: .08em; }.growth-tree__shared > div { display: flex; flex-wrap: wrap; justify-content: center; gap: 8px; }
.growth-tree__shared-node { display: inline-flex; align-items: center; gap: 8px; padding: 8px 12px; border: 1px solid #ccdccc; border-radius: 999px; color: var(--hp-ink); background: #fff; font: inherit; font-size: 13px; cursor: pointer; }.growth-tree__shared-node i { width: 8px; height: 8px; border-radius: 50%; background: #b3bdb2; }.growth-tree__shared-node.is-lit i { background: #80aa71; }.growth-tree__shared-node.is-learning i { background: #d5ad60; }.growth-tree__shared-node.is-selected { outline: 2px solid #8dbb76; }
.growth-tree__root { display: flex; flex-direction: column; align-items: center; gap: 5px; width: fit-content; max-width: 100%; margin: 0 auto; padding: 17px 36px; border: 1px solid #97b98c; border-radius: 17px; color: #315a33; background: #dae9d4; text-align: center; }.growth-tree__root span { font-size: 11px; }.growth-tree__root strong { overflow-wrap: anywhere; font-size: 17px; }
.growth-tree__empty { margin: 24px 0 0; color: var(--hp-muted); }
.growth-tree__detail { display: flex; justify-content: space-between; align-items: center; gap: 24px; margin-top: 20px; padding: 22px 27px; }.growth-tree__detail p { margin: 0 0 5px; color: var(--hp-green-ink); font-size: 11px; font-weight: 800; }.growth-tree__detail h2 { margin: 0 0 6px; font-size: 19px; }.growth-tree__detail span, .growth-tree__detail small { display: block; color: var(--hp-ink-2); font-size: 13px; line-height: 1.7; }.growth-tree__detail small { color: var(--hp-muted); }.growth-tree__detail a { flex: 0 0 auto; color: #567d4e; font-size: 13px; font-weight: 700; text-decoration: none; }
@media (max-width: 900px) { .growth-tree__hero { align-items: flex-start; flex-direction: column; }.growth-tree__branch { padding: 15px; }.growth-tree__branches { gap: 10px; } }
@media (max-width: 680px) { .growth-tree { width: calc(100% - 30px); padding-top: 20px; }.growth-tree__hero { padding: 28px 0; }.growth-tree__selector, .growth-tree__board { padding: 20px; }.growth-tree__selector-head, .growth-tree__board-head, .growth-tree__detail { align-items: flex-start; flex-direction: column; }.growth-tree__count { align-items: flex-start; }.growth-tree__branches { grid-template-columns: 1fr; gap: 22px; }.growth-tree__branch::after { bottom: -23px; height: 23px; }.growth-tree__junction { width: 0; height: 30px; margin: 22px auto 0; border-top: 0; border-left: 2px solid #c1cbb7; }.growth-tree__junction::after { display: none; }.growth-tree__branch-head { min-height: 0; }.growth-tree__shared { width: 100%; }.growth-tree__root { width: 100%; }.growth-tree__detail { padding: 20px; } }
@media (prefers-reduced-motion: reduce) { .growth-tree__node, .growth-tree__roles button { transition: none; } }
</style>
