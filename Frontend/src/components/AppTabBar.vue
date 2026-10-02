<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { clearAuth, getUserInfo } from '../utils/auth'

defineProps({
  embedded: {
    type: Boolean,
    default: false,
  },
  variant: {
    type: String,
    default: 'default',
  },
})

const router = useRouter()
const route = useRoute()
const showProfilePanel = ref(false)
const userInfo = computed(() => getUserInfo() || {})
const avatarUrl = computed(() => userInfo.value.avatar || '')
const displayName = computed(() => userInfo.value.realName || userInfo.value.username || '未登录')
const studentId = computed(() => userInfo.value.studentId || userInfo.value.personalNumber || '—')
const avatarText = computed(() => {
  return displayName.value.slice(0, 1).toUpperCase()
})

function toggleProfilePanel() {
  showProfilePanel.value = !showProfilePanel.value
}

// 子页面（如星图探索内的 Python 学习）也需要保持所属模块的选中态
function inSection(basePath) {
  return route.path === basePath || route.path.startsWith(`${basePath}/`)
}

function openProfileRoute(path) {
  showProfilePanel.value = false
  router.push(path)
}

function handleLogout() {
  showProfilePanel.value = false
  clearAuth()
  router.replace('/login')
}

const shortcutItems = [
  { label: '我的消息', to: '/mine/messages' },
  { label: '我的课表', to: '/mine/schedule' },
  { label: '我的活动', to: '/mine/activities' },
  { label: 'AI 会话历史', to: '/mine/ai-history' },
  { label: '我的试卷', to: '/mine/papers' },
  { label: '账户设置', to: '/mine/account-settings' },
]

const isLearningRoute = computed(() => (
  inSection('/learning') ||
  inSection('/career/nebula/python') ||
  inSection('/paper') ||
  (inSection('/ai-tools') && !inSection('/ai-tools/resume')) ||
  inSection('/ai-studio') ||
  inSection('/convert') ||
  inSection('/courses')
))

// Python 学习页归入「学习实践」，因此「岗位探索」要排除这棵子树，避免两个入口同时高亮
const isCareerRoute = computed(() => (
  (inSection('/career') && !inSection('/career/nebula/python')) ||
  inSection('/jobs/explore')
))

const isEmploymentRoute = computed(() => inSection('/employment'))
const isCommunityRoute = computed(() => inSection('/community'))
const isAiCareerRoute = computed(() => (
  inSection('/ai-career') || inSection('/interview') || inSection('/ai-tools/resume') || inSection('/mine/ai-history')
))
</script>

<template>
  <header
    class="app-site-header"
    :class="{
      'app-site-header--embedded': embedded,
      'app-site-header--product': variant === 'product',
    }"
  >
    <div class="app-site-header__inner">
      <RouterLink class="app-site-header__brand" to="/home">
        数智<span>诊断</span>港
      </RouterLink>

      <nav class="app-site-header__nav" aria-label="主导航">
        <RouterLink to="/home">首页</RouterLink>
        <RouterLink to="/growth" :class="{ 'app-site-header__nav-link--active': inSection('/growth') || inSection('/profile-radar') || inSection('/activities') || inSection('/map') || inSection('/mine/activities') }">成长中心</RouterLink>
        <RouterLink to="/career" :class="{ 'app-site-header__nav-link--active': isCareerRoute }">岗位探索</RouterLink>
        <RouterLink to="/learning" :class="{ 'app-site-header__nav-link--active': isLearningRoute }">学习实践</RouterLink>
        <RouterLink to="/employment" :class="{ 'app-site-header__nav-link--active': isEmploymentRoute }">实习就业</RouterLink>
        <RouterLink to="/community" :class="{ 'app-site-header__nav-link--active': isCommunityRoute }">校友社区</RouterLink>
        <RouterLink to="/ai-career" :class="{ 'app-site-header__nav-link--active': isAiCareerRoute }">AI 求职</RouterLink>
      </nav>

      <div class="app-tab-nav__profile">
        <button class="app-tab-nav__avatar" type="button" aria-label="个人头像" @click="toggleProfilePanel">
          <img v-if="avatarUrl" :src="avatarUrl" alt="" />
          <span v-else>{{ avatarText }}</span>
        </button>
        <transition name="profile-panel">
          <div v-if="showProfilePanel" class="app-tab-nav__panel">
            <section class="app-tab-nav__panel-section">
              <p class="app-tab-nav__name">{{ displayName }}</p>
              <p class="app-tab-nav__student">学号 {{ studentId }}</p>
            </section>

            <section class="app-tab-nav__panel-section app-tab-nav__panel-section--list">
              <button
                v-for="item in shortcutItems"
                :key="item.to"
                class="app-tab-nav__panel-row"
                type="button"
                @click="openProfileRoute(item.to)"
              >
                <span>{{ item.label }}</span>
                <span class="app-tab-nav__panel-arrow">›</span>
              </button>
            </section>

            <section class="app-tab-nav__panel-section">
              <button class="app-tab-nav__logout" type="button" @click="handleLogout">
                退出登录
              </button>
            </section>
          </div>
        </transition>
      </div>
    </div>
  </header>
</template>

<style scoped>
/* 顶部导航：浅色底 + 深色选中态 的混合式现代导航，全站共用同一套功能 */
.app-site-header {
  position: fixed;
  inset: 0 0 auto;
  z-index: 1000;
  height: 60px;
  border-bottom: 1px solid #ede7de;
  background: #ffffff;
  color: #23262b;
  box-shadow: 0 1px 2px rgba(35, 38, 43, 0.03);
}

.app-site-header--embedded {
  position: static;
}

.app-site-header__inner {
  display: flex;
  align-items: center;
  width: min(1440px, calc(100% - 48px));
  height: 100%;
  margin: 0 auto;
  gap: 24px;
}

.app-site-header__brand {
  flex: 0 0 auto;
  color: #23262b;
  font-size: 17px;
  font-weight: 700;
  letter-spacing: 0.01em;
  text-decoration: none;
  white-space: nowrap;
}

.app-site-header__brand span {
  color: #5c8cb4;
}

.app-site-header__nav {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  min-width: 0;
  flex: 1;
  gap: 4px;
  overflow-x: auto;
  padding-right: 6px;
  scrollbar-width: none;
}

.app-site-header__nav::-webkit-scrollbar {
  display: none;
}

.app-site-header__nav a {
  display: grid;
  place-items: center;
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  color: #5a6069;
  font-size: 14px;
  font-weight: 500;
  text-decoration: none;
  white-space: nowrap;
  flex: 0 0 auto;
  transition: background 0.18s ease, color 0.18s ease;
}

.app-site-header__nav a:hover {
  background: #f4f1ec;
  color: #23262b;
}

/* 选中态用深色胶囊，形成浅色/深色的对比 */
.app-site-header__nav a.router-link-active,
.app-site-header__nav a.app-site-header__nav-link--active {
  background: #23262b;
  color: #ffffff;
  font-weight: 600;
}

.app-tab-nav__profile {
  position: relative;
  flex: 0 0 auto;
}

.app-tab-nav__avatar {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  min-height: 36px;
  border: 1px solid #e4ded4;
  border-radius: 50%;
  background: #f7f5f1;
  color: #23262b;
  font-size: 14px;
  font-weight: 700;
  overflow: hidden;
  padding: 0;
}

.app-tab-nav__avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.app-tab-nav__panel {
  position: absolute;
  top: 48px;
  right: 0;
  width: 220px;
  border: 1px solid #eae4da;
  border-radius: 18px;
  background: #ffffff;
  box-shadow: 0 4px 8px rgba(35, 38, 43, 0.04), 0 20px 44px rgba(35, 38, 43, 0.1);
  overflow: hidden;
}

.app-tab-nav__panel-section {
  padding: 14px 16px;
}

.app-tab-nav__panel-section + .app-tab-nav__panel-section {
  border-top: 1px solid #eae4da;
}

.app-tab-nav__panel-section--list {
  padding-top: 8px;
  padding-bottom: 8px;
}

.app-tab-nav__name,
.app-tab-nav__student {
  margin: 0;
}

.app-tab-nav__name {
  color: #23262b;
  font-size: 16px;
  font-weight: 700;
}

.app-tab-nav__student {
  margin-top: 8px;
  color: #8a9099;
  font-size: 13px;
}

.app-tab-nav__panel-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  min-height: 38px;
  padding: 0;
  color: #5a6069;
  background: transparent;
  text-align: left;
}

.app-tab-nav__panel-row + .app-tab-nav__panel-row {
  margin-top: 4px;
}

.app-tab-nav__panel-arrow {
  color: #8a9099;
  font-size: 20px;
}

.app-tab-nav__logout {
  width: 100%;
  min-height: 38px;
  border: 1px solid #efd6dc;
  border-radius: 12px;
  color: #b4707f;
  background: #ffffff;
  font-weight: 700;
}

.profile-panel-enter-active,
.profile-panel-leave-active {
  transition: opacity 0.18s ease, transform 0.18s ease;
}

.profile-panel-enter-from,
.profile-panel-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* AI 面试与岗位星图沿用同一套导航，底色压深以适应深色页面 */
.app-site-header--product {
  border-bottom-color: #14171b;
  background: #1b1e24;
  box-shadow: none;
}

.app-site-header--product .app-site-header__brand {
  color: #f4f1ec;
}

.app-site-header--product .app-site-header__brand span {
  color: #9dc0dd;
}

.app-site-header--product .app-site-header__nav a {
  color: rgba(244, 241, 236, 0.72);
}

.app-site-header--product .app-site-header__nav a:hover {
  background: rgba(244, 241, 236, 0.1);
  color: #f4f1ec;
}

.app-site-header--product .app-site-header__nav a.router-link-active,
.app-site-header--product .app-site-header__nav a.app-site-header__nav-link--active {
  background: #f2f4f7;
  color: #1b1e24;
}

.app-site-header--product .app-tab-nav__avatar {
  border-color: rgba(244, 241, 236, 0.28);
  background: #f4f1ec;
  color: #1b1e24;
}

@media (max-width: 680px) {
  .app-site-header__inner {
    width: min(100%, calc(100% - 24px));
    gap: 12px;
  }

  .app-site-header__brand {
    font-size: 16px;
  }
}

@media (max-width: 760px) {
  .app-site-header--product .app-site-header__inner {
    width: calc(100% - 16px);
    gap: 8px;
  }

  .app-site-header--product .app-site-header__brand {
    display: none;
  }

  .app-site-header--product .app-site-header__nav {
    justify-content: flex-start;
  }

  .app-site-header--product .app-site-header__nav a {
    min-height: 34px;
    padding: 0 10px;
    font-size: 12px;
  }
}
</style>
