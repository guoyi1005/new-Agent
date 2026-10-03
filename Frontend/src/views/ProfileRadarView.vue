<script setup>
import { computed, onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import AppTabBar from '../components/AppTabBar.vue'
import { getPracticeSummary, getPythonKnowledgeGraph } from '../api/learning'

defineProps({ embedded: { type: Boolean, default: false } })

const loading = ref(true)
const practice = ref(null)
const pythonGraph = ref(null)
const errors = ref([])
const clamp = (value) => Math.max(0, Math.min(100, Number(value) || 0))

const courses = computed(() => Array.isArray(practice.value?.courses) ? practice.value.courses : [])
const skills = computed(() => (Array.isArray(practice.value?.skills) ? practice.value.skills : [])
  .filter((item) => item?.name && Number(item.evidenceCount) > 0)
  .map((item) => ({ ...item, level: item.level == null ? null : clamp(item.level) })))
const scoredSkills = computed(() => skills.value.filter((item) => item.level != null))
const completedChapters = computed(() => courses.value.reduce((sum, course) => sum + (Number(course.completedChapters) || 0), 0))
const totalChapters = computed(() => courses.value.reduce((sum, course) => sum + (Number(course.totalChapters) || 0), 0))
const solvedCount = computed(() => Number(practice.value?.problems?.solvedCount) || 0)
const judgeableCount = computed(() => Number(practice.value?.problems?.judgeableCount) || 0)
const practicedNodes = computed(() => (Array.isArray(pythonGraph.value?.nodes) ? pythonGraph.value.nodes : [])
  .filter((node) => Number(node.attemptCount) > 0)
  .map((node) => ({ ...node, displayName: node.title === node.id ? (node.description || node.title) : node.title }))
  .sort((a, b) => (Number(b.attemptCount) || 0) - (Number(a.attemptCount) || 0)))
const trend = computed(() => (Array.isArray(practice.value?.trend) ? practice.value.trend : [])
  .filter((point) => point?.date && Number(point.eventCount) > 0))
const activeDays = computed(() => trend.value.filter((point) => {
  const day = new Date(`${point.date}T00:00:00`)
  const daysAgo = (Date.now() - day.getTime()) / 86400000
  return Number.isFinite(daysAgo) && daysAgo >= 0 && daysAgo < 30
}).length)
const hasEvidence = computed(() => courses.value.length > 0 || skills.value.length > 0 || solvedCount.value > 0 || practicedNodes.value.length > 0)
const chapterPercent = computed(() => totalChapters.value ? clamp(completedChapters.value / totalChapters.value * 100) : null)
const courseArc = computed(() => 276.46 * (chapterPercent.value || 0) / 100)

const dimensions = computed(() => {
  const pythonScores = practicedNodes.value.filter((node) => node.score != null)
    .map((node) => Number(node.score)).filter(Number.isFinite)
  const average = (values) => values.length ? clamp(values.reduce((sum, value) => sum + value, 0) / values.length) : null
  return [
    { label: '技能进度', value: average(scoredSkills.value.map((skill) => skill.level)), basis: `${scoredSkills.value.length} 项可信技能` },
    { label: '课程章节', value: chapterPercent.value, basis: totalChapters.value ? `${completedChapters.value} / ${totalChapters.value} 章` : '未记录' },
    { label: '题目练习', value: solvedCount.value && judgeableCount.value ? clamp(solvedCount.value / judgeableCount.value * 100) : null, basis: solvedCount.value ? `${solvedCount.value} / ${judgeableCount.value} 道可判题` : '未记录' },
    { label: 'Python知识', value: average(pythonScores), basis: pythonScores.length ? `${pythonScores.length} 个已练知识点均分` : '未记录' },
    { label: '近30日活跃', value: activeDays.value ? clamp(activeDays.value / 30 * 100) : null, basis: activeDays.value ? `${activeDays.value} / 30 天有记录` : '未记录' },
  ]
})
const radarPoint = (index, fraction, radius = 116) => {
  const angle = -Math.PI / 2 + index * Math.PI * 2 / dimensions.value.length
  return { x: 200 + Math.cos(angle) * radius * fraction, y: 200 + Math.sin(angle) * radius * fraction }
}
const radarGrid = computed(() => [0.25, 0.5, 0.75, 1].map((fraction) =>
  dimensions.value.map((_, index) => {
    const point = radarPoint(index, fraction)
    return `${point.x},${point.y}`
  }).join(' ')))
const radarData = computed(() => dimensions.value.flatMap((item, index) => {
  if (item.value == null) return []
  const point = radarPoint(index, item.value / 100)
  return [{ ...point, label: item.label, value: Math.round(item.value) }]
}))
const radarPolygon = computed(() => radarData.value.map((point) => `${point.x},${point.y}`).join(' '))

async function loadEvidence() {
  loading.value = true
  errors.value = []
  practice.value = null
  pythonGraph.value = null
  const [summary, graph] = await Promise.allSettled([getPracticeSummary(true), getPythonKnowledgeGraph()])
  if (summary.status === 'fulfilled') practice.value = summary.value
  else errors.value.push('课程与技能记录暂时无法读取')
  if (graph.status === 'fulfilled') pythonGraph.value = graph.value
  else errors.value.push('Python 练习记录暂时无法读取')
  loading.value = false
}

onMounted(loadEvidence)
</script>

<template>
  <div class="feature-page" :class="{ 'is-embedded': embedded }">
    <AppTabBar v-if="!embedded" />
    <main class="feature-container portrait">
      <header class="portrait-header">
        <div><p class="portrait-overline">EVIDENCE / PROFILE</p><component :is="embedded ? 'h2' : 'h1'">个人画像</component><p>把实际发生的学习与练习，转化为可以追溯的成长图谱。</p></div>
        <RouterLink to="/learning">继续学习 →</RouterLink>
      </header>
      <p class="portrait-method">所有图形仅使用真实记录。空缺维度表示尚无证据，不等于能力为零；这里的百分比不是岗位匹配度。</p>
      <p v-if="errors.length" class="portrait-warning" role="status">{{ errors.join('；') }}。已读取的数据仍可查看。</p>
      <div v-if="loading" class="portrait-empty">正在整理你的学习记录…</div>
      <div v-else-if="!hasEvidence" class="portrait-empty">
        <h3>{{ errors.length === 2 ? '暂时无法读取画像' : '画像正在等待第一份学习证据' }}</h3>
        <p>{{ errors.length === 2 ? '数据连接未成功，请稍后重试。' : '完成课程章节或练习后，图谱会从真实记录中逐步生长。' }}</p>
        <button v-if="errors.length === 2" type="button" @click="loadEvidence">重试读取</button>
        <RouterLink v-else to="/learning">去学习实践 →</RouterLink>
      </div>
      <template v-else>
        <section class="portrait-overview" aria-label="成长证据雷达图">
          <div class="portrait-radar">
            <div class="portrait-section-heading"><span>01 / 成长观察</span><h3>五个维度，看见真实足迹</h3></div>
            <svg viewBox="0 0 400 400" role="img" aria-label="学习证据雷达图；未记录的维度不绘制分数">
              <polygon v-for="(points, index) in radarGrid" :key="index" class="portrait-radar__grid" :points="points" />
              <line v-for="(_, index) in dimensions" :key="index" class="portrait-radar__axis" x1="200" y1="200" :x2="radarPoint(index, 1).x" :y2="radarPoint(index, 1).y" />
              <polygon v-if="radarData.length >= 3" class="portrait-radar__area" :points="radarPolygon" />
              <circle v-for="point in radarData" :key="point.label" class="portrait-radar__point" :cx="point.x" :cy="point.y" r="5"><title>{{ point.label }} {{ point.value }}%</title></circle>
              <text v-for="(item, index) in dimensions" :key="item.label" :x="radarPoint(index, 1, 160).x" :y="radarPoint(index, 1, 160).y" text-anchor="middle" dominant-baseline="middle">{{ item.label }}</text>
            </svg>
            <p v-if="radarData.length < 3" class="portrait-radar__hint">已有 {{ radarData.length }} 个可计算维度；集齐至少 3 个维度后连成画像轮廓。</p>
          </div>
          <div class="portrait-evidence">
            <div class="portrait-section-heading"><span>证据索引</span><h3>每一笔都有来源</h3></div>
            <div v-for="item in dimensions" :key="item.label" class="portrait-evidence__row">
              <span>{{ item.label }}<small>{{ item.basis }}</small></span>
              <strong :class="{ 'is-missing': item.value == null }">{{ item.value == null ? '未记录' : `${Math.round(item.value)}%` }}</strong>
            </div>
          </div>
        </section>

        <section class="portrait-learning" aria-label="课程学习与技能记录">
          <div class="portrait-section-heading"><span>02 / 学习积累</span><h3>从课程到技能</h3><RouterLink to="/growth/courses">查看课程管理 →</RouterLink></div>
          <div class="portrait-learning__body">
            <div class="portrait-course-chart">
              <div class="portrait-ring">
                <svg viewBox="0 0 120 120" aria-hidden="true"><circle class="portrait-ring__track" cx="60" cy="60" r="44" /><circle class="portrait-ring__value" cx="60" cy="60" r="44" :stroke-dasharray="`${courseArc} 276.46`" /></svg>
                <div><strong>{{ completedChapters }}</strong><span>/ {{ totalChapters }} 章</span></div>
              </div>
              <div class="portrait-course-list">
                <p>{{ courses.length }} 门已加入课程 · 章节完成情况</p>
                <RouterLink v-for="course in courses.slice(0, 5)" :key="course.courseId" :to="{ path: '/growth/courses', query: { course: course.courseId } }">
                  <span>{{ course.name }}</span><strong>{{ course.completedChapters || 0 }} / {{ course.totalChapters || 0 }}</strong>
                </RouterLink>
                <span v-if="!courses.length" class="portrait-muted">暂无可信课程记录。</span>
              </div>
            </div>
            <div class="portrait-skill-chart">
              <div class="portrait-inline-head"><h4>技能学习记录</h4><RouterLink to="/growth/skills">查看技能树 →</RouterLink></div>
              <p v-if="!skills.length" class="portrait-muted">尚无可核实的技能学习记录。</p>
              <div v-for="skill in skills.slice(0, 6)" :key="skill.code || skill.name" class="portrait-skill-line">
                <span>{{ skill.name }}</span><div class="portrait-bar" :class="{ 'is-uncertain': skill.level == null }"><i v-if="skill.level != null" :style="{ width: `${skill.level}%` }" /></div>
                <strong>{{ skill.level == null ? `${skill.evidenceCount} 次记录` : `${skill.level}%` }}</strong>
              </div>
              <p v-if="skills.some((skill) => skill.level == null)" class="portrait-footnote">部分历史进度曾受演示数据影响，仅列真实记录次数，不显示不可信分数。</p>
            </div>
          </div>
        </section>

        <section class="portrait-practice" aria-label="练习成绩分布">
          <div class="portrait-section-heading"><span>03 / 练习反馈</span><h3>知识点练习分布</h3><RouterLink to="/growth/skills">查看知识点 →</RouterLink></div>
          <p class="portrait-muted">只显示实际练习过的 Python 知识点；横轴为已记录得分，不代表全部知识掌握程度。</p>
          <div v-if="!practicedNodes.length" class="portrait-empty-line">暂无知识点练习记录。</div>
          <div v-for="node in practicedNodes.slice(0, 8)" :key="node.id" class="portrait-score-row">
            <div><strong>{{ node.displayName }}</strong><small>练习 {{ node.attemptCount }} 次</small></div>
            <div class="portrait-score-axis"><i v-if="node.score != null && Number.isFinite(Number(node.score))" :style="{ left: `${clamp(node.score)}%` }" /></div>
            <span>{{ node.score == null ? '未评分' : `${Math.round(Number(node.score))} 分` }}</span>
          </div>
          <div v-if="practicedNodes.length" class="portrait-scale"><span>0</span><span>50</span><span>100</span></div>
        </section>

        <section class="portrait-timeline" aria-label="最近学习动态">
          <div class="portrait-section-heading"><span>04 / 时间线</span><h3>最近的学习节奏</h3><RouterLink to="/learning">前往学习实践 →</RouterLink></div>
          <p v-if="!trend.length" class="portrait-muted">暂无可核实的学习事件。</p>
          <div v-else class="portrait-timeline__track">
            <div v-for="point in trend.slice(-10)" :key="point.date" class="portrait-day"><i :style="{ height: `${18 + Math.min(54, Number(point.eventCount) * 12)}px` }" /><strong>{{ point.eventCount }} 次</strong><span>{{ point.date.slice(5) }}</span></div>
          </div>
          <p class="portrait-footnote">学习节奏按实际记录事件日期汇总；不生成虚构动态。</p>
        </section>
      </template>
    </main>
  </div>
</template>

<style scoped>
.portrait{color:var(--hp-ink);line-height:1.5}.portrait-header{display:flex;align-items:end;justify-content:space-between;gap:24px;padding:28px 0 18px}.portrait-overline,.portrait-section-heading>span{margin:0;color:var(--hp-green-ink);font-size:11px;font-weight:800;letter-spacing:.13em}.portrait-header h1,.portrait-header h2{margin:8px 0 9px;font-size:clamp(36px,4vw,54px);line-height:1.1}.portrait-header p:last-child{margin:0;color:var(--hp-ink-2)}.portrait a{color:#6f6046;text-decoration:none;font-weight:700}.portrait a:hover{text-decoration:underline}.portrait-header>a{white-space:nowrap}.portrait-method{margin:0 0 28px;padding:13px 0;border-top:1px solid var(--hp-line);border-bottom:1px solid var(--hp-line);color:var(--hp-muted);font-size:13px}.portrait-warning{padding:12px 15px;border:1px solid #e8d9bc;border-radius:12px;background:#fbf3df;color:#735b36}.portrait-empty{padding:62px 18px;text-align:center}.portrait-empty h3{font-size:24px}.portrait-empty p,.portrait-muted{color:var(--hp-muted)}.portrait-empty button{border:0;background:none;color:#6f6046;font:inherit;font-weight:700;cursor:pointer}
.portrait-section-heading{display:flex;align-items:baseline;gap:14px;margin-bottom:24px}.portrait-section-heading h3{margin:0;font-size:clamp(22px,2.4vw,30px)}.portrait-section-heading a{margin-left:auto;font-size:13px;white-space:nowrap}.portrait-overview{display:grid;grid-template-columns:minmax(0,1.15fr) minmax(280px,.85fr);gap:6%;align-items:center;padding:16px 0 48px}.portrait-radar svg{display:block;width:min(440px,100%);margin:auto;overflow:visible}.portrait-radar__grid{fill:none;stroke:#d9e2d8;stroke-width:1}.portrait-radar__axis{stroke:#d9e2d8;stroke-width:1}.portrait-radar__area{fill:rgba(126,170,145,.22);stroke:#6a9d83;stroke-width:2}.portrait-radar__point{fill:#6a9d83;stroke:#fff;stroke-width:2}.portrait-radar text{fill:#706d64;font-size:12px;font-weight:650}.portrait-radar__hint{margin:0;text-align:center;color:var(--hp-muted);font-size:12px}.portrait-evidence__row{display:flex;align-items:center;justify-content:space-between;gap:20px;padding:15px 0;border-bottom:1px solid var(--hp-line)}.portrait-evidence__row span{font-weight:700}.portrait-evidence__row small{display:block;margin-top:4px;color:var(--hp-muted);font-size:12px;font-weight:400}.portrait-evidence__row strong{font-size:19px;white-space:nowrap}.portrait-evidence__row .is-missing{color:#a5a8a0;font-size:12px;font-weight:500}
.portrait-learning,.portrait-practice,.portrait-timeline{padding:42px 0;border-top:1px solid var(--hp-line)}.portrait-learning__body{display:grid;grid-template-columns:1fr 1fr;gap:6%}.portrait-course-chart{display:flex;align-items:center;gap:26px;min-width:0}.portrait-ring{position:relative;flex:0 0 150px;width:150px;height:150px}.portrait-ring svg{width:100%;height:100%;transform:rotate(-90deg)}.portrait-ring circle{fill:none;stroke-width:9}.portrait-ring__track{stroke:#e3e8de}.portrait-ring__value{stroke:#8eae80;stroke-linecap:round}.portrait-ring>div{position:absolute;inset:0;display:flex;flex-direction:column;align-items:center;justify-content:center}.portrait-ring strong{font-size:32px;line-height:1}.portrait-ring span{margin-top:3px;color:var(--hp-muted);font-size:12px}.portrait-course-list{flex:1;min-width:0}.portrait-course-list p{margin:0 0 12px;color:var(--hp-muted);font-size:13px}.portrait-course-list a{display:flex;justify-content:space-between;gap:12px;padding:9px 0;border-bottom:1px solid var(--hp-line);font-size:13px}.portrait-course-list a span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.portrait-course-list a strong{white-space:nowrap}.portrait-inline-head{display:flex;justify-content:space-between;gap:12px;align-items:baseline}.portrait-inline-head h4{margin:0 0 14px;font-size:18px}.portrait-inline-head a{font-size:12px}.portrait-skill-line{display:grid;grid-template-columns:110px 1fr 80px;gap:12px;align-items:center;margin:14px 0;font-size:13px}.portrait-skill-line>span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.portrait-skill-line strong{text-align:right;font-size:12px}.portrait-bar{height:8px;border-radius:99px;background:#e7e8e1}.portrait-bar i{display:block;height:100%;border-radius:inherit;background:#86a99d}.portrait-bar.is-uncertain{background:repeating-linear-gradient(90deg,#deded6 0 6px,transparent 6px 10px)}.portrait-footnote{margin:14px 0 0;color:var(--hp-muted);font-size:12px;line-height:1.6}
.portrait-practice>.portrait-muted{margin:-10px 0 22px;font-size:13px}.portrait-score-row{display:grid;grid-template-columns:minmax(160px,1fr) minmax(160px,2fr) 80px;align-items:center;gap:22px;padding:11px 0}.portrait-score-row>div:first-child{display:flex;flex-direction:column;gap:2px}.portrait-score-row strong{overflow-wrap:anywhere;font-size:13px}.portrait-score-row small{color:var(--hp-muted);font-size:11px}.portrait-score-row>span{text-align:right;font-size:12px}.portrait-score-axis{position:relative;height:2px;background:#dce4dd}.portrait-score-axis:before,.portrait-score-axis:after{content:'';position:absolute;top:-3px;width:1px;height:8px;background:#c9d4cb}.portrait-score-axis:after{right:0}.portrait-score-axis i{position:absolute;top:-5px;width:12px;height:12px;border:2px solid #fff;border-radius:50%;background:#8ca89b;transform:translateX(-50%);box-shadow:0 0 0 1px #8ca89b}.portrait-scale{display:flex;justify-content:space-between;margin-left:calc((100% - 160px) / 3);padding-right:80px;color:var(--hp-muted);font-size:11px}.portrait-empty-line{padding:20px 0;color:var(--hp-muted)}
.portrait-timeline__track{display:flex;gap:14px;align-items:end;overflow-x:auto;padding:24px 0 4px}.portrait-day{display:flex;flex:1 0 58px;flex-direction:column;align-items:center;gap:5px;min-width:58px}.portrait-day i{width:24px;border-radius:8px 8px 3px 3px;background:#a6bea2}.portrait-day strong{font-size:11px}.portrait-day span{color:var(--hp-muted);font-size:11px}.feature-page.is-embedded{min-height:0;padding-top:0;background:transparent}.is-embedded .feature-container{padding-top:12px}
@media(max-width:900px){.portrait-overview,.portrait-learning__body{grid-template-columns:1fr;gap:30px}.portrait-evidence{max-width:560px}.portrait-course-chart{justify-content:flex-start}}@media(max-width:680px){.portrait-header{align-items:flex-start;flex-direction:column}.portrait-section-heading{align-items:flex-start;flex-wrap:wrap;gap:7px}.portrait-section-heading h3{width:100%}.portrait-section-heading a{margin-left:0}.portrait-course-chart{align-items:flex-start;flex-direction:column;gap:14px}.portrait-ring{align-self:center}.portrait-course-list{width:100%}.portrait-score-row{grid-template-columns:1fr 1fr;gap:8px}.portrait-score-axis{grid-column:1 / -1;grid-row:2}.portrait-score-row>span{grid-column:2;grid-row:1}.portrait-scale{margin:0;padding:0}.portrait-skill-line{grid-template-columns:84px 1fr 72px;gap:7px}}
</style>
