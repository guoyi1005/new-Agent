<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import GrowthTreeDiagram from '../components/GrowthTreeDiagram.vue'

import { getCareerExploration, getCareerNebulaMap } from '../api/careerNebula'
import { getCampusCourses } from '../api/campusCourse'
import { getContentTags, getPracticeSummary, getPythonHome } from '../api/learning'
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
const practiceSkills = ref([])
const practiceLoaded = ref(false)
const courseTags = ref([])
const campusCourses = ref([])
const mapError = ref('')
const masteryError = ref('')
const practiceError = ref('')
const courseError = ref('')
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
const practiceEvidence = computed(() => new Map(practiceSkills.value
  .filter((item) => item.name)
  .map((item) => [skillKey(item.name), item])))
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
  }
  const record = learningEvidence.value.get(skillKey(name))
  if (record) {
    if (record.status === 'mastered') return { kind: 'lit', detail: '学习记录显示已掌握' }
    if (record.status === 'learning' || record.status === 'weak') return { kind: 'learning', detail: '学习记录显示仍在巩固' }
  }
  const practice = practiceEvidence.value.get(skillKey(name))
  if (practice) {
    const level = Math.max(0, Math.min(100, Number(practice.level) || 0))
    if (level >= 100) return { kind: 'lit', detail: '学习实践技能进度已达 100%' }
    if (level > 0) return { kind: 'learning', detail: `学习实践技能进度 ${level}%` }
  }
  if (record) return { kind: 'unlit', detail: '学习记录中尚未掌握' }
  if (planet) return { kind: 'unlit', detail: planet.configured ? '岗位课程尚未完成' : '尚未关联可学习课程' }
  if (linkedPlanetId && (careerLoading.value || careerError.value)) return { kind: 'unknown', detail: careerError.value || '正在核对岗位课程记录' }
  if (!masteryLoaded.value || !practiceLoaded.value) return { kind: 'unknown', detail: masteryError.value || practiceError.value || '正在核对学习记录' }
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
const selectedBranch = computed(() => treeData.value.branches.find((branch) => `branch:${branch.id}` === selectedPointKey.value))
const diagramData = computed(() => {
  const nodes = []
  const edges = []
  let trunk = ''
  treeData.value.shared.forEach((skill) => {
    nodes.push({ id: skill.key, label: skill.name, status: skill.kind === 'lit' ? 'mastered' : skill.kind, stateLabel: stateLabel(skill.kind) })
    if (trunk) edges.push({ source: trunk, target: skill.key })
    trunk = skill.key
  })
  treeData.value.branches.forEach((branch) => {
    const branchId = `branch:${branch.id}`
    nodes.push({ id: branchId, label: branch.title, type: 'branch' })
    if (trunk) edges.push({ source: trunk, target: branchId })
    let parent = branchId
    branch.skills.forEach((skill) => {
      nodes.push({ id: skill.key, label: skill.name, status: skill.kind === 'lit' ? 'mastered' : skill.kind, stateLabel: stateLabel(skill.kind) })
      edges.push({ source: parent, target: skill.key })
      parent = skill.key
    })
  })
  return { nodes, edges }
})
const selectedCourse = computed(() => {
  if (!selectedPoint.value) return null
  const tag = courseTags.value.find((item) => item.skills?.some((skill) => skillKey(skill.name) === skillKey(selectedPoint.value.name)))
  return campusCourses.value.find((course) => course.id === tag?.sourceId) || null
})

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
    getPracticeSummary(true).then((result) => {
      practiceSkills.value = Array.isArray(result?.skills) ? result.skills : []
      practiceLoaded.value = true
    }).catch((error) => { practiceError.value = error.message || '学习实践记录暂不可用' }),
    getContentTags('COURSE').then((result) => { courseTags.value = Array.isArray(result) ? result : [] })
      .catch(() => { courseError.value = '课程关联暂不可用' }),
    getCampusCourses().then((result) => { campusCourses.value = Array.isArray(result?.data) ? result.data : [] })
      .catch(() => { courseError.value = '课程关联暂不可用' }),
  ])
})
</script>

<template>
  <div class="growth-tree-page">
    <main class="growth-tree">
      <header class="growth-tree__hero">
        <div><p class="growth-tree__eyebrow">YOUR GROWTH PATH</p><h2>成长树</h2><p>从岗位要求出发，结合学习实践记录，看到已点亮的技能和下一步分支。</p></div>
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
        <p v-if="unknownCount || mapError || masteryError || practiceError || courseError || careerError" class="growth-tree__notice" role="status">{{ unknownCount ? `${unknownCount} 个技能点的状态尚未核实。` : '' }}{{ mapError || masteryError || practiceError || courseError || careerError }}</p>
        <GrowthTreeDiagram v-if="treeData.branches.length" :root-label="selectedTitle" :nodes="diagramData.nodes" :edges="diagramData.edges" :selected-id="selectedPointKey" @select="selectedPointKey = $event" @close="selectedPointKey = ''">
          <template #detail>
            <template v-if="selectedPoint">
              <span class="node-detail__eyebrow">技能点详情 · {{ stateLabel(selectedPoint.kind) }}</span>
              <h3>{{ selectedPoint.name }}</h3>
              <p>{{ selectedPoint.note || '岗位探索中未提供该技能的详细说明。' }}</p>
              <small>{{ selectedPoint.detail }}</small>
            </template>
            <template v-else-if="selectedBranch">
              <span class="node-detail__eyebrow">职业分支</span>
              <h3>{{ selectedBranch.title }}</h3>
              <p>{{ selectedBranch.direction || '沿该分支逐步完成岗位技能要求。' }}</p>
            </template>
            <RouterLink v-if="selectedCourse" :to="{ path: '/growth/courses', query: { course: selectedCourse.id } }">学习关联课程：{{ selectedCourse.name }} →</RouterLink>
            <RouterLink v-else to="/learning">前往学习实践 →</RouterLink>
          </template>
        </GrowthTreeDiagram>
        <p v-else class="growth-tree__empty">当前岗位尚无可展示的技能要求。</p>
      </section>
    </main>
  </div>
</template>

<style scoped>
.growth-tree-page { color: var(--hp-ink); }
.growth-tree { width: min(1360px, calc(100% - 48px)); margin: 0 auto; padding: 0 0 72px; }
.growth-tree__hero { display: flex; justify-content: space-between; align-items: end; gap: 24px; padding: 42px 0 34px; }
.growth-tree__hero h2 { margin: 7px 0 10px; font-size: clamp(36px, 4vw, 54px); letter-spacing: -.05em; }
.growth-tree__hero p { margin: 0; color: var(--hp-ink-2); line-height: 1.7; }
.growth-tree__eyebrow { color: var(--hp-green-ink) !important; font-size: 11px; font-weight: 800; letter-spacing: .16em; }
.growth-tree__legend { display: flex; flex-wrap: wrap; gap: 10px 17px; padding-bottom: 7px; color: var(--hp-ink-2); font-size: 12px; }
.growth-tree__legend span { display: inline-flex; align-items: center; gap: 7px; white-space: nowrap; }
.growth-tree__legend i { width: 9px; height: 9px; border-radius: 50%; }
.growth-tree__legend .is-lit { background: #80aa71; }.growth-tree__legend .is-learning { background: #d5ad60; }.growth-tree__legend .is-unlit { border: 1px solid #aeb6ab; background: #f8f7f2; }.growth-tree__legend .is-unknown { background: #b7bfcb; }
.growth-tree__selector, .growth-tree__board { border: 1px solid var(--hp-line); border-radius: 24px; background: var(--hp-surface); box-shadow: var(--hp-shadow-sm); }
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
.growth-tree__board :deep(.tree-scroll) { margin-top: 24px; }
.growth-tree__empty { margin: 24px 0 0; color: var(--hp-muted); }
@media (max-width: 900px) { .growth-tree__hero { align-items: flex-start; flex-direction: column; } }
@media (max-width: 680px) { .growth-tree { width: calc(100% - 30px); padding-top: 20px; }.growth-tree__hero { padding: 28px 0; }.growth-tree__selector, .growth-tree__board { padding: 20px; }.growth-tree__selector-head, .growth-tree__board-head { align-items: flex-start; flex-direction: column; }.growth-tree__count { align-items: flex-start; } }
@media (prefers-reduced-motion: reduce) { .growth-tree__roles button { transition: none; } }
</style>
