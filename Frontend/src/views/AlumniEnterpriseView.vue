<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'

import AppTabBar from '../components/AppTabBar.vue'
import { getEmploymentAlumni } from '../api/employment'

/* 校友企业详情页：展示管理端维护的校友企业名单，
 * 企业信息（行业、招聘方向、校友在职人数、开放岗位、简介）都在这里逐条展开。 */

const router = useRouter()

const loading = ref(true)
const keyword = ref('')
const activeField = ref('all')
const enterprises = ref([])

async function loadEnterprises() {
  loading.value = true
  try {
    const result = await getEmploymentAlumni()
    enterprises.value = Array.isArray(result?.data) ? result.data : []
  } catch {
    enterprises.value = []
  } finally {
    loading.value = false
  }
}

onMounted(loadEnterprises)

function markOf(item) {
  return item.shortName || String(item.name || '').replace(/^成都/, '').slice(0, 2) || '企业'
}

function fieldsOf(item) {
  return String(item.fields || '')
    .split(',')
    .map((field) => field.trim())
    .filter(Boolean)
}

const fieldOptions = computed(() => {
  const set = new Set()
  enterprises.value.forEach((item) => fieldsOf(item).forEach((field) => set.add(field)))
  return ['all', ...set]
})

const stats = computed(() => {
  const list = enterprises.value
  return {
    companies: list.length,
    hiring: list.filter((item) => item.hiring).length,
    openPositions: list.reduce((sum, item) => sum + (Number(item.openPositions) || 0), 0),
    alumniAtWork: list.reduce((sum, item) => sum + (Number(item.alumniCount) || 0), 0),
  }
})

const visibleEnterprises = computed(() => {
  const key = keyword.value.trim().toLowerCase()
  return enterprises.value.filter((item) => {
    const fields = fieldsOf(item)
    if (activeField.value !== 'all' && !fields.includes(activeField.value)) return false
    if (!key) return true
    return [item.name, item.shortName, item.industry, fields.join(' '), item.description]
      .filter(Boolean)
      .join(' ')
      .toLowerCase()
      .includes(key)
  })
})

const hasFilters = computed(() => Boolean(keyword.value.trim()) || activeField.value !== 'all')

function clearFilters() {
  keyword.value = ''
  activeField.value = 'all'
}

/** 回到实习就业页，并切到「校友企业」页签查看对应岗位。 */
function openAlumniJobs() {
  router.push({ path: '/employment', query: { tab: 'alumni' } })
}
</script>

<template>
  <div class="feature-page alumni-page">
    <AppTabBar />

    <main class="feature-container alumni-container">
      <header class="alumni-head">
        <div class="alumni-head__copy">
          <h1>校友企业</h1>
          <p>
            共 {{ stats.companies }} 家校友企业 · {{ stats.hiring }} 家正在招聘 ·
            本校校友 {{ stats.alumniAtWork }} 人在职 · 当前开放 {{ stats.openPositions }} 个岗位
          </p>
        </div>
        <div class="alumni-head__actions">
          <button class="feature-button" type="button" @click="router.push('/employment')">
            ← 返回实习就业
          </button>
          <button class="feature-button feature-button--primary" type="button" @click="openAlumniJobs">
            查看校友企业岗位
          </button>
        </div>
      </header>

      <section class="alumni-toolbar">
        <div class="alumni-search">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">
            <circle cx="11" cy="11" r="7" />
            <line x1="21" y1="21" x2="16.65" y2="16.65" />
          </svg>
          <input
            v-model="keyword"
            type="text"
            placeholder="搜索企业名称、行业或招聘方向…"
            aria-label="搜索校友企业"
          />
        </div>
        <div v-if="fieldOptions.length > 1" class="alumni-chips">
          <button
            v-for="field in fieldOptions"
            :key="field"
            type="button"
            :class="['alumni-chip', { active: activeField === field }]"
            @click="activeField = field"
          >
            {{ field === 'all' ? '全部方向' : field }}
          </button>
        </div>
      </section>

      <div v-if="loading" class="alumni-state">
        <span class="alumni-state__spinner" aria-hidden="true"></span>
        <p>正在加载校友企业…</p>
      </div>

      <div v-else-if="!visibleEnterprises.length" class="alumni-state">
        <p class="alumni-state__title">
          {{ hasFilters ? '没有找到匹配的校友企业' : '暂无校友企业信息' }}
        </p>
        <p class="alumni-state__hint">
          {{ hasFilters ? '试试更换招聘方向或换个关键词' : '学校维护的企业名单会在这里展示' }}
        </p>
        <button v-if="hasFilters" class="feature-button" type="button" @click="clearFilters">
          清除筛选条件
        </button>
      </div>

      <section v-else class="alumni-grid">
        <article v-for="item in visibleEnterprises" :key="item.id" class="alumni-card">
          <header class="alumni-card__head">
            <span class="alumni-card__mark" aria-hidden="true">{{ markOf(item) }}</span>
            <div class="alumni-card__title">
              <h3>{{ item.name }}</h3>
              <p v-if="item.industry">{{ item.industry }}</p>
            </div>
            <span :class="['alumni-card__status', item.hiring ? 'is-hiring' : 'is-idle']">
              {{ item.hiring ? '正在招聘' : '暂未招聘' }}
            </span>
          </header>

          <ul class="alumni-card__facts">
            <li>
              <span>本校校友在职</span>
              <strong>{{ item.alumniCount || 0 }} 人</strong>
            </li>
            <li>
              <span>当前开放岗位</span>
              <strong>{{ item.openPositions || 0 }} 个</strong>
            </li>
          </ul>

          <div v-if="fieldsOf(item).length" class="alumni-card__fields">
            <span v-for="field in fieldsOf(item)" :key="field">{{ field }}</span>
          </div>

          <p v-if="item.description" class="alumni-card__desc">{{ item.description }}</p>

          <footer class="alumni-card__foot">
            <span v-if="item.contactName" class="alumni-card__contact">
              联系人 {{ item.contactName }}
              <template v-if="item.contactPhone"> · {{ item.contactPhone }}</template>
            </span>
            <span v-else class="alumni-card__contact is-muted">暂未提供联系人信息</span>
            <button class="feature-link" type="button" @click="openAlumniJobs">查看岗位 →</button>
          </footer>
        </article>
      </section>
    </main>
  </div>
</template>

<style scoped>
.alumni-page {
  padding-bottom: 48px;
}

.alumni-container {
  display: grid;
  gap: var(--hp-gap);
}

.alumni-head {
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

.alumni-head__copy h1 {
  margin: 0 0 8px;
  color: var(--hp-ink);
  font-size: 28px;
  font-weight: 700;
  letter-spacing: -0.02em;
}

.alumni-head__copy p {
  margin: 0;
  color: var(--hp-ink-2);
  font-size: 13.5px;
  line-height: 1.7;
}

.alumni-head__actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.alumni-toolbar {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.alumni-search {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 16px;
  border: 1px solid var(--hp-line);
  border-radius: 999px;
  background: var(--hp-surface);
}

.alumni-search svg {
  width: 16px;
  height: 16px;
  color: var(--hp-muted);
  flex-shrink: 0;
}

.alumni-search input {
  width: 100%;
  border: none;
  background: transparent;
  color: var(--hp-ink);
  font-size: 14px;
  outline: none;
}

.alumni-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.alumni-chip {
  padding: 6px 14px;
  border: 1px solid var(--hp-line);
  border-radius: 999px;
  background: var(--hp-surface);
  color: var(--hp-ink-2);
  cursor: pointer;
  font-size: 13px;
  transition: background 0.15s ease, border-color 0.15s ease, color 0.15s ease;
}

.alumni-chip:hover {
  border-color: var(--hp-line-strong);
}

.alumni-chip.active {
  border-color: var(--hp-ink);
  background: var(--hp-ink);
  color: var(--hp-cream);
}

.alumni-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: var(--hp-gap);
}

.alumni-card {
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

.alumni-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--hp-shadow-md);
}

.alumni-card__head {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.alumni-card__mark {
  display: grid;
  width: 44px;
  height: 44px;
  border-radius: 14px;
  background: var(--hp-green);
  color: var(--hp-ink);
  font-size: 14px;
  font-weight: 700;
  place-items: center;
  flex-shrink: 0;
}

.alumni-card__title {
  min-width: 0;
  flex: 1;
}

.alumni-card__title h3 {
  margin: 0 0 4px;
  color: var(--hp-ink);
  font-size: 16px;
  font-weight: 700;
  line-height: 1.4;
}

.alumni-card__title p {
  margin: 0;
  color: var(--hp-muted);
  font-size: 12.5px;
}

.alumni-card__status {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  white-space: nowrap;
  flex-shrink: 0;
}

.alumni-card__status.is-hiring {
  background: var(--hp-green);
  color: var(--hp-green-ink);
}

.alumni-card__status.is-idle {
  background: var(--hp-surface-2);
  color: var(--hp-muted);
}

.alumni-card__facts {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.alumni-card__facts li {
  display: grid;
  gap: 4px;
  padding: 10px 12px;
  border-radius: var(--hp-r-sm);
  background: var(--hp-surface-2);
}

.alumni-card__facts span {
  color: var(--hp-muted);
  font-size: 11.5px;
}

.alumni-card__facts strong {
  color: var(--hp-ink);
  font-size: 15px;
  font-weight: 700;
}

.alumni-card__fields {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.alumni-card__fields span {
  padding: 4px 12px;
  border-radius: 999px;
  background: var(--hp-blue);
  color: var(--hp-ink);
  font-size: 12.5px;
}

.alumni-card__desc {
  margin: 0;
  color: var(--hp-ink-2);
  font-size: 13px;
  line-height: 1.7;
}

.alumni-card__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: auto;
  padding-top: 12px;
  border-top: 1px dashed var(--hp-line);
}

.alumni-card__contact {
  color: var(--hp-ink-2);
  font-size: 12.5px;
}

.alumni-card__contact.is-muted {
  color: var(--hp-muted);
}

.alumni-state {
  display: grid;
  gap: 8px;
  justify-items: center;
  padding: 56px 20px;
  border: 1px dashed var(--hp-line-strong);
  border-radius: var(--hp-r-md);
  background: var(--hp-surface);
  text-align: center;
}

.alumni-state p {
  margin: 0;
  color: var(--hp-muted);
  font-size: 13.5px;
}

.alumni-state__title {
  color: var(--hp-ink) !important;
  font-size: 15px !important;
  font-weight: 600;
}

.alumni-state__hint {
  font-size: 12.5px !important;
}

.alumni-state__spinner {
  width: 22px;
  height: 22px;
  border: 2px solid var(--hp-line);
  border-top-color: var(--hp-ink);
  border-radius: 50%;
  animation: alumni-spin 0.8s linear infinite;
}

@keyframes alumni-spin {
  to {
    transform: rotate(360deg);
  }
}

@media (max-width: 640px) {
  .alumni-head {
    padding: 20px;
  }

  .alumni-head__copy h1 {
    font-size: 24px;
  }

  .alumni-grid {
    grid-template-columns: minmax(0, 1fr);
  }
}
</style>
