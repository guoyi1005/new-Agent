<script setup>
import { computed } from 'vue'

/**
 * 轻量进度条：目标岗位的核心技能进度与“当前能力成长”共用。
 * tone 决定强调色，每个区块只用一种强调色。
 */
const props = defineProps({
  label: {
    type: String,
    required: true,
  },
  value: {
    type: Number,
    default: 0,
  },
  tone: {
    type: String,
    default: 'blue',
  },
  caption: {
    type: String,
    default: '',
  },
  compact: {
    type: Boolean,
    default: false,
  },
})

const percent = computed(() => Math.max(0, Math.min(100, Math.round(props.value))))
</script>

<template>
  <div class="skill" :class="{ 'skill--compact': compact }">
    <div class="skill__row">
      <span class="skill__label">{{ label }}</span>
      <span class="skill__value">{{ percent }}%</span>
    </div>
    <span
      class="skill__track"
      :class="`is-${tone}`"
      role="progressbar"
      :aria-label="label"
      :aria-valuenow="percent"
      aria-valuemin="0"
      aria-valuemax="100"
    >
      <i :style="{ width: `${percent}%` }"></i>
    </span>
    <p v-if="caption" class="skill__caption">{{ caption }}</p>
  </div>
</template>

<style scoped>
.skill {
  display: flex;
  flex-direction: column;
  gap: 7px;
}

.skill__row {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: 12px;
}

.skill__label {
  color: var(--hp-ink, #23262b);
  font-size: 14px;
  font-weight: 600;
}

.skill__value {
  color: var(--hp-muted, #8a9099);
  font-size: 12px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.skill__track {
  display: block;
  height: 8px;
  border-radius: 999px;
  background: var(--hp-track, #efeae1);
  overflow: hidden;
}

.skill__track > i {
  display: block;
  height: 100%;
  border-radius: 999px;
  background: var(--hp-accent, #9dc0dd);
  transition: width 0.32s ease;
}

.skill__track.is-blue > i {
  background: #9dc0dd;
}

.skill__track.is-green > i {
  background: #a9c69a;
}

.skill__track.is-pink > i {
  background: #e3b3bd;
}

.skill__track.is-yellow > i {
  background: #e8c979;
}

.skill__caption {
  margin: 0;
  color: var(--hp-muted, #8a9099);
  font-size: 12px;
}

.skill--compact .skill__label {
  font-size: 13px;
}
</style>
