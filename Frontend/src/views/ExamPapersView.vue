<script setup>
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import AppTabBar from '../components/AppTabBar.vue'
import { listExamPapers, startExam } from '../api/exam'

const route = useRoute()
const router = useRouter()
const papers = ref([])
const loading = ref(true)
const starting = ref(null)
const error = ref('')

async function loadPapers() {
  loading.value = true
  error.value = ''
  try {
    const result = await listExamPapers({ page: 0, size: 30 })
    papers.value = result.content || []
  } catch (cause) {
    error.value = cause.message
  } finally {
    loading.value = false
  }
}

async function enterExam(paper) {
  starting.value = paper.id
  error.value = ''
  try {
    const attempt = await startExam(paper.id)
    router.push({ path: `/mine/papers/attempts/${attempt.id}`, query: route.query })
  } catch (cause) {
    error.value = cause.message
  } finally {
    starting.value = null
  }
}

function goBack() {
  // 从课程详情的「课程考试」进来时，直接回到那门课程
  if (route.query.from === 'course' && route.query.courseId) {
    router.push(`/courses/${route.query.courseId}`)
    return
  }
  if (window.history.length > 1) {
    router.back()
    return
  }
  router.push('/mine')
}

onMounted(loadPapers)
</script>

<template>
  <div class="feature-page papers-shell">
    <AppTabBar />
    <header class="papers-header">
      <div class="papers-header__left">
        <button class="back-button" type="button" aria-label="返回上一页" @click="goBack">← 返回上一页</button>
        <div><h1>我的试卷</h1><p>考试与练习</p></div>
      </div>
      <button class="account-entry" type="button" @click="router.push('/mine')">个人中心</button>
    </header>
    <main class="papers-main">
      <div v-if="error" class="message message--error">{{ error }}</div>
      <div v-if="loading" class="empty">正在加载试卷…</div>
      <div v-else-if="!papers.length" class="empty">暂无可参加的试卷</div>
      <article v-for="paper in papers" :key="paper.id" class="paper">
        <div class="paper__status">
          <span>可参加</span><small>已完成 {{ paper.attemptCount }} 次</small>
        </div>
        <h2>{{ paper.title }}</h2>
        <p>{{ paper.subtitle || '请在规定时间内独立完成试卷' }}</p>
        <div class="paper__facts">
          <div><b>{{ paper.questionCount }}</b><span>题目</span></div>
          <div><b>{{ paper.totalScore }}</b><span>总分</span></div>
          <div><b>{{ paper.durationMinutes }}</b><span>分钟</span></div>
        </div>
        <button type="button" :disabled="starting === paper.id" @click="enterExam(paper)">
          {{ starting === paper.id ? '正在生成试卷…' : paper.inProgressAttemptId ? '继续答题' : '开始考试' }}
        </button>
        <button class="history-entry" type="button"
          @click="router.push(`/mine/papers/${paper.id}/history`)">
          <span><i></i>历史记录</span>
          <span>共完成 {{ paper.attemptCount }} 次　›</span>
        </button>
      </article>
    </main>
  </div>
</template>

<style scoped>
.papers-shell {
  min-height: 100vh;
  padding-top: 60px;
  color: var(--hp-ink);
  background: var(--hp-bg);
  font-family: Inter, 'Segoe UI', system-ui, -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.papers-shell *,
.papers-shell *::before,
.papers-shell *::after {
  box-sizing: border-box;
}

.papers-header {
  position: sticky;
  top: 60px;
  z-index: 8;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 18px;
  width: min(calc(100% - 48px), 1120px);
  margin: 0 auto;
  padding: 20px 0;
  border-bottom: 1px solid var(--hp-line);
  background: var(--hp-bg);
}

.papers-header__left {
  display: flex;
  align-items: center;
  gap: 14px;
  min-width: 0;
}

.back-button,
.account-entry {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid var(--hp-line-strong);
  border-radius: 999px;
  color: var(--hp-ink);
  background: var(--hp-surface);
  font-size: 13px;
  font-weight: 650;
  white-space: nowrap;
  cursor: pointer;
}

.account-entry {
  border-color: var(--hp-ink);
  color: #fff;
  background: var(--hp-ink);
}

.papers-header h1 {
  margin: 0;
  color: var(--hp-ink);
  font-size: 23px;
}

.papers-header p {
  margin: 3px 0 0;
  color: var(--hp-muted);
  font-size: 13px;
}

.papers-main {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  width: min(calc(100% - 48px), 1120px);
  margin: 0 auto;
  padding: 20px 0 60px;
}

.paper,
.message,
.empty {
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-lg);
  background: var(--hp-surface);
  box-shadow: var(--hp-shadow-sm);
}

.paper {
  display: grid;
  gap: 13px;
  padding: 24px;
}

.paper__status {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.paper__status span {
  padding: 5px 9px;
  border-radius: 999px;
  color: var(--hp-blue-ink);
  background: var(--hp-blue);
  font-size: 12px;
  font-weight: 700;
}

.paper__status small,
.paper > p,
.paper__facts span,
.history-entry span:last-child {
  color: var(--hp-muted);
  font-size: 12px;
}

.paper h2 {
  margin: 0;
  color: var(--hp-ink);
  font-size: 20px;
}

.paper > p {
  margin: 0;
  font-size: 13.5px;
}

.paper__facts {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  margin: 8px 0;
  padding: 14px 0;
  border-top: 1px solid var(--hp-line);
  border-bottom: 1px solid var(--hp-line);
}

.paper__facts div {
  text-align: center;
}

.paper__facts div + div {
  border-left: 1px solid var(--hp-line);
}

.paper__facts b,
.paper__facts span {
  display: block;
}

.paper__facts b {
  color: var(--hp-ink);
  font-size: 19px;
}

.paper > button:not(.history-entry) {
  width: 100%;
  min-height: 44px;
  border: 1px solid var(--hp-ink);
  border-radius: 999px;
  color: #fff;
  background: var(--hp-ink);
  font-weight: 700;
  cursor: pointer;
}

.paper > button:disabled {
  opacity: .55;
  cursor: wait;
}

.history-entry {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  padding: 13px 2px 0;
  border: 0;
  border-top: 1px solid var(--hp-line);
  color: var(--hp-ink);
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.history-entry span:first-child {
  display: flex;
  align-items: center;
  gap: 9px;
  font-weight: 700;
}

.history-entry i {
  display: inline-block;
  width: 15px;
  height: 15px;
  border: 2px solid var(--hp-blue-ink);
  border-radius: 50%;
  box-shadow: inset 0 0 0 3px #fff;
  background: var(--hp-blue-ink);
}

.message,
.empty {
  grid-column: 1 / -1;
  padding: 36px;
  color: var(--hp-muted);
  text-align: center;
}

.message--error {
  color: #a54239;
  background: #faf0ee;
}

@media (max-width: 820px) {
  .papers-header,
  .papers-main {
    width: min(calc(100% - 24px), 1120px);
  }

  .papers-main {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .papers-header {
    align-items: flex-start;
  }

  .papers-header__left {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
