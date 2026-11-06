<template>
  <div class="page-wrapper">
    <Sidebar />
    <main class="main-content">
      <div class="content-container">
        <!-- 页面标题 -->
        <div class="page-header">
          <h1 class="page-title">学习资源</h1>
        </div>

        <!-- 按岗位分类展示题目 -->
        <div v-if="loading" class="loading-text">加载中...</div>
        <div v-else-if="error" class="error-text">{{ error }}</div>
        <div v-else class="category-layout">
          <!-- 第一行：彩色渐变卡片（推荐课程样式）-->
          <section
            v-for="(items, position) in groupedCoursesRow1"
            :key="position"
            :class="['section', 'category-section', { 'product-manager-section': position === '产品经理' }]"
          >
            <div class="section-header">
              <div class="section-title-wrap">
                <h2 class="section-title">{{ position }}</h2>
              </div>
              <a href="#/question-bank" class="view-all">查看全部</a>
            </div>
            <div class="course-grid">
              <div
                v-for="(item, index) in getRow1Items(items)"
                :key="item?.id || `placeholder-${index}`"
                :class="['course-card', 'course-card-' + (index + 1), { 'placeholder-card': !item }]"
                @click="item && openDetail(item)"
              >
                <template v-if="item">
                  <div class="card-visual" :style="{ backgroundImage: `url(${index === 1 ? embeddedImg2 : index === 2 ? embeddedImg3 : embeddedImg})` }">
                    <div class="course-level">{{ getDifficultyText(item.difficulty) }}</div>
                    <div class="course-icon">{{ getQuestionIcon(item.question_type) }}</div>
                  </div>
                  <div class="course-info">
                    <div class="course-tags">
                      <span class="tag">{{ item.question_type }}</span>
                    </div>
                    <h3 class="course-title" :title="item.question">{{ item.question }}</h3>
                  </div>
                </template>
                <template v-else>
                  <div class="card-visual placeholder-visual" :style="{ backgroundImage: `url(${index === 1 ? embeddedImg2 : index === 2 ? embeddedImg3 : embeddedImg})` }">
                    <div class="course-icon">💠</div>
                  </div>
                  <div class="course-info">
                    <div class="course-tags">
                      <span class="tag">暂无数据</span>
                    </div>
                    <h3 class="course-title">敬请期待</h3>
                  </div>
                </template>
              </div>
            </div>
          </section>

          <!-- 面试文档区域：圆角长方形卡片排布 -->
          <section v-if="hasDocRowData" class="section category-section doc-section">
            <div class="section-header">
              <div class="section-title-wrap">
                <h2 class="section-title">运维开发工程师</h2>
              </div>
              <a href="#/question-bank" class="view-all">查看全部</a>
            </div>
            <div class="doc-row">
              <div
                v-for="(item, index) in docRowItems"
                :key="item.id"
                :class="['doc-card', 'doc-card-' + (index % 4)]"
                @click="openDetail(item)"
              >
                <div class="doc-icon-box">{{ getQuestionIcon(item.question_type) }}</div>
                <div class="doc-info">
                  <h3 class="doc-title" :title="item.question">{{ item.question }}</h3>
                  <p class="doc-meta">{{ item.question_type }} · {{ getDifficultyText(item.difficulty) }}</p>
                </div>
              </div>
            </div>
          </section>

          <!-- 第二行：左侧大卡片 + 右侧小卡片网格 -->
          <div v-if="hasRow2Data" class="split-layout-row">
            <!-- 左侧大卡片 -->
            <section v-for="(items, position) in groupedCoursesRow2Left" :key="position" class="section category-section featured-section">
              <div class="section-header">
                <div class="section-title-wrap">
                  <h2 class="section-title">{{ position }}</h2>
                </div>
              </div>
              <div v-if="items[0]" class="featured-card" @click="openDetail(items[0])">
                <div class="featured-badge">精选题目</div>
                <h3 class="featured-title">{{ items[0].question }}</h3>
                <p class="featured-desc">{{ items[0].excellent_answer || '暂无参考答案' }}</p>
                <span class="featured-link">查看详情</span>
                <div class="featured-stats" v-if="items.length > 1">
                  <div class="stat-item">
                    <span class="stat-label">更多题目</span>
                    <span class="stat-value">{{ items.length }} 道</span>
                  </div>
                </div>
              </div>
            </section>

            <!-- 右侧小卡片网格 -->
            <section v-for="(items, position) in groupedCoursesRow2Right" :key="position" class="section category-section grid-section">
              <div class="section-header">
                <div class="section-title-wrap">
                  <h2 class="section-title">{{ position }}</h2>
                </div>
              </div>
              <div class="mini-grid">
                <div
                  v-for="(item, index) in items.slice(0, 4)"
                  :key="item.id"
                  :class="['mini-card', 'mini-card-' + (index % 4)]"
                  @click="openDetail(item)"
                >
                  <div class="mini-icon">{{ getQuestionIcon(item.question_type) }}</div>
                  <div class="mini-badge">{{ item.question_type }}</div>
                  <span class="mini-title">{{ item.question }}</span>
                </div>
              </div>
            </section>
          </div>


        </div>

        <!-- 详情弹窗 -->
        <div v-if="selectedItem" class="modal-overlay" @click="closeDetail">
          <div class="modal-content" @click.stop>
            <div class="modal-header">
              <div class="modal-tags">
                <span class="modal-tag">{{ selectedItem.job_position }}</span>
                <span class="modal-tag">{{ selectedItem.question_type }}</span>
                <span class="modal-tag difficulty">{{ getDifficultyText(selectedItem.difficulty) }}</span>
              </div>
              <button class="modal-close" @click="closeDetail">×</button>
            </div>
            <div class="modal-body">
              <h3 class="modal-title">{{ selectedItem.question }}</h3>
              <div class="modal-section">
                <h4>参考答案</h4>
                <p>{{ selectedItem.excellent_answer || '暂无参考答案' }}</p>
              </div>
              <div v-if="selectedItem.answer_points" class="modal-section">
                <h4>答题要点</h4>
                <p>{{ selectedItem.answer_points }}</p>
              </div>
              <div v-if="selectedItem.question_intent" class="modal-section">
                <h4>考察意图</h4>
                <p>{{ selectedItem.question_intent }}</p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import Sidebar from '../components/Sidebar.vue';
import { knowledgeApi, type KnowledgeItem } from '../api/knowledge';
import embeddedImg from '@/assets/interview/Embedded1.png';
import embeddedImg2 from '@/assets/interview/Embedded2.png';
import embeddedImg3 from '@/assets/interview/Embedded3.png';

// 推荐课程数据（从题库接口获取）
const recommendedCourses = ref<KnowledgeItem[]>([]);
const loading = ref(false);
const error = ref('');
const selectedItem = ref<KnowledgeItem | null>(null);
const searchKeyword = ref('');

// 按岗位分组题目
const groupedCourses = computed(() => {
  const filtered = searchKeyword.value
    ? recommendedCourses.value.filter(item =>
        item.question.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
        item.job_position.toLowerCase().includes(searchKeyword.value.toLowerCase()) ||
        item.question_type.toLowerCase().includes(searchKeyword.value.toLowerCase())
      )
    : recommendedCourses.value;

  const groups: Record<string, KnowledgeItem[]> = {};
  filtered.forEach(item => {
    const position = item.job_position || '其他';
    if (!groups[position]) {
      groups[position] = [];
    }
    groups[position].push(item);
  });
  return groups;
});

// 获取题目图标
const getQuestionIcon = (questionType: string) => {
  const iconMap: Record<string, string> = {
    '技术问答': '💬',
    '算法题': '🔢',
    '系统设计': '🏗️',
    '场景题': '🎭',
    '概念理解': '📖',
    '编程题': '💻',
    '前端': '🎨',
    '后端': '⚙️',
    '数据库': '🗄️',
    '网络': '🌐',
    '操作系统': '🖥️',
    '安全': '🔒',
  };
  return iconMap[questionType] || '💠';
};

// 将分组数据分配到不同行
const groupedCoursesRow1 = computed(() => {
  const entries = Object.entries(groupedCourses.value);
  const result: Record<string, KnowledgeItem[]> = {};
  entries.slice(0, 1).forEach(([key, value]) => {
    result[key] = value;
  });
  return result;
});

const groupedCoursesRow2Left = computed(() => {
  const entries = Object.entries(groupedCourses.value);
  const result: Record<string, KnowledgeItem[]> = {};
  entries.slice(1, 2).forEach(([key, value]) => {
    result[key] = value;
  });
  return result;
});

const groupedCoursesRow2Right = computed(() => {
  const entries = Object.entries(groupedCourses.value);
  const result: Record<string, KnowledgeItem[]> = {};
  entries.slice(2, 3).forEach(([key, value]) => {
    result[key] = value;
  });
  return result;
});

const hasRow2Data = computed(() => {
  return Object.keys(groupedCoursesRow2Left.value).length > 0 ||
         Object.keys(groupedCoursesRow2Right.value).length > 0;
});

// 运维开发工程师的题目（固定从后端获取4个）
const devOpsItems = ref<KnowledgeItem[]>([]);

// 面试文档区域数据（固定显示运维开发工程师的4个题目）
const docRowItems = computed((): KnowledgeItem[] => {
  return devOpsItems.value.slice(0, 4);
});

const hasDocRowData = computed(() => docRowItems.value.length > 0);



// 搜索处理
const handleSearch = () => {
  // 搜索逻辑通过 computed 自动处理
};

// 打开详情弹窗
const openDetail = (item: KnowledgeItem) => {
  selectedItem.value = item;
};

// 关闭详情弹窗
const closeDetail = () => {
  selectedItem.value = null;
};

// 获取推荐题目列表
const fetchRecommendedCourses = async () => {
  loading.value = true;
  error.value = '';
  try {
    const res = await knowledgeApi.list({ page: 1, page_size: 50 });
    recommendedCourses.value = res.items;
  } catch (err) {
    error.value = '获取推荐题目失败';
    console.error('获取推荐题目失败:', err);
  } finally {
    loading.value = false;
  }
};

// 获取运维开发工程师的题目（固定4个）
const fetchDevOpsItems = async () => {
  try {
    const res = await knowledgeApi.list({
      page: 1,
      page_size: 4,
      job_position: '运维开发工程师'
    });
    devOpsItems.value = res.items;
  } catch (err) {
    console.error('获取运维开发工程师题目失败:', err);
    devOpsItems.value = [];
  }
};

// 将难度数字转换为显示文本
const getDifficultyText = (difficulty?: number) => {
  const map: Record<number, string> = { 1: 'BEGINNER', 2: 'BEGINNER', 3: 'MID-LEVEL', 4: 'ADVANCED', 5: 'EXPERT' };
  return map[difficulty || 3] || 'MID-LEVEL';
};

// 根据难度获取卡片样式类名
const getCardClass = (difficulty?: number) => {
  const map: Record<number, string> = { 1: 'green', 2: 'green', 3: 'orange', 4: 'blue', 5: 'green' };
  return map[difficulty || 3] || 'orange';
};

// 获取第一行显示的3个不同题目（当前岗位不足时从其他岗位补充）
const getRow1Items = (items: KnowledgeItem[]): KnowledgeItem[] => {
  const result: KnowledgeItem[] = [];
  // 先取当前岗位的数据
  for (let i = 0; i < Math.min(3, items.length); i++) {
    result.push(items[i]!);
  }
  // 如果不足3个，从所有题目中补充不同的数据
  if (result.length < 3) {
    const allItems = recommendedCourses.value;
    for (const item of allItems) {
      if (result.length >= 3) break;
      // 避免重复添加已存在的题目
      if (!result.some(r => r.id === item.id)) {
        result.push(item);
      }
    }
  }
  return result;
};

onMounted(() => {
  fetchRecommendedCourses();
  fetchDevOpsItems();
});
</script>

<style scoped>
.page-wrapper {
  display: flex;
  min-height: 100vh;
  background-color: var(--hp-surface);
}

.main-content {
  flex: 1;
  margin-left: clamp(200px, 25vw, 320px);
  padding: clamp(20px, 4vh, 32px) clamp(16px, 3vw, 24px);
}

.content-container {
  max-width: 1200px;
  margin: 0 auto;
}

/* 页面标题 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 32px;
}

.page-title {
  font-size: 24px;
  font-weight: 600;
  color: #ffffff;
  margin: 0;
}


/* 区块样式 */
.section {
  margin-bottom: 32px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.section-title-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.section-title {
  font-size: 16px;
  font-weight: 600;
  color: #ffffff;
  margin: 0;
}

.view-all {
  color: #4a9eff;
  font-size: 14px;
  text-decoration: none;
}

/* 分类布局 */
.category-layout {
  display: flex;
  flex-direction: column;
  gap: 32px;
}

/* 第一行：彩色渐变卡片网格 */
.course-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

/* 分类区块 */
.category-section {
  margin-bottom: 0;
}



/* 第一行：文档式小卡片横向排列 */
.doc-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.doc-row::-webkit-scrollbar {
  height: 6px;
}

.doc-row::-webkit-scrollbar-track {
  background: rgba(255, 255, 255, 0.05);
  border-radius: 3px;
}

.doc-row::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.2);
  border-radius: 3px;
}

.doc-card {
  background-color: var(--hp-surface);
  border: 1px solid var(--hp-line);
  border-radius: 12px;
  padding: 16px;
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  min-width: 0;
}

.doc-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
}

.doc-card-0 .doc-icon-box {
  background-color: rgba(239, 83, 80, 0.15);
  color: #ef5350;
}

.doc-card-1 .doc-icon-box {
  background-color: rgba(74, 158, 255, 0.15);
  color: #4a9eff;
}

.doc-card-2 .doc-icon-box {
  background-color: rgba(255, 202, 40, 0.15);
  color: #ffca28;
}

.doc-card-3 .doc-icon-box {
  background-color: rgba(0, 200, 83, 0.15);
  color: #00c853;
}

.doc-icon-box {
  width: 48px;
  height: 48px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.doc-info {
  flex: 1;
  min-width: 0;
}

.doc-title {
  font-size: 13px;
  font-weight: 500;
  color: #ffffff;
  margin: 0 0 4px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.doc-meta {
  font-size: 11px;
  color: #5a6270;
}

/* 第二行：分栏布局 */
.split-layout-row {
  display: grid;
  grid-template-columns: 1.2fr 0.8fr;
  gap: 24px;
}

/* 左侧大卡片 */
.featured-section {
  display: flex;
  flex-direction: column;
}

.featured-card {
  background-color: var(--hp-surface);
  border: 1px solid var(--hp-line);
  border-radius: 12px;
  padding: 24px;
  display: flex;
  flex-direction: column;
  flex: 1;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  border-left: 3px solid #4a9eff;
}

.featured-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
}

.featured-badge {
  font-size: 10px;
  color: #4a9eff;
  letter-spacing: 1px;
  margin-bottom: 12px;
}

.featured-title {
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
  margin: 0 0 12px 0;
  line-height: 1.4;
}

.featured-desc {
  font-size: 13px;
  color: #8b92a8;
  margin: 0 0 16px 0;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.featured-link {
  color: #4a9eff;
  font-size: 13px;
  text-decoration: none;
  margin-bottom: 16px;
}

.featured-stats {
  display: flex;
  gap: 16px;
  padding-top: 16px;
  border-top: 1px solid var(--hp-line);
}

.stat-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.stat-label {
  font-size: 11px;
  color: #5a6270;
}

.stat-value {
  font-size: 14px;
  color: #ffffff;
  font-weight: 500;
}

/* 右侧小卡片网格 */
.grid-section {
  display: flex;
  flex-direction: column;
}

.mini-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.mini-card {
  background-color: var(--hp-surface);
  border: 1px solid var(--hp-line);
  border-radius: 10px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 120px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  position: relative;
  overflow: hidden;
}

.mini-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.3);
}

.mini-card-0 {
  border-left: 3px solid #4a5cff;
}

.mini-card-1 {
  border-left: 3px solid #00c853;
}

.mini-card-2 {
  border-left: 3px solid #ff6d00;
}

.mini-card-3 {
  border-left: 3px solid #ef5350;
}

.mini-icon {
  font-size: 28px;
  opacity: 0.5;
  margin-bottom: 8px;
}

.mini-badge {
  font-size: 9px;
  color: #4a9eff;
  background-color: rgba(74, 158, 255, 0.1);
  padding: 2px 6px;
  border-radius: 3px;
  margin-bottom: 8px;
}

.mini-title {
  font-size: 12px;
  color: #ffffff;
  text-align: center;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  line-height: 1.4;
}

.course-card {
  background-color: var(--hp-surface);
  border: 1px solid var(--hp-line);
  border-radius: 12px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  min-height: 280px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.course-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.3);
}



.course-card .card-visual {
  height: 70%;
  min-height: 70%;
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
}

/* 卡片背景区域圆角 */
.course-card-1 .card-visual,
.course-card-2 .card-visual,
.course-card-3 .card-visual {
  border-radius: 12px 12px 0 0;
}

.course-level {
  position: absolute;
  top: 12px;
  left: 12px;
  background-color: rgba(0, 0, 0, 0.3);
  color: #ffffff;
  font-size: 10px;
  font-weight: 600;
  padding: 4px 8px;
  border-radius: 4px;
}

/* 统一图标图片基础样式 */
.icon-img {
  width: 48px;
  height: 48px;
  object-fit: contain;
}

/* 第一行卡片图标 */
.course-icon-img {
  width: 48px;
  height: 48px;
}

/* 面试文档卡片图标 */
.doc-icon-img {
  width: 48px;
  height: 48px;
  flex-shrink: 0;
}

/* 小卡片网格图标 */
.mini-icon-img {
  width: 28px;
  height: 28px;
  margin-bottom: 8px;
}

.course-info {
  height: 30%;
  min-height: 30%;
  background-color: var(--hp-surface);
  padding: 16px;
  border-top: 1px solid var(--hp-line);
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  overflow: hidden;
}

.course-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.tag {
  background-color: rgba(74, 158, 255, 0.15);
  color: #4a9eff;
  font-size: 11px;
  padding: 3px 8px;
  border-radius: 4px;
}

.course-title {
  font-size: 14px;
  font-weight: 600;
  color: #ffffff;
  margin: 0 0 6px 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 产品经理资源标题较长：保留图片与卡片宽度，仅释放底部信息区高度。 */
.product-manager-section .course-card .card-visual {
  height: 196px;
  min-height: 196px;
  flex: 0 0 196px;
}

.product-manager-section .course-info {
  height: auto;
  min-height: 112px;
  flex: 1 1 auto;
  padding-bottom: 18px;
  overflow: visible;
}

.product-manager-section .course-title {
  min-width: 0;
  margin-bottom: 0;
  line-height: 1.5;
  white-space: normal;
  word-break: break-word;
  overflow-wrap: anywhere;
  overflow: visible;
  text-overflow: clip;
}



/* 加载和错误状态 */
.loading-text,
.error-text {
  text-align: center;
  padding: 40px;
  color: #8b92a8;
  font-size: 14px;
}

.error-text {
  color: #ef5350;
}

/* 详情弹窗 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 20px;
}

.modal-content {
  background-color: var(--hp-surface);
  border: 1px solid var(--hp-line);
  border-radius: 12px;
  width: 100%;
  max-width: 600px;
  max-height: 80vh;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid var(--hp-line);
}

.modal-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.modal-tag {
  background-color: rgba(74, 158, 255, 0.15);
  color: #4a9eff;
  font-size: 11px;
  padding: 4px 10px;
  border-radius: 4px;
}

.modal-tag.difficulty {
  background-color: rgba(0, 200, 83, 0.15);
  color: #00c853;
}

.modal-close {
  background: none;
  border: none;
  color: #8b92a8;
  font-size: 24px;
  cursor: pointer;
  padding: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  transition: background-color 0.2s;
}

.modal-close:hover {
  background-color: rgba(255, 255, 255, 0.1);
  color: #ffffff;
}

.modal-body {
  padding: 20px;
  overflow-y: auto;
}

.modal-title {
  font-size: 18px;
  font-weight: 600;
  color: #ffffff;
  margin: 0 0 20px 0;
  line-height: 1.5;
}

.modal-section {
  margin-bottom: 20px;
}

.modal-section:last-child {
  margin-bottom: 0;
}

.modal-section h4 {
  font-size: 13px;
  font-weight: 600;
  color: #4a9eff;
  margin: 0 0 8px 0;
}

.modal-section p {
  font-size: 14px;
  color: #8b92a8;
  margin: 0;
  line-height: 1.6;
  white-space: pre-wrap;
}
</style>
