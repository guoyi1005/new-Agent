<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { usePythonProblemBank } from '../../composables/usePythonProblemBank'

const router = useRouter()

const {
  loading,
  loadError,
  searchKeyword,
  activeDifficulty,
  activeStatus,
  activeTags,
  allTags,
  doneCount,
  totalCount,
  judgeableTotal,
  unjudgeableTotal,
  progressPercent,
  difficultyOptions,
  statusOptions,
  filteredQuestions,
  difficultyLabel,
  toggleTag,
  clearFilters,
  loadProblems,
} = usePythonProblemBank()

function goToPractice(id) {
  router.push(`/career/nebula/python/practice/${id}`)
}

onMounted(loadProblems)
</script>

<template>
  <div class="feature-page py-bank-page">
    <main class="py-bank-shell">
      <header class="py-bank-header">
        <div class="py-bank-header__intro">
<h1>Python 题库</h1>
          <p>在线刷题与编程练习，支持运行、提交与 AI 辅助</p>
        </div>
      </header>

      <div v-if="loading && totalCount === 0" class="py-bank-state">正在加载题库…</div>
      <div v-else-if="loadError && totalCount === 0" class="py-bank-state">
        <p>题库加载失败，请检查网络后重试</p>
        <button class="feature-button feature-button--primary" type="button" @click="loadProblems">重新加载</button>
      </div>

      <div v-else class="py-bank-layout">
        <aside class="py-bank-sidebar feature-card">
          <section class="py-bank-progress">
            <div class="py-bank-progress__head">
              <strong>刷题进度</strong>
              <span>{{ doneCount }}/{{ judgeableTotal }} 已解决</span>
            </div>
            <div class="py-bank-progress__track">
              <div class="py-bank-progress__fill" :style="{ width: progressPercent + '%' }" />
            </div>
            <p class="py-bank-progress__note">
              共 {{ totalCount }} 题，{{ unjudgeableTotal }} 题暂不支持在线判题
            </p>
          </section>

          <section class="py-bank-filter">
            <h2>难度</h2>
            <div class="py-bank-chips">
              <button
                v-for="d in difficultyOptions"
                :key="d.key"
                type="button"
                class="py-bank-chip"
                :class="{ 'py-bank-chip--active': activeDifficulty === d.key }"
                @click="activeDifficulty = d.key"
              >
                {{ d.label }}
                <span>{{ d.count }}</span>
              </button>
            </div>
          </section>

          <section class="py-bank-filter">
            <h2>状态</h2>
            <div class="py-bank-chips">
              <button
                v-for="s in statusOptions"
                :key="s.key"
                type="button"
                class="py-bank-chip"
                :class="{ 'py-bank-chip--active': activeStatus === s.key }"
                @click="activeStatus = s.key"
              >
                {{ s.label }}
                <span>{{ s.count }}</span>
              </button>
            </div>
          </section>

          <section v-if="allTags.length" class="py-bank-filter">
            <h2>标签</h2>
            <div class="py-bank-tags">
              <button
                v-for="t in allTags"
                :key="t.name"
                type="button"
                class="py-bank-tag"
                :class="{ 'py-bank-tag--active': activeTags.includes(t.name) }"
                @click="toggleTag(t.name)"
              >
                {{ t.name }}
                <span>{{ t.count }}</span>
              </button>
            </div>
          </section>

          <button
            v-if="searchKeyword || activeDifficulty !== 'all' || activeStatus !== 'all' || activeTags.length"
            class="py-bank-clear"
            type="button"
            @click="clearFilters"
          >
            清除筛选
          </button>
        </aside>

        <section class="py-bank-main feature-card">
          <div class="py-bank-toolbar">
            <label class="py-bank-search">
              <img src="/icons/search.svg" alt="" />
              <input
                v-model="searchKeyword"
                type="search"
                placeholder="搜索题号、标题或标签…"
              />
              <button v-if="searchKeyword" type="button" aria-label="清除搜索" @click="searchKeyword = ''">×</button>
            </label>
            <button class="py-bank-ai" type="button" @click="router.push('/paper')">AI 出题</button>
            <span class="py-bank-count">共 {{ filteredQuestions.length }} 道</span>
          </div>

          <div class="py-bank-table-wrap">
            <table class="py-bank-table">
              <thead>
                <tr>
                  <th class="col-status" scope="col">状态</th>
                  <th class="col-no" scope="col">题号</th>
                  <th class="col-title" scope="col">题目</th>
                  <th class="col-diff" scope="col">难度</th>
                  <th class="col-rate" scope="col">通过率</th>
                  <th class="col-tags" scope="col">标签</th>
                  <th class="col-action" scope="col" />
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="q in filteredQuestions"
                  :key="q.id"
                  class="py-bank-row"
                  tabindex="0"
                  @click="goToPractice(q.id)"
                  @keydown.enter="goToPractice(q.id)"
                >
                  <td class="col-status">
                    <span class="py-bank-status" :class="{ 'py-bank-status--done': q.done }">
                      <svg v-if="q.done" viewBox="0 0 16 16" aria-hidden="true"><path d="M6.5 11.5L3 8l1-1 2.5 2.5L12 4l1 1z" fill="currentColor"/></svg>
                    </span>
                  </td>
                  <td class="col-no">{{ q.number }}</td>
                  <td class="col-title">
                    <span class="py-bank-title">{{ q.title }}</span>
                    <span v-if="!q.judgeable" class="py-bank-badge">仅练习</span>
                    <span v-if="q.skills && q.skills.length" class="py-bank-skill-row">
                      <span v-for="s in q.skills.slice(0, 2)" :key="s.code" class="py-bank-skill">{{ s.name }}</span>
                    </span>
                  </td>
                  <td class="col-diff">
                    <span class="py-diff" :class="'py-diff--' + q.difficulty">{{ difficultyLabel(q.difficulty) }}</span>
                  </td>
                  <td class="col-rate">{{ q.passRate }}%</td>
                  <td class="col-tags">
                    <span v-for="t in q.tags.slice(0, 3)" :key="t" class="py-bank-tag-inline">{{ t }}</span>
                  </td>
                  <td class="col-action">
                    <button type="button" class="py-bank-go" @click.stop="goToPractice(q.id)">开始答题</button>
                  </td>
                </tr>
              </tbody>
            </table>

            <div v-if="!filteredQuestions.length" class="py-bank-empty">
              <p>未找到匹配的题目</p>
              <button type="button" class="feature-button" @click="clearFilters">重置筛选</button>
            </div>
          </div>
        </section>
      </div>
    </main>
  </div>
</template>

<style scoped>
.py-bank-shell {
  width: min(1400px, calc(100% - 48px));
  margin: 0 auto;
  padding: 12px 0 48px;
}

.py-bank-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 22px;
}

  .py-bank-header__intro h1 {
    margin: 0;
    color: var(--hp-ink);
  font-size: 26px;
}

  .py-bank-header__intro p {
    margin: 7px 0 0;
    color: var(--hp-muted);
  font-size: 14px;
}

.py-bank-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.py-bank-sidebar {
  padding: 18px;
  position: sticky;
  top: 76px;
}

.py-bank-progress__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 10px;
}

  .py-bank-progress__head strong {
    color: var(--hp-ink);
  font-size: 14px;
}

  .py-bank-progress__head span {
    color: var(--hp-muted);
  font-size: 12px;
}

.py-bank-progress__track {
  height: 6px;
  border-radius: 999px;
  background: var(--hp-surface-2);
  overflow: hidden;
}

.py-bank-progress__fill {
  height: 100%;
  border-radius: 999px;
  background: var(--hp-blue-ink);
}

.py-bank-progress__note {
  margin: 10px 0 18px;
  color: var(--hp-muted);
  font-size: 12px;
  line-height: 1.5;
}

.py-bank-filter + .py-bank-filter {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--hp-surface-2);
}

.py-bank-filter h2 {
  margin: 0 0 10px;
  color: var(--hp-muted);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.04em;
  text-transform: uppercase;
}

.py-bank-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.py-bank-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 10px;
  border: 1px solid var(--hp-line);
  border-radius: 6px;
  color: var(--hp-ink-2);
  background: var(--hp-surface);
  font-size: 13px;
}

.py-bank-chip span {
  color: var(--hp-muted);
  font-size: 11px;
}

.py-bank-chip--active {
  border-color: var(--hp-line-strong);
  color: var(--hp-blue-ink);
  background: var(--hp-blue);
}

.py-bank-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.py-bank-tag {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 5px 9px;
  border: 1px solid var(--hp-line);
  border-radius: 999px;
  color: var(--hp-muted);
  background: var(--hp-surface-2);
  font-size: 12px;
}

.py-bank-tag--active {
  border-color: var(--hp-line-strong);
  color: var(--hp-blue-ink);
  background: var(--hp-blue);
}

.py-bank-clear {
  width: 100%;
  margin-top: 16px;
  padding: 8px;
  border: 1px dashed var(--hp-line-strong);
  border-radius: 6px;
  color: var(--hp-muted);
  background: transparent;
  font-size: 13px;
}

.py-bank-main {
  overflow: hidden;
}

.py-bank-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 16px 18px;
  border-bottom: 1px solid var(--hp-surface-2);
}

.py-bank-search {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  max-width: 420px;
  padding: 0 12px;
  border: 1px solid var(--hp-line);
  border-radius: 8px;
  background: var(--hp-surface-2);
}

.py-bank-search img {
  width: 16px;
  height: 16px;
  opacity: 0.45;
}

.py-bank-search input {
  flex: 1;
  min-width: 0;
  height: 38px;
    border: 0;
    outline: none;
    background: transparent;
    color: var(--hp-ink);
  font-size: 14px;
}

.py-bank-search button {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  color: var(--hp-muted);
  background: var(--hp-surface-2);
  font-size: 16px;
  line-height: 1;
}

.py-bank-ai {
  min-height: 34px;
  padding: 0 12px;
  border: 1px solid var(--hp-line-strong);
  border-radius: 999px;
  color: var(--hp-ink-2);
  background: var(--hp-surface-2);
  font-size: 12px;
  font-weight: 700;
  cursor: pointer;
}

.py-bank-count {
  color: var(--hp-muted);
  font-size: 13px;
  white-space: nowrap;
}

.py-bank-table-wrap {
  overflow-x: auto;
}

.py-bank-table {
  width: 100%;
  border-collapse: collapse;
}

.py-bank-table th {
  padding: 12px 14px;
  border-bottom: 1px solid var(--hp-surface-2);
  color: var(--hp-muted);
  background: var(--hp-surface-2);
  font-size: 12px;
  font-weight: 700;
  text-align: left;
  white-space: nowrap;
}

.py-bank-table td {
  padding: 14px;
  border-bottom: 1px solid var(--hp-surface-2);
  vertical-align: middle;
}

.py-bank-row {
  cursor: pointer;
  transition: background 0.12s;
}

.py-bank-row:hover,
.py-bank-row:focus-visible {
  background: var(--hp-tint);
  outline: none;
}

.col-status { width: 52px; }
.col-no { width: 56px; color: var(--hp-muted); font-size: 13px; }
.col-diff { width: 72px; }
.col-rate { width: 80px; color: var(--hp-muted); font-size: 13px; }
.col-action { width: 100px; text-align: right; }

.py-bank-status {
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  border: 1.5px solid var(--hp-line-strong);
  border-radius: 50%;
  color: transparent;
}

.py-bank-status--done {
  border-color: var(--hp-green-ink);
  background: var(--hp-green-ink);
  color: var(--hp-surface);
}

.py-bank-status svg {
  width: 14px;
  height: 14px;
}

  .py-bank-title {
    color: var(--hp-ink);
  font-size: 14px;
  font-weight: 600;
}

.py-bank-badge {
  margin-left: 8px;
  padding: 2px 6px;
  border-radius: 4px;
  color: var(--hp-muted);
  background: var(--hp-surface-2);
  font-size: 11px;
}

.py-diff {
  display: inline-block;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.py-diff--easy { color: var(--hp-green-ink); background: var(--hp-green); }
.py-diff--medium { color: var(--hp-pink-ink); background: var(--hp-pink); }
.py-diff--hard { color: var(--hp-pink-ink); background: var(--hp-pink); }

.py-bank-tag-inline {
  display: inline-block;
  margin-right: 4px;
  padding: 2px 7px;
  border-radius: 4px;
  color: var(--hp-muted);
  background: var(--hp-surface-2);
  font-size: 11px;
}

.py-bank-skill-row {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 4px;
  margin-left: 8px;
  vertical-align: middle;
}

.py-bank-skill {
  padding: 2px 7px;
  border: 1px solid var(--hp-line);
  border-radius: 999px;
  color: var(--hp-blue-ink);
  background: var(--hp-blue);
  font-size: 11px;
}

.py-bank-go {
  padding: 6px 12px;
  border-radius: 6px;
  color: var(--hp-blue-ink);
  background: var(--hp-blue);
  font-size: 13px;
  font-weight: 600;
  opacity: 0;
  transition: opacity 0.12s;
}

.py-bank-row:hover .py-bank-go,
.py-bank-row:focus-visible .py-bank-go {
  opacity: 1;
}

.py-bank-empty,
.py-bank-state {
  padding: 48px 24px;
  text-align: center;
  color: var(--hp-muted);
}

.py-bank-empty p,
.py-bank-state p {
  margin: 0 0 14px;
}

/* 返回学习实践：与站内其他页面的返回按钮保持一致的胶囊样式 */
.py-bank-back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 14px;
  padding: 7px 14px;
  border: 1px solid var(--hp-line-strong);
  border-radius: 999px;
  color: var(--hp-ink-2);
  background: var(--hp-surface);
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  transition: border-color .2s ease, color .2s ease, background .2s ease;
}

.py-bank-back:hover {
  border-color: var(--hp-blue-ink);
  color: var(--hp-blue-ink);
  background: var(--hp-tint);
}

.py-bank-back__arrow {
  font-size: 13px;
  line-height: 1;
  transition: transform .2s ease;
}

.py-bank-back:hover .py-bank-back__arrow {
  transform: translateX(-2px);
}
@media (max-width: 960px) {
  .py-bank-header {
    flex-direction: column;
  }

  .py-bank-layout {
    grid-template-columns: 1fr;
  }

  .py-bank-sidebar {
    position: static;
  }

  .py-bank-go {
    opacity: 1;
  }

  .col-tags {
    display: none;
  }
}

@media (prefers-reduced-motion: reduce) {
  .py-bank-back,
  .py-bank-back__arrow {
    transition: none;
  }
}
</style>
