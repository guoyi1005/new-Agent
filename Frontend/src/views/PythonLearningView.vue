<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  completePathItem,
  getPythonHome,
  replanPythonPath,
  startPathItem,
} from '../api/learning'

const router = useRouter()
const loading = ref(true)
const busy = ref('')
const error = ref('')
const home = ref({})

const mastery = computed(() => Array.isArray(home.value.mastery) ? home.value.mastery : [])
const pathItems = computed(() => home.value.activePath?.items || [])
const recommendations = computed(() => home.value.recommendations || [])
const counts = computed(() => mastery.value.reduce((result, item) => {
  const key = ['mastered', 'weak', 'learning'].includes(item.status) ? item.status : 'other'
  result[key] = (result[key] || 0) + 1
  return result
}, {}))

async function load() {
  loading.value = true
  error.value = ''
  try { home.value = await getPythonHome() || {} } catch (cause) { error.value = cause.message } finally { loading.value = false }
}

async function updateItem(item, action) {
  busy.value = `${action}-${item.id}`
  error.value = ''
  try {
    if (action === 'start') await startPathItem(item.id)
    else await completePathItem(item.id)
    await load()
  } catch (cause) { error.value = cause.message } finally { busy.value = '' }
}

async function replan() {
  busy.value = 'replan'
  try { await replanPythonPath(); await load() } catch (cause) { error.value = cause.message } finally { busy.value = '' }
}

onMounted(load)
</script>

<template>
  <div class="feature-page py-dark">
    <main class="feature-container">
      <header class="feature-heading">
        <div>
          <RouterLink class="py-back py-back--nebula" to="/learning">
            <span class="py-back__arrow" aria-hidden="true">←</span>
            <span>返回学习实践</span>
          </RouterLink>
          <h1>Python 个性化学习</h1><p>根据真实答题和学习记录规划下一步</p>
        </div>
        <div class="feature-actions">
          <button class="feature-button" :disabled="busy === 'replan'" @click="replan">重新规划路径</button>
          <button class="feature-button feature-button--primary" @click="router.push('/career/nebula/python/resources')">生成专项资源</button>
        </div>
      </header>
      <div v-if="error" class="feature-error">{{ error }}</div>
      <div v-if="loading" class="feature-empty">正在加载学习数据…</div>
      <div v-else class="learning-dashboard">
        <section class="feature-card graph-entry" @click="router.push('/career/nebula/python/knowledge-graph')">
          <div class="graph-entry__copy"><span class="graph-entry__eyebrow">KNOWLEDGE GRAPH</span><h2>个人知识图谱</h2><p>查看知识关系、掌握状态和前置依赖</p><button class="feature-button feature-button--primary">进入知识图谱</button></div>
          <div class="graph-entry__preview" aria-hidden="true">
            <i class="node n1"></i><i class="node n2"></i><i class="node n3"></i><i class="node weak"></i>
            <b class="line l1"></b><b class="line l2"></b><b class="line l3"></b>
          </div>
        </section>
        <aside class="feature-card feature-section overview">
          <div class="feature-section__head"><h2>学习概览</h2></div>
          <div class="overview__row"><span>已掌握</span><strong>{{ counts.mastered || 0 }}</strong></div>
          <div class="overview__row"><span>需巩固</span><strong>{{ counts.weak || 0 }}</strong></div>
          <div class="overview__row"><span>学习中</span><strong>{{ counts.learning || 0 }}</strong></div>
          <div class="overview__row"><span>画像完整度</span><strong>{{ home.profileCompleteness ?? '—' }}{{ home.profileCompleteness == null ? '' : '%' }}</strong></div>
        </aside>
        <section class="feature-card feature-section path-panel">
          <div class="feature-section__head"><div><h2>当前学习路径</h2><p>{{ home.activePath?.goal || '尚未生成学习目标' }}</p></div></div>
          <div v-if="!pathItems.length" class="feature-empty">暂无学习路径，可点击“重新规划路径”生成</div>
          <div v-else class="feature-list">
            <div v-for="item in pathItems" :key="item.id" class="feature-row">
              <div class="path-sequence">{{ item.sequenceNo }}</div>
              <div class="feature-row__copy"><strong>{{ item.knowledgePoint }}</strong><span>{{ item.objective }}</span></div>
              <span :class="`feature-status feature-status--${item.status || 'pending'}`">{{ item.status || 'pending' }}</span>
              <button v-if="item.status !== 'completed'" class="feature-button" :disabled="busy.endsWith(`-${item.id}`)" @click="updateItem(item, item.status === 'in_progress' ? 'complete' : 'start')">
                {{ item.status === 'in_progress' ? '标记完成' : '开始学习' }}
              </button>
            </div>
          </div>
        </section>
        <section class="feature-card feature-section recommendation-panel">
          <div class="feature-section__head"><h2>精准推荐</h2><a href="#" @click.prevent="router.push('/career/nebula/python/resources')">生成资源</a></div>
          <div v-if="!recommendations.length" class="feature-empty">完成练习后会展示基于真实证据的推荐</div>
          <div v-else class="feature-list">
            <button v-for="item in recommendations.slice(0, 6)" :key="item.id" class="recommendation" @click="router.push({ path:'/career/nebula/python/resources', query:{ topic:item.title || item.knowledgePoint } })">
              <span>{{ item.title || item.knowledgePoint || '学习建议' }}</span><small>{{ item.reason || item.rationale }}</small>
            </button>
          </div>
        </section>
      </div>
    </main>
  </div>
</template>

<style scoped>
.learning-dashboard{display:grid;grid-template-columns:minmax(0,2fr) minmax(280px,1fr);gap:20px}.graph-entry{display:flex;min-height:310px;padding:30px;cursor:pointer}.graph-entry__copy{position:relative;z-index:2;width:42%}.graph-entry__eyebrow{color:#6c8196;font-size:11px;font-weight:800;letter-spacing:1.4px}.graph-entry h2{margin:14px 0 8px;color:#20344b;font-size:25px}.graph-entry p{margin:0 0 28px;color:#718096}.graph-entry__preview{position:relative;flex:1;min-height:240px}.node{position:absolute;width:70px;height:32px;border:2px solid #6d8ca8;border-radius:17px;background:#f4f8fb}.node:after{content:'';position:absolute;inset:9px 27px;border-radius:50%;background:#527696}.n1{left:5%;top:45%}.n2{left:42%;top:14%;border-color:#5d9b7d}.n2:after{background:#4c9471}.n3{left:48%;top:67%;border-color:#5d9b7d}.n3:after{background:#4c9471}.weak{right:3%;top:42%;border-color:#bb6c65}.weak:after{background:#b85f57}.line{position:absolute;height:1px;background:#9eb0c1;transform-origin:left}.l1{left:22%;top:48%;width:115px;transform:rotate(-28deg)}.l2{left:22%;top:52%;width:126px;transform:rotate(23deg)}.l3{left:61%;top:34%;width:105px;transform:rotate(20deg)}.overview__row{display:flex;justify-content:space-between;padding:16px 0;border-top:1px solid #edf1f5}.overview__row span{color:#65758a}.overview__row strong{color:#26384d}.path-panel{grid-column:1}.recommendation-panel{grid-column:2}.path-sequence{display:grid;flex:0 0 30px;place-items:center;width:30px;height:30px;border-radius:50%;color:#315f8c;background:#eaf1f7;font-weight:800}.feature-row__copy{flex:1}.feature-section__head p{margin:5px 0 0;color:#718096;font-size:13px}.recommendation{display:block;width:100%;padding:14px;border:1px solid #e1e7ed;border-radius:8px;color:#26384d;background:#fff;text-align:left}.recommendation span,.recommendation small{display:block}.recommendation span{font-weight:750}.recommendation small{margin-top:7px;color:#718096;line-height:1.5}@media(max-width:900px){.learning-dashboard{grid-template-columns:1fr}.graph-entry,.overview,.path-panel,.recommendation-panel{grid-column:1}.graph-entry__copy{width:55%}}

/* 深色未来感 Dashboard 主题补充 */
.py-dark .graph-entry{background:linear-gradient(135deg,rgba(31,36,58,.9),rgba(20,24,38,.86))}
.py-dark .graph-entry__eyebrow{color:#8d98ae}
.py-dark .graph-entry h2{color:#f2f5ff}
.py-dark .graph-entry p{color:#93a0b8}
.py-dark .node{border-color:rgba(124,137,255,.5);background:rgba(23,28,43,.85)}
.py-dark .node:after{background:#8f9bff}
.py-dark .node.n2,.py-dark .node.n3{border-color:rgba(117,221,185,.55)}
.py-dark .node.n2:after,.py-dark .node.n3:after{background:#75ddb9}
.py-dark .node.weak{border-color:rgba(239,158,183,.6)}
.py-dark .node.weak:after{background:#ef9eb7}
.py-dark .line{background:rgba(124,137,255,.34)}
.py-dark .overview__row span{color:#8893aa}
.py-dark .overview__row strong{color:#e7ecf8}
.py-dark .path-sequence{color:#c9d2ff;background:rgba(83,91,199,.24)}
.py-dark .feature-section__head p{color:#8893aa}
.py-dark .recommendation{border-color:rgba(148,163,184,.14);border-radius:16px;color:#e7ecf8;background:rgba(23,28,43,.72)}
.py-dark .recommendation:hover{border-color:rgba(124,137,255,.45);background:rgba(52,59,94,.6)}
.py-dark .recommendation small{color:#8893aa}

.py-dark .py-back--nebula {
  min-height: 34px;
  margin-bottom: 14px;
  padding: 7px 13px 7px 11px;
  border: 1px solid rgba(80, 180, 255, 0.35);
  border-radius: 10px;
  color: #c8ddf2;
  background: rgba(10, 20, 40, 0.75);
  box-shadow: 0 5px 16px rgba(10, 28, 58, 0.24), 0 0 12px rgba(99, 102, 241, 0.06);
  font-size: 13px;
  line-height: 18px;
  cursor: pointer;
  transition: transform 0.18s ease, color 0.18s ease, border-color 0.18s ease,
    background-color 0.18s ease, box-shadow 0.18s ease;
}

.py-dark .py-back--nebula::before {
  content: none;
}

.py-back__arrow {
  display: inline-block;
  margin-right: 7px;
  color: #83c7ff;
  font-size: 15px;
  line-height: 1;
  transition: transform 0.18s ease;
}

.py-dark .py-back--nebula:hover {
  transform: translateY(-1px);
  border-color: rgba(103, 196, 255, 0.62);
  color: #dcecff;
  background: rgba(20, 36, 65, 0.88);
  box-shadow: 0 7px 18px rgba(10, 28, 58, 0.3), 0 0 15px rgba(88, 134, 255, 0.16);
}

.py-back--nebula:hover .py-back__arrow {
  transform: translateX(-2px);
}

.py-dark .py-back--nebula:active {
  transform: translateY(0);
  background: rgba(8, 17, 34, 0.88);
  box-shadow: 0 2px 8px rgba(10, 28, 58, 0.2);
}

.py-dark .py-back--nebula:focus-visible {
  outline: 2px solid rgba(105, 190, 255, 0.72);
  outline-offset: 3px;
  border-color: rgba(103, 196, 255, 0.58);
}

@media (prefers-reduced-motion: reduce) {
  .py-dark .py-back--nebula,
  .py-back__arrow {
    transition: none;
  }
}
</style>
