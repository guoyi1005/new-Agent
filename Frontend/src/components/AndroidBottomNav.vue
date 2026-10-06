<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'

const route = useRoute()
const moreOpen = ref(false)

const tabs = [
  { label: '首页', to: '/home', icon: 'M3 10.5 12 3l9 7.5V21H3V10.5Zm6 10.5v-7h6v7' },
  { label: '成长', to: '/growth', icon: 'M12 21V4m0 10c-5 0-8-2-8-7 4 0 8 2 8 7Zm0-4c0-4 3-6 8-6 0 5-3 7-8 7' },
  { label: '岗位', to: '/career', icon: 'M4 7h16v13H4V7Zm5 0V4h6v3M4 12h16m-10 0v2h4v-2' },
  { label: '学习', to: '/learning', icon: 'M3 5c3-1 6-1 9 1v15c-3-2-6-2-9-1V5Zm18 0c-3-1-6-1-9 1v15c3-2 6-2 9-1V5Z' },
  { label: '实习', to: '/employment', icon: 'M3 8h18v12H3V8Zm5 0V5h8v3m-13 5h18m-11 0v2h4v-2' },
]

const moreItems = [
  { label: '校友社区', to: '/community', detail: '交流经验与就业信息' },
  { label: 'AI 求职', to: '/ai-career', detail: '简历、面试与求职工具' },
  { label: '我的', to: '/mine', detail: '个人信息与服务' },
  { label: '服务器连接', to: '/mobile-connection', detail: '更换后端地址' },
]

const isMoreActive = computed(() => ['/community', '/ai-career', '/interview', '/ai-tools/resume', '/mine', '/mobile-connection']
  .some((path) => route.path === path || route.path.startsWith(`${path}/`)))

function closeMore() { moreOpen.value = false }
</script>

<template>
  <div class="android-nav">
    <div v-if="moreOpen" class="android-nav__scrim" @click="closeMore"></div>
    <section v-if="moreOpen" class="android-nav__sheet" aria-label="更多功能">
      <div class="android-nav__sheet-head"><h2>更多功能</h2><button type="button" aria-label="关闭更多功能" @click="closeMore">×</button></div>
      <RouterLink v-for="item in moreItems" :key="item.to" :to="item.to" @click="closeMore">
        <span><strong>{{ item.label }}</strong><small>{{ item.detail }}</small></span><span aria-hidden="true">›</span>
      </RouterLink>
    </section>
    <nav class="android-nav__bar" aria-label="安卓主导航">
      <RouterLink v-for="tab in tabs" :key="tab.to" :to="tab.to" active-class="is-active" @click="closeMore">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path :d="tab.icon" /></svg>
        <span>{{ tab.label }}</span>
      </RouterLink>
      <button type="button" :class="{ 'is-active': isMoreActive || moreOpen }" :aria-expanded="moreOpen" @click="moreOpen = !moreOpen">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" aria-hidden="true"><circle cx="5" cy="12" r="1" /><circle cx="12" cy="12" r="1" /><circle cx="19" cy="12" r="1" /></svg>
        <span>更多</span>
      </button>
    </nav>
  </div>
</template>

<style scoped>
.android-nav{position:fixed;z-index:1300;inset:auto 0 0;color:#534234;font-family:Inter,'PingFang SC','Microsoft YaHei',sans-serif}
.android-nav__bar{display:grid;grid-template-columns:repeat(6,minmax(0,1fr));min-height:62px;padding:3px 4px calc(3px + env(safe-area-inset-bottom));border-top:1px solid #e6d9c9;background:#fffaf3;box-shadow:0 -6px 24px rgba(76,52,35,.08)}
.android-nav__bar a,.android-nav__bar button{display:flex;flex-direction:column;align-items:center;justify-content:center;gap:3px;min-width:0;min-height:56px;padding:0;border:0;border-radius:12px;background:transparent;color:#77695c;font:inherit;font-size:10px;font-weight:700;text-decoration:none;cursor:pointer}
.android-nav__bar svg{width:21px;height:21px;flex:none}
.android-nav__bar .is-active{color:#69452f;background:#f4e6d0}
.android-nav__scrim{position:fixed;inset:0;background:rgba(32,27,23,.42)}
.android-nav__sheet{position:relative;z-index:1;padding:20px 20px 12px;border-radius:24px 24px 0 0;background:#fffaf3;box-shadow:0 -18px 50px rgba(32,27,23,.12)}
.android-nav__sheet-head{display:flex;justify-content:space-between;align-items:center;margin-bottom:8px}
.android-nav__sheet h2{margin:0;font-size:19px}
.android-nav__sheet-head button{width:40px;height:40px;border:0;border-radius:50%;background:#f1e7db;color:#534234;font-size:25px}
.android-nav__sheet>a{display:flex;align-items:center;justify-content:space-between;min-height:61px;border-bottom:1px solid #e9ded0;color:#4d3b2e;text-decoration:none}
.android-nav__sheet>a:last-child{border-bottom:0}
.android-nav__sheet>a span:first-child{display:flex;flex-direction:column;gap:3px}
.android-nav__sheet strong{font-size:14px}.android-nav__sheet small{color:#78695a;font-size:11px}
.android-nav__sheet>a span:last-child{font-size:23px}
</style>
