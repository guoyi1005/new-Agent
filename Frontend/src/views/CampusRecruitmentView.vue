<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import AppTabBar from '../components/AppTabBar.vue'
import { getCampusRecruitments } from '../api/employment'

/* 校园招聘详情页：把管理端维护的宣讲会、双选会与校招企业岗位分开陈列。
 * 数据全部来自 /api/app/employment/campus-recruitments，不在页面里写死。 */

const router = useRouter()

const loading = ref(true)
const items = ref([])

async function loadRecruitments() {
  loading.value = true
  try {
    const result = await getCampusRecruitments()
    items.value = Array.isArray(result?.data) ? result.data : []
  } catch {
    items.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadRecruitments)

const talks = computed(() => items.value.filter((item) => item.type === 'TALK'))
const fairs = computed(() => items.value.filter((item) => item.type === 'FAIR'))
const companies = computed(() => items.value.filter((item) => item.type === 'JOB'))

const season = computed(() => items.value.find((item) => item.season)?.season || '')

const roleTotal = computed(() =>
  items.value.reduce((sum, item) => sum + (Number(item.roleCount) || 0), 0))

const upcoming = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return items.value.filter((item) => item.eventDate && item.eventDate >= today).length
})

const stats = computed(() => ([
  { label: '宣讲会', value: talks.value.length, unit: '场' },
  { label: '双选会', value: fairs.value.length, unit: '场' },
  { label: '校招企业', value: companies.value.length, unit: '家' },
  { label: '校招岗位', value: roleTotal.value, unit: '个' },
  { label: '待进行日程', value: upcoming.value, unit: '场' },
]))

function formatMonthDay(value) {
  if (!value) return '待定'
  const parts = String(value).slice(0, 10).split('-')
  if (parts.length < 3) return String(value)
  return `${Number(parts[1])}.${Number(parts[2])}`
}

function formatFullDate(value) {
  if (!value) return '时间待定'
  return String(value).slice(0, 10)
}

function markOf(name) {
  return String(name || '').replace(/^成都/, '').slice(0, 2) || '校招'
}

function placeOf(item) {
  return item.location || item.city || '地点待定'
}

/** 回到实习就业页并切到「校招」页签。 */
function openCampusJobs() {
  router.push({ path: '/employment', query: { tab: 'campus' } })
}
</script>

<template>
  <div class="feature-page campus-page">
    <AppTabBar />

    <main class="feature-container campus-container">
      <header class="campus-head">
        <div class="campus-head__copy">
          <p v-if="season" class="campus-head__eyebrow">{{ season }}</p>
          <h1>校园招聘</h1>
          <p class="campus-head__desc">宣讲会、双选会与校招企业岗位汇总</p>
        </div>
        <div class="campus-head__actions">
          <button class="feature-button" type="button" @click="router.push('/employment')">
            ← 返回实习就业
          </button>
        </div>
      </header>

      <div v-if="loading" class="campus-state">
        <span class="campus-state__spinner" aria-hidden="true"></span>
        <p>正在加载校园招聘信息…</p>
      </div>

      <div v-else-if="!items.length" class="campus-state">
        <p class="campus-state__title">暂无校园招聘信息</p>
        <p class="campus-state__hint">学校发布的宣讲会、双选会和校招岗位会在这里展示</p>
      </div>

      <template v-else>
        <section class="campus-stats">
          <div v-for="item in stats" :key="item.label" class="campus-stat">
            <span>{{ item.label }}</span>
            <p>
              <strong>{{ item.value }}</strong>
              <em>{{ item.unit }}</em>
            </p>
          </div>
        </section>

        <div class="campus-boards">
          <section class="campus-board">
            <header class="campus-board__head">
              <h2>宣讲会</h2>
              <span>{{ talks.length }} 场</span>
            </header>
            <ul v-if="talks.length" class="campus-board__list">
              <li v-for="item in talks" :key="item.id" class="campus-event">
                <span class="campus-event__date">{{ formatMonthDay(item.eventDate) }}</span>
                <div class="campus-event__copy">
                  <h3>{{ item.title }}</h3>
                  <p>{{ item.company || '主办单位待定' }}</p>
                </div>
                <span class="campus-event__place">{{ placeOf(item) }}</span>
              </li>
            </ul>
            <p v-else class="campus-board__empty">暂无宣讲会安排</p>
          </section>

          <section class="campus-board">
            <header class="campus-board__head">
              <h2>双选会</h2>
              <span>{{ fairs.length }} 场</span>
            </header>
            <ul v-if="fairs.length" class="campus-board__list">
              <li v-for="item in fairs" :key="item.id" class="campus-event">
                <span class="campus-event__date">{{ formatMonthDay(item.eventDate) }}</span>
                <div class="campus-event__copy">
                  <h3>{{ item.title }}</h3>
                  <p>{{ item.company || '主办单位待定' }}</p>
                </div>
                <span class="campus-event__place">{{ placeOf(item) }}</span>
              </li>
            </ul>
            <p v-else class="campus-board__empty">暂无双选会安排</p>
          </section>
        </div>

        <section class="campus-company-block">
          <header class="campus-company-block__head">
            <h2>校招企业</h2>
            <span v-if="companies.length">{{ companies.length }} 家企业 · {{ roleTotal }} 个岗位</span>
            <span v-else>开放校招岗位的企业详情</span>
          </header>

          <ul v-if="companies.length" class="campus-company-list">
            <li v-for="item in companies" :key="item.id" class="campus-company">
              <div class="campus-company__id">
                <span class="campus-company__mark" aria-hidden="true">{{ markOf(item.company || item.title) }}</span>
                <div class="campus-company__title">
                  <h3>{{ item.company || item.title }}</h3>
                  <p>{{ [item.city, item.title].filter(Boolean).join(' · ') || '企业信息待补充' }}</p>
                </div>
              </div>

              <dl class="campus-company__facts">
                <div>
                  <dt>项目</dt>
                  <dd>{{ item.title || '待定' }}</dd>
                </div>
                <div>
                  <dt>时间</dt>
                  <dd>{{ formatFullDate(item.eventDate) }}</dd>
                </div>
                <div>
                  <dt>地点</dt>
                  <dd>{{ placeOf(item) }}</dd>
                </div>
                <div>
                  <dt>开放岗位</dt>
                  <dd>{{ item.roleCount || 0 }} 个</dd>
                </div>
              </dl>

              <p v-if="item.description" class="campus-company__desc">{{ item.description }}</p>

              <button class="feature-button campus-company__cta" type="button" @click="openCampusJobs">
                查看岗位 →
              </button>
            </li>
          </ul>
          <p v-else class="campus-company-block__empty">暂无校招企业岗位</p>
        </section>
      </template>
    </main>
  </div>
</template>

<style scoped>
.campus-page {
  padding-bottom: 48px;
}

.campus-container {
  display: grid;
  gap: 18px;
}

/* ---------- 页头 ---------- */

.campus-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 20px 24px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-lg);
  background: var(--hp-surface);
  box-shadow: var(--hp-shadow-sm);
}

.campus-head__eyebrow {
  margin: 0 0 6px;
  color: var(--hp-blue-ink);
  font-size: 12.5px;
  font-weight: 600;
  letter-spacing: 0.04em;
}

.campus-head__copy h1 {
  margin: 0;
  color: var(--hp-ink);
  font-size: 26px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.campus-head__desc {
  margin: 6px 0 0;
  color: var(--hp-ink-2);
  font-size: 13px;
}

.campus-head__actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

/* ---------- 数据条 ---------- */

.campus-stats {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 12px;
}

.campus-stat {
  display: grid;
  gap: 6px;
  padding: 14px 18px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-md);
  background: var(--hp-surface);
}

.campus-stat span {
  color: var(--hp-muted);
  font-size: 12.5px;
}

.campus-stat p {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin: 0;
}

.campus-stat strong {
  color: var(--hp-ink);
  font-size: 24px;
  font-weight: 700;
  line-height: 1;
}

.campus-stat em {
  color: var(--hp-ink-2);
  font-size: 12px;
  font-style: normal;
}

/* ---------- 宣讲会 / 双选会 双栏 ---------- */

.campus-boards {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
  align-items: start;
}

.campus-board {
  display: grid;
  gap: 12px;
  padding: 18px 20px 20px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-lg);
  background: var(--hp-surface);
}

.campus-board__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.campus-board__head h2 {
  margin: 0;
  color: var(--hp-ink);
  font-size: 17px;
  font-weight: 700;
}

.campus-board__head span {
  color: var(--hp-muted);
  font-size: 12.5px;
}

.campus-board__list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.campus-event {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border-radius: var(--hp-r-md);
  background: var(--hp-surface-2);
}

.campus-event__date {
  display: grid;
  width: 50px;
  height: 46px;
  border-radius: var(--hp-r-sm);
  background: var(--hp-yellow);
  color: var(--hp-ink);
  font-size: 13.5px;
  font-weight: 700;
  place-items: center;
  flex-shrink: 0;
}

.campus-event__copy {
  min-width: 0;
  flex: 1;
}

.campus-event__copy h3 {
  margin: 0 0 3px;
  color: var(--hp-ink);
  font-size: 14.5px;
  font-weight: 600;
}

.campus-event__copy p {
  margin: 0;
  overflow: hidden;
  color: var(--hp-muted);
  font-size: 12.5px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.campus-event__place {
  padding: 3px 10px;
  border-radius: 999px;
  background: var(--hp-surface);
  color: var(--hp-ink-2);
  font-size: 12px;
  white-space: nowrap;
  flex-shrink: 0;
}

.campus-board__empty,
.campus-company-block__empty {
  margin: 0;
  padding: 22px 16px;
  border: 1px dashed var(--hp-line-strong);
  border-radius: var(--hp-r-md);
  color: var(--hp-muted);
  font-size: 13px;
  text-align: center;
}

/* ---------- 校招企业 ---------- */

.campus-company-block {
  display: grid;
  gap: 12px;
  padding: 18px 20px 20px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-lg);
  background: var(--hp-surface);
}

.campus-company-block__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.campus-company-block__head h2 {
  margin: 0;
  color: var(--hp-ink);
  font-size: 17px;
  font-weight: 700;
}

.campus-company-block__head span {
  color: var(--hp-muted);
  font-size: 12.5px;
}

.campus-company-list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.campus-company {
  display: grid;
  grid-template-columns: minmax(0, 1.1fr) minmax(0, 1.8fr) auto;
  align-items: center;
  gap: 16px;
  padding: 14px 16px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-md);
  background: var(--hp-surface-2);
  transition: border-color 0.18s ease, box-shadow 0.18s ease;
}

.campus-company:hover {
  border-color: var(--hp-line-strong);
  box-shadow: var(--hp-shadow-sm);
}

.campus-company__id {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.campus-company__mark {
  display: grid;
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: var(--hp-blue);
  color: var(--hp-ink);
  font-size: 14px;
  font-weight: 700;
  place-items: center;
  flex-shrink: 0;
}

.campus-company__title {
  min-width: 0;
}

.campus-company__title h3 {
  margin: 0 0 4px;
  color: var(--hp-ink);
  font-size: 15px;
  font-weight: 700;
  line-height: 1.4;
}

.campus-company__title p {
  margin: 0;
  color: var(--hp-muted);
  font-size: 12.5px;
}

.campus-company__facts {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin: 0;
}

.campus-company__facts div {
  display: grid;
  gap: 3px;
  min-width: 0;
}

.campus-company__facts dt {
  color: var(--hp-muted);
  font-size: 11.5px;
}

.campus-company__facts dd {
  margin: 0;
  overflow: hidden;
  color: var(--hp-ink);
  font-size: 13px;
  font-weight: 600;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.campus-company__desc {
  grid-column: 1 / -1;
  margin: 0;
  color: var(--hp-ink-2);
  font-size: 13px;
  line-height: 1.7;
}

.campus-company__cta {
  white-space: nowrap;
}

/* ---------- 加载 / 空状态 ---------- */

.campus-state {
  display: grid;
  gap: 8px;
  justify-items: center;
  padding: 56px 20px;
  border: 1px dashed var(--hp-line-strong);
  border-radius: var(--hp-r-md);
  background: var(--hp-surface);
  text-align: center;
}

.campus-state p {
  margin: 0;
  color: var(--hp-muted);
  font-size: 13.5px;
}

.campus-state__title {
  color: var(--hp-ink) !important;
  font-size: 15px !important;
  font-weight: 600;
}

.campus-state__hint {
  font-size: 12.5px !important;
}

.campus-state__spinner {
  width: 22px;
  height: 22px;
  border: 2px solid var(--hp-line);
  border-top-color: var(--hp-ink);
  border-radius: 50%;
  animation: campus-spin 0.8s linear infinite;
}

@keyframes campus-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 1080px) {
  .campus-stats {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .campus-company {
    grid-template-columns: minmax(0, 1fr) auto;
  }

  .campus-company__facts {
    grid-column: 1 / -1;
  }
}

@media (max-width: 860px) {
  .campus-boards {
    grid-template-columns: minmax(0, 1fr);
  }
}

@media (max-width: 640px) {
  .campus-head {
    padding: 18px 20px;
  }

  .campus-head__copy h1 {
    font-size: 22px;
  }

  .campus-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .campus-event {
    align-items: flex-start;
    flex-wrap: wrap;
  }

  .campus-company {
    grid-template-columns: minmax(0, 1fr);
  }

  .campus-company__facts {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
