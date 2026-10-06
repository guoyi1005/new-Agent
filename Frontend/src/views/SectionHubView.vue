<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import AppTabBar from '../components/AppTabBar.vue'

const route = useRoute()

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
      { code: '02', title: 'AI模拟面试与报告', description: '在独立面试工作台开始模拟面试，并查看面试表现分析、历史记录与评估报告。', to: '/interview/index', tone: 'yellow' },
    ],
  },
}

const section = computed(() => sections[route.meta.section] || sections.aiCareer)
const isAiCareer = computed(() => route.meta.section === 'aiCareer')
const aiCareerItems = computed(() => sections.aiCareer.items)
</script>

<template>
  <div class="section-hub">
    <AppTabBar />
    <main class="section-hub__main" :class="{ 'section-hub__main--ai-career': isAiCareer }">
      <header class="section-hub__hero" :class="{ 'section-hub__hero--ai-career': isAiCareer }">
        <div class="section-hub__hero-copy">
          <p>{{ section.eyebrow }}</p>
          <h1>{{ section.title }}</h1>
          <span>{{ section.description }}</span>
          <div v-if="isAiCareer" class="ai-career-hero__caption">从一份更清晰的简历，到一次更从容的面试。</div>
        </div>
        <div v-if="isAiCareer" class="ai-career-hero__art" aria-hidden="true">
          <div class="ai-career-hero__orbit"></div>
          <div class="ai-career-hero__paper"><span>CURRICULUM VITAE</span><i></i><i></i><i></i><b>→</b></div>
          <span class="ai-career-hero__stamp">YOUR NEXT<br>CHAPTER</span>
        </div>
      </header>

      <template v-if="isAiCareer">
        <div class="ai-career-sections">
          <section
            v-for="item in aiCareerItems"
            :key="item.code"
            class="ai-career-content"
            :class="`ai-career-content--${item.tone}`"
          >
            <div class="ai-career-content__index">{{ item.code }} <span>/ AI CAREER</span></div>
            <div class="ai-career-content__body">
              <div>
                <span class="ai-career-content__eyebrow">{{ item.title === '智能简历' ? '简历工具' : '面试训练 · 表现分析 · 历史报告' }}</span>
                <h2>{{ item.title }}</h2>
                <p>{{ item.description }}</p>
              </div>
              <RouterLink v-if="item.to" class="ai-career-content__action" :to="item.to">
                {{ item.title === 'AI模拟面试与报告' ? '打开面试工作台' : `进入${item.title}` }} <span aria-hidden="true">→</span>
              </RouterLink>
            </div>
            <div v-if="item.title === '智能简历'" class="ai-career-art ai-career-art--resume" aria-hidden="true">
              <div class="ai-career-art__sheet"><span>PROFILE / 01</span><i></i><i></i><i></i><i></i></div>
              <div class="ai-career-art__seal">CV<span>↗</span></div>
            </div>
            <div v-if="item.title === 'AI模拟面试与报告'" class="ai-career-art ai-career-art--interview" aria-hidden="true">
              <span class="ai-career-art__letter">Q</span><span class="ai-career-art__connector">—</span><span class="ai-career-art__letter">A</span>
              <span class="ai-career-art__note">PRACTICE · REFLECT · GROW</span>
            </div>
            <div class="ai-career-content__footnote">
              <span class="ai-career-content__dot" />
              <span v-if="item.title === 'AI模拟面试与报告'">面试工作台包含开始模拟、表现分析、面试历史及评估报告；报告根据真实面试记录生成。</span>
              <span v-else>使用现有简历工作台创建、编辑和管理简历。</span>
            </div>
          </section>
        </div>
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

.section-hub__main--ai-career {
  width: min(1320px, calc(100% - 48px));
}

.section-hub__hero {
  max-width: 760px;
  margin-bottom: 38px;
}

.section-hub__hero-copy > p {
  margin: 0 0 14px;
  color: #5c8cb4;
  font: 700 12px/1.2 Inter, sans-serif;
  letter-spacing: .16em;
}

.section-hub__hero-copy h1 {
  margin: 0 0 14px;
  font-size: clamp(38px, 5vw, 64px);
  line-height: 1;
  letter-spacing: -.055em;
}

.section-hub__hero-copy > span {
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

.section-hub__hero--ai-career {
  position: relative;
  display: flex;
  align-items: center;
  max-width: none;
  min-height: 348px;
  padding: 52px 64px;
  overflow: hidden;
  border: 1px solid #4b3a30;
  border-radius: 30px;
  background: #302b27;
  box-shadow: 0 18px 42px rgba(65, 43, 28, .13);
}
.section-hub__hero--ai-career .section-hub__hero-copy { position: relative; z-index: 1; max-width: 600px; }
.section-hub__hero--ai-career .section-hub__hero-copy > p { color: #e3a36b; }
.section-hub__hero--ai-career h1 { margin-bottom: 22px; color: #fff7e9; font-size: clamp(60px, 7vw, 96px); letter-spacing: -.08em; }
.section-hub__hero--ai-career .section-hub__hero-copy > span { max-width: 520px; color: #e7d9c5; font-size: 17px; }
.ai-career-hero__caption { margin-top: 30px; padding-left: 14px; border-left: 2px solid #d99b6a; color: #e2b98e; font-size: 13px; letter-spacing: .03em; }
.ai-career-hero__art { position: absolute; inset: 0 0 0 auto; width: 43%; pointer-events: none; }
.ai-career-hero__orbit { position: absolute; top: -170px; right: -68px; width: 590px; height: 590px; border: 1px solid rgba(255, 240, 213, .24); border-radius: 50%; background: #a96747; }
.ai-career-hero__orbit::after { position: absolute; inset: 55px; border: 1px solid rgba(255, 240, 213, .38); border-radius: 50%; content: ''; }
.ai-career-hero__paper { position: absolute; top: 46px; right: 22%; display: grid; align-content: start; gap: 18px; width: 225px; height: 282px; padding: 32px 26px; border: 1px solid #e4d0aa; border-radius: 12px; background: #fff8e9; box-shadow: 18px 20px 0 rgba(49, 42, 37, .18); transform: rotate(10deg); }
.ai-career-hero__paper span { color: #89553f; font: 800 10px/1.2 Inter, sans-serif; letter-spacing: .15em; }
.ai-career-hero__paper i { height: 8px; border-radius: 8px; background: #decfba; }
.ai-career-hero__paper i:nth-of-type(2) { width: 75%; }
.ai-career-hero__paper i:nth-of-type(3) { width: 88%; }
.ai-career-hero__paper b { position: absolute; right: 22px; bottom: 20px; color: #b45e3a; font-size: 42px; font-weight: 400; }
.ai-career-hero__stamp { position: absolute; right: 70%; bottom: 28px; color: #fff0d6; font: 800 13px/1.45 Inter, sans-serif; letter-spacing: .18em; transform: rotate(-8deg); }

.ai-career-sections { display: grid; gap: 18px; }
.ai-career-content {
  --section-accent: #a36343;
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, .8fr) minmax(0, 1.2fr);
  column-gap: clamp(26px, 5vw, 80px);
  min-height: 290px;
  padding: 34px clamp(28px, 4vw, 58px) 24px;
  overflow: hidden;
  border: 1px solid #e4d8c8;
  border-radius: 26px;
  background: #f5eee3;
  box-shadow: 0 8px 22px rgba(79, 50, 29, .045);
  animation: ai-career-enter .38s ease both;
}
.ai-career-content::before { position: absolute; top: -180px; right: -90px; width: 490px; height: 490px; border: 1px solid rgba(105, 77, 54, .12); border-radius: 50%; content: ''; pointer-events: none; }
.ai-career-content--yellow { --section-accent: #a66438; background: #f4e5d1; }
.ai-career-content__index { grid-column: 1 / -1; display: flex; align-items: center; gap: 8px; color: var(--section-accent); font: 800 13px/1.2 Inter, sans-serif; letter-spacing: .12em; }
.ai-career-content__index::after { width: 52px; height: 1px; margin-left: 10px; background: currentColor; content: ''; opacity: .55; }
.ai-career-content__index span { color: #75685a; font-weight: 600; }
.ai-career-content__body { position: relative; z-index: 1; display: flex; align-items: flex-start; flex-direction: column; justify-content: center; padding: 38px 0 28px; }
.ai-career-content__eyebrow { color: var(--section-accent); font-size: 12px; font-weight: 800; letter-spacing: .12em; }
.ai-career-content h2 { margin: 14px 0 18px; color: #2d2925; font-size: clamp(31px, 3.2vw, 46px); line-height: 1.14; letter-spacing: -.055em; }
.ai-career-content p { max-width: 420px; margin: 0; color: #5b5149; font-size: 15px; line-height: 1.8; }
.ai-career-content__action { display: inline-flex; align-items: center; justify-content: space-between; gap: 28px; min-height: 46px; margin-top: 28px; padding: 10px 18px; border: 1px solid #332d29; border-radius: 10px; color: #fff8ec; background: #332d29; text-decoration: none; font-size: 14px; font-weight: 700; transition: background .2s ease, transform .2s ease, box-shadow .2s ease; }
.ai-career-content__action:hover { transform: translateY(-2px); background: #754a34; box-shadow: 0 8px 16px rgba(87, 54, 36, .14); }
.ai-career-content__action:focus-visible { outline: 3px solid #c2754e; outline-offset: 3px; }
.ai-career-content__action span { font-size: 18px; }
.ai-career-content__footnote { grid-column: 1 / -1; display: flex; align-items: center; gap: 10px; padding-top: 15px; border-top: 1px solid rgba(75, 54, 38, .16); color: #655a50; font-size: 12px; line-height: 1.6; }
.ai-career-content__dot { width: 7px; height: 7px; flex: 0 0 7px; border-radius: 50%; background: var(--section-accent); }
.ai-career-art { position: relative; z-index: 1; display: grid; place-items: center; align-self: center; height: 240px; overflow: hidden; border: 1px solid rgba(81, 59, 39, .15); border-radius: 20px; background: rgba(255, 250, 240, .55); }
.ai-career-art--resume::before { position: absolute; bottom: -110px; left: -30px; width: 340px; height: 340px; border: 1px solid rgba(125, 79, 50, .16); border-radius: 50%; content: ''; }
.ai-career-art__sheet { display: grid; align-content: start; gap: 14px; width: 170px; height: 210px; padding: 24px; border: 1px solid #d2bca1; border-radius: 6px; background: #fffcf4; box-shadow: 13px 12px 0 #e4d1b6; transform: rotate(-8deg) translateY(24px); }
.ai-career-art__sheet span { color: #8b5239; font: 800 9px/1.2 Inter, sans-serif; letter-spacing: .12em; }
.ai-career-art__sheet i { height: 6px; border-radius: 8px; background: #ded3c4; }
.ai-career-art__sheet i:nth-of-type(3) { width: 76%; }
.ai-career-art__sheet i:nth-of-type(4) { width: 88%; }
.ai-career-art__seal { position: absolute; right: 19%; bottom: 24px; display: flex; align-items: center; justify-content: center; gap: 9px; width: 78px; height: 78px; border-radius: 50%; color: #fff8ec; background: #a66343; font: 800 22px Inter, sans-serif; transform: rotate(12deg); }
.ai-career-art__seal span { font-size: 18px; }
.ai-career-art--interview { display: flex; gap: 15px; background: #fff4e3; }
.ai-career-art__letter { color: #794b35; font: 800 clamp(90px, 10vw, 146px)/1 Inter, sans-serif; letter-spacing: -.1em; }
.ai-career-art__connector { color: #c67d54; font-size: 58px; font-weight: 200; }
.ai-career-art__note { position: absolute; right: 24px; bottom: 16px; color: #875b45; font: 800 10px Inter, sans-serif; letter-spacing: .16em; }

@keyframes ai-career-enter { from { opacity: .7; transform: translateY(9px); } to { opacity: 1; transform: translateY(0); } }

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
  .section-hub__main--ai-career { width: calc(100% - 24px); }
  .section-hub__grid { grid-template-columns: 1fr; }
  .section-hub__card { grid-template-columns: 34px 1fr 38px; padding: 22px; }
  .section-hub__hero--ai-career { min-height: 300px; padding: 38px 30px; border-radius: 24px; }
  .section-hub__hero--ai-career h1 { font-size: clamp(52px, 12vw, 74px); }
  .ai-career-hero__art { right: -90px; width: 60%; opacity: .23; }
  .ai-career-hero__caption { max-width: 280px; }
  .ai-career-content { grid-template-columns: 1fr; min-height: 0; padding: 26px 24px 20px; border-radius: 20px; }
  .ai-career-content__index, .ai-career-content__footnote { grid-column: 1; }
  .ai-career-content__body { padding: 30px 0 24px; }
  .ai-career-content h2 { font-size: clamp(29px, 8vw, 38px); }
  .ai-career-art { height: 190px; margin-bottom: 24px; }
  .ai-career-art__letter { font-size: 105px; }
}

@media (max-width: 440px) {
  .section-hub__hero--ai-career { padding: 34px 24px; }
  .section-hub__hero--ai-career .section-hub__hero-copy > span { font-size: 14px; }
  .ai-career-hero__art { display: none; }
  .ai-career-content { padding-inline: 19px; }
}

@media (prefers-reduced-motion: reduce) {
  .ai-career-content { animation: none; }
  .ai-career-content__action { transition: none; }
}
</style>
