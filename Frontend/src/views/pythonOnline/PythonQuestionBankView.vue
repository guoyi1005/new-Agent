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
          <RouterLink class="py-bank-back" to="/learning">
            <span class="py-bank-back__arrow" aria-hidden="true">←</span>
            <span>返回学习实践</span>
          </RouterLink>
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
  width: min(100%, 1280px);
  margin: 0 auto;
  padding: 24px 20px 48px;
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
  font-size: 28px;
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
  background: #eef2f6;
  overflow: hidden;
}

.py-bank-progress__fill {
  height: 100%;
  border-radius: 999px;
  background: #2f76bd;
}

.py-bank-progress__note {
  margin: 10px 0 18px;
  color: #98a2b3;
  font-size: 12px;
  line-height: 1.5;
}

.py-bank-filter + .py-bank-filter {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid #eef2f6;
}

.py-bank-filter h2 {
  margin: 0 0 10px;
  color: #667085;
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
  border: 1px solid #e1e7ed;
  border-radius: 6px;
  color: #344054;
  background: #fff;
  font-size: 13px;
}

.py-bank-chip span {
  color: #98a2b3;
  font-size: 11px;
}

.py-bank-chip--active {
  border-color: #b8d4ef;
  color: #2f76bd;
  background: #eaf4fd;
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
  border: 1px solid #e1e7ed;
  border-radius: 999px;
  color: #667085;
  background: #f8fafc;
  font-size: 12px;
}

.py-bank-tag--active {
  border-color: #b8d4ef;
  color: #2f76bd;
  background: #eaf4fd;
}

.py-bank-clear {
  width: 100%;
  margin-top: 16px;
  padding: 8px;
  border: 1px dashed #d0d5dd;
  border-radius: 6px;
  color: #667085;
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
  border-bottom: 1px solid #eef2f6;
}

.py-bank-search {
  display: flex;
  align-items: center;
  gap: 8px;
  flex: 1;
  max-width: 420px;
  padding: 0 12px;
  border: 1px solid #e1e7ed;
  border-radius: 8px;
  background: #f8fafc;
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
  color: #98a2b3;
  background: #eef2f6;
  font-size: 16px;
  line-height: 1;
}

.py-bank-count {
  color: #98a2b3;
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
  border-bottom: 1px solid #eef2f6;
  color: #667085;
  background: #fafbfc;
  font-size: 12px;
  font-weight: 700;
  text-align: left;
  white-space: nowrap;
}

.py-bank-table td {
  padding: 14px;
  border-bottom: 1px solid #f1f3f6;
  vertical-align: middle;
}

.py-bank-row {
  cursor: pointer;
  transition: background 0.12s;
}

.py-bank-row:hover,
.py-bank-row:focus-visible {
  background: #f8fbff;
  outline: none;
}

.col-status { width: 52px; }
.col-no { width: 56px; color: #667085; font-size: 13px; }
.col-diff { width: 72px; }
.col-rate { width: 80px; color: #667085; font-size: 13px; }
.col-action { width: 100px; text-align: right; }

.py-bank-status {
  display: grid;
  place-items: center;
  width: 22px;
  height: 22px;
  border: 1.5px solid #d0d5dd;
  border-radius: 50%;
  color: transparent;
}

.py-bank-status--done {
  border-color: #12b76a;
  background: #12b76a;
  color: #fff;
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
  color: #667085;
  background: #f2f4f7;
  font-size: 11px;
}

.py-diff {
  display: inline-block;
  padding: 3px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.py-diff--easy { color: #027a48; background: #ecfdf3; }
.py-diff--medium { color: #6941c6; background: #f4f3ff; }
.py-diff--hard { color: #b42318; background: #fef3f2; }

.py-bank-tag-inline {
  display: inline-block;
  margin-right: 4px;
  padding: 2px 7px;
  border-radius: 4px;
  color: #667085;
  background: #f2f4f7;
  font-size: 11px;
}

.py-bank-go {
  padding: 6px 12px;
  border-radius: 6px;
  color: #2f76bd;
  background: #eaf4fd;
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
  color: #667085;
}

.py-bank-empty p,
.py-bank-state p {
  margin: 0 0 14px;
}

/* ── 深色未来感 Dashboard 主题（与星图探索保持一致）── */
.py-bank-page {
  --ink: #edf2ff;
  --muted: #8893aa;
  --line: rgba(148, 163, 184, .16);
  --surface: rgba(16, 20, 31, .78);
  --sky: #72c7ff;
  --mint: #75ddb9;
  --pink: #ef9eb7;
  --sand: #ead27b;
  min-height: 100vh;
  padding: 74px 0 40px;
  color: var(--ink);
  background:
    radial-gradient(circle at 86% 8%, rgba(104, 82, 220, .16), transparent 30%),
    radial-gradient(circle at 8% 32%, rgba(39, 124, 184, .12), transparent 26%),
    linear-gradient(145deg, #060810 0%, #0a0d16 48%, #070911 100%);
}

.py-bank-back {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  width: max-content;
  min-height: 34px;
  margin-bottom: 14px;
  padding: 7px 13px 7px 11px;
  border: 1px solid rgba(80, 180, 255, .35);
  border-radius: 10px;
  color: #c8ddf2;
  background: rgba(10, 20, 40, .75);
  box-shadow: 0 5px 16px rgba(10, 28, 58, .24), 0 0 12px rgba(99, 102, 241, .06);
  font-size: 13px;
  font-weight: 600;
  line-height: 18px;
  text-decoration: none;
  cursor: pointer;
  transition: transform .18s ease, color .18s ease, border-color .18s ease,
    background-color .18s ease, box-shadow .18s ease;
}

.py-bank-back__arrow {
  display: inline-block;
  color: #83c7ff;
  font-size: 15px;
  line-height: 1;
  transition: transform .18s ease;
}

.py-bank-back:hover {
  transform: translateY(-1px);
  border-color: rgba(103, 196, 255, .62);
  color: #dcecff;
  background: rgba(20, 36, 65, .88);
  box-shadow: 0 7px 18px rgba(10, 28, 58, .3), 0 0 15px rgba(88, 134, 255, .16);
}

.py-bank-back:hover .py-bank-back__arrow { transform: translateX(-2px); }

.py-bank-back:active {
  transform: translateY(0);
  background: rgba(8, 17, 34, .88);
  box-shadow: 0 2px 8px rgba(10, 28, 58, .2);
}

.py-bank-back:focus-visible {
  outline: 2px solid rgba(105, 190, 255, .72);
  outline-offset: 3px;
  border-color: rgba(103, 196, 255, .58);
}

.py-bank-shell { width: min(1480px, calc(100% - 48px)); padding: 0; }

.py-bank-header__intro h1 {
  color: #f5f7ff;
  font-size: clamp(28px, 3.2vw, 40px);
  letter-spacing: -.05em;
}

.py-bank-header__intro p { color: #8d98ae; font-size: 13px; }

.py-bank-layout { grid-template-columns: 268px minmax(0, 1fr); gap: 14px; }

.py-bank-sidebar,
.py-bank-main {
  border: 1px solid var(--line);
  border-radius: 24px;
  background: var(--surface);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, .03), 0 18px 48px rgba(0, 0, 0, .16);
  backdrop-filter: blur(12px);
}

.py-bank-progress__head strong { color: #eef2fb; }
.py-bank-progress__head span { color: #7d879e; }
.py-bank-progress__track { background: rgba(141, 153, 186, .17); }
.py-bank-progress__fill {
  background: linear-gradient(90deg, #6d77f4, #78c8f5);
  box-shadow: 0 0 12px rgba(108, 120, 245, .42);
}
.py-bank-progress__note { color: #7d879e; }

.py-bank-filter + .py-bank-filter { border-top-color: rgba(148, 163, 184, .12); }
.py-bank-filter h2 { color: #626e88; }

.py-bank-chip {
  border-color: rgba(148, 163, 184, .14);
  border-radius: 999px;
  color: #aab4c8;
  background: rgba(23, 28, 43, .72);
  transition: border-color .18s ease, color .18s ease, background .18s ease;
}

.py-bank-chip span { color: #6e7890; }
.py-bank-chip:hover { border-color: rgba(124, 137, 255, .42); color: #e6ebf8; }

.py-bank-chip--active {
  border-color: transparent;
  color: #fff;
  background: linear-gradient(135deg, #626bf0, #8177ed);
  box-shadow: 0 7px 20px rgba(88, 93, 220, .32);
}

.py-bank-chip--active span { color: rgba(255, 255, 255, .72); }

.py-bank-tag {
  border-color: rgba(148, 163, 184, .12);
  color: #8d98ae;
  background: rgba(23, 28, 43, .6);
}

.py-bank-tag span { color: #6e7890; }

.py-bank-tag--active {
  border-color: rgba(117, 221, 185, .42);
  color: #a8f0d3;
  background: rgba(52, 120, 102, .24);
}

.py-bank-tag--active span { color: rgba(168, 240, 211, .7); }

.py-bank-clear { border-color: rgba(148, 163, 184, .22); color: #9aa4ba; }
.py-bank-clear:hover { border-color: rgba(124, 137, 255, .5); color: #e9edf9; }

.py-bank-toolbar { border-bottom-color: rgba(148, 163, 184, .12); }

.py-bank-search {
  border-color: rgba(148, 163, 184, .16);
  border-radius: 999px;
  background: rgba(5, 8, 15, .66);
}

.py-bank-search:focus-within { border-color: rgba(124, 137, 255, .72); }
.py-bank-search input { color: #eef2fb; }
.py-bank-search input::placeholder { color: #677188; }
.py-bank-search img { filter: invert(.78) opacity(.55); }
.py-bank-search button { color: #9aa4ba; background: rgba(148, 163, 184, .14); }
.py-bank-count { color: #7d879e; }

.py-bank-table th {
  border-bottom-color: rgba(148, 163, 184, .12);
  color: #78829a;
  background: rgba(9, 12, 20, .72);
}

.py-bank-table td { border-bottom-color: rgba(148, 163, 184, .08); }
.py-bank-row:hover,
.py-bank-row:focus-visible { background: rgba(83, 91, 199, .16); }

.col-no,
.col-rate { color: #8893aa; }

.py-bank-status { border-color: rgba(148, 163, 184, .3); color: transparent; }
.py-bank-status--done {
  border-color: transparent;
  color: #08130f;
  background: var(--mint);
  box-shadow: 0 0 14px rgba(117, 221, 185, .4);
}

.py-bank-title { color: #e7ecf8; }
.py-bank-badge { color: #9aa4ba; background: rgba(148, 163, 184, .12); }

.py-diff--easy { color: #9ef0c4; background: rgba(60, 140, 105, .22); }
.py-diff--medium { color: #c3b6ff; background: rgba(92, 80, 190, .22); }
.py-diff--hard { color: #ffb4b0; background: rgba(170, 70, 64, .2); }

.py-bank-tag-inline { color: #93a0b8; background: rgba(148, 163, 184, .1); }

.py-bank-go {
  border: 1px solid rgba(124, 137, 255, .38);
  border-radius: 999px;
  color: #c9d2ff;
  background: rgba(83, 91, 199, .16);
}

.py-bank-row:hover .py-bank-go,
.py-bank-row:focus-visible .py-bank-go {
  border-color: transparent;
  color: #fff;
  background: linear-gradient(135deg, #626bf0, #8177ed);
}

.py-bank-empty,
.py-bank-state { color: #8893aa; }

.py-bank-state .feature-button--primary,
.py-bank-empty .feature-button--primary {
  border-color: transparent;
  color: #fff;
  background: linear-gradient(135deg, #606af0, #8075e8);
  box-shadow: 0 10px 24px rgba(82, 88, 203, .26);
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
