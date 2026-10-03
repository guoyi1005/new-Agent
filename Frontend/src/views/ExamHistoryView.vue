<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getExamPaper, listExamHistory } from '../api/exam'
import AppTabBar from '../components/AppTabBar.vue'
import { useExamReturn } from '../composables/useExamReturn'

const route = useRoute()
const router = useRouter()
const { examQuery, goExamBack } = useExamReturn()
const records = ref([])
const paper = ref(null)
const loading = ref(true)
const error = ref('')

const completedCount = computed(() => records.value.length)
const scores = computed(() => records.value.map((item) => Number(item.objectiveScore || 0)))
const fullScore = computed(() => Number(records.value[0]?.objectiveTotalScore || paper.value?.totalScore || 100))
const bestScore = computed(() => (scores.value.length ? Math.max(...scores.value) : 0))
const averageScore = computed(() => (
  scores.value.length
    ? Math.round(scores.value.reduce((sum, value) => sum + value, 0) / scores.value.length)
    : 0
))
const latestScore = computed(() => (scores.value.length ? scores.value[0] : 0))

function formatDate(value) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric', month: '2-digit', day: '2-digit',
    hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false,
  }).format(new Date(value)).replaceAll('/', '-')
}

function duration(record) {
  if (!record.startedAt || !record.submittedAt) return '—'
  const seconds = Math.max(0, Math.floor(
    (new Date(record.submittedAt) - new Date(record.startedAt)) / 1000,
  ))
  return `${Math.floor(seconds / 60)}分${seconds % 60}秒`
}

/* 得分分档，避免所有分数都是同一种颜色的"平铺"观感 */
function scoreTone(value) {
  const total = fullScore.value || 100
  const percent = total ? (Number(value) / total) * 100 : 0
  if (percent >= 60) return 'pass'
  if (percent > 0) return 'partial'
  return 'zero'
}

onMounted(async () => {
  const paperId = route.params.paperId
  const [historyResult, paperResult] = await Promise.allSettled([
    listExamHistory(paperId),
    getExamPaper(paperId),
  ])
  if (historyResult.status === 'fulfilled') {
    records.value = Array.isArray(historyResult.value) ? historyResult.value : []
  } else {
    error.value = historyResult.reason?.message || '历史记录加载失败'
  }
  if (paperResult.status === 'fulfilled') {
    paper.value = paperResult.value || null
  }
  loading.value = false
})
</script>

<template>
  <div class="history-page">
    <AppTabBar />
    <header class="history-header">
      <button class="history-back" type="button" aria-label="返回来源" @click="goExamBack()">‹</button>
      <div class="history-header__title">
        <h1>{{ paper?.title || '历史记录' }}</h1>
        <p>
          共完成 {{ completedCount }} 次
          <template v-if="paper"> · {{ paper.questionCount }} 题 · 满分 {{ paper.totalScore }} 分</template>
        </p>
      </div>
    </header>

    <main class="history-main">
      <div v-if="error" class="state state--error">{{ error }}</div>
      <div v-else-if="loading" class="state">正在加载历史记录…</div>
      <div v-else-if="!records.length" class="state">
        <b>暂无历史记录</b><span>完成并提交试卷后，记录会显示在这里。</span>
      </div>

      <template v-else>
        <section class="stats">
          <article><span>最高分</span><strong>{{ bestScore }}</strong><small>/ {{ fullScore }} 分</small></article>
          <article><span>平均分</span><strong>{{ averageScore }}</strong><small>/ {{ fullScore }} 分</small></article>
          <article>
            <span>最近一次</span>
            <strong :class="`tone-${scoreTone(latestScore)}`">{{ latestScore }}</strong>
            <small>/ {{ fullScore }} 分</small>
          </article>
        </section>

        <article v-for="record in records" :key="record.id" class="record"
          @click="router.push({ path: `/mine/papers/results/${record.id}`, query: examQuery })">
          <div class="record__head">
            <div>
              <b>第 {{ record.attemptNo }} 次作答</b>
              <span>{{ record.status === 'AUTO_SUBMITTED' ? '自动交卷' : '已交卷' }}</span>
            </div>
            <strong :class="`tone-${scoreTone(record.objectiveScore)}`">
              {{ record.objectiveScore ?? 0 }}<small>/ {{ record.objectiveTotalScore ?? fullScore }} 分</small>
            </strong>
          </div>
          <dl>
            <div><dt>开始时间</dt><dd>{{ formatDate(record.startedAt) }}</dd></div>
            <div><dt>交卷时间</dt><dd>{{ formatDate(record.submittedAt) }}</dd></div>
            <div><dt>答题用时</dt><dd>{{ duration(record) }}</dd></div>
          </dl>
          <div class="record__actions">
            <button type="button"
              @click.stop="router.push({ path: `/mine/papers/results/${record.id}`, query: examQuery })">查看成绩</button>
            <button class="primary" type="button"
              @click.stop="router.push({ path: `/mine/papers/results/${record.id}/details`, query: examQuery })">查看答题记录</button>
          </div>
        </article>
      </template>
    </main>
  </div>
</template>

<style scoped>
/* 页头与内容使用同一个 1120px 容器，跟「我的试卷」列表页保持一致 */
.history-page {
  min-height: 100vh;
  padding-top: 60px;
  color: var(--hp-ink);
  background: var(--hp-bg);
}

.history-header {
  position: sticky;
  top: 60px;
  z-index: 8;
  display: flex;
  align-items: center;
  gap: 14px;
  width: min(calc(100% - 48px), 1120px);
  margin: 0 auto;
  padding: 20px 0;
  border-bottom: 1px solid var(--hp-line);
  background: var(--hp-bg);
}

.history-back {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex: 0 0 38px;
  width: 38px;
  height: 38px;
  border: 1px solid var(--hp-line-strong);
  border-radius: 999px;
  color: var(--hp-ink);
  background: var(--hp-surface);
  font-size: 22px;
  line-height: 1;
  cursor: pointer;
  transition: border-color 0.18s ease, color 0.18s ease;
}

.history-back:hover {
  border-color: var(--hp-blue-ink);
  color: var(--hp-blue-ink);
}

.history-header__title {
  min-width: 0;
}

.history-header h1 {
  margin: 0;
  color: var(--hp-ink);
  font-size: 23px;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.history-header p {
  margin: 4px 0 0;
  color: var(--hp-muted);
  font-size: 13px;
}

.history-main {
  width: min(calc(100% - 48px), 1120px);
  margin: 0 auto;
  padding: 20px 0 60px;
}

/* 成绩概览：替代原来只显示"累计完成"的单一卡片 */
.stats {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 18px;
}

.stats article {
  display: grid;
  gap: 4px;
  padding: 18px 20px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-md);
  background: var(--hp-surface);
  box-shadow: var(--hp-shadow-sm);
}

.stats span {
  color: var(--hp-muted);
  font-size: 12.5px;
}

.stats strong {
  color: var(--hp-blue-ink);
  font-size: 30px;
  line-height: 1.15;
  font-variant-numeric: tabular-nums;
}

.stats small {
  color: var(--hp-muted);
  font-size: 11.5px;
}

.record {
  margin-bottom: 14px;
  padding: 20px 22px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-lg);
  background: var(--hp-surface);
  box-shadow: var(--hp-shadow-sm);
  cursor: pointer;
  transition: border-color 0.18s ease, transform 0.18s ease;
}

.record:hover {
  border-color: var(--hp-line-strong);
  transform: translateY(-1px);
}

.record__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid var(--hp-line);
}

.record__head > div {
  display: flex;
  align-items: center;
  gap: 10px;
}

.record__head b {
  color: var(--hp-ink);
  font-size: 17px;
}

.record__head span {
  padding: 3px 9px;
  border-radius: 999px;
  color: var(--hp-blue-ink);
  background: var(--hp-blue);
  font-size: 12px;
}

.record__head > strong {
  font-size: 27px;
  font-variant-numeric: tabular-nums;
}

.record__head small {
  margin-left: 3px;
  color: var(--hp-muted);
  font-size: 13px;
  font-weight: 400;
}

/* 得分分档：及格 / 有得分 / 零分 */
.tone-pass {
  color: var(--hp-green-ink);
}

.tone-partial {
  color: var(--hp-blue-ink);
}

.tone-zero {
  color: var(--hp-muted);
}

.record dl {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
  margin: 16px 0;
}

.record dt {
  margin-bottom: 5px;
  color: var(--hp-muted);
  font-size: 12px;
}

.record dd {
  margin: 0;
  color: var(--hp-ink-2);
  font-size: 13px;
}

.record__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.record__actions button {
  height: 34px;
  padding: 0 15px;
  border: 1px solid var(--hp-line-strong);
  border-radius: 999px;
  color: var(--hp-ink);
  background: var(--hp-surface);
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
}

.record__actions button.primary {
  border-color: var(--hp-ink);
  color: #fff;
  background: var(--hp-ink);
}

.state {
  display: grid;
  gap: 9px;
  padding: 70px 20px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-lg);
  color: var(--hp-muted);
  background: var(--hp-surface);
  text-align: center;
}

.state b {
  color: var(--hp-ink);
  font-size: 18px;
}

.state--error {
  color: #a54239;
  background: #faf0ee;
}

@media (max-width: 720px) {
  .history-header,
  .history-main {
    width: calc(100% - 32px);
  }

  .stats {
    grid-template-columns: 1fr;
  }

  .record dl {
    grid-template-columns: 1fr;
  }

  .record__head {
    align-items: flex-start;
  }

  .record__actions button {
    flex: 1;
  }
}
</style>
