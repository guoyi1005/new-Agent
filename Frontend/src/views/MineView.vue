<script setup>
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import AppTabBar from '../components/AppTabBar.vue'
import { clearAuth, getUserInfo } from '../utils/auth'

const router = useRouter()
const userInfo = computed(() => getUserInfo() || {})
const displayName = computed(() => userInfo.value.realName || userInfo.value.username || '未登录')
const studentId = computed(() => userInfo.value.studentId || userInfo.value.personalNumber || '—')
const menuItems = [
  { label: '我的消息', description: '查看系统消息和互动通知', to: '/mine/messages' },
  { label: '我的课表', description: '查看课程安排与学期设置', to: '/mine/schedule' },
  { label: '我的活动', description: '查看报名和已参加活动', to: '/mine/activities' },
  { label: 'AI 会话历史', description: '查看 AI 助手历史会话', to: '/mine/ai-history' },
  { label: '我的试卷', description: '查看试卷和答题历史', to: '/mine/papers' },
  { label: '个人画像', description: '查看学习能力与成长画像', to: '/profile-radar' },
  { label: '账户设置', description: '修改密码、头像和账户信息', to: '/mine/account-settings' },
]

function goBack() {
  if (window.history.length > 1) router.back()
  else router.push('/home')
}

function openMenuItem(item) {
  router.push(item.to)
}

function logout() {
  clearAuth()
  router.replace('/login')
}
</script>

<template>
  <div class="mine-page">
    <AppTabBar />
    <main class="mine-shell">
      <div class="mine-nav">
        <button type="button" class="back-link" @click="goBack">← 返回上一页</button>
        <button type="button" class="back-link" @click="router.push('/home')">返回首页</button>
      </div>
      <header class="mine-hero">
        <div class="avatar">
          <img v-if="userInfo.avatar" :src="userInfo.avatar" alt="" />
          <span v-else>{{ displayName.slice(0, 1).toUpperCase() }}</span>
        </div>
        <div class="mine-hero__copy">
          <p>MY ACCOUNT</p>
          <h1>个人中心</h1>
          <strong>{{ displayName }}</strong>
          <span>学号 {{ studentId }}</span>
        </div>
        <button type="button" class="mine-button" @click="router.push('/mine/account-settings')">账户设置</button>
      </header>

      <div class="mine-layout">
        <section class="mine-section">
          <div class="section-head"><div><h2>常用功能</h2><span>快速进入个人消息、学习和活动记录</span></div><span>{{ menuItems.length }} 个入口</span></div>
          <div class="menu-grid">
            <button v-for="(item, index) in menuItems" :key="item.to" type="button" class="menu-card" @click="openMenuItem(item)">
              <span class="menu-index">{{ String(index + 1).padStart(2, '0') }}</span>
              <div><strong>{{ item.label }}</strong><p>{{ item.description }}</p></div>
              <em>→</em>
            </button>
          </div>
        </section>

        <aside class="account-panel">
          <div class="account-panel__head"><h2>账户信息</h2><span>当前登录账号</span></div>
          <dl>
            <div><dt>姓名</dt><dd>{{ displayName }}</dd></div>
            <div><dt>学号</dt><dd>{{ studentId }}</dd></div>
            <div><dt>账号状态</dt><dd>正常</dd></div>
          </dl>
          <button type="button" class="mine-button mine-button--primary" @click="router.push('/mine/account-settings')">管理账户</button>
          <button type="button" class="logout-button" @click="logout">退出登录</button>
        </aside>
      </div>
    </main>
  </div>
</template>

<style scoped>
.mine-page {
  min-height: 100vh;
  color: var(--hp-ink);
  background: var(--hp-bg);
  font-family: Inter, 'Segoe UI', system-ui, -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.mine-page *,
.mine-page *::before,
.mine-page *::after {
  box-sizing: border-box;
}

.mine-shell {
  width: min(1280px, calc(100% - 48px));
  margin: 0 auto;
  padding: 96px 0 80px;
}

.mine-hero,
.mine-section,
.account-panel,
.menu-card {
  border: 1px solid var(--hp-line);
  background: var(--hp-surface);
  box-shadow: var(--hp-shadow-sm);
}

.mine-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.back-link {
  min-height: 36px;
  padding: 0 12px;
  border: 1px solid var(--hp-line-strong);
  border-radius: 999px;
  color: var(--hp-ink-2);
  background: var(--hp-surface);
  font-size: 12px;
  font-weight: 650;
  cursor: pointer;
}
.mine-hero {
  display: grid;
  grid-template-columns: 82px minmax(0, 1fr) auto;
  align-items: center;
  gap: 22px;
  padding: 24px 28px;
  border-color: #e4ebf2;
  border-radius: var(--hp-r-lg);
  background: var(--hp-tint);
}

.avatar {
  display: grid;
  width: 76px;
  height: 76px;
  place-items: center;
  overflow: hidden;
  border: 1px solid #d3e0e9;
  border-radius: 50%;
  color: var(--hp-blue-ink);
  background: var(--hp-blue);
  font-size: 28px;
  font-weight: 800;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.mine-hero__copy {
  display: grid;
  gap: 5px;
  min-width: 0;
}

.mine-hero__copy p,
.section-head span,
.account-panel__head span,
.menu-card p {
  margin: 0;
  color: var(--hp-muted);
  font-size: 12px;
  line-height: 1.6;
}

.mine-hero__copy p {
  color: var(--hp-blue-ink);
  font-weight: 750;
  letter-spacing: .12em;
}

.mine-hero h1 {
  margin: 0;
  font-size: 26px;
  line-height: 1.2;
}

.mine-hero__copy strong {
  font-size: 15px;
}

.mine-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-height: 42px;
  padding: 0 18px;
  border: 1px solid var(--hp-line-strong);
  border-radius: 999px;
  color: var(--hp-ink);
  background: var(--hp-surface);
  font-size: 13px;
  font-weight: 650;
  text-decoration: none;
  cursor: pointer;
}

.mine-button--primary {
  border-color: var(--hp-ink);
  color: #fff;
  background: var(--hp-ink);
}

.mine-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
  align-items: start;
  margin-top: 16px;
}

.mine-section,
.account-panel {
  border-radius: var(--hp-r-lg);
}

.mine-section {
  padding: 24px 28px;
}

.section-head,
.account-panel__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.section-head h2,
.account-panel h2 {
  margin: 0 0 5px;
  font-size: 20px;
}

.menu-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 18px;
}

.menu-card {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr) auto;
  align-items: center;
  gap: 13px;
  min-height: 102px;
  padding: 16px;
  border-radius: 14px;
  color: var(--hp-ink);
  text-align: left;
  cursor: pointer;
  transition: transform .18s ease, border-color .18s ease, box-shadow .18s ease;
}

.menu-card:hover {
  transform: translateY(-2px);
  border-color: #bfcbd4;
  box-shadow: var(--hp-shadow-md);
}

.menu-index {
  color: var(--hp-blue-ink);
  font-size: 12px;
  font-weight: 800;
}

.menu-card > div {
  display: grid;
  gap: 6px;
}

.menu-card strong {
  font-size: 15px;
}

.menu-card em {
  color: var(--hp-blue-ink);
  font-size: 16px;
  font-style: normal;
}

.account-panel {
  display: grid;
  gap: 16px;
  padding: 24px;
}

.account-panel dl {
  display: grid;
  gap: 0;
  margin: 0;
}

.account-panel dl > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 12px 0;
  border-top: 1px solid var(--hp-line);
}

.account-panel dt {
  color: var(--hp-muted);
  font-size: 12px;
}

.account-panel dd {
  margin: 0;
  font-size: 13px;
  font-weight: 650;
}

.logout-button {
  min-height: 42px;
  border: 1px solid #d9b0ab;
  border-radius: 999px;
  color: #a54239;
  background: transparent;
  font-weight: 700;
  cursor: pointer;
}

.logout-button:hover {
  color: #fffdf8;
  background: #a54239;
}

@media (max-width: 900px) {
  .mine-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 680px) {
  .mine-shell {
    width: min(100% - 24px, 1280px);
    padding-top: 82px;
  }

  .mine-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.back-link {
  min-height: 36px;
  padding: 0 12px;
  border: 1px solid var(--hp-line-strong);
  border-radius: 999px;
  color: var(--hp-ink-2);
  background: var(--hp-surface);
  font-size: 12px;
  font-weight: 650;
  cursor: pointer;
}
.mine-hero {
    grid-template-columns: 64px minmax(0, 1fr);
    padding: 20px;
  }

  .avatar {
    width: 60px;
    height: 60px;
  }

  .mine-hero > .mine-button {
    grid-column: 1 / -1;
  }

  .mine-section,
  .account-panel {
    padding: 18px;
  }

  .menu-grid {
    grid-template-columns: 1fr;
  }
}
</style>
