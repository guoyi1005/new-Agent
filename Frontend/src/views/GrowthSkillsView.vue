<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import GrowthTreeDiagram from '../components/GrowthTreeDiagram.vue'

import { getCareerExploration, getCareerNebulaMap } from '../api/careerNebula'
import { getCampusCourses } from '../api/campusCourse'
import { getContentTags, getPracticeSummary, getPythonKnowledgeGraph } from '../api/learning'

const pythonGraph = ref({ nodes: [], edges: [] })
const careerMap = ref({ careers: [], skills: [], edges: [] })
const careerPlanets = ref([])
const courseTags = ref([])
const campusCourses = ref([])
const practiceSkills = ref([])
const practiceLoaded = ref(false)
const selectedTrack = ref('python')
const selectedId = ref('')
const keyword = ref('')
const stateFilter = ref('all')
const loading = ref(true)
const sourceErrors = ref([])
const careerLoading = ref(false)
const careerError = ref('')
let careerRequest = 0

const tracks = computed(() => [
  ...(pythonGraph.value.nodes.length ? [{ id: 'python', name: 'Python 知识技能' }] : []),
  ...(courseNodes.value.length ? [{ id: 'learning-courses', name: '课程关联技能' }] : []),
  ...careerMap.value.careers
    .filter((career) => career.status === 'enabled' && careerMap.value.skills.some((skill) =>
      skill.status === 'enabled' && (skill.careerId || 'testing') === career.id))
    .map((career) => ({ id: career.id, name: career.name })),
])
const activeTrack = computed(() => tracks.value.find((track) => track.id === selectedTrack.value))
const isPython = computed(() => selectedTrack.value === 'python')

function skillKey(value) {
  return String(value || '').toLowerCase().replace(/[\s·、/／_-]/g, '')
}

function careerLevels(nodes, edges) {
  const ids = new Set(nodes.map((node) => node.id))
  const incoming = new Map(nodes.map((node) => [node.id, 0]))
  const outgoing = new Map(nodes.map((node) => [node.id, []]))
  edges.forEach((edge) => {
    if (!ids.has(edge.source) || !ids.has(edge.target)) return
    incoming.set(edge.target, incoming.get(edge.target) + 1)
    outgoing.get(edge.source).push(edge.target)
  })
  const levels = new Map(nodes.map((node) => [node.id, 0]))
  const queue = nodes.filter((node) => incoming.get(node.id) === 0).map((node) => node.id)
  while (queue.length) {
    const source = queue.shift()
    outgoing.get(source).forEach((target) => {
      levels.set(target, Math.max(levels.get(target), levels.get(source) + 1))
      incoming.set(target, incoming.get(target) - 1)
      if (incoming.get(target) === 0) queue.push(target)
    })
  }
  return levels
}

const courseNodes = computed(() => {
  const availableCourseIds = new Set(campusCourses.value.map((course) => course.id))
  const skillMap = new Map()
  const edges = []
  courseTags.value.filter((tag) => availableCourseIds.has(tag.sourceId)).forEach((tag) => {
    for (const skill of tag.skills || []) {
      const id = skillKey(skill.code || skill.name)
      if (!id) continue
      if (!skillMap.has(id)) skillMap.set(id, { id, title: skill.name, courseIds: new Set(), prerequisites: new Set() })
      const node = skillMap.get(id)
      node.courseIds.add(tag.sourceId)
      for (const prior of tag.prerequisites || []) {
        const source = skillKey(prior.code || prior.name)
        if (source && source !== id) {
          node.prerequisites.add(source)
          edges.push({ source, target: id })
        }
      }
    }
  })
  const rows = [...skillMap.values()]
  const levels = careerLevels(rows, edges)
  const evidence = new Map(practiceSkills.value.map((skill) => [skillKey(skill.code || skill.name), skill]))
  return rows.map((row) => {
    const record = evidence.get(row.id) || practiceSkills.value.find((skill) => skillKey(skill.name) === skillKey(row.title))
    const progress = record ? Math.max(0, Math.min(100, Number(record.level) || 0)) : 0
    return {
      ...row,
      level: levels.get(row.id) || 0,
      status: !practiceLoaded.value ? 'unknown' : progress >= 100 ? 'mastered' : progress > 0 ? 'learning' : 'available',
      progress: practiceLoaded.value ? progress : null,
      courseIds: [...row.courseIds],
      prerequisites: [...row.prerequisites],
      description: record?.category || '',
    }
  })
})

const nodes = computed(() => {
  if (selectedTrack.value === 'learning-courses') return courseNodes.value
  if (isPython.value) return pythonGraph.value.nodes.map((node) => ({
    id: node.id,
    title: node.title === node.id && node.description ? node.description : node.title,
    description: node.description,
    level: Number(node.level) || 0,
    status: node.status,
    progress: node.score == null ? null : Math.round(Number(node.score)),
    attempts: node.attemptCount,
    prerequisites: node.prerequisiteIds || [],
    pathItemId: node.pathItemId,
  }))
  const current = careerMap.value.skills.filter((skill) => skill.status === 'enabled'
    && (skill.careerId || 'testing') === selectedTrack.value)
  const edges = careerMap.value.edges.filter((edge) => current.some((skill) => skill.id === edge.source)
    && current.some((skill) => skill.id === edge.target))
  const levels = careerLevels(current, edges)
  const evidence = new Map(careerPlanets.value.map((planet) => [planet.id, planet]))
  return current.map((skill) => {
    const planet = evidence.get(skill.id)
    const progress = Number(planet?.explorationProgress || 0)
    return {
      id: skill.id,
      title: skill.name,
      description: skill.description || planet?.description,
      level: levels.get(skill.id) || 0,
      status: careerLoading.value || careerError.value ? 'unknown'
        : progress >= 100 || planet?.progressStatus === 'completed' ? 'mastered'
          : progress > 0 ? 'learning' : 'available',
      progress: planet ? progress : null,
      attempts: null,
      prerequisites: edges.filter((edge) => edge.target === skill.id).map((edge) => edge.source),
      courseId: planet?.configured ? planet.courseId : null,
      configured: planet?.configured,
    }
  })
})
const visibleNodes = computed(() => nodes.value.filter((node) => {
  const query = keyword.value.trim().toLowerCase()
  return (!query || `${node.title} ${node.description || ''}`.toLowerCase().includes(query))
    && (stateFilter.value === 'all' || stateGroup(node.status) === stateFilter.value)
}))
const diagramNodes = computed(() => visibleNodes.value.map((node) => ({
  id: String(node.id), label: node.title, status: stateGroup(node.status), stateLabel: stateLabel(node.status),
})))
const diagramEdges = computed(() => visibleNodes.value.flatMap((node) =>
  (node.prerequisites || []).map((source) => ({ source: String(source), target: String(node.id) }))))
const selectedNode = computed(() => nodes.value.find((node) => String(node.id) === selectedId.value))
const prerequisites = computed(() => (selectedNode.value?.prerequisites || [])
  .map((id) => nodes.value.find((node) => node.id === id)?.title).filter(Boolean))
const masteredCount = computed(() => nodes.value.filter((node) => node.status === 'mastered').length)
const relatedCourses = computed(() => {
  if (!selectedNode.value) return []
  const ids = selectedNode.value.courseIds || courseTags.value
    .filter((tag) => tag.skills?.some((skill) => skillKey(skill.name) === skillKey(selectedNode.value.title)))
    .map((tag) => tag.sourceId)
  return campusCourses.value.filter((course) => ids.includes(course.id))
})

function stateGroup(status) {
  if (status === 'mastered') return 'mastered'
  if (status === 'learning' || status === 'weak') return 'learning'
  if (status === 'locked') return 'locked'
  if (status === 'unknown') return 'unknown'
  return 'available'
}

function stateLabel(status) {
  return { mastered: '已掌握', learning: '学习中', weak: '待巩固', available: '待学习', locked: '待解锁', unknown: '未核实' }[status] || '待学习'
}

async function loadCareer(careerId) {
  const request = ++careerRequest
  careerPlanets.value = []
  careerError.value = ''
  careerLoading.value = careerId !== 'python' && careerId !== 'learning-courses'
  if (!careerLoading.value) return
  try {
    const result = await getCareerExploration(careerId)
    if (request === careerRequest) careerPlanets.value = Array.isArray(result.planets) ? result.planets : []
  } catch (cause) {
    if (request === careerRequest) careerError.value = cause.message || '技能进度暂时无法核实'
  } finally {
    if (request === careerRequest) careerLoading.value = false
  }
}

watch(selectedTrack, (track) => {
  selectedId.value = ''
  stateFilter.value = 'all'
  loadCareer(track)
})

async function load() {
  loading.value = true
  sourceErrors.value = []
  const [python, career, courses, tags, practice] = await Promise.allSettled([
    getPythonKnowledgeGraph(), getCareerNebulaMap(), getCampusCourses(), getContentTags('COURSE'), getPracticeSummary(true),
  ])
  if (python.status === 'fulfilled') pythonGraph.value = {
    nodes: Array.isArray(python.value?.nodes) ? python.value.nodes : [],
    edges: Array.isArray(python.value?.edges) ? python.value.edges : [],
  }
  else sourceErrors.value.push('Python 技能记录加载失败')
  if (career.status === 'fulfilled') careerMap.value = career.value
  else sourceErrors.value.push('岗位技能结构加载失败')
  campusCourses.value = courses.status === 'fulfilled' && Array.isArray(courses.value?.data) ? courses.value.data : []
  courseTags.value = tags.status === 'fulfilled' && Array.isArray(tags.value) ? tags.value : []
  practiceSkills.value = practice.status === 'fulfilled' && Array.isArray(practice.value?.skills) ? practice.value.skills : []
  practiceLoaded.value = practice.status === 'fulfilled'
  if (courses.status === 'rejected' || tags.status === 'rejected') sourceErrors.value.push('课程技能关联加载失败')
  if (practice.status === 'rejected') sourceErrors.value.push('学习实践记录加载失败')
  if (!tracks.value.some((track) => track.id === selectedTrack.value)) selectedTrack.value = tracks.value[0]?.id || 'python'
  if (selectedTrack.value !== 'python' && selectedTrack.value !== 'learning-courses') loadCareer(selectedTrack.value)
  loading.value = false
}

onMounted(load)
</script>

<template>
  <div class="skills-page">
    <main class="skills-shell">
      <header class="skills-hero"><div><p>SKILL TREE</p><h2>技能树</h2><span>结合知识图谱与学习实践的课程技能，查看掌握状态和关联课程。</span></div><RouterLink to="/growth/tree">查看岗位成长树 →</RouterLink></header>

      <div v-if="sourceErrors.length" class="skills-notice" role="status">{{ sourceErrors.join('；') }}。<button type="button" @click="load">重试</button></div>
      <p v-if="loading" class="skills-empty">正在读取技能记录…</p>
      <template v-else-if="tracks.length">
        <section class="skills-toolbar" aria-label="技能筛选">
          <div class="skills-tracks" role="tablist" aria-label="技能方向"><button v-for="track in tracks" :key="track.id" type="button" role="tab" :class="{ active: selectedTrack === track.id }" :aria-selected="selectedTrack === track.id" @click="selectedTrack = track.id">{{ track.name }}</button></div>
          <div class="skills-filters"><input v-model="keyword" type="search" placeholder="搜索技能" aria-label="搜索技能"><select v-model="stateFilter" aria-label="按技能状态筛选"><option value="all">全部状态</option><option value="mastered">已掌握</option><option value="learning">学习中</option><option value="available">待学习</option><option value="locked">待解锁</option><option value="unknown">未核实</option></select></div>
        </section>

        <section class="skills-board" aria-label="技能关系图">
          <div class="skills-board-head"><div><p>当前方向</p><h2>{{ activeTrack?.name }}</h2></div><div class="skills-count"><strong>{{ masteredCount }} / {{ nodes.length }}</strong><span>已掌握技能</span></div></div>
          <p v-if="careerError" class="skills-notice" role="status">{{ careerError }}。当前节点状态暂未核实。</p>
          <p v-if="!visibleNodes.length" class="skills-empty">{{ nodes.length ? '没有符合筛选条件的技能。' : '当前方向暂无可展示的技能。' }}</p>
          <GrowthTreeDiagram v-else :root-label="activeTrack?.name || '技能起点'" :nodes="diagramNodes" :edges="diagramEdges" :selected-id="String(selectedId)" @select="selectedId = $event" @close="selectedId = ''">
            <template #detail>
              <template v-if="selectedNode">
                <span class="node-detail__eyebrow">技能详情 · {{ stateLabel(selectedNode.status) }}</span>
                <h3>{{ selectedNode.title }}</h3>
                <p>{{ selectedNode.description || '暂无技能说明。' }}</p>
                <small v-if="prerequisites.length">前置技能：{{ prerequisites.join(' · ') }}</small>
                <small v-if="selectedNode.attempts">练习次数：{{ selectedNode.attempts }}</small>
                <small v-if="selectedNode.progress != null && selectedNode.status !== 'unknown'">当前进度：{{ selectedNode.progress }}%</small>
                <div class="skills-detail-actions">
                  <RouterLink v-for="course in relatedCourses.slice(0, 3)" :key="course.id" :to="{ path: '/growth/courses', query: { course: course.id } }">{{ course.name }} →</RouterLink>
                  <RouterLink v-if="!relatedCourses.length && selectedNode.courseId" :to="`/courses/${selectedNode.courseId}`">查看关联课程 →</RouterLink>
                  <RouterLink v-if="!relatedCourses.length && !selectedNode.courseId && isPython" to="/learning/python/plan">前往学习路径 →</RouterLink>
                </div>
              </template>
            </template>
          </GrowthTreeDiagram>
        </section>
      </template>
      <p v-else class="skills-empty">暂无技能数据，请稍后重试。</p>
    </main>
  </div>
</template>

<style scoped>
.skills-page{color:var(--hp-ink)}
.skills-shell{width:min(1360px,calc(100% - 48px));margin:auto;padding:0 0 72px}
.skills-hero{display:flex;justify-content:space-between;align-items:end;gap:20px;padding:40px 0 30px}.skills-hero p,.skills-board-head p{margin:0;color:var(--hp-blue-ink);font-size:11px;font-weight:800;letter-spacing:.12em}.skills-hero h2{margin:8px 0;font-size:clamp(34px,4vw,52px)}.skills-hero span{color:var(--hp-ink-2)}.skills-hero a{color:var(--hp-blue-ink);font-weight:700;text-decoration:none;white-space:nowrap}
.skills-toolbar,.skills-board{border:1px solid var(--hp-line);border-radius:24px;background:var(--hp-surface);box-shadow:var(--hp-shadow-sm)}
.skills-toolbar{display:grid;gap:18px;padding:24px}.skills-tracks,.skills-filters{display:flex;flex-wrap:wrap;gap:9px}.skills-tracks button,.skills-filters input,.skills-filters select{border:1px solid var(--hp-line-strong);border-radius:999px;background:var(--hp-surface-2);color:var(--hp-ink-2);font:inherit;font-size:13px}.skills-tracks button{padding:9px 15px;cursor:pointer}.skills-tracks button.active{border-color:#a5bed1;background:var(--hp-blue);color:var(--hp-blue-ink);font-weight:700}.skills-filters input,.skills-filters select{padding:10px 14px}.skills-filters input{flex:1;min-width:180px}
.skills-board{margin-top:20px;padding:28px;background:#fbfaf7}.skills-board-head{display:flex;justify-content:space-between;align-items:center;gap:20px;padding-bottom:22px;border-bottom:1px solid var(--hp-line)}.skills-board-head h2{margin:5px 0 0;font-size:25px}.skills-count{display:grid;text-align:right}.skills-count strong{color:var(--hp-blue-ink);font-size:28px}.skills-count span{color:var(--hp-muted);font-size:12px}
.skills-empty,.skills-notice{margin:20px 0;padding:18px;border-radius:14px;background:var(--hp-surface);color:var(--hp-ink-2)}.skills-notice{border:1px solid #eadbbd;background:#fff8e9}.skills-notice button{margin-left:10px;border:0;background:none;color:var(--hp-blue-ink);font:inherit;font-weight:700;cursor:pointer}
.skills-detail-actions{display:grid;gap:3px}
@media(max-width:680px){.skills-shell{width:calc(100% - 30px)}.skills-hero,.skills-board-head{align-items:flex-start;flex-direction:column}.skills-board,.skills-toolbar{padding:20px}.skills-count{text-align:left}.skills-filters input,.skills-filters select{width:100%}}
</style>
