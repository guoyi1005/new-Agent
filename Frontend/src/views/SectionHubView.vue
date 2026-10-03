<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import AppTabBar from '../components/AppTabBar.vue'

const route = useRoute()

const sections = {
  growth: {
    eyebrow: 'GROWTH CENTER',
    title: '成长中心',
    description: '集中查看能力、学习记录与成长路径，了解自己正在向目标前进到哪一步。',
    items: [
      { code: '01', title: '成长树', description: '按岗位方向查看成长分支，以及已点亮和未点亮的技能点。', to: '/growth/tree', tone: 'green' },
      { code: '02', title: '技能树', description: '查看已有技能掌握情况、技能关联与能力进度。', to: '/growth/skills', tone: 'blue' },
      { code: '03', title: '课程成绩', description: '查看已有试卷、测验成绩与答题记录。', to: '/growth/grades', tone: 'yellow' },
      { code: '04', title: '证书', description: '证书管理功能尚未接入真实数据。', tone: 'purple', pending: true },
      { code: '05', title: '项目', description: '项目成果与完成记录模块尚待建设。', tone: 'pink', pending: true },
      { code: '06', title: '校园活动', description: '浏览活动列表、详情、地点、报名状态及已参加活动。', to: '/growth/campus-activity', tone: 'pink' },
      { code: '07', title: '校园地图', description: '使用现有地图浏览、建筑搜索、定位与区域查看功能。', to: '/growth/campus-map', tone: 'blue' },
      { code: '08', title: '能力档案', description: '查看基于真实学习与使用记录生成的个人能力画像。', to: '/growth/profile', tone: 'green' },
    ],
  },
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
      { code: '01', title: '智能简历', description: '查看、填写、上传和分析简历，使用现有简历辅助能力。', to: '/ai-career/resume', tone: 'blue' },
      { code: '02', title: 'AI润色', description: '独立的 AI 简历润色功能尚未实现。', tone: 'pink', pending: true },
      { code: '03', title: '岗位定制简历', description: '岗位定制简历功能尚未实现。', tone: 'green', pending: true },
      { code: '04', title: 'AI模拟面试', description: '进入完整模拟面试流程，包含环境检测、实时提示与面试过程。', to: '/ai-career/interview', tone: 'yellow' },
      { code: '05', title: '面试报告', description: '从现有面试历史查看评估结果、反馈与历史报告。', to: '/ai-career/report', tone: 'purple' },
    ],
  },
}

const section = computed(() => sections[route.meta.section] || sections.growth)
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

      <section class="section-hub__grid" :aria-label="`${section.title}功能结构`">
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
}
</style>
