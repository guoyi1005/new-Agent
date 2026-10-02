<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { resolveBossJobSearchLink } from '../api/jobRecommendations'
import AppTabBar from '../components/AppTabBar.vue'
import {
  JOB_PROFILES,
  TARGET_JOB_STORAGE_KEY,
  buildPortraitRows,
  buildSkillGaps,
  getJobDetail,
  getJobMatchRate,
  readStoredTargetJob,
} from '../data/jobCatalog'

/* 岗位详情页四段固定顺序：岗位画像 / 我的岗位能力对照 / 当前核心差距 / 我的提升路径。
 * 内容来自 data/jobCatalog.js；要求-我 的差值、重点补齐、达标数量都是算出来的。 */

const route = useRoute()
const router = useRouter()

const jobId = computed(() => String(route.params.jobId || ''))
const detail = computed(() => getJobDetail(jobId.value))

const targetJobTitle = ref(readStoredTargetJob())
const matchRate = computed(() => (detail.value ? getJobMatchRate(jobId.value, targetJobTitle.value) : null))
const isCurrentTarget = computed(() => Boolean(detail.value) && targetJobTitle.value === detail.value.title)
/* 目标岗位的匹配度与推荐岗位数据只覆盖了其中一部分岗位，没覆盖到的给出提示，避免用户以为页面出错。 */
const targetHasProfile = computed(() => JOB_PROFILES.some((profile) => profile.title === detail.value?.title))

const portraitRows = computed(() => buildPortraitRows(detail.value))
const compareRows = computed(() => buildSkillGaps(detail.value))
const missing = computed(() => compareRows.value.filter((row) => !row.reached))
const reachedCount = computed(() => compareRows.value.length - missing.value.length)

const skillLine = computed(() => (detail.value?.requirements || []).map((item) => item.name).join(' / '))

function clampPercent(value) {
  return Math.max(0, Math.min(100, Math.round(Number(value) || 0)))
}

function setAsTarget() {
  if (!detail.value || isCurrentTarget.value) return
  targetJobTitle.value = detail.value.title
  try {
    localStorage.setItem(TARGET_JOB_STORAGE_KEY, detail.value.title)
  } catch {
    /* 本地存储不可用时仅本次会话生效 */
  }
}

function openExternalSearch() {
  if (!detail.value) return
  window.open(resolveBossJobSearchLink(detail.value.title), '_blank', 'noopener,noreferrer')
}
</script>

<template>
  <div class="feature-page">
    <AppTabBar />

    <main class="feature-container jobdetail">
      <button class="jobdetail-back" type="button" @click="router.push('/career')">← 返回岗位探索</button>

      <template v-if="detail">
        <header class="feature-card jobdetail-hero">
          <div class="jobdetail-hero__top">
            <div>
              <h1>{{ detail.title }}</h1>
              <p class="jobdetail-hero__line">岗位方向 · {{ detail.direction }}</p>
              <p class="jobdetail-hero__line">核心技能 · {{ skillLine }}</p>
            </div>
            <div class="jobdetail-hero__side">
              <strong class="jobdetail-rate">{{ matchRate === null ? '—' : `${matchRate}%` }}</strong>
              <span>{{ compareRows.length }} 项要求</span>
              <span>{{ reachedCount }} 项达标</span>
            </div>
          </div>

          <div class="jobdetail-hero__actions">
            <button
              class="feature-button feature-button--primary"
              type="button"
              :disabled="isCurrentTarget"
              @click="setAsTarget"
            >
              {{ isCurrentTarget ? '已是目标岗位' : '设为目标岗位' }}
            </button>
            <button class="feature-button" type="button" @click="openExternalSearch">查看招聘岗位</button>
          </div>
          <p v-if="isCurrentTarget && !targetHasProfile" class="jobdetail-hero__hint">
            已设为目标岗位。该岗位的匹配度和推荐岗位数据还在补全，岗位探索页会先显示「尚未完成岗位体检」。
          </p>
        </header>

        <div class="jobdetail-steps">
          <!-- 01 岗位画像 -->
          <article class="feature-card jobdetail-step">
            <h2 class="jobdetail-step__title"><span class="jobdetail-step__code">01</span>岗位画像</h2>
            <p class="jobdetail-step__lead">{{ detail.summary }}</p>
            <div class="jobdetail-table">
              <div class="jobdetail-table__row jobdetail-table__row--head">
                <span>做什么</span>
                <span>用什么</span>
                <span>发展方向</span>
              </div>
              <div v-for="row in portraitRows" :key="row.task" class="jobdetail-table__row">
                <span>{{ row.task }}</span>
                <span><i class="jobdetail-tool">{{ row.tool }}</i></span>
                <span>{{ row.direction }}</span>
              </div>
            </div>
          </article>

          <!-- 02 我的岗位能力对照 -->
          <article class="feature-card jobdetail-step">
            <h2 class="jobdetail-step__title"><span class="jobdetail-step__code">02</span>我的岗位能力对照</h2>
            <div class="jobdetail-compare">
              <div class="jobdetail-compare__row jobdetail-compare__row--head">
                <span>技能</span>
                <span>岗位要求</span>
                <span>我的水平</span>
                <span>状态</span>
              </div>
              <div v-for="row in compareRows" :key="row.name" class="jobdetail-compare__row">
                <strong>{{ row.name }}</strong>
                <span>{{ row.required }}</span>
                <span>{{ row.current }}</span>
                <span class="jobdetail-pill" :class="row.reached ? 'jobdetail-pill--ok' : 'jobdetail-pill--gap'">
                  {{ row.reached ? '已达标' : `差 ${row.diff}` }}
                </span>
              </div>
            </div>
          </article>

          <!-- 03 当前核心差距 -->
          <article class="feature-card jobdetail-step">
            <h2 class="jobdetail-step__title"><span class="jobdetail-step__code">03</span>当前核心差距</h2>
            <div class="jobdetail-gap">
              <p class="jobdetail-gap__line">
                <span class="jobdetail-gap__label">重点补齐</span>
                <template v-if="missing.length">
                  <i v-for="row in missing" :key="row.name" class="jobdetail-pill jobdetail-pill--gap">{{ row.name }}</i>
                </template>
                <span v-else class="jobdetail-gap__none">岗位要求的技能你都已经达到</span>
              </p>
              <p class="jobdetail-gap__rate">
                <span>当前匹配度</span>
                <strong>{{ matchRate === null ? '—' : `${matchRate}%` }}</strong>
                <span v-if="matchRate !== null" class="jobdetail-meter" aria-hidden="true">
                  <i :style="{ width: `${clampPercent(matchRate)}%` }"></i>
                </span>
              </p>
            </div>
          </article>

          <!-- 04 我的提升路径 -->
          <article class="feature-card jobdetail-step jobdetail-step--last">
            <h2 class="jobdetail-step__title"><span class="jobdetail-step__code">04</span>我的提升路径</h2>
            <p class="jobdetail-step__lead">{{ detail.advice }}</p>
            <ol class="jobdetail-path">
              <li v-for="(item, index) in detail.path" :key="item.title">
                <span class="jobdetail-path__index">{{ index + 1 }}</span>
                <strong>{{ item.title }}</strong>
                <span class="jobdetail-path__gain">预计 +{{ item.gain }}%</span>
              </li>
            </ol>
            <footer class="jobdetail-step__foot">
              <button
                class="feature-button feature-button--primary"
                type="button"
                @click="router.push('/interview/ai-career-plan')"
              >
                生成我的成长计划
              </button>
            </footer>
          </article>
        </div>
      </template>

      <section v-else class="feature-empty">没有找到这个岗位，返回岗位探索重新选择。</section>
    </main>
  </div>
</template>

<style scoped>
.jobdetail {
  display: block;
}

.jobdetail-back {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  margin-bottom: 16px;
  padding: 0;
  border: 0;
  background: transparent;
  color: var(--hp-muted);
  font-size: 13px;
  cursor: pointer;
}

.jobdetail-back:hover {
  color: var(--hp-ink);
}

/* ---------- 头部 ---------- */

.jobdetail-hero {
  padding: 26px 30px;
  border-radius: var(--hp-r-lg);
}

.jobdetail-hero__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
}

.jobdetail-hero h1 {
  margin: 0;
  color: var(--hp-ink);
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.jobdetail-hero__line {
  margin: 10px 0 0;
  color: var(--hp-muted);
  font-size: 13.5px;
}

.jobdetail-hero__side {
  display: grid;
  flex: 0 0 auto;
  justify-items: end;
  gap: 8px;
  text-align: right;
}

.jobdetail-rate {
  color: var(--hp-ink);
  font-size: 34px;
  font-weight: 700;
  line-height: 1;
  letter-spacing: -0.02em;
}

.jobdetail-hero__side span {
  color: var(--hp-muted);
  font-size: 13px;
}

.jobdetail-hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 22px;
}

.jobdetail-hero__hint {
  margin: 14px 0 0;
  color: var(--hp-muted);
  font-size: 12.5px;
  line-height: 1.7;
}

/* ---------- 四段 ---------- */

.jobdetail-steps {
  display: grid;
  gap: 16px;
  margin-top: 24px;
}

.jobdetail-step {
  position: relative;
  padding: 24px 28px;
  border-radius: var(--hp-r-lg);
}

/* 段与段之间的虚线，保留「01 → 02 → 03 → 04」的推进感 */
.jobdetail-step:not(.jobdetail-step--last)::after {
  position: absolute;
  bottom: -15px;
  left: 43px;
  height: 15px;
  border-left: 1px dashed var(--hp-line-strong);
  content: '';
}

.jobdetail-step__title {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0 0 16px;
  color: var(--hp-ink);
  font-size: 17px;
  font-weight: 600;
}

.jobdetail-step__code {
  display: inline-grid;
  width: 30px;
  height: 30px;
  border: 1px solid var(--hp-line-strong);
  border-radius: 50%;
  background: var(--hp-yellow);
  color: var(--hp-ink);
  font-size: 12px;
  font-weight: 700;
  place-items: center;
}

.jobdetail-step__lead {
  margin: 0 0 16px;
  color: var(--hp-ink-2);
  font-size: 14px;
  line-height: 1.75;
}

.jobdetail-step__foot {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

/* 01 岗位画像 */

.jobdetail-table {
  display: grid;
  overflow: hidden;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-md);
}

.jobdetail-table__row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  align-items: center;
  padding: 13px 16px;
  border-bottom: 1px solid var(--hp-line);
  color: var(--hp-ink);
  font-size: 14px;
}

.jobdetail-table__row:last-child {
  border-bottom: 0;
}

.jobdetail-table__row--head {
  border-bottom: 0;
  background: var(--hp-surface-2);
  color: var(--hp-muted);
  font-size: 12px;
  letter-spacing: 0.04em;
}

.jobdetail-tool {
  display: inline-block;
  padding: 3px 11px;
  border-radius: 999px;
  background: var(--hp-yellow);
  color: var(--hp-ink);
  font-size: 12.5px;
  font-style: normal;
  font-weight: 600;
}

/* 02 能力对照 */

.jobdetail-compare {
  display: grid;
  overflow: hidden;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-md);
}

.jobdetail-compare__row {
  display: grid;
  grid-template-columns: minmax(0, 1.3fr) 0.8fr 0.8fr 0.9fr;
  gap: 12px;
  align-items: center;
  padding: 13px 16px;
  border-bottom: 1px solid var(--hp-line);
}

.jobdetail-compare__row:last-child {
  border-bottom: 0;
}

.jobdetail-compare__row--head {
  border-bottom: 0;
  background: var(--hp-surface-2);
  color: var(--hp-muted);
  font-size: 12px;
  letter-spacing: 0.04em;
}

.jobdetail-compare__row > * {
  color: var(--hp-ink-2);
  font-size: 14px;
}

.jobdetail-compare__row strong {
  color: var(--hp-ink);
  font-weight: 600;
}

/* 03 当前核心差距 */

.jobdetail-gap {
  display: grid;
  gap: 14px;
}

.jobdetail-gap__line {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin: 0;
}

.jobdetail-gap__label,
.jobdetail-gap__rate > span:first-child {
  color: var(--hp-muted);
  font-size: 13px;
}

.jobdetail-gap__none {
  color: var(--hp-ink-2);
  font-size: 13.5px;
}

.jobdetail-gap__rate {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 0;
}

.jobdetail-gap__rate strong {
  color: var(--hp-ink);
  font-size: 20px;
  font-weight: 700;
  line-height: 1;
}

.jobdetail-gap__rate .jobdetail-meter {
  flex: 1 1 160px;
  max-width: 320px;
}

.jobdetail-meter {
  display: block;
  height: 10px;
  border: 1px solid var(--hp-line-strong);
  border-radius: 999px;
  overflow: hidden;
}

.jobdetail-meter i {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: var(--hp-yellow);
}

/* 04 提升路径 */

.jobdetail-path {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.jobdetail-path li {
  display: grid;
  grid-template-columns: 28px minmax(0, 1fr) auto;
  gap: 14px;
  align-items: center;
  padding: 14px 16px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-md);
  background: var(--hp-surface-2);
}

.jobdetail-path__index {
  display: inline-grid;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: var(--hp-yellow);
  color: var(--hp-ink);
  font-size: 12.5px;
  font-weight: 700;
  place-items: center;
}

.jobdetail-path strong {
  color: var(--hp-ink);
  font-size: 14.5px;
  font-weight: 600;
}

.jobdetail-path__gain {
  padding: 3px 11px;
  border-radius: 999px;
  background: var(--hp-green);
  color: var(--hp-ink);
  font-size: 12.5px;
  font-weight: 600;
  white-space: nowrap;
}

/* 状态标签 */

.jobdetail-pill {
  display: inline-block;
  padding: 3px 11px;
  border-radius: 999px;
  color: var(--hp-ink);
  font-size: 11.5px;
  font-style: normal;
  font-weight: 600;
  white-space: nowrap;
}

.jobdetail-pill--ok {
  background: var(--hp-green);
}

.jobdetail-pill--gap {
  background: #eec3cb;
}

@media (max-width: 760px) {
  .jobdetail-hero {
    padding: 22px 20px;
  }

  .jobdetail-hero__top {
    flex-direction: column;
    gap: 16px;
  }

  .jobdetail-hero__side {
    justify-items: start;
    text-align: left;
  }

  .jobdetail-hero h1 {
    font-size: 24px;
  }

  .jobdetail-step {
    padding: 20px;
  }

  .jobdetail-step:not(.jobdetail-step--last)::after {
    left: 35px;
  }

  .jobdetail-table__row,
  .jobdetail-compare__row {
    gap: 10px;
    font-size: 13px;
  }

  .jobdetail-table__row > span,
  .jobdetail-compare__row > * {
    font-size: 13px;
  }
}
</style>
