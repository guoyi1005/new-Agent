<script setup>
import AppTabBar from '../../components/AppTabBar.vue'
import PythonLearningNav from '../../components/pythonOnline/PythonLearningNav.vue'
</script>

<template>
  <div class="python-learning-shell">
    <AppTabBar />
    <div class="python-learning-shell__nav-row">
      <RouterLink class="py-shell-back" to="/learning">
        <span class="py-shell-back__arrow" aria-hidden="true">←</span>
        <span>返回学习实践</span>
      </RouterLink>
      <PythonLearningNav />
    </div>
    <RouterView />
  </div>
</template>

<style scoped>
.python-learning-shell {
  min-height: 100vh;
  background: var(--hp-bg, #f7f5f1);
}

.python-learning-shell__nav-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  width: min(1480px, calc(100% - 48px));
  margin: 0 auto;
  padding: 72px 0 8px;
}

/* 返回入口统一放在二级导航行左侧，与右侧索引栏凑成一行 */
.py-shell-back {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border: 1px solid var(--hp-line-strong);
  border-radius: 999px;
  color: var(--hp-ink-2);
  background: var(--hp-surface);
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  transition: border-color 0.2s ease, color 0.2s ease, background 0.2s ease;
}

.py-shell-back:hover {
  border-color: var(--hp-blue-ink);
  color: var(--hp-blue-ink);
  background: var(--hp-tint);
}

.py-shell-back__arrow {
  line-height: 1;
  transition: transform 0.2s ease;
}

.py-shell-back:hover .py-shell-back__arrow {
  transform: translateX(-2px);
}

.python-learning-shell :deep(.py-bank-page) {
  min-height: calc(100vh - 100px);
  background: transparent;
}

@media (max-width: 720px) {
  .python-learning-shell__nav-row {
    width: min(100% - 24px, 720px);
    overflow-x: auto;
  }
}
</style>
