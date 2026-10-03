<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppTabBar from '../../components/AppTabBar.vue'
import { deletePaper, listPapers } from '../../api/paper'
import PaperPageShell from './PaperPageShell.vue'

const router = useRouter()
const route = useRoute()
const papers = ref([])
const loading = ref(false)
const loadError = ref('')
const deletingPaperId = ref(null)
const backTo = computed(() => (route.query.from === 'learning' ? '/learning?tab=python' : '/ai-tools'))

const sources = [
  { key: 'public', icon: '🌐', name: '共有题库', desc: '共同维护' },
  { key: 'private', icon: '📚', name: '私有题库', desc: '我的题库' },
  { key: 'favorite', icon: '★', name: '收藏夹', desc: '快速选题' },
]

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    papers.value = await listPapers({ status: 'draft' }) || []
  } catch (cause) {
    loadError.value = cause.message || '试卷数据加载失败，请检查后端服务'
  } finally {
    loading.value = false
  }
}

function createPaper() {
  router.push('/paper/info')
}

function goMine() {
  router.push('/paper/mine')
}

function chooseSource(source) {
  const paper = papers.value[0]
  router.push({
    path: '/paper/select',
    query: {
      ...(paper ? { paperId: paper.id } : {}),
      source,
    },
  })
}

function openPaper(paper) {
  router.push({ path: '/paper/select', query: { paperId: paper.id, source: 'public' } })
}

async function confirmDelete(paper) {
  if (deletingPaperId.value !== null) return
  if (!window.confirm('删除后该草稿无法恢复，确认删除？')) return
  deletingPaperId.value = paper.id
  try {
    await deletePaper(paper.id)
    papers.value = papers.value.filter((item) => item.id !== paper.id)
  } catch (cause) {
    window.alert(cause.message || '删除失败')
  } finally {
    deletingPaperId.value = null
  }
}

onMounted(load)
</script>

<template>
  <div class="paper-home-page">
    <AppTabBar />
  <PaperPageShell title="试卷生成" subtitle="创建、选题并导出试卷" :back-to="backTo">
    <section class="paper-grid-2">
      <button class="feature-card feature-card--primary" type="button" @click="createPaper">
        <span class="feature-icon">＋</span>
        <strong>创建新试卷</strong>
        <span>填写基本信息后选择题目</span>
      </button>
      <button class="feature-card" type="button" @click="goMine">
        <span class="feature-icon">📄</span>
        <strong>我的试卷</strong>
        <span>查看自己创建的试卷</span>
      </button>
    </section>

    <h2 class="section-title">选题来源</h2>
    <section class="paper-grid-3">
      <button v-for="item in sources" :key="item.key" class="source-card" type="button" @click="chooseSource(item.key)">
        <span class="source-icon">
          <svg v-if="item.key === 'public'" viewBox="0 0 24 24" aria-hidden="true">
            <circle cx="12" cy="12" r="9" />
            <path d="M3 12h18M12 3c2.4 2.4 3.6 5.4 3.6 9s-1.2 6.6-3.6 9c-2.4-2.4-3.6-5.4-3.6-9S9.6 5.4 12 3Z" />
          </svg>
          <svg v-else-if="item.key === 'private'" viewBox="0 0 24 24" aria-hidden="true">
            <path d="M4 5.5A2.5 2.5 0 0 1 6.5 3H11v16H6.5A2.5 2.5 0 0 0 4 21.5v-16Z" />
            <path d="M20 5.5A2.5 2.5 0 0 0 17.5 3H13v16h4.5a2.5 2.5 0 0 1 2.5 2.5v-16Z" />
          </svg>
          <svg v-else viewBox="0 0 24 24" aria-hidden="true">
            <path d="m12 3 2.8 5.7 6.2.9-4.5 4.4 1.1 6.2-5.6-2.9-5.6 2.9 1.1-6.2L3 9.6l6.2-.9L12 3Z" />
          </svg>
        </span>
        <strong>{{ item.name }}</strong>
        <span>{{ item.desc }}</span>
      </button>
    </section>

    <div class="section-head">
      <h2 class="section-title">最近编辑</h2>
      <button class="paper-link" type="button" @click="goMine">我的试卷 ›</button>
    </div>

    <div v-if="loading" class="paper-state">正在加载…</div>
    <div v-else-if="loadError" class="paper-empty paper-empty--error" @click="load">{{ loadError }}，点击重新加载</div>
    <div v-else-if="!papers.length" class="paper-empty">
      <strong>还没有试卷</strong>
      <span>点击上方创建你的第一份试卷</span>
    </div>
    <article v-for="paper in papers" :key="paper.id" class="paper-card draft-card">
      <div class="draft-card__main" @click="openPaper(paper)">
        <strong>{{ paper.name }}</strong>
        <span>{{ paper.subject }} · {{ paper.questionCount || 0 }}题 · {{ paper.totalScore || 0 }}分</span>
      </div>
      <div class="draft-card__actions">
        <span class="status">草稿</span>
        <button type="button" :disabled="deletingPaperId === paper.id" @click.stop="confirmDelete(paper)">
          {{ deletingPaperId === paper.id ? '删除中' : '删除' }}
        </button>
      </div>
    </article>
  </PaperPageShell>
  </div>
</template>

<style scoped>
@import './paper.css';

.paper-home-page {
  min-height: 100vh;
  background: var(--hp-bg);
}

.paper-home-page :deep(.paper-shell) {
  min-height: 100vh;
  padding-top: 60px;
  color: var(--hp-ink);
  background: var(--hp-bg);
  font-family: Inter, 'Segoe UI', system-ui, -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.paper-home-page :deep(.paper-header) {
  position: relative;
  top: auto;
  width: min(1400px, calc(100% - 48px));
  margin: 0 auto;
  padding: 32px 0 0;
  border-bottom: 0;
  background: transparent;
}

.paper-home-page :deep(.paper-header__back) {
  width: 38px;
  height: 38px;
  border: 1px solid rgba(23, 23, 23, 0.28);
  color: var(--hp-ink);
  background: transparent;
  transition: color 0.18s ease, background 0.18s ease;
}

.paper-home-page :deep(.paper-header__back:hover) {
  color: var(--hp-cream);
  background: var(--hp-ink);
}

.paper-home-page :deep(.paper-header__title h1) {
  color: var(--hp-ink);
  font-size: 26px;
  font-weight: 700;
  line-height: 1.25;
}

.paper-home-page :deep(.paper-header__title p) {
  color: var(--hp-muted);
  font-size: 14px;
}

.paper-home-page :deep(.paper-main) {
  width: min(1400px, calc(100% - 48px));
  padding: 0 0 56px;
}

.paper-grid-2,
.paper-grid-3 {
  gap: 18px;
}

.feature-card {
  position: relative;
  display: grid;
  gap: 8px;
  padding: 24px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-lg);
  color: var(--hp-ink);
  background: var(--hp-cream);
  text-align: left;
  transition: transform 0.2s ease, background 0.2s ease;
}

.feature-card:hover {
  transform: translateY(-2px);
}

.feature-card--primary {
  color: var(--hp-ink);
  background: var(--hp-blue);
  border-color: var(--hp-line);
}

.feature-card strong {
  color: var(--hp-ink);
  font-size: 17px;
  font-weight: 700;
}

.feature-card span:last-child {
  color: #55504a;
  font-size: 13px;
}

.feature-icon {
  position: relative;
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border: 1px solid var(--hp-line);
  border-radius: 10px;
  color: var(--hp-ink);
  background: var(--hp-yellow);
  font-size: 0;
}

.feature-icon::before,
.feature-icon::after {
  position: absolute;
  content: "";
}

.feature-card:not(.feature-card--primary) .feature-icon::before {
  inset: 9px 10px;
  border: 1.5px solid currentColor;
  border-radius: 2px;
}

.feature-card:not(.feature-card--primary) .feature-icon::after {
  top: 14px;
  left: 13px;
  width: 7px;
  height: 1.5px;
  border-radius: 2px;
  background: currentColor;
  box-shadow: 0 5px 0 currentColor;
}

.feature-card--primary .feature-icon {
  background: var(--hp-cream);
}

.feature-card--primary .feature-icon::before {
  top: 16px;
  left: 9px;
  width: 14px;
  height: 2px;
  border-radius: 2px;
  background: currentColor;
}

.feature-card--primary .feature-icon::after {
  top: 10px;
  left: 15px;
  width: 2px;
  height: 14px;
  border-radius: 2px;
  background: currentColor;
}

.section-title {
  margin: 32px 0 14px;
  color: var(--hp-ink);
  font-size: 19px;
  font-weight: 700;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.source-card {
  display: grid;
  justify-items: center;
  gap: 9px;
  padding: 20px 16px;
  border: 1px solid var(--hp-line);
  border-radius: 18px;
  color: var(--hp-ink);
  background: var(--hp-cream);
  text-align: center;
  transition: transform 0.2s ease, background 0.2s ease;
}

.source-card:nth-child(1) {
  background: var(--hp-blue);
}

.source-card:nth-child(2) {
  background: var(--hp-green);
}

.source-card:nth-child(3) {
  background: var(--hp-pink-soft);
}

.source-card:hover {
  transform: translateY(-2px);
}

.source-icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 42px;
  border: 1px solid var(--hp-line);
  border-radius: 12px;
  background: var(--hp-cream);
}

.source-icon svg {
  width: 23px;
  height: 23px;
  fill: none;
  stroke: currentColor;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.source-card strong {
  color: var(--hp-ink);
  font-size: 15px;
  font-weight: 700;
}

.source-card span:last-child {
  color: #55504a;
  font-size: 12px;
}

.draft-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  cursor: pointer;
  margin-bottom: 0;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-lg);
  background: var(--hp-cream);
  box-shadow: none;
}

.draft-card__main {
  display: grid;
  gap: 6px;
}

.draft-card__main span {
  color: var(--hp-muted);
  font-size: 13px;
}

.draft-card__actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.status {
  padding: 4px 10px;
  border-radius: 999px;
  color: var(--hp-ink);
  background: var(--hp-yellow);
  font-size: 13px;
  font-weight: 600;
}

.draft-card__actions button {
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid #d9b0ab;
  border-radius: 999px;
  color: #a54239;
  background: transparent;
  font-size: 13px;
  font-weight: 600;
}

.draft-card__actions button:hover:not(:disabled) {
  color: #fffdf8;
  background: #a54239;
}

.paper-empty {
  display: grid;
  gap: 8px;
  padding: 48px 20px;
  border: 1px dashed rgba(23, 23, 23, 0.28);
  border-radius: var(--hp-r-md);
  color: var(--hp-muted);
  background: var(--hp-cream);
  text-align: center;
}

.paper-empty strong {
  color: var(--hp-ink);
  font-size: 16px;
}

.paper-empty--error {
  border-style: solid;
  border-color: #d9b0ab;
  color: #a54239;
  background: #faf0ee;
}

.paper-link {
  color: var(--hp-ink);
}

@media (max-width: 760px) {
  .paper-home-page :deep(.paper-header),
  .paper-home-page :deep(.paper-main) {
    width: calc(100% - 32px);
  }

  .paper-home-page :deep(.paper-header) {
    padding-top: 24px;
  }
}
</style>
