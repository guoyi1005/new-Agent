<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppTabBar from '../../components/AppTabBar.vue'
import { getPythonProblemList } from '../../api/pythonProblem'
import { deletePythonPaper, getPythonPaper, listPythonPapers, savePythonPaper } from '../../api/pythonPaper'

const router = useRouter()
const route = useRoute()

const loading = ref(true)
const error = ref('')
const problems = ref([])

/* 生成配置 */
const paperTitle = ref('Python 算法练习卷')
const paperCount = ref(10)
const difficulty = ref('all')
const selectedTags = ref([])

/* 生成结果 */
const paper = ref([])
const generatedAt = ref('')

/* 正在查看的历史试卷：非空表示处于「查看模式」，此时不改动生成配置 */
const viewingPaper = ref(null)

/* 已保存的试卷 */
const savedPapers = ref([])
const savingPaper = ref(false)
const paperMessage = ref('')

const COUNT_OPTIONS = [5, 10, 15, 20]
const SCORE_PER_QUESTION = 10
const DIFFICULTY_LABELS = { easy: '简单', medium: '中等', hard: '困难' }

/* 这个页面属于「学习实践」，返回统一回到技能练习；
   不再回退到 AI 工具箱，避免把用户带到无关页面 */
const backTo = '/learning?tab=python'

async function load() {
  loading.value = true
  error.value = ''
  try {
    const response = await getPythonProblemList()
    const list = Array.isArray(response?.data) ? response.data
      : (Array.isArray(response) ? response : [])
    problems.value = list
  } catch (cause) {
    error.value = cause?.message || '题库加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

/* 标签按出现次数排序，方便挑高频知识点 */
const allTags = computed(() => {
  const counter = new Map()
  for (const item of problems.value) {
    for (const tag of item.tags || []) counter.set(tag, (counter.get(tag) || 0) + 1)
  }
  return [...counter.entries()]
    .map(([name, total]) => ({ name, total }))
    .sort((left, right) => right.total - left.total)
})

const difficultyOptions = computed(() => {
  const countOf = (key) => problems.value.filter((item) => item.difficulty === key).length
  return [
    { key: 'all', label: '全部', total: problems.value.length },
    { key: 'easy', label: '简单', total: countOf('easy') },
    { key: 'medium', label: '中等', total: countOf('medium') },
    { key: 'hard', label: '困难', total: countOf('hard') },
  ]
})

/* 符合当前条件的题目池 */
const matched = computed(() => problems.value.filter((item) => {
  if (difficulty.value !== 'all' && item.difficulty !== difficulty.value) return false
  if (selectedTags.value.length) {
    const tags = item.tags || []
    if (!selectedTags.value.every((tag) => tags.includes(tag))) return false
  }
  return true
}))

const totalScore = computed(() => (paper.value.length || 0) * SCORE_PER_QUESTION)

function toggleTag(name) {
  const index = selectedTags.value.indexOf(name)
  if (index >= 0) selectedTags.value.splice(index, 1)
  else selectedTags.value.push(name)
}

function clearFilters() {
  difficulty.value = 'all'
  selectedTags.value = []
}

function difficultyLabel(value) {
  return DIFFICULTY_LABELS[value] || value || '未知'
}

/* 从符合条件的题目里随机抽题组成试卷 */
function generatePaper() {
  viewingPaper.value = null
  const pool = [...matched.value]
  if (!pool.length) {
    paper.value = []
    generatedAt.value = ''
    return
  }
  for (let index = pool.length - 1; index > 0; index -= 1) {
    const swap = Math.floor(Math.random() * (index + 1))
    ;[pool[index], pool[swap]] = [pool[swap], pool[index]]
  }
  paper.value = pool.slice(0, Math.min(paperCount.value, pool.length))
  generatedAt.value = new Date().toLocaleString('zh-CN', { hour12: false })
}

function reshuffle() {
  generatePaper()
}

function startPractice() {
  if (!paper.value.length) return
  router.push(`/career/nebula/python/practice/${paper.value[0].id}`)
}

function openProblem(id) {
  router.push(`/career/nebula/python/practice/${id}`)
}

function printPaper() {
  window.print()
}

async function loadSavedPapers() {
  try {
    const list = await listPythonPapers()
    savedPapers.value = Array.isArray(list) ? list : []
  } catch {
    savedPapers.value = []
  }
}

/* 保存当前试卷：只存题目 ID 快照，题目正文仍从题库读取 */
async function savePaper() {
  if (!paper.value.length || savingPaper.value) return
  savingPaper.value = true
  paperMessage.value = ''
  try {
    await savePythonPaper({
      title: paperTitle.value,
      difficulty: difficulty.value,
      tags: [...selectedTags.value],
      questionIds: paper.value.map((item) => item.id),
    })
    await loadSavedPapers()
    viewingPaper.value = null
    paperMessage.value = '试卷已保存，可在「我的练习卷」中随时打开'
  } catch (cause) {
    paperMessage.value = cause?.message || '保存失败，请稍后重试'
  } finally {
    savingPaper.value = false
  }
}

/* 打开已保存的试卷：只还原题目，不动生成配置，避免和历史条件混淆 */
function openSavedPaper(saved) {
  const byId = new Map(problems.value.map((item) => [item.id, item]))
  paper.value = (saved.questionIds || []).map((id) => byId.get(id)).filter(Boolean)
  viewingPaper.value = saved
  generatedAt.value = ''
  paperMessage.value = `正在查看已保存的试卷「${saved.title}」`
}

/* 退出查看模式，回到自己的生成条件 */
function exitViewing() {
  viewingPaper.value = null
  paper.value = []
  generatedAt.value = ''
  paperMessage.value = ''
}

async function removeSavedPaper(saved) {
  if (!window.confirm(`删除试卷「${saved.title}」？删除后无法恢复。`)) return
  try {
    await deletePythonPaper(saved.id)
    await loadSavedPapers()
    paperMessage.value = '试卷已删除'
  } catch (cause) {
    paperMessage.value = cause?.message || '删除失败，请稍后重试'
  }
}

onMounted(async () => {
  await load()
  await loadSavedPapers()
  // 从「我的练习卷」打开某一份试卷
  const paperId = route.query.paperId
  if (paperId) {
    try {
      const saved = await getPythonPaper(paperId)
      if (saved) openSavedPaper(saved)
    } catch (cause) {
      paperMessage.value = cause?.message || '试卷打开失败，请到「我的练习卷」重试'
    }
  }
})
</script>

<template>
  <div class="feature-page paper-page">
    <AppTabBar />
    <main class="feature-container">
      <header class="feature-heading">
        <div>
          <h1>Python 试卷生成</h1>
          <p>从 Python 题库按难度和知识点抽题，生成一份可直接打印的练习卷</p>
        </div>
        <div class="feature-actions">
          <button type="button" class="feature-button" @click="router.push('/paper/mine')">
            我的练习卷<span v-if="savedPapers.length">（{{ savedPapers.length }}）</span>
          </button>
          <button type="button" class="feature-button feature-button--primary" @click="router.push(backTo)">返回学习实践</button>
        </div>
      </header>

      <div v-if="paperMessage" class="paper-message">
        <span>{{ paperMessage }}</span>
        <button v-if="viewingPaper" type="button" class="paper-message__action" @click="exitViewing">退出查看，生成新试卷</button>
      </div>
      <div v-if="error" class="feature-error">{{ error }}</div>
      <div v-if="loading" class="feature-empty">正在加载题库…</div>

      <template v-else>
        <section class="feature-card feature-section paper-config">
          <div class="feature-section__head">
            <div>
              <h2>生成配置</h2>
              <p>题库共 {{ problems.length }} 道题，当前条件匹配 {{ matched.length }} 道</p>
            </div>
            <button type="button" class="paper-link" @click="clearFilters">清空条件</button>
          </div>

          <label class="paper-field">
            <span>试卷标题</span>
            <input v-model="paperTitle" class="paper-input" type="text" placeholder="给这份卷子起个名字" />
          </label>

          <div class="paper-field">
            <span>题量</span>
            <div class="paper-chips">
              <button v-for="value in COUNT_OPTIONS" :key="value" type="button" class="paper-chip"
                :class="{ 'paper-chip--active': paperCount === value }" @click="paperCount = value">{{ value }} 题</button>
            </div>
          </div>

          <div class="paper-field">
            <span>难度</span>
            <div class="paper-chips">
              <button v-for="item in difficultyOptions" :key="item.key" type="button" class="paper-chip"
                :class="{ 'paper-chip--active': difficulty === item.key }" @click="difficulty = item.key">
                {{ item.label }}<em>{{ item.total }}</em>
              </button>
            </div>
          </div>

          <div v-if="allTags.length" class="paper-field">
            <span>知识点（可多选，选中的知识点需同时满足）</span>
            <div class="paper-chips paper-chips--tags">
              <button v-for="tag in allTags" :key="tag.name" type="button" class="paper-chip"
                :class="{ 'paper-chip--active': selectedTags.includes(tag.name) }" @click="toggleTag(tag.name)">
                {{ tag.name }}<em>{{ tag.total }}</em>
              </button>
            </div>
          </div>

          <button type="button" class="feature-button feature-button--primary paper-generate"
            :disabled="!matched.length" @click="generatePaper">
            生成试卷
          </button>
          <p v-if="!matched.length" class="paper-hint">当前条件下没有题目，请放宽难度或取消部分知识点。</p>
        </section>

        <section v-if="paper.length" class="paper-sheet">
          <header class="paper-sheet__head">
            <h2>{{ viewingPaper ? viewingPaper.title : (paperTitle || 'Python 练习卷') }}</h2>
            <p v-if="viewingPaper">共 {{ paper.length }} 题 · 满分 {{ viewingPaper.totalScore }} 分 · 每题 {{ SCORE_PER_QUESTION }} 分 · 保存于 {{ viewingPaper.createdAt }}</p>
            <p v-else>共 {{ paper.length }} 题 · 满分 {{ totalScore }} 分 · 每题 {{ SCORE_PER_QUESTION }} 分<span v-if="generatedAt"> · 生成于 {{ generatedAt }}</span></p>
          </header>

          <ol class="paper-questions">
            <li v-for="(item, index) in paper" :key="item.id">
              <div class="paper-question__main">
                <div class="paper-question__title">
                  <span class="paper-question__no">{{ index + 1 }}</span>
                  <strong>{{ item.title }}</strong>
                  <span class="paper-question__score">{{ SCORE_PER_QUESTION }} 分</span>
                </div>
                <div class="paper-question__meta">
                  <span :class="`paper-diff paper-diff--${item.difficulty}`">{{ difficultyLabel(item.difficulty) }}</span>
                  <span v-for="tag in item.tags || []" :key="tag" class="paper-tag">{{ tag }}</span>
                  <span class="paper-rate">通过率 {{ item.passRate }}%</span>
                </div>
              </div>
              <button type="button" class="paper-open" @click="openProblem(item.id)">去做这题</button>
            </li>
          </ol>

          <div class="paper-sheet__actions">
            <button v-if="!viewingPaper" type="button" class="feature-button" @click="reshuffle">换一批</button>
            <button type="button" class="feature-button" @click="printPaper">打印试卷</button>
            <button v-if="!viewingPaper" type="button" class="feature-button" :disabled="savingPaper" @click="savePaper">{{ savingPaper ? '保存中…' : '保存试卷' }}</button>
            <button v-else type="button" class="feature-button" @click="exitViewing">退出查看</button>
            <button type="button" class="feature-button feature-button--primary" @click="startPractice">从第 1 题开始</button>
          </div>
        </section>

        <div v-else class="feature-empty paper-empty">
          <strong>还没有生成试卷</strong>
          <p>在上方选择题量、难度和知识点，然后点「生成试卷」。</p>
        </div>

        <section v-if="savedPapers.length" class="feature-card feature-section paper-saved">
          <div class="feature-section__head">
            <div>
              <h2>我保存的试卷</h2>
              <p>共 {{ savedPapers.length }} 份，点击即可重新打开</p>
            </div>
          </div>
          <ul class="paper-saved__list">
            <li v-for="saved in savedPapers" :key="saved.id">
              <button type="button" class="paper-saved__open" @click="openSavedPaper(saved)">
                <strong>{{ saved.title }}</strong>
                <small>{{ saved.questionCount }} 题 · 满分 {{ saved.totalScore }} 分 · {{ saved.createdAt }}</small>
              </button>
              <button type="button" class="paper-saved__delete" @click="removeSavedPaper(saved)">删除</button>
            </li>
          </ul>
        </section>
      </template>
    </main>
  </div>
</template>

<style scoped>
/* 外层已有主导航，收紧顶部留白 */
.feature-container {
  padding: 12px 0 56px;
}

.feature-section__head p {
  margin: 5px 0 0;
  color: var(--hp-muted);
  font-size: 13px;
}

.paper-link {
  padding: 0;
  border: 0;
  color: var(--hp-blue-ink);
  background: transparent;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.paper-field {
  display: grid;
  gap: 8px;
  margin-top: 18px;
}

.paper-field > span {
  color: var(--hp-ink-2);
  font-size: 13px;
  font-weight: 600;
}

.paper-input {
  height: 42px;
  padding: 0 14px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-sm);
  color: var(--hp-ink);
  background: var(--hp-surface);
  font: inherit;
  font-size: 13.5px;
  outline: none;
}

.paper-input:focus {
  border-color: var(--hp-blue-ink);
}

.paper-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.paper-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border: 1px solid var(--hp-line);
  border-radius: 999px;
  color: var(--hp-ink-2);
  background: var(--hp-surface);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  transition: border-color 0.18s ease, color 0.18s ease, background 0.18s ease;
}

.paper-chip em {
  color: var(--hp-muted);
  font-style: normal;
  font-size: 11.5px;
}

.paper-chip:hover {
  border-color: var(--hp-line-strong);
  color: var(--hp-ink);
}

.paper-chip--active {
  border-color: var(--hp-ink);
  color: #fff;
  background: var(--hp-ink);
}

.paper-chip--active em {
  color: rgba(255, 255, 255, 0.7);
}

.paper-generate {
  width: max-content;
  margin-top: 22px;
}

.paper-hint {
  margin: 10px 0 0;
  color: var(--hp-muted);
  font-size: 12.5px;
}

/* 生成结果：一张可打印的卷子 */
.paper-sheet {
  margin-top: 20px;
  padding: 28px 30px 30px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-lg);
  background: var(--hp-surface);
  box-shadow: var(--hp-shadow-sm);
}

.paper-sheet__head {
  padding-bottom: 16px;
  border-bottom: 1px solid var(--hp-line);
  text-align: center;
}

.paper-sheet__head h2 {
  margin: 0;
  color: var(--hp-ink);
  font-size: 22px;
  letter-spacing: 0.02em;
}

.paper-sheet__head p {
  margin: 8px 0 0;
  color: var(--hp-muted);
  font-size: 12.5px;
}

.paper-questions {
  display: grid;
  gap: 12px;
  margin: 20px 0 0;
  padding: 0;
  list-style: none;
}

.paper-questions li {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-sm);
  background: var(--hp-surface);
}

.paper-question__title {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.paper-question__no {
  display: grid;
  place-items: center;
  width: 26px;
  height: 26px;
  border-radius: 50%;
  color: var(--hp-blue-ink);
  background: var(--hp-blue);
  font-size: 12.5px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.paper-question__title strong {
  color: var(--hp-ink);
  font-size: 15px;
}

.paper-question__score {
  color: var(--hp-muted);
  font-size: 12px;
}

.paper-question__meta {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin: 8px 0 0 36px;
}

.paper-diff {
  padding: 2px 9px;
  border-radius: 999px;
  font-size: 11.5px;
  font-weight: 600;
}

.paper-diff--easy {
  color: var(--hp-green-ink);
  background: var(--hp-green);
}

.paper-diff--medium {
  color: var(--hp-yellow-ink);
  background: var(--hp-yellow);
}

.paper-diff--hard {
  color: var(--hp-pink-ink);
  background: var(--hp-pink);
}

.paper-tag {
  padding: 2px 9px;
  border: 1px solid var(--hp-line);
  border-radius: 999px;
  color: var(--hp-ink-2);
  background: var(--hp-surface-2);
  font-size: 11.5px;
}

.paper-rate {
  color: var(--hp-muted);
  font-size: 11.5px;
}

.paper-open {
  padding: 7px 14px;
  border: 1px solid var(--hp-line-strong);
  border-radius: 999px;
  color: var(--hp-ink);
  background: transparent;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
  transition: border-color 0.18s ease, color 0.18s ease, background 0.18s ease;
}

.paper-open:hover {
  border-color: var(--hp-blue-ink);
  color: var(--hp-blue-ink);
  background: var(--hp-tint);
}

.paper-sheet__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 22px;
  padding-top: 18px;
  border-top: 1px solid var(--hp-line);
}

.paper-empty p {
  margin: 6px 0 0;
}

@media (max-width: 720px) {
  .paper-sheet {
    padding: 20px 16px 22px;
  }

  .paper-questions li {
    grid-template-columns: 1fr;
  }
}

.paper-message {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin: 0 0 16px;
  padding: 12px 16px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-sm);
  color: var(--hp-blue-ink);
  background: var(--hp-tint);
  font-size: 13px;
}

.paper-message__action {
  padding: 6px 14px;
  border: 1px solid var(--hp-line-strong);
  border-radius: 999px;
  color: var(--hp-ink);
  background: var(--hp-surface);
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}

.paper-message__action:hover {
  border-color: var(--hp-blue-ink);
  color: var(--hp-blue-ink);
}

.paper-saved {
  margin-top: 20px;
}

.paper-saved__list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.paper-saved__list li {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-sm);
}

.paper-saved__open {
  display: grid;
  gap: 5px;
  padding: 0;
  border: 0;
  color: var(--hp-ink);
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.paper-saved__open strong {
  font-size: 14.5px;
}

.paper-saved__open small {
  color: var(--hp-muted);
  font-size: 12px;
}

.paper-saved__delete {
  padding: 7px 14px;
  border: 1px solid #d9b0ab;
  border-radius: 999px;
  color: #a54239;
  background: transparent;
  font-size: 12.5px;
  font-weight: 600;
  cursor: pointer;
  white-space: nowrap;
}

.paper-saved__delete:hover {
  color: #fffdf8;
  background: #a54239;
}

/* 打印时只保留卷面 */
@media print {
  .paper-page :deep(.app-site-header),
  .paper-config,
  .paper-empty,
  .paper-open,
  .paper-saved,
  .paper-message,
  .paper-sheet__actions {
    display: none !important;
  }

  .paper-page :deep(.feature-container) {
    width: 100%;
    padding: 0;
  }

  .paper-sheet {
    margin: 0;
    padding: 0;
    border: 0;
    box-shadow: none;
  }

  .paper-questions li {
    break-inside: avoid;
    page-break-inside: avoid;
  }
}
</style>
