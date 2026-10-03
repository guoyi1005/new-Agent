<script setup>
import { useRoute } from 'vue-router'

const route = useRoute()

const links = [
  { to: '/career/nebula/python/plan', label: '学习规划', match: (path) => path === '/career/nebula/python/plan' || path === '/career/nebula/python/resources' },
  { to: '/career/nebula/python/knowledge-graph', label: '知识图谱', match: (path) => path === '/career/nebula/python/knowledge-graph' },
  { to: '/career/nebula/python', label: '题库', match: (path) => path === '/career/nebula/python' || path.startsWith('/career/nebula/python/practice') },
]

function isActive(link) {
  return link.match(route.path)
}
</script>

<template>
  <nav class="py-learning-nav" aria-label="Python 学习导航">
    <RouterLink
      v-for="link in links"
      :key="link.to"
      :to="link.to"
      class="py-learning-nav__link"
      :class="{ 'py-learning-nav__link--active': isActive(link) }"
    >
      {{ link.label }}
    </RouterLink>
  </nav>
</template>

<style scoped>
/* 与站内其它二级导航保持一致：浅色胶囊 + 深色选中态 */
.py-learning-nav {
  display: inline-flex;
  gap: 4px;
  padding: 4px;
  border: 1px solid var(--hp-line);
  border-radius: 999px;
  background: var(--hp-surface-2);
}

.py-learning-nav__link {
  padding: 8px 18px;
  border-radius: 999px;
  color: var(--hp-muted);
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
  transition: background 0.18s ease, color 0.18s ease;
}

.py-learning-nav__link:hover {
  color: var(--hp-ink);
  background: var(--hp-surface);
}

.py-learning-nav__link--active {
  color: #fff;
  background: var(--hp-ink);
}

.py-learning-nav__link:active {
  transform: translateY(1px);
}
</style>
