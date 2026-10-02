<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import AppTabBar from '../components/AppTabBar.vue'
import { enrollCampusCourse, getCampusCourse, updateCampusCourseProgress } from '../api/campusCourse'
import { getContentTags } from '../api/learning'

const route = useRoute()
const router = useRouter()
const course = ref(null)
const loading = ref(true)
const error = ref('')
const activeChapterId = ref(null)
const saving = ref(false)
const tags = ref(null)
const chapterTags = ref({})
const actionError = ref('')
const enrolling = ref(false)
const activeVideoPage = ref(1)

const completedCount = computed(() => (course.value?.chapters || []).filter((item) => item.completed).length)

// B 站官方外链播放器：只引用 BV 号，视频内容始终在 B 站
const bilibiliPlayerUrl = computed(() => {
  const bvid = course.value?.videoBvid
  if (!bvid) return ''
  return `https://player.bilibili.com/player.html?bvid=${bvid}&page=${activeVideoPage.value}&high_quality=1&danmaku=0&autoplay=0`
})
const bilibiliWatchUrl = computed(() => {
  const bvid = course.value?.videoBvid
  return bvid ? `https://www.bilibili.com/video/${bvid}` : ''
})

async function loadCourse() {
  loading.value = true
  error.value = ''
  try {
    const response = await getCampusCourse(route.params.courseId)
    course.value = response.data
    activeChapterId.value ||= course.value?.chapters?.find((item) => !item.completed)?.id || null
    try {
      const tagList = await getContentTags('COURSE', route.params.courseId)
      tags.value = Array.isArray(tagList) && tagList.length ? tagList[0] : null
    } catch {
      tags.value = null
    }
    try {
      const chapterList = await getContentTags('COURSE_CHAPTER')
      const map = {}
      if (Array.isArray(chapterList)) {
        chapterList.forEach((item) => {
          if (item && item.sourceId != null) map[item.sourceId] = item.skills || []
        })
      }
      chapterTags.value = map
    } catch {
      chapterTags.value = {}
    }
  } catch (requestError) {
    error.value = requestError.message || '课程加载失败'
  } finally {
    loading.value = false
  }
}

function chapterSkills(chapterId) {
  return chapterTags.value[chapterId] || []
}
function difficultyLabel(value) {
  return { BEGINNER: '入门', INTERMEDIATE: '进阶', ADVANCED: '挑战' }[value] || ''
}

// 每一章都能跳去 B 站：已映射分P的跳到对应那一集，未映射的跳到视频页（合集/分P列表在页面里选）
function chapterWatchUrl(chapter) {
  const bvid = course.value?.videoBvid
  if (!bvid) return ''
  const page = chapter?.videoPage
  return page && page > 1
    ? `https://www.bilibili.com/video/${bvid}?p=${page}`
    : `https://www.bilibili.com/video/${bvid}`
}

function playChapterVideo(chapter) {
  if (!chapter?.videoPage) return
  activeVideoPage.value = chapter.videoPage
  const frame = document.querySelector('.course-video')
  if (frame && typeof frame.scrollIntoView === 'function') {
    frame.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

async function joinCourse() {
  if (enrolling.value) return
  enrolling.value = true
  actionError.value = ''
  try {
    await enrollCampusCourse(course.value.id)
    await loadCourse()
  } catch (requestError) {
    actionError.value = requestError.message || '加入课程失败，请稍后重试'
  } finally {
    enrolling.value = false
  }
}

async function toggleProgress(chapter) {
  if (saving.value) return
  actionError.value = ''
  saving.value = true
  try {
    const response = await updateCampusCourseProgress(course.value.id, chapter.id, !chapter.completed)
    course.value = response.data
  } catch (requestError) {
    actionError.value = requestError.message || '学习进度保存失败，请稍后重试'
  } finally {
    saving.value = false
  }
}

onMounted(loadCourse)
</script>

<template>
  <div class="course-page">
    <AppTabBar />
    <main class="course-shell">
      <button class="back-link" type="button" @click="router.back()">← 返回校园课程</button>

      <p v-if="loading" class="state">正在打开课程书…</p>
      <p v-else-if="error" class="state">{{ error }}</p>
      <template v-else-if="course">
        <header class="course-hero">
          <div class="book-cover">课</div>
          <div class="course-hero__copy">
            <p class="eyebrow">CAMPUS COURSE</p>
            <h1>{{ course.name }}</h1>
            <strong>{{ course.bookTitle }}</strong>
            <span>{{ course.ownerName }}</span>
            <div v-if="tags" class="course-hero__tags"><span v-for="skill in tags.skills" :key="skill.code" class="hero-tag" :class="{ 'is-primary': skill.primary }">{{ skill.name }}</span><span v-if="difficultyLabel(tags.difficulty)" class="hero-tag hero-tag--level">{{ difficultyLabel(tags.difficulty) }}</span></div>
            <p v-if="tags && tags.jobs.length" class="course-hero__meta">适合岗位：{{ tags.jobs.map((job) => job.name).join('、') }}</p>
            <p v-if="tags && tags.recommendationOrder" class="course-hero__meta">建议学习顺序：第 {{ tags.recommendationOrder }} 步</p>
            <p v-if="tags && tags.prerequisites.length" class="course-hero__meta">先修技能：{{ tags.prerequisites.map((skill) => skill.name).join('、') }}</p>
          </div>
          <div class="course-hero__summary">
            <span>学习进度</span>
            <strong>{{ course.progressPercent }}%</strong>
            <small>{{ completedCount }}/{{ course.chapters.length }} 个章节已完成</small>
            <button v-if="course.enrolled === false" class="hero-join" type="button" :disabled="enrolling" @click="joinCourse">{{ enrolling ? '正在加入…' : '加入课程' }}</button>
            <small v-else-if="course.enrolled === true" class="hero-joined">已加入课程</small>
          </div>
        </header>

        <div class="course-layout">
          <section class="course-main">
            <img
              v-if="course.displayImageUrl || course.coverUrl"
              class="course-display-image"
              :src="course.displayImageUrl || course.coverUrl"
              :alt="`${course.name}展示图`"
            >

            <section v-if="course.videoBvid" class="course-video course-section">
              <div class="section-title"><div><h2>课程视频</h2><span>视频来自 B 站，使用官方播放器在站内播放</span></div><a :href="bilibiliWatchUrl" target="_blank" rel="noreferrer noopener">在 B 站打开 ↗</a></div>
              <div class="video-frame">
                <iframe :src="bilibiliPlayerUrl" title="课程视频" allowfullscreen scrolling="no" frameborder="0"></iframe>
              </div>
              <p class="video-note">视频版权归原 UP 主所有，本平台仅引用 B 站官方播放器，不下载、不转存。</p>
            </section>

            <section class="course-section">
              <div class="section-title"><div><h2>课程目录</h2><span>按章节完成阅读和练习</span></div><span>{{ course.chapters.length }} 章</span></div>
              <article v-for="(chapter, index) in course.chapters" :key="chapter.id" class="chapter course-card">
                <button class="chapter-head" type="button" @click="activeChapterId = activeChapterId === chapter.id ? null : chapter.id">
                  <span class="chapter-number">{{ chapter.completed ? '✓' : index + 1 }}</span>
                  <span><strong>{{ chapter.title }}</strong><small>{{ chapter.estimatedMinutes || 30 }} 分钟 · {{ chapter.required ? '必修' : '选修' }}</small><span v-if="chapterSkills(chapter.id).length" class="chapter-skills"><i v-for="skill in chapterSkills(chapter.id)" :key="skill.code" class="chapter-skill">{{ skill.name }}</i></span></span>
                  <em>{{ activeChapterId === chapter.id ? '收起' : '阅读' }}</em>
                </button>
                <div v-if="activeChapterId === chapter.id" class="chapter-body">
                  <p>{{ chapter.summary }}</p>
                  <div class="chapter-content">{{ chapter.content || '管理员暂未录入本章正文。' }}</div>
                  <a v-if="chapter.resourceUrl" :href="chapter.resourceUrl" target="_blank" rel="noreferrer">打开附加资料</a>
                  <div v-if="course.videoBvid" class="chapter-actions">
                    <button v-if="chapter.videoPage" type="button" class="chapter-play" @click="playChapterVideo(chapter)">播放本集视频</button>
                    <a class="chapter-watch" :href="chapterWatchUrl(chapter)" target="_blank" rel="noreferrer noopener">{{ chapter.videoPage ? '去 B 站观看本集 ↗' : '去 B 站观看完整课程 ↗' }}</a>
                  </div>
                  <p v-if="actionError" class="action-error">{{ actionError }}<button v-if="course.enrolled === false" type="button" class="action-error__join" @click="joinCourse">先加入课程</button></p>
                </div>
              </article>
            </section>
          </section>

          <aside class="course-sidebar">
            <section class="course-card progress-panel">
              <div class="sidebar-head"><h2>学习进度</h2><strong>{{ course.progressPercent }}%</strong></div>
              <progress :value="course.progressPercent" max="100" />
              <small>{{ completedCount }}/{{ course.chapters.length }} 个章节已完成</small>
            </section>

            <section class="course-card exam-panel">
              <div class="sidebar-head"><h2>课程考试</h2><span>{{ course.exams.length }} 场</span></div>
              <button v-for="exam in course.exams" :key="exam.id" class="exam-card" type="button" @click="router.push('/mine/papers')">
                <span>考</span>
                <div><strong>{{ exam.title }}</strong><small>{{ exam.chapterScope || '全部章节' }} · {{ exam.questionCount }} 题 · {{ exam.durationMinutes }} 分钟</small></div>
                <em>进入考试</em>
              </button>
              <p v-if="!course.exams.length" class="state-card">当前课程暂无已发布考试</p>
            </section>
          </aside>
        </div>
      </template>
    </main>
  </div>
</template>

<style scoped>
.course-page {
  min-height: 100vh;
  color: var(--hp-ink);
  background: var(--hp-bg);
  font-family: Inter, 'Segoe UI', system-ui, -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

.course-page *,
.course-page *::before,
.course-page *::after {
  box-sizing: border-box;
}

.course-shell {
  width: min(1280px, calc(100% - 48px));
  margin: 0 auto;
  padding: 96px 0 80px;
}

.back-link {
  margin-bottom: 14px;
  padding: 8px 12px;
  border: 1px solid var(--hp-line-strong);
  border-radius: 999px;
  color: var(--hp-ink-2);
  background: var(--hp-surface);
  font-size: 13px;
  font-weight: 650;
  cursor: pointer;
}

.course-hero,
.course-card {
  border: 1px solid var(--hp-line);
  background: var(--hp-surface);
  box-shadow: var(--hp-shadow-sm);
}

.course-hero {
  display: grid;
  grid-template-columns: 108px minmax(0, 1fr) auto;
  align-items: center;
  gap: 24px;
  padding: 28px 30px;
  border-color: #e4ebf2;
  border-radius: var(--hp-r-lg);
  background: var(--hp-tint);
}

.book-cover {
  display: grid;
  width: 88px;
  height: 116px;
  place-items: center;
  border: 1px solid #d3e0e9;
  border-radius: 10px 16px 16px 10px;
  color: var(--hp-blue-ink);
  background: #e6eef3;
  box-shadow: inset 5px 0 #cad8e1;
  font-size: 29px;
  font-weight: 800;
}

.course-hero__copy {
  display: grid;
  gap: 7px;
  min-width: 0;
}

.eyebrow {
  margin: 0;
  color: var(--hp-blue-ink);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: .13em;
}

.course-hero h1 {
  margin: 0;
  font-size: clamp(28px, 4vw, 42px);
  line-height: 1.15;
  letter-spacing: -.035em;
}

.course-hero__copy strong {
  color: var(--hp-ink-2);
  font-size: 14px;
}

.course-hero__copy > span {
  color: var(--hp-muted);
  font-size: 13px;
}

.chapter-skills {
  display: inline-flex;
  flex-wrap: wrap;
  gap: 5px;
  margin-top: 4px;
}

.chapter-skill {
  padding: 2px 8px;
  border: 1px solid #d8e3ec;
  border-radius: 999px;
  color: var(--hp-blue-ink);
  background: #eef5fa;
  font-size: 11px;
  font-style: normal;
  font-weight: 600;
}

.course-hero__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 2px;
}

.hero-tag {
  padding: 3px 10px;
  border: 1px solid #d8e3ec;
  border-radius: 999px;
  color: var(--hp-ink-2);
  background: rgba(255, 255, 255, .72);
  font-size: 11.5px;
  font-weight: 600;
}

.hero-tag.is-primary {
  border-color: #bcd2e2;
  color: var(--hp-blue-ink);
}

.hero-tag--level {
  background: #e6eef3;
}

.course-hero__meta {
  margin: 0;
  color: var(--hp-muted);
  font-size: 12.5px;
}

.course-hero__summary {
  display: grid;
  gap: 6px;
  min-width: 190px;
  padding: 18px;
  border: 1px solid rgba(92, 140, 180, .18);
  border-radius: 14px;
  background: rgba(255, 255, 255, .64);
}

.course-hero__summary span,
.course-hero__summary small {
  color: var(--hp-muted);
  font-size: 12px;
}

.hero-join {
  margin-top: 8px;
  padding: 8px 14px;
  border: 0;
  border-radius: 8px;
  color: #fff;
  background: var(--hp-blue-ink);
  font-size: 13px;
  font-weight: 650;
  cursor: pointer;
}

.hero-join:disabled {
  opacity: .6;
  cursor: default;
}

.hero-joined {
  color: #2f855a;
}

.action-error {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 10px;
  padding: 10px 12px;
  border: 1px solid #f3c7c2;
  border-radius: 8px;
  color: #b42318;
  background: #fef3f2;
  font-size: 13px;
}

.action-error__join {
  padding: 4px 10px;
  border: 1px solid #b42318;
  border-radius: 6px;
  color: #b42318;
  background: #fff;
  font-size: 12px;
  cursor: pointer;
}

.course-video {
  margin-bottom: 16px;
}

.video-frame {
  position: relative;
  width: 100%;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  border-radius: 12px;
  background: #000;
}

.video-frame iframe {
  width: 100%;
  height: 100%;
  border: 0;
}

.video-note {
  margin: 10px 0 0;
  color: var(--hp-muted);
  font-size: 12px;
}

.chapter-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin: 0 0 12px;
}

.chapter-body .chapter-play,
.chapter-body .chapter-watch {
  display: inline-flex;
  align-items: center;
  width: auto;
  min-height: 38px;
  padding: 0 16px;
  border: 1px solid var(--hp-line-strong);
  border-radius: 8px;
  color: var(--hp-ink-2);
  background: var(--hp-surface);
  font-size: 13px;
  font-weight: 600;
  text-decoration: none;
  cursor: pointer;
}

.chapter-body .chapter-play:hover,
.chapter-body .chapter-watch:hover {
  border-color: var(--hp-blue-ink);
  color: var(--hp-blue-ink);
}

.course-hero__summary strong {
  color: var(--hp-ink);
  font-size: 30px;
}

.course-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
  align-items: start;
  margin-top: 16px;
}

.course-main,
.course-sidebar {
  display: grid;
  gap: 16px;
}

.course-display-image {
  display: block;
  width: 100%;
  aspect-ratio: 16 / 8;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-lg);
  object-fit: cover;
  background: #e6eef3;
  box-shadow: var(--hp-shadow-sm);
}

.course-section,
.course-card {
  border-radius: var(--hp-r-lg);
}

.course-section {
  display: grid;
  gap: 12px;
}

.section-title,
.sidebar-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}

.section-title h2,
.sidebar-head h2 {
  margin: 0;
  font-size: 20px;
  letter-spacing: -.02em;
}

.section-title > div > span,
.section-title > span,
.sidebar-head > span,
.sidebar-head > small {
  color: var(--hp-muted);
  font-size: 12px;
}

.chapter,
.progress-panel,
.exam-panel {
  padding: 20px 22px;
}

.chapter {
  overflow: hidden;
}

.chapter-head {
  display: flex;
  width: 100%;
  align-items: center;
  gap: 13px;
  padding: 0;
  border: 0;
  color: var(--hp-ink);
  background: transparent;
  text-align: left;
  cursor: pointer;
}

.chapter-number {
  display: grid;
  width: 36px;
  height: 36px;
  flex: none;
  place-items: center;
  border: 1px solid #cbd5df;
  border-radius: 50%;
  color: var(--hp-blue-ink);
  background: var(--hp-surface-2);
  font-weight: 750;
}

.chapter-head > span:nth-child(2) {
  display: grid;
  flex: 1;
  gap: 4px;
}

.chapter-head strong {
  font-size: 15px;
}

.chapter-head small,
.chapter-head em,
.exam-card small,
.exam-card em {
  color: var(--hp-muted);
  font-size: 12px;
  font-style: normal;
}

.chapter-body {
  margin-top: 18px;
  padding-top: 18px;
  border-top: 1px solid var(--hp-line);
}

.chapter-body > p {
  margin: 0 0 12px;
  color: var(--hp-ink-2);
  font-size: 13px;
  line-height: 1.7;
}

.chapter-content {
  margin: 12px 0 16px;
  color: var(--hp-ink-2);
  font-size: 14px;
  line-height: 1.8;
  white-space: pre-wrap;
}

.chapter-body a {
  display: inline-block;
  margin-bottom: 14px;
  color: var(--hp-blue-ink);
  font-size: 13px;
  font-weight: 650;
}

.chapter-body button {
  width: 100%;
  min-height: 44px;
  border: 1px solid var(--hp-ink);
  border-radius: 999px;
  color: #fff;
  background: var(--hp-ink);
  font-size: 13px;
  font-weight: 650;
  cursor: pointer;
}

.course-sidebar {
  position: sticky;
  top: 88px;
}

.progress-panel {
  display: grid;
  gap: 13px;
}

.progress-panel progress {
  width: 100%;
  height: 8px;
  accent-color: var(--hp-ink);
}

.progress-panel small {
  color: var(--hp-muted);
  font-size: 12px;
}

.exam-panel {
  display: grid;
  gap: 10px;
}

.exam-card {
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 13px;
  border: 1px solid var(--hp-line);
  border-radius: 12px;
  color: var(--hp-ink);
  background: var(--hp-surface-2);
  text-align: left;
  cursor: pointer;
}

.exam-card > span {
  display: grid;
  width: 36px;
  height: 36px;
  place-items: center;
  border-radius: 10px;
  color: var(--hp-blue-ink);
  background: var(--hp-blue);
  font-weight: 800;
}

.exam-card > div {
  display: grid;
  gap: 4px;
  min-width: 0;
}

.exam-card strong {
  font-size: 13px;
}

.state,
.state-card {
  padding: 24px;
  border: 1px dashed var(--hp-line-strong);
  border-radius: var(--hp-r-md);
  color: var(--hp-muted);
  background: var(--hp-surface-2);
  font-size: 13px;
  text-align: center;
}

@media (max-width: 980px) {
  .course-layout {
    grid-template-columns: 1fr;
  }

  .course-sidebar {
    position: static;
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 680px) {
  .course-shell {
    width: min(100% - 24px, 1280px);
    padding-top: 82px;
  }

  .course-hero {
    grid-template-columns: 72px minmax(0, 1fr);
    padding: 22px;
  }

  .book-cover {
    width: 66px;
    height: 88px;
  }

  .course-hero__summary {
    grid-column: 1 / -1;
    min-width: 0;
  }

  .course-sidebar {
    grid-template-columns: 1fr;
  }

  .chapter,
  .progress-panel,
  .exam-panel {
    padding: 17px;
  }
}
</style>
