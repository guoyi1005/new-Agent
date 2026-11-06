<template>
  <div class="page-layout">
    <Sidebar />
    <main class="main-content">
      <div class="page-container">
        <div class="page-header">
          <div>
            <h1>模拟筛选题目</h1>
            <p>输入关键词，查看向量库匹配到的知识库题目。</p>
          </div>
          <div class="header-stat">
            <span class="stat-label">命中题目</span>
            <span class="stat-value">{{ results.length }}</span>
          </div>
        </div>

        <form class="filter-panel" @submit.prevent="runSearch">
          <label class="field field-query">
            <span class="label">关键词/描述</span>
            <textarea
              v-model="query"
              rows="4"
              maxlength="500"
              placeholder="例如：Redis 缓存穿透怎么解决"
            ></textarea>
          </label>

          <div class="filter-grid">
            <label class="field">
              <span class="label">返回数量</span>
              <input v-model.number="limit" type="number" min="1" max="20" />
            </label>

            <label class="field">
              <span class="label">岗位</span>
              <select v-model="selectedPosition">
                <option value="">全部岗位</option>
                <option v-for="item in positionOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </option>
              </select>
            </label>

            <label class="field">
              <span class="label">题型</span>
              <select v-model="selectedType">
                <option value="">全部题型</option>
                <option v-for="item in typeOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </option>
              </select>
            </label>

            <label class="field">
              <span class="label">状态</span>
              <select v-model="selectedStatus">
                <option value="">全部状态</option>
                <option value="1">启用</option>
                <option value="0">停用</option>
              </select>
            </label>
          </div>

          <div v-if="errorMsg" class="error-banner">{{ errorMsg }}</div>

          <div class="actions-row">
            <button type="button" class="btn-outline" @click="resetFilters">重置</button>
            <button type="submit" class="btn-primary" :disabled="loading || !query.trim()">
              {{ loading ? '匹配中...' : '开始匹配' }}
            </button>
          </div>
        </form>

        <section class="result-panel">
          <div class="result-head">
            <h2>匹配结果</h2>
            <span v-if="lastQuery" class="last-query">“{{ lastQuery }}”</span>
          </div>

          <div v-if="loading" class="state-box">
            <span class="loading-spinner"></span>
            <span>正在查询向量库...</span>
          </div>

          <div v-else-if="hasSearched && results.length === 0" class="state-box">
            当前条件下暂无匹配题目
          </div>

          <div v-else-if="!hasSearched" class="state-box muted">
            输入关键词后开始匹配
          </div>

          <div v-else class="result-list">
            <article v-for="(item, index) in results" :key="item.doc_id || index" class="result-item">
              <div class="result-rank">{{ index + 1 }}</div>
              <div class="result-body">
                <div class="result-top">
                  <div class="title-wrap">
                    <h3 v-if="item.knowledge">{{ item.knowledge.question }}</h3>
                    <h3 v-else>题目已不存在</h3>
                    <div class="meta-row">
                      <span v-if="item.knowledge" class="tag tag-position">{{ item.knowledge.job_position }}</span>
                      <span v-if="item.knowledge" class="tag tag-type">{{ item.knowledge.question_type }}</span>
                      <span v-if="item.knowledge" class="tag tag-difficulty">
                        {{ diffLabel(item.knowledge.difficulty) }}
                      </span>
                      <span v-if="item.knowledge" class="tag" :class="item.knowledge.status === 1 ? 'tag-on' : 'tag-off'">
                        {{ item.knowledge.status === 1 ? '启用' : '停用' }}
                      </span>
                      <span class="tag tag-distance">距离 {{ formatDistance(item.distance) }}</span>
                    </div>
                  </div>
                  <button
                    class="btn-detail"
                    :disabled="!item.knowledge"
                    @click="openDetail(item)"
                  >
                    查看详情
                  </button>
                </div>

                <div v-if="item.knowledge?.keywords" class="keywords">
                  <span class="keywords-label">关键词</span>
                  <span>{{ item.knowledge.keywords }}</span>
                </div>

                <div class="doc-block">
                  <div class="doc-label">向量文档</div>
                  <p>{{ item.document || '无文档内容' }}</p>
                </div>
              </div>
            </article>
          </div>
        </section>
      </div>
    </main>

    <div v-if="showDetail" class="modal-overlay" @click.self="closeDetail">
      <div class="modal-box">
        <div class="modal-header">
          <h2>题目详情</h2>
          <button class="close-btn" @click="closeDetail">x</button>
        </div>
        <div v-if="selectedDetail" class="modal-body">
          <div class="detail-grid">
            <div class="detail-field">
              <span class="detail-label">岗位</span>
              <span>{{ selectedDetail.job_position }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-label">题型</span>
              <span>{{ selectedDetail.question_type }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-label">难度</span>
              <span>{{ diffLabel(selectedDetail.difficulty) }}</span>
            </div>
            <div class="detail-field">
              <span class="detail-label">状态</span>
              <span>{{ selectedDetail.status === 1 ? '启用' : '停用' }}</span>
            </div>
          </div>
          <div class="detail-section">
            <h3>题干</h3>
            <p>{{ selectedDetail.question }}</p>
          </div>
          <div class="detail-section">
            <h3>答题要点</h3>
            <p>{{ selectedDetail.answer_points || '未填写' }}</p>
          </div>
          <div class="detail-section">
            <h3>优秀答案</h3>
            <p>{{ selectedDetail.excellent_answer || '未填写' }}</p>
          </div>
          <div class="detail-section">
            <h3>评分标准</h3>
            <p>{{ selectedDetail.score_standard || '未填写' }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import Sidebar from '../components/Sidebar.vue'
import {
  chromaApi,
  knowledgeApi,
  type ChromaMatchItem,
  type KnowledgeItem,
  type KnowledgePositionItem,
  type KnowledgeTypeItem,
} from '../api/knowledge'

const query = ref('')
const limit = ref(5)
const selectedPosition = ref('')
const selectedType = ref('')
const selectedStatus = ref('')
const positionOptions = ref<KnowledgePositionItem[]>([])
const typeOptions = ref<KnowledgeTypeItem[]>([])
const results = ref<ChromaMatchItem[]>([])
const loading = ref(false)
const hasSearched = ref(false)
const lastQuery = ref('')
const errorMsg = ref('')
const showDetail = ref(false)
const selectedDetail = ref<KnowledgeItem | null>(null)

const normalizedLimit = computed(() => {
  const value = Number(limit.value || 5)
  if (value < 1) return 1
  if (value > 20) return 20
  return value
})

const diffLabel = (difficulty?: number) => {
  const labels: Record<number, string> = {
    1: '入门',
    2: '基础',
    3: '中等',
    4: '进阶',
    5: '大厂真题',
  }
  return labels[Number(difficulty || 0)] || '未知'
}

const formatDistance = (distance?: number | null) => {
  if (distance === null || distance === undefined) return '-'
  const value = Number(distance)
  return Number.isFinite(value) ? value.toFixed(4) : '-'
}

const loadOptions = async () => {
  try {
    const [positions, types] = await Promise.all([
      knowledgeApi.positions(),
      knowledgeApi.types(),
    ])
    positionOptions.value = positions.items || []
    typeOptions.value = types.items || []
  } catch (err) {
    console.error('[MockQuestionFilter] load options failed', err)
  }
}

const runSearch = async () => {
  const text = query.value.trim()
  if (!text) {
    errorMsg.value = '请输入关键词或描述'
    return
  }
  loading.value = true
  errorMsg.value = ''
  hasSearched.value = true
  try {
    const params: {
      q: string
      k: number
      job_position?: string
      question_type?: string
      status?: number
    } = {
      q: text,
      k: normalizedLimit.value,
    }
    if (selectedPosition.value) params.job_position = selectedPosition.value
    if (selectedType.value) params.question_type = selectedType.value
    if (selectedStatus.value !== '') params.status = Number(selectedStatus.value)

    const res = await chromaApi.match(params)
    results.value = res.items || []
    lastQuery.value = text
    limit.value = normalizedLimit.value
  } catch (err: any) {
    results.value = []
    errorMsg.value = err?.response?.data?.message || err?.message || '向量匹配失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const resetFilters = () => {
  query.value = ''
  limit.value = 5
  selectedPosition.value = ''
  selectedType.value = ''
  selectedStatus.value = ''
  results.value = []
  hasSearched.value = false
  lastQuery.value = ''
  errorMsg.value = ''
}

const openDetail = (item: ChromaMatchItem) => {
  if (!item.knowledge) return
  selectedDetail.value = item.knowledge
  showDetail.value = true
}

const closeDetail = () => {
  showDetail.value = false
  selectedDetail.value = null
}

onMounted(() => {
  void loadOptions()
})
</script>

<style scoped>
.page-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
  background: var(--hp-surface);
  color: var(--hp-ink);
}

.main-content {
  flex: 1;
  margin-left: clamp(200px, 25vw, 320px);
  height: 100vh;
  overflow-y: auto;
}

.page-container {
  max-width: 1380px;
  margin: 0 auto;
  padding: 28px clamp(16px, 3vw, 28px) 48px;
}

.page-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 18px;
}

.page-header h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
}

.page-header p {
  margin: 8px 0 0;
  color: #8fa0b8;
  font-size: 14px;
}

.header-stat {
  min-width: 112px;
  padding: 14px 18px;
  border: 1px solid var(--hp-line);
  border-radius: 8px;
  background: var(--hp-surface);
  text-align: right;
}

.stat-label {
  display: block;
  color: #8090a8;
  font-size: 12px;
}

.stat-value {
  display: block;
  margin-top: 4px;
  color: #60a5fa;
  font-size: 26px;
  font-weight: 700;
}

.filter-panel,
.result-panel {
  border: 1px solid var(--hp-line);
  border-radius: 8px;
  background: var(--hp-surface);
  box-shadow: 0 18px 36px rgba(0, 0, 0, 0.18);
}

.filter-panel {
  padding: 18px;
  margin-bottom: 18px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.field-query {
  margin-bottom: 16px;
}

.label {
  color: #9aa8bc;
  font-size: 13px;
  font-weight: 600;
}

textarea,
input,
select {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid var(--hp-line);
  border-radius: 8px;
  background: var(--hp-surface);
  color: var(--hp-ink);
  font-size: 14px;
  outline: none;
}

textarea {
  resize: vertical;
  min-height: 92px;
  padding: 12px 14px;
  line-height: 1.6;
}

input,
select {
  height: 42px;
  padding: 0 12px;
}

textarea:focus,
input:focus,
select:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 3px rgba(59, 130, 246, 0.14);
}

.filter-grid {
  display: grid;
  grid-template-columns: minmax(110px, 0.6fr) repeat(3, minmax(160px, 1fr));
  gap: 14px;
}

.actions-row {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
}

.btn-primary,
.btn-outline,
.btn-detail {
  height: 38px;
  border: none;
  border-radius: 8px;
  padding: 0 18px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
}

.btn-primary {
  background: #3b82f6;
  color: #fff;
}

.btn-primary:disabled,
.btn-detail:disabled {
  cursor: not-allowed;
  opacity: 0.55;
}

.btn-outline {
  border: 1px solid #334766;
  background: transparent;
  color: #b8c5d8;
}

.btn-detail {
  flex: 0 0 auto;
  background: #1d4f86;
  color: #cfe5ff;
}

.error-banner {
  margin-top: 14px;
  padding: 10px 12px;
  border: 1px solid rgba(248, 113, 113, 0.3);
  border-radius: 8px;
  background: rgba(127, 29, 29, 0.24);
  color: #fecaca;
  font-size: 13px;
}

.result-panel {
  padding: 18px;
}

.result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-bottom: 14px;
}

.result-head h2 {
  margin: 0;
  font-size: 18px;
}

.last-query {
  max-width: min(520px, 50vw);
  overflow: hidden;
  color: #8fa0b8;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.state-box {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 180px;
  border: 1px dashed var(--hp-line);
  border-radius: 8px;
  color: #a9b8cc;
}

.state-box.muted {
  color: #77869a;
}

.loading-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(96, 165, 250, 0.25);
  border-top-color: #60a5fa;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.result-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.result-item {
  display: grid;
  grid-template-columns: 42px 1fr;
  gap: 14px;
  padding: 16px;
  border: 1px solid var(--hp-line);
  border-radius: 8px;
  background: var(--hp-surface);
}

.result-rank {
  display: grid;
  width: 34px;
  height: 34px;
  place-items: center;
  border-radius: 8px;
  background: var(--hp-surface);
  color: #93c5fd;
  font-weight: 700;
}

.result-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.title-wrap {
  min-width: 0;
}

.result-item h3 {
  margin: 0;
  color: #eef4ff;
  font-size: 16px;
  line-height: 1.6;
}

.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.tag {
  display: inline-flex;
  align-items: center;
  min-height: 24px;
  padding: 0 9px;
  border-radius: 6px;
  background: var(--hp-surface);
  color: #b8c6da;
  font-size: 12px;
}

.tag-position {
  background: var(--hp-surface);
  color: #93c5fd;
}

.tag-type {
  background: var(--hp-surface);
  color: #6ee7b7;
}

.tag-difficulty {
  background: var(--hp-surface);
  color: var(--hp-ink);
}

.tag-on {
  background: var(--hp-surface);
  color: #6ee7b7;
}

.tag-off {
  background: var(--hp-surface);
  color: var(--hp-ink);
}

.tag-distance {
  background: var(--hp-surface);
  color: #c4b5fd;
}

.keywords {
  display: flex;
  gap: 10px;
  margin-top: 14px;
  color: #b8c6da;
  font-size: 13px;
}

.keywords-label,
.doc-label {
  color: #7f8fa8;
  font-weight: 600;
}

.doc-block {
  margin-top: 14px;
  padding: 12px;
  border-radius: 8px;
  background: var(--hp-surface);
}

.doc-block p {
  display: -webkit-box;
  margin: 8px 0 0;
  overflow: hidden;
  color: #aab8cc;
  font-size: 13px;
  line-height: 1.65;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 4;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(4, 9, 18, 0.72);
}

.modal-box {
  width: min(820px, 96vw);
  max-height: 86vh;
  overflow: hidden;
  border: 1px solid var(--hp-line);
  border-radius: 8px;
  background: var(--hp-surface);
  box-shadow: 0 24px 70px rgba(0, 0, 0, 0.42);
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 18px 20px;
  border-bottom: 1px solid var(--hp-line);
}

.modal-header h2 {
  margin: 0;
  font-size: 18px;
}

.close-btn {
  width: 34px;
  height: 34px;
  border: none;
  border-radius: 8px;
  background: var(--hp-surface);
  color: #cbd7e8;
  cursor: pointer;
}

.modal-body {
  max-height: calc(86vh - 72px);
  overflow-y: auto;
  padding: 20px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
}

.detail-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 12px;
  border-radius: 8px;
  background: var(--hp-surface);
  color: var(--hp-ink);
}

.detail-label {
  color: #8392a8;
  font-size: 12px;
}

.detail-section {
  margin-top: 14px;
  padding: 14px;
  border-radius: 8px;
  background: var(--hp-surface);
}

.detail-section h3 {
  margin: 0 0 8px;
  color: #9fb2cc;
  font-size: 14px;
}

.detail-section p {
  margin: 0;
  color: #d8e2f0;
  line-height: 1.7;
  white-space: pre-wrap;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 920px) {
  .main-content {
    margin-left: 0;
  }

  .page-header,
  .result-top {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-grid,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .result-item {
    grid-template-columns: 1fr;
  }
}
</style>
