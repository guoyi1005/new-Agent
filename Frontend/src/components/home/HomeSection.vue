<script setup>
/**
 * 首页通用区块：统一标题层级与左右对齐，右上角可放操作区（分页、全部入口等）。
 * 页面里“岗位探索 / 我的目标岗位 / 今日计划 / 继续学习 / 当前能力成长 / 推荐岗位”共用同一套标题规格。
 */
defineProps({
  title: {
    type: String,
    required: true,
  },
  hint: {
    type: String,
    default: '',
  },
  /* lg：通栏区块标题；sm：成长路线阶段里的模块标题 */
  size: {
    type: String,
    default: 'lg',
  },
})
</script>

<template>
  <section class="home-section" :class="{ 'home-section--sm': size === 'sm' }">
    <header class="home-section__head">
      <h2 class="home-section__title">
        {{ title }}
        <span v-if="hint" class="home-section__hint">{{ hint }}</span>
      </h2>
      <div v-if="$slots.aside" class="home-section__aside">
        <slot name="aside" />
      </div>
    </header>
    <slot />
  </section>
</template>

<style scoped>
.home-section {
  margin-top: var(--hp-gap-section, 32px);
}

.home-section__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.home-section__title {
  display: flex;
  align-items: baseline;
  gap: 10px;
  margin: 0;
  color: var(--hp-ink, #23262b);
  font-size: 22px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.home-section__hint {
  color: var(--hp-muted, #8a9099);
  font-size: 13px;
  font-weight: 500;
  letter-spacing: 0;
}

.home-section__aside {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 0 0 auto;
}

/* 阶段内的模块标题：比阶段标题低一级，带一个强调色圆点 */
.home-section--sm .home-section__head {
  margin-bottom: 14px;
}

.home-section--sm .home-section__title {
  position: relative;
  padding-left: 14px;
  font-size: 17px;
  font-weight: 600;
}

.home-section--sm .home-section__title::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--hp-dot, #9dc0dd);
  transform: translateY(-50%);
}

.home-section--sm .home-section__hint {
  font-size: 12.5px;
}
</style>
