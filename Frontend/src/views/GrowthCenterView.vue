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
          <p class="growth-center__eyebrow">成长手札</p>
          <h1>成长中心</h1>
          <p class="growth-center__intro">从学习记录到职业方向，在这里看见每一步成长。</p>
        </div>
        <svg class="growth-center__botanical" viewBox="0 0 240 250" fill="none" stroke="#8b7355" aria-hidden="true">
          <path d="M47 227c25-53 43-94 74-142m-55 116c32-11 63-19 105-19M95 128c-12-30-16-48-12-72m21 45c33-7 51-18 67-42" />
          <path fill="#d4a0a0" d="M68 195c-30-17-39-30-31-44 19 1 31 13 31 44Zm16-28c9-35 20-48 40-48 0 25-12 41-40 48Zm30-67c-20-17-27-32-20-47 20 7 26 23 20 47Zm21-13c3-29 15-42 35-45 0 22-12 37-35 45Zm25 95c15-27 32-37 51-32-9 22-27 33-51 32Z" />
          <circle fill="#f5d75f" cx="176" cy="39" r="13" /><circle fill="#f5d75f" cx="174" cy="39" r="4" /><circle fill="#f5d75f" cx="35" cy="145" r="9" /><circle fill="#f5d75f" cx="35" cy="145" r="3" />
        </svg>
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
.growth-center { min-height: 100vh; color: #47382e; background: repeating-linear-gradient(95deg, rgba(139, 115, 85, .018) 0 1px, transparent 1px 7px), #faf6f0; font-family: Georgia, 'Noto Serif SC', 'Songti SC', 'SimSun', serif; }
.growth-center__main { --hp-ink: #47382e; --hp-ink-2: #594a3c; --hp-muted: #655449; --hp-line: #dacabe; --hp-line-strong: #baa89a; --hp-surface: #fffaf3; --hp-surface-2: #f7eee4; --hp-green-ink: #4d714b; --hp-blue-ink: #70504a; --hp-yellow-ink: #77552b; --hp-green: #e0ebdb; --hp-blue: #f2e3e2; --hp-yellow: #f5d75f; padding: 74px 0 76px; }
.growth-center__header, .growth-center__tabs, .growth-center__section-label { width: min(1360px, calc(100% - 48px)); margin-inline: auto; }
.growth-center__header { position: relative; display: flex; justify-content: space-between; align-items: end; gap: 24px; min-height: 0; padding: 27px clamp(28px, 4vw, 52px); overflow: hidden; border: 1px solid rgba(212, 160, 160, .48); border-radius: 26px 26px 48px 26px; background: radial-gradient(ellipse at 91% 12%, rgba(212, 160, 160, .32), transparent 41%), #f4e8da; box-shadow: 0 12px 28px rgba(105, 75, 51, .09); }
.growth-center__header::after { content: ''; position: absolute; right: 12%; bottom: -90px; width: 260px; height: 190px; border: 1px solid rgba(139, 115, 85, .15); border-radius: 50%; pointer-events: none; }
.growth-center__header > :not(.growth-center__botanical) { position: relative; z-index: 1; }
.growth-center__botanical { position: absolute; z-index: 0; right: 7%; bottom: -25px; width: 142px; height: 150px; fill: #d4a0a0; fill-opacity: .4; stroke: #8b7355; stroke-width: 2.2; stroke-linecap: round; stroke-linejoin: round; opacity: .62; pointer-events: none; transition: transform .7s ease-in-out; }
.growth-center__header:hover .growth-center__botanical { transform: rotate(.7deg); }
.growth-center__botanical path:first-child { fill: none; }
.growth-center__botanical circle { fill: #f5d75f; fill-opacity: .7; }
.growth-center__eyebrow { margin: 0 0 9px; color: #805e49; font: 700 11px/1.3 Georgia, serif; letter-spacing: .07em; }
.growth-center__header h1 { margin: 0; color: #594434; font-size: clamp(36px, 3.7vw, 52px); line-height: 1.1; letter-spacing: -.04em; }
.growth-center__intro { max-width: 38em; margin: 10px 0 0; color: #665044; font-size: 14px; line-height: 1.6; }
.growth-center__tabs { display: flex; gap: 5px; margin-top: 20px; padding: 7px; overflow-x: auto; border: 1px solid rgba(212, 160, 160, .48); border-radius: 18px; background: #fffaf3; box-shadow: 0 9px 26px rgba(105, 75, 51, .07); scrollbar-width: thin; }
.growth-center__tab { flex: 1 0 auto; min-height: 44px; padding: 13px 15px; border-radius: 999px; color: #5c493c; font-size: 14px; font-weight: 700; text-align: center; text-decoration: none; white-space: nowrap; transition: background .55s ease-in-out, color .55s ease-in-out, transform .55s ease-in-out; }
.growth-center__tab:hover { background: #f2dede; color: #543d34; transform: rotate(-.4deg) scale(1.02); }
.growth-center__tab:focus-visible { outline: 2px solid #8b7355; outline-offset: 2px; }
.growth-center__tab:active { transform: scale(.97); }
.growth-center__tab.is-active { color: #fffaf3; background: #70543f; box-shadow: 0 5px 12px rgba(112, 84, 63, .17); }
.growth-center__section-label { display: flex; align-items: center; gap: 14px; margin-top: 25px; }
.growth-center__section-rule { width: 28px; height: 3px; border-radius: 99px; background: #d4a0a0; }
.growth-center__section-label p { margin: 0; color: #685449; font-size: 13px; font-weight: 600; }
.growth-center__main :deep(a:focus-visible), .growth-center__main :deep(button:focus-visible), .growth-center__main :deep(input:focus-visible), .growth-center__main :deep(select:focus-visible) { outline: 2px solid #8b7355; outline-offset: 3px; }

/* 温暖纸张、花粉和大地色覆盖成长中心各子页，不影响其他一级页面。 */
.growth-center :deep(.growth-tree__hero), .growth-center :deep(.skills-hero), .growth-center :deep(.courses-hero) { padding-top: 30px; }
.growth-center :deep(.growth-tree__hero h2), .growth-center :deep(.skills-hero h2), .growth-center :deep(.courses-hero h2), .growth-center :deep(.portrait-header h2) { color: #594434; letter-spacing: -.04em; }
.growth-center :deep(.growth-tree__eyebrow), .growth-center :deep(.skills-hero p), .growth-center :deep(.courses-hero p), .growth-center :deep(.portrait-overline) { color: #835748 !important; }
.growth-center :deep(.growth-tree__legend) { gap: 8px; padding: 8px; border-radius: 999px; background: #f2e5dc; color: #57443a; font-size: 12px; font-weight: 700; }
.growth-center :deep(.growth-tree__legend span) { padding: 5px 9px; }
.growth-center :deep(.growth-tree__legend i) { width: 11px; height: 11px; }
.growth-center :deep(.growth-tree__legend .is-lit) { background: #5a8f5a; }
.growth-center :deep(.growth-tree__legend .is-learning) { background: #b47f27; }
.growth-center :deep(.growth-tree__legend .is-unlit) { border-color: #9a8271; background: #fffaf3; }
.growth-center :deep(.growth-tree__legend .is-unknown) { background: #a38b7d; }
.growth-center :deep(.growth-tree__selector), .growth-center :deep(.skills-toolbar) { border-color: rgba(212, 160, 160, .48); border-radius: 22px; background: #fffaf3; box-shadow: 0 12px 26px rgba(105, 75, 51, .07); }
.growth-center :deep(.growth-tree__selector-head p), .growth-center :deep(.growth-tree__board-head p), .growth-center :deep(.skills-count span) { color: #655147; }
.growth-center :deep(.growth-tree__roles button), .growth-center :deep(.skills-tracks button) { min-height: 44px; border-color: #d4b9ad; color: #604a3b; background: #faf2e9; font-family: inherit; font-weight: 650; transition: background .55s ease-in-out, transform .55s ease-in-out, border-color .55s ease-in-out; }
.growth-center :deep(.growth-tree__roles button:hover), .growth-center :deep(.skills-tracks button:hover) { border-color: #b98783; transform: rotate(-.5deg) scale(1.02); }
.growth-center :deep(.growth-tree__roles button:focus-visible), .growth-center :deep(.skills-tracks button:focus-visible) { outline: 2px solid #8b7355; outline-offset: 2px; }
.growth-center :deep(.growth-tree__roles button:active), .growth-center :deep(.skills-tracks button:active) { transform: scale(.97); }
.growth-center :deep(.growth-tree__roles button.is-active), .growth-center :deep(.skills-tracks button.active) { border-color: #775849; color: #fffaf3; background: #775849; box-shadow: 0 5px 12px rgba(105, 75, 51, .16); }
.growth-center :deep(.growth-tree__board), .growth-center :deep(.skills-board) { border: 1px solid rgba(212, 160, 160, .42); border-radius: 28px 58px 28px 28px; background: radial-gradient(circle at 100% 0, rgba(212, 160, 160, .13), transparent 34%), #fffaf3; box-shadow: 0 18px 36px rgba(105, 75, 51, .10); }
.growth-center :deep(.growth-tree__board-head), .growth-center :deep(.skills-board-head) { border-bottom-color: #ddc9bd; }
.growth-center :deep(.growth-tree__board-head h2), .growth-center :deep(.skills-board-head h2) { color: #594434; }
.growth-center :deep(.growth-tree__board-head p), .growth-center :deep(.skills-board-head p), .growth-center :deep(.growth-tree__count span), .growth-center :deep(.skills-count span) { color: #69564a; }
.growth-center :deep(.growth-tree__count strong), .growth-center :deep(.skills-count strong) { color: #785049; }
.growth-center :deep(.growth-tree__count small) { color: #69564a; }
.growth-center :deep(.growth-tree__notice), .growth-center :deep(.skills-board .skills-notice) { border-color: #d7b78b; color: #664c33; background: #fcf1dc; }
.growth-center :deep(.growth-tree__empty), .growth-center :deep(.skills-board .skills-empty) { color: #655147; }
.growth-center :deep(.tree-scroll__hint) { color: #6b5648; font-size: 12px; font-weight: 700; }
.growth-center :deep(.tree-links path) { stroke: #b79782; stroke-width: 2.5; }
.growth-center :deep(.tree-root) { border-color: #b8966b; background: #f5e1b8; color: #4a382d; box-shadow: 0 8px 18px rgba(105, 75, 51, .12); }
.growth-center :deep(.tree-root small) { color: #71563c; }
.growth-center :deep(.tree-node) { border-color: #d2b6a4; background: #faf6f0; color: #47382e; box-shadow: 0 8px 20px rgba(105, 75, 51, .10); transition: transform .55s ease-in-out, box-shadow .55s ease-in-out, border-color .55s ease-in-out; }
.growth-center :deep(.tree-node:hover) { transform: rotate(-.45deg) scale(1.025); box-shadow: 0 14px 26px rgba(105, 75, 51, .15); }
.growth-center :deep(.tree-node:focus-visible) { outline: 2px solid #8b7355; outline-offset: 3px; }
.growth-center :deep(.tree-node:active) { transform: scale(.97); }
.growth-center :deep(.tree-node.is-selected) { border-color: #a36d66; outline: 3px solid #eac9c5; }
.growth-center :deep(.tree-node.is-branch) { border-color: #c99595; background: #f4e0df; }
.growth-center :deep(.tree-node.is-locked) { opacity: 1; background: #eee5dc; color: #5e4a3e; }
.growth-center :deep(.tree-node strong) { font-size: 14px; }
.growth-center :deep(.tree-node small) { color: #665246; font-size: 11px; }
.growth-center :deep(.tree-node__mark) { border-color: #9a8271; }
.growth-center :deep(.tree-node.is-mastered .tree-node__mark) { border-color: #5a8f5a; background: #5a8f5a; }
.growth-center :deep(.tree-node.is-learning .tree-node__mark) { border-color: #b47f27; background: #f5d75f; }
.growth-center :deep(.tree-node.is-unknown .tree-node__mark) { border-color: #987e73; background: #c4aaa0; }
.growth-center :deep(.tree-popover) { border-color: #d4b1aa; background: #fffaf3; color: #47382e; box-shadow: 0 18px 38px rgba(105, 75, 51, .18); }
.growth-center :deep(.tree-popover p), .growth-center :deep(.tree-popover small) { color: #5c493d; }
.growth-center :deep(.tree-popover a) { color: #795047; }

/* 课程和档案采用奶油纸面，彩色只用于真实进度和选中状态。 */
.growth-center :deep(.skills-filters input), .growth-center :deep(.skills-filters select), .growth-center :deep(.courses-filters input), .growth-center :deep(.courses-filters select) { border-color: #cbb7a9; border-radius: 14px; background: #faf6f0; color: #47382e; font-family: inherit; }
.growth-center :deep(.skills-filters input:focus), .growth-center :deep(.skills-filters select:focus), .growth-center :deep(.courses-filters input:focus), .growth-center :deep(.courses-filters select:focus) { outline: 2px solid #b98783; outline-offset: 2px; }
.growth-center :deep(.courses-summary) { border: 0; color: #fffaf3; background: #785846; box-shadow: 0 10px 22px rgba(105, 75, 51, .14); }
.growth-center :deep(.courses-summary span) { color: #fff2df; }
.growth-center :deep(.courses-list), .growth-center :deep(.courses-detail) { border-color: rgba(212, 160, 160, .45); background: #fffaf3; box-shadow: 0 15px 30px rgba(105, 75, 51, .07); }
.growth-center :deep(.courses-list) { border-top: 3px solid #d4a0a0; }
.growth-center :deep(.courses-detail) { border-top: 3px solid #c3a071; }
.growth-center :deep(.courses-item) { border-color: #d8c3b8; background: #fffdf8; transition: background .55s ease-in-out, border-color .55s ease-in-out, transform .55s ease-in-out; }
.growth-center :deep(.courses-item:hover) { border-color: #c48f8f; background: #f8e9e7; transform: rotate(-.35deg) scale(1.015); }
.growth-center :deep(.courses-item:focus-visible) { outline: 2px solid #8b7355; outline-offset: 2px; }
.growth-center :deep(.courses-item:active) { transform: scale(.97); }
.growth-center :deep(.courses-item.selected) { border-color: #a87975; background: #f3dfdd; box-shadow: 0 5px 14px rgba(105, 75, 51, .10); }
.growth-center :deep(.courses-item small), .growth-center :deep(.courses-detail-head small), .growth-center :deep(.courses-facts span), .growth-center :deep(.courses-chapter small), .growth-center :deep(.courses-exam small) { color: #69574b; }
.growth-center :deep(.courses-progress) { background: #eaded3; }
.growth-center :deep(.courses-progress i) { background: #5a8f5a; }
.growth-center :deep(.courses-facts div) { border-color: #ddc8bc; background: #f9efe5; }
.growth-center :deep(.courses-facts strong) { color: #705047; }
.growth-center :deep(.courses-detail-head button), .growth-center :deep(.courses-exam-actions button) { min-height: 44px; border-color: #70543f; border-radius: 999px; color: #fffaf3; background: #70543f; font-family: inherit; box-shadow: 0 5px 12px rgba(105, 75, 51, .14); transition: background .55s ease-in-out, transform .55s ease-in-out; }
.growth-center :deep(.courses-detail-head button:hover:not(:disabled)), .growth-center :deep(.courses-exam-actions button:hover:not(:disabled)) { background: #896a50; transform: rotate(-.4deg) scale(1.025); }
.growth-center :deep(.courses-detail-head button:focus-visible), .growth-center :deep(.courses-exam-actions button:focus-visible) { outline: 2px solid #8b7355; outline-offset: 2px; }
.growth-center :deep(.courses-detail-head button:active:not(:disabled)), .growth-center :deep(.courses-exam-actions button:active:not(:disabled)) { transform: scale(.97); }
.growth-center :deep(.courses-section-title a), .growth-center :deep(.courses-exam-actions a), .growth-center :deep(.courses-history-toggle) { color: #795047; }
.growth-center :deep(.portrait-section-heading > span) { color: #8b5b4c; }
.growth-center :deep(.portrait-overview) { gap: 4%; margin-bottom: 34px; padding: 28px 34px; border: 1px solid rgba(212, 160, 160, .48); border-radius: 28px 58px 28px 28px; background: #f3e4de; box-shadow: 0 16px 32px rgba(105, 75, 51, .10); }
.growth-center :deep(.portrait-overview .portrait-section-heading h3), .growth-center :deep(.portrait-overview .portrait-section-heading > span), .growth-center :deep(.portrait-evidence__row span), .growth-center :deep(.portrait-evidence__row strong) { color: #574139; }
.growth-center :deep(.portrait-evidence__row) { border-bottom-color: #d7bdb3; }
.growth-center :deep(.portrait-evidence__row small), .growth-center :deep(.portrait-evidence__row .is-missing), .growth-center :deep(.portrait-radar__hint) { color: #675248; }
.growth-center :deep(.portrait-radar__grid), .growth-center :deep(.portrait-radar__axis) { stroke: #b8998a; stroke-width: 1.4; }
.growth-center :deep(.portrait-radar__area) { fill: rgba(180, 116, 118, .32); stroke: #935d64; stroke-width: 3; }
.growth-center :deep(.portrait-radar__point) { fill: #935d64; stroke: #faf6f0; stroke-width: 2; }
.growth-center :deep(.portrait-radar text) { fill: #513c32; font-size: 13px; font-weight: 750; }
.growth-center :deep(.portrait-ring__track) { stroke: #e4d4c8; }
.growth-center :deep(.portrait-ring__value) { stroke: #5a8f5a; }
.growth-center :deep(.portrait-bar) { background: #e6d9cb; }
.growth-center :deep(.portrait-bar i) { background: #5a8f5a; }
.growth-center :deep(.portrait-day i) { background: #a77470; }
.growth-center :deep(.portrait-score-axis) { height: 8px; background: #eadbd0; }
.growth-center :deep(.portrait-score-axis i) { top: -6px; border-color: #faf6f0; background: #9b6166; box-shadow: 0 0 0 2px #9b6166; }
.growth-center :deep(.portrait-method), .growth-center :deep(.portrait-footnote), .growth-center :deep(.portrait-muted) { color: #655348; }

@media (prefers-reduced-motion: no-preference) {
  .growth-center__header { animation: growth-enter .65s ease-in-out both; }
  .growth-center__tabs { animation: growth-enter .65s .08s both; }
  .growth-center :deep(.growth-tree__board), .growth-center :deep(.skills-board), .growth-center :deep(.portrait-overview) { animation: growth-enter .55s .12s ease-in-out both; }
  .growth-center :deep(.portrait-radar__area) { transform-origin: 200px 200px; animation: growth-radar .7s .18s ease-in-out both; }
  @keyframes growth-enter { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: translateY(0); } }
  @keyframes growth-radar { from { opacity: 0; transform: scale(.88); } to { opacity: 1; transform: scale(1); } }
}
@media (max-width: 760px) {
  .growth-center__main { padding-top: 74px; }
  .growth-center__header, .growth-center__tabs, .growth-center__section-label { width: calc(100% - 30px); }
  .growth-center__header { min-height: 0; padding: 28px 24px 30px; border-bottom-right-radius: 48px; }
  .growth-center__botanical { right: -14px; bottom: -28px; width: 92px; height: 100px; opacity: .32; }
  .growth-center__tab { flex: 0 0 auto; }
  .growth-center :deep(.growth-tree__board), .growth-center :deep(.skills-board), .growth-center :deep(.portrait-overview) { border-top-right-radius: 42px; }
  .growth-center :deep(.portrait-overview) { padding: 22px 18px; }
}
@media (prefers-reduced-motion: reduce) { .growth-center__botanical, .growth-center__tab, .growth-center :deep(.tree-node), .growth-center :deep(.courses-item), .growth-center :deep(.growth-tree__roles button), .growth-center :deep(.skills-tracks button) { animation: none; transition: none; } }
</style>
