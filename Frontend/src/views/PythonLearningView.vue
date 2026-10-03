<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  completePathItem,
  getPythonHome,
  replanPythonPath,
  startPathItem,
} from '../api/learning'

const router = useRouter()
const loading = ref(true)
const busy = ref('')
const error = ref('')
const home = ref({})

const mastery = computed(() => Array.isArray(home.value.mastery) ? home.value.mastery : [])
const pathItems = computed(() => home.value.activePath?.items || [])
const recommendations = computed(() => home.value.recommendations || [])
const counts = computed(() => mastery.value.reduce((result, item) => {
  const key = ['mastered', 'weak', 'learning'].includes(item.status) ? item.status : 'other'
  result[key] = (result[key] || 0) + 1
  return result
}, {}))

/* 路径状态中文化：后端返回的是 ready / locked / in_progress 这类英文枚举 */
const STATUS_LABELS = {
  completed: '已完成',
  in_progress: '学习中',
  ready: '可开始',
  locked: '待解锁',
  needs_review: '需复习',
  pending: '待开始',
}
function statusLabel(value) {
  return STATUS_LABELS[value] || '待开始'
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    home.value = await getPythonHome() || {}
  } catch (cause) {
    error.value = cause.message
  } finally {
    loading.value = false
  }
}

async function updateItem(item, action) {
  busy.value = `${action}-${item.id}`
  error.value = ''
  try {
    if (action === 'start') await startPathItem(item.id)
    else await completePathItem(item.id)
    await load()
  } catch (cause) {
    error.value = cause.message
  } finally {
    busy.value = ''
  }
}

async function replan() {
  busy.value = 'replan'
  try {
    await replanPythonPath()
    await load()
  } catch (cause) {
    error.value = cause.message
  } finally {
    busy.value = ''
  }
}

onMounted(load)
</script>

<template>
  <div class="feature-page">
    <main class="feature-container">
      <header class="feature-heading">
        <div>
          <h1>Python 个性化学习</h1>
          <p>根据真实答题与学习记录，按顺序推进下面的环节</p>
        </div>
        <div class="feature-actions">
          <button class="feature-button feature-button--primary" :disabled="busy === 'replan'" @click="replan">重新规划路径</button>
        </div>
      </header>

      <div v-if="error" class="feature-error">{{ error }}</div>
      <div v-if="loading" class="feature-empty">正在加载学习数据…</div>

      <template v-else>
        <section class="py-stats">
          <article><span>已掌握</span><strong>{{ counts.mastered || 0 }}</strong><small>个知识点</small></article>
          <article><span>需巩固</span><strong>{{ counts.weak || 0 }}</strong><small>个知识点</small></article>
          <article><span>学习中</span><strong>{{ counts.learning || 0 }}</strong><small>个知识点</small></article>
          <article><span>画像完整度</span><strong>{{ home.profileCompleteness ?? 0 }}%</strong><small>答题后持续更新</small></article>
        </section>

        <div class="py-plan-grid">
          <section class="feature-card feature-section py-path-panel">
            <div class="feature-section__head">
              <div>
                <h2>当前学习路径</h2>
                <p>{{ home.activePath?.goal || '尚未生成学习目标' }}</p>
              </div>
              <span class="py-count">{{ pathItems.length }} 个环节</span>
            </div>
            <div v-if="!pathItems.length" class="feature-empty">暂无学习路径，可点击右上角「重新规划路径」生成</div>
            <ol v-else class="py-path">
              <li v-for="item in pathItems" :key="item.id">
                <span class="py-path__no">{{ String(item.sequenceNo || 0).padStart(2, '0') }}</span>
                <div class="py-path__body">
                  <div class="py-path__head">
                    <strong>{{ item.knowledgePoint }}</strong>
                    <em :class="`py-status py-status--${item.status || 'pending'}`">{{ statusLabel(item.status) }}</em>
                  </div>
                  <p>{{ item.objective || '按当前掌握度推进这个知识点' }}</p>
                </div>
                <button v-if="item.status !== 'completed'" class="feature-button"
                  :disabled="busy.endsWith(`-${item.id}`)"
                  @click="updateItem(item, item.status === 'in_progress' ? 'complete' : 'start')">
                  {{ item.status === 'in_progress' ? '标记完成' : '开始学习' }}
                </button>
                <span v-else class="py-path__done">已完成</span>
              </li>
            </ol>
          </section>

          <aside class="feature-card feature-section py-rec-panel">
            <div class="feature-section__head">
              <div>
                <h2>学习建议</h2>
                <p>根据近期练习与掌握情况整理</p>
              </div>
            </div>
            <div v-if="!recommendations.length" class="feature-empty">完成练习后会展示基于真实证据的建议</div>
            <div v-else class="py-recs">
              <div v-for="item in recommendations.slice(0, 6)" :key="item.id" class="py-rec">
                <strong>{{ item.title || item.knowledgePoint || '学习建议' }}</strong>
                <small>{{ item.reason || item.rationale || '按当前掌握度推荐' }}</small>
              </div>
            </div>
          </aside>
        </div>
      </template>
    </main>
  </div>
</template>

<style scoped>
/* 外层已有二级导航，收紧顶部留白，与题库/图谱页保持一致 */
.feature-container {
  padding: 12px 0 48px;
}

/* 顶部概览：与站内其它页面的统计条保持一致 */
.py-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  margin-bottom: 20px;
}

.py-stats article {
  display: grid;
  gap: 4px;
  padding: 18px 20px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-md);
  background: var(--hp-surface);
  box-shadow: var(--hp-shadow-sm);
}

.py-stats span {
  color: var(--hp-muted);
  font-size: 12.5px;
}

.py-stats strong {
  color: var(--hp-ink);
  font-size: 26px;
  line-height: 1.2;
  font-variant-numeric: tabular-nums;
}

.py-stats small {
  color: var(--hp-muted);
  font-size: 11.5px;
}

/* 主体两栏：左侧学习路径为主，右侧推荐为辅 */
.py-plan-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.6fr) minmax(300px, 1fr);
  gap: 20px;
  align-items: start;
}

.py-count {
  color: var(--hp-muted);
  font-size: 12.5px;
  white-space: nowrap;
}

.py-path {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.py-path li {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-sm);
  background: var(--hp-surface);
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.py-path li:hover {
  border-color: var(--hp-line-strong);
  box-shadow: var(--hp-shadow-sm);
}

.py-path__no {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  border-radius: 50%;
  color: var(--hp-blue-ink);
  background: var(--hp-blue);
  font-size: 12.5px;
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}

.py-path__body {
  min-width: 0;
}

.py-path__head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
}

.py-path__head strong {
  color: var(--hp-ink);
  font-size: 14.5px;
}

.py-path__body p {
  margin: 5px 0 0;
  color: var(--hp-muted);
  font-size: 12.5px;
  line-height: 1.6;
}

.py-path__done {
  color: var(--hp-green-ink);
  font-size: 12.5px;
  font-weight: 600;
  white-space: nowrap;
}

.py-status {
  padding: 2px 9px;
  border-radius: 999px;
  font-size: 11.5px;
  font-style: normal;
  font-weight: 600;
  white-space: nowrap;
}

.py-status--completed {
  color: var(--hp-green-ink);
  background: var(--hp-green);
}

.py-status--in_progress {
  color: var(--hp-blue-ink);
  background: var(--hp-blue);
}

.py-status--ready {
  color: var(--hp-blue-ink);
  background: var(--hp-tint);
}

.py-status--needs_review {
  color: var(--hp-pink-ink);
  background: var(--hp-pink);
}

.py-status--locked,
.py-status--pending {
  color: var(--hp-muted);
  background: var(--hp-surface-2);
}

.py-recs {
  display: grid;
  gap: 10px;
}

.py-rec {
  display: grid;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-sm);
  color: var(--hp-ink);
  background: var(--hp-surface-2);
}

.py-rec strong {
  font-size: 14px;
}

.py-rec small {
  color: var(--hp-muted);
  font-size: 12.5px;
  line-height: 1.6;
}

.feature-section__head p {
  margin: 5px 0 0;
  color: var(--hp-muted);
  font-size: 13px;
}

@media (max-width: 1000px) {
  .py-plan-grid {
    grid-template-columns: 1fr;
  }

  .py-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .py-stats {
    grid-template-columns: 1fr;
  }

  .py-path li {
    grid-template-columns: 30px minmax(0, 1fr);
  }

  .py-path li > .feature-button,
  .py-path__done {
    grid-column: 2;
    justify-self: start;
  }
}
</style>
