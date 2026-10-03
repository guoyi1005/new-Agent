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
          <h1>校园招聘</h1>
          <p v-if="season">{{ season }} · 宣讲会 {{ talks.length }} 场 · 双选会 {{ fairs.length }} 场 · 校招岗位 {{ roleTotal }} 个</p>
          <p v-else>宣讲会、双选会与校招企业岗位汇总</p>
        </div>
        <div class="campus-head__actions">
          <button class="feature-button" type="button" @click="router.push('/employment')">
            ← 返回实习就业
          </button>
          <button class="feature-button feature-button--primary" type="button" @click="openCampusJobs">
            查看校招岗位
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
        <section class="campus-summary">
          <div class="campus-summary__item">
            <span>宣讲会</span>
            <strong>{{ talks.length }}</strong>
            <em>场</em>
          </div>
          <div class="campus-summary__item">
            <span>双选会</span>
            <strong>{{ fairs.length }}</strong>
            <em>场</em>
          </div>
          <div class="campus-summary__item">
            <span>校招企业</span>
            <strong>{{ companies.length }}</strong>
            <em>家</em>
          </div>
          <div class="campus-summary__item">
            <span>校招岗位</span>
            <strong>{{ roleTotal }}</strong>
            <em>个</em>
          </div>
          <div class="campus-summary__item">
            <span>待进行日程</span>
            <strong>{{ upcoming }}</strong>
            <em>场</em>
          </div>
        </section>

        <section class="campus-block">
          <div class="campus-block__head">
            <h2>宣讲会</h2>
            <span>企业进校宣讲安排</span>
          </div>
          <div v-if="talks.length" class="campus-events">
            <article v-for="item in talks" :key="item.id" class="campus-event">
              <span class="campus-event__date">{{ formatMonthDay(item.eventDate) }}</span>
              <div class="campus-event__copy">
                <h3>{{ item.title }}</h3>
                <p>{{ item.company || '主办单位待定' }}</p>
              </div>
              <span class="campus-event__place">{{ placeOf(item) }}</span>
            </article>
          </div>
          <p v-else class="campus-block__empty">暂无宣讲会安排</p>
        </section>

        <section class="campus-block">
          <div class="campus-block__head">
            <h2>双选会</h2>
            <span>集中双选与专场招聘</span>
          </div>
          <div v-if="fairs.length" class="campus-events">
            <article v-for="item in fairs" :key="item.id" class="campus-event">
              <span class="campus-event__date">{{ formatMonthDay(item.eventDate) }}</span>
              <div class="campus-event__copy">
                <h3>{{ item.title }}</h3>
                <p>{{ item.company || '主办单位待定' }}</p>
              </div>
              <span class="campus-event__place">{{ placeOf(item) }}</span>
            </article>
          </div>
          <p v-else class="campus-block__empty">暂无双选会安排</p>
        </section>

        <section class="campus-block">
          <div class="campus-block__head">
            <h2>校招企业</h2>
            <span>开放校招岗位的企业详情</span>
          </div>
          <div v-if="companies.length" class="campus-companies">
            <article v-for="item in companies" :key="item.id" class="campus-company">
              <header class="campus-company__head">
                <span class="campus-company__mark" aria-hidden="true">{{ markOf(item.company || item.title) }}</span>
                <div class="campus-company__title">
                  <h3>{{ item.company || item.title }}</h3>
                  <p>{{ item.city || '城市待定' }}</p>
                </div>
                <span class="campus-company__roles">{{ item.roleCount || 0 }} 个岗位</span>
              </header>

              <p v-if="item.description" class="campus-company__desc">{{ item.description }}</p>

              <ul class="campus-company__facts">
                <li>
                  <span>项目</span>
                  <strong>{{ item.title }}</strong>
                </li>
                <li>
                  <span>时间</span>
                  <strong>{{ formatFullDate(item.eventDate) }}</strong>
                </li>
                <li>
                  <span>地点</span>
                  <strong>{{ placeOf(item) }}</strong>
                </li>
              </ul>

              <footer class="campus-company__foot">
                <button class="feature-link" type="button" @click="openCampusJobs">查看岗位 →</button>
              </footer>
            </article>
          </div>
          <p v-else class="campus-block__empty">暂无校招企业岗位</p>
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
  gap: var(--hp-gap);
}

.campus-head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 26px 28px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-lg);
  background: var(--hp-surface);
  box-shadow: var(--hp-shadow-sm);
}

.campus-head__copy h1 {
  margin: 0 0 8px;
  color: var(--hp-ink);
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.campus-head__copy p {
  margin: 0;
  color: var(--hp-ink-2);
  font-size: 13.5px;
  line-height: 1.7;
}

.campus-head__actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.campus-summary {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}

.campus-summary__item {
  display: flex;
  align-items: baseline;
  gap: 6px;
  padding: 14px 18px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-md);
  background: var(--hp-surface);
}

.campus-summary__item span {
  flex: 1;
  color: var(--hp-muted);
  font-size: 12.5px;
}

.campus-summary__item strong {
  color: var(--hp-ink);
  font-size: 22px;
  font-weight: 700;
}

.campus-summary__item em {
  color: var(--hp-ink-2);
  font-size: 12px;
  font-style: normal;
}

.campus-block {
  display: grid;
  gap: 14px;
}

.campus-block__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.campus-block__head h2 {
  margin: 0;
  color: var(--hp-ink);
  font-size: 18px;
  font-weight: 700;
}

.campus-block__head span {
  color: var(--hp-muted);
  font-size: 12.5px;
}

.campus-block__empty {
  margin: 0;
  padding: 26px 20px;
  border: 1px dashed var(--hp-line-strong);
  border-radius: var(--hp-r-md);
  background: var(--hp-surface);
  color: var(--hp-muted);
  font-size: 13px;
  text-align: center;
}

.campus-events {
  display: grid;
  gap: 10px;
}

.campus-event {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 14px 18px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-md);
  background: var(--hp-surface);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.campus-event:hover {
  transform: translateY(-2px);
  box-shadow: var(--hp-shadow-sm);
}

.campus-event__date {
  display: grid;
  width: 56px;
  height: 52px;
  border-radius: var(--hp-r-sm);
  background: var(--hp-yellow);
  color: var(--hp-ink);
  font-size: 14px;
  font-weight: 700;
  place-items: center;
  flex-shrink: 0;
}

.campus-event__copy {
  min-width: 0;
  flex: 1;
}

.campus-event__copy h3 {
  margin: 0 0 4px;
  color: var(--hp-ink);
  font-size: 15px;
  font-weight: 600;
}

.campus-event__copy p {
  margin: 0;
  color: var(--hp-muted);
  font-size: 12.5px;
}

.campus-event__place {
  padding: 4px 12px;
  border-radius: 999px;
  background: var(--hp-surface-2);
  color: var(--hp-ink-2);
  font-size: 12.5px;
  white-space: nowrap;
}

.campus-companies {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--hp-gap);
}

.campus-company {
  display: flex;
  flex-direction: column;
  gap: 14px;
  padding: 20px 22px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-md);
  background: var(--hp-surface);
  box-shadow: var(--hp-shadow-sm);
  transition: transform 0.18s ease, box-shadow 0.18s ease;
}

.campus-company:hover {
  transform: translateY(-2px);
  box-shadow: var(--hp-shadow-md);
}

.campus-company__head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
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
  flex: 1;
}

.campus-company__title h3 {
  margin: 0 0 4px;
  color: var(--hp-ink);
  font-size: 16px;
  font-weight: 700;
  line-height: 1.4;
}

.campus-company__title p {
  margin: 0;
  color: var(--hp-muted);
  font-size: 12.5px;
}

.campus-company__roles {
  padding: 4px 10px;
  border-radius: 999px;
  background: var(--hp-green);
  color: var(--hp-green-ink);
  font-size: 12px;
  white-space: nowrap;
  flex-shrink: 0;
}

.campus-company__desc {
  margin: 0;
  color: var(--hp-ink-2);
  font-size: 13px;
  line-height: 1.7;
}

.campus-company__facts {
  display: grid;
  gap: 8px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.campus-company__facts li {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 10px;
  padding: 8px 12px;
  border-radius: var(--hp-r-sm);
  background: var(--hp-surface-2);
}

.campus-company__facts span {
  color: var(--hp-muted);
  font-size: 11.5px;
}

.campus-company__facts strong {
  color: var(--hp-ink);
  font-size: 13px;
  font-weight: 600;
  text-align: right;
}

.campus-company__foot {
  display: flex;
  justify-content: flex-end;
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px dashed var(--hp-line);
}

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

@media (max-width: 640px) {
  .campus-head {
    padding: 20px;
  }

  .campus-head__copy h1 {
    font-size: 24px;
  }

  .campus-event {
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
  }

  .campus-companies {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
