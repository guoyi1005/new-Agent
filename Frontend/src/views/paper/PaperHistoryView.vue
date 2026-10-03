<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppTabBar from '../../components/AppTabBar.vue'
import { deletePythonPaper, listPythonPapers } from '../../api/pythonPaper'

const router = useRouter()
const route = useRoute()

const loading = ref(true)
const error = ref('')
const message = ref('')
const papers = ref([])

const DIFFICULTY_TEXT = { all: '不限难度', easy: '简单', medium: '中等', hard: '困难' }

function difficultyText(value) {
  return DIFFICULTY_TEXT[value] || '不限难度'
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const list = await listPythonPapers()
    papers.value = Array.isArray(list) ? list : []
  } catch (cause) {
    error.value = cause?.message || '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

/* 打开试卷：回到生成页并带上 paperId，由那边还原题目 */
function openPaper(item) {
  router.push({ path: '/paper', query: { paperId: item.id, from: route.query.from || undefined } })
}

async function removePaper(item) {
  if (!window.confirm(`删除试卷「${item.title}」？删除后无法恢复。`)) return
  try {
    await deletePythonPaper(item.id)
    papers.value = papers.value.filter((paper) => paper.id !== item.id)
    message.value = '试卷已删除'
  } catch (cause) {
    message.value = cause?.message || '删除失败，请稍后重试'
  }
}

onMounted(load)
</script>

<template>
  <div class="feature-page">
    <AppTabBar />
    <main class="feature-container">
      <header class="feature-heading">
        <div>
          <h1>我的练习卷</h1>
          <p>你保存过的 Python 试卷，可以随时打开、打印或删除</p>
        </div>
        <div class="feature-actions">
          <button type="button" class="feature-button feature-button--primary" @click="router.push('/paper')">去生成新试卷</button>
        </div>
      </header>

      <p v-if="message" class="history-message">{{ message }}</p>
      <div v-if="error" class="feature-error">{{ error }}</div>
      <div v-if="loading" class="feature-empty">正在加载…</div>

      <template v-else>
        <div v-if="!papers.length" class="feature-empty history-empty">
          <strong>还没有保存过试卷</strong>
          <p>在「Python 试卷生成」里选题量和知识点，生成后点「保存试卷」，就会出现在这里。</p>
          <button type="button" class="feature-button feature-button--primary" @click="router.push('/paper')">去生成试卷</button>
        </div>

        <ul v-else class="history-list">
          <li v-for="item in papers" :key="item.id" class="feature-card history-item">
            <div class="history-item__main">
              <strong>{{ item.title }}</strong>
              <span class="history-item__meta">
                {{ item.questionCount }} 题 · 满分 {{ item.totalScore }} 分 · {{ difficultyText(item.difficulty) }} · {{ item.createdAt }}
              </span>
              <div v-if="item.tags && item.tags.length" class="history-item__tags">
                <span v-for="tag in item.tags" :key="tag">{{ tag }}</span>
              </div>
            </div>
            <div class="history-item__actions">
              <button type="button" class="feature-button" @click="openPaper(item)">打开</button>
              <button type="button" class="history-item__delete" @click="removePaper(item)">删除</button>
            </div>
          </li>
        </ul>
      </template>
    </main>
  </div>
</template>

<style scoped>
/* 外层已有主导航，收紧顶部留白 */
.feature-container {
  padding: 12px 0 56px;
}

.history-message {
  margin: 0 0 16px;
  padding: 12px 16px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-sm);
  color: var(--hp-blue-ink);
  background: var(--hp-tint);
  font-size: 13px;
}

.history-empty p {
  margin: 6px 0 16px;
}

.history-list {
  display: grid;
  gap: 12px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.history-item {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 16px;
  padding: 18px 20px;
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.history-item:hover {
  border-color: var(--hp-line-strong);
  box-shadow: var(--hp-shadow-sm);
}

.history-item__main {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.history-item__main strong {
  color: var(--hp-ink);
  font-size: 16px;
}

.history-item__meta {
  color: var(--hp-muted);
  font-size: 12.5px;
}

.history-item__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.history-item__tags span {
  padding: 2px 9px;
  border: 1px solid var(--hp-line);
  border-radius: 999px;
  color: var(--hp-ink-2);
  background: var(--hp-surface-2);
  font-size: 11.5px;
}

.history-item__actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.history-item__delete {
  padding: 9px 16px;
  border: 1px solid #d9b0ab;
  border-radius: 999px;
  color: #a54239;
  background: transparent;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.18s ease, background 0.18s ease;
}

.history-item__delete:hover {
  color: #fffdf8;
  background: #a54239;
}

@media (max-width: 720px) {
  .history-item {
    grid-template-columns: 1fr;
  }
}
</style>
