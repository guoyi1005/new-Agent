<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { getCampusCourses } from '../api/campusCourse'
import AppTabBar from '../components/AppTabBar.vue'

const router = useRouter()
const route = useRoute()

const heroIndex = ref(0)
const activeCategory = ref('hot')
const toolIndex = ref(1)
const toolPosition = ref(0)
const campusCourses = ref([])
const campusLoading = ref(false)
const carouselPaused = ref(false)
const dragStartX = ref(null)
let autoplayTimer
let revealObserver

const heroSlides = [
  {
    key: 'exam',
    eyebrow: 'AI SMART EXAM',
    title: '试卷生成',
    subtitle: '智能生成各学科标准化试卷',
    features: ['多学科支持', '题型智能匹配', '一键导出打印'],
    color: '#ff3943',
    route: '/paper',
  },
  {
    key: 'mind',
    eyebrow: 'KNOWLEDGE MAPPING',
    title: '思维导图',
    subtitle: '把复杂知识整理成清晰结构',
    features: ['章节知识梳理', '层级关系清晰', '支持继续编辑'],
    color: '#6c43d9',
    artSet: 'core',
    art: 4,
    route: 'mind_map',
  },
  {
    key: 'ppt',
    eyebrow: 'AI PRESENTATION',
    title: 'PPT生成',
    subtitle: '从主题到演示文稿一站完成',
    features: ['智能规划大纲', '自动生成逐页内容', '快速导出课件'],
    color: '#ff9900',
    artSet: 'core',
    art: 3,
    route: 'presentation',
  },
  {
    key: 'image',
    eyebrow: 'TEXT TO IMAGE',
    title: 'AI文生图',
    subtitle: '把文字描述转化为视觉作品',
    features: ['自然语言描述', '多种画面风格', '生成结果预览'],
    color: '#7546d9',
    artSet: 'core',
    art: 1,
    route: 'image',
  },
  {
    key: 'writing',
    eyebrow: 'AI WRITING',
    title: '智能写作',
    subtitle: '快速生成校园常用文稿',
    features: ['多种表达语气', '目标字数控制', '支持继续润色'],
    color: '#2975df',
    artSet: 'core',
    art: 0,
    route: 'writing',
  },
  {
    key: 'chat',
    eyebrow: 'CAMPUS COPILOT',
    title: 'AI对话',
    subtitle: '上传资料，即问即答',
    features: ['多资源理解', '图片智能识别', '校园服务协作'],
    color: '#1768e6',
    artSet: 'service',
    art: 0,
    route: 'writing',
  },
]

const categories = [
  { key: 'hot', label: '热门工具', icon: 'flame', color: '#ff343f' },
  { key: 'creation', label: 'AI创作', icon: 'bolt', color: '#7546d9' },
  { key: 'diagram', label: '图表设计', icon: 'campus', color: '#18a37d' },
  { key: 'learning', label: '学习测评', icon: 'book', color: '#f59e0b' },
  { key: 'campus', label: '校园求职', icon: 'briefcase', color: '#1768e6' },
  { key: 'convert', label: '格式转换', icon: 'convert', color: '#315f8c' },
]

const baseTools = [
  { name: '校园 AI 助手', desc: '多资源上传、识图与校园智能问答', category: ['hot', 'campus'], artSet: 'service', art: 0, route: '/ai', accent: '#1768e6' },
  { name: '智能写作', desc: '生成校园常用文稿并支持润色', category: ['hot', 'creation'], artSet: 'core', art: 0, route: '/ai-studio/writing', accent: '#4077de' },
  { name: 'AI 文生图', desc: '根据文字描述生成图片', category: ['hot', 'creation'], artSet: 'core', art: 1, route: '/ai-studio/image', accent: '#7546d9' },
  { name: 'AI伪原创', desc: '图片内容创作与水印处理', category: ['hot', 'creation'], artSet: 'core', art: 5, route: '/ai-original', accent: '#8b63e8' },
  { name: '试卷生成', desc: '生成结构化练习与标准试卷', category: ['hot', 'learning'], artSet: 'core', art: 2, route: '/paper', accent: '#ff3943' },
  { name: 'PPT 生成', desc: '生成演示大纲与文稿资源', category: ['hot', 'creation', 'learning'], artSet: 'core', art: 3, route: '/ai-studio/presentation', accent: '#ff9900' },
  { name: '思维导图', desc: '梳理主题与课程知识结构', category: ['hot', 'diagram', 'learning'], artSet: 'core', art: 4, route: '/ai-studio/mind_map', accent: '#7546d9' },
  { name: '架构图', desc: '生成系统架构可视化资源', category: ['diagram'], artSet: 'core', art: 5, route: '/ai-studio/architecture', accent: '#3b82f6' },
  { name: '流程图', desc: '生成清晰的业务与逻辑流程', category: ['diagram'], artSet: 'core', art: 6, route: '/ai-studio/flowchart', accent: '#ee5eaa' },
  { name: 'Python 在线编程', desc: '在线刷题编程练习', category: ['hot', 'learning'], artSet: 'core', art: 7, route: '/career/nebula/python', accent: '#10B981' },
  { name: '知识图谱', desc: '查看课程知识关系与学习路径', category: ['diagram', 'learning'], artSet: 'core', art: 4, route: '/career/nebula/python/knowledge-graph', accent: '#18a37d' },
  { name: '校园地图', desc: '查询校园地点、设施和导航', category: ['campus'], artSet: 'service', art: 1, route: '/growth/campus-map', accent: '#56aa1b' },
  { name: '我的简历', desc: '简历查看、编辑、上传与 AI 优化', category: ['hot', 'campus'], artSet: 'service', art: 2, route: '/ai-career/resume', accent: '#1768e6' },
  { name: '岗位雷达', desc: 'AI 整理近一周软件工程热门岗位', category: ['hot', 'campus'], artSet: 'service', art: 3, route: '/employment', accent: '#527797' },
  { name: 'PDF → Word', desc: 'PDF 转 Word 文档', category: ['convert'], artSet: 'core', art: 0, route: '/convert?type=pdf_to_docx', accent: '#5C7A99' },
  { name: 'PPT → Word', desc: 'PPT 转 Word 文档', category: ['convert'], artSet: 'core', art: 1, route: '/convert?type=ppt_to_docx', accent: '#6B9B7A' },
  { name: 'Word → PDF', desc: 'Word 转 PDF 文档', category: ['convert'], artSet: 'core', art: 2, route: '/convert?type=docx_to_pdf', accent: '#B89B7A' },
  { name: 'PDF → PPT', desc: 'PDF 转 PPT 演示文稿', category: ['convert'], artSet: 'core', art: 3, route: '/convert?type=pdf_to_ppt', accent: '#8B7AB8' },
  { name: 'PPT → PDF', desc: 'PPT 转 PDF 文档', category: ['convert'], artSet: 'core', art: 4, route: '/convert?type=ppt_to_pdf', accent: '#7A9BB8' },
  { name: 'Word → PPT', desc: 'Word 转 PPT 演示文稿', category: ['convert'], artSet: 'core', art: 5, route: '/convert?type=docx_to_ppt', accent: '#A67B7B' },
]

const displayedTools = computed(() => {
  if (activeCategory.value === 'campus') {
    const courseTools = campusCourses.value.map((course, index) => ({
      name: course.name,
      desc: `${course.currentChapterTitle || course.bookTitle} · ${course.progressPercent || 0}%`,
      artSet: 'core',
      art: index % 7,
      route: `/courses/${course.id}`,
      accent: '#56aa1b',
    }))
    return [...baseTools.filter((tool) => tool.category.includes('campus')), ...courseTools]
  }
  return baseTools.filter((tool) => tool.category.includes(activeCategory.value))
})

const carouselTools = computed(() => {
  const tools = displayedTools.value
  if (!tools.length) return []
  return Array.from({ length: 31 }, (_, copyIndex) => (
    tools.map((tool, sourceIndex) => ({
      ...tool,
      sourceIndex,
      loopKey: `${copyIndex}-${sourceIndex}-${tool.name}`,
    }))
  )).flat()
})

function circularOffset(index, active, total) {
  let offset = index - active
  if (offset > total / 2) offset -= total
  if (offset < -total / 2) offset += total
  return offset
}

function heroClass(index) {
  const offset = circularOffset(index, heroIndex.value, heroSlides.length)
  return {
    active: offset === 0,
    previous: offset === -1,
    'previous-far': offset === -2,
    next: offset === 1,
    'next-far': offset === 2,
    hidden: Math.abs(offset) > 2,
  }
}

function changeHero(direction) {
  heroIndex.value = (heroIndex.value + direction + heroSlides.length) % heroSlides.length
  restartAutoplay()
}

function startDrag(event) {
  dragStartX.value = event.clientX ?? event.touches?.[0]?.clientX ?? null
}

function endDrag(event) {
  if (dragStartX.value === null) return
  const endX = event.clientX ?? event.changedTouches?.[0]?.clientX ?? dragStartX.value
  const distance = endX - dragStartX.value
  dragStartX.value = null
  if (Math.abs(distance) < 45) return
  changeHero(distance > 0 ? -1 : 1)
}

function restartAutoplay() {
  window.clearInterval(autoplayTimer)
  autoplayTimer = window.setInterval(() => {
    if (!carouselPaused.value) changeHero(1)
  }, 5200)
}

function setCategory(key) {
  activeCategory.value = key
}

function moveTools(direction) {
  const total = displayedTools.value.length
  if (!total) return
  toolPosition.value += direction
  toolIndex.value = (toolPosition.value % total + total) % total
}

function selectCarouselTool(tool, position) {
  if (position === toolPosition.value) {
    openTool(tool)
    return
  }
  toolPosition.value = position
  toolIndex.value = tool.sourceIndex
}

function selectToolDot(index) {
  const total = displayedTools.value.length
  if (!total) return
  toolIndex.value = index
  toolPosition.value = total * 15 + index
}

function openTool(tool) {
  if (tool.route?.startsWith('/')) {
    router.push(tool.route)
    return
  }
  router.push(`/ai-studio/${tool.route || 'writing'}`)
}

function openHero(slide) {
  if (slide.route?.startsWith('/')) {
    router.push(slide.route)
    return
  }
  router.push(`/ai-studio/${slide.route}`)
}

/**
 * 工具卡片插图：与首页同一套画法（奶油底、黑色描边、低饱和马卡龙色），
 * 全部为内联 SVG，按工具用途挑选图形，避免位图被拉伸变形。
 */
const TOOL_ART = {
  doc: `
    <path d="M88 30h48l28 28v64a12 12 0 0 1-12 12H88a12 12 0 0 1-12-12V42a12 12 0 0 1 12-12z" fill="#FBF8F2" stroke="#171717" stroke-width="3"/>
    <path d="M136 30v28h28" fill="none" stroke="#171717" stroke-width="3" stroke-linejoin="round"/>
    <rect x="94" y="72" width="52" height="8" rx="4" fill="#BED2E4"/>
    <rect x="94" y="92" width="52" height="8" rx="4" fill="#BCC99C"/>
    <rect x="94" y="112" width="32" height="8" rx="4" fill="#EAD574"/>`,
  image: `
    <rect x="60" y="36" width="120" height="88" rx="14" fill="#FBF8F2" stroke="#171717" stroke-width="3"/>
    <circle cx="96" cy="66" r="11" fill="#EAD574" stroke="#171717" stroke-width="3"/>
    <path d="M72 114l34-38 22 26 18-16 22 28z" fill="#BCC99C" stroke="#171717" stroke-width="3" stroke-linejoin="round"/>`,
  slides: `
    <rect x="62" y="34" width="116" height="76" rx="12" fill="#FBF8F2" stroke="#171717" stroke-width="3"/>
    <rect x="82" y="80" width="14" height="18" rx="4" fill="#EEC3CF"/>
    <rect x="104" y="66" width="14" height="32" rx="4" fill="#EAD574"/>
    <rect x="126" y="54" width="14" height="44" rx="4" fill="#BCC99C"/>
    <rect x="148" y="74" width="14" height="24" rx="4" fill="#BED2E4"/>
    <path d="M108 110v14M132 110v14M94 124h52" fill="none" stroke="#171717" stroke-width="3" stroke-linecap="round"/>`,
  exam: `
    <rect x="66" y="26" width="94" height="108" rx="12" fill="#FBF8F2" stroke="#171717" stroke-width="3"/>
    <rect x="82" y="46" width="62" height="8" rx="4" fill="#BED2E4"/>
    <rect x="82" y="66" width="46" height="8" rx="4" fill="#BCC99C"/>
    <rect x="82" y="86" width="62" height="8" rx="4" fill="#EAD574"/>
    <circle cx="164" cy="116" r="19" fill="#BCC99C" stroke="#171717" stroke-width="3"/>
    <path d="M155 116l6 6 12-14" fill="none" stroke="#171717" stroke-width="3.4" stroke-linecap="round" stroke-linejoin="round"/>`,
  mind: `
    <path d="M120 80H74V46M120 80H74V114M120 80h46" fill="none" stroke="#171717" stroke-width="3" stroke-linecap="round"/>
    <rect x="56" y="32" width="36" height="28" rx="9" fill="#FBF8F2" stroke="#171717" stroke-width="3"/>
    <rect x="56" y="100" width="36" height="28" rx="9" fill="#FBF8F2" stroke="#171717" stroke-width="3"/>
    <rect x="166" y="66" width="36" height="28" rx="9" fill="#FBF8F2" stroke="#171717" stroke-width="3"/>
    <circle cx="120" cy="80" r="17" fill="#EAD574" stroke="#171717" stroke-width="3"/>`,
  network: `
    <path d="M120 70v24M76 74h12M152 74h12" fill="none" stroke="#171717" stroke-width="3" stroke-linecap="round"/>
    <rect x="54" y="40" width="44" height="30" rx="10" fill="#FBF8F2" stroke="#171717" stroke-width="3"/>
    <rect x="142" y="40" width="44" height="30" rx="10" fill="#FBF8F2" stroke="#171717" stroke-width="3"/>
    <rect x="98" y="94" width="44" height="30" rx="10" fill="#FBF8F2" stroke="#171717" stroke-width="3"/>
    <circle cx="76" cy="74" r="5" fill="#EEC3CF"/>
    <circle cx="164" cy="74" r="5" fill="#BED2E4"/>
    <circle cx="120" cy="94" r="5" fill="#BCC99C"/>`,
  flow: `
    <rect x="46" y="62" width="52" height="36" rx="11" fill="#FBF8F2" stroke="#171717" stroke-width="3"/>
    <path d="M128 62l22 18-22 18-22-18z" fill="#EAD574" stroke="#171717" stroke-width="3" stroke-linejoin="round"/>
    <rect x="158" y="62" width="52" height="36" rx="11" fill="#FBF8F2" stroke="#171717" stroke-width="3"/>`,
  code: `
    <rect x="62" y="38" width="116" height="80" rx="12" fill="#FBF8F2" stroke="#171717" stroke-width="3"/>
    <path d="M62 62h116" stroke="#171717" stroke-width="3"/>
    <circle cx="80" cy="50" r="3.5" fill="#171717"/>
    <circle cx="92" cy="50" r="3.5" fill="#171717"/>
    <circle cx="104" cy="50" r="3.5" fill="#171717"/>
    <path d="M92 80l-9 9 9 9M130 80l9 9-9 9M122 74l-8 30" fill="none" stroke="#171717" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round"/>`,
  graph: `
    <path d="M120 80L84 54M120 80l30-34M120 80l36 36M120 80l-28 42" fill="none" stroke="#171717" stroke-width="3" stroke-linecap="round"/>
    <circle cx="84" cy="54" r="10" fill="#EEC3CF" stroke="#171717" stroke-width="3"/>
    <circle cx="150" cy="46" r="9" fill="#BED2E4" stroke="#171717" stroke-width="3"/>
    <circle cx="156" cy="116" r="11" fill="#BCC99C" stroke="#171717" stroke-width="3"/>
    <circle cx="92" cy="122" r="9" fill="#FBF8F2" stroke="#171717" stroke-width="3"/>
    <circle cx="120" cy="80" r="14" fill="#EAD574" stroke="#171717" stroke-width="3"/>`,
  map: `
    <path d="M46 46l46-14 48 14 46-14v82l-46 14-48-14-46 14z" fill="#FBF8F2" stroke="#171717" stroke-width="3" stroke-linejoin="round"/>
    <path d="M92 32v82M140 46v82" fill="none" stroke="#171717" stroke-width="3"/>
    <path d="M164 62c-10 0-18 8-18 18 0 13 18 32 18 32s18-19 18-32c0-10-8-18-18-18z" fill="#EEC3CF" stroke="#171717" stroke-width="3" stroke-linejoin="round"/>
    <circle cx="164" cy="80" r="6" fill="#FBF8F2" stroke="#171717" stroke-width="3"/>`,
  resume: `
    <rect x="56" y="42" width="128" height="80" rx="14" fill="#FBF8F2" stroke="#171717" stroke-width="3"/>
    <circle cx="92" cy="76" r="17" fill="#BED2E4" stroke="#171717" stroke-width="3"/>
    <rect x="120" y="62" width="48" height="8" rx="4" fill="#EAD574"/>
    <rect x="120" y="80" width="36" height="8" rx="4" fill="#BCC99C"/>
    <rect x="74" y="100" width="94" height="8" rx="4" fill="#EEC3CF"/>`,
  jobs: `
    <rect x="50" y="36" width="104" height="92" rx="14" fill="#FBF8F2" stroke="#171717" stroke-width="3"/>
    <rect x="68" y="56" width="68" height="8" rx="4" fill="#BED2E4"/>
    <rect x="68" y="76" width="52" height="8" rx="4" fill="#BCC99C"/>
    <rect x="68" y="96" width="68" height="8" rx="4" fill="#EAD574"/>
    <circle cx="158" cy="104" r="24" fill="none" stroke="#171717" stroke-width="3.4"/>
    <path d="M176 122l18 18" fill="none" stroke="#171717" stroke-width="3.6" stroke-linecap="round"/>`,
  chat: `
    <path d="M62 42h116a16 16 0 0 1 16 16v46a16 16 0 0 1-16 16h-58l-26 22v-22H62a16 16 0 0 1-16-16V58a16 16 0 0 1 16-16z" fill="#FBF8F2" stroke="#171717" stroke-width="3" stroke-linejoin="round"/>
    <circle cx="96" cy="81" r="6" fill="#EAD574"/>
    <circle cx="120" cy="81" r="6" fill="#BCC99C"/>
    <circle cx="144" cy="81" r="6" fill="#EEC3CF"/>`,
  seal: `
    <rect x="54" y="38" width="112" height="80" rx="12" fill="#FBF8F2" stroke="#171717" stroke-width="3"/>
    <circle cx="88" cy="64" r="9" fill="#EAD574"/>
    <path d="M64 106l28-26 20 18 16-12 24 20z" fill="#BCC99C"/>
    <rect x="146" y="86" width="44" height="44" rx="10" fill="#EEC3CF" stroke="#171717" stroke-width="3" transform="rotate(-12 168 108)"/>
    <path d="M158 108l7 7 12-14" fill="none" stroke="#171717" stroke-width="3.2" stroke-linecap="round" stroke-linejoin="round" transform="rotate(-12 168 108)"/>`,
  convert: `
    <rect x="40" y="46" width="64" height="68" rx="12" fill="#FBF8F2" stroke="#171717" stroke-width="3"/>
    <path d="M60 68h26M60 84h26M60 100h16" fill="none" stroke="#171717" stroke-width="3" stroke-linecap="round"/>
    <rect x="136" y="46" width="64" height="68" rx="12" fill="#FBF8F2" stroke="#171717" stroke-width="3"/>
    <path d="M156 68h26M156 84h26M156 100h16" fill="none" stroke="#171717" stroke-width="3" stroke-linecap="round"/>
    <path d="M110 68h22M124 60l8 8-8 8M130 96h-22M116 88l-8 8 8 8" fill="none" stroke="#171717" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>`,
  course: `
    <rect x="52" y="40" width="136" height="84" rx="14" fill="#FBF8F2" stroke="#171717" stroke-width="3"/>
    <path d="M52 66h136" stroke="#171717" stroke-width="3"/>
    <circle cx="120" cy="88" r="18" fill="#EAD574" stroke="#171717" stroke-width="3"/>
    <path d="M114 80l14 8-14 8z" fill="#171717"/>`,
}

const HERO_ART_KEYS = {
  exam: 'exam',
  mind: 'mind',
  ppt: 'slides',
  image: 'image',
  writing: 'doc',
  chat: 'chat',
}

function toolArtKey(tool) {
  const route = String(tool?.route || '')
  if (route.startsWith('/convert')) return 'convert'
  if (route.startsWith('/ai-original')) return 'seal'
  if (route.startsWith('/ai-studio/writing')) return 'doc'
  if (route.startsWith('/ai-studio/image')) return 'image'
  if (route.startsWith('/ai-studio/presentation')) return 'slides'
  if (route.startsWith('/ai-studio/mind_map')) return 'mind'
  if (route.startsWith('/ai-studio/architecture')) return 'network'
  if (route.startsWith('/ai-studio/flowchart')) return 'flow'
  if (route.startsWith('/paper')) return 'exam'
  if (route.startsWith('/career/nebula/python/knowledge-graph')) return 'graph'
  if (route.startsWith('/career/nebula/python')) return 'code'
  if (route.startsWith('/courses')) return 'course'
  if (route.startsWith('/map')) return 'map'
  if (route.startsWith('/jobs')) return 'jobs'
  if (route.startsWith('/resume') || route.startsWith('/ai-tools/resume')) return 'resume'
  if (route.startsWith('/ai')) return 'chat'
  return 'doc'
}

function artSvg(kind) {
  const shapes = TOOL_ART[kind] || TOOL_ART.doc
  return `<svg viewBox="0 0 240 160" width="100%" height="100%" preserveAspectRatio="xMidYMid meet" aria-hidden="true">${shapes}</svg>`
}

function toolArtSvg(tool) {
  return artSvg(toolArtKey(tool))
}

function heroArtSvg(slide) {
  return artSvg(HERO_ART_KEYS[slide.key] || 'doc')
}

async function loadCampusCourses() {
  if (campusLoading.value || campusCourses.value.length) return
  campusLoading.value = true
  try {
    const response = await getCampusCourses()
    campusCourses.value = response.data || []
  } catch {
    campusCourses.value = []
  } finally {
    campusLoading.value = false
  }
}

watch(activeCategory, (category) => {
  if (category === 'campus') loadCampusCourses()
})

watch(displayedTools, (tools) => {
  toolIndex.value = tools.length > 1 ? 1 : 0
  toolPosition.value = tools.length * 15 + toolIndex.value
}, { immediate: true })

onMounted(() => {
  const requestedCategory = String(route.query.category || '')
  if (categories.some((item) => item.key === requestedCategory)) {
    activeCategory.value = requestedCategory
  }
  restartAutoplay()
  revealObserver = new IntersectionObserver((entries) => {
    entries.forEach((entry) => {
      if (entry.isIntersecting) entry.target.classList.add('is-visible')
    })
  }, { threshold: 0.12 })
  document.querySelectorAll('.reveal-section').forEach((element) => revealObserver.observe(element))
})

onBeforeUnmount(() => {
  window.clearInterval(autoplayTimer)
  revealObserver?.disconnect()
})
</script>

<template>
  <div class="ai-tools-page">
    <AppTabBar />

    <main>
      <section
        class="hero-stage reveal-section"
        aria-label="AI工具推荐"
        @mouseenter="carouselPaused = true"
        @mouseleave="carouselPaused = false"
        @pointerdown="startDrag"
        @pointerup="endDrag"
        @pointercancel="dragStartX = null"
      >
        <button class="carousel-arrow carousel-arrow--left" type="button" aria-label="上一个工具" @click="changeHero(-1)">
          <svg viewBox="0 0 24 24"><path d="m15 5-7 7 7 7" /></svg>
        </button>

        <article
          v-for="(slide, index) in heroSlides"
          :key="slide.key"
          class="hero-slide"
          :class="heroClass(index)"
          :style="{ '--hero-color': slide.color }"
        >
          <div class="hero-slide__content">
            <span>{{ slide.eyebrow }}</span>
            <h1>{{ slide.title }}</h1>
            <p>{{ slide.subtitle }}</p>
            <ul>
              <li v-for="feature in slide.features" :key="feature">
                <svg viewBox="0 0 20 20"><path d="m5.5 10 3 3 6-7" /></svg>
                {{ feature }}
              </li>
            </ul>
            <button type="button" @click="openHero(slide)">
              立即使用
              <svg viewBox="0 0 24 24"><path d="m9 5 7 7-7 7" /></svg>
            </button>
          </div>
          <span class="hero-slide__art" aria-hidden="true" v-html="heroArtSvg(slide)"></span>
        </article>

        <button class="carousel-arrow carousel-arrow--right" type="button" aria-label="下一个工具" @click="changeHero(1)">
          <svg viewBox="0 0 24 24"><path d="m9 5 7 7-7 7" /></svg>
        </button>

        <div class="hero-dots" aria-label="轮播位置">
          <button
            v-for="(slide, index) in heroSlides"
            :key="slide.key"
            type="button"
            :class="{ active: heroIndex === index }"
            :aria-label="`切换到${slide.title}`"
            @click="heroIndex = index; restartAutoplay()"
          ></button>
        </div>
      </section>

      <section class="category-dock reveal-section">
        <button
          v-for="category in categories"
          :key="category.key"
          type="button"
          :class="{ active: activeCategory === category.key }"
          :style="{ '--category-color': category.color }"
          @click="setCategory(category.key)"
        >
          <span class="category-icon">
            <svg v-if="category.icon === 'flame'" viewBox="0 0 24 24"><path d="M13 2c1 5-3 5-1 9 1.6-1.2 2.4-2.5 2.2-4.2C18 9.2 20 12 19 16a7 7 0 0 1-14 0c0-3.6 2-6.8 5.5-9-.4 2.2.2 3.8 1.5 4.8" /></svg>
            <svg v-else-if="category.icon === 'book'" viewBox="0 0 24 24"><path d="M3 5.5A3.5 3.5 0 0 1 6.5 2H11v17H6.5A3.5 3.5 0 0 0 3 22V5.5Zm18 0A3.5 3.5 0 0 0 17.5 2H13v17h4.5A3.5 3.5 0 0 1 21 22V5.5Z" /></svg>
            <svg v-else-if="category.icon === 'campus'" viewBox="0 0 24 24"><path d="M3 21V9l5-3v15m8 0V6l5 3v12M8 9h8M6 12h1m-1 3h1m-1 3h1m10-6h1m-1 3h1m-1 3h1M10 13h4v8h-4z" /></svg>
            <svg v-else-if="category.icon === 'briefcase'" viewBox="0 0 24 24"><path d="M8 7V5a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2M3 9h18v11H3zM3 13c5 2 13 2 18 0M10 13h4" /></svg>
            <svg v-else-if="category.icon === 'convert'" viewBox="0 0 24 24"><path d="M4 8h13m-3-3 3 3-3 3M20 16H7m3 3-3-3 3-3" /></svg>
            <svg v-else viewBox="0 0 24 24"><path d="m13 2-8 12h6l-1 8 9-13h-6z" /></svg>
          </span>
          <strong>{{ category.label }}</strong>
        </button>
      </section>

      <section class="tools-section reveal-section">
        <header class="section-title">
          <div>
            <p>{{ categories.find((item) => item.key === activeCategory)?.label }}</p>
            <h2>找到适合你的智能工具</h2>
          </div>
          <button type="button" @click="router.push('/ai-tools')">
            查看全部
            <svg viewBox="0 0 24 24"><path d="m9 5 7 7-7 7" /></svg>
          </button>
        </header>

        <div class="tool-carousel">
          <button
            class="tool-arrow tool-arrow--left"
            type="button"
            aria-label="上一个工具"
            @click="moveTools(-1)"
          >
            <svg viewBox="0 0 24 24"><path d="m15 5-7 7 7 7" /></svg>
          </button>

          <div v-if="campusLoading" class="tools-loading">正在加载校园课程...</div>
          <div v-else class="tool-window">
            <div
              class="tool-track"
              :style="{ '--tool-position': toolPosition }"
            >
              <button
                v-for="(tool, index) in carouselTools"
                :key="`${activeCategory}-${tool.loopKey}`"
                class="tool-poster"
                :class="{ active: toolPosition === index }"
                :style="{ '--accent': tool.accent }"
                type="button"
                @click="selectCarouselTool(tool, index)"
              >
                <span class="tool-poster__art" v-html="toolArtSvg(tool)"></span>
                <span class="tool-poster__content">
                  <strong>{{ tool.name }}</strong>
                  <em>{{ tool.desc }}</em>
                  <span v-if="toolPosition === index" class="tool-poster__action">
                    立即使用
                    <svg viewBox="0 0 24 24"><path d="m9 5 7 7-7 7" /></svg>
                  </span>
                </span>
              </button>
            </div>
          </div>

          <button
            class="tool-arrow tool-arrow--right"
            type="button"
            aria-label="下一个工具"
            @click="moveTools(1)"
          >
            <svg viewBox="0 0 24 24"><path d="m9 5 7 7-7 7" /></svg>
          </button>
        </div>

        <div class="tool-dots">
          <button
            v-for="(_, index) in displayedTools"
            :key="index"
            type="button"
            :class="{ active: toolIndex === index }"
            :aria-label="`切换到第${index + 1}个工具`"
            @click="selectToolDot(index)"
          ></button>
        </div>

        <p class="drag-hint">
          <svg viewBox="0 0 24 24"><path d="M8 11V7a2 2 0 0 1 4 0v3-5a2 2 0 0 1 4 0v5-2a2 2 0 0 1 4 0v6c0 5-3 8-8 8-3 0-5-1.5-7-4l-2-3a2 2 0 0 1 3-2l2 2" /></svg>
          点击卡片或左右按钮探索更多工具
        </p>
      </section>
    </main>
  </div>
</template>

<style scoped>
.ai-tools-page {
  min-height: 100vh;
  padding-top: 60px;
  overflow: hidden;
  color: var(--hp-ink);
  background: var(--hp-bg);
  font-family: Inter, 'Segoe UI', system-ui, -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

button,
a {
  -webkit-tap-highlight-color: transparent;
}

svg {
  fill: none;
  stroke: currentColor;
  stroke-linecap: round;
  stroke-linejoin: round;
  stroke-width: 1.9;
}

.hero-stage {
  position: relative;
  width: 100%;
  height: clamp(360px, 32vw, 470px);
  margin-top: 16px;
  touch-action: pan-y;
  user-select: none;
}

.hero-slide {
  position: absolute;
  top: 0;
  left: 50%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 42%);
  align-items: center;
  gap: 26px;
  width: min(880px, 46vw);
  height: calc(100% - 40px);
  padding: 38px 40px;
  border: 1px solid var(--hp-line);
  border-radius: 24px;
  background: color-mix(in srgb, var(--hero-color) 14%, var(--hp-cream));
  opacity: 0;
  transform-origin: center;
  transform: translateX(-50%) scale(0.84);
  transition:
    transform 0.72s cubic-bezier(0.22, 0.82, 0.22, 1),
    opacity 0.45s ease;
}

.hero-slide.active {
  z-index: 4;
  opacity: 1;
  transform: translateX(-50%) scale(1);
}

.hero-slide.previous,
.hero-slide.next {
  z-index: 3;
  opacity: 0.92;
}

.hero-slide.previous {
  transform: translateX(calc(-50% - min(38vw, 700px))) scale(0.7) rotate(-2deg);
}

.hero-slide.next {
  transform: translateX(calc(-50% + min(38vw, 700px))) scale(0.7) rotate(2deg);
}

.hero-slide.previous-far,
.hero-slide.next-far {
  z-index: 1;
  opacity: 0.5;
}

.hero-slide.previous-far {
  transform: translateX(calc(-50% - min(58vw, 1060px))) scale(0.42) rotate(-4deg);
}

.hero-slide.next-far {
  transform: translateX(calc(-50% + min(58vw, 1060px))) scale(0.42) rotate(4deg);
}

.hero-slide.hidden {
  pointer-events: none;
  transform: translateX(-50%) scale(0.72);
}

.hero-slide__art {
  display: block;
  width: 100%;
  aspect-ratio: 3 / 2;
  padding: 14px 18px;
  border: 1px solid var(--hp-line);
  border-radius: var(--hp-r-md);
  background: #ffffff;
  overflow: hidden;
}

.hero-slide__content {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  min-width: 0;
  height: 100%;
}

.hero-slide__content > span {
  color: var(--hp-muted);
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.18em;
}

.hero-slide h1 {
  margin: 12px 0 6px;
  font-size: clamp(28px, 2.4vw, 42px);
  font-weight: 700;
  line-height: 1.15;
  letter-spacing: -0.02em;
}

.hero-slide p {
  margin: 0;
  color: #55504a;
  font-size: clamp(14px, 1.2vw, 16px);
  font-weight: 500;
}

.hero-slide ul {
  display: grid;
  gap: 7px;
  margin: 20px 0 18px;
  padding: 0;
  list-style: none;
}

.hero-slide li {
  display: flex;
  align-items: center;
  gap: 8px;
  color: var(--hp-muted);
  font-size: 13px;
  font-weight: 500;
}

.hero-slide li svg {
  width: 17px;
  height: 17px;
  padding: 3px;
  border-radius: 50%;
  background: var(--hp-green);
  color: var(--hp-ink);
}

.hero-slide__content > button {
  display: flex;
  align-items: center;
  gap: 7px;
  min-height: 40px;
  margin-top: auto;
  padding: 0 20px;
  border-radius: 999px;
  color: var(--hp-cream);
  background: var(--hp-ink);
  font-size: 13px;
  font-weight: 600;
  transition: background 0.2s ease;
}

.hero-slide__content > button:hover {
  background: #2f2f2f;
}

.hero-slide__content > button svg,
.section-title button svg,
.tool-poster__action svg {
  width: 16px;
  height: 16px;
}

.carousel-arrow,
.tool-arrow {
  position: absolute;
  z-index: 12;
  display: grid;
  place-items: center;
  border: 1px solid var(--hp-line);
  border-radius: 50%;
  color: var(--hp-ink);
  background: var(--hp-cream);
  transition: 0.22s ease;
}

.carousel-arrow {
  top: 43%;
  width: 46px;
  height: 46px;
}

.carousel-arrow:hover,
.tool-arrow:hover:not(:disabled) {
  color: var(--hp-cream);
  background: var(--hp-ink);
  transform: scale(1.06);
}

.carousel-arrow svg,
.tool-arrow svg {
  width: 22px;
  height: 22px;
}

.carousel-arrow--left {
  left: 20px;
}

.carousel-arrow--right {
  right: 20px;
}

.hero-dots {
  position: absolute;
  z-index: 14;
  right: 0;
  bottom: 9px;
  left: 0;
  display: flex;
  justify-content: center;
  gap: 8px;
}

.hero-dots button,
.tool-dots button {
  width: 8px;
  height: 8px;
  padding: 0;
  border-radius: 99px;
  background: #d8d1c4;
  transition: 0.25s ease;
}

.hero-dots button.active,
.tool-dots button.active {
  width: 22px;
  background: var(--hp-ink);
}

.category-dock {
  position: relative;
  z-index: 16;
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  width: min(1180px, calc(100% - 56px));
  min-height: 70px;
  margin: -30px auto 0;
  border: 1px solid var(--hp-line);
  border-radius: 999px;
  background: var(--hp-cream);
  overflow: hidden;
}

.category-dock button {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-width: 0;
  border-radius: 999px;
  color: var(--hp-ink);
  background: transparent;
  font-size: 15px;
  font-weight: 600;
  transition: 0.25s ease;
}

.category-dock button + button::before {
  position: absolute;
  left: 0;
  width: 1px;
  height: 26px;
  background: rgba(23, 23, 23, 0.12);
  content: "";
}

.category-dock button:hover {
  background: rgba(23, 23, 23, 0.04);
}

.category-dock button.active {
  background: var(--hp-yellow);
}

.category-dock button.active::before,
.category-dock button.active + button::before {
  opacity: 0;
}

.category-icon {
  display: grid;
  place-items: center;
  width: 30px;
  height: 30px;
  color: color-mix(in srgb, var(--category-color) 30%, var(--hp-ink));
}

.category-dock button.active .category-icon {
  color: var(--hp-ink);
}

.category-icon svg {
  width: 27px;
  height: 27px;
  stroke-width: 1.9;
}

.tools-section {
  padding: 58px 0 28px;
}

.section-title {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  width: min(1420px, calc(100% - 48px));
  margin: 0 auto 22px;
}

.section-title p {
  width: max-content;
  margin: 0 0 8px;
  color: var(--hp-muted);
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.16em;
}

.section-title h2 {
  margin: 0;
  color: var(--hp-ink);
  font-size: 21px;
  font-weight: 700;
  letter-spacing: -0.01em;
}

.section-title > button {
  display: flex;
  align-items: center;
  gap: 7px;
  min-height: 38px;
  padding: 0 18px;
  border: 1px solid var(--hp-line);
  border-radius: 999px;
  color: var(--hp-ink);
  background: transparent;
  font-size: 13px;
  font-weight: 600;
  transition: background 0.2s ease, color 0.2s ease;
}

.section-title > button:hover {
  background: var(--hp-ink);
  color: var(--hp-cream);
}

.tool-carousel {
  position: relative;
  min-height: 390px;
}

.tool-window {
  position: relative;
  width: 100%;
  min-height: 390px;
  overflow: hidden;
}

.tool-track {
  --card-width: 246px;
  --card-gap: 18px;
  position: absolute;
  top: 25px;
  left: 50%;
  display: flex;
  gap: var(--card-gap);
  width: max-content;
  transform: translateX(calc(-1 * var(--tool-position) * (var(--card-width) + var(--card-gap)) - var(--card-width) / 2));
  transition: transform 0.46s cubic-bezier(0.22, 0.82, 0.22, 1);
}

.tool-poster {
  display: flex;
  flex: 0 0 var(--card-width);
  flex-direction: column;
  width: var(--card-width);
  height: 328px;
  padding: 0;
  border: 1px solid rgba(23, 23, 23, 0.16);
  border-radius: 18px;
  overflow: hidden;
  color: var(--hp-ink);
  background: var(--hp-cream);
  text-align: left;
  opacity: 0.8;
  transform: translateY(16px) scale(0.94);
  transition:
    transform 0.5s cubic-bezier(0.22, 0.82, 0.22, 1),
    opacity 0.35s ease,
    border-color 0.35s ease;
}

.tool-poster:hover {
  opacity: 1;
  transform: translateY(4px) scale(0.98);
}

.tool-poster.active {
  z-index: 3;
  opacity: 1;
  border-color: var(--hp-line);
  transform: translateY(-4px) scale(1.08);
}

.tool-poster__art {
  display: block;
  width: 100%;
  height: 214px;
  padding: 20px 24px;
  background: color-mix(in srgb, var(--accent) 14%, var(--hp-cream));
}

.tool-poster__content {
  position: relative;
  display: grid;
  gap: 5px;
  flex: 1;
  align-content: start;
  padding: 16px 18px;
  border-top: 3px solid color-mix(in srgb, var(--accent) 50%, var(--hp-cream));
  background: var(--hp-cream);
}

.tool-poster__content strong {
  font-size: 18px;
  font-weight: 600;
  line-height: 1.3;
}

.tool-poster__content em {
  color: var(--hp-muted);
  font-size: 12px;
  font-style: normal;
}

.tool-poster__action {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 5px;
  width: 112px;
  min-height: 32px;
  margin-top: 8px;
  border-radius: 999px;
  color: var(--hp-cream);
  background: var(--hp-ink);
  font-size: 12px;
  font-weight: 600;
  animation: action-in 0.35s ease both;
}

.tool-arrow {
  top: 44%;
  width: 44px;
  height: 44px;
}

.tool-arrow--left {
  left: 20px;
}

.tool-arrow--right {
  right: 20px;
}

.tools-loading {
  display: grid;
  min-height: 340px;
  place-items: center;
  color: var(--hp-muted);
}

.tool-dots {
  display: flex;
  justify-content: center;
  gap: 8px;
  margin-top: 2px;
}

.drag-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  width: max-content;
  margin: 18px auto 0;
  padding: 8px 14px;
  border: 1px solid rgba(23, 23, 23, 0.16);
  border-radius: 999px;
  color: var(--hp-muted);
  background: transparent;
  font-size: 11px;
}

.drag-hint svg {
  width: 16px;
  height: 16px;
}

.reveal-section {
  opacity: 0;
  transform: translateY(26px);
  transition:
    opacity 0.7s ease,
    transform 0.7s cubic-bezier(0.22, 0.82, 0.22, 1);
}

.reveal-section.is-visible {
  opacity: 1;
  transform: translateY(0);
}

@keyframes action-in {
  from { opacity: 0; transform: translateY(6px); }
  to { opacity: 1; transform: translateY(0); }
}

@media (max-width: 1120px) {
  .hero-slide {
    width: 62vw;
  }

  .hero-slide.previous {
    transform: translateX(calc(-50% - 46vw)) scale(0.72);
  }

  .hero-slide.next {
    transform: translateX(calc(-50% + 46vw)) scale(0.72);
  }

  .hero-slide.previous-far {
    transform: translateX(calc(-50% - 68vw)) scale(0.44);
  }

  .hero-slide.next-far {
    transform: translateX(calc(-50% + 68vw)) scale(0.44);
  }
}

@media (max-width: 760px) {
  .hero-stage {
    height: 470px;
    margin-top: 8px;
  }

  .hero-slide {
    grid-template-columns: minmax(0, 1fr);
    align-content: start;
    gap: 16px;
    width: calc(100% - 38px);
    height: 430px;
    padding: 24px;
    border-radius: 20px;
  }

  .hero-slide__art {
    width: 180px;
    aspect-ratio: 3 / 2;
  }

  .hero-slide.previous {
    transform: translateX(calc(-50% - 92vw)) scale(0.92);
  }

  .hero-slide.next {
    transform: translateX(calc(-50% + 92vw)) scale(0.92);
  }

  .hero-slide.previous-far,
  .hero-slide.next-far {
    opacity: 0;
    pointer-events: none;
  }

  .hero-slide h1 {
    font-size: 30px;
  }

  .hero-slide p {
    font-size: 13px;
  }

  .hero-slide li {
    font-size: 12px;
  }

  .carousel-arrow {
    display: none;
  }

  .category-dock {
    display: flex;
    width: calc(100% - 24px);
    min-height: 62px;
    margin-top: -24px;
    overflow-x: auto;
    border-radius: 22px;
    scrollbar-width: none;
  }

  .category-dock button {
    flex: 0 0 132px;
    gap: 7px;
    border-radius: 20px;
    font-size: 14px;
  }

  .category-icon {
    width: 27px;
    height: 27px;
  }

  .category-icon svg {
    width: 25px;
    height: 25px;
  }

  .tools-section {
    padding-top: 45px;
  }

  .section-title {
    width: calc(100% - 28px);
  }

  .section-title p {
    font-size: 12px;
  }

  .section-title h2 {
    font-size: 18px;
  }

  .tool-track {
    --card-width: 224px;
    --card-gap: 14px;
  }

  .tool-poster {
    height: 316px;
  }

  .tool-poster__art {
    height: 202px;
  }

  .tool-arrow {
    width: 40px;
    height: 40px;
  }

  .tool-arrow--left {
    left: 8px;
  }

  .tool-arrow--right {
    right: 8px;
  }
}

@media (prefers-reduced-motion: reduce) {
  *,
  *::before,
  *::after {
    scroll-behavior: auto !important;
    animation-duration: 0.01ms !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
