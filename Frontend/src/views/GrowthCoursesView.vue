<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import {
  enrollCampusCourse,
  getCampusCourse,
  getCampusCourses,
  unenrollCampusCourse,
  updateCampusCourseProgress,
} from '../api/campusCourse'
import { listExamHistory, listExamPapers, startExam } from '../api/exam'
import { getContentTags, getPracticeSummary } from '../api/learning'
import { TARGET_JOB_STORAGE_KEY } from '../data_tmp/jobCatalog'

const route = useRoute()
const router = useRouter()
const courses = ref([])
const examPapers = ref([])
const courseListRef = ref(null)
const courseTags = ref(new Map())
const practiceCourses = ref(new Map())
const targetJob = (() => {
  try { return localStorage.getItem(TARGET_JOB_STORAGE_KEY) || '' } catch { return '' }
})()
const selectedCourseId = ref(null)
const detail = ref(null)
const query = ref('')
const filter = ref('all')
const loading = ref(true)
const detailLoading = ref(false)
const saving = ref(false)
const startingPaperId = ref(null)
const error = ref('')
const actionError = ref('')
const historyPaperId = ref(null)
const historyRecords = ref([])
const historyLoading = ref(false)
const historyError = ref('')
let detailRequest = 0

const enrolledCount = computed(() => courses.value.filter((course) => course.enrolled).length)
const matchedCount = computed(() => courses.value.filter((course) => matchesTargetJob(course.id)).length)
const visibleCourses = computed(() => courses.value.filter((course) => {
  const keyword = query.value.trim().toLowerCase()
  return (filter.value === 'all' || (filter.value === 'enrolled' ? course.enrolled
    : filter.value === 'matched' ? matchesTargetJob(course.id) : !course.enrolled))
    && (!keyword || `${course.name} ${course.bookTitle || ''} ${course.teacherName || ''}`.toLowerCase().includes(keyword))
}).sort((left, right) => Number(matchesTargetJob(right.id)) - Number(matchesTargetJob(left.id))))
const selectedSummary = computed(() => courses.value.find((course) => course.id === selectedCourseId.value))
const selectedTags = computed(() => courseTags.value.get(detail.value?.id) || null)
const selectedPractice = computed(() => practiceCourses.value.get(detail.value?.id) || null)
const paperById = computed(() => new Map(examPapers.value.map((paper) => [paper.id, paper])))
const completedChapters = computed(() => (detail.value?.chapters || []).filter((chapter) => chapter.completed).length)

function matchesTargetJob(courseId) {
  return Boolean(targetJob && courseTags.value.get(courseId)?.jobs?.some((job) => job.name === targetJob))
}

async function load() {
  loading.value = true
  error.value = ''
  const [courseResult, examResult, tagsResult, practiceResult] = await Promise.allSettled([
    getCampusCourses(),
    listExamPapers({ page: 0, size: 100 }),
    getContentTags('COURSE'),
    getPracticeSummary(),
  ])
  courseTags.value = new Map((tagsResult.status === 'fulfilled' && Array.isArray(tagsResult.value) ? tagsResult.value : [])
    .filter((item) => item.sourceId != null).map((item) => [item.sourceId, item]))
  practiceCourses.value = new Map((practiceResult.status === 'fulfilled' && Array.isArray(practiceResult.value?.courses) ? practiceResult.value.courses : [])
    .map((item) => [item.courseId, item]))
  if (courseResult.status === 'fulfilled') {
    courses.value = Array.isArray(courseResult.value?.data) ? courseResult.value.data : []
    if (!courses.value.some((course) => course.id === selectedCourseId.value)) {
      const requestedId = Number(route.query.course)
      selectedCourseId.value = courses.value.find((course) => course.id === requestedId)?.id
        || courses.value.find((course) => course.enrolled)?.id || courses.value[0]?.id || null
    }
  } else {
    courses.value = []
    error.value = courseResult.reason?.message || '课程加载失败'
  }
  if (examResult.status === 'fulfilled') {
    examPapers.value = Array.isArray(examResult.value?.content) ? examResult.value.content : []
  } else {
    examPapers.value = []
  }
  loading.value = false
  await nextTick()
  const list = courseListRef.value
  const selected = list?.querySelector('.courses-item.selected')
  if (selected) list.scrollTop = Math.max(0, selected.offsetTop - list.offsetTop - list.clientHeight / 3)
}

async function loadDetail(id) {
  const request = ++detailRequest
  detail.value = null
  actionError.value = ''
  if (id == null) return
  detailLoading.value = true
  try {
    const response = await getCampusCourse(id)
    if (request === detailRequest) detail.value = response.data
  } catch (cause) {
    if (request === detailRequest) actionError.value = cause.message || '课程详情加载失败'
  } finally {
    if (request === detailRequest) detailLoading.value = false
  }
}

watch(selectedCourseId, (id) => {
  historyPaperId.value = null
  historyRecords.value = []
  loadDetail(id)
})

async function toggleHistory(paperId) {
  if (historyPaperId.value === paperId) {
    historyPaperId.value = null
    return
  }
  historyPaperId.value = paperId
  historyRecords.value = []
  historyError.value = ''
  historyLoading.value = true
  try {
    const records = await listExamHistory(paperId)
    if (historyPaperId.value === paperId) historyRecords.value = Array.isArray(records) ? records : []
  } catch (cause) {
    if (historyPaperId.value === paperId) historyError.value = cause.message || '成绩记录加载失败'
  } finally {
    if (historyPaperId.value === paperId) historyLoading.value = false
  }
}

async function changeEnrollment() {
  if (!selectedSummary.value || saving.value) return
  const course = selectedSummary.value
  saving.value = true
  actionError.value = ''
  try {
    if (course.enrolled) await unenrollCampusCourse(course.id)
    else await enrollCampusCourse(course.id)
    await load()
    await loadDetail(course.id)
  } catch (cause) {
    actionError.value = cause.message || '课程状态保存失败'
  } finally {
    saving.value = false
  }
}

async function toggleChapter(chapter) {
  if (!detail.value?.enrolled || saving.value) return
  saving.value = true
  actionError.value = ''
  try {
    const response = await updateCampusCourseProgress(detail.value.id, chapter.id, !chapter.completed)
    detail.value = response.data
    const summaries = await getCampusCourses()
    courses.value = Array.isArray(summaries.data) ? summaries.data : courses.value
    try {
      const practice = await getPracticeSummary()
      practiceCourses.value = new Map((Array.isArray(practice?.courses) ? practice.courses : [])
        .map((item) => [item.courseId, item]))
    } catch {
      practiceCourses.value = new Map()
    }
  } catch (cause) {
    actionError.value = cause.message || '章节进度保存失败'
  } finally {
    saving.value = false
  }
}

async function enterExam(exam) {
  if (!exam.paperId || startingPaperId.value != null) return
  startingPaperId.value = exam.paperId
  actionError.value = ''
  try {
    const attempt = await startExam(exam.paperId)
    router.push(`/mine/papers/attempts/${attempt.id}`)
  } catch (cause) {
    actionError.value = cause.message || '暂时无法进入试卷'
  } finally {
    startingPaperId.value = null
  }
}

onMounted(load)
</script>

<template>
  <div class="courses-page">
    <main class="courses-shell">
      <header class="courses-hero"><div><p>MY COURSES</p><h2>课程管理</h2><span>与学习实践使用同一批校内课程；在这里查看技能关联、学习进度与试卷记录。</span></div><div class="courses-summary"><strong>{{ enrolledCount }}</strong><span>已加入课程</span></div></header>
      <p v-if="targetJob && matchedCount" class="courses-match-summary">目标岗位：{{ targetJob }} · {{ matchedCount }} 门关联课程</p>

      <div v-if="error" class="courses-notice" role="alert">{{ error }} <button type="button" @click="load">重试</button></div>
      <p v-if="loading" class="courses-empty">正在读取课程和试卷…</p>
      <div v-else class="courses-layout">
        <section ref="courseListRef" class="courses-list" aria-label="课程列表">
          <div class="courses-list-head"><h2>课程列表</h2><span>{{ visibleCourses.length }} 门</span></div>
          <div class="courses-filters"><input v-model="query" type="search" placeholder="搜索课程或教师" aria-label="搜索课程或教师"><select v-model="filter" aria-label="筛选课程"><option value="all">全部课程</option><option v-if="targetJob" value="matched">匹配目标岗位</option><option value="enrolled">我加入的</option><option value="available">可加入的</option></select></div>
          <p v-if="!visibleCourses.length" class="courses-empty">{{ courses.length ? '没有符合条件的课程。' : '当前没有可管理的课程。' }}</p>
          <button v-for="course in visibleCourses" :key="course.id" type="button" class="courses-item" :class="{ selected: selectedCourseId === course.id }" :aria-label="`查看课程${course.name}`" @click="selectedCourseId = course.id">
            <span class="courses-item-top"><strong>{{ course.name }}</strong><em>{{ course.enrolled ? '已加入' : '可加入' }}</em></span>
            <small>{{ course.teacherName || course.ownerName || '课程教师待公布' }} · {{ course.chapterCount }} 章 · {{ course.examCount }} 场考试</small>
            <span v-if="matchesTargetJob(course.id)" class="courses-match-tag">匹配目标岗位</span>
            <span v-if="courseTags.get(course.id)?.skills?.length" class="courses-item-skills">{{ courseTags.get(course.id).skills.slice(0, 3).map((skill) => skill.name).join(' · ') }}</span>
            <span class="courses-progress"><i :style="{ width: `${course.progressPercent || 0}%` }" /></span><small>学习进度 {{ course.progressPercent || 0 }}%</small>
          </button>
        </section>

        <section class="courses-detail" aria-label="课程详情">
          <p v-if="detailLoading" class="courses-empty">正在读取课程详情…</p>
          <p v-else-if="!detail" class="courses-empty">选择左侧课程，查看章节、进度与关联试卷。</p>
          <template v-else>
            <div class="courses-detail-head"><div><p>COURSE OVERVIEW</p><h2>{{ detail.name }}</h2><span>{{ detail.bookTitle || detail.description || '暂无课程简介' }}</span><small>{{ detail.teacherName || detail.ownerName || '课程教师待公布' }} · {{ detail.semester || '学期未标注' }}</small></div><button type="button" :disabled="saving" @click="changeEnrollment">{{ saving ? '正在保存…' : detail.enrolled ? '退出课程' : '加入课程' }}</button></div>
            <div class="courses-facts"><div><strong>{{ selectedPractice?.progressPercent ?? detail.progressPercent ?? 0 }}%</strong><span>学习实践进度</span></div><div><strong>{{ selectedPractice?.completedChapters ?? completedChapters }}/{{ selectedPractice?.totalChapters ?? detail.chapters?.length ?? 0 }}</strong><span>完成章节</span></div><div><strong>{{ detail.exams?.length || 0 }}</strong><span>关联试卷</span></div></div>
            <div v-if="actionError" class="courses-notice" role="alert">{{ actionError }}</div>

            <section v-if="selectedTags" class="courses-detail-section courses-tags-section" aria-label="课程技能与岗位关联">
              <div class="courses-section-title"><h3>课程关联</h3><span>技能与岗位信息来自学习实践</span></div>
              <div v-if="selectedTags.skills?.length" class="courses-tag-row"><span class="courses-tag-label">学习技能</span><span v-for="skill in selectedTags.skills" :key="skill.code || skill.name" class="courses-skill-tag">{{ skill.name }}</span></div>
              <p v-if="selectedTags.prerequisites?.length" class="courses-tag-note">先修技能：{{ selectedTags.prerequisites.map((skill) => skill.name).join('、') }}</p>
              <p v-if="selectedTags.jobs?.length" class="courses-tag-note">适合岗位：{{ selectedTags.jobs.map((job) => job.name).join('、') }}</p>
            </section>

            <section class="courses-detail-section"><div class="courses-section-title"><h3>课程章节</h3><RouterLink :to="`/courses/${detail.id}`">打开完整课程 →</RouterLink></div>
              <p v-if="!detail.chapters?.length" class="courses-empty">尚无章节。</p>
              <div v-for="(chapter, index) in detail.chapters || []" :key="chapter.id" class="courses-chapter"><span>{{ String(index + 1).padStart(2, '0') }}</span><div><strong>{{ chapter.title }}</strong><small>{{ chapter.summary || (chapter.required ? '必修章节' : '选修章节') }}</small></div><button type="button" :disabled="!detail.enrolled || saving" :aria-label="`${chapter.completed ? '取消完成' : '标记完成'}${chapter.title}`" @click="toggleChapter(chapter)">{{ chapter.completed ? '已完成 ✓' : detail.enrolled ? '标记完成' : '加入后可记录' }}</button></div>
            </section>

            <section class="courses-detail-section"><div class="courses-section-title"><h3>关联试卷</h3><span>从试卷管理读取考试记录</span></div>
              <p v-if="!detail.exams?.length" class="courses-empty">这门课程暂无关联试卷。</p>
              <template v-for="exam in detail.exams || []" :key="exam.id"><article class="courses-exam"><div><strong>{{ exam.title }}</strong><small>{{ exam.questionCount }} 题 · {{ exam.durationMinutes }} 分钟 · {{ exam.chapterScope || '全课程' }}</small><small v-if="paperById.has(exam.paperId)">已完成 {{ paperById.get(exam.paperId).attemptCount }} 次</small></div><div class="courses-exam-actions"><button v-if="exam.published && exam.paperId" type="button" :disabled="startingPaperId != null" @click="enterExam(exam)">{{ startingPaperId === exam.paperId ? '正在进入…' : '进入考试' }}</button><button v-if="exam.paperId && paperById.has(exam.paperId)" type="button" class="courses-history-toggle" :aria-expanded="historyPaperId === exam.paperId" @click="toggleHistory(exam.paperId)">{{ historyPaperId === exam.paperId ? '收起成绩' : '查看成绩' }} →</button><span v-if="!exam.published">尚未发布</span></div></article><div v-if="historyPaperId === exam.paperId" class="courses-history"><p v-if="historyLoading">正在读取成绩…</p><p v-else-if="historyError">{{ historyError }}</p><p v-else-if="!historyRecords.length">暂无已提交的考试记录。</p><RouterLink v-for="record in historyRecords" v-else :key="record.id" :to="`/mine/papers/results/${record.id}`"><span>第 {{ record.attemptNo }} 次 · {{ String(record.submittedAt || '').replace('T', ' ').slice(0, 16) }}</span><strong>{{ record.objectiveScore ?? 0 }} / {{ record.objectiveTotalScore ?? 100 }} 分</strong></RouterLink></div></template>
            </section>
          </template>
        </section>
      </div>
    </main>
  </div>
</template>

<style scoped>
.courses-page{color:var(--hp-ink)}.courses-shell{width:min(1360px,calc(100% - 48px));margin:auto;padding:0 0 72px}
.courses-hero{display:flex;justify-content:space-between;align-items:end;gap:20px;padding:40px 0 30px}.courses-hero p,.courses-detail-head p{margin:0;color:var(--hp-yellow-ink);font-size:11px;font-weight:800;letter-spacing:.12em}.courses-hero h2{margin:8px 0;font-size:clamp(34px,4vw,52px)}.courses-hero span{color:var(--hp-ink-2)}.courses-summary{display:grid;min-width:125px;padding:14px 22px;border:1px solid var(--hp-line);border-radius:18px;background:var(--hp-yellow);text-align:center}.courses-summary strong{font-size:25px}.courses-summary span{font-size:12px}.courses-match-summary{margin:-12px 0 24px;color:var(--hp-yellow-ink);font-size:13px;font-weight:700}
.courses-layout{display:grid;grid-template-columns:minmax(290px,360px) minmax(0,1fr);align-items:start;gap:20px}.courses-list,.courses-detail{border:1px solid var(--hp-line);border-radius:24px;background:var(--hp-surface);box-shadow:var(--hp-shadow-sm)}.courses-list{position:relative;max-height:min(72vh,780px);overflow-y:auto;padding:22px;scrollbar-width:thin}.courses-list-head,.courses-section-title{display:flex;justify-content:space-between;align-items:center;gap:12px}.courses-list-head h2,.courses-section-title h3{margin:0;font-size:17px}.courses-list-head span,.courses-section-title span{color:var(--hp-muted);font-size:12px}.courses-filters{display:flex;gap:8px;margin:18px 0}.courses-filters input,.courses-filters select{min-width:0;padding:10px 12px;border:1px solid var(--hp-line-strong);border-radius:10px;background:#fff;font:inherit;font-size:12px}.courses-filters input{flex:1;width:58%}.courses-filters select{width:40%}
.courses-item{display:grid;gap:8px;width:100%;margin-top:10px;padding:15px;border:1px solid var(--hp-line);border-radius:15px;background:#fff;text-align:left;font:inherit;cursor:pointer}.courses-item:hover,.courses-item.selected{border-color:#d4b765;background:#fffbef}.courses-item-top{display:flex;justify-content:space-between;align-items:start;gap:8px}.courses-item strong{font-size:14px}.courses-item em{flex:0 0 auto;color:#8b7133;font-size:11px;font-style:normal}.courses-item small,.courses-chapter small,.courses-exam small{display:block;color:var(--hp-muted);font-size:11px;line-height:1.5}.courses-progress{height:6px;overflow:hidden;border-radius:20px;background:#ede9df}.courses-progress i{display:block;height:100%;border-radius:20px;background:#dfbc5a;transition:width .2s ease}
.courses-match-tag{justify-self:start;padding:4px 8px;border-radius:999px;color:var(--hp-green-ink);background:var(--hp-green);font-size:11px;font-weight:700}.courses-item-skills{color:var(--hp-ink-2);font-size:11px}.courses-tags-section{padding:18px;border:1px solid var(--hp-line);border-radius:16px;background:var(--hp-surface-2)}.courses-tag-row{display:flex;align-items:center;flex-wrap:wrap;gap:7px}.courses-tag-label,.courses-tag-note{color:var(--hp-muted);font-size:12px}.courses-skill-tag{padding:6px 10px;border:1px solid var(--hp-line);border-radius:999px;background:#fff;color:var(--hp-ink-2);font-size:12px}.courses-tag-note{margin:10px 0 0;line-height:1.6}
.courses-detail{min-height:300px;padding:27px}.courses-detail-head{display:flex;justify-content:space-between;align-items:start;gap:20px}.courses-detail-head h2{margin:6px 0;font-size:27px}.courses-detail-head span{display:block;color:var(--hp-ink-2);font-size:13px;line-height:1.6}.courses-detail-head small{display:block;margin-top:8px;color:var(--hp-muted)}.courses-detail-head button,.courses-exam-actions button{flex:0 0 auto;padding:10px 16px;border:1px solid #d2b95e;border-radius:11px;background:var(--hp-yellow);color:var(--hp-ink);font:inherit;font-size:13px;font-weight:700;cursor:pointer}.courses-detail button:disabled{opacity:.55;cursor:not-allowed}.courses-facts{display:grid;grid-template-columns:repeat(3,1fr);gap:10px;margin:24px 0}.courses-facts div{display:grid;gap:4px;padding:15px;border:1px solid var(--hp-line);border-radius:14px;background:#faf9f5}.courses-facts strong{font-size:20px}.courses-facts span{color:var(--hp-muted);font-size:11px}
.courses-detail-section{margin-top:30px}.courses-section-title{margin-bottom:12px}.courses-section-title a,.courses-exam-actions a{color:#8a6c29;font-size:12px;font-weight:700;text-decoration:none}.courses-chapter,.courses-exam{display:flex;align-items:center;gap:14px;padding:14px 0;border-top:1px solid var(--hp-line)}.courses-chapter>span{color:#b49a5e;font-size:12px;font-weight:800}.courses-chapter>div,.courses-exam>div:first-child{flex:1;min-width:0}.courses-chapter strong,.courses-exam strong{display:block;margin-bottom:4px;font-size:13px}.courses-chapter button{padding:8px 12px;border:1px solid var(--hp-line-strong);border-radius:999px;background:#fff;color:var(--hp-ink-2);font:inherit;font-size:12px;cursor:pointer}.courses-exam-actions{display:grid;justify-items:end;gap:7px}.courses-exam-actions span{color:var(--hp-muted);font-size:12px}.courses-empty,.courses-notice{margin:14px 0;padding:18px;border-radius:12px;background:#faf8f2;color:var(--hp-ink-2);font-size:13px}.courses-notice{border:1px solid #ead9ba;background:#fff8e8}.courses-notice button{border:0;background:none;color:#8a6c29;font:inherit;font-weight:700;cursor:pointer}
.courses-exam-actions .courses-history-toggle{padding:0;border:0;background:none;color:#8a6c29;font-size:12px}.courses-history{display:grid;gap:7px;margin:0 0 14px;padding:12px;border-radius:12px;background:#faf8f2}.courses-history p{margin:0;color:var(--hp-muted);font-size:12px}.courses-history a{display:flex;justify-content:space-between;gap:12px;color:var(--hp-ink-2);font-size:12px;text-decoration:none}.courses-history a strong{color:#8a6c29}
@media(max-width:900px){.courses-layout{grid-template-columns:1fr}.courses-list{max-height:440px;overflow:auto}}
@media(max-width:680px){.courses-shell{width:calc(100% - 30px)}.courses-hero,.courses-detail-head{align-items:start;flex-direction:column}.courses-list,.courses-detail{padding:20px}.courses-facts{grid-template-columns:1fr 1fr}.courses-chapter,.courses-exam{align-items:flex-start;flex-wrap:wrap}.courses-chapter button,.courses-exam-actions{margin-left:30px}.courses-summary{align-self:flex-start}}
@media(prefers-reduced-motion:reduce){.courses-progress i{transition:none}}
</style>
