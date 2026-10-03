<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import AppTabBar from '../components/AppTabBar.vue'
import GrowthTreeView from './GrowthTreeView.vue'
import GrowthSkillsView from './GrowthSkillsView.vue'
import GrowthCoursesView from './GrowthCoursesView.vue'
import CampusActivitiesView from './CampusActivitiesView.vue'
import MapView from './MapView.vue'
import ProfileRadarView from './ProfileRadarView.vue'

const route = useRoute()
const tabs = [
  { title: '成长树', path: '/growth/tree', component: GrowthTreeView, note: '沿岗位方向查看已点亮与待点亮的技能点' },
  { title: '技能树', path: '/growth/skills', component: GrowthSkillsView, note: '梳理技能关系、掌握状态与学习依据' },
  { title: '课程管理', path: '/growth/courses', component: GrowthCoursesView, note: '继续课程学习，管理进度与关联试卷' },
  { title: '校园活动', path: '/growth/campus-activity', component: CampusActivitiesView, embedded: true, note: '发现正在发生的校园活动' },
  { title: '校园地图', path: '/growth/campus-map', component: MapView, embedded: true, note: '在校园地图中查找地点' },
  { title: '能力档案', path: '/growth/profile', component: ProfileRadarView, embedded: true, note: '回顾已有学习与使用记录形成的能力画像' },
]
const activeTab = computed(() => tabs.find((tab) => tab.path === route.path) || tabs[0])
</script>

<template>
  <div class="growth-center">
    <AppTabBar />
    <main class="growth-center__main">
      <header class="growth-center__header">
        <div>
          <p class="growth-center__eyebrow">YOUR GROWTH SPACE</p>
          <h1>成长中心</h1>
          <p class="growth-center__intro">从学习记录到职业方向，在这里看见每一步成长。</p>
        </div>
        <span class="growth-center__mark" aria-hidden="true">GROW / 01</span>
      </header>

      <nav class="growth-center__tabs" aria-label="成长中心子导航">
        <RouterLink
          v-for="tab in tabs"
          :key="tab.path"
          :to="tab.path"
          class="growth-center__tab"
          :class="{ 'is-active': activeTab.path === tab.path }"
          :aria-current="activeTab.path === tab.path ? 'page' : undefined"
        >{{ tab.title }}</RouterLink>
      </nav>

      <div class="growth-center__section-label">
        <span class="growth-center__section-rule" aria-hidden="true" />
        <p>{{ activeTab.note }}</p>
      </div>

      <component :is="activeTab.component" :key="activeTab.path" v-bind="activeTab.embedded ? { embedded: true } : {}" />
    </main>
  </div>
</template>

<style scoped>
.growth-center { min-height: 100vh; color: var(--hp-ink); background: var(--hp-bg); }
.growth-center__main { padding: 102px 0 76px; }
.growth-center__header, .growth-center__tabs, .growth-center__section-label { width: min(1360px, calc(100% - 48px)); margin-inline: auto; }
.growth-center__header { display: flex; justify-content: space-between; align-items: end; gap: 24px; padding: 0 0 32px; border-bottom: 1px solid var(--hp-line); }
.growth-center__eyebrow { margin: 0 0 12px; color: var(--hp-green-ink); font: 800 11px/1.3 Inter, sans-serif; letter-spacing: .16em; }
.growth-center__header h1 { margin: 0; font-size: clamp(38px, 4.5vw, 60px); line-height: 1.1; letter-spacing: -.05em; }
.growth-center__intro { margin: 14px 0 0; color: var(--hp-ink-2); font-size: 15px; line-height: 1.7; }
.growth-center__mark { flex: 0 0 auto; padding: 8px 0; color: var(--hp-muted); font: 700 12px/1 Inter, sans-serif; letter-spacing: .14em; }
.growth-center__tabs { display: flex; gap: 4px; margin-top: 20px; padding: 6px; overflow-x: auto; border: 1px solid var(--hp-line); border-radius: 16px; background: var(--hp-surface); scrollbar-width: thin; }
.growth-center__tab { flex: 1 0 auto; padding: 12px 15px; border-radius: 11px; color: var(--hp-ink-2); font-size: 14px; font-weight: 650; text-align: center; text-decoration: none; white-space: nowrap; transition: background .18s ease, color .18s ease; }
.growth-center__tab:hover { background: var(--hp-surface-2); color: var(--hp-ink); }
.growth-center__tab.is-active { color: var(--hp-ink); background: var(--hp-yellow); }
.growth-center__section-label { display: flex; align-items: center; gap: 14px; margin-top: 25px; }
.growth-center__section-rule { width: 28px; height: 2px; background: var(--hp-green-ink); }
.growth-center__section-label p { margin: 0; color: var(--hp-muted); font-size: 13px; }
@media (max-width: 760px) {
  .growth-center__main { padding-top: 88px; }
  .growth-center__header, .growth-center__tabs, .growth-center__section-label { width: calc(100% - 30px); }
  .growth-center__header { padding-bottom: 24px; }
  .growth-center__mark { display: none; }
  .growth-center__tab { flex: 0 0 auto; }
}
@media (prefers-reduced-motion: reduce) { .growth-center__tab { transition: none; } }
</style>
