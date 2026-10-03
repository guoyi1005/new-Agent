<script setup>
import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const props = defineProps({
  rootLabel: { type: String, required: true },
  nodes: { type: Array, default: () => [] },
  edges: { type: Array, default: () => [] },
  selectedId: { type: String, default: '' },
})
const emit = defineEmits(['select', 'close'])
const scrollHost = ref(null)
const canvasHost = ref(null)
const viewport = ref({ left: 0, width: 720 })

function syncViewport() {
  if (!scrollHost.value || !canvasHost.value) return
  const left = scrollHost.value.getBoundingClientRect().left - canvasHost.value.getBoundingClientRect().left
  viewport.value = { left, width: scrollHost.value.clientWidth }
}

onMounted(() => {
  syncViewport()
  window.addEventListener('resize', syncViewport)
})
onBeforeUnmount(() => window.removeEventListener('resize', syncViewport))

const diagram = computed(() => {
  const ids = new Set(props.nodes.map((node) => String(node.id)))
  const links = props.edges.filter((edge) => ids.has(String(edge.source)) && ids.has(String(edge.target)) && String(edge.source) !== String(edge.target))
  const parents = new Map(props.nodes.map((node) => [String(node.id), []]))
  links.forEach((edge) => parents.get(String(edge.target)).push(String(edge.source)))
  const depths = new Map()
  const visiting = new Set()
  function depth(id) {
    if (depths.has(id)) return depths.get(id)
    if (visiting.has(id)) return 1
    visiting.add(id)
    const predecessors = parents.get(id) || []
    const value = predecessors.length ? 1 + Math.max(...predecessors.map(depth)) : 1
    visiting.delete(id)
    depths.set(id, value)
    return value
  }
  props.nodes.forEach((node) => depth(String(node.id)))
  const layers = new Map()
  props.nodes.forEach((node) => {
    const level = depths.get(String(node.id))
    if (!layers.has(level)) layers.set(level, [])
    layers.get(level).push(node)
  })
  const maxDepth = Math.max(0, ...depths.values())
  const width = Math.max(720, 24 + maxDepth * 230 + 176 + 24)
  const height = Math.max(270, ...[...layers.values()].map((items) => items.length * 90 + 36))
  const positions = new Map([['__root__', { x: 24, y: (height - 66) / 2 }]])
  layers.forEach((items, level) => {
    const columnHeight = items.length * 66 + (items.length - 1) * 24
    const startY = (height - columnHeight) / 2
    items.forEach((node, index) => positions.set(String(node.id), {
      x: 24 + level * 230,
      y: startY + index * 90,
    }))
  })
  const roots = props.nodes.filter((node) => !(parents.get(String(node.id)) || []).length)
  const allLinks = [...links, ...roots.map((node) => ({ source: '__root__', target: String(node.id) }))]
  const paths = allLinks.map((edge) => {
    const from = positions.get(String(edge.source))
    const to = positions.get(String(edge.target))
    if (!from || !to) return null
    const x1 = from.x + 176
    const y1 = from.y + 33
    const x2 = to.x
    const y2 = to.y + 33
    const middle = (x1 + x2) / 2
    return `M ${x1} ${y1} C ${middle} ${y1}, ${middle} ${y2}, ${x2} ${y2}`
  }).filter(Boolean)
  return { width, height, positions, paths }
})

const selectedNode = computed(() => props.nodes.find((node) => String(node.id) === props.selectedId))
const popoverStyle = computed(() => {
  const position = diagram.value.positions.get(props.selectedId)
  if (!position) return {}
  const panelWidth = Math.min(280, Math.max(200, viewport.value.width - 24))
  const visibleLeft = viewport.value.left
  const visibleRight = visibleLeft + viewport.value.width
  const right = position.x + 190
  const left = position.x - panelWidth - 14
  let x
  let y = position.y - 24
  if (right + panelWidth <= visibleRight - 8) x = right
  else if (left >= visibleLeft + 8) x = left
  else {
    x = Math.max(visibleLeft + 8, Math.min(position.x + 88 - panelWidth / 2, visibleRight - panelWidth - 8))
    y = position.y + 78
  }
  return {
    width: `${panelWidth}px`,
    left: `${x}px`,
    top: `${Math.max(8, Math.min(y, diagram.value.height - 230))}px`,
  }
})
</script>

<template>
  <div ref="scrollHost" class="tree-scroll" role="group" :aria-label="`${rootLabel}树状关系图`" @scroll="syncViewport" @keydown.esc="emit('close')">
    <p class="tree-scroll__hint">从左向右探索分支 · 点击节点查看依据</p>
    <div ref="canvasHost" class="tree-canvas" :style="{ width: `${diagram.width}px`, height: `${diagram.height}px` }">
      <svg class="tree-links" :viewBox="`0 0 ${diagram.width} ${diagram.height}`" aria-hidden="true">
        <path v-for="(path, index) in diagram.paths" :key="index" :d="path" />
      </svg>
      <div class="tree-root" :style="{ left: `${diagram.positions.get('__root__').x}px`, top: `${diagram.positions.get('__root__').y}px` }">
        <small>成长起点</small><strong>{{ rootLabel }}</strong>
      </div>
      <button v-for="node in nodes" :key="node.id" type="button" class="tree-node"
        :class="[`is-${node.status || 'available'}`, { 'is-branch': node.type === 'branch', 'is-selected': selectedId === String(node.id) }]"
        :style="{ left: `${diagram.positions.get(String(node.id))?.x || 0}px`, top: `${diagram.positions.get(String(node.id))?.y || 0}px` }"
        :aria-label="`${node.label}，${node.stateLabel || '分支'}`" :aria-expanded="selectedId === String(node.id)"
        @click="emit('select', selectedId === String(node.id) ? '' : String(node.id))">
        <span class="tree-node__mark" aria-hidden="true" />
        <span class="tree-node__copy"><strong>{{ node.label }}</strong><small>{{ node.stateLabel || '职业分支' }}</small></span>
      </button>
      <aside v-if="selectedNode" class="tree-popover" :style="popoverStyle" role="region" :aria-label="`${selectedNode.label}详情`">
        <button type="button" class="tree-popover__close" aria-label="关闭节点详情" @click="emit('close')">×</button>
        <slot name="detail" :node="selectedNode" />
      </aside>
    </div>
  </div>
</template>

<style scoped>
.tree-scroll{max-width:100%;overflow-x:auto;overflow-y:hidden;scrollbar-color:#c9c5b8 transparent}.tree-scroll__hint{margin:0 0 14px;color:#847d70;font-size:12px}.tree-canvas{position:relative;margin:auto}.tree-links{position:absolute;inset:0;width:100%;height:100%;pointer-events:none}.tree-links path{fill:none;stroke:#bcc9b5;stroke-width:2;stroke-linecap:round}
.tree-root,.tree-node{position:absolute;display:flex;align-items:center;justify-content:center;box-sizing:border-box;width:176px;height:66px;border:1px solid #cbd7c3;border-radius:17px;background:#f9fbf6;color:#2a241f;text-align:center}.tree-root{flex-direction:column;gap:3px;border-color:#9ebc91;background:#dfead8}.tree-root small{color:#53724b;font-size:10px;letter-spacing:.1em}.tree-root strong{max-width:160px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:14px}
.tree-node{gap:10px;justify-content:flex-start;padding:8px 11px;font:inherit;cursor:pointer;transition:transform .18s ease,border-color .18s ease}.tree-node:hover{transform:translateY(-3px)}.tree-node.is-selected{border-color:#557957;outline:2px solid rgba(118,160,109,.28)}.tree-node__mark{flex:0 0 11px;width:11px;height:11px;border:1.5px solid #b5bcb0;border-radius:50%;background:#fff}.tree-node__copy{display:grid;min-width:0;gap:3px;text-align:left}.tree-node strong{overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:13px}.tree-node small{color:#777a70;font-size:10px}.tree-node.is-mastered .tree-node__mark{border-color:#78a36f;background:#78a36f}.tree-node.is-learning .tree-node__mark{border-color:#dbad56;background:#f3d383}.tree-node.is-unknown .tree-node__mark{border-color:#b7c2ce;background:#dfe6ed}.tree-node.is-locked{opacity:.62}.tree-node.is-branch{border-color:#d4d3e1;background:#f1eff8}.tree-node.is-branch .tree-node__mark{border-color:#ab9dcb;background:#c9b9e8}
.tree-popover{position:absolute;z-index:5;box-sizing:border-box;max-height:220px;overflow-y:auto;padding:17px 19px 18px;border:1px solid #d9dfd2;border-radius:16px;background:#fffdf8;box-shadow:0 10px 28px rgba(43,47,35,.13);color:#2a241f}.tree-popover__close{position:absolute;top:7px;right:8px;display:grid;place-items:center;width:27px;height:27px;border:0;border-radius:50%;background:transparent;color:#6c6e65;font:inherit;font-size:20px;cursor:pointer}.tree-popover__close:hover{background:#f1f2e9}.tree-popover :deep(.node-detail__eyebrow){display:block;margin:0 24px 5px 0;color:#63825b;font-size:11px;font-weight:800}.tree-popover :deep(h3){margin:0 18px 8px 0;font-size:18px;line-height:1.35}.tree-popover :deep(p){margin:0 0 8px;color:#595f55;font-size:12px;line-height:1.55}.tree-popover :deep(small){display:block;margin-top:5px;color:#82877d;font-size:11px;line-height:1.45}.tree-popover :deep(a){display:inline-block;margin-top:10px;color:#52794e;font-size:12px;font-weight:700;text-decoration:none}.tree-popover :deep(a:hover){text-decoration:underline}
@media(prefers-reduced-motion:reduce){.tree-node{transition:none}}
</style>
