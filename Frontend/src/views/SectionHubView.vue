<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'

import AppTabBar from '../components/AppTabBar.vue'
import { polishOrExpandResume } from '../api/aiGeneration'

const route = useRoute()
const activeAiCareerItem = ref('智能简历')
const resumeDraft = ref('')
const targetPosition = ref('')
const targetJobDescription = ref('')
const aiResult = ref('')
const aiBusy = ref(false)
const aiError = ref('')

const sections = {
  career: {
    eyebrow: 'CAREER EXPLORATION',
    title: '岗位探索',
    description: '理解岗位关系、职业画像和技能要求，并看清自己与目标岗位的差距。',
    items: [
      { code: '01', title: '岗位星图', description: '查看岗位关系、职业分支、发展路线与关联技能。', to: '/career/star-map', tone: 'purple' },
      { code: '02', title: '岗位搜索', description: '按岗位名称、方向、技能与热门关键词检索现有岗位。', to: '/career/search', tone: 'blue' },
      { code: '03', title: '岗位画像', description: '查看现有岗位介绍、所属行业、岗位方向与详情。', to: '/career/profile', tone: 'pink' },
      { code: '04', title: '技能要求', description: '从现有岗位详情中查看岗位所需技能与学习建议。', to: '/career/skills', tone: 'yellow' },
      { code: '05', title: '我的能力差距', description: '查看目标岗位匹配度、未掌握能力与推荐学习内容。', to: '/career/gap', tone: 'green' },
    ],
  },
  learning: {
    eyebrow: 'LEARNING & PRACTICE',
    title: '学习实践',
    description: '围绕目标岗位继续课程学习、题库练习、测验与项目实践。',
    items: [
      { code: '01', title: 'Python学习', description: '继续现有 Python 课程、学习计划、知识星系与课程进度。', to: '/learning/python/plan', tone: 'blue' },
      { code: '02', title: '题库', description: '进入现有编程题库、题库练习、测试与错题相关功能。', to: '/learning/question-bank', tone: 'green' },
      { code: '03', title: '项目实训', description: '通过现有 Python 学习计划继续项目实战与编程实训。', to: '/learning/python/plan', tone: 'pink' },
      { code: '04', title: '专项训练', description: '按知识薄弱点使用现有专项学习资源。', to: '/learning/special-training', tone: 'yellow' },
      { code: '05', title: '商业沙盘', description: '商业沙盘功能尚未实现。', tone: 'purple', pending: true },
      { code: '06', title: '企业模拟', description: '企业模拟功能尚未实现。', tone: 'blue', pending: true },
    ],
  },
  employment: {
    eyebrow: 'INTERNSHIP & EMPLOYMENT',
    title: '实习就业',
    description: '查看当前真实招聘机会和外部岗位来源，未接入的数据专区明确保留建设位置。',
    items: [
      { code: '01', title: '实习雷达', description: '复用现有推荐岗位与匹配推荐能力，不新增虚假雷达算法。', to: '/employment/radar', tone: 'blue' },
      { code: '02', title: '多平台岗位聚合', description: '查看当前已有的真实外部岗位来源与跳转链接。', to: '/employment/aggregate', tone: 'green' },
      { code: '03', title: '本地就业专区', description: '本地岗位数据尚未接入。', tone: 'yellow', pending: true },
      { code: '04', title: '校招', description: '使用当前已有招聘岗位数据，暂不伪造校招分类。', to: '/employment/campus-recruitment', tone: 'pink' },
      { code: '05', title: '校友企业', description: '校友企业数据尚未接入。', tone: 'purple', pending: true },
    ],
  },
  community: {
    eyebrow: 'CAMPUS COMMUNITY',
    title: '校友社区',
    description: '围绕学长学姐成长经验、就业案例与问答交流建设校友支持网络。',
    items: [
      { code: '01', title: '学长学姐成长路径', description: '尚未接入真实校友成长案例。', tone: 'green', pending: true },
      { code: '02', title: '经验分享', description: '尚未接入真实学习或就业经验内容。', tone: 'blue', pending: true },
      { code: '03', title: '就业案例', description: '尚未接入真实就业结果与求职案例。', tone: 'yellow', pending: true },
      { code: '04', title: '问答交流', description: '尚未实现社区问答与讨论能力。', tone: 'pink', pending: true },
    ],
  },
  aiCareer: {
    eyebrow: 'AI CAREER',
    title: 'AI 求职',
    description: '集中使用直接服务求职过程的智能简历、模拟面试与面试报告能力。',
    items: [
      { code: '01', title: '智能简历', description: '查看、填写、上传和分析简历，使用现有简历辅助能力。', to: '/ai-tools/resume', tone: 'blue' },
      { code: '02', title: 'AI润色', description: '粘贴简历内容，调用简历 AI 智能体优化表达与结构，并保留真实经历。', tone: 'pink' },
      { code: '03', title: '岗位定制简历', description: '结合目标岗位与招聘描述，调用简历 AI 智能体生成针对性简历优化稿。', tone: 'green' },
      { code: '04', title: 'AI模拟面试', description: '进入完整模拟面试流程，包含环境检测、实时提示与面试过程。', to: '/interview', tone: 'yellow' },
      { code: '05', title: '面试报告', description: '查看已完成面试对应的评估结果、反馈与历史报告。', to: '/interview', tone: 'purple' },
    ],
  },
}

const section = computed(() => sections[route.meta.section] || sections.aiCareer)
const isAiCareer = computed(() => route.meta.section === 'aiCareer')
const activeAiCareer = computed(() =>
  sections.aiCareer.items.find((item) => item.title === activeAiCareerItem.value) || sections.aiCareer.items[0],
)

watch(isAiCareer, (value) => {
  if (value) activeAiCareerItem.value = sections.aiCareer.items[0].title
}, { immediate: true })

watch(activeAiCareerItem, () => {
  aiResult.value = ''
  aiError.value = ''
})

const isResumeAiTool = computed(() => ['AI润色', '岗位定制简历'].includes(activeAiCareerItem.value))

async function generateResumeContent() {
  const source = resumeDraft.value.trim()
  if (!source) {
    aiError.value = '请先粘贴简历内容，再开始生成。'
    return
  }
  if (activeAiCareerItem.value === '岗位定制简历' && !targetPosition.value.trim()) {
    aiError.value = '请填写目标岗位名称。'
    return
  }

  const isTailoring = activeAiCareerItem.value === '岗位定制简历'
  const jobInput = isTailoring
    ? `【目标岗位】\n${targetPosition.value.trim()}\n\n【岗位描述】\n${targetJobDescription.value.trim() || '未提供'}\n\n【原简历】\n${source}`
    : source
  if (jobInput.length > 4000) {
    aiError.value = '岗位描述与简历合计不能超过 4000 字，请精简后重试。'
    return
  }
  const prompt = isTailoring
    ? '你是求职简历顾问。用户输入包含目标岗位、岗位描述和原简历。请依据真实材料输出针对岗位的简历优化稿，只调整表达、排序和重点，不得编造经历、技能、数字、公司、学历或成果；缺少信息时用【待补充】标注。先给完整可复制的简历文本，再简要说明主要调整点。'
    : '你是求职简历编辑。请润色用户提供的简历，提升清晰度、专业度和可读性。必须严格保留事实，不得编造职责、技能、数字、公司、学历或成果；信息不足时用【待补充】标注。先输出完整可复制的润色稿，再简要列出调整点。'

  aiBusy.value = true
  aiError.value = ''
  aiResult.value = ''
  try {
    const response = await polishOrExpandResume({
      prompt,
      input: jobInput,
    })
    const answer = String(response?.answer || '').trim()
    if (!answer) throw new Error('AI 暂未返回可用内容，请稍后重试。')
    aiResult.value = answer
  } catch (error) {
    aiError.value = error?.message || '请求失败，请检查登录状态及 AI 服务配置后重试。'
  } finally {
    aiBusy.value = false
  }
}

async function copyAiResult() {
  if (!aiResult.value) return
  try {
    await navigator.clipboard.writeText(aiResult.value)
  } catch {
    aiError.value = '复制失败，请手动选择并复制生成内容。'
  }
}
</script>

<template>
  <div class="section-hub">
    <AppTabBar />
    <main class="section-hub__main">
      <header class="section-hub__hero">
        <p>{{ section.eyebrow }}</p>
        <h1>{{ section.title }}</h1>
        <span>{{ section.description }}</span>
      </header>

      <template v-if="isAiCareer">
        <nav class="ai-career-nav" aria-label="AI 求职子版块">
          <button
            v-for="item in section.items"
            :key="item.title"
            type="button"
            class="ai-career-nav__item"
            :class="{ 'is-active': activeAiCareerItem === item.title, 'is-pending': item.pending }"
            :aria-current="activeAiCareerItem === item.title ? 'page' : undefined"
            @click="activeAiCareerItem = item.title"
          >
            <span>{{ item.code }}</span>
            <strong>{{ item.title }}</strong>
          </button>
        </nav>

        <section class="ai-career-content" :class="[`ai-career-content--${activeAiCareer.tone}`, { 'is-pending': activeAiCareer.pending }]">
          <div class="ai-career-content__index">{{ activeAiCareer.code }} <span>/ AI CAREER</span></div>
          <div class="ai-career-content__body">
            <div>
              <span class="ai-career-content__eyebrow">{{ activeAiCareer.pending ? '功能状态 · 待建设' : isResumeAiTool ? '可用 · 简历 AI 工具' : 'AI 求职工具' }}</span>
              <h2>{{ activeAiCareer.title }}</h2>
              <p>{{ activeAiCareer.description }}</p>
            </div>
            <RouterLink v-if="activeAiCareer.to && !activeAiCareer.pending && !isResumeAiTool" class="ai-career-content__action" :to="activeAiCareer.to">
              {{ activeAiCareer.title === '面试报告' ? '查看面试与报告' : `进入${activeAiCareer.title}` }} <span aria-hidden="true">→</span>
            </RouterLink>
            <span v-else-if="activeAiCareer.pending" class="ai-career-content__status">该功能尚未开放</span>
          </div>
          <form v-if="isResumeAiTool" class="ai-career-form" @submit.prevent="generateResumeContent">
            <label v-if="activeAiCareer.title === '岗位定制简历'" class="ai-career-field">
              <span>目标岗位</span>
              <input v-model="targetPosition" maxlength="80" placeholder="例如：Python 开发工程师" />
            </label>
            <label v-if="activeAiCareer.title === '岗位定制简历'" class="ai-career-field">
              <span>岗位描述 <small>选填，粘贴招聘要求可获得更有针对性的优化</small></span>
              <textarea v-model="targetJobDescription" maxlength="4000" rows="3" placeholder="粘贴岗位职责与任职要求" />
            </label>
            <label class="ai-career-field">
              <span>{{ activeAiCareer.title === '岗位定制简历' ? '现有简历内容' : '需要润色的简历内容' }}</span>
              <textarea v-model="resumeDraft" maxlength="4000" rows="6" placeholder="粘贴真实的简历文本。系统只会优化表达，不会补造经历或成果。" />
              <small class="ai-career-field__count">{{ resumeDraft.length }} / 4000</small>
            </label>
            <p v-if="aiError" class="ai-career-error" role="alert">{{ aiError }}</p>
            <button class="ai-career-content__action" type="submit" :disabled="aiBusy">
              {{ aiBusy ? '正在生成…' : activeAiCareer.title === '岗位定制简历' ? '生成岗位定制稿' : '开始 AI 润色' }}
              <span aria-hidden="true">→</span>
            </button>
            <section v-if="aiResult" class="ai-career-result" aria-live="polite">
              <header><strong>生成结果</strong><div><button type="button" @click="copyAiResult">复制</button><button type="button" @click="resumeDraft = aiResult">替换输入</button></div></header>
              <pre>{{ aiResult }}</pre>
            </section>
          </form>
          <div class="ai-career-content__footnote">
            <span class="ai-career-content__dot" />
            <span v-if="activeAiCareer.title === '面试报告'">报告基于已完成的面试记录生成；尚无记录时可先开始模拟面试。</span>
            <span v-else-if="activeAiCareer.pending">此版块暂不提供模拟结果或示例数据。</span>
            <span v-else-if="isResumeAiTool">提交内容后将请求后端简历 AI 智能体；只优化表达，不生成虚构经历。</span>
            <span v-else>在本页面查看版块说明，进入对应流程后继续操作。</span>
          </div>
        </section>
      </template>

      <section v-else class="section-hub__grid" :aria-label="`${section.title}功能结构`">
        <component
          v-for="item in section.items"
          :is="item.to ? 'RouterLink' : 'article'"
          :key="item.title"
          class="section-hub__card"
          :class="[`section-hub__card--${item.tone}`, { 'is-pending': item.pending }]"
          :to="item.to || undefined"
        >
          <span class="section-hub__code">{{ item.code }}</span>
          <div>
            <h2>{{ item.title }}</h2>
            <p>{{ item.description }}</p>
            <span v-if="item.pending" class="section-hub__status">待建设</span>
          </div>
          <span v-if="item.to" class="section-hub__arrow" aria-hidden="true">→</span>
        </component>
      </section>
    </main>
  </div>
</template>

<style scoped>
.section-hub {
  min-height: 100vh;
  color: #23262b;
  background: #f7f5f1;
}

.section-hub__main {
  width: min(1180px, calc(100% - 40px));
  margin: 0 auto;
  padding: 108px 0 72px;
}

.section-hub__hero {
  max-width: 760px;
  margin-bottom: 38px;
}

.section-hub__hero > p {
  margin: 0 0 14px;
  color: #5c8cb4;
  font: 700 12px/1.2 Inter, sans-serif;
  letter-spacing: .16em;
}

.section-hub__hero h1 {
  margin: 0 0 14px;
  font-size: clamp(38px, 5vw, 64px);
  line-height: 1;
  letter-spacing: -.055em;
}

.section-hub__hero > span {
  display: block;
  max-width: 660px;
  color: #69717b;
  font-size: 16px;
  line-height: 1.8;
}

.section-hub__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.ai-career-nav {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 18px;
  padding: 8px;
  border: 1px solid #e3ded6;
  border-radius: 18px;
  background: rgba(255, 255, 255, .72);
}

.ai-career-nav__item {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  min-height: 46px;
  padding: 0 16px;
  border: 1px solid transparent;
  border-radius: 12px;
  color: #525963;
  background: transparent;
  cursor: pointer;
  font: inherit;
  transition: background .18s ease, color .18s ease, border-color .18s ease, transform .18s ease;
}

.ai-career-nav__item:hover { transform: translateY(-1px); background: #f5f0e7; }
.ai-career-nav__item.is-active { border-color: #dfc777; color: #29251f; background: #f4e8b9; }
.ai-career-nav__item.is-pending:not(.is-active) { color: #777d85; }
.ai-career-nav__item > span { color: #81868c; font: 700 11px/1 Inter, sans-serif; letter-spacing: .06em; }
.ai-career-nav__item strong { font-size: 14px; font-weight: 680; }

.ai-career-content {
  min-height: 300px;
  padding: 30px 34px 22px;
  overflow: hidden;
  border: 1px solid #e3ded6;
  border-radius: 26px;
  background: #e7f0f7;
  box-shadow: 0 12px 32px rgba(35, 38, 43, .06);
  animation: ai-career-enter .22s ease both;
}

.ai-career-content--green { background: #e7efe1; }
.ai-career-content--yellow { background: #f4e8b9; }
.ai-career-content--pink { background: #f1e3e2; }
.ai-career-content--purple { background: #ece7f4; }
.ai-career-content.is-pending { background: #f1efeb; }
.ai-career-content__index { color: #69717b; font: 700 12px/1.2 Inter, sans-serif; letter-spacing: .1em; }
.ai-career-content__index span { color: #8c9095; font-weight: 500; }
.ai-career-content__body { display: flex; align-items: end; justify-content: space-between; gap: 32px; margin-top: 48px; }
.ai-career-content__eyebrow { color: #65717a; font-size: 12px; font-weight: 650; }
.ai-career-content h2 { margin: 10px 0 12px; color: #23262b; font-size: clamp(28px, 4vw, 42px); line-height: 1.12; letter-spacing: -.04em; }
.ai-career-content p { max-width: 670px; margin: 0; color: #555e67; font-size: 15px; line-height: 1.8; }
.ai-career-content__action { display: inline-flex; flex: 0 0 auto; align-items: center; gap: 20px; padding: 13px 18px; border: 1px solid #23262b; border-radius: 12px; color: #fff; background: #23262b; text-decoration: none; font-size: 14px; font-weight: 650; transition: background .18s ease, transform .18s ease; }
.ai-career-content__action:hover { transform: translateY(-2px); background: #41464d; }
.ai-career-content__action span { font-size: 18px; }
.ai-career-content__status { flex: 0 0 auto; padding: 10px 14px; border: 1px solid #cfcbc4; border-radius: 999px; color: #777d85; background: rgba(255,255,255,.55); font-size: 13px; }
.ai-career-content__footnote { display: flex; align-items: center; gap: 9px; margin-top: 34px; padding-top: 16px; border-top: 1px solid rgba(35,38,43,.12); color: #68717a; font-size: 12px; line-height: 1.6; }
.ai-career-content__dot { width: 7px; height: 7px; flex: 0 0 7px; border-radius: 50%; background: #5c8cb4; }
.ai-career-content.is-pending .ai-career-content__dot { background: #99938a; }
.ai-career-form { display: grid; gap: 14px; margin-top: 28px; }
.ai-career-field { position: relative; display: grid; gap: 8px; color: #343a40; font-size: 13px; font-weight: 680; }
.ai-career-field > span { display: flex; align-items: baseline; flex-wrap: wrap; gap: 8px; }
.ai-career-field small { color: #777d85; font-size: 11px; font-weight: 450; }
.ai-career-field input, .ai-career-field textarea { width: 100%; padding: 12px 14px; border: 1px solid rgba(35,38,43,.22); border-radius: 12px; color: #23262b; background: rgba(255,255,255,.82); font: inherit; font-weight: 450; line-height: 1.65; outline: none; }
.ai-career-field input:focus, .ai-career-field textarea:focus { border-color: #91743f; box-shadow: 0 0 0 3px rgba(145,116,63,.12); }
.ai-career-field textarea { min-height: 110px; resize: vertical; }
.ai-career-field__count { justify-self: end; margin-top: -6px; color: #777d85; font-size: 11px; font-weight: 450; }
.ai-career-form > .ai-career-content__action { justify-self: start; cursor: pointer; }
.ai-career-form > .ai-career-content__action:disabled { cursor: wait; opacity: .65; transform: none; }
.ai-career-error { margin: 0; color: #9a3d34; font-size: 13px; line-height: 1.6; }
.ai-career-result { overflow: hidden; border: 1px solid rgba(35,38,43,.16); border-radius: 14px; background: rgba(255,255,255,.82); }
.ai-career-result > header { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 12px 15px; border-bottom: 1px solid rgba(35,38,43,.12); color: #343a40; font-size: 13px; }
.ai-career-result > header > div { display: flex; gap: 8px; }
.ai-career-result button { padding: 6px 9px; border: 1px solid #ded9d1; border-radius: 8px; color: #343a40; background: #fff; font-size: 12px; cursor: pointer; }
.ai-career-result pre { max-height: 420px; overflow: auto; margin: 0; padding: 16px; color: #343a40; font: inherit; font-size: 13px; line-height: 1.75; white-space: pre-wrap; overflow-wrap: anywhere; }

@keyframes ai-career-enter { from { opacity: .65; transform: translateY(5px); } to { opacity: 1; transform: translateY(0); } }

.section-hub__card {
  position: relative;
  display: grid;
  grid-template-columns: 42px 1fr 42px;
  gap: 18px;
  min-height: 188px;
  padding: 28px;
  overflow: hidden;
  border: 1px solid #e3ded6;
  border-radius: 24px;
  color: #23262b;
  text-decoration: none;
  background: #fff;
  box-shadow: 0 10px 28px rgba(35, 38, 43, .05);
  transition: transform .18s ease, box-shadow .18s ease;
}

.section-hub__card:hover {
  transform: translateY(-3px);
  box-shadow: 0 16px 36px rgba(35, 38, 43, .09);
}

.section-hub__card.is-pending {
  cursor: default;
  box-shadow: none;
}

.section-hub__card.is-pending:hover {
  transform: none;
}

.section-hub__card--blue { background: #e7f0f7; }
.section-hub__card--green { background: #e7efe1; }
.section-hub__card--yellow { background: #f4e8b9; }
.section-hub__card--pink { background: #f1e3e2; }
.section-hub__card--purple { background: #ece7f4; }

.section-hub__code {
  color: #69717b;
  font: 700 13px/1.2 Inter, sans-serif;
  letter-spacing: .08em;
}

.section-hub__card h2 {
  margin: 0 0 12px;
  font-size: 24px;
  line-height: 1.2;
  letter-spacing: -.035em;
}

.section-hub__card p {
  margin: 0;
  color: #5f6670;
  font-size: 14px;
  line-height: 1.75;
}

.section-hub__status {
  display: inline-flex;
  margin-top: 18px;
  padding: 5px 10px;
  border: 1px solid rgba(35, 38, 43, .16);
  border-radius: 999px;
  color: #5f6670;
  background: rgba(255, 255, 255, .56);
  font-size: 12px;
  font-weight: 650;
}

.section-hub__arrow {
  align-self: end;
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  color: #fff;
  background: #23262b;
  font-size: 19px;
}

@media (max-width: 760px) {
  .section-hub__main { width: min(100% - 24px, 1180px); padding-top: 92px; }
  .section-hub__grid { grid-template-columns: 1fr; }
  .section-hub__card { grid-template-columns: 34px 1fr 38px; padding: 22px; }
  .ai-career-nav { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); }
  .ai-career-nav__item { justify-content: flex-start; padding: 0 12px; }
  .ai-career-content { min-height: 280px; padding: 24px 22px 18px; border-radius: 20px; }
  .ai-career-content__body { align-items: flex-start; flex-direction: column; gap: 22px; margin-top: 36px; }
  .ai-career-content__footnote { margin-top: 26px; }
  .ai-career-form > .ai-career-content__action { width: 100%; justify-content: space-between; }
}
</style>
