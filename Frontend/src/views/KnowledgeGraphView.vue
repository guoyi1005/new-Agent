<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getPythonKnowledgeGraph } from '../api/learning'

const router = useRouter()
const loading = ref(true)
const error = ref('')
const graph = ref({ nodes: [], edges: [], summary: {} })
const selected = ref(null)
const keyword = ref('')
const status = ref('all')
const filters = [
  ['all', '全部'], ['weak', '需巩固'], ['learning', '学习中'],
  ['mastered', '已掌握'], ['available', '可学习'], ['locked', '待解锁'],
]
const labels = { weak: '需巩固', learning: '学习中', mastered: '已掌握', available: '可学习', locked: '待解锁' }

const nodes = computed(() => graph.value.nodes.filter((node) => {
  const hitStatus = status.value === 'all' || node.status === status.value
  const query = keyword.value.trim().toLowerCase()
  return hitStatus && (!query || `${node.title} ${node.group}`.toLowerCase().includes(query))
}))
const nodeById = computed(() => new Map(graph.value.nodes.map((node) => [node.id, node])))
const prerequisites = computed(() => (selected.value?.prerequisiteIds || [])
  .map((id) => nodeById.value.get(id)).filter(Boolean))

/* 按阶段（level）分栏：不再用固定画布 + 绝对定位，宽度自适应 */
const columns = computed(() => {
  const grouped = new Map()
  for (const node of nodes.value) {
    const level = Number(node.level || 0)
    if (!grouped.has(level)) {
      grouped.set(level, { level, title: node.group || `阶段 ${level + 1}`, items: [] })
    }
    const column = grouped.get(level)
    if (!column.title && node.group) column.title = node.group
    column.items.push(node)
  }
  return [...grouped.values()].sort((left, right) => left.level - right.level)
})

/* 前置知识直接写在节点上，替代原来的跨列连线 */
function prerequisiteText(node) {
  const names = (node.prerequisiteIds || [])
    .map((id) => nodeById.value.get(id))
    .filter(Boolean)
    .map((item) => item.title)
  return names.length ? `前置：${names.join('、')}` : ''
}

function statusLabel(value) {
  return labels[value] || '待解锁'
}

async function load() {
  loading.value = true
  try {
    graph.value = await getPythonKnowledgeGraph() || { nodes: [], edges: [], summary: {} }
    selected.value = graph.value.nodes.find((node) => node.status === 'weak') || graph.value.nodes[0] || null
  } catch (cause) {
    error.value = cause.message
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="feature-page">
    <main class="feature-container">
      <header class="feature-heading">
        <div>
          <h1>Python 知识图谱</h1>
          <p>按阶段查看知识点关系与掌握状态，点击任一知识点查看详情</p>
        </div>
      </header>

      <section class="kg-toolbar">
        <input v-model="keyword" class="kg-search" type="search" placeholder="搜索知识点" />
        <div class="kg-chips">
          <button v-for="[value, label] in filters" :key="value" type="button" class="kg-chip"
            :class="{ 'kg-chip--active': status === value }" @click="status = value">{{ label }}</button>
        </div>
      </section>

      <div v-if="error" class="feature-error">{{ error }}</div>
      <div v-if="loading" class="feature-empty">正在加载知识图谱…</div>

      <div v-else class="kg-workspace">
        <section class="feature-card kg-map">
          <div v-if="!columns.length" class="feature-empty">没有符合条件的知识点</div>
          <div v-else class="kg-columns">
            <div v-for="column in columns" :key="column.level" class="kg-column">
              <header class="kg-column__head">
                <strong>{{ column.title }}</strong>
                <span>{{ column.items.length }} 个</span>
              </header>
              <div class="kg-column__items">
                <button v-for="node in column.items" :key="node.id" type="button" class="kg-node"
                  :class="[`kg-node--${node.status}`, { 'kg-node--selected': selected?.id === node.id }]"
                  @click="selected = node">
                  <span class="kg-node__dot"></span>
                  <strong>{{ node.title }}</strong>
                  <small>{{ statusLabel(node.status) }}</small>
                  <em v-if="prerequisiteText(node)">{{ prerequisiteText(node) }}</em>
                </button>
              </div>
            </div>
          </div>
        </section>

        <aside class="feature-card feature-section kg-detail">
          <template v-if="selected">
            <div class="feature-section__head">
              <h2>知识点详情</h2>
              <span :class="`kg-status kg-status--${selected.status}`">{{ statusLabel(selected.status) }}</span>
            </div>
            <h3>{{ selected.title }}</h3>
            <p class="kg-description">{{ selected.description || '该知识点暂无补充说明' }}</p>
            <div class="kg-evidence">
              <div><span>掌握度</span><strong>{{ selected.attemptCount ? `${Math.round(selected.score || 0)}%` : '—' }}</strong></div>
              <div><span>答题次数</span><strong>{{ selected.attemptCount || '—' }}</strong></div>
              <div><span>错误次数</span><strong>{{ selected.attemptCount ? selected.wrongCount : '—' }}</strong></div>
            </div>
            <section class="kg-block">
              <h4>前置知识</h4>
              <div v-if="prerequisites.length" class="kg-prereq">
                <span v-for="item in prerequisites" :key="item.id">{{ item.title }}<em>{{ statusLabel(item.status) }}</em></span>
              </div>
              <p v-else>无需前置知识</p>
            </section>
            <section class="kg-block">
              <h4>学习建议</h4>
              <p>{{ selected.pathObjective || (selected.status === 'weak' ? '这个知识点偏薄弱，建议回到题库做几道对应练习。' : '按当前学习路径继续学习。') }}</p>
            </section>
            <div class="kg-actions">
              <button class="feature-button feature-button--primary" @click="router.push('/career/nebula/python')">去题库练习</button>
            </div>
          </template>
          <div v-else class="feature-empty">请选择左侧任一知识点</div>
        </aside>
      </div>
    </main>
  </div>
</template>

<style scoped>
/* 外层已有二级导航，收紧顶部留白，与题库/规划页保持一致 */
.feature-container {
  padding: 12px 0 48px;
}

/* 搜索 + 状态筛选：与站内其它页面的工具条一致 */
.kg-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  margin-bottom: 18px;
}

.kg-search {
  width: 260px;
  height: 40px;
  padding: 0 16px;
  border: 1px solid var(--hp-line);
  border-radius: 999px;
  color: var(--hp-ink);
  background: var(--hp-surface);
  font: inherit;
  font-size: 13.5px;
  outline: none;
}

.kg-search:focus {
  border-color: var(--hp-blue-ink);
}

.kg-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.kg-chip {
  padding: 7px 15px;
  border: 1px solid var(--hp-line);
  border-radius: 999px;
  color: var(--hp-ink-2);
  background: var(--hp-surface);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.18s ease, color 0.18s ease, background 0.18s ease;
}

.kg-chip:hover {
  border-color: var(--hp-line-strong);
  color: var(--hp-ink);
}

.kg-chip--active {
  border-color: var(--hp-ink);
  color: #fff;
  background: var(--hp-ink);
}

/* 左图谱 + 右详情 */
.kg-workspace {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 18px;
  align-items: start;
}

.kg-map {
  padding: 22px 24px 26px;
}

/* 阶段列平均分配可用宽度；列数多时也不会撑出横向滚动条 */
.kg-columns {
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: minmax(0, 1fr);
  gap: 14px;
}

.kg-column {
  display: grid;
  align-content: start;
  gap: 10px;
  min-width: 0;
}

.kg-column__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 8px;
  padding-bottom: 10px;
  border-bottom: 1px solid var(--hp-line);
}

.kg-column__head strong {
  color: var(--hp-blue-ink);
  font-size: 13px;
  letter-spacing: 0.02em;
}

.kg-column__head span {
  color: var(--hp-muted);
  font-size: 11.5px;
}

.kg-column__items {
  display: grid;
  gap: 10px;
}

.kg-node {
  display: grid;
  gap: 4px;
  padding: 12px 14px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-sm);
  color: var(--hp-ink);
  background: var(--hp-surface);
  text-align: left;
  cursor: pointer;
  transition: border-color 0.18s ease, box-shadow 0.18s ease, transform 0.18s ease;
}

.kg-node:hover {
  transform: translateY(-2px);
  border-color: var(--hp-line-strong);
  box-shadow: var(--hp-shadow-sm);
}

.kg-node__dot {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--hp-muted);
}

.kg-node strong {
  font-size: 14px;
  line-height: 1.4;
}

.kg-node small {
  color: var(--hp-muted);
  font-size: 11.5px;
}

.kg-node em {
  color: var(--hp-muted);
  font-size: 11px;
  font-style: normal;
  line-height: 1.5;
}

.kg-node--mastered {
  background: var(--hp-green);
}

.kg-node--mastered .kg-node__dot {
  background: var(--hp-green-ink);
}

.kg-node--weak {
  background: var(--hp-pink);
}

.kg-node--weak .kg-node__dot {
  background: var(--hp-pink-ink);
}

.kg-node--learning,
.kg-node--available {
  background: var(--hp-blue);
}

.kg-node--learning .kg-node__dot,
.kg-node--available .kg-node__dot {
  background: var(--hp-blue-ink);
}

.kg-node--locked {
  opacity: 0.6;
}

.kg-node--selected {
  border-color: var(--hp-ink);
  box-shadow: 0 0 0 3px rgba(23, 23, 23, 0.1);
}

/* 右侧详情 */
.kg-detail h3 {
  margin: 18px 0 8px;
  color: var(--hp-ink);
  font-size: 20px;
  word-break: break-word;
}

.kg-description {
  margin: 0;
  color: var(--hp-muted);
  font-size: 13px;
  line-height: 1.7;
}

.kg-status {
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 600;
  white-space: nowrap;
}

.kg-status--mastered {
  color: var(--hp-green-ink);
  background: var(--hp-green);
}

.kg-status--weak {
  color: var(--hp-pink-ink);
  background: var(--hp-pink);
}

.kg-status--learning,
.kg-status--available {
  color: var(--hp-blue-ink);
  background: var(--hp-blue);
}

.kg-status--locked {
  color: var(--hp-muted);
  background: var(--hp-surface-2);
}

.kg-evidence {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin: 18px 0;
  border-top: 1px solid var(--hp-line);
  border-bottom: 1px solid var(--hp-line);
}

.kg-evidence div {
  padding: 14px 4px;
  text-align: center;
}

.kg-evidence div + div {
  border-left: 1px solid var(--hp-line);
}

.kg-evidence span,
.kg-evidence strong {
  display: block;
}

.kg-evidence span {
  color: var(--hp-muted);
  font-size: 11.5px;
}

.kg-evidence strong {
  margin-top: 6px;
  color: var(--hp-ink);
  font-size: 19px;
  font-variant-numeric: tabular-nums;
}

.kg-block {
  margin-top: 18px;
}

.kg-block h4 {
  margin: 0 0 10px;
  color: var(--hp-ink);
  font-size: 13.5px;
}

.kg-block p {
  margin: 0;
  color: var(--hp-muted);
  font-size: 13px;
  line-height: 1.7;
}

.kg-prereq {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.kg-prereq span {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 11px;
  border: 1px solid var(--hp-line);
  border-radius: 999px;
  color: var(--hp-ink-2);
  background: var(--hp-surface-2);
  font-size: 12px;
}

.kg-prereq em {
  color: var(--hp-muted);
  font-style: normal;
  font-size: 11px;
}

.kg-actions {
  display: grid;
  grid-template-columns: 1fr;
  gap: 10px;
  margin-top: 22px;
}

@media (max-width: 1000px) {
  .kg-workspace {
    grid-template-columns: 1fr;
  }

  .kg-columns {
    grid-auto-flow: row;
    grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  }

  .kg-search {
    width: 100%;
  }
}
</style>
